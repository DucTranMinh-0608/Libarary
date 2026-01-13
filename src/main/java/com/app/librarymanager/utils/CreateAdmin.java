package com.app.librarymanager.utils;

import com.app.librarymanager.models.User;
import com.app.librarymanager.services.Firebase;
import com.app.librarymanager.services.FirebaseAuthentication;
import org.json.JSONObject;

/**
 * Small helper that allows seeding an admin account without going through the UI.
 *
 * Usage examples:
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass="com.app.librarymanager.utils.AdminAccountCreator" \
 *       -Dexec.args="admin@example.com StrongPass!123 \"Admin User\" 1990-01-01"
 * </pre>
 */
public final class CreateAdmin {

    private CreateAdmin() {
    }

    /**
     * Creates an admin account by delegating to the Firebase auth helper that already exists.
     *
     * @param email       admin email
     * @param password    admin password
     * @param displayName full name to show in UI
     * @param birthday    ISO date string, leave empty if unknown
     * @return JSON response returned by {@link FirebaseAuthentication}
     */
    public static JSONObject createAdminUser(String email, String password, String displayName,
                                             String birthday) {
        Firebase.getApp(); // ensures DEFAULT FirebaseApp exists before hitting FirebaseAuth
        User user = new User(email, password, displayName, birthday, null, true);
        user.setPhoneNumber("+84368388205");
        return FirebaseAuthentication.createAccountWithEmailAndPasswordUsingFirebaseAuth(user);
    }

    /**
     * Simple CLI entry point so devs can seed an admin account quickly.
     */
    public static void main(String[] args) {
//    if (args.length < 4) {
//      System.out.println(
//          "Usage: AdminAccountCreator <email> <password> <displayName> <birthday(yyyy-MM-dd|empty)>");
//      return;
//    }

        String email = "admin123@admin.com";
        String password = "123456";
        String displayName = "admin";
        String birthday = "";

        JSONObject response = createAdminUser(email, password, displayName, birthday);
        System.out.println(response.toString(2));
    }
}
