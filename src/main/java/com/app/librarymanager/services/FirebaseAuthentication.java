package com.app.librarymanager.services;

import com.app.librarymanager.controllers.AuthController;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.Fetcher;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class FirebaseAuthentication {

  private static final Dotenv dotenv = Dotenv.load();
  private static final String LOGIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
  private static final String REGISTER_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=";

  public static JSONObject loginWithEmailAndPassword(String email, String password) {
    String url = LOGIN_URL + Firebase.getApiKey();
    String body =
        "{\n" + "  \"email\": \"" + email + "\",\n" + "  \"password\": \"" + password + "\",\n"
            + "  \"returnSecureToken\": true\n" + "}";
    JSONObject response = Fetcher.post(url, body);
    if (response == null) {
      return new JSONObject(Map.of("success", false, "message", "Login Failed"));
    }
    if (response.has("error")) {
      JSONObject error = response.getJSONObject("error");
      if (error.has("message")) {
        return new JSONObject(Map.of("success", false, "message", error.getString("message")));
      }
    } else {
      return new JSONObject(Map.of("success", true, "data", response));
    }
    return new JSONObject(Map.of("success", false, "message", "Login Failed"));
  }

  public static JSONObject createAccountWithEmailAndPassword(User user) {
    String url = REGISTER_URL + Firebase.getApiKey();
    String body = String.format(
        "{\n  \"email\": \"%s\",\n  \"password\": \"%s\",\n  \"returnSecureToken\": true,\n  \"displayName\": \"%s\"}",
        user.getEmail(), user.getPassword(), user.getDisplayName());
    JSONObject response = Fetcher.post(url, body);
    if (response == null) {
      return new JSONObject().put("success", false).put("message", "Registration Failed");
    }
    if (response.has("error")) {
      JSONObject error = response.getJSONObject("error");
      if (error.has("message")) {
        return new JSONObject().put("success", false).put("message", error.getString("message"));
      }
    } else {
      try {
        String localId = response.getString("localId");
        Map<String, Object> claims = new HashMap<>();
        claims.put("admin", user.isAdmin());
        claims.put("birthday", user.getBirthday());
        long now = System.currentTimeMillis();
        claims.put("lastModifiedAt", now);
        FirebaseAuth.getInstance().setCustomUserClaims(localId, claims);
        user.setLastModifiedDate(String.valueOf(now));
        response.put("claims", claims);
      } catch (FirebaseAuthException e) {
        throw new RuntimeException(e);
      }
      return new JSONObject().put("success", true).put("data", response)
          .put("message", "Registration Successful");
    }
    return new JSONObject().put("success", false).put("message", "Registration Failed");
  }

  public static JSONObject createAccountWithEmailAndPasswordUsingFirebaseAuth(@NotNull User user) {
    CreateRequest request = new CreateRequest()
        .setEmail(user.getEmail())
        .setEmailVerified(false)
        .setPassword(user.getPassword())
        .setDisplayName(user.getDisplayName())
        .setPhoneNumber(user.getPhoneNumber())
        .setDisabled(false);
    try {
      UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
      JSONObject response = new JSONObject(userRecord);
      Map<String, Object> claims = new HashMap<>();
      claims.put("admin", user.isAdmin());
      claims.put("birthday", user.getBirthday());
      long now = System.currentTimeMillis();
      claims.put("lastModifiedAt", now);
      FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);
      user.setLastModifiedDate(String.valueOf(now));
      response.put("claims", claims);
      return new JSONObject().put("success", true).put("data", response)
          .put("message", "User created successfully.");
    } catch (FirebaseAuthException e) {
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static JSONObject refreshAccessToken(String refreshToken) {
    String url = "https://securetoken.googleapis.com/v1/token?key=" + Firebase.getApiKey();
    String body =
        "{\n" + "  \"grant_type\": \"refresh_token\",\n" + "  \"refresh_token\": \"" + refreshToken
            + "\"\n" + "}";
    return Fetcher.post(url, body);
  }

  public static JSONObject getUserData(String idToken) {
    String url =
        "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + Firebase.getApiKey();
    String body = "{\n" + "  \"idToken\": \"" + idToken + "\"\n" + "}";
    return Fetcher.post(url, body);
  }

  public static int countTotalUser() {
    try {
      Iterable<ExportedUserRecord> users = FirebaseAuth.getInstance().listUsers(null).getValues();
      return (int) StreamSupport.stream(users.spliterator(), false).count();
    } catch (Exception e) {
      return 0;
    }
  }

  public static boolean verifyPassword(String email, String password) {
    JSONObject response = loginWithEmailAndPassword(email, password);
    return response.has("data");
  }

}
