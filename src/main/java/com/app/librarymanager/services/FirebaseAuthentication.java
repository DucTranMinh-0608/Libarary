package com.app.librarymanager.services;

import com.app.librarymanager.controllers.AuthController;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.Fetcher;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import io.github.cdimascio.dotenv.Dotenv;
import java.awt.Desktop;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class FirebaseAuthentication {

  private static final Dotenv dotenv = Dotenv.load();
  private static final String LOGIN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
  private static final String REGISTER_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String CREDENTIALS_FILE_PATH = dotenv.get("FIREBASE_CLIENT_SECRET_PATH");
  private static final Collection<String> SCOPES = Arrays.asList(
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/userinfo.profile");
  private static LocalServerReceiver receiver;

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
        //  System.out.println("Login Failed: " + error.getString("message"));
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
    //  System.out.println(response);
    if (response == null) {
      AuthController.getInstance().onRegisterFailure("Registration Failed");
      return new JSONObject().put("success", false).put("message", "Registration Failed");
    }
    if (response.has("error")) {
      JSONObject error = response.getJSONObject("error");
      if (error.has("message")) {
//        AuthController.getInstance().onRegisterFailure(error.getString("message"));
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
//      AuthController.getInstance().onRegisterSuccess(response);
      return new JSONObject().put("success", true).put("data", response)
          .put("message", "Registration Successful");
    }
    return new JSONObject().put("success", false).put("message", "Registration Failed");
  }

  public static JSONObject createAccountWithEmailAndPasswordUsingFirebaseAuth(@NotNull User user) {
    //  System.out.println("Creating user: " + user.toString());
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
      //  System.out.println("User created successfully: " + response.toString());
      return new JSONObject().put("success", true).put("data", response)
          .put("message", "User created successfully.");
    } catch (FirebaseAuthException e) {
      //  System.err.println("Error creating user: " + e.getMessage());
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

  public static JSONObject signInWithIdp(String idToken) {
    String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key="
        + Firebase.getApiKey();
    String body = "{\n" + "  \"postBody\": \"id_token=" + idToken + "&providerId=google.com\",\n"
        + "  \"requestUri\": \"http://localhost:8889\",\n" + "  \"returnIdpCredential\": true,\n"
        + "  \"returnSecureToken\": true\n" + "}";
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

  private static GoogleClientSecrets loadClientSecrets() throws IOException {
    assert CREDENTIALS_FILE_PATH != null;
    FileReader reader = new FileReader(CREDENTIALS_FILE_PATH);
    return GoogleClientSecrets.load(JSON_FACTORY, reader);
  }

  public static JSONObject getIdTokenFromGAccount() throws IOException {
    NetHttpTransport httpTransport = new NetHttpTransport();
    GoogleClientSecrets clientSecrets = loadClientSecrets();

    AuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
        JSON_FACTORY, clientSecrets, SCOPES).setAccessType("offline").build();

    if (receiver != null) {
      receiver.stop();
    }
    receiver = new LocalServerReceiver.Builder().setPort(8889).build();
    String redirectUri = receiver.getRedirectUri();
    String authUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri)
        .setState(String.valueOf(new Random().nextInt(999_999))).set("prompt", "select_account")
        .build();
    //  System.out.println("Authorization URL: " + authUrl);

    Desktop.getDesktop().browse(java.net.URI.create(authUrl));

    String authCode = receiver.waitForCode();
    if (authCode == null || authCode.isEmpty()) {
      receiver.stop();
      return new JSONObject(
          Map.of("success", false, "message", "Failed to get authorization code", "code",
              "AUTH_CODE_NOT_FOUND"));
    }
    GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(httpTransport,
        JSON_FACTORY, clientSecrets.getDetails().getClientId(),
        clientSecrets.getDetails().getClientSecret(), authCode, redirectUri).execute();

    String oAuthToken = tokenResponse.getIdToken();
    JSONObject resp = signInWithIdp(oAuthToken);

    if (resp.has("error")) {
      receiver.stop();
      JSONObject error = resp.getJSONObject("error");
      return new JSONObject(Map.of("success", false, "message", error.getString("message"), "code",
          error.getString("code")));
    }

    receiver.stop();
    return new JSONObject(Map.of("success", true, "data", resp));
  }

  public static void stopReceiver() {
    try {
      if (receiver != null) {
        receiver.stop();
      }
    } catch (IOException e) {
      AlertDialog.showAlert("error", "Error", "An error occurred while stopping the receiver.",
          null);
      e.printStackTrace();
    }
  }

  public static boolean verifyPassword(String email, String password) {
    JSONObject response = loginWithEmailAndPassword(email, password);
    return response.has("data");
  }

}
