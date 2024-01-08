package it.dhd.bcrmanager.utils;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionsUtil {

    private static Context mAppContext;

    private static final String PERMISSION_PREFERENCE = "bcr_manager_permissions";

    public static void init(Context appContext) {
        mAppContext = appContext;
    }


    public static boolean hasReadContactsPermissions() {

        return hasPermission(permission.READ_CONTACTS);
    }

    public static boolean hasWriteContactsPermissions() {
        return hasPermission(permission.WRITE_CONTACTS);
    }

    public static boolean hasReadPhoneStatePermissions() {
        return hasPermission(permission.READ_PHONE_STATE);
    }

    public static boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mAppContext, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReadCallLogPermissions() {
        return hasPermission(permission.READ_CALL_LOG);
    }

}
