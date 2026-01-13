package com.app.librarymanager.controllers;

import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.services.MongoDB;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class BookCopiesController {

  public static Map<String, Object> copiesToMap(BookCopies copies) {
    return new HashMap<>(Map.of("bookId", copies.getBookId(), "copies", copies.getCopies()));
  }

  public static Document findCopy(BookCopies copies) {
    return MongoDB.getInstance().findAnObject("bookCopies", "bookId", copies.getBookId());
  }

  public static Document addCopy(BookCopies copies) {
    Document document = findCopy(copies);
    if (document != null) {
      return document;
    }
    document = MongoDB.getInstance().addToCollection("bookCopies", copiesToMap(copies));
    return document;
  }

  public static boolean increaseCopy(BookCopies copies) {
    return MongoDB.getInstance().updateAll("bookCopies", Filters.eq("bookId", copies.getBookId()),
        Updates.combine(Updates.inc("copies", copies.getCopies()),
            Updates.set("lastUpdated", new Timestamp(System.currentTimeMillis()))));
  }

  public static Document editCopy(BookCopies copies) {
    if (copies.getCopies() < 0) {
      return null;
    }
    return MongoDB.getInstance()
        .updateData("bookCopies", "bookId", copies.getBookId(), copiesToMap(copies));
  }

  public static boolean removeCopy(BookCopies copies) {
    return MongoDB.getInstance().deleteFromCollection("bookCopies", "bookId", copies.getBookId());
  }

  public static boolean removeAllCopies(String bookId) {
    return MongoDB.getInstance().deleteAll("bookCopies", Filters.eq("bookId", bookId));
  }

  public static List<BookCopies> getCopies(int start, int length) {
    List<BookCopies> copies = new ArrayList<>();
    MongoDB.getInstance().findAllObject("bookCopies", Filters.empty(), start, length)
        .forEach(document -> copies.add(new BookCopies(document)));
    return copies;
  }
}
