package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.CommentController.ReturnUserComment;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookRating;
import com.app.librarymanager.models.BookUser;
import com.app.librarymanager.models.Comment;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.AvatarUtil;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.StageManager;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import org.bson.Document;
import org.kordamp.ikonli.javafx.FontIcon;

public class BookDetailController extends ControllerWithLoader {

  private Book book;
  @FXML
  private VBox detailContainer;
  @FXML
  private ImageView bookCover;
  @FXML
  private Label bookTitle;
  @FXML
  private Label bookAuthor;
  @FXML
  private TextArea bookDescription;
  @FXML
  private Label bookPublisher;
  @FXML
  private FlowPane bookCategories;
  @FXML
  private Label bookLanguage;
  @FXML
  private Label bookPublishedDate;
  @FXML
  private Label bookIsbn;
  @FXML
  private Button closeBtn;
  @FXML
  private Label bookPublishingInfo;
  @FXML
  private Text bookPrice;
  @FXML
  private Text bookDiscountPrice;
  @FXML
  private Text currencyCode;
  @FXML
  private HBox starsContainer;
  @FXML
  private HBox userRatingContainer;
  @FXML
  private Button addToFavorite;
  @FXML
  private Button borrowEBook;
  @FXML
  private Button borrowPhysicalBook;
  @FXML
  private Text physicalCopiesText;
  @FXML
  private ListView<ReturnUserComment> commentsContainer;
  @FXML
  private TextArea newCommentTextArea;
  @FXML
  private Button addCommentButton;
  @FXML
  private Label emptyComments;


  private List<ReturnUserComment> commentList = new ArrayList<>();

  private boolean isFavorite = false;

