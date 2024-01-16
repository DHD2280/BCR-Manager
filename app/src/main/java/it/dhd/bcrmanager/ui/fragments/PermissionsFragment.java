package it.dhd.bcrmanager.ui.fragments;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.SetupPermissionsBinding;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class PermissionsFragment extends Fragment {

    private SetupPermissionsBinding binding;

    ArrayList<String> permissionsList;
    String[] permissionsStr = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
    };
    int permissionsCount = 0;

    public PermissionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.mHideMenu = true;
        ((AppCompatActivity)requireActivity()).supportInvalidateOptionsMenu();
        permissionsList = new ArrayList<>();
        permissionsList.addAll(Arrays.asList(permissionsStr));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = SetupPermissionsBinding.inflate(inflater, container, false);

        binding.setupPermissionsLoadingProgress.setVisibility(View.GONE);

        binding.setupPermissionsGrant.setOnClickListener(v -> askForPermissions(permissionsList));

        return binding.getRoot();
    }

    /**
     * Register for permissions result
     */
    ActivityResultLauncher<String[]> permissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    new ActivityResultCallback<>() {
                        @Override
                        public void onActivityResult(Map<String, Boolean> result) {
                            ArrayList<Boolean> list = new ArrayList<>(result.values());
                            permissionsList = new ArrayList<>();
                            permissionsCount = 0;
                            for (int i = 0; i < list.size(); i++) {
                                if (shouldShowRequestPermissionRationale(permissionsStr[i])) {
                                    permissionsList.add(permissionsStr[i]);
                                } else if (!PermissionsUtil.hasPermission(permissionsStr[i])) {
                                    permissionsCount++;
                                }
                            }
                            if (permissionsList.size() > 0) {
                                //Some permissions are denied and can be asked again.
                                askForPermissions(permissionsList);
                            } else {
                                //All permissions granted. Do your stuff 🤞
                                binding.setupPermissionsLoadingProgress.setVisibility(View.GONE);
                                PreferenceUtils.setFirstTime();
                                binding.setupPermissionsGrant.setIcon(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_all_done));
                                binding.setupPermissionsGrant.setText(R.string.setup_permissions_granted);
                                binding.setupPermissionsGrant.setOnClickListener(v ->
                                        getParentFragmentManager().beginTransaction()
                                                .replace(R.id.frame_layout, new NewHome(), NewHome.class.getSimpleName())
                                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                                .commit());
                            }
                        }
                    });

    /**
     * Ask for permissions
     * @param permissionsList The list of permissions to ask
     */
    private void askForPermissions(ArrayList<String> permissionsList) {
        String[] newPermissionStr = new String[permissionsList.size()];
        for (int i = 0; i < newPermissionStr.length; i++) {
            newPermissionStr[i] = permissionsList.get(i);
        }
        if (newPermissionStr.length > 0) {
            binding.setupPermissionsLoadingProgress.setVisibility(View.VISIBLE);
            permissionsLauncher.launch(newPermissionStr);
        } else {
            /* User has pressed 'Deny & Don't ask again' so we have to show the enable permissions dialog
            which will lead them to app details page to enable permissions from there. */
            binding.setupPermissionsLoadingProgress.setVisibility(View.GONE);
        }
    }

}
