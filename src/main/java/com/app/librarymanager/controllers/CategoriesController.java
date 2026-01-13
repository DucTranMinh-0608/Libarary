package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.Categories;
import com.app.librarymanager.services.MongoDB;
import com.app.librarymanager.utils.StringUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.checkerframework.checker.units.qual.C;

public class CategoriesController {

  public static Document findCategory(Categories categories) {
//    System.out.println(categories.getName());
    return MongoDB.getInstance()
        .findAnObject("categories", Filters.eq("name", categories.getName()));
  }

  public static Document addCategory(Categories categories) {
    Document document = findCategory(categories);
    if (document != null) {
      return null;
    }
    document = MongoDB.getInstance()
        .addToCollection("categories", Map.of("name", categories.getName()));
    return document;
  }

  public static boolean addCategoryList(List<Categories> categories) {
    // Just update bulk in this function, so I decided to hard-code
    try {
//      System.err.println("Trying to add " + categories);
      MongoCollection<Document> categoriesCollection = MongoDB.getInstance().getDatabase()
          .getCollection("categories");
      Set<String> existedCategories = MongoDB.getInstance().findAllObject("categories",
              Filters.in("name", categories.stream().map(Categories::getName).toList())).stream()
          .map(doc -> doc.getString("name")).collect(Collectors.toSet());
      List<Categories> uniqueCategories = categories.stream()
          .filter(cat -> !existedCategories.contains(cat.getName())).toList();
      if (!uniqueCategories.isEmpty()) {
        uniqueCategories.forEach(cat -> cat.setLastUpdated(new Date()));
        categoriesCollection.insertMany(
            uniqueCategories.stream().map(Categories::toDocument).distinct()
                .collect(Collectors.toList()));
      }
      return true;
    } catch (Exception e) {
//      System.out.println(
//          "Fail when trying to add categories: " + categories + " since " + e.getMessage());
      return false;
    }
  }

  public static boolean removeCategory(Categories categories) {
    return MongoDB.getInstance().deleteFromCollection("categories", "name", categories.getName());
  }

  public static long countCategories() {
    return MongoDB.getInstance().countDocuments("categories");
  }

  public static List<Categories> getCategories(int start, int length) {
    try {
      return MongoDB.getInstance().findAllObject("categories", Filters.empty(), start, length)
          .stream().map(Categories::new).toList();
    } catch (Exception e) {
      return null;
    }
  }

  public static List<Book> getBookOfCategory(Categories categories, int start, int length) {
    try {
      return MongoDB.getInstance().findAllObject("books",
          Filters.regex("categories", StringUtil.escapeString(categories.getName().toLowerCase()),
              "i")).stream().map(BookController::getBookFromDocument).toList();
    } catch (Exception e) {
      return null;
    }
  }

  public static long countBookOfCategory(Categories categories) {
    return MongoDB.getInstance().countDocuments("books",
        Filters.regex("categories", StringUtil.escapeString(categories.getName().toLowerCase()),
            "i"));
  }
}
