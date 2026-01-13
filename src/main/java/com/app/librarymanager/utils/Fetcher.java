package com.app.librarymanager.utils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class Fetcher {
  private static final HttpClient client = HttpClient.newHttpClient();

  @Nullable
  public static JSONObject get(String url) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return new JSONObject(response.body());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Nullable
  public static JSONObject post(String url, String body) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return new JSONObject(response.body());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Nullable
  public static HttpResponse<String> put(String url, String body) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(body))
        .build();
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Nullable
  public static HttpResponse<String> delete(String url) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(url))
        .DELETE()
        .build();
    try {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
