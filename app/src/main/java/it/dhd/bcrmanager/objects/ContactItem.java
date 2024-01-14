package it.dhd.bcrmanager.objects;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import it.dhd.bcrmanager.drawable.LetterTileDrawable;

public class ContactItem {

    private long contactId;
    private final String phoneNumber;
    private final String phoneNumberFormatted;
    private String numberLabel;
    private int numberType;
    private String contactName;
    private boolean isContactSaved;
    private Uri contactImage;
    private String lookupKey;
    private Uri lookupUri;
    private int contactType;
    private int count;


    public ContactItem(long contactId, String contactName, String phoneNumber, String numberLabel, int numberType,
                       String phoneNumberFormatted, boolean isContactSaved, Uri contactImage,
                       String lookupKey, Uri lookupUri, int contactType) {
        this.contactId = contactId;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.phoneNumberFormatted = phoneNumberFormatted;
        this.numberLabel = numberLabel;
        this.numberType = numberType;
        this.isContactSaved = isContactSaved;
        this.contactImage = contactImage;
        this.lookupKey = lookupKey;
        this.lookupUri = lookupUri;
        this.contactType = contactType;
        this.count = 1;
    }

    public String getContactName() { return contactName; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isContactSaved() {
        return isContactSaved;
    }

    public Uri getContactImage() {
        return contactImage;
    }

    public String getPhoneNumberFormatted() { return this.phoneNumberFormatted; }

    public String getLookupKey() { return lookupKey; }

    public int getContactType() { return contactType; }

    public int getCount() { return count; }

    public Drawable getContactDrawable(Context context) { return new LetterTileDrawable(context).setCanonicalDialerLetterTileDetails(
            getContactName(), isContactSaved() ? getLookupKey() : getPhoneNumber(), LetterTileDrawable.SHAPE_CIRCLE, getContactType()); }


    public void setCount(int c) { count = c; }

    public Uri getLookupUri() { return lookupUri; }

    public long getContactId() { return contactId; }

    public void setContactName(String newName) {
        this.contactName = newName;
    }

    public void setContactId(long newId) { this.contactId = newId; }

    public void setContactImage(Uri contactImage) { this.contactImage = contactImage; }

    public void setContactType(int contactType) {
        this.contactType = contactType;
    }

    public void setContactSaved(boolean contactSaved) { this.isContactSaved = contactSaved; }

    public void setIsSaved(boolean b) {
        this.isContactSaved = b;
    }

    public void setLookupKey(String key) {
        this.lookupKey = key;
    }

    public void resetContact() {
        this.contactId = 0;
        this.contactName = phoneNumberFormatted;
        this.isContactSaved = false;
        this.contactImage = null;
        this.lookupKey = null;
        this.contactType = LetterTileDrawable.TYPE_GENERIC_AVATAR;
        this.lookupUri = null;
        this.numberLabel = null;
        this.numberType = 0;
    }

    public String getNumberLabel() {
        return numberLabel;
    }

    public int getNumberType() {
        return numberType;
    }
}
