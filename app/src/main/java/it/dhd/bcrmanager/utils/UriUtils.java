package it.dhd.bcrmanager.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import java.io.File;
import java.util.List;

/** Utility methods for dealing with URIs. */
public class UriUtils {

    private static final String LOOKUP_URI_ENCODED = "encoded";

    /** Static helper, not instantiable. */
    private UriUtils() {}

    /** Checks whether two URI are equal, taking care of the case where either is null. */
    public static boolean areEqual(Uri uri1, Uri uri2) {
        if (uri1 == null && uri2 == null) {
            return true;
        }
        if (uri1 == null || uri2 == null) {
            return false;
        }
        return uri1.equals(uri2);
    }

    /** Parses a string into a URI and returns null if the given string is null. */
    public static Uri parseUriOrNull(String uriString) {
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }

    /** Converts a URI into a string, returns null if the given URI is null. */
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    public static boolean isEncodedContactUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        final String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            return false;
        }
        return lastPathSegment.equals(LOOKUP_URI_ENCODED);
    }

    /**
     * @return {@code uri} as-is if the authority is of contacts provider. Otherwise or {@code uri} is
     *     null, return null otherwise
     */
    public static Uri nullForNonContactsUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        return ContactsContract.AUTHORITY.equals(uri.getAuthority()) ? uri : null;
    }

    /** Parses the given URI to determine the original lookup key of the contact. */
    public static String getLookupKeyFromUri(Uri lookupUri) {
        // Would be nice to be able to persist the lookup key somehow to avoid having to parse
        // the uri entirely just to retrieve the lookup key, but every uri is already parsed
        // once anyway to check if it is an encoded JSON uri, so this has negligible effect
        // on performance.
        if (lookupUri != null && !UriUtils.isEncodedContactUri(lookupUri)) {
            final List<String> segments = lookupUri.getPathSegments();
            // This returns the third path segment of the uri, where the lookup key is located.
            // See {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}.
            return (segments.size() < 3) ? null : Uri.encode(segments.get(2));
        } else {
            return null;
        }
    }

    public static boolean deleteFile(Uri fileUri, Context context) {
        try {
            // Get the file path from the Uri
            String filePath = getFilePathFromUri(fileUri, context);

            if (filePath != null) {
                // Create a File object from the file path
                File file = new File(filePath);

                // Delete the file
                return file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String getFilePathFromUri(Uri uri, Context context) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                filePath = cursor.getString(columnIndex);
            }
        }

        return filePath;
    }

    public static void sendIntentWithAppend(Context context, String action, Uri URI, String append) {
        Intent intent = new Intent(action);
        Uri uri = Uri.withAppendedPath(URI,
                append);
        intent.setData(uri);
        context.startActivity(intent);
    }


    @SuppressLint("Range")
    public static String fetchContactIdFromPhoneNumber(Context mContext, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor cursor = mContext.getContentResolver().query(uri,
                new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID },
                null, null, null);

        String contactId = "";

        if (cursor.moveToFirst()) {
            do {
                contactId = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.PhoneLookup._ID));
            } while (cursor.moveToNext());
        }

        return contactId;
    }

    public static Uri getPhotoUri(Context mContext, long contactId) {
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            Cursor cursor = contentResolver
                    .query(ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID
                                    + "="
                                    + contactId
                                    + " AND "

                                    + ContactsContract.Data.MIMETYPE
                                    + "='"
                                    + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                    + "'", null, null);

            if (cursor != null) {
                if (!cursor.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Uri person = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, contactId);
        return Uri.withAppendedPath(person,
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

}
