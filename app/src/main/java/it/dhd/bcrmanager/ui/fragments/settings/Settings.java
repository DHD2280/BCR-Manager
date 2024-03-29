package it.dhd.bcrmanager.ui.fragments.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Objects;

import it.dhd.bcrmanager.BuildConfig;
import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.ui.fragments.settingscontainers.BottomPlayerSettings;
import it.dhd.bcrmanager.ui.fragments.settingscontainers.ItemSettings;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.viewmodel.FileViewModel;

public class Settings extends  PreferenceFragmentCompat {

    private Preference mDirPref;
    private FileViewModel fileModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        requireActivity().setTitle(R.string.settings);
        fileModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
        mDirPref = findPreference("bcr_directory");
        Uri treeUri = Uri.parse(PreferenceUtils.getStoredFolderFromPreference());
        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireContext(), treeUri);
        mDirPref.setSummary(FileUtils.getPathFromUri(requireContext(), Objects.requireNonNull(pickedDir).getUri()));
        mDirPref.setOnPreferenceClickListener(pref -> {
            ((MainActivity) requireActivity()).openSomeActivityForResult();
            return true;
        });


        Preference itemPref = findPreference("item_entry_appearance");
        if (itemPref != null) {
            itemPref.setOnPreferenceClickListener(pref -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new ItemSettings(), ItemSettings.class.getSimpleName())
                        .addToBackStack(ItemSettings.class.getSimpleName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                return true;
            });
        }

        Preference playerPref = findPreference("bottom_player");
        if (playerPref != null) {
            playerPref.setOnPreferenceClickListener(pref -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new BottomPlayerSettings(), BottomPlayerSettings.class.getSimpleName())
                        .addToBackStack(BottomPlayerSettings.class.getSimpleName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                return true;
            });
        }

        SwitchPreferenceCompat mDynamicColor = findPreference(PreferenceUtils.Keys.PREFS_KEY_DYNAMIC_COLOR);
        DropDownPreference mThemeColor = findPreference(PreferenceUtils.Keys.PREFS_KEY_THEME_COLOR);
        if (mDynamicColor != null) {
            mDynamicColor.setOnPreferenceChangeListener((preference, newValue) -> {
                if (mThemeColor != null) mThemeColor.setVisible(!((boolean) newValue));
                requireActivity().recreate();

                return true;
            });
        }


        if (mThemeColor != null) {
            mThemeColor.setVisible(!PreferenceUtils.getAppPreferences().getBoolean(PreferenceUtils.Keys.PREFS_KEY_DYNAMIC_COLOR, true));
            mThemeColor.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });
        }

        DropDownPreference mDarkMode =  findPreference(PreferenceUtils.Keys.PREFS_KEY_DARK_MODE);
        if (mDarkMode != null) {
            mDarkMode.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });
        }

        Preference mVersionPref = findPreference("version");
        if (mVersionPref != null) {
            mVersionPref.setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            mVersionPref.setOnPreferenceClickListener(pref -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/DHD2280/BCR-Manager"));
                startActivity(intent);
                return true;
            });
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        Uri treeUri = Uri.parse(PreferenceUtils.getStoredFolderFromPreference());
        DocumentFile pickedDir = DocumentFile.fromTreeUri(requireContext(), treeUri);
        mDirPref.setSummary(FileUtils.getPathFromUri(requireContext(), Objects.requireNonNull(pickedDir).getUri()));
        if (!TextUtils.equals(PreferenceUtils.getLatestFolder(), PreferenceUtils.getStoredFolderFromPreference())) {
            fileModel.fetchData(requireContext(), null);
        }
    }
}
