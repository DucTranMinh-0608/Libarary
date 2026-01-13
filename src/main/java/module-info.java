module com.app.librarymanager {
  requires javafx.controls;
  requires javafx.fxml;

  requires org.kordamp.ikonli.javafx;
  requires org.kordamp.bootstrapfx.core;
  requires java.dotenv;
  requires java.net.http;
  requires annotations;
  requires  org.json;
  requires com.google.gson;
  requires firebase.admin;
  requires com.google.auth.oauth2;
  requires com.google.auth;
  requires google.cloud.firestore;
  requires google.cloud.storage;
  requires com.google.api.apicommon;
  requires google.cloud.core;
  requires static lombok;
  requires java.sql;
  requires org.mongodb.driver.core;
  requires org.mongodb.driver.sync.client;
  requires org.mongodb.bson;
  requires com.google.api.client;
  requires google.api.client;
  requires com.google.api.client.auth;
  requires com.google.api.client.json.jackson2;
  requires com.google.api.client.extensions.jetty.auth;
  requires java.prefs;
  requires com.google.api.client.json.gson;
  requires jdk.httpserver;
  requires org.apache.commons.lang3;
  requires org.checkerframework.checker.qual;
  requires java.desktop;

  opens com.app.librarymanager to javafx.fxml, com.google.gson;
  exports com.app.librarymanager;
  exports com.app.librarymanager.controllers;
  exports com.app.librarymanager.interfaces;
  exports com.app.librarymanager.models;
  opens com.app.librarymanager.controllers to javafx.fxml, com.google.gson;
  opens com.app.librarymanager.models to com.google.gson;
}