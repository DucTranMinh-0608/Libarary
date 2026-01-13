package com.app.librarymanager.models;

import java.util.Date;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

@Data
public class BookUser {

  private ObjectId _id;
  private String bookId;
  private String userId;
  private Date lastUpdated;

  public BookUser() {
    _id = null;
    bookId = null;
    userId = null;
    lastUpdated = null;
  }

  public BookUser(String _id, String userId, String bookId) {
    this._id = new ObjectId(_id);
    this.userId = userId;
    this.bookId = bookId;
    this.lastUpdated = null;
  }

  public BookUser(ObjectId _id, String userId, String bookId) {
    this._id = _id;
    this.userId = userId;
    this.bookId = bookId;
    this.lastUpdated = null;
  }

  public BookUser(String userId, String bookId) {
    this._id = null;
    this.userId = userId;
    this.bookId = bookId;
    this.lastUpdated = null;
  }

  public BookUser(User user, Book book) {
    this._id = null;
    this.userId = user.getUid();
    this.bookId = book.getId();
    this.lastUpdated = null;
  }

  public BookUser(ObjectId _id, String bookId, String userId, Date lastUpdated) {
    this._id = _id;
    this.bookId = bookId;
    this.userId = userId;
    this.lastUpdated = lastUpdated;
  }

  public BookUser(Document document) {
    this._id = document.getObjectId("_id");
    this.bookId = document.getString("bookId");
    this.userId = document.getString("userId");
    this.lastUpdated = document.getDate("lastUpdated");
  }

  public Document toDocument() {
    Document document = new Document();
    document.append("_id", _id);
    document.append("bookId", bookId);
    document.append("userId", userId);
    document.append("lastUpdated", lastUpdated);
    return document;
  }

//  public BookUser(JSONObject jsonObject) {
//    this._id = new ObjectId(jsonObject.optJSONObject("_id").optString("$oid"));
//    this.bookId = jsonObject.optString("bookId");
//    this.userId = jsonObject.optString("userId");
////    System.out.println(jsonObject.optJSONObject("lastUpdated").optString("$date"));
//    this.lastUpdated = Date.valueOf(
//        jsonObject.optJSONObject("lastUpdated").optString("$date"));
//  }
}
