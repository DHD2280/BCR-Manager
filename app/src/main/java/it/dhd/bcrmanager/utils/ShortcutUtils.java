package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.ui.activity.ContactActivity;

public class ShortcutUtils {


    private final Context mContext;

    /**
     * Constructor
     * @param context The context
     */
    public ShortcutUtils(@NonNull Context context) {
        this.mContext = context;
    }

    /**
     * Add starred dynamic shortcut
     */
    public void addDynamicShortcutStarred() {
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "starred")
                .setShortLabel(mContext.getString(R.string.starred))
                .setLongLabel(mContext.getString(R.string.starred))
                .setIcon(
                        IconCompat.createWithResource(mContext, R.drawable.ic_star))
                .setIntent(new Intent(mContext,
                        ContactActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setAction("it.dhd.bcrmanager.ACTION_VIEW_CONTACT")
                        .putExtra("contact", "starred_contacts"))
                .setRank(0)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(mContext, shortcut);
    }

    /**
     * Add a dynamic shortcut
     * @param item The contact item to load for the shortcut
     * It will set rank based on the number of calls
     */
    public void addDynamicShortcut(ContactItem item) {
        Log.d("JsonFileLoader.loadInBackground", "addDynamicShortcut: " + item.getContactName() + ", " + item.getPhoneNumber() + ", image null: " + (item.getContactImage() != null));
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, item.getLookupKey())
                .setShortLabel(item.getContactName())
                .setLongLabel(item.getContactName())
                .setIcon(
                        new IconFactory(mContext).create(item.getLookupUri(), item.getContactName(), item.getLookupKey()))
                .setIntent(new Intent(mContext,
                        ContactActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setAction("it.dhd.bcrmanager.ACTION_VIEW_CONTACT")
                        .putExtra("contact", item.getContactName())
                        .putExtra("contactId", item.getContactId())
                        .putExtra("lookupKey", item.getLookupKey())
                        .putExtra("contactUri", item.getLookupUri().toString()))
                .setRank(item.getCount())
                .build();
        ShortcutManagerCompat.pushDynamicShortcut(mContext, shortcut);

    }

}
