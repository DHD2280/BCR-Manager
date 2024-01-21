package it.dhd.bcrmanager.ui.fragments.settingscontainers;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.BottomPlayerSettingsBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.ui.fragments.settings.PlayerPreferences;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.viewmodel.FileViewModel;

public class BottomPlayerSettings extends Fragment {

    private BottomPlayerSettingsBinding binding;
    private SharedPreferences prefs;
    private CallLogItem randomItem;
    private List<CallLogItem> itemHolder;
    private FileViewModel fileModel;

    public BottomPlayerSettings() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
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
            if (itemHolder != null && itemHolder.size() > 0) randomItem = itemHolder.get(new Random().nextInt(itemHolder.size()));
            setupItem();
        }
        super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = BottomPlayerSettingsBinding.inflate(inflater, container, false);
        requireActivity().setTitle(R.string.bottom_player);
        prefs = PreferenceUtils.getAppPreferences();
        if (fileModel.getSortedItems().size()> 0) {
            itemHolder = fileModel.getSortedItems().stream()
                    .filter(CallLogItem.class::isInstance) // Filter only CallLogItem
                    .map(CallLogItem.class::cast) // Converts Object to CallLogItem
                    .collect(Collectors.toList()); // Collects in a new list of CallLogItem
            randomItem = itemHolder.get(new Random().nextInt(itemHolder.size()));
        }

        if (randomItem != null) binding.player.setCallLogItem(randomItem);

        getChildFragmentManager().beginTransaction().add(R.id.settings_container,
                new PlayerPreferences(new PlayerPreferences.onPreferenceChangeListener() {
                    @Override
                    public void onSimChanged(String newType) {
                        setupSim(newType);
                    }

                    @Override
                    public void onLabelChanged(boolean newType) {
                        setupLabel(newType);
                    }
                })).commit();

        setupItem();

        return binding.getRoot();
    }

    private void setupItem() {
        if (randomItem == null) return;
        binding.player.setShowLabel(prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_PLAYER_SHOW_LABEL, true));
        binding.player.setShowSim(PreferenceUtils.showSimPlayer(SimUtils.getNumberOfSimCards(requireContext())));
        binding.player.setCallLogItem(randomItem);

        switch (randomItem.getDirection()) {
            case "in" -> binding.player.callIconPlayer.setImageResource(R.drawable.ic_in);
            case "out" -> binding.player.callIconPlayer.setImageResource(R.drawable.ic_out);
            case "conference" -> binding.player.callIconPlayer.setImageResource(R.drawable.ic_conference);
        }

        binding.player.datePlayer.setText(randomItem.getFormattedTimestamp(requireContext().getString(R.string.today), requireContext().getString(R.string.yesterday)));

        if (randomItem.getContactIcon()!=null)
            Picasso.get().load(randomItem.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.player.contactIconPlayer);
        else if (PreferenceUtils.showTiles())
            binding.player.contactIconPlayer.setImageDrawable(randomItem.getContactDrawable(requireContext()));
        else
            binding.player.contactIconPlayer.setImageResource(R.drawable.ic_default_contact);
        
        binding.player.datePlayer.setText(randomItem.getFormattedTimestamp(requireContext().getString(R.string.today), requireContext().getString(R.string.yesterday)));
    }

    private void setupSim(String pref) {
        switch (pref) {
            case "0" -> binding.player.setShowSim(false);
            case "1" -> binding.player.setShowSim(SimUtils.getNumberOfSimCards(requireContext()) > 1);
            case "2" -> binding.player.setShowSim(true);
        }
    }

    private void setupLabel(boolean enabled) {
        binding.player.setShowLabel(enabled);
    }

}
