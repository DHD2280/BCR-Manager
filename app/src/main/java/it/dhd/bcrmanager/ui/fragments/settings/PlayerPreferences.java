package it.dhd.bcrmanager.ui.fragments.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class PlayerPreferences extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private final onPreferenceChangeListener mListener;

    public interface onPreferenceChangeListener {
        void onSimChanged(String newType);
        void onLabelChanged(boolean newType);
    }

    public PlayerPreferences(onPreferenceChangeListener listener) {
        // Required empty public constructor
        this.mListener = listener;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.player_settings, rootKey);

        DropDownPreference mSimPlayer = findPreference(PreferenceUtils.Keys.PREFS_KEY_PLAYER_SHOW_SIM);
        if (mSimPlayer != null) {
            mSimPlayer.setOnPreferenceChangeListener(this);
        }

        SwitchPreferenceCompat mLabelPlayer = findPreference(PreferenceUtils.Keys.PREFS_KEY_PLAYER_SHOW_LABEL);
        if (mLabelPlayer != null) {
            mLabelPlayer.setOnPreferenceChangeListener(this);
        }

    }


    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case PreferenceUtils.Keys.PREFS_KEY_PLAYER_SHOW_SIM -> mListener.onSimChanged((String) newValue);
            case PreferenceUtils.Keys.PREFS_KEY_PLAYER_SHOW_LABEL -> mListener.onLabelChanged((boolean) newValue);
        }
        return true;
    }
}
