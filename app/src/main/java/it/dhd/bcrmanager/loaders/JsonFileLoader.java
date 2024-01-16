package it.dhd.bcrmanager.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.loader.content.AsyncTaskLoader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.drawable.LetterTileDrawable;
import it.dhd.bcrmanager.json.Call;
import it.dhd.bcrmanager.json.CallLogResponse;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.utils.CursorUtils;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.ShortcutUtils;
import it.dhd.bcrmanager.utils.UriUtils;


public class JsonFileLoader extends AsyncTaskLoader<JsonFileLoader.TwoListsWrapper> {
    private final String storedUriString;

    boolean hasReadContactsPermission, hasWriteContactsPermission, hasReadCallLogPermission, hasReadPhoneStatePermission;

    public List<ContactItem> contactList = new ArrayList<>();
    private boolean onlyStarred;
    private boolean onlySelectedContact;
    private String selectedContact;
    private final List<String> contactNumbers = new ArrayList<>();
    private List<CallLogItem> yourListOfItems = new ArrayList<>();

    private final List<String> fileError = new ArrayList<>();

    /**
     * Constructor for JsonFileLoader class
     * @param context The application context
     * @param lookupKey The lookup key of the selected contact
     * This will load the JSON files from the selected directory.
     * The lookup key is used to filter the JSON files for the selected contact.
     * Lookup key can be empty if we haven't any particular contact.
     * "starred_contacts" if we want to load only starred contacts.
     **/
    public JsonFileLoader(Context context, String lookupKey) {
        super(context);
        this.storedUriString = PreferenceUtils.getStoredFolderFromPreference();
        if (lookupKey.equals("starred_contacts")) onlyStarred = true;
        else if (!lookupKey.isEmpty()) {
            selectedContact = lookupKey;
            onlySelectedContact = true;
        } else {
            onlyStarred = false;
            onlySelectedContact = false;
        }
    }

    @Override
    protected void onStartLoading() {
        if (isStarted()) forceLoad();
    }

    /**
     * Load the JSON files from the selected directory
     * This will do the parsing.
     * It will parse all JSON files based on constructor.
     * Will check if some permissions are set, otherwise will not use Cursor, so will not parse contacts.
     * @return A TwoListsWrapper object containing the list of starred items and the list of all items
     */
    @Override
    public TwoListsWrapper loadInBackground() {

        boolean needToParse = true, contactsChanged = false;

        hasReadContactsPermission = PermissionsUtil.hasReadContactsPermissions();
        hasWriteContactsPermission = PermissionsUtil.hasWriteContactsPermissions();
        hasReadCallLogPermission = PermissionsUtil.hasReadCallLogPermissions();
        hasReadPhoneStatePermission = PermissionsUtil.hasReadPhoneStatePermissions();

        // Here will check if we have selectedContact to filter JSON files
        if (onlySelectedContact && hasReadContactsPermission)
            contactNumbers.addAll(CursorUtils.getPhoneNumbersForContact(getContext(), selectedContact));

        if (storedUriString != null && !storedUriString.isEmpty()) {

            Uri treeUri = Uri.parse(storedUriString);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), treeUri);


