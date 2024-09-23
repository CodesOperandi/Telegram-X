package org.xorworks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.thunderdog.challegram.data.UserContext;
import org.thunderdog.challegram.telegram.TdlibCache;

public class UserWhitelist implements TdlibCache.UserDataChangeListener {

  // Singleton instance
  private static volatile UserWhitelist instance;

  // Context to access SharedPreferences
  private final Context context;

  // OkHttpClient for network requests
  private final OkHttpClient client;

  // SharedPreferences for caching
  private final SharedPreferences prefs;

  // Key for storing whitelist in SharedPreferences
  private static final String PREFS_NAME = "user_whitelist_prefs";
  private static final String KEY_WHITELIST = "whitelisted_chats";

  // Current whitelist
  private List<Long> whitelist;

  // Listener for whitelist updates
  public interface OnWhitelistUpdateListener {
    void onWhitelistUpdated(List<Long> newWhitelist);
    void onWhitelistUpdateFailed(Exception e);
  }

  private OnWhitelistUpdateListener listener;

  // Handler for main thread operations
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final UserController userController;

  /**
   * Private constructor to enforce singleton pattern.
   */
  private UserWhitelist(Context context) {
    this.context = context.getApplicationContext();
    this.client = new OkHttpClient();
    this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    this.whitelist = loadWhitelistFromPrefs();
    userController = new UserController();
    userController.getCurrentUserId();
    //addUserListeners(user, true);
  }

  /**
   * Get the singleton instance of UserWhitelist.
   *
   * @param context Application context.
   * @return Singleton instance.
   */
  public static UserWhitelist getInstance(Context context) {
    if (instance == null) {
      synchronized (UserWhitelist.class) {
        if (instance == null) {
          instance = new UserWhitelist(context);
        }
      }
    }
    return instance;
  }

  /**
   * Set the listener for whitelist updates.
   *
   * @param listener Listener to notify.
   */
  public void setOnWhitelistUpdateListener(@Nullable OnWhitelistUpdateListener listener) {
    this.listener = listener;
  }

  /**
   * Get the current whitelist.
   *
   * @return List of whitelisted user IDs.
   */
  public List<Long> getWhitelist() {
    return new ArrayList<>(whitelist);
  }

  /**
   * Fetch the whitelist from the remote server.
   *
   * @param userId The ID of the current user.
   */
  public void fetchWhitelist(String userId) {
    String url = "http://192.168.1.2:3003/get_whitelist?user_id=" + userId; // Adjust IP for physical devices

    Request request = new Request.Builder()
      .url(url)
      .build();

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        notifyFailure(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) {
          notifyFailure(new IOException("Unexpected code " + response));
          return;
        }

        String responseBody = response.body().string();
        try {
          JSONObject json = new JSONObject(responseBody);
          JSONArray whitelistArray = json.getJSONArray("whitelist");
          List<Long> fetchedWhitelist = new ArrayList<>();
          for (int i = 0; i < whitelistArray.length(); i++) {
            fetchedWhitelist.add(whitelistArray.getLong(i));
          }
          // Update local cache
          whitelist = fetchedWhitelist;
          saveWhitelistToPrefs(fetchedWhitelist);
          notifySuccess(fetchedWhitelist);
        } catch (JSONException e) {
          notifyFailure(e);
        }
      }
    });
  }

  /**
   * Load the whitelist from SharedPreferences.
   *
   * @return List of whitelisted user IDs.
   */
  private List<Long> loadWhitelistFromPrefs() {
    Set<String> whitelistSet = prefs.getStringSet(KEY_WHITELIST, new HashSet<>());
    List<Long> loadedWhitelist = new ArrayList<>();
    for (String idStr : whitelistSet) {
      try {
        loadedWhitelist.add(Long.parseLong(idStr));
      } catch (NumberFormatException e) {
        // Log or handle invalid format if necessary
      }
    }
    return loadedWhitelist;
  }

  /**
   * Save the whitelist to SharedPreferences.
   *
   * @param whitelist List of whitelisted user IDs.
   */
  private void saveWhitelistToPrefs(List<Long> whitelist) {
    SharedPreferences.Editor editor = prefs.edit();
    Set<String> whitelistSet = new HashSet<>();
    for (Long id : whitelist) {
      whitelistSet.add(id.toString());
    }
    editor.putStringSet(KEY_WHITELIST, whitelistSet);
    editor.apply();
  }

  /**
   * Notify listener of successful update.
   *
   * @param newWhitelist The updated whitelist.
   */
  private void notifySuccess(List<Long> newWhitelist) {
    if (listener != null) {
      mainHandler.post(() -> listener.onWhitelistUpdated(newWhitelist));
    }
  }

  /**
   * Notify listener of failure.
   *
   * @param e The exception that occurred.
   */
  private void notifyFailure(Exception e) {
    if (listener != null) {
      mainHandler.post(() -> listener.onWhitelistUpdateFailed(e));
    }
  }

  @Override
  public void onUserUpdated (TdApi.User user) {
    Log.i("xorworks", "user updated : " + user);
  }

  @Override
  public void onUserFullUpdated (long userId, TdApi.UserFullInfo userFull) {
    Log.i("xorworks", "full user updated : " + user);
  }

  private void addUserListeners (TdApi.User user, boolean add) {
    if (add) {
      tdlib.cache().subscribeToUserUpdates(user.id, this);
    } else {
      tdlib.cache().unsubscribeFromUserUpdates(user.id, this);
    }
  }
}
