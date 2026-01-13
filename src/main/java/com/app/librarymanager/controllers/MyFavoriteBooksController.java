package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookLoan.Mode;
import com.app.librarymanager.models.BookUser;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class MyFavoriteBooksController extends ControllerWithLoader {

  @FXML
  private FlowPane favoriteBooksFlowPane;
  @FXML
  private ScrollPane favoriteScrollPane;
  @FXML
  private TextField searchField;
  @FXML
  private Label searchStatus;

  private ObservableList<Book> favoriteBooks = FXCollections.observableArrayList();

  private PauseTransition pauseTransition;
  private Task<List<Book>> searchTask;
  private List<Task<HBox>> renderTasks = new ArrayList<>();

  @FXML
  private void initialize() {
    showCancel(false);
    favoriteScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
      double deltaY = event.getDeltaY() * 3;
      favoriteScrollPane.setVvalue(
          favoriteScrollPane.getVvalue() - deltaY / favoriteScrollPane.getContent()
              .getBoundsInLocal().getHeight());
      event.consume();
    });
    favoriteScrollPane.viewportBoundsProperty()
        .addListener((observable, oldValue, newValue) -> {
          favoriteBooksFlowPane.setPrefWidth(newValue.getWidth());
        });
    loadFavoriteBooks();
    pauseTransition = new PauseTransition(Duration.millis(200));
    pauseTransition.setOnFinished(event -> handleSearch());
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      pauseTransition.playFromStart();
    });
    Platform.runLater(() -> searchField.requestFocus());
  }

  private void loadFavoriteBooks() {

    Task<List<Book>> task = new Task<List<Book>>() {
      @Override
      protected List<Book> call() {
        User user = AuthController.getInstance().getCurrentUser();
        return FavoriteController.getFavoriteBookOfUser(user.getUid());
      }
    };
    setLoadingText("Loading favorite books...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      searchStatus.setText(task.getValue().size() + " results found");
      favoriteBooks.clear();
      favoriteBooks.addAll(task.getValue());
      favoriteBooksFlowPane.getChildren().clear();
      favoriteBooks.forEach(book -> {
        Task<HBox> bookTask = new Task<HBox>() {
          @Override
          protected HBox call() {
            return createBookCell(book);
          }
        };
        bookTask.setOnSucceeded(e -> favoriteBooksFlowPane.getChildren().add(bookTask.getValue()));
        new Thread(bookTask).start();
      });
    });
    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
    });
    new Thread(task).start();
  }

  private HBox createBookCell(Book book) {
    HBox content = new HBox();
    content.getStyleClass().add("flowPane-cell");
    ImageView thumbnail = new ImageView();
    VBox details = new VBox(3);
    Text title = new Text();
    title.setWrappingWidth(280);
    Text authors = new Text();
    authors.setText("Authors: " + book.getAuthors().toString().replaceAll("[\\[\\]]", ""));
    authors.setWrappingWidth(280);
    Text publishedDate = new Text();
    publishedDate.setText("Published Date: " + DateUtil.ymdToDmy(book.getPublishedDate()));
    Text publisher = new Text();
    publisher.setText("Publisher: " + book.getPublisher());
    publisher.setWrappingWidth(280);
    HBox actionButtons = new HBox();
    actionButtons.setAlignment(Pos.CENTER_LEFT);
    Button heartButton = new Button();
    heartButton.setGraphic(new FontIcon("antf-heart"));
    heartButton.setOnAction(event -> handleRemoveFromFavorite(book));
    heartButton.getStyleClass().addAll("btn", "btn-icon", "heart", "on");
    if (!book.getThumbnail().isEmpty() && book.getThumbnail().startsWith("http")) {
      thumbnail.setImage(new Image(book.getThumbnail()));
    } else {
      thumbnail.setImage(new Image(
          "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api"));
    }
    thumbnail.setFitHeight(200);
    thumbnail.setPreserveRatio(true);
    title.setText(book.getTitle());
    title.getStyleClass().addAll("bold", "link");
    title.setOnMouseClicked(event -> handleFavoriteBookClick(book.getId(), content));

    actionButtons.getChildren().addAll(heartButton);
    actionButtons.setSpacing(5);
    details.getChildren().addAll(title, authors, publisher, publishedDate, actionButtons);
    content.getChildren().addAll(thumbnail, details);
    content.setSpacing(10);

    content.setUserData(book);

    return content;
  }

  private void handleRemoveFromFavorite(Book book) {
    Task<Boolean> task = new Task<Boolean>() {
      @Override
      protected Boolean call() {
        User user = AuthController.getInstance().getCurrentUser();
        return FavoriteController.removeFromFavorite(new BookUser(user.getUid(), book.getId()));
      }
    };
    task.setOnRunning(event -> {
      showLoading(true);
      searchField.setDisable(true);
    });
    task.setOnSucceeded(event -> {
      showLoading(false);
      searchField.setDisable(false);
      if (task.getValue()) {
        favoriteBooks.remove(book);
        favoriteBooksFlowPane.getChildren().removeIf(node -> node.getUserData().equals(book));
        searchStatus.setText(favoriteBooks.size() + " results found");
      } else {
        AlertDialog.showAlert("error", "Error", "Failed to remove book from favorite", null);
      }
    });
    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to remove book from favorite", null);
    });
    new Thread(task).start();
  }

  private void handleSearch() {
    searchStatus.setText("Searching...");
    String query = searchField.getText().trim();
    if (query.isEmpty()) {
      favoriteBooksFlowPane.getChildren().clear();
      loadFavoriteBooks();
      return;
    }

    FilteredList<Book> filteredBooks = favoriteBooks.filtered(book -> {
      return book.getTitle().toLowerCase().contains(query.toLowerCase());
    });

    searchStatus.setText(filteredBooks.size() + " results found");

    favoriteBooksFlowPane.getChildren().clear();

    renderTasks.forEach(Task::cancel);

    renderTasks.clear();

    filteredBooks.forEach(book -> {
      Task<HBox> bookTask = new Task<HBox>() {
        @Override
        protected HBox call() {
          return createBookCell(book);
        }
      };
      bookTask.setOnSucceeded(e -> favoriteBooksFlowPane.getChildren().add(bookTask.getValue()));
      renderTasks.add(bookTask);
      new Thread(bookTask).start();
    });
  }

  private void handleFavoriteBookClick(String id, Parent container) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/book-detail.fxml"));
      Parent root = loader.load();
      BookDetailController controller = loader.getController();
      controller.getBookDetail(id);

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
      e.printStackTrace();
    }
  }
}