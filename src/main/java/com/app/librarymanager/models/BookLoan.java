package com.app.librarymanager.models;

import com.app.librarymanager.utils.DateUtil;
import java.time.LocalDate;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.bson.Document;
import org.bson.types.ObjectId;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookLoan extends BookUser {

  public enum Mode {
    OFFLINE,
    ONLINE
  }

  private Date borrowDate;
  private Date dueDate;
  private boolean valid;
  private Mode type;
  private int numCopies; // = 0 iff online

  public BookLoan() {
    super();
    borrowDate = null;
    dueDate = null;
    valid = false;
    type = Mode.ONLINE;
    numCopies = 0;
  }

  public BookLoan(String userId, String bookId) {
    super(userId, bookId);
    this.borrowDate = null;
    this.dueDate = null;
    this.valid = false;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  public BookLoan(String userId, String bookId, Date borrowDate, Date dueDate) {
    super(userId, bookId);
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  public BookLoan(String userId, String bookId, LocalDate borrowDate, LocalDate dueDate) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(borrowDate);
    this.dueDate = DateUtil.localDateToDate(dueDate);
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  /**
   * Constructor with date like "dd/mm/yyyy"
   *
   * @param userId     user id
   * @param bookId     book id
   * @param borrowDate borrow date
   * @param dueDate    excepted return date
   */
  public BookLoan(String userId, String bookId, String borrowDate, String dueDate) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(DateUtil.parse(borrowDate));
    this.dueDate = DateUtil.localDateToDate(DateUtil.parse(dueDate));
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  public BookLoan(String userId, String bookId, Date borrowDate, Date dueDate, int numCopies) {
    super(userId, bookId);
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = true;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
  }

  public BookLoan(String userId, String bookId, LocalDate borrowDate, LocalDate dueDate,
      int numCopies) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(borrowDate);
    this.dueDate = DateUtil.localDateToDate(dueDate);
    this.valid = true;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
  }

  /**
   * Constructor with date like "dd/mm/yyyy"
   *
   * @param userId     user id
   * @param bookId     book id
   * @param borrowDate borrow date
   * @param dueDate    excepted return date
   * @param numCopies  number of copies borrowed
   */
  public BookLoan(String userId, String bookId, String borrowDate, String dueDate, int numCopies) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(DateUtil.parse(borrowDate));
    this.dueDate = DateUtil.localDateToDate(DateUtil.parse(dueDate));
    this.valid = true;
    this.numCopies = 0;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
  }

  public BookLoan(ObjectId id, String userId, String bookId, Date borrowDate, Date dueDate,
      boolean valid, Mode type, int numCopies) {
    super(id, userId, bookId);
    this.setLastUpdated(new Date());
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = valid;
    this.type = type;
    this.numCopies = numCopies;
  }

  public Document toDocument() {
    Document document = super.toDocument();
    document.put("borrowDate", borrowDate);
    document.put("dueDate", dueDate);
    document.put("valid", valid);
    document.put("type", type.toString());
    document.put("numCopies", numCopies);
    return document;
  }

  public BookLoan(Document document) {
    super(document);
    this.borrowDate = document.getDate("borrowDate");
    this.dueDate = document.getDate("dueDate");
    this.valid = document.getBoolean("valid");
    this.type = document.getString("type").equals("OFFLINE") ? Mode.OFFLINE : Mode.ONLINE;
    this.numCopies = document.getInteger("numCopies");
  }
}
