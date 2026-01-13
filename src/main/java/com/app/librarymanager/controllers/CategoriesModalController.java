package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Categories;
import lombok.Setter;

public class CategoriesModalController {

  @FunctionalInterface
  public interface SaveCallback {

    void onSave(Categories category);
  }

  private Categories category;
  @Setter
  private SaveCallback saveCallback;

  public void setCategories(Categories category) {
  }

  public void onSubmit() {
    if (saveCallback != null) {
      saveCallback.onSave(category);
    }

  }
}
