package com.app.librarymanager.controllers;

import com.app.librarymanager.services.FirebaseAuthentication;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.StageManager;
import java.io.IOException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

public class LoginController extends ControllerWithLoader {

  @FXML
  private TextField emailField;
  @FXML
  private PasswordField passwordField;

  @FXML
  private Button loginButton;

  @FXML
  private void initialize() {
    Platform.runLater(() -> emailField.requestFocus());
    setLoadingText("Logging in...");
  }

  @FXML
  private void handleLoginAction() {
    String email = emailField.getText().trim();
    String password = passwordField.getText().trim();

    if (validateEmailAndPassword(email, password)) {
      return;
    }
    Task<JSONObject> loginTask = new Task<JSONObject>() {
      @Override
      protected JSONObject call() throws Exception {
        return AuthController.login(email, password);
      }
    };
    loginTask.setOnRunning(e -> showLoading(true));
    loginTask.setOnSucceeded(e -> {
      showLoading(false);
      JSONObject response = loginTask.getValue();
      if (response.getBoolean("success")) {
        AuthController.getInstance().onLoginSuccess(response.getJSONObject("data"));
        StageManager.closeActiveChildWindow();
      } else {
        passwordField.clear();
        passwordField.requestFocus();
        AuthController.getInstance().onLoginFailure(response.getString("message"));
      }
    });
    loginTask.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error",
          "An error occurred while logging in. Please try again.",
          null);
    });
    new Thread(loginTask).start();
  }

  static boolean validateEmailAndPassword(String email, String password) {
    if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
      AlertDialog.showAlert("error", "Invalid Email", "Please enter a valid email address.", null);
      return true;
    }

    if (password.length() < 6) {
      AlertDialog.showAlert("error", "Invalid Password",
          "Password must be at least 6 characters long.", null);
      return true;
    }
    return false;
  }





  @FXML
  private void handleOpenRegister() {
    StageManager.showRegisterWindow();
  }

  @FXML
  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ENTER) {
      handleLoginAction();
    }
  }

  @FXML
  public void handleClose() {
  }

}