package it.dhd.bcrmanager.ui.fragments.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class ItemPreferences extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private SharedPreferences prefs;

    public interface onSettingsChangedListener {
        void onIconSettingsChanged(boolean contactIcon, boolean tiles);
        void onNumberLabelSettingsChanged(boolean numberLabel);
        void onSimSettingsChanged(String simInfo);
    }

    private final onSettingsChangedListener mListener;


    public ItemPreferences(onSettingsChangedListener listener) {
        // Required empty public constructor
        mListener = listener;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.item_entry_settings, rootKey);
        prefs = PreferenceUtils.getAppPreferences();
        SwitchPreferenceCompat contactIcon, contactTile, darkTile, numberLabel;
        DropDownPreference simInfo;
        contactIcon = findPreference("show_contact_icon");
        contactTile = findPreference("show_colored_tiles");
        darkTile = findPreference("show_dark_tiles");
        simInfo = findPreference("show_sim_info");
        numberLabel = findPreference("show_number_label");

        if (contactIcon != null)
            contactIcon.setOnPreferenceChangeListener(this);
        if (contactTile != null)
            contactTile.setOnPreferenceChangeListener(this);
        if (darkTile != null)
            darkTile.setOnPreferenceChangeListener(this);
        if (simInfo != null)
            simInfo.setOnPreferenceChangeListener(this);
        if (numberLabel != null)
            numberLabel.setOnPreferenceChangeListener(this);
    }

    /**
     * Called when a preference has been changed by the user. This is called before the state
     * of the preference is about to be updated and before the state is persisted.
     *
     * @param preference The changed preference
     * @param newValue   The new value of the preference
     * @return {@code true} to update the state of the preference with the new value
     */
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case "show_contact_icon" -> mListener.onIconSettingsChanged((boolean) newValue, prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
            case "show_colored_tiles" -> mListener.onIconSettingsChanged(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), (boolean) newValue);
            case "show_dark_tiles" -> mListener.onIconSettingsChanged(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
            case "show_number_label" -> mListener.onNumberLabelSettingsChanged((boolean) newValue);
            case "show_sim_info" -> mListener.onSimSettingsChanged((String) newValue);
        }
        return true;
    }
}
