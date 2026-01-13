package com.app.librarymanager.utils;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class AvatarUtil {

  private Map<String, String> settings = new HashMap<>();

  public AvatarUtil setImageSize(int size) {
    settings.put("size", String.valueOf(size));
    return this;
  }

  public AvatarUtil setBold(boolean bold) {
    settings.put("bold", String.valueOf(bold));
    return this;
  }

  public AvatarUtil setBackground(String background) {
    settings.put("background", background);
    return this;
  }

  public AvatarUtil setFormat(String format) {
    settings.put("format", format);
    return this;
  }

  public AvatarUtil setRounded(boolean rounded) {
    settings.put("rounded", String.valueOf(rounded));
    return this;
  }

  public AvatarUtil setFontSize(int fontSize) {
    settings.put("font-size", String.valueOf(fontSize));
    return this;
  }

  public AvatarUtil setFontColor(String fontColor) {
    settings.put("color", fontColor);
    return this;
  }

  public AvatarUtil setInitLength(int length) {
    settings.put("length", String.valueOf(length));
    return this;
  }

  public AvatarUtil() {
    settings.put("size", "128");
    settings.put("bold", "true");
    settings.put("background", "random");
  }

  public String getAvatarUrl(String displayName) {
    if (displayName == null || displayName.isEmpty()) {
      displayName = "Anonymous";
    }
    StringBuilder url = new StringBuilder("https://ui-avatars.com/api/?name=" + displayName);
    settings.forEach((key, value) -> url.append("&").append(key).append("=").append(value));
    return url.toString();
  }

}