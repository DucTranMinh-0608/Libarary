package com.app.librarymanager.utils;

import javafx.event.Event;
import javafx.scene.control.DateCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DatePickerUtil {

  public static void setDatePickerFormat(DatePicker datePicker) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    datePicker.setConverter(new StringConverter<LocalDate>() {
      @Override
      public String toString(LocalDate date) {
        if (date != null) {
          return dateFormatter.format(date);
        } else {
          return "";
        }
      }

      @Override
      public LocalDate fromString(String string) {
        if (string != null && !string.isEmpty()) {
          try {
            return LocalDate.parse(string, dateFormatter);
          } catch (DateTimeParseException e) {
            return null;
          }
        } else {
          return null;
        }
      }
    });

    TextField editor = datePicker.getEditor();
    final boolean[] ignoreListener = {false};

    editor.textProperty().addListener((observable, oldValue, newValue) -> {
      if (ignoreListener[0]) {
        return;
      }

      if (!newValue.matches("\\d{0,2}(/\\d{0,2}(/\\d{0,4})?)?")) {
        ignoreListener[0] = true;
        editor.setText(oldValue);
        ignoreListener[0] = false;
      }
    });

    editor.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        String text = editor.getText();
        if (text != null && !text.isEmpty()) {
          try {
            LocalDate.parse(text, dateFormatter);
          } catch (DateTimeParseException e) {
            editor.setText("");
          }
        }
      }
    });

    datePicker.setPromptText("dd/MM/yyyy");
  }

  public static void disableFutureDates(DatePicker datePicker) {
    datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
      @Override
      public DateCell call(DatePicker param) {
        return new DateCell() {
          @Override
          public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (item.isAfter(LocalDate.now())) {
              setDisable(true);
              setStyle("-fx-opacity: 0.5;");
            }
          }
        };
      }
    });
  }

  public static void disableEditor(DatePicker field) {
    field.getEditor().addEventFilter(KeyEvent.KEY_TYPED, Event::consume);
    field.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
    field.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, Event::consume);
  }
}