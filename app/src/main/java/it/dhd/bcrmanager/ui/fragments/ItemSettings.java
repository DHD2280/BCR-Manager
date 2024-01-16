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
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ItemSettingsBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.ui.fragments.settings.ItemPreferences;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
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
        binding.itemEntry.rootLayout.setStrokeColor(ThemeUtils.getOnBackgroundColor(requireContext()));
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
        getChildFragmentManager().beginTransaction().add(R.id.settings_container,
                new ItemPreferences(new ItemPreferences.onSettingsChangedListener() {
                    @Override
                    public void onIconSettingsChanged(boolean contactIcon, boolean tiles) {
                        setupContactIcon(contactIcon, tiles);
                    }

                    @Override
                    public void onNumberLabelSettingsChanged(boolean numberLabel) {
                        setupNumberLabel(numberLabel);
                    }

                    @Override
                    public void onSimSettingsChanged(String simInfo) {
                        setupSim(simInfo);
                    }
                })).commit();
        setupItem();
        return binding.getRoot();
    }

    private void setupItem() {
        setupContactIcon(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_CONTACT_ICON, true), prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_TILES, true));
        setupDate();
        setupSim(prefs.getString(PreferenceUtils.Keys.PREFS_KEY_SHOW_SIM_NUMBER, "1"));
        setupNumberLabel(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_SHOW_NUMBER_LABEL, true));
    }

    private void setupDate() {
        binding.itemEntry.date.setText(randomItem.getFormattedTimestamp(getString(R.string.today), getString(R.string.yesterday)));
    }

    private void setupContactIcon(boolean enabled, boolean tiles) {
        binding.itemEntry.contactIcon.setImageDrawable(null);
        binding.itemEntry.setShowIcon(enabled);
        Picasso.get().cancelRequest(binding.itemEntry.contactIcon);
        if (randomItem.getContactIcon()!=null)
            Picasso.get().load(randomItem.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.itemEntry.contactIcon);
        else if (tiles)
            binding.itemEntry.contactIcon.setImageDrawable(randomItem.getContactDrawable(requireContext()));
        else
            binding.itemEntry.contactIcon.setImageResource(R.drawable.ic_default_contact);
    }

    private void setupSim(String pref) {
        Log.d("ItemSettings", "setupSim: " + pref);
        switch (pref) {
            case "0" -> binding.itemEntry.setShowSim(false);
            case "1" -> binding.itemEntry.setShowSim(NewHome.nSim > 1);
            case "2" -> binding.itemEntry.setShowSim(true);
        }
    }

    private void setupNumberLabel(boolean showLabel) {
        binding.itemEntry.setShowLabel(showLabel);
    }

}
