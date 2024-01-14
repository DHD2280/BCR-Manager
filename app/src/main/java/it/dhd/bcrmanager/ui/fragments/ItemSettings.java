package it.dhd.bcrmanager.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import it.dhd.bcrmanager.drawable.LetterTileDrawable;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;

public class ItemSettings extends Fragment {

    private static ItemSettingsBinding binding;
    private static SharedPreferences prefs;
    private static CallLogItem randomItem;

    public ItemSettings() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = ItemSettingsBinding.inflate(inflater, container, false);
        binding.itemEntry.rootLayout.setStrokeColor(ThemeUtils.getOnBackgroundColor());
        binding.itemEntry.expandingLayout.setVisibility(View.GONE);
        binding.itemEntry.setShowSim(true);
        if (NewHome.yourListOfItems.size() > 0) {
            List<CallLogItem> itemHolder = NewHome.yourListOfItems.stream()
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
        setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true));
        setupDate();
    }

    private void setupDate() {
        //binding.itemEntry.date.setText(randomItem.getFormattedTimestamp());
    }

    private static void setupContactIcon(boolean enabled) {
        binding.itemEntry.contactIcon.setImageDrawable(null);
        if (enabled) {
            binding.itemEntry.contactIcon.setVisibility(View.VISIBLE);
            int dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, MainActivity.getAppContext().getResources().getDisplayMetrics());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                    (dp, dp);
            params.gravity = Gravity.CENTER_VERTICAL;

            binding.itemEntry.contactIcon.setLayoutParams(params);

        } else {
            binding.itemEntry.contactIcon.setVisibility(View.GONE);
            binding.itemEntry.contactIcon.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            return;
        }
        Picasso.get().cancelRequest(binding.itemEntry.contactIcon);
        if (randomItem.getContactIcon()!=null)
            Picasso.get().load(randomItem.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.itemEntry.contactIcon);
        else if (prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true))
            binding.itemEntry.contactIcon.setImageDrawable(randomItem.getContactDrawable(MainActivity.getAppContext()));
        else
            binding.itemEntry.contactIcon.setImageResource(R.drawable.ic_default_contact);
    }

    public static class ItemPreferences extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.item_entry_settings, rootKey);
            SwitchPreferenceCompat contactIcon, contactTile, darkTile;
            contactIcon = findPreference("show_contact_icon");
            contactTile = findPreference("show_colored_tiles");
            contactIcon.setOnPreferenceChangeListener(this);
            contactTile.setOnPreferenceChangeListener(this);
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
                case "show_contact_icon" -> setupContactIcon((boolean) newValue);
                case "show_colored_tiles" -> setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true));
                case "h" -> { }
            }
            return true;
        }
    }

}
