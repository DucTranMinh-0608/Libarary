package com.app.librarymanager.utils;

import java.util.regex.Pattern;
import org.apache.commons.lang3.text.WordUtils;

public class StringUtil {

  public static String escapeString(String s) {
    return Pattern.quote(s);
  }

  public static String toCapitalize(String s) {
    return WordUtils.capitalizeFully(s.toLowerCase());
  }
}
