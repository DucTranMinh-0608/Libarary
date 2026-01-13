package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.controllers.BookRatingController.ReturnRating;
import com.app.librarymanager.interfaces.AuthStateListener;
import com.app.librarymanager.controllers.AuthController;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.StageManager;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.json.JSONObject;
import org.kordamp.ikonli.javafx.FontIcon;

public class HomeController extends ControllerWithLoader implements AuthStateListener {

  private static HomeController instance;
  @FXML
  private ScrollPane loansScrollPane;
  @FXML
  private ScrollPane favoriteScrollPane;
  @FXML
  private ScrollPane topRatedScrollPane;
  @FXML
  private ScrollPane topLoansScrollPane;
  @FXML
  private VBox favoriteBooksContainer;

  public static synchronized HomeController getInstance() {
    if (instance == null) {
      instance = new HomeController();
    }
    return instance;
  }

  @FXML
  private Label welcomeLabel;
  @FXML
  private PauseTransition pauseTransition;
  @FXML
  private Button logoutButton;


  @FXML
  private void initialize() {
    AuthController.getInstance().addAuthStateListener(this);
    updateUI(AuthController.getInstance().isAuthenticated(),
        AuthController.getInstance().getCurrentUser());
    loadData();
    showCancel(false);
    setLoadingText("Loading data...");
  }

  private void updateUI(boolean isAuthenticated, User user) {
    if (isAuthenticated) {
      welcomeLabel.setText("Welcome, " + user.getEmail());
      favoriteBooksContainer.setVisible(true);
      favoriteBooksContainer.setManaged(true);
      if (logoutButton != null) {
        logoutButton.setVisible(true);
        logoutButton.setManaged(true);
      }
      loadFavoriteBooks();
    } else {
      welcomeLabel.setText("Welcome, Guest");
      favoriteBooksContainer.setVisible(false);
      favoriteBooksContainer.setManaged(false);
      if (logoutButton != null) {
        logoutButton.setVisible(false);
        logoutButton.setManaged(false);
      }
    }
  }

  private void loadData() {
    if (AuthController.getInstance().isAuthenticated()) {
      loadFavoriteBooks();
    }
    Task<List<ReturnRating>> topRatedTask = createTask(
        () -> BookRatingController.getTopRatingBook(0, 10));

    topRatedTask.setOnSucceeded(
        event -> displayReturnRating(topRatedScrollPane, topRatedTask.getValue()));

    topRatedTask.setOnFailed(event -> {
      topRatedTask.getException().printStackTrace();
      AlertDialog.showAlert("error", "Loading failed",
          "An error occurred while loading the top rated books", null);
    });

    new Thread(topRatedTask).start();

    Task<List<ReturnBookLoan>> topLoansTask = createTask(
        () -> BookLoanController.getTopLentBook(0, 10));

    topLoansTask.setOnSucceeded(
        event -> displayReturnLoan(topLoansScrollPane, topLoansTask.getValue()));

    topLoansTask.setOnFailed(event -> {
      topLoansTask.getException().printStackTrace();
      AlertDialog.showAlert("error", "Loading failed",
          "An error occurred while loading the top loans books", null);
    });

    new Thread(topLoansTask).start();
//
//    if (favoriteTask.isDone() || topRatedTask.isDone() || topLoansTask.isDone()) {
//      showLoading(false);
//    }
  }

  private void loadFavoriteBooks() {
    Task<List<Book>> favoriteTask = createTask(() -> FavoriteController.getFavoriteBookOfUser(
        AuthController.getInstance().getCurrentUser().getUid()));
    favoriteTask.setOnSucceeded(
        event -> {
          displayBooksToScrollPane(favoriteScrollPane, favoriteTask.getValue());
        });

    favoriteTask.setOnFailed(event -> {
      favoriteTask.getException().printStackTrace();
      AlertDialog.showAlert("error", "Loading failed",
          "An error occurred while loading the favorite books", null);
    });

    new Thread(favoriteTask).start();
  }

  private <T> Task<T> createTask(Callable<T> callable) {
    return new Task<>() {
      @Override
      protected T call() throws Exception {
        return callable.call();
      }
    };
  }

