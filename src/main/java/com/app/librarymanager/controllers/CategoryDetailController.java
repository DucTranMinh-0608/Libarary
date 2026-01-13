package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookUser;
import com.app.librarymanager.models.Categories;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.StageManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bson.Document;
import org.kordamp.ikonli.javafx.FontIcon;

public class CategoryDetailController extends ControllerWithLoader {

  @FXML
  private ScrollPane categoryDetailScrollPane;
  @FXML
  private FlowPane categoryDetailFlowPane;
  @FXML
  private Text categoryTitle;
  @FXML
  private Text categoryDescription;

  private ObservableList<Book> books = FXCollections.observableArrayList();
  private List<Task<VBox>> renderTasks = new ArrayList<>();
  private int totalBooks = 0;

  private String currentCategory;

  @FXML
  private void initialize() {
    categoryDetailScrollPane.viewportBoundsProperty()
        .addListener((observable, oldValue, newValue) -> {
          categoryDetailFlowPane.setPrefWidth(newValue.getWidth());
        });

    categoryDetailScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.doubleValue() == categoryDetailScrollPane.getVmax()
          && books.size() < totalBooks) {
        loadBooks(currentCategory, currentPage);
      }
    });

    showCancel(false);
  }

  private static final int PAGE_SIZE = 20;
  private int currentPage = 0;
  private boolean isLoading = false;

  private void loadBooks(String category, int page) {
    if (isLoading) {
      return;
    }
    isLoading = true;

    //  System.out.println("Loading books of category " + category + " page " + page);

    Task<List<Book>> task = new Task<>() {
      @Override
      protected List<Book> call() {
        return CategoriesController.getBookOfCategory(new Categories(category), page * PAGE_SIZE,
            PAGE_SIZE);
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(event -> {
      List<Book> newBooks = task.getValue();
      if (newBooks != null && !newBooks.isEmpty()) {
        books.addAll(newBooks);
        renderBooks(newBooks);
        currentPage++;
      } else {
        AlertDialog.showAlert("warning", "Not found", "No book found for this category", null);
      }
      showLoading(false);
      isLoading = false;
    });
    task.setOnFailed(event -> {
      showLoading(false);
      isLoading = false;
      AlertDialog.showAlert("error", "Error", "Failed to load books", null);
    });

    new Thread(task).start();
  }

  private void countBooks(String category) {
    Task<Integer> task = new Task<>() {
      @Override
      protected Integer call() {
        return (int) CategoriesController.countBookOfCategory(new Categories(category));
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(event -> {
      totalBooks = task.getValue();
      showLoading(false);
      categoryDescription.setText("Total books: " + totalBooks);
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to count books", null);
    });

    new Thread(task).start();
  }

  public void getCategoryDetails(String category) {
    categoryTitle.setText(category);
    setLoadingText("Loading books of category " + category);
    currentCategory = category;
    books.clear();
    categoryDetailFlowPane.getChildren().clear();
    currentPage = 0;
    loadBooks(category, currentPage);
    countBooks(category);
  }

  private void renderBooks(List<Book> books) {
    renderTasks.forEach(Task::cancel);
    renderTasks.clear();

    for (Book book : books) {
      Task<VBox> bookTask = new Task<VBox>() {
        @Override
        protected VBox call() {
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/components/book.fxml"));
          VBox bookComponent = null;
          try {
            bookComponent = loader.load();
          } catch (Exception e) {
            e.printStackTrace();
          }
          BookComponentController bookComponentController = loader.getController();
          bookComponentController.setBook(book);
          return bookComponent;
        }
      };
      bookTask.setOnSucceeded(e -> categoryDetailFlowPane.getChildren().add(bookTask.getValue()));
      renderTasks.add(bookTask);
      new Thread(bookTask).start();
    }
  }

  @FXML
  private void close() {
    //  System.out.println("Closing category detail");
    // remove this view from the stack
  }
}
