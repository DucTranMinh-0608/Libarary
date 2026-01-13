package com.app.librarymanager.models;

import com.app.librarymanager.utils.StringUtil;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

@Data
public class Categories {

  private ObjectId _id;
  private String name;
  private Date lastUpdated;

  public Categories() {
    _id = null;
    name = null;
    lastUpdated = null;
  }

  public Categories(String name) {
    this._id = null;
    this.name = StringUtil.toCapitalize(name).trim();
    this.lastUpdated = null;
  }

  public Categories(ObjectId _id, String name, Date lastUpdated) {
    this._id = _id;
    this.name = StringUtil.toCapitalize(name).trim();
    this.lastUpdated = lastUpdated;
  }

  public Categories(Document document) {
    this._id = document.getObjectId("_id");
    this.name = document.getString("name");
    this.lastUpdated = document.getDate("lastUpdated");
  }

  public Document toDocument() {
    return new Document(
        Map.of("name", StringUtil.toCapitalize(name).trim(), "lastUpdated",
            new Timestamp(System.currentTimeMillis())));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return name.equals(((Categories) o).getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
