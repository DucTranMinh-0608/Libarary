package com.app.librarymanager.models;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.Document;
import org.bson.types.ObjectId;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookRating extends BookUser {

  private double rate;

  public BookRating() {
    super();
    rate = -1;
  }

  public BookRating(String _id, String bookId, String userId, double rate) {
    super(_id, userId, bookId);
    this.rate = rate;
  }

  public BookRating(ObjectId _id, String bookId, String userId, double rate) {
    super(_id, userId, bookId);
    this.rate = rate;
  }

  public BookRating(String bookId, String userId, double rate) {
    super(userId, bookId);
    this.rate = rate;
  }

  public BookRating(Book book, User user, double rate) {
    super(user.getUid(), book.getId());
    this.rate = rate;
  }

  public BookRating(Document document) {
    super(document);
    this.rate = document.getDouble("rate");
  }
}