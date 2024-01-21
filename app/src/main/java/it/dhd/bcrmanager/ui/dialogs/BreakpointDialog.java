package it.dhd.bcrmanager.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import it.dhd.bcrmanager.R;

public class BreakpointDialog extends AppCompatDialogFragment {

    boolean edit = false;
    private String id = null;
    private int position = -1;

    private final onEditListener mEditListener;
    private final onAddListener mAddListener;

    public interface onEditListener {
        void onEdit(int position, String id, String title, String desc);
    }

    public interface onAddListener {
        void onAdd(String time, String title, String desc);
    }

    public BreakpointDialog(onEditListener editListener, onAddListener addListener) {
        // Required empty public constructor
        this.mEditListener = editListener;
        this.mAddListener = addListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.breakpoint_dialog, null);

        builder.setView(view);

        String time = "", title = null, desc = null;
        id = "";
        if (getArguments() != null) {
            time = getArguments().getString("time");
            title = getArguments().getString("title");
            id = getArguments().getString("id");
            if (id != null && !id.isEmpty()) {
                edit = true;
                position = getArguments().getInt("position");
            }
            desc = getArguments().getString("desc");
            Log.d("BreakpointDialog", "time: " + time + " title: " + title + " desc: " + desc);
        }

        // Define Views Variables
        TextInputEditText titleTV, descTV;
        MaterialButton btnNegative, btnPositive;

        // Setup Views
        titleTV = view.findViewById(R.id.breakpointDialogTitle);
        titleTV.setOnFocusChangeListener((v, hasFocus) -> {
            // your code here
            if (titleTV.getText().toString().trim().length() < 5) {
                titleTV.setError(requireContext().getString(R.string.breakpoints_title_empty));
            } else {
                // your code here
                titleTV.setError(null);
            }

        });
        descTV = view.findViewById(R.id.breakpointDialogDescription);
        btnNegative = view.findViewById(R.id.btnNegative);
        btnPositive = view.findViewById(R.id.btnPositive);

        if (title != null) titleTV.setText(title);
        if (desc != null) descTV.setText(desc);

        btnNegative.setOnClickListener(v -> dismiss());
        String finalTime = time;
        if (edit) btnPositive.setText(R.string.edit_breakpoint);
        btnPositive.setOnClickListener(v -> {
            if (titleTV.getText().toString().trim().length() == 0) {
                titleTV.setError(requireContext().getString(R.string.breakpoints_title_empty));
                return;
            }
            dismiss();
            if (edit && position != -1) mEditListener.onEdit(position, id, String.valueOf(titleTV.getText()), String.valueOf(descTV.getText()));
            else mAddListener.onAdd(finalTime, String.valueOf(titleTV.getText()), String.valueOf(descTV.getText()));
        });

        return builder.create();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.breakpoint_dialog,container, false);

        if (edit) Objects.requireNonNull(getDialog()).setTitle(R.string.edit_breakpoint);
        else Objects.requireNonNull(getDialog()).setTitle(R.string.add_breakpoint_title);

        return view;
    }
}
