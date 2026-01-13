package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Book;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Popup;

public class BookComponentController {

  @FXML
  private VBox bookContainer;

  @FXML
  private TextField id;

  @FXML
  private ImageView bookCover;

  @FXML
  private Label bookTitle;

  @FXML
  private Label bookAuthor;

  @FXML
  private void initialize() {
    bookCover.setPreserveRatio(true);
    bookCover.setSmooth(true);
    bookCover.setCache(true);

    bookCover.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
      Rectangle clip = new Rectangle(newValue.getWidth(), newValue.getHeight());
      clip.setArcWidth(10);
      clip.setArcHeight(10);
      bookCover.setClip(clip);
    });
    bookContainer.setOnMouseClicked(event -> handleBookClick());
  }

  public void setBook(Book book) {
    id.setText(book.getId());
    bookTitle.setText(book.getTitle());
    bookAuthor.setText("by " + book.getAuthors().toString().replaceAll("[\\[\\]]", ""));
    if (book.getThumbnail() != null && !book.getThumbnail().isEmpty()) {
      try {
        bookCover.setImage(new Image(book.getThumbnail()));
      } catch (Exception e) {
        bookCover.setImage(new Image(
            "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"));
      }
    } else {
      bookCover.setImage(new Image(
          "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api"));
    }
  }

  private void handleBookClick() {
    AuthController.requireLogin();
    if (!AuthController.getInstance().isAuthenticated()) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/book-detail.fxml"));
      Parent root = loader.load();
      BookDetailController controller = loader.getController();
      controller.getBookDetail(id.getText());

      StackPane overlay = new StackPane(root);
      overlay.getStyleClass().add("overlay");
      StackPane stackPane = (StackPane) bookContainer.getScene().lookup("#contentPane");
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