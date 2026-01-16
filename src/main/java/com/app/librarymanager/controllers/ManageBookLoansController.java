package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.BookLoanUser;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.DateUtil;
import java.util.Date;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import com.app.librarymanager.models.BookLoan;

public class ManageBookLoansController extends ControllerWithLoader {

  @FXML
  private Pagination pagination;
  @FXML
  private TableView<BookLoanUser> bookLoansTable;
  @FXML
  private TableColumn<BookLoanUser, String> _idColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userIdColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userDisplayNameColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userAvatarColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userEmailColumn;
  @FXML
  private TableColumn<BookLoanUser, String> bookIdColumn;
  @FXML
  private TableColumn<BookLoanUser, String> bookTitleColumn;
  @FXML
  private TableColumn<BookLoanUser, String> bookThumbnailColumn;
  @FXML
  private TableColumn<BookLoanUser, String> borrowDateColumn;
  @FXML
  private TableColumn<BookLoanUser, String> dueDateColumn;
  @FXML
  private TableColumn<BookLoanUser, Boolean> validColumn;
  @FXML
  private TableColumn<BookLoanUser, String> typeColumn;
  @FXML
  private TableColumn<BookLoanUser, String> createdAtColumn;
  @FXML
  private TableColumn<BookLoanUser, String> lastUpdatedColumn;

  private int currentPage = 0;
  @FXML
  private ComboBox<String> pageSizeInput;
  private int pageSize = 10;
  private int totalRecords = 0;

  @FXML
  private TextField searchField;

  @FXML
  private VBox loadingOverlay;
  @FXML
  private ProgressIndicator loadingSpinner;
  @FXML
  private Text loadingText;
  @FXML
  private Button cancelButton;

  private ObservableList<BookLoanUser> bookLoansList = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    showCancel(false);
    setLoadingText("Loading book loans...");

   _idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getBookLoan().get_id().toString()));
userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getUser() != null ? cellData.getValue().getUser().getUid() : "[REMOVED USER]"));
userDisplayNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getUser() != null ? cellData.getValue().getUser().getDisplayName() : "[REMOVED USER]"));
userAvatarColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getUser() != null ? cellData.getValue().getUser().getPhotoUrl() : ""));
userEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getUser() != null ? cellData.getValue().getUser().getEmail() : "[REMOVED USER]"));
bookIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getBook() != null ? cellData.getValue().getBook().getId() : "[REMOVED BOOK]"));
bookTitleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getBook() != null ? cellData.getValue().getBook().getTitle() : "[REMOVED BOOK]"));
bookThumbnailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getBook() != null ? cellData.getValue().getBook().getThumbnail() : ""));
borrowDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    DateUtil.dateToString(cellData.getValue().getBookLoan().getBorrowDate())));
dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    DateUtil.dateToString(cellData.getValue().getBookLoan().getDueDate())));
validColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(
    cellData.getValue().getBookLoan().isValid()));
typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    cellData.getValue().getBookLoan().getType().name()));
createdAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    DateUtil.convertToStringFrom(
        (cellData.getValue().getBookLoan().get_id().toString()))));
lastUpdatedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    DateUtil.dateToString(cellData.getValue().getBookLoan().getLastUpdated())));

    setTypeCellFactory(typeColumn);
    setImageCellFactory(bookThumbnailColumn);

    setImageCellFactory(userAvatarColumn);
    userDisplayNameColumn.setPrefWidth(150);
    userEmailColumn.setPrefWidth(150);
    bookTitleColumn.setPrefWidth(150);
    userIdColumn.setPrefWidth(200);
    bookIdColumn.setPrefWidth(200);

    loadBookLoans();

    pagination.setPageFactory(index -> {
      currentPage = index;
      loadBookLoans();
      return new Label();
    });

    pageSizeInput.getItems().addAll("10", "20", "50", "100");
    pageSizeInput.setValue("10");
    pageSizeInput.setOnAction(e -> {
      pageSize = Integer.parseInt(pageSizeInput.getValue());
      loadBookLoans();
    });

    setRowContextMenu();
  }

  private void loadBookLoans() {
    Task<List<BookLoanUser>> task = new Task<>() {
      @Override
      protected List<BookLoanUser> call() {
        totalRecords = (int) BookLoanController.numberOfRecords();
        return BookLoanController.getAllLentBook(currentPage * pageSize, pageSize);
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      bookLoansList = FXCollections.observableArrayList(task.getValue());
      bookLoansTable.setItems(bookLoansList);
      //  System.out.println("Book loans loaded successfully. Total: " + totalRecords);

      pagination.setPageCount((int) Math.ceil((double) totalRecords / pageSize));
      pagination.setCurrentPageIndex(currentPage);

      // Wait for images to load before hiding loading overlay
      // Count total images that need to be loaded
      int totalImages = bookLoansList.size() * 2; // avatar + thumbnail per loan
      if (totalImages == 0) {
        showLoading(false);
        return;
      }

      // Use PauseTransition to delay hiding the loading overlay
      // This gives images time to load without blocking the UI thread
      PauseTransition delay = new PauseTransition(Duration.seconds(2));
      delay.setOnFinished(event -> showLoading(false));
      delay.play();
    });
    task.setOnFailed(e -> {
      //  System.out.println("Error while fetching book loans: " + task.getException().getMessage());
      showLoading(false);
    });

    new Thread(task).start();
}

