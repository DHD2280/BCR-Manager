package it.dhd.bcrmanager.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ItemSettingsBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;

public class ItemSettings extends Fragment {

    private static ItemSettingsBinding binding;
    private static SharedPreferences prefs;
    private static CallLogItem randomItem;
    private List<CallLogItem> itemHolder;

    public ItemSettings() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh_item) {
            randomItem = itemHolder.get(new Random().nextInt(itemHolder.size()));
            binding.itemEntry.setCallLogItem(randomItem);
            setupItem();
        }
        super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = ItemSettingsBinding.inflate(inflater, container, false);
        binding.itemEntry.rootLayout.setStrokeColor(ThemeUtils.getOnBackgroundColor());
        binding.itemEntry.expandingLayout.setVisibility(View.GONE);
        binding.itemEntry.setShowLabel(true);
        if (NewHome.yourListOfItems.size() > 0) {
            itemHolder = NewHome.yourListOfItems.stream()
                    .filter(CallLogItem.class::isInstance) // Filter only CallLogItem
                    .map(CallLogItem.class::cast) // Converts Object to CallLogItem
                    .collect(Collectors.toList()); // Collects in a new list of CallLogItem
            randomItem = itemHolder.get(new Random().nextInt(itemHolder.size()));
        }

        binding.itemEntry.setCallLogItem(randomItem);
        prefs = PreferenceUtils.getAppPreferences();
        getChildFragmentManager().beginTransaction().add(R.id.settings_container, new ItemPreferences()).commit();
        setupItem();
        return binding.getRoot();
    }

    private void setupItem() {
        setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
        setupDate();
        binding.itemEntry.setShowSim(PreferenceUtils.showSim(SimUtils.getNumberOfSimCards(requireContext())));
        setupNumberLabel(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_NUMBER_LABEL, true));
    }

    private void setupDate() {
        binding.itemEntry.date.setText(randomItem.getFormattedTimestamp(getString(R.string.today), getString(R.string.yesterday)));
    }

    private static void setupContactIcon(boolean enabled, boolean tiles) {
        binding.itemEntry.contactIcon.setImageDrawable(null);
        binding.itemEntry.setShowIcon(enabled);
        Picasso.get().cancelRequest(binding.itemEntry.contactIcon);
        if (randomItem.getContactIcon()!=null)
            Picasso.get().load(randomItem.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.itemEntry.contactIcon);
        else if (tiles)
            binding.itemEntry.contactIcon.setImageDrawable(randomItem.getContactDrawable(MainActivity.getAppContext()));
        else
            binding.itemEntry.contactIcon.setImageResource(R.drawable.ic_default_contact);
    }

    private static void setupSim(String pref) {
        Log.d("ItemSettings", "setupSim: " + pref);
        switch (pref) {
            case "0" -> binding.itemEntry.setShowSim(false);
            case "1" -> binding.itemEntry.setShowSim(NewHome.nSim > 1);
            case "2" -> binding.itemEntry.setShowSim(true);
        }
    }

    private static void setupNumberLabel(boolean showLabel) {
        binding.itemEntry.setShowLabel(showLabel);
    }

    public static class ItemPreferences extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.item_entry_settings, rootKey);
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
                case "show_contact_icon" -> setupContactIcon((boolean) newValue, prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
                case "show_colored_tiles" -> setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), (boolean) newValue);
                case "show_dark_tiles" -> setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
                case "show_number_label" -> setupNumberLabel((boolean) newValue);
                case "show_sim_info" -> setupSim((String) newValue);
            }
            return true;
        }
    }

}
