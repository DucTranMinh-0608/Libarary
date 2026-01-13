package com.app.librarymanager.controllers;

import com.app.librarymanager.utils.AlertDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class ControllerWithLoader {

  @FXML
  protected VBox loadingOverlay;
  @FXML
  protected ProgressIndicator loadingSpinner;
  @FXML
  protected Text loadingText;
  @FXML
  private Button cancelButton;

  protected void setLoadingText(String text) {
    loadingText.setText(text);
  }

  protected void setCancelLoadingAction(Callback<Void, Void> action) {
    if (cancelButton == null) {
      AlertDialog.showAlert("Error", "Cancel button not found.",
          "Please check the FXML file for missing components.", null);
      return;
    }
    cancelButton.setOnAction(e -> action.call(null));
  }

  protected void showCancel(boolean show) {
    if (cancelButton == null) {
      AlertDialog.showAlert("Error", "Cancel button not found.",
          "Please check the FXML file for missing components.", null);
      return;
    }
    cancelButton.setVisible(show);
    cancelButton.setManaged(show);
  }

  protected void showLoading(boolean show) {
    if (loadingOverlay == null || loadingSpinner == null) {
      AlertDialog.showAlert("Error", "Loading components not found.",
          "Please check the FXML file for missing components.", null);
      return;
    }
    loadingOverlay.setVisible(show);
    loadingSpinner.setVisible(show);
  }
}
