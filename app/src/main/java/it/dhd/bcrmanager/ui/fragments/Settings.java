package it.dhd.bcrmanager.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import it.dhd.bcrmanager.BuildConfig;
import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class Settings extends  PreferenceFragmentCompat {

    private Preference mDirPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
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
    }
}
