package it.dhd.bcrmanager.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import it.dhd.bcrmanager.json.UriJsonAdapter;

public class FileUtils {

    public static final String STORED_REG = "stored_reg.json";
    public static final String STORED_CONTACTS = "stored_contacts.json";

    /**
     * Delete a file from the storage
     * @param context The application context
     * @param fileUri The file Uri
     */
    public static void deleteFile(Context context, Uri fileUri) {

        String path = getPathFromUri(context, fileUri);
        Log.d("FileUtils", "Path: " + path);

        if (path==null) {
            Log.d("FileUtils", "Path is null");
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                Log.d("FileUtils", "File deleted");
            } else {
                Log.d("FileUtils", "File not deleted");
            }
        } else {
            Log.d("FileUtils", "File not found");
        }
        /*if (documentFile != null && documentFile.exists()) {
            documentFile.delete();
        }*/
    }

    public static long getLastModifiedFolder(Context context) {
        String storedUriString = PreferenceUtils.getStoredFolderFromPreference();
        Uri treeUri = Uri.parse(storedUriString);
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
        return pickedDir != null ? pickedDir.lastModified() : 0;
    }

    public static void deleteFileUri(Context context, Uri fileUri) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
        ContentResolver contentResolver = context.getContentResolver();

        try {
            DocumentsContract.deleteDocument(contentResolver, fileUri);
        } catch (FileNotFoundException e) {
            Log.d("FileUtils", "File not found");
        }
    }

    public static String getPathFromUri(final Context context, final Uri uri) {

        Log.d("FileUtils", "getPathFromUri: " + uri.toString());
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static <T> void saveObjectList(Context context, List<T> objectList, String fileName, Class<T> objectClass) {
        File file = new File(context.getFilesDir(), fileName);

        try (FileWriter writer = new FileWriter(file, false)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Uri.class, new UriJsonAdapter());
            Gson gson = gsonBuilder.create();
            Type type = TypeToken.getParameterized(List.class, objectClass).getType();
            String json = gson.toJson(objectList, type);

            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // Gestire l'eccezione in base alle esigenze dell'app
        }
    }

    // Metodo per caricare una lista di oggetti da un file JSON
    public static <T> List<T> loadObjectList(Context context, String fileName, Class<T> objectClass) {
        Log.d("FileUtils", "loadObjectList: " + fileName);
        File file = new File(context.getFilesDir(), fileName);

        try {

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Uri.class, new UriJsonAdapter());
            Gson gson = gsonBuilder.create();
            Type type = TypeToken.getParameterized(List.class, objectClass).getType();

            return gson.fromJson(FileUtils.readFile(file), type);
        } catch (IOException e) {
            e.printStackTrace();
            // Gestire l'eccezione in base alle esigenze dell'app
            return null;
        }
    }

    /**
     * Read a file and return its content as a String
     * @param file The file to read
     * @return The file content as a String
     * @throws IOException If the file doesn't exist
     */
    public static String readFile(File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return stringBuilder.toString();
    }

    public static boolean fileExists(Context context, Uri pickedDir, String fileName) {
        File f = new File(getPathFromUri(context, pickedDir) + "/" + fileName);
        return f.exists();
    }
}
