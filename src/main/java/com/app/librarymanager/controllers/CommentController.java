package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.Comment;
import com.app.librarymanager.models.User;
import com.app.librarymanager.services.Firebase;
import com.app.librarymanager.services.MongoDB;
import com.app.librarymanager.utils.StringUtil;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.bson.Document;

@Data
public class CommentController {

  public static Document addComment(Comment comment) {
    return MongoDB.getInstance().addToCollection("comment", MongoDB.objectToMap(comment));
  }

  public static boolean removeComment(Comment comment) {
    return MongoDB.getInstance().deleteFromCollection("comment", "_id", comment.get_id());
  }

  public static boolean removeAllComment(String bookId) {
    return MongoDB.getInstance().deleteAll("comment", Filters.eq("bookId", bookId));
  }

  public static boolean removeAllCommentOf(String userId) {
    return MongoDB.getInstance().deleteAll("comment", Filters.eq("userId", userId));
  }

  public static Document editComment(Comment comment) {
    return MongoDB.getInstance()
        .updateData("comment", "_id", comment.get_id(), MongoDB.objectToMap(comment));
  }

  @Data
  public static class ReturnUserComment {

    private String userDisplayName;
    private String userEmail;
    private String userPhotoUrl;
    private String content;

    public ReturnUserComment(String userDisplayName, String userEmail, String userPhotoUrl,
        String content) {
      this.userDisplayName = userDisplayName;
      this.userEmail = userEmail;
      this.userPhotoUrl = userPhotoUrl;
      this.content = content;
    }
  }

  public static List<ReturnUserComment> getAllCommentOfBook(String bookId) {
    try {
      List<Document> relatedComments = MongoDB.getInstance()
          .findAllObject("comment", "bookId", StringUtil.escapeString(bookId));
      Map<String, User> idToUser = UserController.listUsers(
              relatedComments.stream().map(doc -> doc.getString("userId")).toList()).stream()
          .collect(Collectors.toMap(User::getUid, doc -> doc, (existing, replacement) -> existing));
      return relatedComments.stream().map(doc -> {
        User currentUser = idToUser.get(doc.getString("userId"));
        return new ReturnUserComment(currentUser.getDisplayName(), currentUser.getEmail(),
            currentUser.getPhotoUrl(), doc.getString("content"));
      }).toList();
    } catch (Exception e) {
      //  System.out.println(e.getMessage());
      return null;
    }
  }

  public static List<Comment> getAllCommentOfUser(String userId) {
    return MongoDB.getInstance().findAllObject("comment", "userId", userId).stream()
        .map(Comment::new).toList();
  }

  @Data
  public static class ReturnComment {

    private String bookTitle;
    private String bookThumbnail;
    private int numComment;

    public ReturnComment(String bookTitle, String bookThumbnail, int numComment) {
      this.bookTitle = bookTitle;
      this.bookThumbnail = bookThumbnail;
      this.numComment = numComment;
    }
  }

  public static List<ReturnComment> getMostCommentedBooks(int start, int length) {
    try {
      List<Document> documents = MongoDB.getInstance().getAggregate("comment", List.of(
          new Document("$group",
              new Document("_id", "$bookId").append("count", new Document("$sum", 1))),
          new Document("$sort", new Document("count", -1)), new Document("$skip", start),
          new Document("$limit", length)));
      Map<String, Document> bookDocs = BookController.findBookByListID(
              documents.stream().map(doc -> doc.getString("_id")).toList()).stream()
          .collect(Collectors.toMap(doc -> doc.getString("id"), doc -> doc));
      return documents.stream().map(doc -> {
        Document bookDoc = bookDocs.get(doc.getString("_id"));
        return new ReturnComment(bookDoc.getString("id"), bookDoc.getString("thumbnail"),
            doc.getInteger("count"));
      }).toList();
    } catch (Exception e) {
      //  System.out.println(e.getMessage());
      return null;
    }
  }
}
