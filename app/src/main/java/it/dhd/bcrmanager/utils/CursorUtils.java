package it.dhd.bcrmanager.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.dhd.bcrmanager.BuildConfig;

public class CursorUtils {

    /**
     * Get the contact name from the phone number
     * Search in contact database with cursor
     * @param context The application context
     * @param phoneNumber The phone number to check in contact database
     * @return The contact name or null if not found
     */
    public static String getContactName(Context context, String phoneNumber) {
        String contactName = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{
                        ContactsContract.PhoneLookup.DISPLAY_NAME
                },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve file information from the cursor
                contactName = cursor.getString(0);
                if (contactName == null || contactName.isEmpty()) contactName = phoneNumber;
                Log.d("CursorUtils.getContactName", "Contact: " + contactName + ", Number: " + phoneNumber);
            } while (cursor.moveToNext());

        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return contactName;
    }

    public static String getContactNameForLookupKey(Context context, String lookupKey) {
        String contactName = null;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                ContactsContract.Contacts.LOOKUP_KEY + "=?",
                new String[]{lookupKey},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve file information from the cursor
                contactName = cursor.getString(0);
            } while (cursor.moveToNext());

        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return contactName;

    }

    /**
     * Get all numbers of a contact
     * @param context The application context
     * @param lookupKey The lookup key of the contact to get numbers
     * @return A list of numbers
     */
    public static List<String> getPhoneNumbersForContact(Context context, String lookupKey) {
        List<String> phoneNumbers = new ArrayList<>();

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[] {ContactsContract.Contacts._ID},
                ContactsContract.Contacts.LOOKUP_KEY + "=?",
                new String[]{lookupKey},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            String contactId =
                    cursor.getString(0);
            //
            //  Get all phone numbers.
            //
            Cursor phones = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE },
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                    null);
            while (phones != null && phones.moveToNext()) {
                String number = phones.getString(0);
                phoneNumbers.add(number);
            }
            if(phones != null && !phones.isClosed()) {
                phones.close();
            }
        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        Log.d("CursorUtils.getPhoneNumbersForContact", "Contact: " + lookupKey + ", Numbers: " + phoneNumbers);
        return phoneNumbers;
    }

    /**
     * Get the contact name from the phone number
     * @param context The application context
     * @param phoneNumber The phone number to check in contact database
     * @return String[] {contactId, contactName, contactIcon (Uri.toString()), isContactSaved (based on contactName == null),
     * defaultIcon based on isContactSaved, lookupKey, lastUpdated (timestamp)}
     */
    public static String[] getContactInfo(Context context, String phoneNumber) {
        long contactId = 0;
        String contactName = null, contactIcon = null, lookupKey = null;
        boolean isContactSaved = false, defaultIcon = true;
        int version = 0;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{
                        ContactsContract.PhoneLookup._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_URI,
                        ContactsContract.PhoneLookup.LOOKUP_KEY
                },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve file information from the cursor
                contactId = cursor.getLong(0);
                contactName = cursor.getString(1);
                contactIcon = cursor.getString(2);
                lookupKey = cursor.getString(3);
                if (contactName == null || contactName.isEmpty()) contactName = phoneNumber;
                if (contactName!=null) {
                    isContactSaved = true;
                    if (contactIcon != null) defaultIcon = false;
                }
            } while (cursor.moveToNext());
        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        if (BuildConfig.DEBUG)
            Log.d("CursorUtils.getContactInfo", "Contact: " + contactName + ", ID:" + contactId + ", Number: " + phoneNumber + ", saved: " + (contactName != null) + ", default icon" + (contactIcon== null));
        return new String[] {String.valueOf(contactId), contactName, contactIcon, String.valueOf(isContactSaved), String.valueOf(defaultIcon), lookupKey, String.valueOf(version)};
    }

    /**
     * Build the lookup uri from contact id and lookup key
     * @param contactId The contact id
     * @param lookupKey The lookup key of the contact
     * @return The lookup uri
     */
    public static Uri buildLookupUri(long contactId, String lookupKey) {
        return ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
    }

    /**
     * Get contact name from the call logs
     * @param context The application context
     * @param phoneNumber The phone number to check in call logs
     * @return The contact name or empty if not found
     */
    public static String getContactNameFromCallLogs(Context context, String phoneNumber) {
        String callLogName = "";
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.FEATURES
                },
                CallLog.Calls.NUMBER + "=?",
                new String[]{phoneNumber},
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            callLogName = cursor.getString(2);
            Log.d("CursorUtils.getContactNameFromCallLogs", "getContactNameFromCallLogs Contact: " + callLogName + ", Number: " + phoneNumber);
        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return callLogName;
    }

    public static String[] searchThingsInCallLog(Context context, String[] callItem) {
        // String[]{date, time, contactNumber};
        String date = callItem[0];
        String time = callItem[1];
        String dateTime = date + time;
        Date dateTimeLong = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

        try {
            // Parsing della stringa combinata
            dateTimeLong = dateFormat.parse(dateTime);

            // Ottieni il valore numerico di Date.getTime()
            long timeInMillis = dateTimeLong.getTime();

            // Output del risultato
        } catch (Exception e) {
            // Gestisci l'eccezione di parsing
            e.printStackTrace();
        }
        String phoneNumber = callItem[2];
        String duration = "", type = "", sim = "", name = "";
        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.PHONE_ACCOUNT_ID,
                        CallLog.Calls.CACHED_NAME
                },
                CallLog.Calls.NUMBER + "=?",
                new String[]{phoneNumber},
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Retrieve file information from the cursor
                String timestamp = cursor.getString(1);
                String timestampNoMillis = timestamp.substring(0, timestamp.length() - 3);
                if (dateTimeLong == null) return null;
                String dateTimeNoMillis = String.valueOf(dateTimeLong.getTime()).substring(0, String.valueOf(dateTimeLong.getTime()).length() - 3);
                if (timestampNoMillis.equals(dateTimeNoMillis)) {
                    // Retrieve file information from the cursor
                    duration = cursor.getString(2);
                    type = checkType(cursor.getInt(3));
                    sim = SimUtils.checkSimSlot(context, cursor.getString(4));
                    name = cursor.getString(5);
                    Log.d("CursorUtils.searchThingsInCallLog", "searchThingsInCallLog Contact: " + name + ", Number: " + phoneNumber + ", Duration: " + duration + ", Type: " + type + ", Sim: " + sim);
                    break;
                }
            } while (cursor.moveToNext());

        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return new String[]{duration, type, sim, name};
    }

    private static String checkType(int callType) {
        switch (callType) {
            case CallLog.Calls.INCOMING_TYPE -> {
                return "in";
            }
            case CallLog.Calls.OUTGOING_TYPE -> {
                return "out";
            }
            default -> {
                return "";
            }
        }
    }
}
