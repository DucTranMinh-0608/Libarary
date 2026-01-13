package com.app.librarymanager.services;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.geoWithinCenter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.github.cdimascio.dotenv.Dotenv;
import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.BsonDocument;
import org.bson.Document;
import java.util.Map;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


public class MongoDB {

  private static MongoDB instance = null;

  private static final Dotenv dotenv = Dotenv.load();
  private static final String connectionString = dotenv.get("MONGODB_URI");
  private static final String databaseName = "library-manager";

  private MongoClient mongoClient = null;
  private MongoDatabase database = null;

  private MongoDB() {
    try {
      ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
      MongoClientSettings settings = MongoClientSettings.builder()
          .applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi)
          .build();
      mongoClient = MongoClients.create(settings);
      database = mongoClient.getDatabase(databaseName);
    } catch (Exception e) {
      //  System.err.println("Error when connecting MongoDB: " + e.getMessage());
    }
  }


  public static synchronized MongoDB getInstance() {
    if (instance == null) {
      instance = new MongoDB();
    }
    return instance;
  }

  public MongoClient getMongoClient() {
    if (instance == null) {
      instance = new MongoDB();
    }
    return mongoClient;
  }

  public MongoDatabase getDatabase() {
    if (instance == null) {
      instance = new MongoDB();
    }
    return database;
  }

  public static <T> Map<String, Object> objectToMap(T object) {
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(object), new TypeToken<Map<String, Object>>() {
    }.getType());
  }

  public static <T> T jsonToObject(String json, Class<T> myClass) {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    return gson.fromJson(json, myClass);
  }

  public static <T> T mapToObject(Map<String, Object> data, Class<T> myClass) {
    Gson gson = new Gson();
    JsonElement jsonElement = gson.toJsonTree(data);
    return gson.fromJson(jsonElement, myClass);
  }

  public Document addToCollection(String collectionName, Map<String, Object> data) {
    try {
      MongoCollection<Document> collection = database.getCollection(collectionName);
      Document toInsert = new Document(data).append("_id", new ObjectId())
          .append("lastUpdated", new Timestamp(System.currentTimeMillis()));
      InsertOneResult result = collection.insertOne(toInsert);
      //  System.out.println("Success! Inserted document id: " + result.getInsertedId());
      return toInsert;
    } catch (Exception e) {
      //  System.err.println("Error when trying to add " + collectionName + e.getMessage());
      return null;
    }
  }

  public long countDocuments(String collectionName) {
    return database.getCollection(collectionName)
        .countDocuments(new BsonDocument(), new CountOptions().hintString("_id_"));
  }

  public long countDocuments(String collectionName, Bson filter) {
    return database.getCollection(collectionName).countDocuments(filter);
  }

  public List<Document> findAllObject(String collectionName, Bson filter) {
    try {
      List<Document> result = new ArrayList<>();
      MongoCollection<Document> collection = database.getCollection(collectionName);
      collection.find(filter).forEach(result::add);
      return result;
    } catch (Exception e) {
      //  System.err.println("Error when trying to find at " + collectionName);
      return null;
    }
  }

  // 0-indexed
  public List<Document> findAllObject(String collectionName, Bson filter, int start, int length) {
    try {
      List<Document> result = new ArrayList<>();
      MongoCollection<Document> collection = database.getCollection(collectionName);
      collection.find(filter).skip(start).limit(length).forEach(result::add);
      return result;
    } catch (Exception e) {
//      System.out.println(
//          "Fail when trying to crawl " + collectionName + " start " + start + " length " + length);
      return null;
    }
  }

  public List<Document> findSortedObject(String collectionName, Bson filter, Bson order, int start,
      int length) {
    try {
      List<Document> result = new ArrayList<>();
      MongoCollection<Document> collection = database.getCollection(collectionName);
      collection.find(filter).sort(order).skip(start).limit(length).forEach(result::add);
      return result;
    } catch (Exception e) {
//      System.out.println(
//          "Fail when trying to crawl " + collectionName + " start " + start + " length " + length);
      return null;
    }
  }

  public List<Document> getAggregate(String collectionName, List<Document> pipeline) {
    try {
      MongoCollection<Document> collection = database.getCollection(collectionName);
      List<Document> documents = new ArrayList<>();
      collection.aggregate(pipeline).forEach(documents::add);
      return documents;
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      return null;
    }
  }

  public List<Document> findAllObject(String collectionName, String criteriaName, String regex) {
    // i: intensive, which doesn't separate from lower and uppercase
    return findAllObject(collectionName, Filters.regex(criteriaName, regex, "i"));
  }

  public Document findAnObject(String collectionName, Bson filter) {
    try {
      return database.getCollection(collectionName).find(filter).first();
    } catch (Exception e) {
      //  System.err.println("Fail when finding: " + e.getMessage());
      return null;
    }
  }

  public Document findAnObject(String collectionName, String criteriaName, Object valueCriteria) {
    return findAnObject(collectionName, eq(criteriaName, valueCriteria));
  }

  public Document findAnObject(String collectionName, Map<String, Object> criteria) {
    return findAnObject(collectionName, Filters.and(
        criteria.entrySet().stream().map(entry -> eq(entry.getKey(), entry.getValue()))
            .toArray(Bson[]::new)));
  }

  public Document updateData(String collectionName, String idCriteria, Object valueCriteria,
      Map<String, Object> newObject) {
    newObject.remove("_id");
    newObject.remove("lastUpdated");
    //  System.out.println(newObject);
    List<Bson> updateList = new ArrayList<>(
        newObject.entrySet().stream().map(entry -> Updates.set(entry.getKey(), entry.getValue()))
            .toList());
    updateList.add(Updates.set("lastUpdated", new Timestamp(System.currentTimeMillis())));
    Bson updates = Updates.combine(updateList);
    MongoCollection<Document> collection = database.getCollection(collectionName);
    //  System.err.println(idCriteria + " " + valueCriteria);
    try {
      return collection.findOneAndUpdate(eq(idCriteria, valueCriteria), updates,
          new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
    } catch (Exception e) {
      //  System.err.println("Fail when trying to update at " + collectionName + " " + e.getMessage());
      return null;
    }
  }

  public boolean updateAll(String collectionName, Bson filter, Bson update) {
    try {
      MongoCollection<Document> collection = database.getCollection(collectionName);
      UpdateResult result = collection.updateMany(filter, update);
      return result.wasAcknowledged();
    } catch (Exception e) {
      //  System.out.println("Fail when trying to update all " + collectionName + " " + e.getMessage());
      return false;
    }
  }

  public boolean deleteFromCollection(String collectionName, String criteriaName,
      Object valueCriteria) {
    try {
      if (findAnObject(collectionName, criteriaName, valueCriteria) == null) {
        return false;
      }
      database.getCollection(collectionName).deleteOne(eq(criteriaName, valueCriteria));
      return true;
    } catch (Exception e) {
      //  System.err.println("Error when trying to delete " + criteriaName + " " + valueCriteria + ": "
//          + e.getMessage());
      return false;
    }
  }

  public boolean deleteAll(String collectionName, Bson filter) {
    try {
      database.getCollection(collectionName).deleteMany(filter);
      return true;
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return false;
    }
  }
}