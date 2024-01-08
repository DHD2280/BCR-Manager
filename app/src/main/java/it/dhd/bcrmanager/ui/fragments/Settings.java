package it.dhd.bcrmanager.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Objects;

import it.dhd.bcrmanager.BuildConfig;
import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class Settings extends  PreferenceFragmentCompat {

    private Preference mDirPref;
    private Toast mDevHitToast;
    private final int TAPS_TO_BE_A_DEVELOPER = 7;
    private Preference mVersionPref;
    private int mDevHitCountdown = 7;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setPreferencesFromResource(R.xml.settings, rootKey);
        mDirPref = findPreference("bcr_directory");
        Uri treeUri = Uri.parse(PreferenceUtils.getStoredFolderFromPreference());
        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireContext(), treeUri);
        mDirPref.setSummary(FileUtils.getPathFromUri(requireContext(), Objects.requireNonNull(pickedDir).getUri()));
        mDirPref.setOnPreferenceClickListener(pref -> {
            ((MainActivity) requireActivity()).openSomeActivityForResult();
            return true;
        });
        SwitchPreferenceCompat switchTiles = findPreference("show_colored_tiles");
        SwitchPreferenceCompat darkLetters = findPreference("dark_letter_on_dark_mode");

        Objects.requireNonNull(switchTiles).setOnPreferenceChangeListener((preference1, newValue) -> {
            if (darkLetters != null) darkLetters.setEnabled((boolean) newValue);
            NewHome f = (NewHome) getParentFragmentManager().findFragmentByTag(NewHome.class.getSimpleName());
            if (f != null) {
                f.notifyAdapter();
            }
            return true;
        });
        Objects.requireNonNull(darkLetters).setOnPreferenceChangeListener((preference1, newValue) -> {
            NewHome f = (NewHome) getParentFragmentManager().findFragmentByTag(NewHome.class.getSimpleName());
            if (f != null) {
                f.notifyAdapter();
            }
            return true;
        });

        mVersionPref = findPreference("version");
        mVersionPref.setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        mVersionPref.setOnPreferenceClickListener(pref -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DHD2280/BCR-Manager"));
            startActivity(intent);
            return true;
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Uri treeUri = Uri.parse(PreferenceUtils.getStoredFolderFromPreference());
        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireContext(), treeUri);
        mDirPref.setSummary(FileUtils.getPathFromUri(requireContext(), Objects.requireNonNull(pickedDir).getUri()));
    }
}
