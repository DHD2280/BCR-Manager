package it.dhd.bcrmanager.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.DeleteDialogBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.UriUtils;

public class DeleteDialog extends AppCompatDialogFragment {

    private final CallLogItem itemToDelete;
    private DeleteDialogBinding binding;

    private final onItemDeletedListener onItemDeleted;


    public DeleteDialog(CallLogItem item, onItemDeletedListener listener) {
        this.onItemDeleted = listener;
        this.itemToDelete = item;
    }

    public interface onItemDeletedListener {
        void onItemDeleted();
    }

    @NonNull
    @SuppressLint("SetTextI18n")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
        binding = DeleteDialogBinding.inflate(requireActivity().getLayoutInflater());

        builder.setView(binding.getRoot());

        assert getArguments() != null;
        int nSim = getArguments().getInt("nSim");

        String phoneNumber = itemToDelete.getNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (itemToDelete.getContactIcon() != null) {
                Picasso.get().load(itemToDelete.getContactIcon()).error(R.drawable.ic_default_contact).placeholder(R.drawable.ic_default_contact).transform(new CircleTransform()).into(binding.contactIconDelete);
            } else if (PreferenceUtils.showTiles()) {
                binding.contactIconDelete.setImageDrawable(itemToDelete.getContactDrawable(requireContext()));
            } else {
                binding.contactIconDelete.setImageResource(R.drawable.ic_default_contact);
            }
            binding.contactNameDelete.setText(itemToDelete.getContactName());
        }

        if (nSim<=1) {
            binding.dividerSimDelete.setVisibility(View.GONE);
            binding.simSlotDelete.setVisibility(View.GONE);
            binding.dividerDateDelete.setVisibility(View.GONE);
        } else {
            binding.simSlotDelete.setText(itemToDelete.getSimSlot().toString());
        }

        if(itemToDelete.getDirection().contains("in")) binding.callIcon.setImageResource(R.drawable.ic_in);
        else binding.callIcon.setImageResource(R.drawable.ic_out);

        binding.dateDelete.setText(itemToDelete.getFormattedTimestamp(getString(R.string.today), getString(R.string.yesterday)));

        binding.durationDelete.setText(itemToDelete.getFormattedDuration(getString(R.string.format_sec), getString(R.string.format_min)));


        // Set click listener for the expand button
        //holder.expandButton.setOnClickListener(v -> toggleExpansion(holder.expandingLayout));
        binding.btnNegative.setOnClickListener(v -> {
            // Dismiss the alert dialog
            Objects.requireNonNull(getDialog()).cancel();
        });
        binding.btnPositive.setOnClickListener(v -> {
            // Dismiss the alert dialog
            MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(requireActivity());
            mBuilder.setTitle("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Dismiss the alert dialog
                        Objects.requireNonNull(getDialog()).dismiss();
                        // Call the method in MainActivity to delete this item

                        onItemDeleted.onItemDeleted();

                        Uri fileUri, audioUri;
                        fileUri = Uri.parse(itemToDelete.getFilePath());
                        audioUri = Uri.parse(itemToDelete.getAudioFilePath());

                        if (UriUtils.areEqual(fileUri, audioUri)) {
                            FileUtils.deleteFileUri(requireContext(), audioUri);
                        } else {
                            FileUtils.deleteFileUri(requireContext(), fileUri);
                            FileUtils.deleteFileUri(requireContext(), audioUri);
                        }

                    });
            mBuilder.show();
        });

        return builder.create();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DeleteDialogBinding binding = DeleteDialogBinding.inflate(inflater, container, false);
        assert getArguments() != null;
        String message = getArguments().getString("message");

        Objects.requireNonNull(getDialog()).setTitle(message);

        return binding.getRoot();
    }

}
