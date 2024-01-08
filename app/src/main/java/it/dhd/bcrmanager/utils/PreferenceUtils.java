package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Class to manage SharedPreferences
 */
public class PreferenceUtils {

    public static String PREFS_NAME = "it.dhd.bcrmanager";
    public static String PREFS_APP_NAME = "it.dhd.bcrmanager_preferences";
    public static String PREFS_NAME_STARRED = "it.dhd.bcrmanager.starred";
    public static String DIRECTORY = "bcr_directory";
    public static String LATEST_DIR = "latest_bcr_dir";

    public static String mDir = "";

    private static Context mAppContext;

    // Prevent instantiation
    private PreferenceUtils() {
    }

    /**
     * Initialize the class
     * @param appContext The application context
     */
    public static void init(Context appContext) {
        mAppContext = appContext;
        mDir = getStoredFolderFromPreference();
    }

    public static SharedPreferences getSharedPreferences() {
        return mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the SharedPreferences for the app
     * @return The SharedPreferences
     */
    public static SharedPreferences getAppPreferences() {
        return mAppContext.getSharedPreferences(PREFS_APP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if first time the app is launched
     * @return true if first time, false otherwise
     */
    public static boolean isFirstTime() {
        SharedPreferences pref = getSharedPreferences();
        return pref.getBoolean("first_time", true);
    }

    /**
     * Set the first time flag to false
     */
    public static void setFirstTime() {
        SharedPreferences pref = getSharedPreferences();
        pref.edit().putBoolean("first_time", false).apply();
    }

    /**
     * Get the stored folder from the SharedPreferences
     * @return The stored folder (String, need to Uri.parse)
     */
    public static String getStoredFolderFromPreference() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getString(DIRECTORY, "");
    }

    /**
     * Save the folder to the SharedPreferences
     * @param folder The folder to save
     */
    public static void saveFolder(String folder) {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(DIRECTORY, folder);
        editor.apply();
    }

    /**
     * Get if registration is starred based on fileName
     * @param filename The filename
     * @return true if starred, false otherwise
     */
    public static boolean isStarred(String filename) {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME_STARRED, Context.MODE_PRIVATE);
        return pref.getBoolean(filename, false);
    }

    /**
     * Set if registration is starred
     * @param filename The filename
     * @param starred true if starred, false otherwise
     */
    public static void setStarred(String filename, boolean starred) {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME_STARRED, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (starred) {
            editor.putBoolean(filename, true);
            editor.apply();
        } else {
            editor.remove(filename).apply();
        }

    }

    /**
     * Get the number of starred registrations
     * @return The number of starred registrations
     */
    public static int getStarredCount() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME_STARRED, Context.MODE_PRIVATE);
        return pref.getAll().size();
    }


    public static boolean showTiles() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_SHOW_TILES, true);
    }

    public static boolean showHeaders() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_SHOW_HEADERS, true);
    }

    public static boolean darkLetter() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_DARK_LETTER, true);
    }

    public static boolean hasVibrate() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_VIBRATE, true);
    }

    public static void saveLastTime(long l) {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong("last_time", l);
        editor.apply();
    }

    public static long getLastTime() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getLong("last_time", 0);
    }

    public static void savePermissions() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PermissionsKeys.PERMISSION_READ_CONTACTS, PermissionsUtil.hasReadContactsPermissions());
        editor.putBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, PermissionsUtil.hasReadCallLogPermissions());
        editor.apply();
    }

    public static boolean getPermissionReadCallLogLastTime() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, false);
    }

    public static boolean getPermissionReadContactsLastTime() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, false);
    }

    public static void saveLastFolder() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(LATEST_DIR, PreferenceUtils.getStoredFolderFromPreference());
        editor.apply();
    }

    public static String getLatestFolder() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getString(LATEST_DIR, "");
    }

    public static int getLastTimeFiles() {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getInt("last_time_files", 0);
    }

    public static void saveLastFiles(int length) {
        SharedPreferences pref = mAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("last_time_files", length);
        editor.apply();
    }

    public static class PermissionsKeys {
        public static final String PERMISSION_READ_CONTACTS = "permission_read_contacts";
        public static final String PERMISSION_READ_CALL_LOG = "permission_read_call_log";

    }

    /**
     * Keys for the SharedPreferences
     */
    public static class Keys {
        public static final String PREFS_KEY_SHOW_TILES = "show_colored_tiles";
        public static final String PREFS_KEY_SHOW_HEADERS = "show_headers";
        public static final String PREFS_KEY_DARK_LETTER = "dark_letter_on_dark_mode";
        public static final String PREFS_KEY_VIBRATE = "vibrate";
    }

}