  @FXML
  private void initialize() {
    showCancel(false);
    borrowEBook.setOnAction(event -> handleBorrowEBook());
    borrowPhysicalBook.setOnAction(event -> handleBorrowPhysicalBook());
    addToFavorite.setOnAction(event -> handleAddToFavorite());
//    detailContainer.setVisible(false);
    addCommentButton.setOnAction(e -> handleAddComment());
    commentsContainer.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(ReturnUserComment comment, boolean empty) {
        super.updateItem(comment, empty);
        if (empty || comment == null) {
          setGraphic(null);
        } else {
          Task<VBox> renderTask = new Task<>() {
            @Override
            protected VBox call() {
              return createCommentComponent(comment);
            }
          };

          renderTask.setOnSucceeded(event -> setGraphic(renderTask.getValue()));
          renderTask.setOnFailed(event -> {
            //  System.out.println("Failed to render comment for: " + comment.getUserDisplayName());
            setGraphic(null);
          });

          new Thread(renderTask).start();
        }
      }
    });
  }

  private Stage loadingStage;

  void getBookDetail(String id) {
    Task<Map<String, Object>> task = new Task<>() {
      @Override
      protected Map<String, Object> call() {
        Book b = BookController.findBookByID(id);
        boolean isFavorite = FavoriteController.findFavorite(
            new BookUser(AuthController.getInstance().getCurrentUser().getUid(), id)) != null;
        double avgRating = BookRatingController.averageRating(id);
        commentList = CommentController.getAllCommentOfBook(id);

        BookRating searchRating = new BookRating();
        searchRating.setBookId(id);
        searchRating.setUserId(AuthController.getInstance().getCurrentUser().getUid());
        Document ratingDoc = BookRatingController.findRating(searchRating);
        double userRating = ratingDoc != null ? ratingDoc.getDouble("rate") : 0.0;

        Document copyDoc = BookCopiesController.findCopy(new BookCopies(id));
        int physicalCopies = copyDoc != null ? copyDoc.getInteger("copies") : 0;

        return Map.of(
            "book", b,
            "isFavorite", isFavorite,
            "avgRating", avgRating,
            "userRating", userRating,
            "physicalCopies", physicalCopies
        );
      }
    };

    task.setOnRunning(event -> showLoading(true));

    task.setOnSucceeded(event -> {
      showLoading(false);
      Map<String, Object> result = task.getValue();

      Platform.runLater(() -> {
        book = (Book) result.get("book");
        isFavorite = (boolean) result.get("isFavorite");
        double avgRating = (double) result.get("avgRating");
        double userRating = (double) result.get("userRating");
        int physicalCopies = (int) result.get("physicalCopies");
        updateBookDetailsUI(book, avgRating, userRating, physicalCopies);
      });

      new Thread(() -> loadCommentsInBatches(commentList)).start();
    });

    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Book not found", "Failed to load book details", null);
    });

    new Thread(task).start();
  }

  private void updateBookDetailsUI(Book book, double avgRating, double userRating, int physicalCopies) {
    if (book == null) {
      AlertDialog.showAlert("error", "Book not found", "No details available for this book.", null);
      return;
    }

    bookTitle.setText(book.isActivated() ? book.getTitle() : "[INACTIVE] " + book.getTitle());
    bookAuthor.setText("by " + String.join(", ", book.getAuthors()));
    bookPublishingInfo.setText("Published by " + book.getPublisher() + " on " +
        DateUtil.ymdToDmy(book.getPublishedDate()));
    bookDescription.setText(book.getDescription());
    bookLanguage.setText(book.getLanguage());
    bookPrice.setText(parsePrice(book.getPrice()));
    currencyCode.setText(book.getCurrencyCode());

    if (book.getDiscountPrice() > 0) {
      bookDiscountPrice.setText(parsePrice(book.getDiscountPrice()));
      bookPrice.getStyleClass().add("small-strike");
    } else {
      bookDiscountPrice.setVisible(false);
    }

    addToFavorite.setGraphic(isFavorite ? new FontIcon("antf-heart") : new FontIcon("anto-heart"));
    addToFavorite.getStyleClass().add(isFavorite ? "on" : "off");
    if (!book.isActivated()) {
      detailContainer.setStyle("-fx-opacity: 0.5;");
      borrowEBook.setDisable(true);
      borrowPhysicalBook.setDisable(true);
      addToFavorite.setDisable(true);
      newCommentTextArea.setDisable(true);
      addCommentButton.setDisable(true);
      AlertDialog.showAlert("warning", "Book is inactive",
          "This book is currently inactive and cannot be borrowed", null);
    }

    physicalCopiesText.setText("Stock: " + physicalCopies);
    if (physicalCopies <= 0) {
      borrowPhysicalBook.setDisable(true);
    }

    for (int i = 0; i < 5; i++) {
      FontIcon star = new FontIcon("antf-star");
      star.getStyleClass().add("star");
      if (avgRating - i >= 0.51) {
        starsContainer.getChildren().add(star);
      } else {
        star.setIconLiteral("anto-star");
        starsContainer.getChildren().add(star);
      }
    }
    starsContainer.getChildren().add(new Label("(" + String.format("%.1f", avgRating) + ")"));

    renderUserRating(userRating);
    Task<Image> imageTask = new Task<>() {
      @Override
      protected Image call() {
        String thumbUrl = book.getThumbnail();
        if (thumbUrl != null && !thumbUrl.isEmpty() && thumbUrl.startsWith("http")) {
          return new Image(thumbUrl);
        }
        try {
          return new Image("https://books.google.com/books/content?id=" + book.getId() +
              "&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
        } catch (Exception e) {
          return new Image(
              "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
        }
      }
    };
    imageTask.setOnSucceeded(event -> bookCover.setImage(imageTask.getValue()));
    new Thread(imageTask).start();
  }

  private void loadCommentsInBatches(List<ReturnUserComment> comments) {
    ObservableList<ReturnUserComment> observableComments = FXCollections.observableArrayList();
    commentsContainer.setItems(observableComments);

    if (comments == null || comments.isEmpty()) {
      Platform.runLater(() -> {
        emptyComments.setVisible(true);
        commentsContainer.setVisible(false);
        commentsContainer.setManaged(false);
      });
      return;
    } else {
      Platform.runLater(() -> {
        emptyComments.setVisible(false);
        commentsContainer.setVisible(true);
        commentsContainer.setManaged(true);
      });
    }

    commentsContainer.getItems().clear();

    for (int i = 0; i < comments.size(); i++) {
      int index = i;
      Platform.runLater(() -> observableComments.add(comments.get(index)));

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }


  private String parsePrice(double price) {
    return String.format("%,.0f", price);
  }

  private void handleBorrowEBook() {
    System.out.println("Borrowing E-Book");
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        Platform.runLater(() -> {
          LocalDate borrowDate = LocalDate.now();
          LocalDate dueDate = borrowDate.plusDays(90);
          BookLoan bookLoan = new BookLoan(AuthController.getInstance().getCurrentUser().getUid(),
              book.getId(), borrowDate, dueDate);
          System.out.println("Borrowing E-Book: " + bookLoan.toString());
          boolean confirm = AlertDialog.showConfirm("Borrow E-Book",
              "Are you sure you want to borrow this E-Book?");
          if (!confirm) {
            return;
          }
          Document doc = BookLoanController.addLoan(bookLoan);
          if (doc != null) {
            AlertDialog.showAlert("success", "E-Book Borrowed Successfully",
                "You have successfully borrowed the E-Book", null);
          } else {
            AlertDialog.showAlert("error", "Failed to Borrow E-Book",
                "An error occurred while borrowing the E-Book", null);
          }
        });
        return null;
      }
    };
    setLoadingText("Borrowing E-Book...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> showLoading(false));
    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Failed to Borrow E-Book",
          "An error occurred while borrowing the E-Book", null);
    });
    new Thread(task).start();
  }

  private void handleBorrowPhysicalBook() {
    System.out.println("Borrowing Physical Book");
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        Platform.runLater(() -> {
          Document copyDoc = BookCopiesController.findCopy(new BookCopies(book.getId()));
          int physicalCopies = copyDoc != null ? copyDoc.getInteger("copies") : 0;

          if (physicalCopies <= 0) {
            AlertDialog.showAlert("error", "Out of Stock",
                "This physical book is currently out of stock", null);
            borrowPhysicalBook.setDisable(true);
            return;
          }

          LocalDate borrowDate = LocalDate.now();
          LocalDate dueDate = borrowDate.plusDays(14); // 2 weeks for physical loan
          BookLoan bookLoan = new BookLoan(AuthController.getInstance().getCurrentUser().getUid(),
              book.getId(), borrowDate, dueDate, 1);
          bookLoan.setType(BookLoan.Mode.OFFLINE);

          boolean confirm = AlertDialog.showConfirm("Confirm Book Borrowing",
              "Are you sure you want to borrow this physical book?");
          if (!confirm) {
            return;
          }

          Document doc = BookLoanController.addLoan(bookLoan);
          if (doc != null) {
            AlertDialog.showAlert("success", "Book Borrowed Successfully",
                "Book borrowed successfully, please come to the counter to pick it up",
                null);
            physicalCopiesText.setText("Stock: " + (physicalCopies - 1));
            if (physicalCopies - 1 <= 0) {
              borrowPhysicalBook.setDisable(true);
            }
          } else {
            AlertDialog.showAlert("error", "Failed to borrow Physical Book",
                "An error occurred while borrowing the physical book", null);
          }
        });
        return null;
      }
    };
    setLoadingText("Borrowing Physical Book...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> showLoading(false));
    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Failed to borrow Physical Book",
          "An error occurred while borrowing the physical book", null);
    });
    new Thread(task).start();
  }

  private void handleAddToFavorite() {
    boolean success;
    if (isFavorite) {
      success = FavoriteController.removeFromFavorite(
          new BookUser(AuthController.getInstance().getCurrentUser().getUid(), book.getId()));
    } else {
      Document favorite = FavoriteController.addToFavorite(
          new BookUser(AuthController.getInstance().getCurrentUser().getUid(), book.getId()));
      success = favorite != null;
    }
    if (success) {
      isFavorite = !isFavorite;
      addToFavorite.setGraphic(
          isFavorite ? new FontIcon("antf-heart") : new FontIcon("anto-heart"));
      addToFavorite.getStyleClass().add(isFavorite ? "on" : "off");
      addToFavorite.getStyleClass().removeAll(isFavorite ? "off" : "on");
    } else {
      AlertDialog.showAlert("error", "Failed to add to favorite",
          "An error occurred while adding the book to favorite", null);
    }
  }

  private VBox createCommentComponent(ReturnUserComment comment) {
    String user = comment.getUserDisplayName().isEmpty()
        ? comment.getUserEmail()
        : comment.getUserDisplayName();
    String photoUrl = comment.getUserPhotoUrl();
    String content = comment.getContent();

    VBox commentBox = new VBox(10);
    commentBox.getStyleClass().add("comment-box");
    ImageView userAvatar = new ImageView();
    userAvatar.setFitHeight(30);
    userAvatar.setFitWidth(30);
    userAvatar.setSmooth(true);
    userAvatar.setPickOnBounds(true);
    userAvatar.setPreserveRatio(true);
    userAvatar.getStyleClass().add("comment-avatar");

    Circle clip = new Circle(15);
    clip.setCenterX(15);
    clip.setCenterY(15);
    userAvatar.setClip(clip);
    if (photoUrl != null && !photoUrl.isEmpty()) {
      userAvatar.setImage(new Image(photoUrl));
    } else {
      userAvatar.setImage(new Image(new AvatarUtil().setRounded(true).getAvatarUrl(user)));
    }

    Label userLabel = new Label(user);
    userLabel.getStyleClass().add("comment-user");
    Label contentLabel = new Label(content);
    contentLabel.getStyleClass().add("comment-content");
    HBox commentHeader = new HBox(10, userAvatar, userLabel);
    commentHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    commentBox.getChildren().addAll(
        commentHeader,
        contentLabel
    );
    return commentBox;
  }

  private void handleAddComment() {
    String content = newCommentTextArea.getText().trim();
    if (content.isEmpty()) {
      AlertDialog.showAlert("error", "Empty Comment", "Comment cannot be empty", null);
      return;
    }
    Comment newComment = new Comment(
        AuthController.getInstance().getCurrentUser().getUid(),
        book.getId(),
        content
    );
    try {
      Document result = CommentController.addComment(newComment);
      if (result != null) {
        AlertDialog.showAlert("success", "Comment Added", "Your comment has been added", null);
        newCommentTextArea.clear();

        ReturnUserComment userComment = new ReturnUserComment(
            AuthController.getInstance().getCurrentUser().getDisplayName(),
            AuthController.getInstance().getCurrentUser().getEmail(),
            AuthController.getInstance().getCurrentUser().getPhotoUrl(),
            content
        );
        if (!(commentList instanceof ArrayList)) {
          commentList = new ArrayList<>(commentList);
        }
        commentList.add(userComment);
        commentsContainer.getItems().add(userComment);

        commentsContainer.scrollTo(commentsContainer.getItems().size() - 1);
        commentsContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        if (!commentsContainer.isVisible()) {
          commentsContainer.setVisible(true);
          commentsContainer.setManaged(true);
          emptyComments.setVisible(false);
        }
      } else {
        AlertDialog.showAlert("error", "Failed to Add Comment",
            "An error occurred while adding your comment", null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Failed to Add Comment",
          "An error occurred while adding your comment", null);
    }
  }

  private void renderUserRating(double userRating) {
    userRatingContainer.getChildren().clear();
    for (int i = 1; i <= 5; i++) {
      FontIcon star = new FontIcon();
      star.getStyleClass().add("star");
      star.setIconSize(24);
      star.getStyleClass().add("clickable-star"); // Add this class for hover effects if needed

      if (i <= userRating) {
        star.setIconLiteral("antf-star");
        star.getStyleClass().add("active"); // Optional: logic for filled star color
      } else {
        star.setIconLiteral("anto-star");
      }

      int finalI = i;
      star.setOnMouseClicked(event -> handleRateBook(finalI));
      userRatingContainer.getChildren().add(star);
    }
  }

  private void handleRateBook(int rating) {
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        BookRating bookRating = new BookRating();
        bookRating.setBookId(book.getId());
        bookRating.setUserId(AuthController.getInstance().getCurrentUser().getUid());
        bookRating.setRate(rating);
        BookRatingController.addRating(bookRating);
        
        // Recalculate average
        double newAvg = BookRatingController.averageRating(book.getId());
        
        Platform.runLater(() -> {
             // Re-render average stars
             starsContainer.getChildren().clear();
             for (int i = 0; i < 5; i++) {
                FontIcon star = new FontIcon("antf-star");
                star.getStyleClass().add("star");
                if (newAvg - i >= 0.51) {
                  starsContainer.getChildren().add(star);
                } else {
                  star.setIconLiteral("anto-star");
                  starsContainer.getChildren().add(star);
                }
             }
             starsContainer.getChildren().add(new Label("(" + String.format("%.1f", newAvg) + ")"));
             
             // Update user rating stars
             renderUserRating(rating);
             
             AlertDialog.showAlert("success", "Rated!", "You rated this book " + rating + " stars.", null);
        });
        return null;
      }
    };
    new Thread(task).start();
  }

  @FXML
  private void close() {
    System.out.println("Closing book detail");
    // remove this view from the stack
  }
}
