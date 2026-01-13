package com.app.librarymanager.controllers;

import com.app.librarymanager.interfaces.AuthStateListener;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.AvatarUtil;
import com.app.librarymanager.utils.StageManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class LayoutController implements AuthStateListener {

  private static LayoutController instance;

  public static synchronized LayoutController getInstance() {
    if (instance == null) {
      instance = new LayoutController();
    }
    return instance;
  }

  private final List<String> ADMIN_ROUTES = List.of("/views/admin/dashboard.fxml",
      "/views/admin/manage-books.fxml", "/views/admin/manage-users.fxml",
      "/views/admin/manage-loans.fxml", "/views/admin/manage-categories.fxml");

  private final List<String> USER_ROUTES = List.of("/views/home.fxml", "/views/profile.fxml");
  @FXML
  private Button homeNavBtn;
  @FXML
  private Button allBooksNavBtn;
  @FXML
  private Button categoriesNavBtn;
  @FXML
  private Button loansNavBtn;
  @FXML
  private Button favoritesNavBtn;
  @FXML
  private Button profileNavBtn;
  @FXML
  private Button logoutNavBtn;
  @FXML
  private ToolBar adminToolBar;
  @FXML
  private ToolBar navBar;
  @FXML
  private StackPane contentPane;
  @FXML
  private ImageView avatarImageView;
  @FXML
  private TextField searchField;
  @FXML
  private ImageView navLogo;


  private Popup popup;


  private final Map<String, Parent> componentCache = new HashMap<>();

  @FXML
  private void initialize() {
    AuthController.getInstance().addAuthStateListener(this);
    AuthController.getInstance().loadSession();

    popup = new Popup();
    popup.setAutoHide(true);

    if (AuthController.getInstance().validateIdToken()) {
      User currentUser = AuthController.getInstance().getCurrentUser();
//      preloadComponents(currentUser);
      updateUI(true, currentUser); //Kiểm tra phiên đăng nhập trước đang là user hay admin
      if (currentUser.isAdmin()) {
        loadComponent("/views/admin/dashboard.fxml");

      } else {
        loadComponent("/views/home.fxml");
      }
    } else {
      AuthController.getInstance().logout();
//      preloadComponents(null);
      updateUI(false, null);
      loadComponent("/views/home.fxml");
    }
    searchField.setOnAction(event -> handleSearch());
    allBooksNavBtn.setOnAction(event -> handleSearch());

    homeNavBtn.setOnAction(event -> loadComponent("/views/home.fxml"));
    categoriesNavBtn.setOnAction(event -> loadComponent("/views/categories.fxml"));
    loansNavBtn.setOnAction(event -> {
      AuthController.requireLogin();
      if (AuthController.getInstance().isAuthenticated()) {
        loadComponent("/views/loans.fxml");
      }
    });
    favoritesNavBtn.setOnAction(event -> {
      AuthController.requireLogin();
      if (AuthController.getInstance().isAuthenticated()) {
        loadComponent("/views/favorite-books.fxml");
      }
    });
    profileNavBtn.setOnAction(event -> {
      AuthController.requireLogin();
      if (AuthController.getInstance().isAuthenticated()) {
        handleProfileSettings();
      }
    });
    navLogo.setOnMouseClicked(event -> loadComponent("/views/home.fxml"));
    logoutNavBtn.setOnAction(event -> handleLogoutNavButton());
  }

  @FXML
  public void handleImageClick() {
    if (popup.isShowing()) {
      popup.hide();
    } else {
      Bounds boundsInScreen = avatarImageView.localToScreen(avatarImageView.getBoundsInLocal());
      popup.setOnShown(event -> {
        double popupX = boundsInScreen.getMinX() + (boundsInScreen.getWidth()) - (popup.getWidth());
        double popupY = boundsInScreen.getMaxY();
        popup.setX(popupX);
        popup.setY(popupY);
      });
      popup.show(avatarImageView, popup.getX(), popup.getY());
    }
  }

  private void preloadComponents(User user) {
    try {
      for (String route : USER_ROUTES) {
        componentCache.put(route, loadFXML(route));
      }
      if (user != null) {
        if (user.isAdmin()) {
          for (String route : ADMIN_ROUTES) {
            componentCache.put(route, loadFXML(route));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Parent loadFXML(String fxmlPath) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    return loader.load();
  }

  private void updateUI(boolean isAuthenticated, User user) {
    adminToolBar.setVisible(isAuthenticated && user.isAdmin());
    adminToolBar.setManaged(isAuthenticated && user.isAdmin());
    logoutNavBtn.setVisible(isAuthenticated);
    logoutNavBtn.setManaged(isAuthenticated);
    profileNavBtn.setVisible(isAuthenticated);
    profileNavBtn.setManaged(isAuthenticated);
    avatarImageView.setVisible(!isAuthenticated);
    avatarImageView.setManaged(!isAuthenticated);
    if (isAuthenticated) {
      if (user.getAvatar() != null) {
        avatarImageView.setImage(user.getAvatar());
      } else if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
        avatarImageView.setImage(new ImageView(user.getPhotoUrl()).getImage());
      } else {
        String displayName = user.getDisplayName();
        String email = user.getEmail();
        String name = displayName != null && !displayName.isEmpty() ? displayName : email;
        avatarImageView.setImage(
            new ImageView(new AvatarUtil().getAvatarUrl(name)).getImage());
      }
      VBox popupContent = new VBox(0);
      popupContent.getStyleClass().add("popup-container");
      Button profileButton = new Button("Profile");
      profileButton.setOnAction(event -> handleProfileSettings());
      Button logoutButton = new Button("Logout");
      logoutButton.getStyleClass().add("danger");
      logoutButton.setOnAction(event -> onLogoutButtonClick());

      popupContent.getChildren().addAll(profileButton, logoutButton);
      popup.getContent().clear();
      popup.getContent().add(popupContent);

    } else {
      VBox popupContent = new VBox(0);
      popupContent.getStyleClass().add("popup-container");
      Button loginButton = new Button("Login");
      loginButton.setOnAction(event -> onLoginButtonClick());

      Button registerButton = new Button("Register");
      registerButton.setOnAction(event -> onRegisterButtonClick());

      popupContent.getChildren().addAll(loginButton, registerButton);

      popup.getContent().clear();
      popup.getContent().add(popupContent);

      avatarImageView.setImage(
          new ImageView(new AvatarUtil().getAvatarUrl("Anonymous")).getImage());
    }
  }

  @FXML
  private void onLoginButtonClick() {
    StageManager.showLoginWindow();
  }

  private boolean isSearchComponentLoaded = false;

  private void handleSearch() {
    String keyword = searchField.getText().trim();
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/search.fxml"));
      Parent searchComponent = loader.load();
      SearchController searchController = loader.getController();
      searchController.setKeyword(keyword);
      contentPane.getChildren().clear();
      contentPane.getChildren().add(searchComponent);
      navBar.getItems().removeIf(item -> item instanceof TextField);
      isSearchComponentLoaded = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void handleViewBookDetails() {
    // Show book details for selected book
  }

  @FXML
  public void handleProfileSettings() {
    loadComponent("/views/profile.fxml");
    popup.hide();
  }

  @FXML
  public void handleShowDashboard(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/dashboard.fxml");

  }

  @FXML
  public void handleManageBooks(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-books.fxml");
  }

  @FXML
  public void handleManageUsers(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-users.fxml");
  }

  @FXML
  void handleManageBookLoans(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-loans.fxml");
  }

  @FXML
  public void handleManageCategories(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-categories.fxml");
  }

  @FXML
  private void onRegisterButtonClick() {
    StageManager.showRegisterWindow();
  }


  @FXML
  private void onLogoutButtonClick() {
    popup.hide();
    performLogout();
  }

  @FXML
  private void handleLogoutNavButton() {
    performLogout();
  }

  private void performLogout() {
    loadComponent("/views/home.fxml");
    if (isSearchComponentLoaded) {
      navBar.getItems().add(2, searchField);
      searchField.clear();
      isSearchComponentLoaded = false;
    }
    if (AuthController.getInstance().getCurrentUser() != null && AuthController.getInstance().getCurrentUser().isAdmin()) {
      adminToolBar.getItems().stream().filter(node -> node instanceof Button)
          .map(node -> (Button) node).filter(button -> button.getStyleClass().contains("active"))
          .forEach(button -> button.getStyleClass().remove("active"));
    }
    AuthController.getInstance().logout();
  }

  @FXML
  public void closeLoginWindow() {

  }

  @Override
  public void onAuthStateChanged(boolean isAuthenticated) {
    updateUI(isAuthenticated, AuthController.getInstance().getCurrentUser());
  }

  private void handleChangeActiveButton(Event e) {
    if (!(e.getSource() instanceof Button clickedButton)) {
      return;
    }
    adminToolBar.getItems().stream().filter(node -> node instanceof Button)
        .map(node -> (Button) node).filter(button -> button.getStyleClass().contains("active"))
        .forEach(button -> button.getStyleClass().remove("active"));

    clickedButton.getStyleClass().add("active");
  }

  private void loadComponent(String fxmlPath) {
    Task<Parent> loadTask = new Task<>() {
      @Override
      protected Parent call() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        return loader.load();
      }
    };

    loadTask.setOnSucceeded(event -> {
      Platform.runLater(() -> {
        Parent component = loadTask.getValue();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(component);
        if (isSearchComponentLoaded) {
          navBar.getItems().add(2, searchField);
          searchField.clear();
          isSearchComponentLoaded = false;
        }
      });
    });

    loadTask.setOnFailed(event -> {
      loadTask.getException().printStackTrace();
    });

    new Thread(loadTask).start();
  }
}