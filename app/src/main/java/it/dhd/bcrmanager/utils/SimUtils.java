package it.dhd.bcrmanager.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class SimUtils {

    /**
     * Get the number of SIM cards
     * @param context The application context
     * @return The number of SIM cards
     */
    public static int getNumberOfSimCards(Context context) {

        if (PermissionsUtil.hasReadPhoneStatePermissions()) {
            TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);

            if (telephonyManager != null) {
                SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
                if (subscriptionManager != null) {
                    // Get the list of active subscription info
                    // This list will contain information about each active SIM card
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return -1;
                    }
                    SubscriptionInfo activeSubscriptionInfo1 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
                    SubscriptionInfo activeSubscriptionInfo2 = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1);

                    if (activeSubscriptionInfo1 != null && activeSubscriptionInfo2 != null) {
                        // Two active SIM cards
                        return 2;
                    } else if (activeSubscriptionInfo1 != null || activeSubscriptionInfo2 != null) {
                        // One active SIM card
                        return 1;
                    } else {
                        // No active SIM cards
                        return 0;
                    }
                }
            }

            // Unable to determine the number of SIM cards
            return -1;
        } else return 1;
    }

    /**
     * Get the SIM card slot number
     * @param context The application context
     * @param subscriptionId The subscription ID
     * @return The SIM card slot number
     */

    public static String checkSimSlot(Context context, String subscriptionId) {
        if (subscriptionId.equals("1") || subscriptionId.equals("2")) return subscriptionId;
        Log.d("SimUtils", "checkSimSlot: " + subscriptionId);
        if (PermissionsUtil.hasReadPhoneStatePermissions()) {
            //if (getNumberOfSimCards(context) <= 1) return "1";
            SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
            if (subscriptionManager != null) {
                // Get the slot index of the SIM card with the specified phone number
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    return String.valueOf(SubscriptionManager.getSlotIndex(Integer.parseInt(subscriptionId)));
                } else {
                    return "1";
                }
            } else {
                return "-1";
            }
        } else {
            return "1";
        }
    }

    public static int getSimSlotIndexFromAccountId(Context context, String accountIdToFind) {
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return -1;
        }
        for (int index = 0; index < telecomManager.getCallCapablePhoneAccounts().size(); index++) {
            PhoneAccountHandle account = telecomManager.getCallCapablePhoneAccounts().get(index);
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(account);
            String accountId = phoneAccount.getAccountHandle().getId();
            if (accountIdToFind.equals(accountId)) {
                return index;
            }
        }
        int parsedAccountId = Integer.parseInt(accountIdToFind);
        if (parsedAccountId >= 0) {
            return parsedAccountId;
        }
        return -1;
    }

}
