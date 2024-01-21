package it.dhd.bcrmanager.ui.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ContactActivityBinding;
import it.dhd.bcrmanager.loaders.JsonFileLoader;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.adapters.CallLogAdapter;
import it.dhd.bcrmanager.ui.fragments.player.PlayerFragment;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;
import it.dhd.bcrmanager.utils.WrapContentLinearLayoutManager;

public class ContactActivity
        extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<JsonFileLoader.TwoListsWrapper> {

    private List<ContactItem> contactList;
    private List<Object> callLogList;
    private int FILTER_TYPE = 0;

    private CallLogAdapter mAdapter;

    private ContactActivityBinding binding;

    private String contactName;

    private MediaPlayerService mediaPlayerService;
    private CallLogItem currentlyPlaying;

    private Handler mUpdateProgress;
    private Runnable mUpdateProgressTask;
    private boolean isBound = false;
    private boolean onlyStarred = false;
    private String lookupKey;

    private final List<Object> contactCalls = new ArrayList<>();
    private final List<Object> contactCallsFiltered = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceUtils.init(getApplicationContext());
        SharedPreferences prefs = PreferenceUtils.getAppPreferences();
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.getDarkTheme());
        if (prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_DYNAMIC_COLOR, true)) {
            DynamicColors.applyToActivityIfAvailable(this);
            DynamicColors.applyToActivitiesIfAvailable(getApplication());
        } else {
            getTheme().applyStyle(ThemeUtils.getColorThemeStyleRes(), true);
        }

        PermissionsUtil.init(this);

        binding = ContactActivityBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Intent intent = getIntent();
        contactName = intent.getStringExtra("contact");
        if (intent.getStringExtra("lookupKey")!=null) lookupKey = intent.getStringExtra("lookupKey");
        assert contactName != null;
        if (contactName.equals("starred_contacts")) {
            lookupKey = "starred_contacts";
            onlyStarred = true;
        }

        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        this.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);   
        
        LoaderManager.getInstance(this).initLoader(1, null, this);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mediaPlayerService != null && mediaPlayerService.isPlaying()) {
            startUpdater();
        }

    }

    @Override
    public void onDestroy() {
        if (isBound) {
            this.unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (MainActivity.active) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else {
                this.finish();
            }
            return true;
        } else if (item.getItemId() == R.id.menu_filter) {
            switch (FILTER_TYPE) {
                case 0 -> {
                    FILTER_TYPE = 1;
                    item.setIcon(R.drawable.ic_in);
                    filter("in");
                }
                case 1 -> {
                    FILTER_TYPE = 2;
                    item.setIcon(R.drawable.ic_out);
                    filter("out");
                }
                case 2 -> {
                    FILTER_TYPE = 0;
                    item.setIcon(R.drawable.ic_filter);
                    filter("");
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filter_menu, menu);
        return true;
    }

    @NonNull
    @Override
    public Loader<JsonFileLoader.TwoListsWrapper> onCreateLoader(int id, @Nullable Bundle args) {
        return new JsonFileLoader(this, lookupKey);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<JsonFileLoader.TwoListsWrapper> loader, JsonFileLoader.TwoListsWrapper data) {

        contactList = data.contactList();
        if (onlyStarred) callLogList = data.starredItemsList();
        else callLogList = data.sortedListWithHeaders();

        setupUi();

    }

    private void setupUi() {
        binding.progressBar.setVisibility(View.GONE);
        binding.contactIcon.setVisibility(View.VISIBLE);
        binding.contactName.setVisibility(View.VISIBLE);
        binding.actionsLayout.setVisibility(View.VISIBLE);
        binding.toolbar.setVisibility(View.VISIBLE);
        Uri contactIconUri = null;
        String phoneNumber = null;
        String phoneNumberFormatted = null;
        boolean contactSaved = false;
        int pos = 0;

        for (ContactItem contact : contactList) {
            if (contact.getLookupKey()!=null && contact.getLookupKey().equals(lookupKey)) {
                contactIconUri = contact.getContactImage();
                contactName = contact.getContactName();
                contactSaved = contact.isContactSaved();
                phoneNumber = contact.getPhoneNumber();
                phoneNumberFormatted = contact.getPhoneNumberFormatted();
                pos = contactList.indexOf(contact);
                break;
            }
        }

        if (onlyStarred) {
            for (Object item : callLogList) {
                if (item instanceof CallLogItem)
                    if (((CallLogItem)item).isStarred())
                        contactCalls.add(item);
            }
        } else {
            for (Object item : callLogList) {
                if (item instanceof CallLogItem)
                    if (((CallLogItem)item).getContactName()!=null && ((CallLogItem)item).getContactName().equals(contactName))
                        contactCalls.add(item);
                    else if (contactName == null || contactName.isEmpty() || ((CallLogItem)item).getNumberFormatted().equals(phoneNumberFormatted))
                        contactCalls.add(item);

            }
        }
        contactCallsFiltered.addAll(contactCalls);


        if (onlyStarred) binding.contactIcon.setImageResource(R.drawable.ic_star);
        else {
            if (contactIconUri != null)
                Picasso.get().load(contactIconUri).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.contactIcon);
            else if (PreferenceUtils.showTiles())
                binding.contactIcon.setImageDrawable(contactList.get(pos).getContactDrawable(this));
            else
                binding.contactIcon.setImageResource(R.drawable.ic_default_contact);
        }

        if (contactName.equals("starred_contacts")) binding.contactName.setText(getString(R.string.starred));
        else binding.contactName.setText(contactName);

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    if (contactName.equals("starred_contacts")) binding.collapsingToolbarLayout.setTitle(getString(R.string.starred));
                    else binding.collapsingToolbarLayout.setTitle(contactName);
                    isShow = true;
                } else if(isShow) {
                    binding.collapsingToolbarLayout.setTitle(" ");//careful there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });

        mAdapter = new CallLogAdapter(this, contactCallsFiltered, mediaPlayerService, this::onPlay, this::onDelete);
        mAdapter.setContactActionsEnabled(false);

        binding.recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));
        binding.recyclerView.setAdapter(mAdapter);

        String finalPhoneNumber = phoneNumber;
        if (!onlyStarred) {
            binding.actionsLayoutInclude.actionCall.setOnClickListener(v -> {
                Intent intent1 = new Intent(Intent.ACTION_DIAL);
                intent1.setData(Uri.parse("tel:" + finalPhoneNumber));
                startActivity(intent1);
            });

            String finalPhoneNumber1 = phoneNumber;
            binding.actionsLayoutInclude.actionMessage.setOnClickListener(v -> {
                Intent intent1 = new Intent(Intent.ACTION_SENDTO);
                intent1.setData(Uri.parse("smsto:" + finalPhoneNumber1));
                startActivity(intent1);
            });

            binding.actionsLayoutInclude.actionCreateContact.setVisibility(contactSaved ? LinearLayout.GONE : LinearLayout.VISIBLE);
            String finalPhoneNumber2 = phoneNumber;
            binding.actionsLayoutInclude.actionCreateContact.setOnClickListener(v -> {
                Intent intent1 = new Intent(Intent.ACTION_INSERT);
                intent1.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent1.putExtra(ContactsContract.Intents.Insert.PHONE, finalPhoneNumber2);
                startActivity(intent1);
            });
        } else {
            binding.actionsLayout.setVisibility(View.GONE);
        }

        List<Object> contactCallsHolder = new ArrayList<>(callLogList);
        binding.playerInfoBarContainer.setOnClickListener(v -> {

            PlayerFragment playerFragment = new PlayerFragment(contactCallsHolder, currentlyPlaying, mediaPlayerService,
                    this::stopUpdater, this::startUpdater,
                    new PlayerFragment.StarredInterface() {
                        @Override
                        public void setStarred(CallLogItem item) {
                            mAdapter.notifyItemChanged(callLogList.indexOf(item));
                        }

                        @Override
                        public void setUnstarred(CallLogItem item) {
                            mAdapter.notifyItemChanged(callLogList.indexOf(item));
                        }
                    },
                    (oldItem, newItem) -> {
                        mAdapter.notifyItemChanged(callLogList.indexOf(oldItem));
                        mAdapter.notifyItemChanged(callLogList.indexOf(newItem));
                    },
                    (item) -> mAdapter.notifyItemChanged(callLogList.indexOf(item)) );
            playerFragment.show(
                    getSupportFragmentManager(), PlayerFragment.class.getSimpleName()
            );

        });
    }

    private void onDelete(CallLogItem callLogItem, RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        int index = contactCallsFiltered.indexOf(callLogItem);
        contactCallsFiltered.remove(callLogItem);
        contactCalls.remove(callLogItem);
        mAdapter.notifyItemRemoved(index);
        FileUtils.deleteRegistration(this, callLogItem);
    }

    private void filter(String direction) {
        contactCallsFiltered.clear();
        if (direction.isEmpty()) {
            contactCallsFiltered.addAll(contactCalls);
        } else {
            for (Object item : contactCalls) {
                if (item instanceof CallLogItem)
                    if (((CallLogItem)item).getDirection().equals(direction))
                        contactCallsFiltered.add(item);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void onPlay(MaterialButton playButton, CallLogItem item, RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        setPlayerInfo(item);
        playButton.setIconTint(ColorStateList.valueOf(ThemeUtils.getPrimaryColor(this)));
        Drawable drawable = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.play_to_pause);

        if (currentlyPlaying == item) {
            // If it's the same item that is currently playing, pause or resume playback
            if (mediaPlayerService != null) {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.pausePlayback();
                    drawable = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.pause_to_play);
                } else {
                    mediaPlayerService.resumePlayback();
                    drawable = ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.play_to_pause);
                }
            }
        } else {

            ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.pause_to_play);
            if (currentlyPlaying != null) {
                currentlyPlaying.setPlaying(false);
                mAdapter.notifyItemChanged(contactCallsFiltered.indexOf(currentlyPlaying));
            }
            currentlyPlaying = item;
            currentlyPlaying.setPlaying(true);

            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            }

            mediaPlayerService.startPlayback(item);
            mediaPlayerService.setOnCompletionListener(() -> {
                //finish(); // finish current activity
                binding.bottomPlayerLayout.playerProgressBar.setProgressCompat(100, true);
                stopUpdater();
                mAdapter.notifyItemChanged(contactCallsFiltered.indexOf(currentlyPlaying));
            });
        }
        playButton.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
    }

    public void animateSwipeUpView(ViewGroup v) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
            v.setTranslationY(v.getHeight());
            v.animate()
                    .translationY(0)
                    .setDuration(250)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();
            TransitionManager.beginDelayedTransition((ViewGroup) v.getParent(), new Slide(Gravity.TOP));
        }
    }

    public void startUpdater() {
        playPauseButtonIcon(true);
        if (mUpdateProgress == null) {
            mUpdateProgress = new Handler();
        }
        mUpdateProgressTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerService!=null && mediaPlayerService.isPlaying() && binding != null) {
                    long currentMillis = mediaPlayerService.getCurrentPosition();
                    long durationMillis = mediaPlayerService.getDuration();
                    int progress = (int) (currentMillis * binding.bottomPlayerLayout.playerProgressBar.getMax() / durationMillis);
                    binding.bottomPlayerLayout.playerProgressBar.setIndeterminate(false);
                    binding.bottomPlayerLayout.playerProgressBar.setProgressCompat(progress, true);
                    mUpdateProgress.postDelayed(this, 50);
                } else {
                    stopUpdater();
                }
            }
        };
        mUpdateProgress.postDelayed(mUpdateProgressTask, 50);
    }

    public void stopUpdater() {
        if (mUpdateProgress == null) return;
        mUpdateProgress.removeCallbacks(mUpdateProgressTask, null);
        mUpdateProgress = null;
        mUpdateProgressTask = null;
        playPauseButtonIcon(false);
        Log.d("Updater", "playPause false");
    }

    public void playPauseButtonIcon(boolean isPlaying) {
        Drawable drawable;
        if (isPlaying) {
            drawable = ContextCompat.getDrawable(this, R.drawable.play_to_pause);
        } else {
            drawable = ContextCompat.getDrawable(this, R.drawable.pause_to_play);
        }
        if (binding != null) binding.bottomPlayerLayout.playPauseButton.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<JsonFileLoader.TwoListsWrapper> loader) {

    }

    public void setPlayerInfo(CallLogItem currentPlayingItem) {

        if (binding.playerInfoBarContainer.getVisibility() != View.VISIBLE) animateSwipeUpView(binding.playerInfoBarContainer);
        binding.bottomPlayerLayout.setShowSim(PreferenceUtils.showSimPlayer(SimUtils.getNumberOfSimCards(this)));
        binding.bottomPlayerLayout.setShowLabel(PreferenceUtils.showLabelPlayer());
        binding.bottomPlayerLayout.setCallLogItem(currentPlayingItem);
        startUpdater();

        switch (currentPlayingItem.getDirection()) {
            case "in" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_in);
            case "out" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_out);
            case "conference" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_conference);
        }

        binding.bottomPlayerLayout.datePlayer.setText(currentPlayingItem.getFormattedTimestamp(getString(R.string.today), getString(R.string.yesterday)));

        binding.bottomPlayerLayout.durationPlayer.setText(currentPlayingItem.getFormattedDurationPlayer());

        if (currentPlayingItem.getContactIcon()!=null)
            Picasso.get().load(currentPlayingItem.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.bottomPlayerLayout.contactIconPlayer);
        else if (PreferenceUtils.showTiles())
            binding.bottomPlayerLayout.contactIconPlayer.setImageDrawable(currentPlayingItem.getContactDrawable(this));
        else
            binding.bottomPlayerLayout.contactIconPlayer.setImageResource(R.drawable.ic_default_contact);

    }
}
