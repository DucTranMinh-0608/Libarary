package com.app.librarymanager.utils;

import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

public class StageManager {

  @Getter
  private static Stage primaryStage;
  private static Stage childStage;

  private static void showStage(Scene scene, Stage stage, String title, boolean resizable) {
    scene.getStylesheets().add(
        Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
            .toExternalForm());
    stage.setScene(scene);
    stage.setTitle(title);
    stage.setResizable(resizable);
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    stage.centerOnScreen();
    stage.getIcons().add(new Image(
        Objects.requireNonNull(StageManager.class.getResource("/images/logo-icon.png"))
            .toExternalForm()));
    stage.show();
  }

  private static Scene loadScene(String fxmlPath) {
    try {
      return new Scene(FXMLLoader.load(
          Objects.requireNonNull(StageManager.class.getResource(fxmlPath))));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void closeActiveChildWindow() {
    if (childStage != null) {
      childStage.close();
      childStage = null;
    }
  }

  public static void handleClosePrimaryStage() {
    closeActiveChildWindow();
    Platform.exit();
    System.exit(0);
  }

  public static void showChildWindow(String fxmlPath, String title, boolean resizable) {
    closeActiveChildWindow();
    Scene childScene = loadScene(fxmlPath);
    if (childScene != null) {
      Stage childStage = new Stage();
      childStage.initOwner(primaryStage);
      childStage.initModality(Modality.WINDOW_MODAL);
      showStage(childScene, childStage, title, resizable);
      StageManager.childStage = childStage;
    }
  }

  public static void showLoginWindow() {
    showChildWindow("/views/auth/login.fxml", "Login | Library Manager", false);
  }

  public static void showRegisterWindow() {
    showChildWindow("/views/auth/register.fxml", "Register | Library Manager", false);
  }

  public static void showHomeWindow() {
    primaryStage.setWidth(1280);
    primaryStage.setHeight(720);
    showStage(Objects.requireNonNull(loadScene("/views/layout.fxml")), primaryStage,
        "Library Manager", true);
  }



  public static void setPrimaryStage(Stage stage) {
    primaryStage = stage;
    primaryStage.setOnCloseRequest(event -> handleClosePrimaryStage());
  }

}