            Log.d("JsonFileLoader.loadInBackground", "treeUri: " + treeUri + ", pickedDir: " + pickedDir);
            if (pickedDir != null && pickedDir.isDirectory()) {
                long lastModified = pickedDir.lastModified();
                long lastTimeParsed = PreferenceUtils.getLastTime();
                Log.d("JsonFileLoader.loadInBackground", "lastModified: " + lastModified + ", lastTimeParsed: " + lastTimeParsed);
                if (PreferenceUtils.getPermissionReadContactsLastTime() != hasReadContactsPermission ||
                        PreferenceUtils.getPermissionReadCallLogLastTime() != hasReadCallLogPermission) {
                    ShortcutManagerCompat.removeAllDynamicShortcuts(getContext());
                    Log.d("JsonFileLoader.loadInBackground", "Permissions changed");
                    removeFiles();
                }
                if (!storedUriString.equals(PreferenceUtils.getLatestFolder())) {
                    Log.d("JsonFileLoader.loadInBackground", "Folder changed");
                    yourListOfItems.clear();
                    contactList.clear();
                    PreferenceUtils.resetStarred();
                    removeFiles();
                }
                if (new File(getContext().getFilesDir(), FileUtils.STORED_REG).exists() &&
                        new File(getContext().getFilesDir(), FileUtils.STORED_CONTACTS).exists()) {
                    Log.d("JsonFileLoader.loadInBackground", "Files exists, imports them");
                    yourListOfItems = FileUtils.loadObjectList(getContext(), FileUtils.STORED_REG, CallLogItem.class);
                    contactList = FileUtils.loadObjectList(getContext(), FileUtils.STORED_CONTACTS, ContactItem.class);
                    StringBuilder contactNumbersBuilder = new StringBuilder();
                    if (contactList != null && contactList.size() != 0 && yourListOfItems != null  && yourListOfItems.size() > 0 && hasReadContactsPermission && hasReadCallLogPermission) {
                        for (ContactItem contact : contactList) {
                            boolean hasChanged = false;

                            Cursor cursor = getContext().getContentResolver().query(
                                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact.getPhoneNumber())),
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
                                    if (cursor.getString(1).isEmpty()) {
                                        // Probably contact was deleted
                                        hasChanged = true;
                                        contact.resetContact();
                                    } else {
                                        // Name changed
                                        if (!TextUtils.equals(contact.getContactName(), cursor.getString(1))) {
                                            hasChanged = true;
                                            contact.setContactName(cursor.getString(1));
                                            if (!contact.isContactSaved()) {
                                                contact.setContactSaved(true);
                                                contact.setContactType(LetterTileDrawable.TYPE_PERSON);
                                            }
                                        }
                                    }
                                    // Check contact Image
                                    if (!UriUtils.areEqual(contact.getContactImage(), UriUtils.parseUriOrNull(cursor.getString(2)))) {
                                        hasChanged = true;
                                        contact.setContactImage(UriUtils.parseUriOrNull(cursor.getString(2)));
                                    }
                                    // Check contact LookupKey
                                    if (!TextUtils.equals(contact.getLookupKey(), cursor.getString(3))) {
                                        hasChanged = true;
                                        contact.setLookupKey(cursor.getString(3));
                                    }
                                    // Check contact ID
                                    if (contact.getContactId() != cursor.getLong(0)) {
                                        hasChanged = true;
                                        contact.setContactId(cursor.getLong(0));
                                    }
                                    // Check number label
                                    if (!TextUtils.isEmpty(cursor.getString(3))) {
                                        Cursor phones = getContext().getContentResolver().query(
                                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                new String[] {
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                                                        ContactsContract.CommonDataKinds.Phone.TYPE,
                                                        ContactsContract.CommonDataKinds.Phone.LABEL } ,
                                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + cursor.getLong(0),
                                                null,
                                                null);
                                        while (phones != null && phones.moveToNext()) {
                                            if (PhoneNumberUtils.compare(contact.getPhoneNumber(), phones.getString(0))) {
                                                // Number Type changed
                                                if (contact.getNumberType() != phones.getInt(1)) {
                                                    hasChanged = true;
                                                    contact.setContactType(phones.getInt(1));
                                                }
                                                String numberLabel;
                                                                                                if (phones.getInt(1) == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                                                    numberLabel = phones.getString(2);
                                                else numberLabel = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(getContext().getResources(), phones.getInt(1), "");
                                                if (TextUtils.equals(contact.getNumberLabel(), numberLabel)) {
                                                    hasChanged = true;
                                                    contact.setNumberLabel(numberLabel);
                                                }
                                                break;
                                            }
                                        }
                                        if(phones != null && !phones.isClosed()) {
                                            phones.close();
                                        }
                                    }
                                    if (hasChanged) {
                                        contactNumbersBuilder.append("N:").append(contact.getPhoneNumber()).append(";");
                                    }
                            } else {
                                // Probably contact was deleted
                                hasChanged = true;
                                contact.resetContact();
                            }
                            if(cursor != null && !cursor.isClosed()) {
                                cursor.close();
                            }
                            if (hasChanged) {
                                contactNumbersBuilder.append("N:").append(contact.getPhoneNumber()).append(";");
                            }
                        }
                        String[] changedContacts = contactNumbersBuilder.toString().split(";");
                        contactsChanged = !(changedContacts.length > 0);
                        for (String changedContact : changedContacts) {
                            String[] contactInfo = changedContact.split(":");
                            if (contactInfo[0].equals("N")) {
                                for (CallLogItem item : yourListOfItems) {
                                    if (!PhoneNumberUtils.compare(item.getNumber(), contactInfo[1])) continue;
                                    ContactItem contact = getContactItemFromList(contactInfo[1]);
                                    if (contact == null) continue;
                                    item.setContactName(contact.getContactName());
                                    item.setContactIcon(contact.getContactImage());
                                    item.setLookupKey(contact.getLookupKey());
                                    item.setContactSaved(contact.isContactSaved());
                                    item.setContactType(contact.getContactType());
                                    item.setNumberLabel(contact.getNumberLabel());
                                    item.setNumberType(contact.getNumberType());
                                }
                            }
                        }
                    }
                }
                if (lastModified > lastTimeParsed) {
                    Log.d("JsonFileLoader.loadInBackground", "lastModified > lastTimeParsed");
                    PreferenceUtils.saveLastTime(pickedDir.lastModified());
                } else {
                    if (yourListOfItems != null && yourListOfItems.size() != 0) {
                        needToParse = false;
                    }
                }
                // Get the list of files in the directory
                String documentId = DocumentsContract.getTreeDocumentId(treeUri);

