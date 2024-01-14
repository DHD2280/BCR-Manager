package it.dhd.bcrmanager.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.drawable.LetterTileDrawable;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.UriUtils;

public class ContactObserver extends ContentObserver {

    private String contactNumber = "";
    private static DataUpdateListener dataUpdateListener;
    public static void registerDataUpdateListener(DataUpdateListener listener) {
        dataUpdateListener = listener;
    }

    public static void unregisterDataUpdateListener(DataUpdateListener listener) {
        dataUpdateListener = null;
    }


    public ContactObserver(Handler handler) {
        super(handler);
    }

    private Context context;

    public ContactObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {


        // Check
        if (!new File(context.getFilesDir(), FileUtils.STORED_CONTACTS).exists()) return;
        List<ContactItem> contacts = FileUtils.loadObjectList(context, FileUtils.STORED_CONTACTS, ContactItem.class);
        StringBuilder changedContacts = new StringBuilder();
        int count = 0;
        if (contacts.size() == 0) return;
        boolean hasChanged = false;
        if (!TextUtils.isEmpty(contactNumber)) {
            // Query for a single number
            ContactItem contactFound = null;
            for (ContactItem contact : contacts) {
                if (TextUtils.isEmpty(contact.getPhoneNumber())) continue;
                if (PhoneNumberUtils.compare(contactNumber, contact.getPhoneNumber())) {
                    contactFound = contact;
                    break;
                }
            }
            if (contactFound == null) return;
            Cursor cursor = context.getContentResolver().query(
                    Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactNumber)),
                    new String[] {
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.PHOTO_URI,
                            ContactsContract.Contacts.LOOKUP_KEY
                    },
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                    if (!TextUtils.isEmpty(cursor.getString(1))) {
                        // Name changed
                        if (!TextUtils.equals(contactFound.getContactName(), cursor.getString(1))) {
                            hasChanged = true;
                            contactFound.setContactName(cursor.getString(1));
                            if (!contactFound.isContactSaved()) {
                                contactFound.setContactSaved(true);
                                contactFound.setContactType(LetterTileDrawable.TYPE_PERSON);
                            }
                        }
                    }
                    // Check contact Image
                    if (!UriUtils.areEqual(contactFound.getContactImage(), UriUtils.parseUriOrNull(cursor.getString(2)))) {
                        hasChanged = true;
                        contactFound.setContactImage(UriUtils.parseUriOrNull(cursor.getString(2)));
                    }
                    // Check contact LookupKey
                    if (!TextUtils.equals(contactFound.getLookupKey(), cursor.getString(3))) {
                        hasChanged = true;
                        contactFound.setLookupKey(cursor.getString(3));
                    }
                    // Check contact ID
                    if (contactFound.getContactId() == cursor.getLong(0)) {
                        hasChanged = true;
                        contactFound.setContactId(cursor.getLong(0));
                    }
            } else {
                // Probably contact was deleted
                hasChanged = true;
                contactFound.resetContact();
            }
            if (hasChanged) {
                count ++;
                changedContacts.append("N:").append(contactFound.getPhoneNumber()).append(";");
            }
            if(cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        } else {
            // Query for all contacts
            for (ContactItem contact : contacts) {
                Cursor cursor = context.getContentResolver().query(
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
                    do {
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
                        if (hasChanged) {
                            count ++;
                            changedContacts.append("N:").append(contact.getPhoneNumber()).append(";");
                        }
                    } while (cursor.moveToNext());
                }
                if(cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (count == 0) {
            if (dataUpdateListener != null) {
                dataUpdateListener.onDataUpdate("", null);
            }
            return;
        }
        FileUtils.saveObjectList(context, contacts, FileUtils.STORED_CONTACTS, ContactItem.class);
        String qty = "0";
        if (count == 1 && !contactNumber.isEmpty())
            count = Integer.parseInt(contactNumber);
        String formattedString = context.getResources().getQuantityString(R.plurals.deleted_registrations, count, count);
        showToast(formattedString);

        if (dataUpdateListener != null) {
            dataUpdateListener.onDataUpdate(changedContacts.toString(), contacts);
        }

    }

    public interface DataUpdateListener {
        void onDataUpdate(String newData, List<ContactItem> newContacts);
    }

    private void showToast(String message) {
        // Verifica che il contesto non sia nullo e mostra il Toast
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}