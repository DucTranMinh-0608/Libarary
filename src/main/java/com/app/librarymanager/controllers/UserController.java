package com.app.librarymanager.controllers;

import com.app.librarymanager.models.User;
import com.app.librarymanager.services.FirebaseAuthentication;
import com.app.librarymanager.utils.UploadFileUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserIdentifier;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserController {

  private static void checkPermission() {
    User currentUser = AuthController.getInstance().getCurrentUser();
    if (currentUser == null || !currentUser.isAdmin()) {
      throw new SecurityException("Access denied! You don't have permission to make the request.");
    }
  }

  public static int countTotalUser() {
    try {
      checkPermission();
      return FirebaseAuthentication.countTotalUser();
    } catch (Exception e) {
      return 0;
    }
  }

  private static User jsonToUser(JSONObject jsonUser) {
    JSONObject customClaims = jsonUser.optJSONObject("customClaims");
    if (customClaims == null) {
      customClaims = new JSONObject();
    }
    long lastModifiedAt = customClaims.optLong("lastModifiedAt", 0L);
    // Fallback to creationTimestamp if lastModifiedAt is not set
    if (lastModifiedAt == 0L) {
      lastModifiedAt = jsonUser.getJSONObject("userMetadata").optLong("creationTimestamp", 0L);
    }

    return new User(jsonUser.getString("uid"), jsonUser.getString("email"),
        jsonUser.optString("password", ""), jsonUser.optString("displayName", ""),
        customClaims.optString("birthday", ""),
        jsonUser.optString("phoneNumber", ""), jsonUser.optString("photoUrl", ""),
        String.valueOf(
            jsonUser.getJSONObject("userMetadata").optLong("creationTimestamp", 0L)),
        String.valueOf(lastModifiedAt),
        String.valueOf(
            jsonUser.getJSONObject("userMetadata").optLong("lastSignInTimestamp", 0L)),
        jsonUser.optString("providerId", ""),
        customClaims.optBoolean("admin", false),
        jsonUser.getBoolean("emailVerified"), jsonUser.getBoolean("disabled"));
  }

  public static User getUser(String userId) {
    try {
      checkPermission();
      JSONObject jsonUser = new JSONObject(FirebaseAuth.getInstance().getUser(userId));
      return jsonToUser(jsonUser);
    } catch (Exception e) {
      return null;
    }
  }

  public static JSONObject createUser(User user) {
    try {
      checkPermission();
      return new JSONObject().put("success",
              (FirebaseAuthentication.createAccountWithEmailAndPassword(user)).getBoolean("success"))
          .put("message", "User created successfully.");
    } catch (Exception e) {
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static JSONObject updateUser(User user) {
    //  System.out.println(user);
    try {
      checkPermission();
      String newPhotoUrl = "";
      try {
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty() && !user.getPhotoUrl().startsWith("http")) {
          JSONObject resp = UploadFileUtil.uploadImage(user.getPhotoUrl(), user.getUid(), 96);
          if (resp.getBoolean("success")) {
            newPhotoUrl = resp.getString("longURL");
          } else {
            System.err.println("Avatar upload failed: " + resp.optString("message"));
          }
        }
      } catch (Exception e) {
        System.err.println("Error uploading avatar: " + e.getMessage());
      }
      if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
        user.setPhotoUrl(newPhotoUrl);
      }
      UpdateRequest userUpdate = getUpdateRequest(user);
      UserRecord updatedUser = FirebaseAuth.getInstance().updateUser(userUpdate);
      Map<String, Object> claims = new HashMap<>();
      claims.put("admin", user.isAdmin());
      claims.put("birthday", user.getBirthday());
      long now = System.currentTimeMillis();
      claims.put("lastModifiedAt", now);
      FirebaseAuth.getInstance().setCustomUserClaims(user.getUid(), claims);
      user.setLastModifiedDate(String.valueOf(now));
      return new JSONObject().put("success", true).put("data", new JSONObject(updatedUser))
          .put("message", "User updated successfully.");
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      JSONObject errorResponse = new JSONObject();
      try {
        String responseBody = e.getMessage().substring(e.getMessage().indexOf("{"));
        //  System.out.println(responseBody);
        if (responseBody.contains("{")) {
          responseBody = responseBody.substring(responseBody.indexOf("{"));
          //  System.out.println(responseBody);
          JSONObject responseJson = new JSONObject(responseBody);
          String errorMessage = responseJson.getJSONObject("error").getString("message");
          errorResponse.put("message", errorMessage);
        } else {
          errorResponse.put("message", e.getMessage());
        }
      } catch (Exception parseException) {
        errorResponse.put("message", e.getMessage());
      }
      return new JSONObject().put("success", false)
          .put("message", errorResponse.getString("message"));
    }
  }

  @NotNull
  private static UpdateRequest getUpdateRequest(User user) {
    UpdateRequest userUpdate = new UpdateRequest(user.getUid());
    userUpdate.setEmailVerified(user.isEmailVerified());
    userUpdate.setPhoneNumber(
        user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() ? user.getPhoneNumber()
            : null);
    if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
      userUpdate.setDisplayName(user.getDisplayName());
    }
    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
      userUpdate.setPhotoUrl(user.getPhotoUrl());
    }
    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
      userUpdate.setPassword(user.getPassword());
    }
    userUpdate.setDisabled(user.isDisabled());
    return userUpdate;
  }

  public static JSONObject deleteUser(User user) {
    try {
      checkPermission();
      BookLoanController.returnAllBookOf(user.getUid());
      BookLoanController.removeAllLoanOf(user.getUid());
      CommentController.removeAllCommentOf(user.getUid());
      BookRatingController.removeAllRatingOf(user.getUid());
      FavoriteController.removeAllFavoriteOf(user.getUid());
      FirebaseAuth.getInstance().deleteUser(user.getUid());
      return new JSONObject().put("success", true).put("message", "User deleted successfully.");
    } catch (Exception e) {
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static List<User> listUsers() {
    try {
      checkPermission();
      JSONArray jsonUsers = new JSONArray(FirebaseAuth.getInstance().listUsers(null).getValues());
      List<User> users = new ArrayList<>();
      for (int i = 0; i < jsonUsers.length(); i++) {
        JSONObject userJson = jsonUsers.getJSONObject(i);
        User user = jsonToUser(userJson);
        users.add(user);
      }
      return users;
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      return null;
    }
  }

  public static List<User> listUsers(List<String> ids) {
    try {
      List<User> users = new ArrayList<>();
      for (String id : ids) {
        if (id == null || id.isEmpty()) {
          continue;
        }

        UserRecord userRecord = FirebaseAuth.getInstance().getUser(id);
        if (userRecord == null) {
          continue;
        }

        User user = jsonToUser(new JSONObject(userRecord));
        users.add(user);
      }
      return users;
    } catch (Exception e) {
      //  System.err.println("aaa" + e.getMessage());
      return null;
    }
  }
}
