package it.dhd.bcrmanager.ui.fragments.player;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.fragments.BreakPointsFragment;

public class PlayerCollectionAdapter extends FragmentStateAdapter {

    private final int mPageNumbers = 2;
    private final String fileName;
    private final MediaPlayerService mediaPlayerService;

    public PlayerCollectionAdapter(Fragment fragment, String fileName, MediaPlayerService mps) {
        super(fragment);
        this.fileName = fileName;
        this.mediaPlayerService = mps;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int).
        Fragment fragment = null;
        Bundle args = new Bundle();
        // The object is just an integer.
        String frag = "";
        switch (position) {
            case 0 -> fragment = new PlayerFragment.ContactIconFragment();
            case 1 -> fragment = new BreakPointsFragment(mediaPlayerService);
        }
        args.putString("frag", frag);
        args.putString("filename", fileName);
        assert fragment != null;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return mPageNumbers;
    }
}
