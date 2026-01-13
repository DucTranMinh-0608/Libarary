package com.app.librarymanager.controllers;

import com.app.librarymanager.utils.AlertDialog;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.app.librarymanager.models.User;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

public class ManageUsersController extends ControllerWithLoader {

  private static ManageUsersController instance;

  @FXML
  private TableView<User> usersTable;

  @FXML
  private TableColumn<User, String> uidColumn;

  @FXML
  private TableColumn<User, String> emailColumn;

  @FXML
  private TableColumn<User, String> phoneNumberColumn;

  @FXML
  private TableColumn<User, String> displayNameColumn;
  @FXML
  private TableColumn<User, String> birthdayColumn;
  @FXML
  private TableColumn<User, Boolean> adminColumn;
  @FXML
  private TableColumn<User, Boolean> emailVerifiedColumn;
  @FXML
  private TableColumn<User, Boolean> disabledColumn;
  @FXML
  private TableColumn<User, String> photoUrlColumn;
  @FXML
  private TableColumn<User, String> createdAtColumn;
  @FXML
  private TableColumn<User, String> updatedAtColumn;
  @FXML
  private TableColumn<User, String> lastLoginAtColumn;

  @FXML
  private TextField searchField;

  @FXML
  private ComboBox<String> adminFilter;
  @FXML
  private ComboBox<String> emailVerifiedFilter;
  @FXML
  private ComboBox<String> disabledFilter;

  @FXML
  private Text totalUsersText;

  private HashMap<String, Image> imageCache = new HashMap<>();

  private ObservableList<User> usersList = FXCollections.observableArrayList();

  public synchronized static ManageUsersController getInstance() {
    if (instance == null) {
      instance = new ManageUsersController();
    }
    return instance;
  }

  @FXML
  public void initialize() {
    setLoadingText("Loading users...");

    uidColumn.setCellValueFactory(new PropertyValueFactory<>("uid"));
    emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    displayNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
    phoneNumberColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
    birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
    adminColumn.setCellValueFactory(new PropertyValueFactory<>("admin"));
    emailVerifiedColumn.setCellValueFactory(new PropertyValueFactory<>("emailVerified"));
    disabledColumn.setCellValueFactory(new PropertyValueFactory<>("disabled"));
    photoUrlColumn.setCellValueFactory(new PropertyValueFactory<>("photoUrl"));
    createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("lastModifiedDate"));
    lastLoginAtColumn.setCellValueFactory(new PropertyValueFactory<>("lastLoginAt"));

