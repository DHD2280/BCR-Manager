package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.IconCompat;

import java.io.InputStream;
import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.drawable.LetterTileDrawable;

public class IconFactory {

    private final Context mContext;

    public IconFactory(@NonNull Context context) {
        this.mContext = context;
    }

    /**
     * Create the icon for the shortcut
     * @param lookupUri The lookup uri of the contact
     * @param displayName The name of the contact
     * @param lookupKey The lookup key of the contact
     * @return The icon for the shortcut
     */
    @NonNull
    public IconCompat create(
            Uri lookupUri, @NonNull String displayName, @NonNull String lookupKey) {

        // In testing, there was no difference between high-res and thumbnail.
        InputStream inputStream =
                ContactsContract.Contacts.openContactPhotoInputStream(
                        mContext.getContentResolver(), lookupUri, false /* preferHighres */);

        return createAdaptiveIcon(displayName, lookupKey, inputStream);
    }

    /**
     * Create the adaptive icon for the shortcut
     * @param displayName The name of the contact
     * @param lookupKey The lookup key of the contact
     * @param inputStream The input stream of the contact icon, passed if we have a contact icon
     * @return The adaptive icon for the shortcut
     */
    private IconCompat createAdaptiveIcon(
            @NonNull String displayName, @NonNull String lookupKey, InputStream inputStream) {
        if (inputStream == null) {
            LetterTileDrawable letterTileDrawable = new LetterTileDrawable(mContext);
            // The adaptive icons clip the drawable to a safe area inside the drawable. Scale the letter
            // so it fits inside the safe area.
            letterTileDrawable.setScale(1f / (1f + AdaptiveIconDrawable.getExtraInsetFraction()));
            letterTileDrawable.setCanonicalDialerLetterTileDetails(
                    displayName,
                    lookupKey,
                    LetterTileDrawable.SHAPE_RECTANGLE,
                    LetterTileDrawable.TYPE_DEFAULT);

            int iconSize =
                    mContext
                            .getResources()
                            .getDimensionPixelSize(R.dimen.launcher_shortcut_adaptive_icon_size);
            return IconCompat.createWithAdaptiveBitmap(
                    Objects.requireNonNull(DrawableConverter.drawableToBitmap(letterTileDrawable, iconSize, iconSize)));
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return IconCompat.createWithAdaptiveBitmap(bitmap);
    }


}