//  @FXML
//  private void onCreateLoan() {
//    openLoanModal(null);
//  }

  @FXML
  private void onSearch() {
    String searchText = searchField.getText().toLowerCase();
    ObservableList<BookLoanUser> filteredList = FXCollections.observableArrayList();
    for (BookLoanUser bookLoan : bookLoansList) {
      if ((bookLoan.getUser() != null && (bookLoan.getUser().getDisplayName().toLowerCase().contains(searchText)
    || bookLoan.getUser().getEmail().toLowerCase().contains(searchText)
    || bookLoan.getUser().getUid().toLowerCase().contains(searchText)))
    || (bookLoan.getBook() != null && (bookLoan.getBook().getTitle().toLowerCase().contains(searchText)
    || bookLoan.getBook().getAuthors().toString().toLowerCase().contains(searchText)
    || bookLoan.getBook().getCategories().toString().toLowerCase().contains(searchText)))) {
  filteredList.add(bookLoan);
}
    }
    bookLoansTable.setItems(filteredList);
  }

  private void setDateCellFactory(TableColumn<BookLoanUser, String> column) {
    column.setCellFactory(col -> new TableCell<BookLoanUser, String>() {
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

  private void setTypeCellFactory(TableColumn<BookLoanUser, String> column) {
    column.setCellFactory(col -> new TableCell<BookLoanUser, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
        } else {
          Label chip = new Label(item);
          chip.getStyleClass().addAll("chip", item.toLowerCase());
          setGraphic(chip);
        }
      }
    });
  }

  private void setImageCellFactory(TableColumn<BookLoanUser, String> column) {
    column.setCellFactory(col -> new TableCell<BookLoanUser, String>() {
      private final ImageView imageView = new ImageView();

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
          setGraphic(null);
        } else {
          try {
            Image image = new Image(item, true);
            image.errorProperty().addListener((observable, oldValue, newValue) -> {
              if (newValue) {
                setGraphic(null);
              }
            });
            imageView.setImage(image);
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setPreserveRatio(true);
            setGraphic(imageView);
          } catch (Exception e) {
            setGraphic(null);
          }
        }
      }
    });
  }

  private void setRowContextMenu() {
    bookLoansTable.setRowFactory(tableView -> {
      final TableRow<BookLoanUser> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem editMenuItem = new MenuItem("Edit Book Loan");

      editMenuItem.setOnAction(e -> {
        BookLoanUser bookLoan = bookLoansTable.getSelectionModel().getSelectedItem();
        openLoanModal(bookLoan);
      });

      contextMenu.getItems().addAll(editMenuItem);
      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
    });

  }

  private void removeBookLoanFromTable(BookLoanUser bookLoan) {
    bookLoansList.remove(bookLoan);
    bookLoansTable.setItems(bookLoansList);
  }

  private void updateBookInTable(BookLoanUser updatedBookLoanUser) {
    for (int i = 0; i < bookLoansList.size(); i++) {
      if (bookLoansList.get(i).getBookLoan().get_id()
          .equals(updatedBookLoanUser.getBookLoan().get_id())) {
        bookLoansList.set(i, updatedBookLoanUser);
        bookLoansTable.setItems(bookLoansList);
        bookLoansTable.refresh();
        break;
      }
    }
  }

  private void openLoanModal(BookLoanUser bookLoan) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/loan-modal.fxml"));
      Parent parent = loader.load();
      LoanModalController controller = loader.getController();
      controller.setBookLoanUser(bookLoan);
      controller.setSaveCallback(updatedBookLoanUser -> {
        updateBookInTable(updatedBookLoanUser);
      });

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Edit Book Loan");
      dialog.initOwner(bookLoansTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      String okButtonText = "Save & Update";

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