package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.Categories;
import com.app.librarymanager.services.MongoDB;
import com.app.librarymanager.utils.Fetcher;
import com.app.librarymanager.utils.StringUtil;
import com.mongodb.client.model.Filters;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.stream.IntStream;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BookController {

  private static final Dotenv dotenv = Dotenv.load();
  private static final String SEARCH_URL = "https://www.googleapis.com/books/v1/volumes?q=";

  /**
   * Get all string contains in an entity of a json object. For example: jsonObject = { "string":
   * ["string1", "string2"] }, it would return {"string1", "string2" }.
   *
   * @param key        name of the entity needed to get
   * @param jsonObject object to fetch from
   * @return an ArrayList contains all string in `key`
   */
  private static ArrayList<String> getAllString(String key, JSONObject jsonObject) {
    try {
      JSONArray jsonArray = jsonObject.getJSONArray(key);
      ArrayList<String> listString = new ArrayList<>();
      IntStream.range(0, jsonArray.length())
          .forEach(i -> listString.add(jsonArray.optString(i, "N/A")));
      return listString;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }


  private static int numBookInUrl(String searchUrl) {
    JSONObject jsonObject = Fetcher.get(searchUrl);
    if (jsonObject == null) {
      return 0;
    }
    return jsonObject.optInt("totalItems");
  }

  private static List<Book> getBookInURL(String searchUrl) {
    try {
      List<Book> bookList = new ArrayList<>();

      JSONObject jsonObject = Fetcher.get(searchUrl);

      assert jsonObject != null;
      JSONArray jsonArray = jsonObject.getJSONArray("items");

      //System.out.println(searchUrl + " " + jsonObject.getJSONArray("items"));
      for (int indexBook = 0; indexBook < jsonArray.length(); indexBook++) {
        JSONObject curBook = jsonArray.getJSONObject(indexBook);

        String id = curBook.getString("id");

        JSONObject volumeInfo = curBook.getJSONObject("volumeInfo");
        String title = volumeInfo.optString("title", "N/A");
        String publisher = volumeInfo.optString("publisher", "N/A");
        String publishedDate = volumeInfo.optString("publishedDate", "N/A");
        String description = volumeInfo.optString("description", "N/A");
        int pageCount = volumeInfo.optInt("pageCount", 0);

        ArrayList<String> categories = getAllString("categories", volumeInfo);

        JSONArray industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers");
        String iSBN = "N/A";
        if (industryIdentifiers != null) {
          for (int j = 0; j < industryIdentifiers.length(); j++) {
            JSONObject currentIdentifier = industryIdentifiers.getJSONObject(j);
            if (currentIdentifier.getString("type").equals("ISBN_13")) {
              iSBN = currentIdentifier.getString("identifier");
              break;
            }
            if (currentIdentifier.getString("type").equals("ISBN_10")) {
              iSBN = currentIdentifier.getString("identifier");
              break;
            }
          }
        }

        String thumbnail = "https://books.google.com/books/content?id=" + id
            + "&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api";

        String language = volumeInfo.optString("language", "N/A");

        ArrayList<String> authors = getAllString("authors", volumeInfo);

        JSONObject saleInfo = curBook.getJSONObject("saleInfo");
        double price = 0;
        String currencyCode = "N/A";

        if (!saleInfo.getString("saleability").equals("NOT_FOR_SALE")) {
          JSONObject retailPrice = saleInfo.optJSONObject("retailPrice");

          if (retailPrice == null) {
            price = 0;
            currencyCode = "N/A";
          } else {
            price = retailPrice.getInt("amount");
            currencyCode = retailPrice.getString("currencyCode");
          }
        }

        JSONObject accessInfo = curBook.optJSONObject("accessInfo");
        String pdfLink = "N/A";
        if (accessInfo != null) {
          JSONObject pdfJson = accessInfo.optJSONObject("pdf");
          if (pdfJson != null) {
            pdfLink = pdfJson.optString("downloadLink", "N/A");
          }
        }

        bookList.add(
            new Book(id, title, publisher, publishedDate, description, pageCount, categories, iSBN,
                thumbnail, language, authors, price, currencyCode, pdfLink));
      }

      return bookList;
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      return new ArrayList<>();
    }

  }

  public static int numBookWithKeyword(String keyword) {
    String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    String searchUrl = SEARCH_URL + encodedKeyword + "&key=" + dotenv.get("GBOOKS_API_KEY");
    return numBookInUrl(searchUrl);
  }

  public static List<Book> searchByKeyword(String keyword, int start, int length) {
    String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    String searchUrl =
        SEARCH_URL + encodedKeyword + "&key=" + dotenv.get("GBOOKS_API_KEY") + "&maxResults="
            + length + "&startIndex=" + start;
    return getBookInURL(searchUrl);
  }

  public static Book searchByISBN(String iSBN) {
    try {
      String searchUrl = SEARCH_URL + "isbn:" + iSBN + "&key=" + dotenv.get("GBOOKS_API_KEY");
      List<Book> bookList = getBookInURL(searchUrl);
      if (bookList.isEmpty()) {
        return null;
      }
      return bookList.get(0);
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isAvailable(Book book) {
    MongoDB database = MongoDB.getInstance();
    if (database.findAnObject("books", "id", book.getId()) != null) {
      return true;
    }
    if (book.getISBN().equals("N/A")) {
      return false;
    }
    return database.findAnObject("books", "iSBN", book.getISBN()) != null;
  }

  public static Book getBookFromDocument(Document document) {
    Book curBook = MongoDB.jsonToObject(document.toJson(), Book.class);
    curBook.set_id(document.getObjectId("_id"));
    curBook.setLastUpdated(document.getDate("lastUpdated"));
    return curBook;
  }

  public static boolean findBook(Book book) {
    return MongoDB.getInstance().findAnObject("books",
        Filters.or(Filters.and(Filters.eq("iSBN", book.getISBN()), Filters.ne("iSBN", "N/A")),
            Filters.eq("id", book.getId()))) != null;
  }

  public static Book findBookByISBN(String iSBN) {
    return getBookFromDocument(MongoDB.getInstance().findAnObject("books", "iSBN", iSBN));
  }

  public static Book findBookByID(String id) {
    return getBookFromDocument(MongoDB.getInstance().findAnObject("books", "id", id));
  }

  // find all books which title contains `keyword`
  public static List<Book> findBookByKeyword(String keyword, int start, int length) {
    try {
      List<Document> jsonBook = MongoDB.getInstance()
          .findAllObject("books", Filters.regex("title", StringUtil.escapeString(keyword), "i"),
              start,
              length);
      List<Book> result = new ArrayList<>();
      jsonBook.forEach(curBook -> result.add(getBookFromDocument(curBook)));
      return result;
    } catch (Exception e) {
      return null;
    }
  }

  public static long countBookByKeyword(String keyword) {
    return MongoDB.getInstance()
        .countDocuments("books", Filters.regex("title", StringUtil.escapeString(keyword), "i"));
  }

  public static List<Book> listDocsToListBook(List<Document> docs) {
    try {
      return docs.stream().map(BookController::getBookFromDocument).toList();
    } catch (Exception e) {
      return null;
    }
  }

  public static List<Document> findBookByListID(List<String> bookId) {
    return MongoDB.getInstance().findAllObject("books", Filters.in("id", bookId));
  }

  public static long numberOfBooks() {
    return MongoDB.getInstance().countDocuments("books");
  }

  public static long numberOfActiveBooks() {
    return MongoDB.getInstance().countDocuments("books", Filters.eq("activated", true));
  }

  public static Document addBook(Book book) {
    if (isAvailable(book)) {
      return null;
    }
    CategoriesController.addCategoryList(
        book.getCategories().stream().map(Categories::new).toList());
    return MongoDB.getInstance().addToCollection("books", MongoDB.objectToMap(book));
  }

  public static boolean deleteBook(Book book) {
    if (!isAvailable(book)) {
      return false;
    }
    BookLoanController.removeAllLoan(book.getId());
    BookCopiesController.removeAllCopies(book.getId());
    CommentController.removeAllComment(book.getId());
    BookRatingController.removeAllRating(book.getId());
    FavoriteController.removeAllFavorite(book.getId());
    return MongoDB.getInstance().deleteFromCollection("books", "id", book.getId());
  }

  public static Document editBook(Book book) {
    MongoDB database = MongoDB.getInstance();

    List<Document> relatedBook = database.findAllObject("books",
        Filters.or(Filters.and(Filters.ne("iSBN", "N/A"), Filters.eq("iSBN", book.getISBN())),
            Filters.eq("id", book.getId())));

    if (relatedBook == null || relatedBook.size() != 1) {
      return null;
    }

    Document document = relatedBook.get(0);
    if (document == null || !document.getObjectId("_id").equals(book.get_id())) {
      return null;
    }

    CategoriesController.addCategoryList(
        book.getCategories().stream().map(Categories::new).toList());
    document = database.updateData("books", "id", book.getId(), MongoDB.objectToMap(book));
    return document;
  }
}