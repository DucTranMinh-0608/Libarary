package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookLoan.Mode;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;
import org.bson.Document;

public class MyLoansController extends ControllerWithLoader {

  @FXML
  private ScrollPane loansScrollPane;
  @FXML
  private FlowPane loansFlowPane;
  @FXML
  private TextField searchField;
  @FXML
  private Label searchStatus;
  @FXML
  private ComboBox<String> validityFilter;
  @FXML
  private ComboBox<String> modeFilter;
  @FXML
  private ComboBox<Integer> pageSize;
  @FXML
  private Button prevPageButton;
  @FXML
  private Button nextPageButton;

  private int pageSizeValue = 10;
  private int currentPage = 0;
  private String validity = "All";
  private String mode = "All";
  private int totalResults = 0;

  private ObservableList<ReturnBookLoan> loans = FXCollections.observableArrayList();
  private PauseTransition pauseTransition;
  private List<Task<HBox>> renderTasks = new ArrayList<>();

  @FXML
  private void initialize() {
    validityFilter.getItems().addAll("All", "Valid", "Invalid");
    validityFilter.setValue(validity);
    pageSize.getItems().addAll(5, 10, 15, 20);
    pageSize.setValue(10);
    loansScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
      double deltaY = event.getDeltaY() * 3;
      loansScrollPane.setVvalue(
          loansScrollPane.getVvalue() - deltaY / loansScrollPane.getContent().getBoundsInLocal()
              .getHeight());
      event.consume();
    });
    loansScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
      loansFlowPane.setPrefWidth(newValue.getWidth());
    });
    loadLoans();
    pauseTransition = new PauseTransition(Duration.millis(200));
    pauseTransition.setOnFinished(event -> handleSearch());
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      pauseTransition.playFromStart();
    });
    showCancel(false);
    pageSize.valueProperty().addListener((observable, oldValue, newValue) -> {
      pageSizeValue = newValue;
      currentPage = 0;
      loadLoans();
    });

    nextPageButton.setOnAction(event -> {
      currentPage++;
      loadLoans();
    });

    prevPageButton.setOnAction(event -> {
      if (currentPage > 0) {
        currentPage--;
        loadLoans();
      }
    });

    validityFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      validity = newValue;
      loadLoans();
    });

    modeFilter.getItems().addAll("All", "Online", "Offline");
    modeFilter.setValue(mode);
    modeFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      mode = newValue;
      loadLoans();
    });
  }

  private void loadLoans() {
    Task<List<ReturnBookLoan>> task = new Task<>() {
      @Override
      protected List<ReturnBookLoan> call() {
        User currentUser = AuthController.getInstance().getCurrentUser();
        boolean isValid = "Valid".equals(validity) || "All".equals(validity);
        boolean isNotValid = "Invalid".equals(validity) || "All".equals(validity);
        boolean isOnline = "Online".equals(mode) || "All".equals(mode);
        boolean isOffline = "Offline".equals(mode) || "All".equals(mode);
        totalResults = (int) BookLoanController.countLoanWithFilterOfUser(currentUser.getUid(),
            isValid, isNotValid, isOnline,
            isOffline);
        nextPageButton.setDisable((currentPage + 1) * pageSizeValue >= totalResults);
        prevPageButton.setDisable(currentPage == 0);
        //  System.out.println("Total results: " + totalResults);
        return BookLoanController.getLoanWithFilterOfUser(currentUser.getUid(), isValid, isNotValid,
            isOnline, isOffline,
            currentPage * pageSizeValue, pageSizeValue);
      }
    };

    task.setOnRunning(event -> showLoading(true));

    task.setOnSucceeded(event -> {
      showLoading(false);
      searchStatus.setText(task.getValue().size() + " results found");
      loans.clear();
      loans.addAll(task.getValue());
      updateLoansFlowPane(loans);
    });

    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to load loans", null);
    });

    new Thread(task).start();
  }

  private void updateLoansFlowPane(ObservableList<ReturnBookLoan> loans) {
    renderTasks.forEach(Task::cancel);
    renderTasks.clear();
    loansFlowPane.getChildren().clear();
    for (ReturnBookLoan loan : loans) {
      Task<HBox> task = new Task<>() {
        @Override
        protected HBox call() {
          return createLoanCell(loan);
        }
      };
      task.setOnSucceeded(event -> loansFlowPane.getChildren().add(task.getValue()));
      task.setOnFailed(event -> {
        AlertDialog.showAlert("error", "Error", "Failed to load loans", null);
      });
      new Thread(task).start();

      renderTasks.add(task);
    }
    searchStatus.setText(loans.size() + " results found");
  }

  private HBox createLoanCell(ReturnBookLoan item) {
    HBox content = new HBox();
    content.getStyleClass().add("flowPane-cell");
    ImageView thumbnail = new ImageView();
    VBox details = new VBox(3);
    Text title = new Text();
    title.setWrappingWidth(280);
    Text dates = new Text();
    Label type = new Label();
    Label valid = new Label();
    Text numCopies = new Text();
    title.setOnMouseClicked(event -> handleBookLoanClick(item.getBookLoan().getBookId(), content));
    title.getStyleClass().add("link");
    HBox actionButtons = new HBox();
    actionButtons.setAlignment(Pos.CENTER_LEFT);
    Button returnButton = new Button("Return");
    Button reBorrowButton = new Button("Re-borrow");
    Button readButton = new Button("Read");

    reBorrowButton.getStyleClass().addAll("btn", "btn-default");
    returnButton.getStyleClass().addAll("btn", "btn-danger");
    readButton.getStyleClass().addAll("btn", "btn-primary");

    thumbnail.setImage(new Image(item.getThumbnailBook()));
    thumbnail.setFitHeight(180);
    thumbnail.setPreserveRatio(true);
    title.setText(item.getTitleBook());
    title.getStyleClass().add("bold");
    BookLoan loan = item.getBookLoan();
    dates.setText(DateUtil.dateToString(loan.getBorrowDate()) + " - " + DateUtil.dateToString(
        loan.getDueDate()));
    valid.setText(loan.isValid() ? "Valid" : "Expired");
    valid.getStyleClass().add("chip");
    valid.getStyleClass().add(loan.isValid() ? "success" : "danger");
    type.setText(loan.getType().name());
    type.getStyleClass().addAll("chip", loan.getType().name().toLowerCase());
    HBox chips = new HBox(valid, type);

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    if (loan.isValid()) {
      // Only allow online books to be returned by users
      // Physical books must be returned at the counter (admin will update)
      if (Mode.ONLINE.equals(loan.getType())) {
        returnButton.setVisible(true);
        returnButton.setOnAction(event -> handleReturnBook(item));
      } else {
        returnButton.setVisible(false);
        returnButton.setManaged(false);
      }
      reBorrowButton.setVisible(false);
      reBorrowButton.setManaged(false);
    } else {
      returnButton.setVisible(false);
      returnButton.setManaged(false);
      reBorrowButton.setVisible(true);
      reBorrowButton.setOnAction(
          event -> handleBookLoanClick(item.getBookLoan().getBookId(), content));
    }
    if (Mode.ONLINE.equals(loan.getType())) {
      readButton.setVisible(loan.isValid());
      readButton.setManaged(loan.isValid());
      readButton.setOnAction(event -> handleReadBook(item));
    } else {
      readButton.setVisible(false);
      readButton.setManaged(false);
    }

    actionButtons.getChildren().addAll(returnButton, reBorrowButton, readButton);
    actionButtons.setSpacing(5);
    details.getChildren().addAll(title, dates, chips, spacer, actionButtons);
    content.getChildren().addAll(thumbnail, details);
    content.setSpacing(10);
    chips.setSpacing(5);

    content.setUserData(loan);

    return content;
  }

  private void handleReturnBook(ReturnBookLoan item) {
    if (!AlertDialog.showConfirm("Return book", "Are you sure you want to return this book?")) {
      return;
    }

    BookLoan bookLoan = item.getBookLoan();
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        return BookLoanController.returnBook(bookLoan);
      }
    };

    setLoadingText("Returning book...");

    task.setOnSucceeded(event -> {
      AlertDialog.showAlert("success", "Success", "Book returned successfully", null);
      Document result = task.getValue();
      BookLoan bookLoanReturned = new BookLoan(result);
      item.setBookLoan(bookLoanReturned);
      updateLoanInFlowPane(item);
    });

    task.setOnFailed(event -> {
      AlertDialog.showAlert("error", "Error", "Failed to return book", null);
    });

    new Thread(task).start();
  }

  private void handleReBorrowBook(ReturnBookLoan item) {

  }

  private void handleReadBook(ReturnBookLoan item) {
    String bookId = item.getBookLoan().getBookId();
    Book b = BookController.findBookByID(bookId);
    try {
      if (!b.isActivated()) {
        AlertDialog.showAlert("error", "Error", "Book is inactive and not available for reading",
            null);
        return;
      }
      if (b.getPdfLink() == null || b.getPdfLink().isEmpty() || b.getPdfLink().equals("N/A")) {
        AlertDialog.showAlert("error", "Error", "No pdf link found", null);
        return;
      }
      Desktop.getDesktop().browse(URI.create(b.getPdfLink()));
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to open book", null);
    }
  }

  private void updateLoanInFlowPane(ReturnBookLoan bookLoan) {
    loansFlowPane.getChildren().removeIf(node -> {
      HBox loanCell = (HBox) node;
      BookLoan loan = (BookLoan) loanCell.getUserData();
      return loan.get_id().equals(bookLoan.getBookLoan().get_id());
    });
    loansFlowPane.getChildren().add(createLoanCell(
        new ReturnBookLoan(bookLoan.getBookLoan(), bookLoan.getTitleBook(),
            bookLoan.getThumbnailBook())));
    if ("Valid".equals(validity)) {
      loadLoans();
    }
  }

  private void handleBookLoanClick(String id, Parent container) {
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

  private void handleSearch() {
    searchStatus.setText("Searching...");
    String query = searchField.getText().trim();
    if (query.isEmpty()) {
      updateLoansFlowPane(loans);
      return;
    }

    FilteredList<ReturnBookLoan> filteredLoans = loans.filtered(
        loan -> loan.getTitleBook().toLowerCase().contains(query.toLowerCase()));
    searchStatus.setText(filteredLoans.size() + " results found");
    updateLoansFlowPane(filteredLoans);
  }
}