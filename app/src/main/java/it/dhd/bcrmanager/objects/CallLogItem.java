package it.dhd.bcrmanager.objects;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import it.dhd.bcrmanager.drawable.LetterTileDrawable;

public class CallLogItem {

    public String contactName;
    private final String number;
    private final String numberFormatted;
    private String numberLabel;
    private int numberType;
    private Uri contactIcon;
    private String lookupKey;
    private boolean contactSaved;
    private int contactType;
    private final String direction;
    private final double duration;
    private final Integer simSlot;
    private final String audioFilePath;
    private final String filePath;
    public Boolean isPlaying;
    public Boolean starred;
    private final long timestampDate;
    private final String fileName;

    public CallLogItem(Uri contactIcon, String lookupKey, boolean contactSaved,
                       int contactType, String contactName, String number,
                       String numberFormatted, String numberLabel, int numberType, String direction, long timestampDate,
                       double duration, Integer simSlot,
                       String audioFilePath, String filePath, String fileName, boolean starred) {
        this.contactIcon = contactIcon;
        this.lookupKey = lookupKey;
        this.contactName = contactName;
        this.contactSaved = contactSaved;
        this.contactType = contactType;
        this.number = number;
        this.numberFormatted = numberFormatted;
        this.numberLabel = numberLabel;
        this.numberType = numberType;
        this.direction = direction;
        this.timestampDate = timestampDate;
        this.duration = duration;
        this.simSlot = simSlot;
        this.audioFilePath = audioFilePath;
        this.fileName = fileName;
        this.filePath = filePath;
        isPlaying = false;
        this.starred = starred;
    }

    public String getContactName() {
        return contactName;
    }

    public String getNumber() {
        return number;
    }

    public String getNumberLabel() {
        return numberLabel;
    }

    public int getNumberType() {
        return numberType;
    }

    public Uri getContactIcon() {
        return contactIcon;
    }

    public String getDirection() {
        return direction;
    }

    public long getTimestampDate() {
        return timestampDate;
    }

    public String getTimeStamp(Context c) {
        SimpleDateFormat sdfToday, sdfOtherDays;
        if (DateFormat.is24HourFormat(c)) {
            sdfToday = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sdfOtherDays = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        } else {
            sdfToday = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
            sdfOtherDays = new SimpleDateFormat("dd MMM hh:mm aa", Locale.getDefault());
        }

        Calendar now = Calendar.getInstance();
        Calendar itemTime = Calendar.getInstance();
        itemTime.setTime(new Date(timestampDate));

        if ((now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)) ||
                (now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) - itemTime.get(Calendar.DAY_OF_YEAR) == 1)) {
            // Today
            return sdfToday.format(timestampDate);
        } else {
            // Older than yesterday
            return sdfOtherDays.format(timestampDate);
        }
    }

    public String getFormattedTimestampComplete(String today, String yesterday) {
        SimpleDateFormat sdfToday = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
        SimpleDateFormat sdfOtherDays = new SimpleDateFormat("EEEE dd MMM HH:mm", Locale.getDefault());

        Calendar now = Calendar.getInstance();
        Calendar itemTime = Calendar.getInstance();
        itemTime.setTime(new Date(timestampDate));

        if (now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)) {
            // Today
            return today + " " + sdfToday.format(timestampDate);
        } else if (now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - itemTime.get(Calendar.DAY_OF_YEAR) == 1) {
            // Yesterday
            return yesterday + " " + sdfToday.format(timestampDate);
        } else {
            // Older than yesterday
            return sdfOtherDays.format(timestampDate);
        }
    }

    public String getFormattedTimestamp(String today, String yesterday) {
        SimpleDateFormat sdfToday = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sdfOtherDays = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());

        Calendar now = Calendar.getInstance();
        Calendar itemTime = Calendar.getInstance();
        itemTime.setTime(new Date(timestampDate));

        if (now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)) {
            // Today
            return today + " " + sdfToday.format(timestampDate);
        } else if (now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - itemTime.get(Calendar.DAY_OF_YEAR) == 1) {
            // Yesterday
            return yesterday + " " + sdfToday.format(timestampDate);
        } else {
            // Older than yesterday
            return sdfOtherDays.format(timestampDate);
        }
    }

    public String getFormattedDurationPlayer() {
        long durationSeconds = (long) Math.ceil(duration);

        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public String getStandardDuration() {
        long durationSeconds = (long) Math.ceil(duration);

        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public double getDuration() {
        return duration;
    }

    public Integer getSimSlot() {
        return simSlot;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public String getFileName() {
        return fileName;
    }


    public boolean isContactSaved() {
        return contactSaved;
    }

    public void setPlaying(boolean b) {
        isPlaying = b;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String getNumberFormatted() {
        return numberFormatted;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public int getContactType() {
        return contactType;
    }

    public Drawable getContactDrawable(Context context) {
        return new LetterTileDrawable(context).setCanonicalDialerLetterTileDetails(
                getContactName(), isContactSaved() ? getLookupKey() : getNumber(), LetterTileDrawable.SHAPE_CIRCLE, getContactType());
    }

    public String getFilePath() {
        return filePath;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setContactIcon(Uri contactImage) {
        this.contactIcon = contactImage;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    public void setContactSaved(boolean contactSaved) {
        this.contactSaved = contactSaved;
    }

    public void setContactType(int contactType) {
        this.contactType = contactType;
    }

    public void setNumberLabel(String numberLabel) {
        this.numberLabel = numberLabel;
    }

    public void setNumberType(int numberType) {
        this.numberType = numberType;
    }


}
