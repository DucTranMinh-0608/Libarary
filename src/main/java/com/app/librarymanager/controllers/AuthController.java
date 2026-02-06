package com.app.librarymanager.controllers;

import com.app.librarymanager.models.User;
import com.app.librarymanager.services.FirebaseAuthentication;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.interfaces.AuthStateListener;
import com.app.librarymanager.utils.StageManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;
import javafx.stage.Stage;
import lombok.Data;
import org.json.JSONObject;

@Data
public class AuthController {

  private static AuthController instance;
  private List<AuthStateListener> authStateListeners = new CopyOnWriteArrayList<>();

  private String idToken;
  private String refreshToken;
  private String userClaims;
  private Boolean isAuthenticated = false;
  private final Preferences authPrefs;

  private User currentUser;


  public AuthController() {
    authPrefs = Preferences.userNodeForPackage(AuthController.class);
  }

  public static synchronized AuthController getInstance() {
    if (instance == null) {
      instance = new AuthController();
    }
    return instance;
  }

  private void setCurrentUser(JSONObject userClaims) {

    if (userClaims != null && !userClaims.isEmpty()) {
      this.currentUser = new User(userClaims);
    } else {
      this.currentUser = null;
    }
  }

  public void loadSession() {
    //  System.out.println("Loading session...");
    //  System.out.println("ID Token: " + authPrefs.get("idToken", null));
    this.idToken = authPrefs.get("idToken", null);
    this.refreshToken = authPrefs.get("refreshToken", null);
    this.userClaims = authPrefs.get("userClaims", null);
    this.isAuthenticated = (idToken != null);
    JSONObject claims = getUserClaims();
    setCurrentUser(claims);
  }


  public static JSONObject login(String email, String password) {
    return FirebaseAuthentication.loginWithEmailAndPassword(email, password);
  }

  public static JSONObject register(User user) {
    return FirebaseAuthentication.createAccountWithEmailAndPassword(user);
  }

  public void onLoginSuccess(JSONObject user) {
    onAuthSuccess(user);
    notifyAuthStateListeners();
  }

  public void onLoginFailure(String errorMessage) {
    //  System.out.println(errorMessage);
    this.isAuthenticated = false;
    switch (errorMessage) {
      case "EMAIL_NOT_FOUND":
        AlertDialog.showAlert("error", "Email Not Found", "Email not found. Please register first.",
                null);
        break;
      case "INVALID_EMAIL":
        AlertDialog.showAlert("error", "Invalid Email", "Please enter a valid email address.",
                null);
        break;
      case "INVALID_LOGIN_CREDENTIALS":
        AlertDialog.showAlert("error", "Invalid Credentials",
                "Wrong email or password. Please try again.", null);
        break;
      case "USER_DISABLED":
        AlertDialog.showAlert("error", "Login failed",
                "Your account has been disabled. Please contact support.", null);
        break;
      default:
        AlertDialog.showAlert("error", "Login Error",
                "An error occurred while logging in. Please try again later.", null);
        break;
    }
    notifyAuthStateListeners();
  }

  public void onRegisterSuccess(JSONObject user) {
    onAuthSuccess(user);
    //  System.out.println("User registered: " + user.getString("email"));
    notifyAuthStateListeners();
  }

  private void onAuthSuccess(JSONObject user) {
    this.isAuthenticated = true;
    this.idToken = user.getString("idToken");
    this.refreshToken = user.getString("refreshToken");
    authPrefs.put("idToken", idToken);
    authPrefs.put("refreshToken", refreshToken);
    JSONObject claims = getUserClaims();
    setCurrentUser(claims);
  }

  public void onRegisterFailure(String errorMessage) {
    this.isAuthenticated = false;
    switch (errorMessage) {
      case "EMAIL_EXISTS":
        AlertDialog.showAlert("error", "Email Exists",
                "The email address that you entered is already in use. Please try logging in instead.",
                null);
        break;
      case "INVALID_EMAIL":
        AlertDialog.showAlert("error", "Invalid Email", "Please enter a valid email address.",
                null);
        break;
      default:
        AlertDialog.showAlert("error", "Registration Error",
                "An error occurred while registering. Please try again later.", null);
        break;
    }
    notifyAuthStateListeners();
  }

  public void logout() {
    authPrefs.remove("idToken");
    authPrefs.remove("refreshToken");
    authPrefs.remove("userClaims");
    this.idToken = null;
    this.refreshToken = null;
    this.isAuthenticated = false;
    this.userClaims = null;
    notifyAuthStateListeners();
  }

  public synchronized void addAuthStateListener(AuthStateListener listener) {
    authStateListeners.add(listener);
  }

  public synchronized void removeAuthStateListener(AuthStateListener listener) {
    authStateListeners.remove(listener);
  }

  private void notifyAuthStateListeners() {
    for (AuthStateListener listener : authStateListeners) {
      if (listener != null) {
        listener.onAuthStateChanged(isAuthenticated);
      }
    }
  }

