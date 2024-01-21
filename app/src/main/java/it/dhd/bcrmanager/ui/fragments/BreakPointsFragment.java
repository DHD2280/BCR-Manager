package it.dhd.bcrmanager.ui.fragments;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.BreakpointsLayoutBinding;
import it.dhd.bcrmanager.objects.Breakpoints;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.adapters.BreakpointAdapter;
import it.dhd.bcrmanager.ui.dialogs.BreakpointDialog;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;
import it.dhd.bcrmanager.utils.VibratorUtils;

public class BreakPointsFragment extends Fragment {

    public static String PREF_KEY_BREAKPOINTS = "";

    static List<Breakpoints> breakpointsList;
    static BreakpointAdapter breakpointAdapter;

    private BreakpointsLayoutBinding binding;
    private final MediaPlayerService mediaPlayerService;

    public BreakPointsFragment(MediaPlayerService mps) {
        // Required empty public constructor
        this.mediaPlayerService = mps;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BreakpointsLayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        assert getArguments() != null;
        String filename = getArguments().getString("filename");
        assert filename != null;
        String prefName = filename.split("/")[filename.split("/").length - 1];
        PREF_KEY_BREAKPOINTS = prefName + "_breakpoints";
        // Get a list of all breakpoints
        Map<String, Map<String, String>> breakpoints = BreakpointUtils.loadBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS);
        breakpointsList = new ArrayList<>();
        // Iterate through the breakpoints and do something with them
        for (Map.Entry<String, Map<String, String>> entry : breakpoints.entrySet()) {
            String key = entry.getKey();
            Map<String, String> breakpoint = entry.getValue();

            // Extract values from the breakpoint
            String time = breakpoint.get("time");
            String title = breakpoint.get("title");
            String description = breakpoint.get("description");
            Log.d("BreakPointsFragment", "time: " + time + " title: " + title + " description: " + description);
            // Do something with the values, e.g., add them to a list
            // breakpointList.add(new Breakpoint(key, time, title, description));
            assert time != null;
            breakpointsList.add(new Breakpoints(key, Integer.parseInt(time), title, description));
        }
        breakpointAdapter = new BreakpointAdapter(breakpointsList,
                (view1, position, breakpoint) -> mediaPlayerService.seekTo(breakpoint.getTime()),
                (view1, position, breakpoint) -> showMenu(view1, R.menu.breakpoint_menu, breakpoint));
        binding.recyclerViewBreakpoints.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.recyclerViewBreakpoints.setAdapter(breakpointAdapter);

        binding.fabAddBreakpoint.setOnClickListener(v -> {
            BreakpointDialog breakpointDialog = new BreakpointDialog(this::editBreakpoint, this::addBreakpoint);
            Bundle args = new Bundle();
            args.putString("time", String.valueOf(mediaPlayerService.getCurrentPosition()));
            breakpointDialog.setArguments(args);
            breakpointDialog.show(getParentFragmentManager(), BreakpointDialog.class.getSimpleName());
        });
    }

    @SuppressLint("RestrictedApi")
    private void showMenu(View v, @MenuRes int menuRes, Breakpoints breakpoint) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.getMenuInflater().inflate(menuRes, popup.getMenu());

        if (popup.getMenu() instanceof MenuBuilder menuBuilder) {
            menuBuilder.setOptionalIconsVisible(true);
            for (MenuItem item : menuBuilder.getVisibleItems()) {
                int iconMarginPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 16, v.getResources().getDisplayMetrics());
                if (item.getIcon() != null) {
                    Drawable icon = item.getIcon();
                    icon.setTint(ThemeUtils.getPrimaryColor(requireContext()));
                    item.setIcon(new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0));
                }
            }
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            // Respond to menu item click.
            int menuId = menuItem.getItemId();
            if (menuId == R.id.edit_breakpoint) {
                BreakpointDialog breakpointDialog = new BreakpointDialog(this::editBreakpoint, this::addBreakpoint);
                Bundle args = new Bundle();
                args.putInt("position", breakpointsList.indexOf(breakpoint));
                args.putString("id", breakpoint.getId());
                args.putString("time", String.valueOf(breakpoint.getTime()));
                args.putString("title", breakpoint.getTitle());
                args.putString("desc", breakpoint.getDescription());
                breakpointDialog.setArguments(args);
                breakpointDialog.show(getParentFragmentManager(), "breakpointDialog");
            } else if (menuId == R.id.delete_breakpoint) {
                breakpointAdapter.notifyItemRemoved(breakpointsList.indexOf(breakpoint));
                breakpointsList.remove(breakpoint);
                deleteBreakpoint(breakpoint.getId());
            }

            return true;
        });
        // Show the popup menu.
        VibratorUtils.vibrate(requireContext(), 50);
        popup.show();
    }

    public void addBreakpoint(String time, String title, String description) {
        // Load existing breakpoints from preferences
        Map<String, Map<String, String>> breakpoints = BreakpointUtils.loadBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS);

        // Generate a new key for the new breakpoint
        String newKey = String.valueOf(breakpoints.size() + 1);

        // Create a new breakpoint
        Map<String, String> newBreakpoint = new HashMap<>();
        newBreakpoint.put("time", time);
        newBreakpoint.put("title", title);
        newBreakpoint.put("description", description);

        // Add the new breakpoint to the existing ones
        breakpoints.put(newKey, newBreakpoint);

        // Save the updated breakpoints to preferences
        BreakpointUtils.saveBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS, breakpoints);
        breakpointsList.add(new Breakpoints(newKey, Integer.parseInt(time), title, description));
        breakpointAdapter.notifyItemInserted(breakpointsList.size() - 1);
    }

    public void deleteBreakpoint(String breakpointId) {
        // Load existing breakpoints from preferences
        Map<String, Map<String, String>> breakpoints = BreakpointUtils.loadBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS);

        // Remove the specified breakpoint
        breakpoints.remove(breakpointId);

        // Save the updated breakpoints to preferences
        BreakpointUtils.saveBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS, breakpoints);
    }

    public void editBreakpoint(int position, String breakpointId, String title, String description) {
        // Load existing breakpoints from preferences
        Map<String, Map<String, String>> breakpoints = BreakpointUtils.loadBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS);

        // Check if the specified breakpoint exists
        if (breakpoints.containsKey(breakpointId)) {
            // Get the existing breakpoint
            Map<String, String> existingBreakpoint = breakpoints.get(breakpointId);

            // Update the existing breakpoint with new information
            existingBreakpoint.put("title", title);
            existingBreakpoint.put("description", description);

            // Save the updated breakpoints to preferences
            BreakpointUtils.saveBreakpoints(requireContext(), PREF_KEY_BREAKPOINTS, breakpoints);
        }
        if (breakpointsList.contains(breakpointsList.get(position))) {
            breakpointsList.get(position).setTitle(title);
            breakpointsList.get(position).setDescription(description);
            breakpointAdapter.notifyItemChanged(position);
        }
    }

}
