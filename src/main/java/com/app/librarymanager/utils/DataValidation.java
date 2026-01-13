package com.app.librarymanager.utils;

import org.json.JSONObject;

public class DataValidation {

  // (?:) means non-capturing group.
  /**
   * This regex accepts space (\\s) or (|) '-' (\\-) character.
   */
  private static final String SEP = "(?:\\-|\\s)";
  /**
   * This regex accepts a group contains 1 to 5 ({1,5}) digits (\\d).
   */
  private static final String GROUP = "(\\d{1,5})";
  private static final String PUBLISHER = "(\\d{1,7})";
  private static final String TITLE = "(\\d{1,6})";

  /**
   * ISBN-10 consists of 4 groups of numbers separated by either dashes (-) or spaces.  The first
   * group is 1-5 characters, second 1-7, third 1-6, and fourth is 1 digit or an X.
   * <p>
   * ^ indicates the start of the string, $ indicates the end of the string. It means that our regex
   * will check all characters of the string. For the case in which our string doesn't contain any
   * separator, the string would be checked by (?:(\d{9}[0-9X]): start with 9 digits, and end with a
   * digit or 'X'. Otherwise, it will be separated into groups, and each group will be checked by
   * the regex we defined before.
   */
  private static final String ISBN10_REGEX =
      "^(?:(\\d{9}[0-9X])|(?:" + GROUP + SEP + PUBLISHER + SEP + TITLE + SEP + "([0-9X])))$";

  /**
   * ISBN-13 consists of 5 groups of numbers separated by either dashes (-) or spaces.  The first
   * group is 978 or 979, the second group is 1-5 characters, third 1-7, fourth 1-6, and fifth is 1
   * digit. For the case in which our string doesn't contain any separator, the string would be
   * checked by (978|979)(?:(\d{10}): start with 978 or 979, and end with 10 digits. Otherwise, it
   * will be separated into groups, and each group will be checked by the regex we defined before.
   */
  private static final String ISBN13_REGEX =
      "^(978|979)(?:(\\d{10})|(?:" + SEP + GROUP + SEP + PUBLISHER + SEP + TITLE + SEP
          + "([0-9])))$";

  public static boolean validISBN(String iSBN) {
    if (!iSBN.matches(ISBN10_REGEX) && !iSBN.matches(ISBN13_REGEX)) {
      return false;
    }
    // Remove all hyphen and space ([- ]), then check the last digit using given formula.
    String trimmedISBN = iSBN.replaceAll("[- ]", "");
    return getExpectedLastDigit(trimmedISBN) == trimmedISBN.charAt(trimmedISBN.length() - 1);
  }

  private static char getExpectedLastDigit(String trimmedISBN) {
    char expectedLastDigit;
    if (trimmedISBN.length() == 13) {
      int valueOfLastDigit = 0;
      for (int i = 0; i + 1 < trimmedISBN.length(); i++) {
        valueOfLastDigit += (i % 2 * 2 + 1) * (trimmedISBN.charAt(i) - '0');
      }
      valueOfLastDigit = (10 - valueOfLastDigit % 10) % 10;
      expectedLastDigit = (char) (valueOfLastDigit + '0');
    } else { // trimmedISBN.length() == 10
      int valueOfLastDigit = 0;
      for (int i = 0; i + 1 < trimmedISBN.length(); i++) {
        valueOfLastDigit += (trimmedISBN.length() - i) * (trimmedISBN.charAt(i) - '0');
      }
      valueOfLastDigit = (11 - valueOfLastDigit % 11) % 11;
      if (valueOfLastDigit == 10) {
        expectedLastDigit = 'X';
      } else {
        expectedLastDigit = (char) (valueOfLastDigit + '0');
      }
    }
    return expectedLastDigit;
  }

  public static String checkInt(String name, String s) {
    try {
      int x = Integer.parseInt(s);
      if (x < 0) {
        throw new Exception("is a negative number");
      }
      return "";
    } catch (Exception e) {
      return "Fail when trying to read " + name + ": " + e.getMessage() + ".";
    }
  }

  public static String checkDouble(String name, String s) {
    try {
      double x = Double.parseDouble(s);
      if (x < 0) {
        throw new Exception("is a negative number");
      }
      return "";
    } catch (Exception e) {
      return "Fail when trying to read " + name + ": " + e.getMessage() + ".";
    }
  }

  public static boolean validEmail(String email) {
    JSONObject js = Fetcher.get("https://verifyemail.vercel.app/api/" + email);
    assert js != null;
    return !js.optString("message", "").equals("not valid email id.");
  }

  public static void main(String[] args) {
  }
}