package it.dhd.bcrmanager.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

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

        super.onChange(selfChange, uri);
        if (!new File(context.getFilesDir(), FileUtils.STORED_CONTACTS).exists()) return;
        List<ContactItem> contacts = FileUtils.loadObjectList(context, FileUtils.STORED_CONTACTS, ContactItem.class);
        StringBuilder changedContacts = new StringBuilder();
        int count = 0;
        if (contacts.size() == 0) return;
        boolean hasChanged = false;
        if (!TextUtils.isEmpty(contactNumber)) {
            Log.d("ContactObserver", "QUERY SINGLE NUMBER");
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
                    new String[]{
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
                // Check Number Type and Label
                if (!TextUtils.isEmpty(cursor.getString(3))) {
                    Cursor phones = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.TYPE,
                                    ContactsContract.CommonDataKinds.Phone.LABEL},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + cursor.getLong(0),
                            null,
                            null);
                    while (phones != null && phones.moveToNext()) {
                        if (PhoneNumberUtils.compare(contactFound.getPhoneNumber(), phones.getString(0))) {
                            // Number Type changed
                            if (contactFound.getNumberType() != phones.getInt(1)) {
                                hasChanged = true;
                                contactFound.setNumberType(phones.getInt(1));
                            }
                            String numberLabel;
                            if (phones.getInt(1) == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                                numberLabel = phones.getString(2);
                            else
                                numberLabel = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), phones.getInt(1), "");
                            if (!TextUtils.equals(contactFound.getNumberLabel(), numberLabel)) {
                                hasChanged = true;
                                contactFound.setNumberLabel(numberLabel);
                            }
                            break;
                        }
                    }
                    if (phones != null && !phones.isClosed()) {
                        phones.close();
                    }
                }
            } else {
                // Probably contact was deleted
                hasChanged = true;
                contactFound.resetContact();
            }
            if (hasChanged) {
                count++;
                changedContacts.append("N:").append(contactFound.getPhoneNumber()).append(";");
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        if (count == 0) {
            if (dataUpdateListener != null) {
                dataUpdateListener.onDataUpdate(false);
            }
            return;
        }
        FileUtils.saveObjectList(context, contacts, FileUtils.STORED_CONTACTS, ContactItem.class);
        if (!TextUtils.isEmpty(contactNumber)) {
            showToast("MAMMT");
        }

        if (dataUpdateListener != null) {
            dataUpdateListener.onDataUpdate(true);
        }

    }

    public interface DataUpdateListener {
        void onDataUpdate(boolean requireUpdate);
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