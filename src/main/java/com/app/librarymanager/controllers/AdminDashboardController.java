package com.app.librarymanager.controllers;


import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class AdminDashboardController extends ControllerWithLoader {

  private int totalBooks;
  private int totalCategories;
  private int totalUsers;
  private int activeBooks;
  private List<ReturnBookLoan> topLentBooks;
  private ObservableList<ReturnBookLoan> recentLoansList = FXCollections.observableArrayList();

  @FXML
  private Text totalBooksCount;
  @FXML
  private Text totalCategoriesCount;
  @FXML
  private Text totalUsersCount;
  @FXML
  private Text activeBooksCount;
  @FXML
  private ListView<ReturnBookLoan> topLentBooksList;
  @FXML
  public ListView<ReturnBookLoan> recentLoans;

  @FXML
  private ScrollPane adminScrollPane;
  @FXML
  private FlowPane adminFlowPane;

  @FXML
  private void initialize() {
    showCancel(false);
    adminScrollPane.viewportBoundsProperty()
        .addListener((observable, oldValue, newValue) -> {
          adminFlowPane.setPrefWidth(newValue.getWidth());
        });
    setLoadingText("Loading dashboard...");
    loadStats();
    recentLoans.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(ReturnBookLoan bookLoan, boolean empty) {
        super.updateItem(bookLoan, empty);
        if (empty || bookLoan == null) {
          setGraphic(null);
        } else {
          Task<HBox> renderTask = new Task<>() {
            @Override
            protected HBox call() {
              return createRecentLoanComponent(bookLoan);
            }
          };

          renderTask.setOnSucceeded(event -> setGraphic(renderTask.getValue()));
          renderTask.setOnFailed(event -> {
            //  System.out.println("Failed to render item for: " + bookLoan.getTitleBook());
            setGraphic(null);
          });

          new Thread(renderTask).start();
        }
      }
    });
    topLentBooksList.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(ReturnBookLoan bookLoan, boolean empty) {
        super.updateItem(bookLoan, empty);
        if (empty || bookLoan == null) {
          setGraphic(null);
        } else {
          Task<HBox> renderTask = new Task<>() {
            @Override
            protected HBox call() {
              return createTopLentBookComponent(bookLoan);
            }
          };

          renderTask.setOnSucceeded(event -> setGraphic(renderTask.getValue()));
          renderTask.setOnFailed(event -> {
            //  System.out.println("Failed to render item for: " + bookLoan.getTitleBook());
            setGraphic(null);
          });

          new Thread(renderTask).start();
        }
      }
    });
  }

  private void loadStats() {
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() {
        totalBooks = (int) BookController.numberOfBooks();
        System.out.println("Total books: " + totalBooks);
        totalCategories = (int) CategoriesController.countCategories();
        System.out.println("Total categories: " + totalCategories);
        totalUsers = UserController.countTotalUser();
        System.out.println("Total users: " + totalUsers);
        activeBooks = (int) BookController.numberOfActiveBooks();
        System.out.println("Active books: " + activeBooks);
        System.out.println("Top lent books: " + BookLoanController.getTopLentBook(0, 10).toString());
        topLentBooks = BookLoanController.getTopLentBook(0, 10);
        recentLoansList.addAll(BookLoanController.getRecentLoan(0, 10));
        System.out.println("Recent loans: " + recentLoansList.toString());
        return null;
      }
    };
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      totalBooksCount.setText(String.format("%d", totalBooks));
      totalCategoriesCount.setText(String.format("%d", totalCategories));
      totalUsersCount.setText(String.format("%d", totalUsers));
      activeBooksCount.setText(String.format("%d", activeBooks));

      if (topLentBooks != null) {
        renderTopLentBooks();
      } else {
        topLentBooksList.setItems(FXCollections.observableArrayList());
      }

      recentLoans.setItems(
          Objects.requireNonNullElseGet(recentLoansList, FXCollections::observableArrayList));
    });
    task.setOnFailed(event -> {
      showLoading(false);
      totalBooksCount.setText("-");
      totalCategoriesCount.setText("-");
      totalUsersCount.setText("-");
      activeBooksCount.setText("-");
      System.err.println("Failed to load dashboard: " + task.getException().getMessage());
      AlertDialog.showAlert("error", "Error", "Failed to load dashboard", null);
    });
    new Thread(task).start();
  }

  private void renderTopLentBooks() {
    topLentBooksList.setItems(FXCollections.observableArrayList(topLentBooks));
  }


  private HBox createTopLentBookComponent(ReturnBookLoan returnBookLoan) {
    HBox bookComponent = new HBox(10);
    ImageView bookImage = new ImageView(
        !returnBookLoan.getThumbnailBook().isEmpty() && returnBookLoan
            .getThumbnailBook().startsWith("http") ? returnBookLoan.getThumbnailBook()
            : "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
    bookImage.setFitWidth(100);
    bookImage.setPreserveRatio(true);
    Text bookTitle = new Text(returnBookLoan.getTitleBook());
    bookTitle.setWrappingWidth(600);
    bookTitle.getStyleClass().addAll("bold", "link");
    bookTitle.setOnMouseClicked(
        e -> handleShowBook(returnBookLoan.getBookLoan().getBookId(), bookComponent));
    BookLoan bookLoan = returnBookLoan.getBookLoan();
    VBox bookInfo = new VBox(bookTitle);
    bookComponent.getChildren().addAll(bookImage, bookInfo);
    return bookComponent;
  }

  private HBox createRecentLoanComponent(ReturnBookLoan returnBookLoan) {
    HBox bookComponent = new HBox(10);
    ImageView bookImage = new ImageView(
        !returnBookLoan.getThumbnailBook().isEmpty() && returnBookLoan.getThumbnailBook()
            .startsWith("http") ? returnBookLoan.getThumbnailBook()
            : "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
    bookImage.setFitWidth(100);
    bookImage.setPreserveRatio(true);
    Text bookTitle = new Text(returnBookLoan.getTitleBook());
    bookTitle.setWrappingWidth(600);
    bookTitle.getStyleClass().addAll("bold", "link");
    bookTitle.setOnMouseClicked(
        e -> handleShowBook(returnBookLoan.getBookLoan().getBookId(), bookComponent));
    BookLoan bookLoan = returnBookLoan.getBookLoan();
    Label borrowDate = new Label("Borrowed on: " + DateUtil.dateToString(bookLoan.getBorrowDate()));
    Label dueDate = new Label("Due Date: " + DateUtil.dateToString(bookLoan.getDueDate()));
    Label borrowBy = new Label("Borrowed by: " + returnBookLoan.getBookLoan().getUserId());
    VBox bookInfo = new VBox(bookTitle, borrowDate, dueDate, borrowBy);
    bookComponent.getChildren().addAll(bookImage, bookInfo);
    return bookComponent;
  }

  private void handleShowBook(String bookId, Parent container) {
    try {

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

}