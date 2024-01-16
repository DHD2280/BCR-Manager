package it.dhd.bcrmanager.ui.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayoutMediator;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.LayoutBottomPlayerBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;

public class PlayerFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    DemoCollectionAdapter demoCollectionAdapter;

    private LayoutBottomPlayerBinding binding;
    private Handler mUpdateProgress;
    private Runnable mUpdateProgressTask;
    boolean isSeeking;
    private String fileName;

    private static CallLogItem currentItem;

    List<Object> allItems;

    private final MediaPlayerService mediaPlayerService;
    private final PlayerStopUpdater stopUp;
    private final PlayerStartUpdater startUp;
    private final StarredInterface starredInterface;
    private final UpdatePlaying updatePlaying;
    private final DialogFinish dialogFinish;

    public interface PlayerStopUpdater {
        void stopUp();
    }

    public interface PlayerStartUpdater {
        void startUp();
    }

    public interface DialogFinish {
        void onFinish(CallLogItem item);
    }

    public interface StarredInterface {
        void setStarred(CallLogItem item);
        void setUnstarred(CallLogItem item);
    }

    public interface UpdatePlaying {
        void updatePlaying(CallLogItem oldItem, CallLogItem newItem);
    }

    public PlayerFragment(List<Object> listOfItems, CallLogItem currentPlayingItem,
                          MediaPlayerService mps, PlayerStopUpdater stopUpdaterInt,
                          PlayerStartUpdater startUpdaterInt, StarredInterface starredInterface,
                          UpdatePlaying updatePlaying, DialogFinish dialogFinish) {
        this.mediaPlayerService = mps;
        allItems = listOfItems;
        currentItem = currentPlayingItem;
        this.stopUp = stopUpdaterInt;
        this.startUp = startUpdaterInt;
        this.starredInterface = starredInterface;
        this.updatePlaying = updatePlaying;
        this.dialogFinish = dialogFinish;
    }

    @Override
    public void onDismiss(@NonNull @NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d("PlayerFragment", "onDismiss: " + currentItem.getContactName());
        dialogFinish.onFinish(currentItem);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new BottomSheetDialog(requireContext(), getTheme());
        dialog.setOnShowListener(it -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) it;
            View parentLayout = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (parentLayout != null) {
                BottomSheetBehavior.from(parentLayout).setState(BottomSheetBehavior.STATE_EXPANDED);
                DisplayMetrics displaymetrics = new DisplayMetrics();
                requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int screenHeight = displaymetrics.heightPixels;
                BottomSheetBehavior.from(parentLayout).setPeekHeight(screenHeight, true);
                setupFullHeight(parentLayout);
            }
        });
        return dialog;
    }

    private void setupFullHeight(View bottomSheet) {
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(layoutParams);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = LayoutBottomPlayerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isSeeking = false;

        if (currentItem != null) {
            boolean isContactSaved = currentItem.isContactSaved();
            fileName = currentItem.getFileName();
            Log.d("PlayerFragment", "number: " + currentItem.getNumber() + " saved: " +isContactSaved);
            if (currentItem.getContactName()!=null) binding.contactNamePlayerDialog.setText(currentItem.getContactName());
            else binding.contactNamePlayerDialog.setText(currentItem.getNumberFormatted());

            switch (currentItem.getDirection()) {
                case "in" -> binding.callTypeIconPlayerDialog.setImageResource(R.drawable.ic_in);
                case "out" -> binding.callTypeIconPlayerDialog.setImageResource(R.drawable.ic_out);
                case "conference" -> binding.callTypeIconPlayerDialog.setImageResource(R.drawable.ic_conference);
            }

            if (SimUtils.getNumberOfSimCards(requireContext())>1) {
                if (!TextUtils.isEmpty(String.valueOf(currentItem.getSimSlot())))
                    binding.simSlotPlayerDialog.setText(String.valueOf(currentItem.getSimSlot()));
            } else {
                binding.simSlotPlayerDialog.setVisibility(View.GONE);
                binding.dividerSimPlayerDialog.setVisibility(View.GONE);
            }

            binding.datePlayerDialog.setText(currentItem.getFormattedTimestampComplete(requireContext().getString(R.string.today),
                    requireContext().getString(R.string.yesterday)));

        }
        Slider seekBar = binding.seekBarPlayer;
        if (!mediaPlayerService.isPlaying()) {
            binding.seekBarSpeed.setValue(mediaPlayerService.getLastSpeed());
            binding.currentTimePlayer.setText(formatTime(mediaPlayerService.getLastPosition()));
            binding.seekBarPlayer.setValue((int) ((long) mediaPlayerService.getLastPosition() * 100 / mediaPlayerService.getDuration()));
        }
        binding.seekBarPlayer.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int progress = (int)seekBar.getValue();
                int duration = mediaPlayerService.getDuration();
                int newPosition = (int) (((float) progress / 100) * duration);

                // Seek to the new position
                mediaPlayerService.seekTo(newPosition);

                // Hide the TextView
                isSeeking = false;
            }
        });
        seekBar.setLabelFormatter(value -> {
            int progress = (int)value;
            int duration = mediaPlayerService.getDuration();
            int newPosition = (int) (((float) progress / 100) * duration);
            return formatTime(newPosition);
        });

        int durationMillis = mediaPlayerService.getDuration();
        binding.totalTimePlayer.setText(String.format(Locale.getDefault(), "%02d:%02d",
                (durationMillis / 1000) / 60, durationMillis / 1000 % 60));
        if (mediaPlayerService.isPlaying()) {
            playPauseButtonIcon(true);
            startUpdater();
        } else playPauseButtonIcon(false);

        binding.btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                playPauseButtonIcon(false);
                stopUpdater(true);
            } else {
                mediaPlayerService.resumePlayback();
                startUpdater();
                playPauseButtonIcon(true);
            }
            updatePlaying.updatePlaying(currentItem, currentItem);
        });

        if (currentItem.isStarred()) {
            binding.favoritePlayerDialog.setImageResource(R.drawable.ic_star);
        } else {
            binding.favoritePlayerDialog.setImageResource(R.drawable.ic_unstar);
        }

        binding.favoritePlayerDialog.setOnClickListener(v -> {
            if (currentItem.isStarred()) {
                currentItem.setStarred(false);
                binding.favoritePlayerDialog.setImageResource(R.drawable.ic_unstar);
                starredInterface.setUnstarred(currentItem);
            } else {
                currentItem.setStarred(true);
                binding.favoritePlayerDialog.setImageResource(R.drawable.ic_star);
                starredInterface.setStarred(currentItem);
            }
            PreferenceUtils.setStarred(currentItem.getFileName(), currentItem.isStarred());
            RotateAnimation rotate = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(300);
            rotate.setInterpolator(new LinearInterpolator());

            binding.favoritePlayerDialog.startAnimation(rotate);
            updatePlaying.updatePlaying(currentItem, currentItem);

        });

        binding.btnPrevious10.setOnClickListener(v -> {
            int currentPosition = mediaPlayerService.getCurrentPosition();
            int newPosition = currentPosition - 10000;
            if (newPosition<0) newPosition = 0;
            mediaPlayerService.seekTo(newPosition);
        });
        binding.btnForward10.setOnClickListener(v -> {
            int currentPosition = mediaPlayerService.getCurrentPosition();
            int newPosition = currentPosition + 10000;
            int duration = mediaPlayerService.getDuration();
            if (newPosition>duration) newPosition = duration;
            mediaPlayerService.seekTo(newPosition);
        });
        binding.btnPrevious.setOnClickListener(this);
        binding.btnNext.setOnClickListener(this);

        binding.seekBarSpeed.setValueFrom(0.25f);
        binding.seekBarSpeed.setValueTo(2.0f);
        if (mediaPlayerService.isPlaying()) binding.seekBarSpeed.setValue(mediaPlayerService.getSpeed());
        else binding.seekBarSpeed.setValue(mediaPlayerService.getLastSpeed());
        final float[] current = {0};
        binding.seekBarSpeed.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                if (current[0] != slider.getValue()) {
                    current[0] = slider.getValue();
                    Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                mediaPlayerService.setSpeed(slider.getValue());
            }
        });

        demoCollectionAdapter = new DemoCollectionAdapter(this, fileName);
        binding.viewPager.setAdapter(demoCollectionAdapter);
        new TabLayoutMediator(view.findViewById(R.id.into_tab_layout), binding.viewPager, (tab, pos) -> {}).attach();

        return view;

    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public void startUpdater() {
        startUp.startUp();
        playPauseButtonIcon(true);
        if (mUpdateProgress == null) {
            mUpdateProgress = new Handler();
        }
        mUpdateProgressTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerService!=null && mediaPlayerService.isPlaying()) {
                    int currentMillis = mediaPlayerService.getCurrentPosition();
                    int durationMillis = mediaPlayerService.getDuration();
                    int progress = (int) ((long) currentMillis * 100 / durationMillis);
                    if (!isSeeking) binding.seekBarPlayer.setValue(progress);
                    mUpdateProgress.postDelayed(this, 50);
                    binding.currentTimePlayer.setText(formatTime(currentMillis));
                } else {
                    stopUpdater(true);
                }
            }
        };
        mUpdateProgress.postDelayed(mUpdateProgressTask, 50);
    }

    public void playPauseButtonIcon(boolean isPlaying) {
        Drawable drawable;
        if (isPlaying) {
            drawable = ContextCompat.getDrawable(requireContext(), R.drawable.play_to_pause);
        } else {
            drawable = ContextCompat.getDrawable(requireContext(), R.drawable.pause_to_play);
        }
        binding.btnPlayPause.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
    }

    private void stopUpdater(boolean stopOtherToo) {
        if (stopOtherToo) stopUp.stopUp();
        if (mUpdateProgress == null) return;
        mUpdateProgress.removeCallbacks(mUpdateProgressTask, null);
        mUpdateProgress = null;
        mUpdateProgressTask = null;
        if (binding != null) playPauseButtonIcon(false);
        Log.d("Updater", "playPause false");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdater(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.btnPrevious && v.getId() != R.id.btnNext) return;
        int currentPosition = allItems.indexOf(currentItem);
        int newPosition = 0;
        mediaPlayerService.stopPlayback();
        stopUpdater(true);
        Animation animation = null;

        if (v.getId() == R.id.btnPrevious) {
            newPosition = previous(currentPosition);
            animation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
        } else if(v.getId() == R.id.btnNext) {
            newPosition = next(currentPosition);
            animation = AnimationUtils.loadAnimation(requireContext(), R.anim.swipe_right);
        }

        CallLogItem newPlaying = (CallLogItem) allItems.get(newPosition);

        Log.d("PlayerFragment", "currentPosition: + " + currentPosition + " newPosition: "+newPosition);
        updatePlaying.updatePlaying(currentItem, newPlaying);
        CallLogItem oldHolder = currentItem;
        currentItem.setPlaying(false);
        currentItem = newPlaying;
        newPlaying.setPlaying(true);
        updatePlaying.updatePlaying(oldHolder, newPlaying);
        // find your fragment
        fileName = newPlaying.getFileName();

        binding.callTypeIconPlayerDialog.setImageResource(newPlaying.getDirection().equals("in") ? R.drawable.ic_in : R.drawable.ic_out);

        if (binding.simSlotPlayerDialog.getVisibility() == View.VISIBLE) binding.simSlotPlayerDialog.setText(newPlaying.getSimSlot());

        binding.datePlayerDialog.setText(newPlaying.getFormattedTimestamp(requireContext().getString(R.string.today),
                requireContext().getString(R.string.yesterday)));

        if (newPosition<0 || newPosition>allItems.size()-1) return;
        mediaPlayerService.startPlayback(getContext(), Uri.parse(newPlaying.getAudioFilePath()));
        mediaPlayerService.setOnCompletionListener(mp -> stopUpdater(true));
        startUpdater();

        int durationMillis = mediaPlayerService.getDuration();
        binding.totalTimePlayer.setText(String.format(Locale.getDefault(), "%02d:%02d",
                (durationMillis / 1000) / 60, durationMillis / 1000 % 60));
        if (newPlaying.getContactName()!=null) binding.contactNamePlayerDialog.setText(newPlaying.getContactName());
        else binding.contactNamePlayerDialog.setText(newPlaying.getNumberFormatted());

        if (newPlaying.isStarred()) {
            binding.favoritePlayerDialog.setImageResource(R.drawable.ic_star);
        } else {
            binding.favoritePlayerDialog.setImageResource(R.drawable.ic_unstar);
        }

        demoCollectionAdapter = new DemoCollectionAdapter(this, fileName);
        binding.viewPager.setAdapter(demoCollectionAdapter);


        //Picasso.get().load(contactImage).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).error(R.drawable.ic_default_contact).into(contactIconIV);
        if (animation!=null) {
            binding.contactNamePlayerDialog.setAnimation(animation);
            binding.callInfoLayout.setAnimation(animation);
            binding.viewPager.setAnimation(animation);
            binding.favoritePlayerDialog.setAnimation(animation);
        }
        binding.seekBarSpeed.setValue(mediaPlayerService.getSpeed());
    }

    private int previous(int current) {
        int previous = current - 1;
        if (previous<0) {
            previous = allItems.size()-1;
        }
        if (allItems.get(previous) instanceof DateHeader) {
            previous = previous(previous);
        }
        return previous;
    }

    private int next(int current) {
        int next = current + 1;
        if (next>allItems.size()-1) next = 0;
        if (allItems.get(next) instanceof DateHeader) {
            next = next + 1;
        }
        return next;
    }

    public static class DemoCollectionAdapter extends FragmentStateAdapter {

        private final int mPageNumbers = 2;
        final String fileName;

        public DemoCollectionAdapter(Fragment fragment, String fileName) {
            super(fragment);
            this.fileName = fileName;
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
                case 0 -> fragment = new ContactIconFragment();
                case 1 -> fragment = new BreakPointsFragment();
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


    public static class ContactIconFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.contact_image, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

            ImageView contactImageView = view.findViewById(R.id.contactIconImageView);
            contactImageView.setImageDrawable(null);
            if (currentItem.getContactIcon()!=null)
                Picasso.get().load(currentItem.getContactIcon())
                        .transform(new CircleTransform())
                        .placeholder(R.drawable.ic_default_contact)
                        .error(R.drawable.ic_default_contact)
                        .into(contactImageView);
            else if (currentItem.getContactDrawable(requireContext())!=null)
                contactImageView.setImageDrawable(currentItem.getContactDrawable(requireContext()));
            else
                contactImageView.setImageResource(R.drawable.ic_default_contact);

        }
    }

}
