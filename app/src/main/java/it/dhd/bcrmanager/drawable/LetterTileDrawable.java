package it.dhd.bcrmanager.drawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.utils.PreferenceUtils;

/**
 * A drawable that encapsulates all the functionality needed to display a letter tile to represent a
 * contact image.
 */
public class LetterTileDrawable extends Drawable {

    /**
     * ContactType indicates the avatar type of the contact. For a person or for the default when no
     * name is provided, it is {@link #TYPE_DEFAULT}, otherwise, for a business it is {@link
     * #TYPE_BUSINESS}, and voicemail contacts should use {@link #TYPE_VOICEMAIL}.
     */
    public @interface ContactType {}

    /** Contact type constants */
    public static final int TYPE_PERSON = 1;

    public static final int TYPE_BUSINESS = 2;
    public static final int TYPE_VOICEMAIL = 3;
    /**
     * A generic avatar that features the default icon, default color, and no letter. Useful for
     * situations where a contact is anonymous.
     */
    public static final int TYPE_GENERIC_AVATAR = 4;

    public static final int TYPE_SPAM = 5;
    public static final int TYPE_CONFERENCE = 6;
    @ContactType public static final int TYPE_DEFAULT = TYPE_PERSON;

    /**
     * Shape indicates the letter tile shape. It can be either a {@link #SHAPE_CIRCLE}, otherwise, it
     * is a {@link #SHAPE_RECTANGLE}.
     */
    @IntDef({SHAPE_CIRCLE, SHAPE_RECTANGLE})
    public @interface Shape {}

    /** Shape constants */
    public static final int SHAPE_CIRCLE = 1;

    public static final int SHAPE_RECTANGLE = 2;

    /** Default icon scale for vector drawable. */
    private static final float VECTOR_ICON_SCALE = 0.7f;

    /** Reusable components to avoid new allocations */
    private final Paint paint = new Paint();

    private final Rect rect = new Rect();
    private final char[] firstChar = new char[1];

    /** Letter tile */
    @NonNull
    private final TypedArray colors;

    private final int spamColor;
    private final int defaultColor;
    private final int tileFontColor;
    private final float letterToTileRatio;
    @NonNull private final Drawable defaultPersonAvatar;
    @NonNull private final Drawable defaultBusinessAvatar;
    @NonNull private final Drawable defaultVoicemailAvatar;
    @NonNull private final Drawable defaultSpamAvatar;
    @NonNull private final Drawable defaultConferenceAvatar;

    @ContactType private int contactType = TYPE_DEFAULT;
    private float scale = 1.0f;
    private final float offset = 0.0f;
    private boolean isCircle = false;

    private int color;
    private Character letter = null;

    private String displayName;


