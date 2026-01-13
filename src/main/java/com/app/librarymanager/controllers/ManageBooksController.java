package com.app.librarymanager.controllers;

import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.DateUtil.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.PauseTransition;
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
import com.app.librarymanager.models.Book;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

public class ManageBooksController extends ControllerWithLoader {

  private static ManageBooksController instance;

  @FXML
  private TableView<Book> booksTable;
  @FXML
  private TableColumn<Book, String> _idColumn;
  @FXML
  private TableColumn<Book, String> idColumn;
  @FXML
  private TableColumn<Book, String> iSBNColumn;
  @FXML
  private TableColumn<Book, String> titleColumn;
  @FXML
  private TableColumn<Book, String> descriptionColumn;
  @FXML
  private TableColumn<Book, String> publisherColumn;
  @FXML
  private TableColumn<Book, ArrayList<String>> authorsColumn;
  @FXML
  private TableColumn<Book, ArrayList<String>> categoriesColumn;
  @FXML
  private TableColumn<Book, Double> priceColumn;
  @FXML
  private TableColumn<Book, Double> discountPriceColumn;
  @FXML
  private TableColumn<Book, String> currencyCodeColumn;
  @FXML
  private TableColumn<Book, Integer> pageCountColumn;
  @FXML
  private TableColumn<Book, String> languageColumn;
  @FXML
  private TableColumn<Book, String> publishedDateColumn;
  @FXML
  private TableColumn<Book, String> thumbnailColumn;
  @FXML
  private TableColumn<Book, Boolean> isActiveColumn;
  @FXML
  private Text paginationInfo;

  @FXML
  private TextField searchField;

  @FXML
  private ComboBox<String> activeFilter;
  @FXML
  private ComboBox<Integer> pageSize;
  @FXML
  private Button prevBtn;
  @FXML
  private Button nextBtn;

  private PauseTransition pause = new PauseTransition(Duration.seconds(0.5));

  private Map<String, Image> imageCache = new HashMap<>();

  private int start = 0;
  private int length = 10;
  private int totalBooks = 0;


  private ObservableList<Book> booksList = FXCollections.observableArrayList();

  public synchronized static ManageBooksController getInstance() {
    if (instance == null) {
      instance = new ManageBooksController();
    }
    return instance;
  }

