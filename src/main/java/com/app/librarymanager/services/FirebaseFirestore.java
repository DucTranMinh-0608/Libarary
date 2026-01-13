package com.app.librarymanager.services;


import com.app.librarymanager.models.Book;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.json.JSONArray;
import org.json.JSONObject;

public class FirebaseFirestore {

  private static FirebaseFirestore instance;
  private final Firestore db = Firebase.getDb();

  public FirebaseFirestore() {

  }

  public static synchronized FirebaseFirestore getInstance() {
    if (instance == null) {
      instance = new FirebaseFirestore();
    }
    return instance;
  }

  public static <T> Map<String, Object> objectToMap(T object) {
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(object), new TypeToken<Map<String, Object>>() {
    }.getType());
  }

  public static <T> T mapToObject(Map<String, Object> data, Class<T> myClass) {
    Gson gson = new Gson();
    JsonElement jsonElement = gson.toJsonTree(data);
    return gson.fromJson(jsonElement, myClass);
  }

  public Timestamp addData(String collection, String document, Map<String, Object> data) {
    DocumentReference docRef = db.collection(collection).document(document);
    ApiFuture<WriteResult> result = docRef.set(data);
    try {
      //  System.out.println("Update time : " + result.get().getUpdateTime());
      return result.get().getUpdateTime();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public JSONObject getData(String collection, String document) {
    DocumentReference docRef = db.collection(collection).document(document);
    ApiFuture<DocumentSnapshot> future = docRef.get();
    try {
      DocumentSnapshot documentSnapshot = future.get();
      if (documentSnapshot.exists()) {
        //  System.out.println("Document data: " + documentSnapshot.getData());
        return new JSONObject(documentSnapshot.getData());
      } else {
        //  System.out.println("No such document!");
        return null;
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }


  public ArrayList<Map<String, Object>> getCollection(String collection) {
    ApiFuture<QuerySnapshot> future = db.collection(collection).get();
    try {
      ArrayList<Map<String, Object>> list = new ArrayList<>();
      for (DocumentSnapshot document : future.get().getDocuments()) {
        list.add(document.getData());
      }
      return list;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> getDocumentObject(String collection, String document) {
    try {
      DocumentReference docRef = db.collection(collection).document(document);
      ApiFuture<DocumentSnapshot> future = docRef.get();
      DocumentSnapshot docSnap = future.get();
      if (docSnap.exists()) {
        return docSnap.getData();
      } else {
        throw new Exception("Document " + document + " doesn't exists in collection " + collection);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Timestamp updateData(String collection, String document, Map<String, Object> data) {
    DocumentReference docRef = db.collection(collection).document(document);
    ApiFuture<WriteResult> result = docRef.update(data);
    try {
      //  System.out.println("Update time : " + result.get().getUpdateTime());
      return result.get().getUpdateTime();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteData(String collection, String document) {
    DocumentReference docRef = db.collection(collection).document(document);
    ApiFuture<WriteResult> result = docRef.delete();
    try {
      System.out.println("Update time : " + result.get().getUpdateTime());
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean haveDocument(String collection, String document) {
    try {
      return db.collection(collection).document(document).get().get().exists();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public JSONArray getDataWithFilter(String collection, String nameField, Object valueField) {
    try {
      CollectionReference colRef = db.collection(collection);
      Query query = colRef.whereEqualTo(nameField, valueField);
      ApiFuture<QuerySnapshot> querySnapshot = query.get();

      List<QueryDocumentSnapshot> listSnapshot = querySnapshot.get().getDocuments();
      List<Map<String, Object>> list = new ArrayList<>();

      for (DocumentSnapshot document : listSnapshot) {
        list.add(document.getData());
      }

      return new JSONArray(list);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void appendToArray(String collection, String document, String nameArray,
      String newElement) {
    try {
      DocumentReference docRef = db.collection(collection).document(document);
      ApiFuture<WriteResult> arrayUnion = docRef.update(nameArray,
          FieldValue.arrayUnion(newElement));
      //  System.out.println("Update time : " + arrayUnion.get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <T> void updateField(String collection, String document, String nameField,
      T newContent) {
    try {
      DocumentReference docRef = db.collection(collection).document(document);
      Map<String, Object> updates = new HashMap<>();
      updates.put(nameField, newContent);
      docRef.update(updates);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void removeFromArray(String collection, String document, String nameArray,
      String newElement) {
    try {
      DocumentReference docRef = db.collection(collection).document(document);
      ApiFuture<WriteResult> arrayRemove = docRef.update(nameArray,
          FieldValue.arrayRemove(newElement));
      //  System.out.println("Update time : " + arrayRemove.get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}