    @SuppressLint("ResourceType")
    public LetterTileDrawable(Context mContext) {
        colors = mContext.getResources().obtainTypedArray(R.array.letter_tile_colors);
        spamColor = ContextCompat.getColor(mContext, R.color.spam_contact_background);
        defaultColor = ContextCompat.getColor(mContext, R.color.letter_tile_default_color);
        boolean darkLetter = PreferenceUtils.darkLetter();
        boolean nightMode =
                ((mContext.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        if (nightMode && darkLetter) {
            tileFontColor = ContextCompat.getColor(mContext, R.color.letter_tile_font_color_dark);
        } else {
            tileFontColor = ContextCompat.getColor(mContext, R.color.letter_tile_font_color);
        }
        letterToTileRatio = mContext.getResources().getFraction(R.dimen.letter_to_tile_ratio, 1, 1);
        defaultPersonAvatar = Objects.requireNonNull(ContextCompat.getDrawable(mContext, R.drawable.ic_default_contact));
        defaultBusinessAvatar = Objects.requireNonNull(ContextCompat.getDrawable(mContext, R.drawable.ic_business));
        defaultVoicemailAvatar = Objects.requireNonNull(ContextCompat.getDrawable(mContext, R.drawable.ic_voicemail));
        defaultSpamAvatar = Objects.requireNonNull(ContextCompat.getDrawable(mContext, R.drawable.ic_spam));
        defaultConferenceAvatar = Objects.requireNonNull(ContextCompat.getDrawable(mContext, R.drawable.ic_group));
        if (nightMode && darkLetter) {
            defaultPersonAvatar.setColorFilter(tileFontColor, android.graphics.PorterDuff.Mode.SRC_IN);
            defaultBusinessAvatar.setColorFilter(tileFontColor, android.graphics.PorterDuff.Mode.SRC_IN);
            defaultVoicemailAvatar.setColorFilter(tileFontColor, android.graphics.PorterDuff.Mode.SRC_IN);
            defaultSpamAvatar.setColorFilter(tileFontColor, android.graphics.PorterDuff.Mode.SRC_IN);
            defaultConferenceAvatar.setColorFilter(tileFontColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        paint.setTypeface(Typeface.create("@*android:string/config_headlineFontFamilyMedium", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        color = defaultColor;
    }

    private Rect getScaledBounds(float scale, float offset) {
        // The drawable should be drawn in the middle of the canvas without changing its width to
        // height ratio.
        final Rect destRect = copyBounds();
        // Crop the destination bounds into a square, scaled and offset as appropriate
        final int halfLength = (int) (scale * Math.min(destRect.width(), destRect.height()) / 2);

        destRect.set(
                destRect.centerX() - halfLength,
                (int) (destRect.centerY() - halfLength + offset * destRect.height()),
                destRect.centerX() + halfLength,
                (int) (destRect.centerY() + halfLength + offset * destRect.height()));
        return destRect;
    }

    private Drawable getDrawableForContactType(int contactType) {
        switch (contactType) {
            case TYPE_BUSINESS -> {
                scale = VECTOR_ICON_SCALE;
                return defaultBusinessAvatar;
            }
            case TYPE_VOICEMAIL -> {
                scale = VECTOR_ICON_SCALE;
                return defaultVoicemailAvatar;
            }
            case TYPE_SPAM -> {
                scale = VECTOR_ICON_SCALE;
                return defaultSpamAvatar;
            }
            case TYPE_CONFERENCE -> {
                scale = VECTOR_ICON_SCALE;
                return defaultConferenceAvatar;
            }
            default -> {
                scale = VECTOR_ICON_SCALE;
                return defaultPersonAvatar;
            }
        }
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        final Rect bounds = getBounds();
        if (!isVisible() || bounds.isEmpty()) {
            return;
        }
        // Draw letter tile.
        drawLetterTile(canvas);
    }

    public Bitmap getBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.setBounds(0, 0, width, height);
        Canvas canvas = new Canvas(bitmap);
        this.draw(canvas);
        return bitmap;
    }

    private void drawLetterTile(final Canvas canvas) {
        // Draw background color.
        paint.setColor(color);

        final Rect bounds = getBounds();
        final int minDimension = Math.min(bounds.width(), bounds.height());

        if (isCircle) {
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), ((float)minDimension / 2), paint);
        } else {
            canvas.drawRect(bounds, paint);
        }

        // Draw letter/digit only if the first character is an english letter or there's a override
        if (letter != null) {
            // Draw letter or digit.
            firstChar[0] = letter;

            // Scale text by canvas bounds and user selected scaling factor
            paint.setTextSize(scale * letterToTileRatio * minDimension);
            paint.getTextBounds(firstChar, 0, 1, rect);
            paint.setTypeface(Typeface.create("@*android:string/config_bodyFontFamily", Typeface.NORMAL));
            paint.setColor(tileFontColor);

            // Draw the letter in the canvas, vertically shifted up or down by the user-defined
            // offset
            canvas.drawText(
                    firstChar,
                    0,
                    1,
                    bounds.centerX(),
                    bounds.centerY() + offset * bounds.height() - rect.exactCenterY(),
                    paint);
        } else {
            // Draw the default image if there is no letter/digit to be drawn
            Drawable drawable = getDrawableForContactType(contactType);
            if (drawable == null) {
                return;
            }

            drawable.setBounds(getScaledBounds(scale, offset));
            drawable.draw(canvas);
        }
    }

    public int getColor() {
        return color;
    }

    public LetterTileDrawable setColor(int color) {
        this.color = color;
        return this;
    }

    /** Returns a deterministic color based on the provided contact identifier string. */
    private int pickColor(final String identifier) {
        if (contactType == TYPE_SPAM) {
            return spamColor;
        }

        if (identifier == null || identifier.isEmpty()) {
            return defaultColor;
        }

        // String.hashCode() implementation is not supposed to change across java versions, so
        // this should guarantee the same email address always maps to the same color.
        // The email should already have been normalized by the ContactRequest.
        final int color = Math.abs(identifier.hashCode()) % colors.length();
        return colors.getColor(color, defaultColor);
    }

    @Override
    public void setAlpha(final int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.OPAQUE;
    }

    /**
     * Scale the drawn letter tile to a ratio of its default size
     *
     * @param scale The ratio the letter tile should be scaled to as a percentage of its default size,
     *     from a scale of 0 to 2.0f. The default is 1.0f.
     */
    public LetterTileDrawable setScale(float scale) {
        this.scale = scale;
        return this;
    }


    public LetterTileDrawable setLetter(Character letter) {
        this.letter = letter;
        return this;
    }

    public Character getLetter() {
        return this.letter;
    }

    private LetterTileDrawable setLetterAndColorFromContactDetails(
            final String displayName, final String identifier) {
        if (displayName != null && !displayName.isEmpty()
                && Character.isLetter(displayName.charAt(0))) {
            letter = Character.toUpperCase(displayName.charAt(0));
        } else {
            letter = null;
        }
        color = pickColor(identifier);
        return this;
    }

    private LetterTileDrawable setContactType(@ContactType int contactType) {
        this.contactType = contactType;
        return this;
    }

    @ContactType
    public int getContactType() {
        return this.contactType;
    }

    public LetterTileDrawable setIsCircular(boolean isCircle) {
        this.isCircle = isCircle;
        return this;
    }

    public boolean tileIsCircular() {
        return this.isCircle;
    }

    /**
     * Creates a canonical letter tile for use across dialer fragments.
     *
     * @param displayName The display name to produce the letter in the tile. Null values or numbers
     *     yield no letter.
     * @param identifierForTileColor The string used to produce the tile color.
     * @param shape The shape of the tile.
     * @param contactType The type of contact, e.g. TYPE_VOICEMAIL.
     * @return this
     */
    public LetterTileDrawable setCanonicalDialerLetterTileDetails(
            @Nullable final String displayName,
            @Nullable final String identifierForTileColor,
            @Shape final int shape,
            final int contactType) {

        this.setIsCircular(shape == SHAPE_CIRCLE);

        if (contactType == TYPE_DEFAULT
                && ((displayName == null && identifierForTileColor == null)
                || (displayName != null && displayName.equals(this.displayName)))) {
            return this;
        }

        this.displayName = displayName;
        setContactType(contactType);

        // Special contact types receive default color and no letter tile, but special iconography.
        if (contactType != TYPE_PERSON) {
            if (contactType == TYPE_BUSINESS) this.setLetterAndColorFromContactDetails(null, displayName);
            else this.setLetterAndColorFromContactDetails(null, null);
        } else {
            this.setLetterAndColorFromContactDetails(displayName, Objects.requireNonNullElse(identifierForTileColor, displayName));
        }
        return this;
    }
}