  private void displayBooksToScrollPane(ScrollPane scrollPane, List<Book> books) {
    HBox hBox = new HBox(10);
    hBox.getStyleClass().add("book-container");
    scrollPane.setContent(hBox);

    if (books == null || books.isEmpty()) {
      favoriteBooksContainer.setVisible(false);
      favoriteBooksContainer.setManaged(false);
      return;
    }

    try {
      for (Book book : books) {
        Task<VBox> task = new Task<>() {
          @Override
          protected VBox call() {
            try {
              FXMLLoader loader = new FXMLLoader(
                  getClass().getResource("/views/components/book.fxml"));
              VBox bookItem = loader.load();
              BookComponentController controller = loader.getController();

              controller.setBook(book);

              return bookItem;
            } catch (Exception e) {
              e.printStackTrace();
              return null;
            }
          }
        };

        task.setOnSucceeded(event -> {
          try {
            Platform.runLater(() -> {
              try {
                hBox.getChildren().add(task.getValue());
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

        task.setOnFailed(event -> {
          task.getException().printStackTrace();
          AlertDialog.showAlert("error", "Loading failed",
              "An error occurred while loading a book component", null);
        });

        new Thread(task).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void displayReturnRating(ScrollPane scrollPane, List<ReturnRating> ratings) {
    HBox hBox = new HBox(10);
    hBox.getStyleClass().add("book-container");
    scrollPane.setContent(hBox);

    try {
      for (ReturnRating rating : ratings) {
        Task<VBox> task = new Task<>() {
          @Override
          protected VBox call() {
            try {
              VBox bookItem = new VBox(10);
              bookItem.setOnMouseClicked(event -> handleBookClick(rating.getBookId(), bookItem));
              bookItem.getStyleClass().add("book-item");
              ImageView thumbnail = new ImageView(rating.getThumbnailBook());
              thumbnail.setFitWidth(200);
              thumbnail.setFitHeight(300);
              thumbnail.setPreserveRatio(true);
              Label title = new Label(rating.getTitleBook());
              title.setWrapText(true);
              title.getStyleClass().add("book-title");
              HBox starsContainer = new HBox(5);
              starsContainer.setAlignment(Pos.CENTER_LEFT);
              for (int i = 0; i < 5; i++) {
                FontIcon star = new FontIcon("antf-star");
                star.getStyleClass().add("star");
                if (rating.getBookRating().getRate() - i >= 0.51) {
                  starsContainer.getChildren().add(star);
                } else {
                  star.setIconLiteral("anto-star");
                  starsContainer.getChildren().add(star);
                }
              }
              starsContainer.getChildren()
                  .add(new Label("(" + rating.getBookRating().getRate() + ")"));

              VBox content = new VBox(5);
              content.getChildren().addAll(title, starsContainer);
              content.getStyleClass().add("book-content");
              bookItem.getChildren().addAll(thumbnail, content);
              return bookItem;
            } catch (Exception e) {
              e.printStackTrace();
              return null;
            }
          }
        };

        task.setOnSucceeded(event -> {
          try {
            Platform.runLater(() -> {
              try {
                hBox.getChildren().add(task.getValue());
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

        task.setOnFailed(event -> {
          task.getException().printStackTrace();
          AlertDialog.showAlert("error", "Loading failed",
              "An error occurred while loading a book component", null);
        });

        new Thread(task).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void displayReturnLoan(ScrollPane scrollPane, List<ReturnBookLoan> loans) {
    HBox hBox = new HBox(10);
    hBox.getStyleClass().add("book-container");
    scrollPane.setContent(hBox);

    try {
      for (ReturnBookLoan loan : loans) {
        Task<VBox> task = new Task<>() {
          @Override
          protected VBox call() {
            try {
              VBox bookItem = new VBox(10);
              bookItem.setOnMouseClicked(
                  event -> handleBookClick(loan.getBookLoan().getBookId(), bookItem));
              bookItem.getStyleClass().add("book-item");
              ImageView thumbnail = new ImageView(
                  loan.getThumbnailBook() == null || loan.getThumbnailBook().isEmpty()
                      || !loan.getThumbnailBook().startsWith("http")
                      ? "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"
                      : loan.getThumbnailBook());
              thumbnail.setFitWidth(200);
              thumbnail.setFitHeight(300);
              thumbnail.setPreserveRatio(true);
              Label title = new Label(loan.getTitleBook());
              title.setWrapText(true);
              title.getStyleClass().add("book-title");
              VBox content = new VBox(5);
              content.getChildren().addAll(title);
              content.getStyleClass().add("book-content");

              bookItem.getChildren().addAll(thumbnail, content);
              return bookItem;
            } catch (Exception e) {
              e.printStackTrace();
              return null;
            }
          }
        };

        task.setOnSucceeded(event -> {
          try {
            Platform.runLater(() -> {
              try {
                hBox.getChildren().add(task.getValue());
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

        task.setOnFailed(event -> {
          task.getException().printStackTrace();
          AlertDialog.showAlert("error", "Loading failed",
              "An error occurred while loading a book component", null);
        });

        new Thread(task).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleBookClick(String bookId, Parent container) {
    try {
      if (!AuthController.getInstance().isAuthenticated()) {
        AlertDialog.showAlert("error", "Unauthenticated",
            "You need to login to view the book details",
            e -> StageManager.showLoginWindow());
        return;
      }
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/book-detail.fxml"));
      Parent root = loader.load();
      BookDetailController controller = loader.getController();
      controller.getBookDetail(bookId);

      StackPane overlay = new StackPane(root);
      overlay.getStyleClass().add("overlay");
      StackPane stackPane = (StackPane) container.getScene().lookup("#contentPane");
      if (stackPane != null) {
        stackPane.getChildren().add(overlay);
        Button closeButton = (Button) root.lookup("#closeBtn");
        closeButton.setOnAction(event -> stackPane.getChildren().remove(overlay));
      } else {
        //  System.err.println("StackPane with id 'contentPane' not found.");
      }
    } catch (Exception e) {
      AlertDialog.showAlert("error", "Error", "Failed to show book", null);
      e.printStackTrace();
    }
  }


  @FXML
  private void handleLogout() {
    AuthController.getInstance().logout();
    // Reload home page after logout
    updateUI(false, null);
  }

  @Override
  public void onAuthStateChanged(boolean isAuthenticated) {
    updateUI(isAuthenticated, AuthController.getInstance().getCurrentUser());
  }
}