    photoUrlColumn.setCellFactory(column -> new TableCell<User, String>() {
      private final ImageView imageView = new ImageView();

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
          setGraphic(null);
        } else {
          if (imageCache.containsKey(item)) {
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setImage(imageCache.get(item));
            setGraphic(imageView);
          } else {
            try {
              Image image = new Image(item, true);
              image.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                  setGraphic(null);
                }
              });
              imageCache.put(item, image);
              imageView.setImage(image);
              imageView.setFitHeight(40);
              imageView.setFitWidth(40);
              setGraphic(imageView);
            } catch (Exception e) {
              setGraphic(null);
            }
          }
        }
      }
    });

    uidColumn.setPrefWidth(150);
    emailColumn.setPrefWidth(200);
    displayNameColumn.setPrefWidth(200);
    phoneNumberColumn.setPrefWidth(150);
    birthdayColumn.setPrefWidth(150);
    adminColumn.setPrefWidth(100);
    emailVerifiedColumn.setPrefWidth(150);
    disabledColumn.setPrefWidth(100);
    photoUrlColumn.setPrefWidth(50);
    createdAtColumn.setPrefWidth(200);
    updatedAtColumn.setPrefWidth(200);
    lastLoginAtColumn.setPrefWidth(200);

    setDateCellFactory(createdAtColumn);
    setDateCellFactory(updatedAtColumn);
    setDateCellFactory(lastLoginAtColumn);

    adminFilter.getItems().addAll("All", "True", "False");
    emailVerifiedFilter.getItems().addAll("All", "True", "False");
    disabledFilter.getItems().addAll("All", "True", "False");

    adminFilter.setValue("All");
    emailVerifiedFilter.setValue("All");
    disabledFilter.setValue("All");
    adminFilter.setPrefWidth(150);
    emailVerifiedFilter.setPrefWidth(150);
    disabledFilter.setPrefWidth(150);

    adminFilter.setOnAction(e -> onFilter());
    emailVerifiedFilter.setOnAction(e -> onFilter());
    disabledFilter.setOnAction(e -> onFilter());

    loadUsers();
    setRowContextMenu();
  }

  private void loadUsers() {
    Task<ObservableList<User>> task = new Task<>() {
      @Override
      protected ObservableList<User> call() {
        ObservableList<User> users = FXCollections.observableArrayList();
        List<User> response = UserController.listUsers();
        if (response == null) {
          AlertDialog.showAlert("error", "Error", "Failed to load users", null);
          return users;
        }
        users.addAll(response);
        return users;
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      usersList.setAll(task.getValue());
      usersTable.setItems(usersList);
      //  System.out.println("Users loaded successfully. Total: " + usersList.size());
      showLoading(false);
      totalUsersText.setText("Total Users: " + usersList.size());
    });
    task.setOnFailed(e -> {
      //  System.out.println("Error while fetching users: " + task.getException().getMessage());
      showLoading(false);
    });

    new Thread(task).start();
  }

  @FXML
  private void onCreateUser() {
    openUserModal(null);
  }

  @FXML
  private void onSearch() {
    String searchText = searchField.getText().toLowerCase();
    ObservableList<User> filteredList = FXCollections.observableArrayList();
    for (User user : usersList) {
      if (user.getDisplayName().toLowerCase().contains(searchText) || user.getEmail().toLowerCase()
          .contains(searchText) || user.getUid().toLowerCase().contains(searchText)
          || user.getPhoneNumber().toLowerCase().contains(searchText)) {
        filteredList.add(user);
      }
    }
    usersTable.setItems(filteredList);
  }

  @FXML
  private void onFilter() {
    String searchText = searchField.getText().toLowerCase();
    String adminFilterValue = adminFilter.getValue();
    String emailVerifiedFilterValue = emailVerifiedFilter.getValue();
    String disabledFilterValue = disabledFilter.getValue();

    ObservableList<User> filteredList = FXCollections.observableArrayList();
    for (User user : usersList) {
      boolean matchesSearch =
          user.getDisplayName().toLowerCase().contains(searchText) || user.getEmail().toLowerCase()
              .contains(searchText) || user.getUid().toLowerCase().contains(searchText)
              || user.getPhoneNumber().toLowerCase().contains(searchText);

      boolean matchesAdmin =
          adminFilterValue.equals("All") || (adminFilterValue.equals("True") && user.isAdmin()) || (
              adminFilterValue.equals("False") && !user.isAdmin());

      boolean matchesEmailVerified =
          emailVerifiedFilterValue.equals("All") || (emailVerifiedFilterValue.equals("True")
              && user.isEmailVerified()) || (emailVerifiedFilterValue.equals("False")
              && !user.isEmailVerified());

      boolean matchesDisabled =
          disabledFilterValue.equals("All") || (disabledFilterValue.equals("True")
              && user.isDisabled()) || (disabledFilterValue.equals("False") && !user.isDisabled());

      if (matchesSearch && matchesAdmin && matchesEmailVerified && matchesDisabled) {
        filteredList.add(user);
      }
    }
    usersTable.setItems(filteredList);
  }

  private void setRowContextMenu() {
    usersTable.setRowFactory(tableView -> {
      final TableRow<User> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem editMenuItem = new MenuItem("Edit");
      final MenuItem deleteMenuItem = new MenuItem("Delete");

      editMenuItem.setOnAction(event -> {
        User user = row.getItem();
        //  System.out.println("Edit user: " + user.getUid());
        openUserModal(user);
      });

      deleteMenuItem.setOnAction(event -> {
        User user = row.getItem();
        //  System.out.println("Delete user: " + user.getUid());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete user " + user.getEmail() + "?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(response -> {
          if (response == ButtonType.YES) {
            Task<JSONObject> task = new Task<JSONObject>() {
              @Override
              protected JSONObject call() throws Exception {
                return UserController.deleteUser(user);
              }
            };
            task.setOnRunning(ev -> showLoading(true));
            task.setOnSucceeded(e -> {
              JSONObject delRes = task.getValue();
              //  System.out.println("User deleted: " + delRes);
              showLoading(false);
//              loadUsers();
              removeUserFromTable(user);
              usersList.remove(user);
              totalUsersText.setText("Total Users: " + usersList.size());
            });
            task.setOnFailed(ev -> {
              showLoading(false);
              //  System.out.println("Error while deleting user: " + task.getException().getMessage());
            });
            new Thread(task).start();
          }
        });
      });

      contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);
      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
    });
  }

  private void setDateCellFactory(TableColumn<User, String> column) {
    column.setCellFactory(col -> new TableCell<User, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
          setText(null);
        } else {
          setText(new Date(Long.parseLong(item)).toLocaleString());
        }
      }
    });
  }

  public void updateUserInTable(User updatedUser) {
    usersList.replaceAll(user -> user.getUid().equals(updatedUser.getUid()) ? updatedUser : user);
    usersTable.refresh();
  }

  private void removeUserFromTable(User user) {
    usersList.remove(user);
    usersTable.getItems().remove(user);
    usersTable.refresh();
  }

  private void openUserModal(User user) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/user-modal.fxml"));
      Parent parent = loader.load();

      UserModalController controller = loader.getController();
      controller.setUser(user);
      controller.setSaveCallback(updatedUser -> {
        if (user == null) {
          usersList.add(updatedUser);
          usersTable.setItems(usersList);
          usersTable.refresh();
        } else {
          updateUserInTable(updatedUser);
        }
      });

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle(user == null ? "Create User" : "Edit User");
      dialog.initOwner(usersTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      String okButtonText = user != null ? "Save & Update" : "Create";

      ButtonType okButtonType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButtonType = ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
      Button saveButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
      saveButton.getStyleClass().addAll("btn", "btn-primary");
      saveButton.addEventFilter(ActionEvent.ACTION, event -> {
        controller.onSubmit();
        event.consume();
      });
      Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
      cancelButton.getStyleClass().addAll("btn", "btn-text");
      cancelButton.addEventFilter(ActionEvent.ACTION, event -> dialog.close());

      dialog.setResultConverter(dialogButton -> null);

      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}