  public boolean isAuthenticated() {
    return isAuthenticated;
  }

  public boolean validateIdToken() {
    try {
      if (this.idToken == null) {
        return false;
      }
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(this.idToken);
      return true;
    } catch (Exception e) {
      //  System.err.println("Invalid or expired ID token: " + e.getMessage());
      if (this.refreshToken == null) {
        return false;
      }
      //  System.out.println("Trying to refresh token...");
      try {
        JSONObject response = FirebaseAuthentication.refreshAccessToken(this.refreshToken);
        if (response.has("error")) {
          JSONObject error = response.getJSONObject("error");
          if (error.has("message")) {
            //  System.err.println("Error refreshing token: " + error.getString("message"));
            return false;
          }
        }
        //  System.out.println(response.toString());
        String idToken;
        String refreshToken = "";
        if (response.has("id_token")) {
          idToken = response.getString("id_token");
        } else {
          idToken = response.getString("idToken");
        }
        if (response.has("refresh_token")) {
          refreshToken = response.getString("refresh_token");
        } else {
          refreshToken = response.getString("refreshToken");
        }
        authPrefs.put("idToken", idToken);
        authPrefs.put("refreshToken", refreshToken);
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.isAuthenticated = true;
        //  System.out.println("Token refreshed successfully.");
        return true;
      } catch (Exception ex) {
        //  System.err.println("Error refreshing token: " + ex.getMessage());
        return false;
      }
    }
  }


  public JSONObject getUserClaims() {
    try {
      if (this.idToken == null) {
        return new JSONObject();
      }
      if (this.userClaims != null) {
        return new JSONObject(this.userClaims);
      }
      //  System.out.println("ID Token: " + this.idToken);
      JSONObject userData = FirebaseAuthentication.getUserData(this.idToken);
      //  System.out.println("User data: " + userData);
      JSONObject claims = new JSONObject();
      if (userData.has("error")) {
        JSONObject error = userData.getJSONObject("error");
        if (error.has("message")) {
          //  System.err.println("Error getting user data: " + error.getString("message"));
          return new JSONObject();
        }
      }
      if (userData.has("users")) {
        JSONObject user = userData.getJSONArray("users").getJSONObject(0);
        claims.put("email", user.getString("email"));
        claims.put("localId", user.getString("localId"));
        claims.put("phoneNumber", user.optString("phoneNumber", ""));
        claims.put("emailVerified", user.optBoolean("emailVerified", false));
        claims.put("createdAt", new Date(user.getLong("createdAt")));
        claims.put("lastLoginAt", new Date(user.getLong("lastLoginAt")));
        if (user.has("customAttributes")) {
          JSONObject customAttributes = new JSONObject(user.getString("customAttributes"));
          claims.put("admin", customAttributes.optBoolean("admin", false));
          claims.put("birthday", customAttributes.optString("birthday", ""));
        } else {
          claims.put("admin", false);
          claims.put("birthday", "");
        }
        String displayName = user.optString("displayName", "");
        String photoUrl = user.optString("photoUrl", "");
        if (displayName.isEmpty() && user.optJSONArray("providerUserInfo") != null) {
          for (int i = 0; i < user.optJSONArray("providerUserInfo").length(); i++) {
            displayName = user.optJSONArray("providerUserInfo").getJSONObject(i)
                    .optString("displayName", "");
            if (!displayName.isEmpty()) {
              break;
            }
          }
        }
        if (photoUrl.isEmpty() && user.optJSONArray("providerUserInfo") != null) {
          for (int i = 0; i < user.optJSONArray("providerUserInfo").length(); i++) {
            photoUrl = user.optJSONArray("providerUserInfo").getJSONObject(i)
                    .optString("photoUrl", "");
            if (!photoUrl.isEmpty()) {
              break;
            }
          }
        }
        claims.put("displayName", displayName);
        claims.put("photoUrl", photoUrl);
        claims.put("providerId",
                user.getJSONArray("providerUserInfo").getJSONObject(0).optString("providerId", ""));
      }
      this.userClaims = claims.toString();
      authPrefs.put("userClaims", this.userClaims);
      return claims;
    } catch (Exception e) {
      //  System.err.println("Error in getting user claims: " + e.getMessage());
      this.userClaims = null;
      return new JSONObject();
    }
  }

  public void getNewUserClaims() {
    this.userClaims = null;
    JSONObject claims = getUserClaims();
    setCurrentUser(claims);
    notifyAuthStateListeners();
  }

  public void setCurrentUser(User updatedUser) {
    this.currentUser = updatedUser;
  }

  public static void requireLogin() {
    if (!AuthController.getInstance().validateIdToken()) {
      AlertDialog.showAlert("error", "Unauthorized", "Please login first to access this page",
              event -> {
                StageManager.showLoginWindow();
              });
    }
  }
}