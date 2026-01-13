package com.app.librarymanager.controllers;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookLoan.Mode;
import com.app.librarymanager.models.User;
import com.app.librarymanager.services.Firebase;
import com.app.librarymanager.services.MongoDB;

import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class BookLoanController {

  public static Map<String, Object> bookLoanToMap(BookLoan bookLoan) {
    return new HashMap<>(
        Map.of("userId", bookLoan.getUserId(), "bookId", bookLoan.getBookId(), "borrowDate",
            bookLoan.getBorrowDate(), "dueDate", bookLoan.getDueDate(), "valid", bookLoan.isValid(),
            "type", bookLoan.getType().name(), "numCopies", bookLoan.getNumCopies()));
  }

  private static Document findOnlineLoan(BookLoan bookLoan) {
    return MongoDB.getInstance().findAnObject("bookLoan", new HashMap<>(
        Map.of("userId", bookLoan.getUserId(), "bookId", bookLoan.getBookId(), "type", "ONLINE")));
  }

  private static Document addOfflineLoan(BookLoan bookLoan) {
    BookCopiesController.increaseCopy(
        new BookCopies(bookLoan.getBookId(), -bookLoan.getNumCopies()));
    return MongoDB.getInstance().addToCollection("bookLoan", bookLoanToMap(bookLoan));
  }

  private static Document addOnlineLoan(BookLoan bookLoan) {
    Document currentLoan = findOnlineLoan(bookLoan);
    ObjectId idInDatabase = null;
    if (currentLoan != null) {
      idInDatabase = currentLoan.getObjectId("_id");
    }
    MongoDB database = MongoDB.getInstance();
    if (idInDatabase != null) {
      return database.updateData("bookLoan", "_id", idInDatabase, bookLoanToMap(bookLoan));
    }
    return database.addToCollection("bookLoan", bookLoanToMap(bookLoan));
  }

  public static Document addLoan(BookLoan bookLoan) {
    return bookLoan.getType() == Mode.OFFLINE ? addOfflineLoan(bookLoan) : addOnlineLoan(bookLoan);
  }

  public static Document editLoan(BookLoan bookLoan) {
    return MongoDB.getInstance()
        .updateData("bookLoan", "_id", bookLoan.get_id(), bookLoanToMap(bookLoan));
  }

  public static boolean removeAllLoan(String bookId) {
    return MongoDB.getInstance().deleteAll("bookLoan", Filters.eq("bookId", bookId));
  }

  public static boolean removeAllLoanOf(String userId) {
    return MongoDB.getInstance().deleteAll("bookLoan", Filters.eq("userId", userId));
  }

  public static boolean returnAllBookOf(String userId) {
    // Just use once when deleting users, so I hardcoded it too
    // After this, userId's loan will be deleted, so I don't handle it here
    try {
      List<Map<String, Object>> updates = MongoDB.getInstance().findAllObject("bookLoan",
              Filters.and(Filters.eq("valid", true), Filters.eq("userId", userId))).stream()
          .map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookId", doc.getString("bookId"));
            map.put("copies", doc.getInteger("numCopies"));
            return map;
          }).toList();
      List<UpdateOneModel<Document>> bulkOperations = updates.stream().map(update -> {
        String id = (String) update.get("bookId");
        int copies = (int) update.get("copies");
        return new UpdateOneModel<Document>(new Document("bookId", id),
            Updates.inc("copies", copies));
      }).toList();
      MongoDB.getInstance().getDatabase().getCollection("bookCopies")
          .bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return false;
    }
  }

  public static Document returnBook(BookLoan bookLoan) {
    if (bookLoan.getType() == Mode.OFFLINE) {
      BookCopiesController.increaseCopy(
          new BookCopies(bookLoan.getBookId(), bookLoan.getNumCopies()));
    }
    return MongoDB.getInstance()
        .updateData("bookLoan", "_id", bookLoan.get_id(), new HashMap<>(Map.of("valid", false)));
  }

  @Data
  public static class ReturnBookLoan {

    private BookLoan bookLoan;
    private String titleBook;
    private String thumbnailBook;

    public ReturnBookLoan(BookLoan bookLoan, String titleBook, String thumbnailBook) {
      this.bookLoan = bookLoan;
      this.thumbnailBook = thumbnailBook;
      this.titleBook = titleBook;
    }
  }

  private static List<ReturnBookLoan> bookLoanFromDocument(List<Document> documents) {
    try {
      Map<String, Document> bookDocs = BookController.findBookByListID(
              documents.stream().map(doc -> doc.getString("bookId")).toList()).stream()
          .collect(Collectors.toMap(doc -> doc.getString("id"), doc -> doc));
      return documents.stream().map(doc -> {
        Document bookDoc = bookDocs.get(doc.getString("bookId"));
        return new ReturnBookLoan(new BookLoan(doc), bookDoc.getString("title"),
            bookDoc.getString("thumbnail"));
      }).toList();
    } catch (Exception e) {
      return null;
    }
  }

  @Data
  public static class BookLoanUser {

    private final ObjectProperty<User> user;
    private final ObjectProperty<BookLoan> bookLoan;
    private final ObjectProperty<Book> book;
//
//    User user;
//    Book book;
//    BookLoan bookLoan;

    public BookLoanUser(User user, Book book, BookLoan bookLoan) {
      this.user = new SimpleObjectProperty<>(user);
      this.bookLoan = new SimpleObjectProperty<>(bookLoan);
      this.book = new SimpleObjectProperty<>(book);
    }

    public User getUser() {
      return user.get();
    }

    public ObjectProperty<User> userProperty() {
      return user;
    }

    public BookLoan getBookLoan() {
      return bookLoan.get();
    }

    public ObjectProperty<BookLoan> bookLoanProperty() {
      return bookLoan;
    }

    public Book getBook() {
      return book.get();
    }

    public ObjectProperty<Book> bookProperty() {
      return book;
    }

  }

  public static List<BookLoanUser> getAllLentBook(int start, int length) {
    try {
      List<Document> bookLoanDocs = MongoDB.getInstance()
          .findAllObject("bookLoan", Filters.empty(), start, length);
      Map<String, User> relatedUser = UserController.listUsers(
          bookLoanDocs.stream().map(doc -> doc.getString("userId")).toList()).stream().collect(
          Collectors.toMap(User::getUid, user -> user, (existing, replacement) -> existing));
      Map<String, Book> relatedBook = BookController.listDocsToListBook(
              BookController.findBookByListID(
                  bookLoanDocs.stream().map(doc -> doc.getString("bookId")).toList())).stream()
          .collect(Collectors.toMap(Book::getId, book -> book));
      return bookLoanDocs.stream().map(
          doc -> new BookLoanUser(relatedUser.get(doc.getString("userId")),
              relatedBook.get(doc.getString("bookId")), new BookLoan(doc))).toList();
    } catch (Exception e) {
      //  System.out.println(e.getMessage());
      return null;
    }
  }

  public static List<ReturnBookLoan> getAllLentBookOf(String userId, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findAllObject("bookLoan", Filters.and(eq("userId", userId)), start, length));
  }

  public static long countLentBookOf(String userId) {
    return MongoDB.getInstance()
        .countDocuments("bookLoan", Filters.and(eq("userId", userId), eq("valid", true)));
  }

  public static List<ReturnBookLoan> getRecentLoan(int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findSortedObject("bookLoan", Filters.eq("valid", true),
            Sorts.orderBy(Sorts.descending("lastUpdated")), start, length));
  }

  public static List<ReturnBookLoan> getTopLentBook(int start, int length) {
    try {
      List<Document> documents = MongoDB.getInstance().getAggregate("bookLoan", List.of(
          new Document("$group",
              new Document("_id", "$bookId").append("count", new Document("$sum", 1))),
          new Document("$sort", new Document("count", -1)), new Document("$skip", start),
          new Document("$limit", length)));
      Map<String, Document> bookDocs = BookController.findBookByListID(
              documents.stream().map(doc -> doc.getString("_id")).toList()).stream()
          .collect(Collectors.toMap(doc -> doc.getString("id"), doc -> doc));
      return documents.stream().map(doc -> {
        Document bookDoc = bookDocs.get(doc.getString("_id"));
        return new ReturnBookLoan(
            new BookLoan("", doc.getString("_id"), new Date(), new Date(), doc.getInteger("count")),
            bookDoc.getString("title"), bookDoc.getString("thumbnail"));
      }).toList();
    } catch (Exception e) {
        System.out.println(e.getMessage());
      return null;
    }
  }

  private static Bson getFilter(boolean isValid, boolean isNotValid, boolean isOnline,
      boolean isOffline) {
    List<Bson> validConditions = new ArrayList<>();
    if (isValid) {
      validConditions.add(Filters.eq("valid", true));
    }
    if (isNotValid) {
      validConditions.add(Filters.eq("valid", false));
    }
    List<Bson> onlineConditions = new ArrayList<>();
    if (isOnline) {
      onlineConditions.add(Filters.eq("type", Mode.ONLINE.toString()));
    }
    if (isOffline) {
      onlineConditions.add(Filters.eq("type", Mode.OFFLINE.toString()));
    }
    Bson filters = Filters.empty();
    if (!validConditions.isEmpty()) {
      filters = Filters.and(filters, Filters.or(validConditions.toArray(Bson[]::new)));
    } else {
      return Filters.expr(false);
    }
    if (!onlineConditions.isEmpty()) {
      filters = Filters.and(filters, Filters.or(onlineConditions.toArray(Bson[]::new)));
    } else {
      return Filters.expr(false);
    }
    return filters;
  }

  public static List<ReturnBookLoan> getLoanWithFilter(boolean isValid, boolean isNotValid,
      boolean isOnline, boolean isOffline, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findAllObject("bookLoan", getFilter(isValid, isNotValid, isOnline, isOffline), start,
            length));
  }

  public static long countLoanWithFilter(boolean isValid, boolean isNotValid, boolean isOnline,
      boolean isOffline) {
    return MongoDB.getInstance()
        .countDocuments("bookLoan", getFilter(isValid, isNotValid, isOnline, isOffline));
  }

  public static List<ReturnBookLoan> getLoanWithFilterOfUser(String userId, boolean isValid,
      boolean isNotValid, boolean isOnline, boolean isOffline, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance().findAllObject("bookLoan",
        Filters.and(Filters.eq("userId", userId),
            getFilter(isValid, isNotValid, isOnline, isOffline)), start, length));
  }

  public static long countLoanWithFilterOfUser(String userId, boolean isValid, boolean isNotValid,
      boolean isOnline, boolean isOffline) {
    return MongoDB.getInstance().countDocuments("bookLoan",
        Filters.and(Filters.eq("userId", userId),
            getFilter(isValid, isNotValid, isOnline, isOffline)));
  }

  public static long numberOfRecords() {
    return MongoDB.getInstance().countDocuments("bookLoan");
  }

  public static long countLentBook() {
    return MongoDB.getInstance().countDocuments("bookLoan", Filters.eq("valid", true));
  }

  public static boolean refreshDatabase() {
    try {
      Date curDate = new Date();
      Bson filterDate = Filters.and(lte("dueDate", curDate), eq("valid", true));
      Bson filterOffline = Filters.and(filterDate, eq("type", Mode.OFFLINE.name()));
      MongoDB.getInstance().findAllObject("bookLoan", filterOffline).forEach(
          e -> BookCopiesController.increaseCopy(
              new BookCopies(e.getString("bookId"), e.getInteger("numCopies"))));
      return MongoDB.getInstance().updateAll("bookLoan", filterDate,
          Updates.combine(Updates.set("valid", false),
              Updates.set("lastUpdated", new Timestamp(System.currentTimeMillis()))));
    } catch (Exception e) {
      return false;
    }
  }
}
