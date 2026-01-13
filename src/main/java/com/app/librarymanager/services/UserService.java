package com.app.librarymanager.services;

import com.app.librarymanager.controllers.AuthController;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.UploadFileUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class UserService {

  public static final String initialDirectory = System.getProperty("user.home");

  private static UserService instance;

  private UserService() {
  }

  public synchronized static UserService getInstance() {
    if (instance == null) {
      instance = new UserService();
    }

    return instance;
  }

  public JSONObject updateUserProfile(User user) {
    try {
      String newPhotoUrl = "";
      if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
        if (user.getPhotoUrl().startsWith("http")) {
          newPhotoUrl = user.getPhotoUrl();
        } else {
          try {
            JSONObject resp = UploadFileUtil.uploadImage(user.getPhotoUrl(), user.getUid(), 96);
            if (resp.getBoolean("success")) {
              newPhotoUrl = resp.getString("longURL");
            } else {
              System.err.println("Avatar upload failed: " + resp.optString("message"));
            }
          } catch (Exception e) {
            System.err.println("Error uploading avatar: " + e.getMessage());
          }
        }
      }
      UpdateRequest request = new UpdateRequest(user.getUid());
      if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
        request.setPhoneNumber(user.getPhoneNumber());
      }
      if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
        request.setDisplayName(user.getDisplayName());
      }
      if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
        request.setPhotoUrl(newPhotoUrl);
      }
      UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
      Map<String, Object> userClaims = new HashMap<>();
      userClaims.put("birthday", user.getBirthday());
      userClaims.put("admin", user.isAdmin());
      long now = System.currentTimeMillis();
      userClaims.put("lastModifiedAt", now);
      FirebaseAuth.getInstance().setCustomUserClaims(user.getUid(), userClaims);
      user.setLastModifiedDate(String.valueOf(now));
      AuthController.getInstance().getNewUserClaims();
      return new JSONObject().put("success", true)
          .put("message", "User profile updated successfully.");
    } catch (Exception e) {
      try {
        if (e.getMessage() != null && e.getMessage().contains("{")) {
          String responseBody = e.getMessage().substring(e.getMessage().indexOf("{"));
          JSONObject responseJson = new JSONObject(responseBody);
          String errorMessage = responseJson.getJSONObject("error").getString("message");
          return new JSONObject().put("success", false)
              .put("message", errorMessage);
        } else {
          return new JSONObject().put("success", false)
              .put("message", e.getMessage());
        }
      } catch (Exception parseException) {
        return new JSONObject().put("success", false)
            .put("message", e.getMessage());
      }
    }
  }

  public JSONObject updateUserPassword(String uid, String email, String oldPassword,
      String newPassword) {
    try {
      boolean isOldPasswordValid = FirebaseAuthentication.verifyPassword(email, oldPassword);
      if (!isOldPasswordValid) {
        return new JSONObject().put("success", false)
            .put("message", "Old password is incorrect.");
      }
      UpdateRequest request = new UpdateRequest(uid)
          .setPassword(newPassword);
      UserRecord userRecord = FirebaseAuth.getInstance().updateUser(request);
      Map<String, Object> claims = new HashMap<>(userRecord.getCustomClaims());
      claims.put("lastModifiedAt", System.currentTimeMillis());
      FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
      return new JSONObject().put("success", true)
          .put("message", "User password updated successfully.");
    } catch (Exception e) {
      return new JSONObject().put("success", false)
          .put("message", e.getMessage());
    }
  }
}
