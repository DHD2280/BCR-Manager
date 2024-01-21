package it.dhd.bcrmanager.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import it.dhd.bcrmanager.databinding.LayoutBottomPlayerBinding;
import it.dhd.bcrmanager.objects.CallLogItem;

public class TrimAudioFragment extends Fragment {

    private LayoutBottomPlayerBinding binding;
    private final CallLogItem recordingToTrim;

    public TrimAudioFragment(CallLogItem item) {
        this.recordingToTrim = item;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = LayoutBottomPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

}
