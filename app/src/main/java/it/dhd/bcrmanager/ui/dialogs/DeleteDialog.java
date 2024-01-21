package it.dhd.bcrmanager.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
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
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class DeleteDialog extends AppCompatDialogFragment {

    private final CallLogItem itemToDelete;

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
        it.dhd.bcrmanager.databinding.DeleteDialogBinding binding = DeleteDialogBinding.inflate(requireActivity().getLayoutInflater());

        builder.setView(binding.getRoot());

        binding.itemEntry.setShowIcon(PreferenceUtils.showIcon());
        binding.itemEntry.setShowLabel(PreferenceUtils.showLabel());
        binding.itemEntry.setShowLabel(PreferenceUtils.showLabel());
        binding.itemEntry.setCallLogItem(itemToDelete);
        binding.itemEntry.actionPlay.setVisibility(View.GONE);
        binding.itemEntry.expandingLayout.setVisibility(View.GONE);

        if (itemToDelete.getContactIcon() != null) {
            Picasso.get().load(itemToDelete.getContactIcon()).error(R.drawable.ic_default_contact).placeholder(R.drawable.ic_default_contact).transform(new CircleTransform()).into(binding.itemEntry.contactIcon);
        } else if (PreferenceUtils.showTiles()) {
            binding.itemEntry.contactIcon.setImageDrawable(itemToDelete.getContactDrawable(requireContext()));
        } else {
            binding.itemEntry.contactIcon.setImageResource(R.drawable.ic_default_contact);
        }

        switch (itemToDelete.getDirection()) {
            case "out" -> binding.itemEntry.callIcon.setImageResource(R.drawable.ic_out);
            case "conference" -> binding.itemEntry.callIcon.setImageResource(R.drawable.ic_conference);
            default -> binding.itemEntry.callIcon.setImageResource(R.drawable.ic_in);
        }

        binding.itemEntry.date.setText(itemToDelete.getFormattedTimestamp(getString(R.string.today), getString(R.string.yesterday)));

        binding.btnNegative.setOnClickListener(v -> {
            // Dismiss the alert dialog
            Objects.requireNonNull(getDialog()).cancel();
        });

        binding.btnPositive.setOnClickListener(v -> {
            // Dismiss the alert dialog
            MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(requireActivity());
            mBuilder.setTitle("Are you sure?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        // Dismiss the alert dialog
                        dialog.dismiss();
                        Objects.requireNonNull(getDialog()).dismiss();
                        // Call the method in MainActivity to delete this item
                        onItemDeleted.onItemDeleted();

                    });
            mBuilder.show();
        });

        return builder.create();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DeleteDialogBinding binding = DeleteDialogBinding.inflate(inflater, container, false);

        Objects.requireNonNull(getDialog()).setTitle(requireContext().getString(R.string.delete_dialog_message));

        return binding.getRoot();
    }

}
