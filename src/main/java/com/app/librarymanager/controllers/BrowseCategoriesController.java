package com.app.librarymanager.controllers;


import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.Categories;
import com.app.librarymanager.utils.AlertDialog;
import java.util.List;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class BrowseCategoriesController extends ControllerWithLoader {

  @FXML
  private TextField searchField;
  @FXML
  private Label searchStatus;
  @FXML
  private ScrollPane categoriesScrollPane;
  @FXML
  private FlowPane categoriesFlowPane;

  private ObservableList<Categories> categories = FXCollections.observableArrayList();

  @FXML
  private void initialize() {
    showCancel(false);
    setLoadingText("Loading categories...");
    categoriesScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
      double deltaY = event.getDeltaY() * 3;
      categoriesScrollPane.setVvalue(
          categoriesScrollPane.getVvalue() - deltaY / categoriesScrollPane.getContent()
              .getBoundsInLocal().getHeight());
      event.consume();
    });
    categoriesScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
      categoriesFlowPane.setPrefWidth(newValue.getWidth());
    });

    loadCategories();

    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      String keyword = newValue.trim();
      handleSearch(keyword);
    });

  }

  private void loadCategories() {
    Task<List<Categories>> task = new Task<>() {
      @Override
      protected List<Categories> call() {
        return CategoriesController.getCategories(0, 10000);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(event -> {
      this.categories = FXCollections.observableArrayList(task.getValue());
      renderCategories(task.getValue());
      searchStatus.setText(task.getValue().size() + " results found");
      showLoading(false);
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to load categories", null);
    });
    new Thread(task).start();
  }

  private void handleCategoryClick(Categories category, Parent container) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/category-detail.fxml"));
      Parent root = loader.load();
      CategoryDetailController controller = loader.getController();
      controller.getCategoryDetails(category.getName());

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

  private void handleSearch(String keyword) {
    if (keyword.isEmpty()) {
      renderCategories(categories);
      return;
    }
    FilteredList<Categories> filteredList = new FilteredList<>(categories);
    filteredList.setPredicate(
        category -> category.getName().toLowerCase().contains(keyword.toLowerCase()));
    renderCategories(filteredList);
    searchStatus.setText(filteredList.size() + " results found");
  }

  private void renderCategories(List<Categories> categories) {
    if (Objects.isNull(categories)) {
      AlertDialog.showAlert("error", "Error", "Failed to load categories", null);
      return;
    }
    categoriesFlowPane.getChildren().clear();
    categories.forEach(category -> {
      HBox hBox = new HBox();
      hBox.setOnMouseClicked(event -> handleCategoryClick(category, hBox));
      hBox.getStyleClass().addAll("card", "card-category");
      Label label = new Label(category.getName());
      label.getStyleClass().add("card-content");
      hBox.getChildren().add(label);
      categoriesFlowPane.getChildren().add(hBox);
    });
  }

}
