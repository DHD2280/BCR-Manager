package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

public class ThemeUtils {

    private static Context mContext;


    private static @ColorInt int colorPrimary, colorOnPrimary, colorBackground, colorOnBackground;

    /**
     * Initialize the class
     * @param context The application context to get the theme
     */
    public static void init(Context context) {
        mContext = context;
    }

    /**
     * Get the primary color from the theme
     * @return @ColorInt The primary color
     */
    public static @ColorInt int getPrimaryColor() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getOnPrimaryColor() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getBackgroundColor() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.backgroundColor, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getOnBackgroundColor() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
        return typedValue.data;
    }

}
