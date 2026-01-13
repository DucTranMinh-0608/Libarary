package com.app.librarymanager.interfaces;

import org.json.JSONObject;

public interface AuthStateListener {
  void onAuthStateChanged(boolean isAuthenticated);
}
