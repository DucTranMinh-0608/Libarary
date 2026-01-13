package com.app.librarymanager.models;

import java.util.Date;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

@Data
public class BookCopies {

  private ObjectId _id;
  private String bookId;
  private int copies;
  private Date lastUpdated;

  public BookCopies() {
    this._id = null;
    this.bookId = null;
    this.copies = 0;
    this.lastUpdated = null;
  }

  public BookCopies(String bookId) {
    this._id = null;
    this.bookId = bookId;
    this.copies = 0;
    this.lastUpdated = null;
  }

  public BookCopies(String bookId, int copies) {
    this.bookId = bookId;
    this.copies = copies;
  }

  public BookCopies(Document document) {
    this._id = document.getObjectId("_id");
    this.bookId = document.getString("bookId");
    this.copies = document.getInteger("copies");
    this.lastUpdated = document.getDate("lastUpdated");
  }
}