  @FXML
  public void initialize() {
    showCancel(false);
    setLoadingText("Loading books...");

    _idColumn.setCellValueFactory(new PropertyValueFactory<>("_id"));
    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    iSBNColumn.setCellValueFactory(new PropertyValueFactory<>("iSBN"));
    thumbnailColumn.setCellValueFactory(new PropertyValueFactory<>("thumbnail"));
    titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    publisherColumn.setCellValueFactory(new PropertyValueFactory<>("publisher"));
    authorsColumn.setCellValueFactory(new PropertyValueFactory<>("authors"));
    categoriesColumn.setCellValueFactory(new PropertyValueFactory<>("categories"));
    priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
    discountPriceColumn.setCellValueFactory(new PropertyValueFactory<>("discountPrice"));
    currencyCodeColumn.setCellValueFactory(new PropertyValueFactory<>("currencyCode"));
    pageCountColumn.setCellValueFactory(new PropertyValueFactory<>("pageCount"));
    languageColumn.setCellValueFactory(new PropertyValueFactory<>("language"));
    isActiveColumn.setCellValueFactory(new PropertyValueFactory<>("activated"));
    publishedDateColumn.setCellValueFactory(new PropertyValueFactory<>("publishedDate"));

    thumbnailColumn.setCellFactory(column -> new TableCell<Book, String>() {
      private final ImageView imageView = new ImageView();

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
          setGraphic(null);
        } else {
          if (imageCache.containsKey(item)) {
            imageView.setImage(imageCache.get(item));
            imageView.setFitHeight(50);
            imageView.setPreserveRatio(true);
            setGraphic(imageView);
          } else {
            try {
              Image image = new Image(item, true);
              image.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                  setGraphic(null);
                }
              });
              imageView.setImage(image);
              imageView.setFitHeight(50);
              imageView.setPreserveRatio(true);
              imageCache.put(item, image);
              setGraphic(imageView);
            } catch (Exception e) {
              setGraphic(null);
            }
          }
        }
      }
    });

    _idColumn.setPrefWidth(150);
    idColumn.setPrefWidth(120);
    iSBNColumn.setPrefWidth(120);
    thumbnailColumn.setPrefWidth(50);
    titleColumn.setPrefWidth(150);
    descriptionColumn.setPrefWidth(150);
    publisherColumn.setPrefWidth(150);
    authorsColumn.setPrefWidth(150);
    categoriesColumn.setPrefWidth(100);
    priceColumn.setPrefWidth(100);
    discountPriceColumn.setPrefWidth(80);
    currencyCodeColumn.setPrefWidth(60);
    pageCountColumn.setPrefWidth(50);
    languageColumn.setPrefWidth(80);
    isActiveColumn.setPrefWidth(80);
    publishedDateColumn.setPrefWidth(150);

    setDateCellFactory(publishedDateColumn);
    setArrayCellFactory(authorsColumn);
    setArrayCellFactory(categoriesColumn);
    setPriceCellFactory(priceColumn);
    setPriceCellFactory(discountPriceColumn);

    activeFilter.getItems().addAll("All", "True", "False");

    activeFilter.setValue("All");
    activeFilter.setPrefWidth(150);

    activeFilter.setOnAction(e -> onFilter());

    pageSize.getItems().addAll(10, 20, 50, 100);
    pageSize.setValue(length);

    pageSize.setOnAction(e -> {
      length = pageSize.getValue();
      start = 0;
      loadBooks();
      updatePaginationInfo();
    });

    if (this.start == 0) {
      prevBtn.setDisable(true);
    }

    loadBooks();
    countTotalBooks();
    setRowContextMenu();
  }

  private void loadBooks() {
    Task<ObservableList<Book>> task = new Task<>() {
      @Override
      protected ObservableList<Book> call() {
        ObservableList<Book> books = FXCollections.observableArrayList();
        List<Book> bookList = BookController.findBookByKeyword("", start, length);
        books.addAll(bookList);
        return books;
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      booksList.setAll(task.getValue());
      booksTable.setItems(booksList);
      //  System.out.println("Books loaded successfully. Total: " + booksList.size());
      showLoading(false);
    });
    task.setOnFailed(e -> {
      //  System.out.println("Error while fetching books: " + task.getException().getMessage());
      AlertDialog.showAlert("error", "Error",
          task.getException().getMessage(), null);
      showLoading(false);
    });

    new Thread(task).start();
  }

  private void countTotalBooks() {
    Task<Integer> countTask = new Task<>() {
      @Override
      protected Integer call() {
        return (int) BookController.numberOfBooks();
      }
    };

    countTask.setOnSucceeded(e -> {
      totalBooks = countTask.getValue();
      //  System.out.println("Total books: " + totalBooks);
      updatePaginationInfo();
    });
    countTask.setOnFailed(e -> {
      //  System.out.println("Error while counting books: " + countTask.getException().getMessage());
      AlertDialog.showAlert("error", "Error",
          countTask.getException().getMessage(), null);
    });
    new Thread(countTask).start();
  }

  @FXML
  private void onCreateBook() {
    openBookModal(null);
  }

  @FXML
  private void onSearch() {
    pause.setOnFinished(event -> {
      String searchText = searchField.getText().toLowerCase();
      Task<ObservableList<Book>> task = new Task<>() {
        @Override
        protected ObservableList<Book> call() {
          List<Book> bookList = BookController.findBookByKeyword(searchText, 0, length);
          return FXCollections.observableArrayList(bookList);
        }
      };

//      task.setOnRunning(e -> showLoading(true));
      task.setOnSucceeded(e -> {
        booksList.setAll(task.getValue());
        booksTable.setItems(booksList);
//        showLoading(false);
      });
      task.setOnFailed(e -> {
//        showLoading(false);
        AlertDialog.showAlert("error", "Error",
            task.getException().getMessage(), null);
        //  System.out.println("Error while searching books: " + task.getException().getMessage());
      });

      new Thread(task).start();
    });

    pause.playFromStart();
  }

  @FXML
  private void onFilter() {
    String searchText = searchField.getText().toLowerCase();
    String activeFilterValue = activeFilter.getValue();

    ObservableList<Book> filteredList = FXCollections.observableArrayList();
    for (Book book : booksList) {
      boolean matchesSearch =
          book.getTitle().toLowerCase().contains(searchText) || book.getAuthors().toString()
              .toLowerCase()
              .contains(searchText) || book.getISBN().toLowerCase().contains(searchText)
              || book.getCategories().toString().toLowerCase().contains(searchText)
              || book.getDescription().toLowerCase().contains(searchText);

      if (matchesSearch && (activeFilterValue.equals("All") || activeFilterValue.toLowerCase()
          .equals(String.valueOf(book.isActivated())))) {
        filteredList.add(book);
      }
    }
    booksTable.setItems(filteredList);
  }

  private void setRowContextMenu() {
    booksTable.setRowFactory(tableView -> {
      final TableRow<Book> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem editMenuItem = new MenuItem("Edit Book");
      final MenuItem deleteMenuItem = new MenuItem("Delete Book");

      editMenuItem.setOnAction(event -> {
        Book book = row.getItem();
        //  System.out.println("Edit book: " + book.get_id());
        openBookModal(book);
      });

      deleteMenuItem.setOnAction(event -> {
        Book book = row.getItem();
        //  System.out.println("Delete book: " + book.get_id());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Book");
        alert.setHeaderText("Are you sure you want to delete book " + book.getISBN() + "?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(response -> {
          if (response == ButtonType.YES) {
            Task<Boolean> task = new Task<Boolean>() {
              @Override
              protected Boolean call() throws Exception {
                BookCopiesController.removeCopy(new BookCopies(book.getId()));
                return BookController.deleteBook(book);
              }
            };
            task.setOnRunning(ev -> showLoading(true));
            task.setOnSucceeded(e -> {
              boolean delRes = task.getValue();
              //  System.out.println("Book deleted: " + delRes);
              showLoading(false);
              removeBookFromTable(book);
              totalBooks--;
              updatePaginationInfo();
            });
            task.setOnFailed(ev -> {
              showLoading(false);
              //  System.out.println("Error while deleting book: " + task.getException().getMessage());
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

  private void setDateCellFactory(TableColumn<Book, String> column) {
    column.setCellFactory(col -> new TableCell<Book, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
          setText(null);
        } else {
          setText(DateUtil.ymdToDmy(item));
        }
      }
    });
  }

  private void setPriceCellFactory(TableColumn<Book, Double> column) {
    column.setCellFactory(col -> new TableCell<Book, Double>() {
      @Override
      protected void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(String.format("%.2f", item));
        }
      }
    });
  }

  private void setArrayCellFactory(TableColumn<Book, ArrayList<String>> column) {
    column.setCellFactory(col -> new TableCell<Book, ArrayList<String>>() {
      @Override
      protected void updateItem(ArrayList<String> items, boolean empty) {
        super.updateItem(items, empty);
        if (empty || items == null || items.isEmpty()) {
          setText(null);
          setGraphic(null);
        } else {
          VBox vBox = new VBox(5);
          vBox.setPadding(new javafx.geometry.Insets(3, 0, 3, 0));
          for (String item : items) {
            Label chip = new Label(item.trim());
            chip.getStyleClass().add("chip");
            vBox.getChildren().add(chip);
          }
          setGraphic(vBox);
          setText(null);
        }
      }
    });
  }

  public void updateBookInTable(Book updatedBook) {
    booksList.replaceAll(book -> book.get_id().equals(updatedBook.get_id()) ? updatedBook : book);
    booksTable.refresh();
  }

  private void removeBookFromTable(Book book) {
    booksList.remove(book);
    booksTable.refresh();
  }

  private void openBookModal(Book book) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/book-info-modal.fxml"));
      Parent parent = loader.load();
      BookModalController controller = loader.getController();
      controller.setBook(book);
      controller.setSaveCallback(updatedBook -> {
        if (book == null) {
          totalBooks++;
          updatePaginationInfo();
          if (booksList.size() < length) {
            booksList.add(updatedBook);
            booksTable.refresh();
          }
        } else {
          updateBookInTable(updatedBook);
        }
      });

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle(book == null ? "Create Book" : "Edit Book");
      dialog.initOwner(booksTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      String okButtonText = book != null ? "Save & Update" : "Create";

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

  private void updatePaginationInfo() {
    paginationInfo.setText(
        "Showing " + (start + 1) + " to " + Math.min(start + length, totalBooks) + " of "
            + totalBooks);
    prevBtn.setDisable(start == 0);
    nextBtn.setDisable(start + length >= totalBooks);
  }

  @FXML
  private void prevPage() {
    if (start >= length) {
      start -= length;
      loadBooks();
      updatePaginationInfo();
    }
  }

  @FXML
  private void nextPage() {
    if (start + length < totalBooks) {
      start += length;
      loadBooks();
      updatePaginationInfo();
    }
  }
}