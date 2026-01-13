package com.app.librarymanager.models;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

@Data
public class User {

  private String uid;
  private String email;
  private String password;
  private String displayName;
  private String birthday;
  private String phoneNumber;
  private String photoUrl;
  private String createdAt;
  private String lastModifiedDate;
  private String lastLoginAt;
  private String providerId;
  private boolean admin;
  private boolean emailVerified;
  private boolean disabled;
  private Image avatar;


  public User(String email, String password, String displayName, String birthday,
      String phoneNumber, String photoUrl, String createdAt, String lastModifiedDate,
      boolean admin) {
    this.email = email;
    this.password = password;
    this.displayName = displayName;
    this.birthday = birthday;
    this.phoneNumber = phoneNumber;
    this.photoUrl = photoUrl;
    this.createdAt = createdAt;
    this.lastModifiedDate = lastModifiedDate;
    this.admin = admin;
  }

  public User(String uid, String email, String password, String displayName, String birthday,
      String phoneNumber, String photoUrl, String createdAt, String lastModifiedDate,
      String lastLoginAt, String providerId,
      boolean admin, boolean emailVerified, boolean disabled) {
    this.uid = uid;
    this.email = email;
    this.password = password;
    this.displayName = displayName;
    this.birthday = birthday;
    this.phoneNumber = phoneNumber;
    this.photoUrl = photoUrl;
    this.createdAt = createdAt;
    this.lastModifiedDate = lastModifiedDate;
    this.admin = admin;
    this.emailVerified = emailVerified;
    this.disabled = disabled;
    this.lastLoginAt = lastLoginAt;
    this.providerId = providerId;
    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  public User() {
    this.admin = false;
    this.emailVerified = false;
    this.disabled = false;
  }

  public User(String email, String password, String fullName, String birthday, String photoUrl,
      boolean admin) {
    this.email = email;
    this.password = password;
    this.displayName = fullName;
    this.birthday = birthday;
    this.admin = admin;
    this.photoUrl = photoUrl;

    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  public User(String uid) {
    this.uid = uid;
  }

  public User(JSONObject data) {
    this.uid = data.optString("uid", data.optString("localId"));
    this.email = data.getString("email");
    this.displayName = data.optString("displayName", "");
    this.birthday = data.optString("birthday");
    this.phoneNumber = data.optString("phoneNumber");
    this.photoUrl = data.optString("photoUrl");
    this.createdAt = data.optString("createdAt");
    this.lastModifiedDate = data.optString("lastModifiedDate", "");
    this.lastLoginAt = data.optString("lastLoginAt", "");
    this.providerId = data.optString("providerId", "");
    this.admin = data.optBoolean("admin", false);
    this.emailVerified = data.optBoolean("emailVerified", false);
    this.disabled = data.optBoolean("disabled", false);

    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  private void setAvatar(String photoUrl) {
    Task<Image> loadImageTask = getImageTask(photoUrl);
    new Thread(loadImageTask).start();
  }

  @NotNull
  private Task<Image> getImageTask(String photoUrl) {
    Task<Image> loadImageTask = new Task<Image>() {
      @Override
      protected Image call() {
        return new Image(photoUrl, true);
      }
    };
    loadImageTask.setOnRunning(event -> {
    });
    loadImageTask.setOnSucceeded(event -> {
      Image image = loadImageTask.getValue();
      if (image.getException() == null) {
        avatar = image;
      } else {
        //  System.err.println("Failed to load image from photoUrl: " + photoUrl);
      }
    });
    loadImageTask.setOnFailed(event -> {
//      System.err.println(
//          "Exception while loading image from photoUrl: " + loadImageTask.getException()
//              .getMessage());
    });
    return loadImageTask;
  }

}
