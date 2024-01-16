package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

import java.util.HashMap;
import java.util.Map;

import it.dhd.bcrmanager.R;

public class ThemeUtils {

    private static final Map<String, Integer> colorThemeMap = new HashMap<>(){{
        put("SAKURA", R.style.ThemeOverlay_MaterialSakura);
        put("MATERIAL_RED", R.style.ThemeOverlay_MaterialRed);
        put("MATERIAL_PINK", R.style.ThemeOverlay_MaterialPink);
        put("MATERIAL_PURPLE", R.style.ThemeOverlay_MaterialPurple);
        put("MATERIAL_DEEP_PURPLE", R.style.ThemeOverlay_MaterialDeepPurple);
        put("MATERIAL_INDIGO", R.style.ThemeOverlay_MaterialIndigo);
        put("MATERIAL_BLUE", R.style.ThemeOverlay_MaterialBlue);
        put("MATERIAL_LIGHT_BLUE", R.style.ThemeOverlay_MaterialLightBlue);
        put("MATERIAL_CYAN", R.style.ThemeOverlay_MaterialCyan);
        put("MATERIAL_TEAL", R.style.ThemeOverlay_MaterialTeal);
        put("MATERIAL_GREEN", R.style.ThemeOverlay_MaterialGreen);
        put("MATERIAL_LIGHT_GREEN", R.style.ThemeOverlay_MaterialLightGreen);
        put("MATERIAL_LIME", R.style.ThemeOverlay_MaterialLime);
        put("MATERIAL_YELLOW", R.style.ThemeOverlay_MaterialYellow);
        put("MATERIAL_AMBER", R.style.ThemeOverlay_MaterialAmber);
        put("MATERIAL_ORANGE", R.style.ThemeOverlay_MaterialOrange);
        put("MATERIAL_DEEP_ORANGE", R.style.ThemeOverlay_MaterialDeepOrange);
        put("MATERIAL_BROWN", R.style.ThemeOverlay_MaterialBrown);
        put("MATERIAL_BLUE_GREY", R.style.ThemeOverlay_MaterialBlueGrey);
    }};

    public static final Map<String, Integer> iconsColor = new HashMap<>(){{
        put("red", R.color.md_theme_material_red_light_primary);
        put("purple", R.color.md_theme_material_purple_light_primary);
        put("blue", R.color.md_theme_material_blue_light_primary);
        put("teal", R.color.md_theme_material_teal_light_primary);
        put("green", R.color.md_theme_material_green_light_primary);
        put("lime", R.color.md_theme_material_lime_light_primary);
        put("yellow", R.color.md_theme_material_yellow_light_primary);
        put("orange", R.color.md_theme_material_orange_light_primary);
        put("brown", R.color.md_theme_material_brown_light_primary);
    }};

    /**
     * Get the primary color from the theme
     * @return @ColorInt The primary color
     */
    public static @ColorInt int getPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getOnPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.backgroundColor, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getOnBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
        return typedValue.data;
    }

    public static @ColorInt int getColorSurfaceHighest(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        return typedValue.data;
    }

    public static boolean isSystemAccent() {
        return DynamicColors.isDynamicColorAvailable() && PreferenceUtils.getAppPreferences().getBoolean(PreferenceUtils.Keys.PREFS_KEY_DYNAMIC_COLOR, true);
    }

    public static String getColorTheme() {
        if (isSystemAccent()) {
            return "system";
        }
        return PreferenceUtils.getAppPreferences().getString("theme_color", "MATERIAL_TEAL");
    }

    @StyleRes
    public static int getColorThemeStyleRes() {
        Integer theme = colorThemeMap.get(getColorTheme());
        if (theme == null) {
            return R.style.ThemeOverlay_MaterialBlue;
        }
        return theme;
    }

    public static int getDarkTheme(String mode) {
        return switch (mode) {
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case "on" -> AppCompatDelegate.MODE_NIGHT_YES;
            case "off" -> AppCompatDelegate.MODE_NIGHT_NO;
        };
    }

    public static int getDarkTheme() {
        return getDarkTheme(PreferenceUtils.getAppPreferences().getString(PreferenceUtils.Keys.PREFS_KEY_DARK_MODE, "system"));
    }

    public static @ColorInt int getDirectionColor(Context context, String direction) {
        Integer color = iconsColor.get(direction);
        if (color == null) {
            return getOnBackgroundColor(context);
        }
        return color;
    }

}
