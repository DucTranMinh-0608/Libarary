package com.app.librarymanager.models;

import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.Document;
import org.bson.types.ObjectId;

@EqualsAndHashCode(callSuper = true)
@Data
public class Comment extends BookUser {

  private String content;

  public Comment() {
    super();
    this.content = null;
  }

  public Comment(String _id, String userId, String bookId, String content) {
    super(_id, userId, bookId);
    this.content = content;
  }

  public Comment(ObjectId _id, String userId, String bookId, String content) {
    super(_id, userId, bookId);
    this.content = content;
  }

  public Comment(String userId, String bookId, String content) {
    super(userId, bookId);
    this.content = content;
  }

  public Comment(User user, Book book, String content) {
    super(user.getUid(), book.getId());
    this.content = content;
  }

  public Comment(ObjectId _id, String bookId, String userId, Timestamp lastUpdated,
      String content) {
    super(_id, bookId, userId, lastUpdated);
    this.content = content;
  }

  public Comment(Document document) {
    super(document);
    this.content = document.getString("content");
  }
}