                // Build a URI for the root directory
                Uri dirUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);

                // Use ContentResolver to query for files in the directory
                Cursor fileCursor = getContext().getContentResolver().query(
                        dirUri,
                        new String[]{
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_MIME_TYPE,
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        },
                        null,
                        null,
                        null
                );
                List<String> fileNames = new ArrayList<>();
                if (yourListOfItems != null && yourListOfItems.size() != 0) {
                    // Store a List of file parsed
                    for (CallLogItem callLogItem : yourListOfItems) {
                        fileNames.add(callLogItem.getFileName());
                    }
                }
                String path = FileUtils.getPathFromUri(getContext(), pickedDir.getUri());
                if (path != null) {
                    File dir = new File(path);
                    List<String> filesInFolder = new ArrayList<>(Arrays.asList(dir.list()));
                    // Remove stored regs if file is not present
                    if (PreferenceUtils.getLastTimeFiles() != filesInFolder.size() &&
                            PreferenceUtils.getLastTimeFiles() != 0 && yourListOfItems.size() != 0) {
                        yourListOfItems.removeIf(callLogItem -> filesInFolder.contains(callLogItem.getFileName()));
                    }
                }

                if (fileCursor != null && fileCursor.moveToFirst() && needToParse) {
                    if (yourListOfItems == null) yourListOfItems = new ArrayList<>();
                    do {
                        // Retrieve file information from the cursor
                        String fileName = fileCursor.getString(0);
                        String mimeType = fileCursor.getString(1);
                        Log.d("JsonFileLoader.loadInBackground", "File: " + fileName + ", Type: " + mimeType);
                        if (fileName.startsWith(".")) continue;
                        if (fileNames.contains(fileName)) {
                            Log.d("JsonFileLoader.loadInBackground", "File already parsed: " + fileName + ", skipping");
                            continue;
                        }
                        if (mimeType.equals("application/json")) {
                            if (onlyStarred && !PreferenceUtils.isStarred(fileName)) {
                                continue;
                            }
                            // Here we parse the JSON file because we haven't parsed it yet
                            Log.d("JsonFileLoader.loadInBackground", "File not parsed: " + fileName);
                            StringBuilder content = new StringBuilder();
                            try {
                                InputStream inputStream = getContext().getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(treeUri, fileCursor.getString(2)));
                                if (inputStream != null) {

                                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                                    CharArrayWriter charArrayWriter = new CharArrayWriter();
                                    char[] buffer = new char[1024];
                                    int bytesRead;
                                    InputStreamReader reader = new InputStreamReader(bufferedInputStream);
                                    while ((bytesRead = reader.read(buffer)) != -1) {
                                        charArrayWriter.write(buffer, 0, bytesRead);
                                    }
                                    content.append(charArrayWriter.toCharArray());

                                    reader.close();
                                    inputStream.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            String jsonResponse = content.toString();
                            try {
                                Gson gson = new GsonBuilder().create();
                                CallLogResponse callLogResponse = gson.fromJson(jsonResponse, CallLogResponse.class);

                                // Access the properties you need
                                String timestamp = callLogResponse.getUnixTimestamp();
                                String direction = callLogResponse.getDirection();
                                int simSlot = callLogResponse.getSimSlot();
                                String callLogName, phoneNumber = "", phoneNumberFormatted = "";
                                List<Call> calls = callLogResponse.getCalls();
                                if (calls != null && calls.size() > 0) {
                                    Call firstCall = calls.get(0);
                                    phoneNumber = firstCall.getPhoneNumber();
                                    phoneNumberFormatted = firstCall.getPhoneNumberFormatted();
                                }
                                if (onlySelectedContact && !contactNumbers.contains(phoneNumber) && !contactNumbers.contains(phoneNumberFormatted)) {
                                    continue;
                                }
                                double durationSecsTotal = callLogResponse.getOutput().getRecording().getDurationSecsTotal();
                                String fileOutput = callLogResponse.getOutput().getFormat().getType();
                                fileOutput = fileOutput.split("\\.")[0];

                                String replacement = "";
                                if (fileOutput.contains("OGG")) {
                                    replacement = ".oga";
                                } else if (fileOutput.contains("MP3")) {
                                    replacement = ".mp3";
                                } else if (fileOutput.contains("WAV")) {
                                    replacement = ".wav";
                                } else if (fileOutput.contains("FLAC")) {
                                    replacement = ".flac";
                                } else if (fileOutput.contains("M4A")) {
                                    replacement = ".m4a";
                                }
                                Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(
                                        treeUri,
                                        DocumentsContract.getTreeDocumentId(treeUri) + "/" + fileName
                                );
                                Uri audioFileUri = DocumentsContract.buildDocumentUriUsingTree(
                                        treeUri,
                                        DocumentsContract.getTreeDocumentId(treeUri) + "/" + fileName.replace(".json", replacement)
                                );
                                ContactItem contactItemFound = searchContactItem(phoneNumber, phoneNumberFormatted, callLogResponse.getCallLogName());

                                if (!contactItemFound.isContactSaved() && contactItemFound.getContactType() == LetterTileDrawable.TYPE_GENERIC_AVATAR)
                                    callLogName = phoneNumberFormatted;
                                else callLogName = contactItemFound.getContactName();
                                yourListOfItems.add(new CallLogItem(contactItemFound.getContactImage(), contactItemFound.getLookupKey(), contactItemFound.isContactSaved(), contactItemFound.getContactType(),
                                        callLogName, phoneNumber, phoneNumberFormatted, contactItemFound.getNumberLabel(), contactItemFound.getNumberType(), direction,
                                        timestamp != null ? Long.parseLong(timestamp) : 0, durationSecsTotal, simSlot,
                                        audioFileUri.toString(), fileUri.toString(), fileName, PreferenceUtils.isStarred(fileName)));

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //}
                            Log.d("File Info", "Name: " + fileName + ", Type: " + mimeType);
                        } else if (mimeType.contains("audio")) {
                            String jsonFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".json";
                            if (FileUtils.fileExists(getContext(), pickedDir.getUri(), jsonFileName)) {
                                Log.d("JsonFileLoader.loadInBackground", "File exists: " + jsonFileName + ", skipping");
                                continue;
                            } else {
                                Log.d("JsonFileLoader.loadInBackground", "File not exists: " + jsonFileName + ", try to parse");
                                Uri audioFileUri = DocumentsContract.buildDocumentUriUsingTree(
                                        treeUri,
                                        DocumentsContract.getTreeDocumentId(treeUri) + "/" + fileName
                                );
                                if (onlyStarred && !PreferenceUtils.isStarred(fileName)) {
                                    continue;
                                }
                                Date dateCheck;

                                String direction = getDirection(fileName);

                                try {
                                    dateCheck = DateUtils.parse(fileName);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    dateCheck = forceDate(fileName);
                                }

                                if (dateCheck.getTime()==0) {
                                    fileError.add(fileName);
                                }
                                String[] callLogSearch = null;
                                Log.d("JsonFileLoader.loadInBackground", "filename: " + fileName + ", dateCheck: " + dateCheck.getTime());
                                if (hasReadCallLogPermission)
                                    callLogSearch = CursorUtils.searchThingsInCallLog(getContext(), dateCheck.getTime());

                                String number, formatted, dur, dir, sim, name;
                                Log.d("JsonFileLoader.loadInBackground", "filename: " + fileName + ", callLogSearch: " + Arrays.toString(callLogSearch));
                                if (callLogSearch != null) {
                                    number = callLogSearch[0];
                                    if (!callLogSearch[1].isEmpty()) dur = callLogSearch[1]; else dur = "0";
                                    dir = callLogSearch[2];
                                    if (!callLogSearch[3].isEmpty()) sim = callLogSearch[3]; else sim = "1";
                                    name = callLogSearch[4];
                                } else {
                                    number = "0";
                                    dur = "0";
                                    dir = TextUtils.isEmpty(direction) ? "" : direction;
                                    sim = "0";
                                    name = getContext().getString(android.R.string.unknownName);
                                }
                                if (!number.isEmpty()) formatted = PhoneNumberUtils.formatNumber(number, Locale.getDefault().getCountry());
                                else formatted = "";

                                ContactItem contactItemFound = searchContactItem(number, formatted, name);

                                if (onlySelectedContact && !number.isEmpty() &&
                                        !contactItemFound.getPhoneNumber().isEmpty() &&
                                        !contactNumbers.contains(contactItemFound.getPhoneNumber()) &&
                                        !contactNumbers.contains(contactItemFound.getPhoneNumberFormatted())) {
                                    continue;
                                }

                                yourListOfItems.add(new CallLogItem(contactItemFound.getContactImage(), contactItemFound.getLookupKey(), contactItemFound.isContactSaved(), contactItemFound.getContactType(),
                                        contactItemFound.getContactName(), number, formatted, contactItemFound.getNumberLabel(), contactItemFound.getNumberType(), dir, dateCheck.getTime(), Double.parseDouble(dur), Integer.parseInt(sim),
                                        audioFileUri.toString(), audioFileUri.toString(), fileName, PreferenceUtils.isStarred(fileName)));

                            }
                        }
                        // Continue processing the file information as needed
                        Log.d("File Info", "Name: " + fileName + ", Type: " + mimeType);
                    } while (fileCursor.moveToNext());
                }
                // Close the cursor after processing
                if(fileCursor != null && !fileCursor.isClosed()) {
                    fileCursor.close();
                }
                if (path != null) {
                    File dir = new File(path);
                    PreferenceUtils.saveLastFiles(dir.list().length);
                }
            } else {
                // Handle the case where the selected item is not a directory
                Log.e("Error", "The selected item is not a directory");
                return null;
            }
        }  // Handle the case where the URI is not stored or is invalid
        // Sort yourListOfItems by timestampDate in descending order (newest first)
        yourListOfItems.sort(Comparator.comparingLong(CallLogItem::getTimestampDate).reversed());
        for(CallLogItem item : yourListOfItems) {
            item.setStarred(PreferenceUtils.isStarred(item.getFileName()));
        }
        if (contactsChanged || needToParse) {
            FileUtils.saveObjectList(getContext(), yourListOfItems, FileUtils.STORED_REG, CallLogItem.class);
            FileUtils.saveObjectList(getContext(), contactList, FileUtils.STORED_CONTACTS, ContactItem.class);
        }
        PreferenceUtils.saveLastFolder();

        // Iterate through the sorted list and add DateHeader objects
        List<Object> sortedListWithHeaders = new ArrayList<>();
        List<Object> starredListWithHeaders = new ArrayList<>();
        Date currentDate = Date.from(Calendar.getInstance().getTime().toInstant());
        String currentHeader = null;


        if (PreferenceUtils.getStarredCount() > 0) starredListWithHeaders.add(new DateHeader(getContext().getString(R.string.starred)));
        double mMaxDuration = 0;
        for (CallLogItem item : yourListOfItems) {

            item.setPlaying(false);
            // Regular item, check if the date has changed
            Date itemDate = new Date(item.getTimestampDate());

            String header;

            if (DateUtils.isToday(itemDate)) {
                // If the date is today, show 'Today'
                header = getContext().getString(R.string.today);
            } else if (DateUtils.isYesterday(currentDate, itemDate)) {
                // If the date is yesterday, show 'Yesterday'
                header = getContext().getString(R.string.yesterday);
            } else if (DateUtils.isLastWeek(currentDate, itemDate)) {
                // If the date is in last week, show the day (e.g., Wednesday)
                header = DateUtils.getDayOfWeek(itemDate);
            } else if (DateUtils.isLastMonth(currentDate, itemDate)) {
                // If the date is in last month, show 'Last Month'
                header = getContext().getString(R.string.last_month);
            } else {
                // For other items, group by month
                header = DateUtils.getMonth(itemDate);
            }

            // Check if the header has changed
            if (!Objects.equals(currentHeader, header)) {
                currentHeader = header;
                sortedListWithHeaders.add(new DateHeader(header));
            }
            if (item.getDuration() > mMaxDuration) mMaxDuration = item.getDuration();

            // Add the regular item
            sortedListWithHeaders.add(item);
            if (item.isStarred()) {
                starredListWithHeaders.add(item);
            }
        }

        if (!onlyStarred && !onlySelectedContact && hasReadContactsPermission) setupContactShortcuts(starredListWithHeaders.size() > 0);

        // Now, sortedListWithHeaders contains both CallLogItem and DateHeader objects
        List<Object> sortedOrderedList = new ArrayList<>(sortedListWithHeaders);
        Log.d("JsonFileLoader.loadInBackground", "contactList size: " + contactList.size());
        PreferenceUtils.savePermissions();

        return new TwoListsWrapper(starredListWithHeaders, sortedOrderedList, contactList, fileError, mMaxDuration);
    }

    private Date forceDate(String fileName) {
        String name = fileName.substring(0, fileName.lastIndexOf("."));
        String cut = name.replaceAll("[^a-zA-Z0-9]", "");
        Log.d("JsonFileLoader.forceDate", "cut: " + cut);
        try {
            return DateUtils.parse(cut);
        } catch (ParseException e) {
            return new Date(0);
        }
    }

    private void removeFiles() {
        if (new File(getContext().getFilesDir(), FileUtils.STORED_REG).exists())
            new File(getContext().getFilesDir(), FileUtils.STORED_REG).delete();
        if (new File(getContext().getFilesDir(), FileUtils.STORED_CONTACTS).exists())
            new File(getContext().getFilesDir(), FileUtils.STORED_CONTACTS).delete();
    }

    private ContactItem getContactItemFromList(String phoneNumber) {
        for (ContactItem contactItem : contactList) {
            if (PhoneNumberUtils.compare(contactItem.getPhoneNumber(), phoneNumber)) {
                return contactItem;
            }
        }
        return null;
    }

    private ContactItem searchContactItem(String phoneNumber, String phoneNumberFormatted,
                                          String contactNameHolder) {
        if (TextUtils.isEmpty(phoneNumber)) return new ContactItem(0, getContext().getString(android.R.string.unknownName), "", "", 0, "",false, null, null, null, LetterTileDrawable.TYPE_GENERIC_AVATAR);
        boolean found = false;
        ContactItem contactItemFound = null;
        if (contactList.size() > 0) {
            for (ContactItem contactItem : contactList) {
                if (PhoneNumberUtils.compare(contactItem.getPhoneNumber(), phoneNumber)) {
                    contactItemFound = contactItem;
                    contactItem.setCount(contactItem.getCount() + 1);
                    found = true;
                    break;
                }
            }
        }
        boolean isContactSaved = false;
        String contactName, lookupKey = null, numberLabel = null;
        Uri lookupUri = null;
        if (!found) {
            String[] contactInfo;
            String contactIconUri;
            long contactId;
            int contactType = LetterTileDrawable.TYPE_GENERIC_AVATAR;
            int numberType = 0;
            if (hasReadContactsPermission) {
                contactInfo = CursorUtils.getContactInfo(getContext(), phoneNumber);
                contactId = Long.parseLong(contactInfo[0]);
                contactName = contactInfo[1];
                contactIconUri = contactInfo[2];
                isContactSaved = Boolean.parseBoolean(contactInfo[3]);
                lookupKey = contactInfo[5];
                lookupUri = CursorUtils.buildLookupUri(contactId, lookupKey);
                numberLabel = contactInfo[6];
                numberType = Integer.parseInt(contactInfo[7]);
            } else {
                contactId = 0;
                contactName = contactNameHolder;
                contactIconUri = null;
            }

            Uri contactUri = null;
            if (contactIconUri!=null) contactUri = Uri.parse(contactIconUri);
            if (TextUtils.isEmpty(contactName)) contactName = phoneNumberFormatted;
            if (isContactSaved) contactType = LetterTileDrawable.TYPE_PERSON;
            boolean isVoiceMail = false;
            PhoneNumberUtils.isVoiceMailNumber(phoneNumber);
            if (hasReadPhoneStatePermission) isVoiceMail = PhoneNumberUtils.isVoiceMailNumber(phoneNumber);
            if (isVoiceMail) {
                if (!isContactSaved || TextUtils.isEmpty(contactName)) {
                    contactName = getContext().getString(android.R.string.defaultVoiceMailAlphaTag);
                }
                contactType = LetterTileDrawable.TYPE_VOICEMAIL;
            }
            if (!isContactSaved && !isVoiceMail && hasReadCallLogPermission) {
                String businessName = CursorUtils.getContactNameFromCallLogs(getContext(), phoneNumber.replaceAll(" ", ""));
                if (businessName!=null && !businessName.isEmpty()) {
                    contactName = businessName;
                    contactType = LetterTileDrawable.TYPE_BUSINESS;
                }
            }
            if (!hasReadCallLogPermission && !hasReadContactsPermission) {
                contactType = LetterTileDrawable.TYPE_GENERIC_AVATAR;
            }
            ContactItem item = new ContactItem(contactId, contactName, phoneNumber, numberLabel, numberType,
                    phoneNumberFormatted, isContactSaved, contactUri, lookupKey, lookupUri, contactType);
            contactList.add(item);
            contactItemFound = item;
        }
        return contactItemFound;
    }

    /**
     * Setup the contact shortcuts
     * @param addStarred If true, will add the starred shortcut, only if we have any starred item
     */
    private void setupContactShortcuts(boolean addStarred) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(getContext());
        int shortCount;
        if (contactList != null && contactList.size() > 0) {
            switch (contactList.size()) {
                case 1 -> shortCount = 1;
                case 2 -> shortCount = 2;
                default -> shortCount = 3;
            }
            List<ContactItem> contactHolder = new ArrayList<>(contactList);
            contactList.sort((item1, item2) -> Integer.compare(item2.getCount(), item1.getCount()));
            List<ContactItem> shortCutter = new ArrayList<>();
            for (ContactItem contact : contactHolder) {
                boolean nameExists = false;

                for (ContactItem uniqueContact : shortCutter) {
                    if (TextUtils.equals(uniqueContact.getContactName(),contact.getContactName())) {
                        nameExists = true;
                        break;
                    }
                }

                if (!nameExists) {
                    shortCutter.add(contact);
                }
            }
            shortCutter.sort((item1, item2) -> Integer.compare(item2.getCount(), item1.getCount()));
            ShortcutUtils shortcutUtils = new ShortcutUtils(getContext());
            if (addStarred) shortcutUtils.addDynamicShortcutStarred();
            switch (shortCount) {
                case 1 -> shortcutUtils.addDynamicShortcut(shortCutter.get(0));
                case 2 -> {
                    shortcutUtils.addDynamicShortcut(shortCutter.get(0));
                    shortcutUtils.addDynamicShortcut(shortCutter.get(1));
                }
                case 3 -> {
                    shortcutUtils.addDynamicShortcut(shortCutter.get(0));
                    shortcutUtils.addDynamicShortcut(shortCutter.get(1));
                    shortcutUtils.addDynamicShortcut(shortCutter.get(2));
                }
            }
        }
    }

    private String getDirection(String fileName) {
        String pat = "[^a-zA-Z0-9](in|out|conference)[^a-zA-Z0-9]";
        Pattern pattern = Pattern.compile(pat);
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group().replaceAll("[^a-zA-Z0-9]", "");
        }
        return null;
    }

    public record TwoListsWrapper(List<Object> starredItemsList,
                                  List<Object> sortedListWithHeaders,
                                  List<ContactItem> contactList,
                                  List<String> errorFiles, double maxDuration) {
    }

}