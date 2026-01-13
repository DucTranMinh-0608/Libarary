package com.app.librarymanager.controllers;

import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import com.app.librarymanager.models.Categories;

public class ManageCategoriesController extends ControllerWithLoader {

  @FXML
  private Button addBtn;
  @FXML
  private TextField nameField;
  @FXML
  private TableView<Categories> bookLoansTable;
  @FXML
  private TableColumn<Categories, ObjectId> _idColumn;
  @FXML
  private TableColumn<Categories, String> nameColumn;
  @FXML
  private TableColumn<Categories, ObjectId> createdAtColumn;
  @FXML
  private TableColumn<Categories, Date> updatedAtColumn;
  @FXML
  private TextField searchField;

  @FXML
  private Text paginationInfo;
  @FXML
  private ComboBox<Integer> pageSize;
  @FXML
  private Button prevBtn;
  @FXML
  private Button nextBtn;


  private int start = 0;
  private int length = 10;
  private int totalCategories = 0;

  private ObservableList<Categories> categoriesList = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    setLoadingText("Loading categories...");

    _idColumn.setCellValueFactory(new PropertyValueFactory<>("_id"));
    _idColumn.setCellFactory(col -> new TableCell<Categories, ObjectId>() {
      @Override
      protected void updateItem(ObjectId item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item.toString());
        }
      }
    });
    nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("_id"));
    updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdated"));

    convertObjectIdToDateCellFactory(createdAtColumn);
    setDateCellFactory(updatedAtColumn);

    _idColumn.setPrefWidth(150);
    nameColumn.setPrefWidth(150);
    createdAtColumn.setPrefWidth(150);
    updatedAtColumn.setPrefWidth(150);

    loadCategories();
    setRowContextMenu();

    addBtn.setOnAction(e -> {
      handleAddCate(e);
    });

    pageSize.getItems().addAll(10, 20, 50, 100);
    pageSize.setValue(length);
    pageSize.setOnAction(e -> {
      length = pageSize.getValue();
      start = 0;
      loadCategories();
      updatePaginationInfo();
    });

    prevBtn.setDisable(true);
    countCategories();
  }

  private void countCategories() {
    Task<Integer> task = new Task<>() {
      @Override
      protected Integer call() {
        return (int) CategoriesController.countCategories();
      }
    };

    task.setOnSucceeded(e -> {
      Platform.runLater(() -> {
        totalCategories = task.getValue();
        //  System.out.println("Total categories: " + totalCategories);
        updatePaginationInfo();
      });
    });
    task.setOnFailed(e -> {
      Platform.runLater(() -> {
        //  System.out.println("Error while fetching categories: " + task.getException().getMessage());
      });
    });

    new Thread(task).start();
  }

  private void loadCategories() {
    Task<ObservableList<Categories>> task = new Task<>() {
      @Override
      protected ObservableList<Categories> call() {
        ObservableList<Categories> categories = FXCollections.observableArrayList();
        List<Categories> cateList = CategoriesController.getCategories(start, length);
        categories.addAll(cateList);
        return categories;
      }
    };

    task.setOnRunning(e -> Platform.runLater(() -> showLoading(true)));
    task.setOnSucceeded(e -> {
      Platform.runLater(() -> {
        categoriesList.setAll(task.getValue());
        bookLoansTable.setItems(categoriesList);
        showLoading(false);
      });
    });
    task.setOnFailed(e -> {
      Platform.runLater(() -> {
        showLoading(false);
        //  System.out.println("Error while fetching categories: " + task.getException().getMessage());
      });
    });

    new Thread(task).start();
  }

  @FXML
  private void onSearch() {
    String searchText = searchField.getText().toLowerCase();
    ObservableList<Categories> filteredList = FXCollections.observableArrayList();
    for (Categories category : categoriesList) {
      if (category.getName().toLowerCase().contains(searchText)) {
        filteredList.add(category);
      }
    }
    bookLoansTable.setItems(filteredList);
  }

  private void handleAddCate(ActionEvent e) {
    String name = nameField.getText();
    if (name.trim().isEmpty()) {
      AlertDialog.showAlert("error", "Error", "Category name cannot be empty.", null);
      return;
    }

    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        Categories category = new Categories(name);
        return CategoriesController.addCategory(category);
      }
    };

    task.setOnRunning(ev -> showLoading(true));
    task.setOnSucceeded(ev -> {
      Document res = task.getValue();
      if (res == null) {
        AlertDialog.showAlert("error", "Error", "Error while adding category.", null);
        return;
      } else {
        showLoading(false);
        Categories category = new Categories(res);
        if (categoriesList.size() < length) {
          categoriesList.add(category);
          bookLoansTable.refresh();
          totalCategories++;
          updatePaginationInfo();
        }
        nameField.clear();
      }
    });
    task.setOnFailed(ev -> {
      showLoading(false);
      //  System.out.println("Error while adding category: " + task.getException().getMessage());
    });

    new Thread(task).start();
  }

  private void setRowContextMenu() {
    bookLoansTable.setRowFactory(tableView -> {
      final TableRow<Categories> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem deleteMenuItem = new MenuItem("Delete Categories");

      deleteMenuItem.setOnAction(event -> {
        Categories category = row.getItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Categories");
        alert.setHeaderText("Are you sure you want to delete category " + category.getName() + "?");
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(response -> {
          if (response == ButtonType.YES) {
            Task<Boolean> task = new Task<Boolean>() {
              @Override
              protected Boolean call() throws Exception {
                return CategoriesController.removeCategory(category);
              }

            };
            task.setOnRunning(ev -> showLoading(true));
            task.setOnSucceeded(e -> {
              boolean delRes = task.getValue();
              showLoading(false);
              if (delRes) {
                removeCategoriesFromTable(category);
                totalCategories--;
                updatePaginationInfo();
              } else {
                AlertDialog.showAlert("error", "Error", "Error while deleting category.", null);
              }
            });
            task.setOnFailed(ev -> {
              showLoading(false);
//              System.out.println(
//                  "Error while deleting category: " + task.getException().getMessage());
            });
            new Thread(task).start();
          }
        });
      });

      contextMenu.getItems().addAll(deleteMenuItem);
      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
    });
  }

  private void updatePaginationInfo() {
    paginationInfo.setText(
        "Showing " + (start + 1) + " to " + Math.min(start + length, totalCategories) + " of "
            + totalCategories);
    prevBtn.setDisable(start == 0);
    nextBtn.setDisable(start + length >= totalCategories);
  }

  private void setDateCellFactory(TableColumn<Categories, Date> column) {
    column.setCellFactory(col -> new TableCell<Categories, Date>() {
      @Override
      protected void updateItem(Date item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item.toLocaleString());
        }
      }
    });
  }

  private void convertObjectIdToDateCellFactory(TableColumn<Categories, ObjectId> column) {
    column.setCellFactory(col -> new TableCell<Categories, ObjectId>() {
      @Override
      protected void updateItem(ObjectId item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(DateUtil.convertToStringFrom(item.toString()));
        }
      }
    });
  }

  private void removeCategoriesFromTable(Categories category) {
    categoriesList.remove(category);
    bookLoansTable.refresh();
  }

  public void updateCategoriesInTable(Categories updatedCategories) {
    categoriesList.replaceAll(
        category -> category.get_id().equals(updatedCategories.get_id()) ? updatedCategories
            : category);
    bookLoansTable.refresh();
  }

  @FXML
  private void prevPage() {
    if (start >= length) {
      start -= length;
      loadCategories();
      updatePaginationInfo();
    }
  }

  @FXML
  private void nextPage() {
    if (start + length < totalCategories) {
      start += length;
      loadCategories();
      updatePaginationInfo();
    }
  }
}