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

}
