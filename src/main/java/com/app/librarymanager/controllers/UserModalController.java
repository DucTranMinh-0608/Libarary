package com.app.librarymanager.controllers;

import com.app.librarymanager.services.UserService;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.AvatarUtil;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import java.time.LocalDate;
import java.util.Objects;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.app.librarymanager.models.User;
import javafx.util.Callback;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class UserModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface SaveCallback {

    void onSave(User user);
  }

  @FXML
  private TextField emailField;
  @FXML
  private TextField displayNameField;
  @FXML
  private TextField phoneNumberField;
  @FXML
  private DatePicker birthdayField;
  @FXML
  private TextField passWordField;
  @FXML
  private CheckBox adminCheckBox;
  @FXML
  private CheckBox emailVerifiedCheckBox;
  @FXML
  private CheckBox disabledCheckBox;
  @FXML
  private VBox phoneNumberContainer;

  @FXML
  private ImageView profileImageView;
  @FXML
  private HBox avatarContainer;
  @FXML
  private StackPane imageStackPane;


  private User user;
  @Setter
  private SaveCallback saveCallback;
  private boolean isEditMode = false;
  private String filePath;
  String displayName;

  @FXML
  private void initialize() {
    showCancel(false);
    birthdayField.getEditor().setOnMouseClicked(event -> {
      birthdayField.show();
    });
    DatePickerUtil.setDatePickerFormat(birthdayField);
    DatePickerUtil.disableFutureDates(birthdayField);
    DatePickerUtil.disableEditor(birthdayField);

    imageStackPane.setOnMouseClicked(event -> handleSelectImage());
    imageStackPane.getStyleClass().add("cursor-pointer");
  }

  private void handleSelectImage() {
    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
    fileChooser.setTitle("Select Profile Image");
    fileChooser.getExtensionFilters().addAll(
        new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
    );
    java.io.File selectedFile = fileChooser.showOpenDialog(avatarContainer.getScene().getWindow());
    if (selectedFile != null) {
      filePath = selectedFile.getAbsolutePath();
      profileImageView.setImage(new Image(selectedFile.toURI().toString()));
    }
  }

  public void setUser(User user) {
    this.user = user;
    if (user != null) {
      isEditMode = true;
      emailField.setText(user.getEmail());
      emailField.setDisable(true);
      displayNameField.setText(user.getDisplayName());
      phoneNumberField.setText(user.getPhoneNumber());
      birthdayField.setValue(
          user.getBirthday() != null && !user.getBirthday().isEmpty() ? DateUtil.parse(
              user.getBirthday()) : null);
      adminCheckBox.setSelected(user.isAdmin());
      emailVerifiedCheckBox.setSelected(user.isEmailVerified());
      disabledCheckBox.setSelected(user.isDisabled());
      displayName = user.getDisplayName();
      if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
        Task<Image> loadImageTask = getImageTask(user.getPhotoUrl());
        new Thread(loadImageTask).start();
      } else {
        setDefaultAvatar();
      }
    } else {
      isEditMode = false;
      emailField.setDisable(false);
      ((VBox) phoneNumberContainer.getParent()).getChildren().remove(phoneNumberContainer);
      ((VBox) avatarContainer.getParent()).getChildren().remove(avatarContainer);
    }
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
      avatarContainer.setVisible(false);
      showLoading(true);
    });
    loadImageTask.setOnSucceeded(event -> {
      showLoading(false);
      avatarContainer.setVisible(true);
      Image image = loadImageTask.getValue();
      if (image.getException() == null) {
        profileImageView.setImage(image);
      } else {
        //  System.err.println("Failed to load image from photoUrl: " + photoUrl);
        setDefaultAvatar();
      }
    });
    loadImageTask.setOnFailed(event -> {
      avatarContainer.setVisible(true);
//      System.err.println(
//          "Exception while loading image from photoUrl: " + loadImageTask.getException()
//              .getMessage());
      setDefaultAvatar();
    });

    return loadImageTask;
  }

  private void setDefaultAvatar() {
    avatarContainer.setVisible(true);
    AvatarUtil avatarUtil = new AvatarUtil();
    avatarUtil.setRounded(true).setBold(true).setBackground("bae6fd");
    profileImageView.setImage(
        new Image(avatarUtil.getAvatarUrl(
            Objects.requireNonNullElse(displayName, "Anonymous"))));
  }

  @FXML
  void onSubmit() {
    if (user == null) {
      user = new User();
    }
    user.setEmail(emailField.getText());
    user.setDisplayName(displayNameField.getText());
    user.setPhoneNumber(phoneNumberField.getText());
    user.setBirthday(birthdayField.getEditor().getText().trim());
    user.setAdmin(adminCheckBox.isSelected());
    user.setEmailVerified(emailVerifiedCheckBox.isSelected());
    user.setDisabled(disabledCheckBox.isSelected());
    user.setPassword(passWordField.getText());
    user.setPhotoUrl(filePath != null && !filePath.isEmpty() ? filePath : user.getPhotoUrl());

    Task<JSONObject> task = new Task<JSONObject>() {
      @Override
      protected JSONObject call() throws Exception {
        return isEditMode ? UserController.updateUser(user) : UserController.createUser(user);
      }
    };

    task.setOnRunning(e -> showLoading(true));

    task.setOnSucceeded(e -> {
      showLoading(false);
      JSONObject resp = task.getValue();
      //  System.out.println(resp);
      Stage stage = (Stage) emailField.getScene().getWindow();
      if (resp.getBoolean("success")) {
        AlertDialog.showAlert("success", "Success", resp.getString("message"), null);
        stage.close();
        if (saveCallback != null) {
          if (resp.has("data") && filePath != null && !filePath.isEmpty()) {
            user.setPhotoUrl(resp.getJSONObject("data").getString("photoUrl"));
          }
          saveCallback.onSave(user);
        }
      } else {
        AlertDialog.showAlert("error", "Error", resp.getString("message"), null);
      }
    });

    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "An error occurred while saving the user.", null);
    });

    new Thread(task).start();
  }

  @FXML
  private void onCancel() {
    Stage stage = (Stage) emailField.getScene().getWindow();
    stage.close();
  }
}