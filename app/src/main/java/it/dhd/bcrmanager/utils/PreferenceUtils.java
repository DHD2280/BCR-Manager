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
    private static SharedPreferences mPreferences, mAppPreferences, mStarredPrefs;

    // Prevent instantiation
    private PreferenceUtils() {
    }

    /**
     * Initialize the class
     * @param appContext The application context
     */
    public static void init(Context appContext) {
        mPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mAppPreferences = appContext.getSharedPreferences(PREFS_APP_NAME, Context.MODE_PRIVATE);
        mStarredPrefs = appContext.getSharedPreferences(PREFS_NAME_STARRED, Context.MODE_PRIVATE);
        mDir = getStoredFolderFromPreference();
    }

    public static SharedPreferences getSharedPreferences() {
        return mPreferences;
    }

    /**
     * Get the SharedPreferences for the app
     * @return The SharedPreferences
     */
    public static SharedPreferences getAppPreferences() {
        return mAppPreferences;
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
        return mPreferences.getString(DIRECTORY, "");
    }

    /**
     * Save the folder to the SharedPreferences
     * @param folder The folder to save
     */
    public static void saveFolder(String folder) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(DIRECTORY, folder);
        editor.apply();
    }

    /**
     * Get if registration is starred based on fileName
     * @param filename The filename
     * @return true if starred, false otherwise
     */
    public static boolean isStarred(String filename) {
        return mStarredPrefs.getBoolean(filename, false);
    }

    /**
     * Set if registration is starred
     * @param filename The filename
     * @param starred true if starred, false otherwise
     */
    public static void setStarred(String filename, boolean starred) {
        SharedPreferences.Editor editor = mStarredPrefs.edit();
        if (starred) {
            editor.putBoolean(filename, true);
            editor.apply();
        } else {
            editor.remove(filename).apply();
        }
    }

    /**
     * Remove starred item from SharedPreferences
     * @param fileName The filename of the registration
     */
    public static void removeStarred(String fileName) {
        SharedPreferences.Editor editor = mStarredPrefs.edit();
        editor.remove(fileName).apply();
    }

    /**
     * Get the number of starred registrations
     * @return The number of starred registrations
     */
    public static int getStarredCount() {
        return mStarredPrefs.getAll().size();
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
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong("last_time", l);
        editor.apply();
    }

    public static long getLastTime() {
        SharedPreferences pref = mPreferences;
        return pref.getLong("last_time", 0);
    }

    public static void savePermissions() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PermissionsKeys.PERMISSION_READ_CONTACTS, PermissionsUtil.hasReadContactsPermissions());
        editor.putBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, PermissionsUtil.hasReadCallLogPermissions());
        editor.apply();
    }

    public static boolean getPermissionReadCallLogLastTime() {
        return mPreferences.getBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, false);
    }

    public static boolean getPermissionReadContactsLastTime() {
        return mPreferences.getBoolean(PermissionsKeys.PERMISSION_READ_CALL_LOG, false);
    }

    public static void saveLastFolder() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(LATEST_DIR, PreferenceUtils.getStoredFolderFromPreference());
        editor.apply();
    }

    public static String getLatestFolder() {
        return mPreferences.getString(LATEST_DIR, "");
    }

    public static int getLastTimeFiles() {
        return mPreferences.getInt("last_time_files", 0);
    }

    public static void saveLastFiles(int length) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt("last_time_files", length);
        editor.apply();
    }

    public static void resetStarred() {
        SharedPreferences.Editor editor = mStarredPrefs.edit();
        editor.clear();
        editor.apply();
    }

    public static class PermissionsKeys {
        public static final String PERMISSION_READ_CONTACTS = "permission_read_contacts";
        public static final String PERMISSION_READ_CALL_LOG = "permission_read_call_log";

    }

    public static boolean showLabel() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_SHOW_NUMBER_LABEL, true);
    }

    public static boolean showLabelPlayer() {
        return getAppPreferences().getBoolean(Keys.PREFS_KEY_PLAYER_SHOW_LABEL, true);
    }

    public static boolean showIcon() {
        SharedPreferences pref = getAppPreferences();
        return pref.getBoolean(Keys.PREFS_KEY_SHOW_CONTACT_ICON, true);
    }

    public static boolean showSim(int nSim) {
        SharedPreferences pref = getAppPreferences();
        String sim = pref.getString(Keys.PREFS_KEY_SHOW_SIM_NUMBER, "1");
        // 0 never
        // 1 only if available (iy you have 1 sim it will not show)
        // 2 always
        switch (sim) {
            case "0" -> {
                return false;
            }
            case "1" -> {
                return nSim > 1;
            }
            case "2" -> {
                return true;
            }
        }
        return true;
    }
    public static boolean showSimPlayer(int nSim) {
        SharedPreferences pref = getAppPreferences();
        String sim = pref.getString(Keys.PREFS_KEY_PLAYER_SHOW_SIM, "1");
        // 0 never
        // 1 only if available (iy you have 1 sim it will not show)
        // 2 always
        switch (sim) {
            case "0" -> {
                return false;
            }
            case "1" -> {
                return nSim > 1;
            }
            case "2" -> {
                return true;
            }
        }
        return true;
    }


    /**
     * Keys for the SharedPreferences
     */
    public static class Keys {
        // Item Entry Prefs
        // Contact Icon
        public static final String PREFS_KEY_SHOW_CONTACT_ICON = "show_contact_icon";
        public static final String PREFS_KEY_SHOW_TILES = "show_colored_tiles";
        // Sim
        public static final String PREFS_KEY_SHOW_SIM_NUMBER = "show_sim_info";
        // Number Label
        public static final String PREFS_KEY_SHOW_NUMBER_LABEL = "show_number_label";
        // Call Info Icons Colors
        public static final String PREFS_KEY_DIRECTION_COLOR = "direction_color";
        public static final String PREFS_KEY_DIRECTION_IN_COLOR = "direction_in_color";
        public static final String PREFS_KEY_DIRECTION_OUT_COLOR = "direction_out_color";
        public static final String PREFS_KEY_DIRECTION_CONFERENCE_COLOR = "direction_conference_color";
        public static final String PREFS_KEY_SIM_COLOR = "sim_color";
        public static final String PREFS_KEY_SIM_1_COLOR = "sim_1_color";
        public static final String PREFS_KEY_SIM_2_COLOR = "sim_2_color";
        public static final String PREFS_KEY_NUMBER_LABEL_COLOR = "number_label_color";

        // Bottom Player Prefs
        public static final String PREFS_KEY_PLAYER_SHOW_LABEL = "show_label_player";
        public static final String PREFS_KEY_PLAYER_SHOW_SIM = "show_sim_player";

        // Style Prefs
        public static final String PREFS_KEY_SHOW_HEADERS = "show_headers";
        public static final String PREFS_KEY_DARK_LETTER = "dark_letter_on_dark_mode";

        // App Prefs
        // Theme
        public static final String PREFS_KEY_DYNAMIC_COLOR = "dynamic_colors";
        public static final String PREFS_KEY_THEME_COLOR = "theme_color";
        public static final String PREFS_KEY_DARK_MODE = "dark_mode";
        public static final String PREFS_KEY_VIBRATE = "vibrate";
    }

}
