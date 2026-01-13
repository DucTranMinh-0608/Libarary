package com.app.librarymanager.utils;

import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class AlertDialog {

  public AlertDialog() {
  }

  public static void showAlert(String type, String title, String message,
      EventHandler<ActionEvent> okCallback) {
    if (okCallback == null) {
      okCallback = event -> {
      };
    }
    Alert alert = getAlert(title, message, type);
    alert.getDialogPane().getStylesheets().add(
        Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
            .toExternalForm());

    alert.getDialogPane().getStyleClass().add("custom-alert");
    alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
    if (alert.getDialogPane().lookup(".header-panel") != null) {
      alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
    }
    Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
    okButton.getStyleClass().addAll("btn", "btn-primary");
    okButton.setOnAction(okCallback);
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    alert.showAndWait();
  }

  @NotNull
  private static Alert getAlert(String title, String message, String type) {
    Alert alert = new Alert(AlertType.INFORMATION);
//    alert.setTitle((!type.isEmpty() ? type.toUpperCase() : "INFORMATION") + " | " + title);
    alert.setTitle(title);
    switch (type.toLowerCase()) {
      case "error":
        alert.setAlertType(AlertType.ERROR);
        break;
      case "warning":
        alert.setAlertType(AlertType.WARNING);
        break;
      case "confirmation":
        alert.setAlertType(AlertType.CONFIRMATION);
        break;
      default:
        alert.setAlertType(AlertType.INFORMATION);
        break;
    }

    alert.setHeaderText(null);
    alert.setContentText(message);
    return alert;
  }

  public static boolean showConfirm(String title, String message) {
    Alert alert = getAlert(title, message, "confirmation");
    alert.getDialogPane().getStylesheets().add(
        Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
            .toExternalForm());
    ButtonType yes = new ButtonType("Yes");
    ButtonType no = new ButtonType("No");
    alert.getButtonTypes().setAll(yes, no);

    alert.getDialogPane().getStyleClass().add("custom-alert");
    alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
    if (alert.getDialogPane().lookup(".header-panel") != null) {
      alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
    }
    Button yesButton = (Button) alert.getDialogPane().lookupButton(yes);
    yesButton.getStyleClass().addAll("btn", "btn-primary");
    Button noButton = (Button) alert.getDialogPane().lookupButton(no);
    noButton.getStyleClass().addAll("btn", "btn-danger");
    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    return alert.showAndWait().filter(yes::equals).isPresent();
  }
}
