package it.dhd.bcrmanager.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import it.dhd.bcrmanager.drawable.LetterTileDrawable;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.utils.FileUtils;

public class ContactObserver extends ContentObserver {

    private String lookupKey = "", contactNumber = "";
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

        showToast("Contatto modificato");
        // Check
        if (!new File(context.getFilesDir(), FileUtils.STORED_CONTACTS).exists()) return;
        List<ContactItem> contacts = FileUtils.loadObjectList(context, FileUtils.STORED_CONTACTS, ContactItem.class);
        StringBuilder changedContacts = new StringBuilder();
        int count = 0;
        if (contacts.size() == 0) return;
        if (lookupKey.isEmpty() && contactNumber.isEmpty()) {
            for (ContactItem contact : contacts) {
                boolean hasChanged = false;
                if (!contact.isContactSaved()) {
                    Uri phoneLookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact.getPhoneNumber()));
                    Cursor cursor = context.getContentResolver().query(
                            phoneLookup,
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
                            if (!cursor.getString(1).isEmpty()) {
                                hasChanged = true;
                                contact.setContactName(cursor.getString(1));
                                contact.setIsSaved(true);
                                contact.setContactType(LetterTileDrawable.TYPE_PERSON);
                            } else {
                                contact.setContactType(LetterTileDrawable.TYPE_GENERIC_AVATAR);
                                contact.setIsSaved(false);
                            }
                            if (cursor.getString(2) != null)
                                contact.setContactImage(Uri.parse(cursor.getString(2)));
                            if (cursor.getString(3) != null)
                                contact.setLookupKey(cursor.getString(3));
                            if (cursor.getLong(0) != 0)
                                contact.setContactId(cursor.getLong(0));
                        } while (cursor.moveToNext());
                    }
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                } else {
                    Cursor cursor = context.getContentResolver().query(
                            ContactsContract.Contacts.CONTENT_URI,
                            new String[] {
                                    ContactsContract.Contacts._ID,
                                    ContactsContract.Contacts.DISPLAY_NAME,
                                    ContactsContract.Contacts.PHOTO_URI,
                                    ContactsContract.Contacts.LOOKUP_KEY
                            },
                            ContactsContract.Contacts.LOOKUP_KEY + "=?",
                            new String[]{contact.getLookupKey()},
                            null);
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            // Retrieve file information from the cursor
                            Log.d("ContactObserver", "onChange: " + cursor.getString(1) + " - " + contact.getContactName());
                            if (!cursor.getString(1).equals(contact.getContactName())) {
                                hasChanged = true;
                                contact.setContactName(cursor.getString(1));
                            }
                            if (contact.getContactImage() == null && cursor.getString(2) != null) {
                                hasChanged = true;
                                contact.setContactImage(Uri.parse(cursor.getString(2)));
                            }
                        } while (cursor.moveToNext());
                    } else {
                        contact.setIsSaved(false);
                        contact.setContactType(LetterTileDrawable.TYPE_GENERIC_AVATAR);
                    }
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
                if (hasChanged) {
                    count ++;
                    changedContacts.append("N:").append(contact.getPhoneNumber()).append(";");
                }
            }
        } else {
            boolean hasChanged = false;
            if (!lookupKey.isEmpty() && contactNumber.isEmpty()) {
                ContactItem contact = null;
                for (ContactItem c : contacts) {
                    if (c.getLookupKey() != null && c.getLookupKey().equals(lookupKey)) {
                        contact = c;
                        break;
                    }
                }
                Cursor cursor = context.getContentResolver().query(
                        Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "/" + lookupKey),
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
                        String cname = cursor.getString(1);
                        String img = cursor.getString(2);
                        if (contact == null) return;
                        if (!cname.equals(contact.getContactName())) {
                            if (cname.isEmpty()) {
                                contact.setContactType(LetterTileDrawable.TYPE_GENERIC_AVATAR);
                            } else {
                                contact.setContactType(LetterTileDrawable.TYPE_PERSON);
                            }
                            contact.setIsSaved(false);
                            contact.setContactName(cname);
                            hasChanged = true;
                        }
                        if (contact.getContactImage() == null && img != null) {
                            contact.setContactImage(Uri.parse(img));
                            hasChanged = true;
                        }
                    } while (cursor.moveToNext());
                    if(!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else if (!contactNumber.isEmpty() && lookupKey.isEmpty()) {
                ContactItem contact = null;
                for (ContactItem c : contacts) {
                    if (PhoneNumberUtils.compare(contactNumber, c.getPhoneNumber())) {
                        contact = c;
                        break;
                    }
                }
                Cursor cursor = context.getContentResolver().query(
                        Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "/" + lookupKey),
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
                        String cname = cursor.getString(1);
                        String img = cursor.getString(2);
                        if (contact == null) return;
                        if (!cname.equals(contact.getContactName())) {
                            if (cname.isEmpty()) {
                                contact.setContactType(LetterTileDrawable.TYPE_GENERIC_AVATAR);
                                contact.setIsSaved(false);
                            } else {
                                contact.setContactType(LetterTileDrawable.TYPE_PERSON);
                                contact.setContactName(cname);
                                contact.setIsSaved(true);
                            }
                            hasChanged = true;
                        }
                        if (contact.getContactImage() == null && img != null) {
                            contact.setContactImage(Uri.parse(img));
                            hasChanged = true;
                        }
                    } while (cursor.moveToNext());
                    if(!cursor.isClosed()) {
                        cursor.close();
                    }
                } else {
                    return;
                }
                if (hasChanged) {
                    count++;
                }
            }
        }
        if (count == 0) {
            if (dataUpdateListener != null) {
                dataUpdateListener.onDataUpdate("");
            }
            return;
        }
        FileUtils.saveObjectList(context, contacts, FileUtils.STORED_CONTACTS, ContactItem.class);
        if (count == 1 && !(lookupKey.isEmpty() || contactNumber.isEmpty()))
            showToast("Contatto aggiornato: " + (lookupKey.isEmpty() ? contactNumber : lookupKey));
        else
            showToast("Contatti aggiornati: " + count);
        if (dataUpdateListener != null) {
            dataUpdateListener.onDataUpdate(changedContacts.toString());
        }

    }

    public interface DataUpdateListener {
        void onDataUpdate(String newData);
    }

    private void showToast(String message) {
        // Verifica che il contesto non sia nullo e mostra il Toast
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}