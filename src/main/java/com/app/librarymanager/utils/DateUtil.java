package com.app.librarymanager.utils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;
import org.bson.types.ObjectId;

public class DateUtil {

  public static final class DateFormat {

    public static final String DD_MM_YYYY = "dd/MM/yyyy";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DD_MM_YYYY_HH_MM_SS = "dd/MM/yyyy HH:mm:ss";
  }

  public static String convertToStringFrom(String objectId) {
    return dateToString(convertToDateFrom(objectId));
  }

  public static Date convertToDateFrom(String objectId) {
    return new Date(convertToTimestampFrom(objectId));
  }

  public static long convertToTimestampFrom(String objectId) {
    return Long.parseLong(objectId.substring(0, 8), 16) * 1000;
  }

  public static String dateToString(Date date) {
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
    return formatter.format(date);
  }

  public static LocalDate parse(String date) {
    try {
      String[] parts = date.trim().split("[/-]");
      int year, month = 1, day = 1;
      if (parts.length == 3) {
        if (parts[2].length() > 2) {
          year = Integer.parseInt(parts[2]);
          day = Integer.parseInt(parts[0]);
        } else {
          year = Integer.parseInt(parts[0]);
          day = Integer.parseInt(parts[2]);
        }
        month = Integer.parseInt(parts[1]);
      } else if (parts.length == 2) {
        if (parts[1].length() > 2) {
          year = Integer.parseInt(parts[1]);
          month = Integer.parseInt(parts[0]);
        } else {
          year = Integer.parseInt(parts[0]);
          month = Integer.parseInt(parts[1]);
        }
      } else {
        year = Integer.parseInt(parts[0]);
      }
      return LocalDate.of(year, month, day);
    } catch (Exception e) {
      return null;
    }
  }

  public static LocalDate parse(String date, String format) {
    return LocalDate.parse(date, DateTimeFormatter.ofPattern(format));
  }

  public static String format(LocalDate date) {
    return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear();
  }

  public static String ymdToDmy(String date) {
    try {
      String[] parts = date.split("-");
      return parts[2] + "/" + parts[1] + "/" + parts[0];

    } catch (ArrayIndexOutOfBoundsException e) {
      try {
        String[] parts = date.split("-");
        return parts[1] + "/" + parts[0];
      } catch (ArrayIndexOutOfBoundsException e1) {
        return date;
      }
    }
  }

  public static String format(LocalDate date, String format) {
    return date.format(DateTimeFormatter.ofPattern(format));
  }

  public static Date localDateToDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public static LocalDate dateToLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  public static boolean isValid(String date) {
    try {
      parse(date);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static Date addDays(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).plusDays(i));
  }

  public static Date addMonths(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).plusMonths(i));
  }

  public static Date addYears(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).plusYears(i));
  }

  public static Date minusDays(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).minusDays(i));
  }

  public static Date minusMonths(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).minusMonths(i));
  }

  public static Date minusYears(Date borrowDate, int i) {
    return localDateToDate(dateToLocalDate(borrowDate).minusYears(i));
  }

  public static boolean isBefore(String date1, String date2) {
    return parse(date1).isBefore(parse(date2));
  }

  public static boolean isAfter(String date1, String date2) {
    return parse(date1).isAfter(parse(date2));
  }

  public static boolean isEqual(String date1, String date2) {
    return parse(date1).isEqual(parse(date2));
  }

  public static boolean isBeforeOrEqual(String date1, String date2) {
    return isBefore(date1, date2) || isEqual(date1, date2);
  }

  public static boolean isAfterOrEqual(String date1, String date2) {
    return isAfter(date1, date2) || isEqual(date1, date2);
  }

  public static boolean isBetween(String date, String start, String end) {
    return isAfterOrEqual(date, start) && isBeforeOrEqual(date, end);
  }

}
