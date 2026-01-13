package com.app.librarymanager.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import javax.imageio.ImageIO;
import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

public class UploadFileUtil {
  private static final Dotenv dotenv = Dotenv.load();
  private static final String IMGBB_API_KEY = dotenv.get("IMGBB_API_KEY");

  public static JSONObject uploadFile(String filePath, String name) {
    if (filePath == null || filePath.isEmpty()) {
      return new JSONObject().put("success", false).put("message", "File path is required");
    }
    File file = new File(filePath);
    String fileName;
    if (name == null || name.isEmpty()) {
      fileName = validateName(file.getName());
    } else {
      fileName = validateName(name + "." + file.getName().split("\\.")[1]);
    }
    String urlString = "https://rdrive.serv00.net/upload?fileName=" + fileName;

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(urlString))
          .header("Content-Type", "application/octet-stream")
          .POST(BodyPublishers.ofFile(file.toPath()))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      //  System.out.println("Response Code: " + response.statusCode());
      //  System.out.println("Response Body: " + response.body());

      return new JSONObject(response.body());

    } catch (Exception e) {
      e.printStackTrace();
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static JSONObject uploadImageToImgBB(byte[] imageBytes) {
    if (IMGBB_API_KEY == null || IMGBB_API_KEY.isEmpty()) {
      return new JSONObject().put("success", false).put("message", "ImgBB API Key is missing in .env file");
    }

    try {
      String base64Image = Base64.getEncoder().encodeToString(imageBytes);
      String requestBody = "key=" + IMGBB_API_KEY + "&image=" + URLEncoder.encode(base64Image, StandardCharsets.UTF_8);

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://api.imgbb.com/1/upload"))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(BodyPublishers.ofString(requestBody))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject jsonResponse = new JSONObject(response.body());

      if (response.statusCode() == 200 && jsonResponse.getBoolean("success")) {
        JSONObject data = jsonResponse.getJSONObject("data");
        return new JSONObject()
            .put("success", true)
            .put("url", data.getString("url"))
            .put("display_url", data.getString("display_url"))
            .put("delete_url", data.getString("delete_url"));
      } else {
        return new JSONObject()
            .put("success", false)
            .put("message", jsonResponse.optJSONObject("error") != null ? 
                jsonResponse.getJSONObject("error").getString("message") : "Upload failed");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new JSONObject().put("success", false).put("message", "ImgBB upload error: " + e.getMessage());
    }
  }

  public static JSONObject uploadImage(String filePath, String name, int width, int height, boolean crop) {
    if (filePath == null || filePath.isEmpty()) {
      return new JSONObject().put("success", false).put("message", "File path is required");
    }
    File file = new File(filePath);

    try {
      BufferedImage processedImage;
      if (crop) {
        processedImage = cropImage(file, width, height);
      } else {
        processedImage = resizeImage(file, width, height);
      }
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
      ImageIO.write(processedImage, extension.equals("png") ? "png" : "jpeg", baos);
      byte[] imageBytes = baos.toByteArray();

      JSONObject imgbbResp = uploadImageToImgBB(imageBytes);
      if (imgbbResp.getBoolean("success")) {
        return new JSONObject()
            .put("success", true)
            .put("longURL", imgbbResp.getString("url"))
            .put("message", "Image uploaded successfully to ImgBB");
      } else {
        return imgbbResp;
      }

    } catch (Exception e) {
      e.printStackTrace();
      return new JSONObject().put("success", false)
          .put("message", "Error in processing image: " + e.getMessage());
    }
  }

  public static JSONObject uploadImage(String filePath, String name, int cropSize) {
    return uploadImage(filePath, name, cropSize, cropSize, true);
  }

  private static String validateName(String name) {
    return name.replaceAll("[^a-zA-Z0-9.-]", "_");
  }

  public static BufferedImage resizeImage(File file, int maxWidth, int maxHeight) throws IOException {
    BufferedImage originalImage = ImageIO.read(file);
    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    double widthRatio = (double) maxWidth / width;
    double heightRatio = (double) maxHeight / height;
    double ratio = Math.min(widthRatio, heightRatio);

    int targetWidth = (int) (width * ratio);
    int targetHeight = (int) (height * ratio);

    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, 
        originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
    resizedImage.getGraphics().drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    return resizedImage;
  }

  public static BufferedImage cropImage(File file, int targetWidth, int targetHeight) throws IOException {
    BufferedImage originalImage = ImageIO.read(file);
    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    double targetRatio = (double) targetWidth / targetHeight;
    double originalRatio = (double) width / height;

    int intermediateWidth, intermediateHeight;
    if (originalRatio > targetRatio) {
      intermediateHeight = targetHeight;
      intermediateWidth = (int) (originalRatio * targetHeight);
    } else {
      intermediateWidth = targetWidth;
      intermediateHeight = (int) (targetWidth / originalRatio);
    }

    BufferedImage resizedImage = new BufferedImage(intermediateWidth, intermediateHeight, 
        originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
    resizedImage.getGraphics().drawImage(originalImage, 0, 0, intermediateWidth, intermediateHeight, null);

    return resizedImage.getSubimage(
        (intermediateWidth - targetWidth) / 2,
        (intermediateHeight - targetHeight) / 2,
        targetWidth,
        targetHeight
    );
  }

  public static BufferedImage cropImage(File file, int size) throws IOException {
    return cropImage(file, size, size);
  }

  public static BufferedImage cropImage(File file) throws IOException {
    BufferedImage originalImage = ImageIO.read(file);
    int size = Math.min(originalImage.getWidth(), originalImage.getHeight());
    return originalImage.getSubimage(
        (originalImage.getWidth() - size) / 2,
        (originalImage.getHeight() - size) / 2,
        size,
        size
    );
  }

}