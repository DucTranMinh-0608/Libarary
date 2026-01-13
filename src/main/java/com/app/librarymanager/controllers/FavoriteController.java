package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookUser;
import com.app.librarymanager.services.MongoDB;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class FavoriteController {

  public static Document findFavorite(BookUser favorite) {
    return MongoDB.getInstance().findAnObject("favorite",
        Map.of("userId", favorite.getUserId(), "bookId", favorite.getBookId()));
  }

  public static Document addToFavorite(BookUser favorite) {
    if (findFavorite(favorite) != null) {
      return null;
    }
    return MongoDB.getInstance().addToCollection("favorite", MongoDB.objectToMap(favorite));
  }

  public static boolean removeFromFavorite(BookUser favorite) {
    Document favDoc = findFavorite(favorite);
    if (favDoc == null) {
      return false;
    }
    return MongoDB.getInstance().deleteFromCollection("favorite", "_id", favDoc.getObjectId("_id"));
  }

  public static boolean removeAllFavorite(String bookId) {
    return MongoDB.getInstance().deleteAll("favorite", Filters.eq("bookId", bookId));
  }

  public static boolean removeAllFavoriteOf(String userId) {
    return MongoDB.getInstance().deleteAll("favorite", Filters.eq("userId", userId));
  }

  public static List<Book> getFavoriteBookOfUser(String userId) {
    try {
      List<Document> documents = MongoDB.getInstance().findAllObject("favorite", "userId", userId);
      List<Book> favoriteBook = new ArrayList<>();
      documents.forEach(
          document -> favoriteBook.add(BookController.findBookByID(document.getString("bookId"))));
      return favoriteBook;
    } catch (Exception e) {
      return null;
    }
  }

  public static long countFavoriteBookOf(String userId) {
    return MongoDB.getInstance().countDocuments("favorite", Filters.eq("userId", userId));
  }
}
