package it.dhd.bcrmanager.ui.fragments;

import static it.dhd.bcrmanager.MainActivity.getAppContext;
import static it.dhd.bcrmanager.utils.SimUtils.getNumberOfSimCards;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.RangeSlider;
import com.squareup.picasso.Picasso;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.callbacks.SwipeCallback;
import it.dhd.bcrmanager.databinding.FragmentLogBinding;
import it.dhd.bcrmanager.databinding.ItemEntryBinding;
import it.dhd.bcrmanager.databinding.ItemHeaderBinding;
import it.dhd.bcrmanager.drawable.LetterTileDrawable;
import it.dhd.bcrmanager.loaders.JsonFileLoader;
import it.dhd.bcrmanager.objects.Breakpoints;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.observer.ContactObserver;
import it.dhd.bcrmanager.runner.TaskRunner;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.adapters.BreakpointAdapter;
import it.dhd.bcrmanager.ui.dialogs.DeleteDialog;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.VibratorUtils;
import it.dhd.bcrmanager.utils.WrapContentLinearLayoutManager;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import me.zhanghai.android.fastscroll.PopupTextProvider;

public class NewHome extends Fragment implements LoaderManager.LoaderCallbacks<JsonFileLoader.TwoListsWrapper>,
                                                    ContactObserver.DataUpdateListener {

    public static CallLogAdapter callLogAdapter;
    public static ConcatAdapter concatAdapter;
    public static CallLogAdapter callStarredLogAdapter;
    public static boolean filterCall;
    public static String filterCallQuery;
    public static List<Object> yourListOfItems = new ArrayList<>();
    public static List<Object> yourListOfItemsFiltered = new ArrayList<>();
    public static List<Object> yourListOfItemsStarred = new ArrayList<>();
    public static List<Object> yourListOfItemsStarredFiltered = new ArrayList<>();
    public static List<ContactItem> contactList = new ArrayList<>();

    public static CallLogItem currentlyPlayingItem;

    public static Integer nSim = 0;

    private static final int LOADER_ID = 1;
    // Setup the loader

    public static MediaPlayerService mediaPlayerService;
    private boolean isBound = false;

    private static Handler mUpdateProgress;
    private static Runnable mUpdateProgressTask;
    private boolean mDatePicked;
    private boolean mDateEnabled;
    private Date mStartDate, mEndDate;
    private int mFilterDateType;
    private final int FILTER_DATE_TYPE_SINGLE = 0;
    private final int FILTER_DATE_TYPE_RANGE = 1;

    private float mStartDuration;
    private float mEndDuration;

    public static double mMaxDuration;

    private boolean showTiles = true, showHeaders = true;

    Vibrator vibrator;

    private String mDir;

    @ColorInt
    int colorPrimary, colorOnPrimary, backgroundColor, colorOnBackground, surfaceHighestColor;

    private String currentHeader, newHeader;

    private FragmentLogBinding binding;
    public static boolean isRunning = false;
    private ContactObserver contentObserver;
    private LinearLayoutManager mLinearLayout;
    private GridLayoutManager mGridLayout;

    public NewHome() {
        // Required empty public constructor
    }


    public void showFilterDialog() {

        if (binding.filterLayout.getVisibility() != View.VISIBLE) {
            binding.filterLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            binding.filterLayout.setVisibility(View.VISIBLE);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(binding.filterLayout, "translationY", -binding.filterLayout.getHeight(), 0);
            slideUp.setDuration(500);
            slideUp.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    // Set visibility to VISIBLE before the animation starts
                    binding.filterLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
                    binding.filterLayout.setVisibility(View.VISIBLE);
                }
            });
            slideUp.start();
        } else {
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(binding.filterLayout, "translationY", 0, -binding.filterLayout.getHeight());
            slideUp.setDuration(500);
            slideUp.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    // Set visibility to VISIBLE before the animation starts
                    binding.filterLayout.setVisibility(View.INVISIBLE);
                    binding.filterLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0));
                }
            });
            slideUp.start();
        }

    }

    private int getFilteredCount() {
        int count = 0;
        int countNonFiltered = 0;

        // Count Filtered

        for (Object item : yourListOfItemsFiltered) {
            if (item instanceof CallLogItem) {
                count++;
            }
        }
        for (Object item : yourListOfItemsStarredFiltered) {
            if (item instanceof CallLogItem) {
                count++;
            }
        }

        // Count non filtered
        for (Object item : yourListOfItems) {
            if (item instanceof CallLogItem) {
                countNonFiltered++;
            }
        }
        for (Object item : yourListOfItemsStarred) {
            if (item instanceof CallLogItem) {
                countNonFiltered++;
            }
        }

        return countNonFiltered == count ? -1 : count;
    }

    public void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                getAppContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(((MainActivity)requireActivity()).searchView.getWindowToken(), 0);
    }

    public static void animateSwipeUpView(ViewGroup v) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
            // Get the height of the view
            int viewHeight = v.getHeight();

            // Create a translate animation (swipe up)
            TranslateAnimation swipe = new TranslateAnimation(0, 0, viewHeight, 0);
            swipe.setDuration(250); // Set the duration of the animation in milliseconds

            // Apply an interpolator for a smoother effect (optional)
            swipe.setInterpolator(new AccelerateInterpolator());
            // Start the animation
            v.startAnimation(swipe);
            TransitionManager.beginDelayedTransition(v, new Slide(Gravity.TOP));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!LoaderManager.getInstance(this).hasRunningLoaders()) {
            binding.progress.setVisibility(View.GONE);
            binding.chronometer.stop();
        }
        Log.d("NewHome", "YourListsize: " + yourListOfItems.size());
        Log.d("NewHome", "onResume");
        MainActivity.mHideMenu = false;
        ((AppCompatActivity)requireActivity()).supportInvalidateOptionsMenu();
        if (mediaPlayerService != null && mediaPlayerService.isPlaying()) {
            Log.d("NewHome", "onResume: isPlaying");
            animateSwipeUpView(binding.playerInfoBarContainer);
            String fileName = currentlyPlayingItem.getFileName();
            for (Object item : yourListOfItems) {
                if (item instanceof CallLogItem callLogItem) {
                    if (callLogItem.getFileName().equals(fileName)) {
                        currentlyPlayingItem = callLogItem;
                        break;
                    }
                }
            }
            setPlayerInfo(((CallLogItem)yourListOfItems.get(yourListOfItems.indexOf(currentlyPlayingItem))));
            startUpdater();
            currentlyPlayingItem.setPlaying(true);
            callLogAdapter.notifyItemChanged(yourListOfItems.indexOf(currentlyPlayingItem));
            if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem)) callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
        }
        checkLoader();
        if (showTiles != PreferenceUtils.showTiles()) {
            showTiles = PreferenceUtils.showTiles();
            callLogAdapter.notifyItemRangeChanged(0, yourListOfItemsFiltered.size());
            callStarredLogAdapter.notifyItemRangeChanged(0, yourListOfItemsStarredFiltered.size());
        }
    }

    private void checkLoader() {
        if (PreferenceUtils.getPermissionReadContactsLastTime() != PermissionsUtil.hasReadContactsPermissions() ||
                PreferenceUtils.getPermissionReadCallLogLastTime() != PreferenceUtils.getPermissionReadCallLogLastTime()) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(requireContext());
            restartLoader(true);
        }
        if (!mDir.equals(PreferenceUtils.getStoredFolderFromPreference())) {
            Log.d("NewHome", "onResume: mDir changed");
            if (yourListOfItems.size()>0) {
                callLogAdapter.notifyItemRangeRemoved(0, yourListOfItems.size());
                yourListOfItems.clear();
            }
            if (yourListOfItemsStarred.size()>0) {
                callStarredLogAdapter.notifyItemRangeRemoved(0, yourListOfItemsStarred.size());
                yourListOfItemsStarred.clear();
            }
            if (contactList.size()>0) contactList.clear();
            restartLoader(true);
            PreferenceUtils.resetStarred();
            mediaPlayerService.stopPlayback();
            currentlyPlayingItem = null;
            mDir = PreferenceUtils.getStoredFolderFromPreference();
            return;
        }
        if (FileUtils.getLastModifiedFolder(requireContext()) != PreferenceUtils.getLastTime()) {
            Log.d("NewHome", "onResume: lastModified changed");
            restartLoader(false);
            return;
        }
        if (showHeaders != PreferenceUtils.showHeaders()) {
            showHeaders = PreferenceUtils.showHeaders();
            editHeaders();
        }
    }

    private void editHeaders() {
        for (Object item : yourListOfItemsFiltered) {
            if (item instanceof DateHeader) {
                callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(item));
            }
        }
    }

    private void restartLoader(boolean shouldRemoveFiles) {
        if (shouldRemoveFiles) FileUtils.deleteCachedFiles(requireContext());
        if (!LoaderManager.getInstance(this).hasRunningLoaders())
            LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
        else {
            LoaderManager.getInstance(this).destroyLoader(LOADER_ID);
            LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceUtils.getAppContext() == null) PreferenceUtils.init(requireContext());
        showTiles = PreferenceUtils.showTiles();
        showHeaders = PreferenceUtils.showHeaders();
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        ((MainActivity)requireActivity()).setupBadge(-1);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireContext().getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        colorOnPrimary = typedValue.data;
        theme.resolveAttribute(com.google.android.material.R.attr.backgroundColor, typedValue, true);
        backgroundColor = typedValue.data;
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
        colorOnBackground = typedValue.data;
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        surfaceHighestColor = typedValue.data;
        if (mDir == null) mDir = PreferenceUtils.getStoredFolderFromPreference();
        Log.d("NewHome", "onCreate: mDir: " + mDir);
        Handler handler = new Handler();
        contentObserver = new ContactObserver(handler, requireContext());
        ContactObserver.registerDataUpdateListener(this);
        Intent intent = new Intent(getContext(), MediaPlayerService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        callLogAdapter = new CallLogAdapter(yourListOfItems); // Create your adapter
        callStarredLogAdapter = new CallLogAdapter(yourListOfItemsStarred);
        concatAdapter = new ConcatAdapter(callStarredLogAdapter, callLogAdapter);



        Log.d("NewHome", "initLoader");
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);

        binding.chronometer.setBase(SystemClock.elapsedRealtime());
        binding.chronometer.start();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.fab.hide();
        binding.fab.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            binding.recyclerView.smoothScrollToPosition(0);
            binding.fab.hide();
        });
        setupFilterLayout();

        nSim = getNumberOfSimCards(requireContext());

        int orientation = getResources().getConfiguration().orientation;
        mLinearLayout = new WrapContentLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        mGridLayout = new GridLayoutManager(requireContext(), 2);
        mGridLayout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (callLogAdapter.getItemViewType(position) == CallLogAdapter.VIEW_TYPE_HEADER)
                    return 2;
                else
                    return 1;
            }
        });
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("NewHome", "onViewCreated: PORTRAIT");
            if (!mLinearLayout.isAttachedToWindow()) binding.recyclerView.setLayoutManager(mLinearLayout);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("NewHome", "onViewCreated: LANDSCAPE");
            if (savedInstanceState == null) binding.recyclerView.setLayoutManager(mGridLayout);
        }

        binding.filterCardLayout.durationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.filterCardLayout.durationLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.filterCardLayout.durationSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (callLogAdapter != null && callStarredLogAdapter != null) {
                callLogAdapter.getFilter().filter(MainActivity.searchQuery);
                callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
            }
        });

        binding.bottomPlayerLayout.playPauseButton.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            assert mediaPlayerService != null;
            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            } else {
                mediaPlayerService.resumePlayback();
                startUpdater();
            }
            callLogAdapter.notifyItemChanged(yourListOfItems.indexOf(currentlyPlayingItem));
            if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem))
                callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
        });
        binding.playerInfoBarContainer.setOnLongClickListener(v -> {
            binding.recyclerView.smoothScrollToPosition(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
            return true;
        });

        binding.playerInfoBarContainer.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            hideSoftKeyboard();
            CallLogItem currentPlayingItem = (CallLogItem) yourListOfItemsFiltered.get(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
            List<Object> allItems = new ArrayList<>();
            allItems.addAll(yourListOfItemsStarredFiltered);
            allItems.addAll(yourListOfItemsFiltered);
            PlayerFragment playerFragment = new PlayerFragment(allItems, currentPlayingItem, mediaPlayerService,
                    this::stopUpdater, this::startUpdater,
                    new PlayerFragment.StarredInterface() {
                        @Override
                        public void setStarred(CallLogItem item) {
                            callStarredLogAdapter.addItem(item);
                            if (yourListOfItemsStarred.size() == 0)
                                yourListOfItemsStarred.add(new DateHeader(getString(R.string.starred)));
                            yourListOfItemsStarred.add(item);
                            callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(item));
                        }

                        @Override
                        public void setUnstarred(CallLogItem item) {
                            callStarredLogAdapter.removeItem(callStarredLogAdapter.callLogItemsFiltered.indexOf(item));
                            yourListOfItemsStarred.remove(item);
                            if (yourListOfItemsStarred.size() == 1) {
                                callStarredLogAdapter.removeItem(0);
                            }
                            callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(item));
                        }
                    },
                    (oldItem, newItem) -> {
                        oldItem.setPlaying(false);
                        callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(oldItem));
                        newItem.setPlaying(true);
                        callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(newItem));
                        callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
                        if (yourListOfItemsStarredFiltered.contains(oldItem))
                            callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(oldItem));
                        if (yourListOfItemsStarredFiltered.contains(newItem))
                            callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(newItem));
                        setPlayerInfo(newItem);
                        currentlyPlayingItem = newItem;
                    },
                    (item) -> {
                        callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(item));
                        if (yourListOfItemsStarredFiltered.contains(item))
                            callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(item));
                    });
            playerFragment.show(
                    getChildFragmentManager(), PlayerFragment.class.getSimpleName()
            );

        });


        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.setItemViewCacheSize(50);
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Show FAB if scrolled beyond the first 20 items
                    if (firstVisibleItemPosition >= 20) {
                        binding.fab.show();
                        if (binding.playerInfoBarContainer.getVisibility() == View.VISIBLE) {
                            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) binding.fab.getLayoutParams();
                            p.setMargins(0, 0, intToDp(), (int) ((binding.playerInfoBarContainer.getHeight() + binding.playerInfoBarContainer.getPaddingBottom())));
                        } else {
                            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) binding.fab.getLayoutParams();
                            p.setMargins(0, 0, intToDp(), intToDp());
                        }
                        binding.fab.requestLayout();
                    } else {
                        binding.fab.hide();
                    }
                }
            }
        });

        PopupTextProvider popupTextProvider;
        if (PreferenceUtils.showHeaders()) popupTextProvider = (view1, position) -> Objects.requireNonNull(getNearestHeader(position, yourListOfItemsFiltered));
        else popupTextProvider = null;

        new FastScrollerBuilder(binding.recyclerView)
                //.setThumbDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.track)))
                .setPadding(0, 0, 10, 0)
                .setPopupTextProvider(popupTextProvider)
                .build();

        binding.filterCardLayout.durationSlider.setLabelFormatter(value -> {
            int minutes = (int) (value / 60);
            int seconds = (int) (value % 60);
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        });

        binding.filterCardLayout.durationSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                List<Float> values = slider.getValues();
                mStartDuration = values.get(0);
                mEndDuration = values.get(1);
                binding.filterCardLayout.durationStartText.setText(DateUtils.formatDuration((long) mStartDuration));
                binding.filterCardLayout.durationEndText.setText(DateUtils.formatDuration((long) mEndDuration));
                callLogAdapter.getFilter().filter(MainActivity.searchQuery);
                callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
            }
        });

        enableSwipeToStar();

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("NewHome", "onViewCreated: PORTRAIT");
            binding.recyclerView.setLayoutManager(mLinearLayout);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("NewHome", "onViewCreated: LANDSCAPE");
            binding.recyclerView.setLayoutManager(mGridLayout);
        }
    }

    private void enableSwipeToStar() {
        SwipeCallback swipeToDeleteCallback = new SwipeCallback(requireContext(), SwipeCallback.TYPE_STAR) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getBindingAdapterPosition();
                CallLogItem item;
                if (viewHolder.getBindingAdapter() == callLogAdapter)
                    item = (CallLogItem) callLogAdapter.callLogItemsFiltered.get(position);
                else
                    item = (CallLogItem) callStarredLogAdapter.callLogItemsFiltered.get(position);

                if (item.isStarred()) {
                    item.setStarred(false);
                    int index = callLogAdapter.callLogItemsFiltered.indexOf(item);
                    yourListOfItemsStarredFiltered.remove(item);
                    callStarredLogAdapter.removeItem(callStarredLogAdapter.callLogItemsFiltered.indexOf(item));
                    callLogAdapter.notifyItemChanged(index);
                    if (callStarredLogAdapter.getItemCount() == 1) {
                        callStarredLogAdapter.removeItem(0);
                    }
                } else {

                    item.setStarred(true);
                    callStarredLogAdapter.addItem(item);
                    yourListOfItemsStarredFiltered.add(item);
                }

                PreferenceUtils.setStarred(item.getFileName(), item.isStarred());

                if (binding.playerInfoBarContainer.getVisibility() == View.VISIBLE) {
                    binding.bottomPlayerLayout.starredIcon.setVisibility(currentlyPlayingItem.isStarred() ? View.VISIBLE : View.GONE);
                }

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(binding.recyclerView);
    }

    public static void deleteCallItem(CallLogItem item) {
        yourListOfItems.remove(item);
        yourListOfItemsFiltered.remove(item);
        callLogAdapter.removeItem(callLogAdapter.callLogItemsFiltered.indexOf(item));
        if (item.isStarred()) {
            yourListOfItemsStarred.remove(item);
            yourListOfItemsStarredFiltered.remove(item);
            callStarredLogAdapter.removeItem(callStarredLogAdapter.callLogItemsFiltered.indexOf(item));
        }
    }

    public static void deleteItemStarred(int position) {
        CallLogItem item = (CallLogItem) yourListOfItemsFiltered.get(position);
        int ps = yourListOfItemsFiltered.indexOf(item);
        yourListOfItemsStarredFiltered.remove(item);
        yourListOfItemsStarred.remove(item);
        callStarredLogAdapter.removeItem(ps);
        callStarredLogAdapter.getFilter().filter(MainActivity.searchQuery);
    }

    public void playPauseButtonIcon(boolean isPlaying) {
        Drawable drawable;
        if (isPlaying) {
            drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.play_to_pause);
        } else {
            drawable = ContextCompat.getDrawable(getAppContext(), R.drawable.pause_to_play);
        }
        if (binding != null) binding.bottomPlayerLayout.playPauseButton.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
    }

    public void enableFilterDate(boolean enabled) {
        binding.filterCardLayout.dateFilterLayoutRadio.setVisibility(enabled ? View.VISIBLE : View.GONE);
        binding.filterCardLayout.radioSingleDate.setEnabled(enabled);
        binding.filterCardLayout.radioRangeDate.setEnabled(enabled);
        binding.filterCardLayout.pickDateButton.setEnabled(enabled);
        mDateEnabled = enabled;
        if (callLogAdapter != null && callLogAdapter.getItemCount()>0)
            callLogAdapter.getFilter().filter(MainActivity.searchQuery);
        if (callStarredLogAdapter != null && callStarredLogAdapter.getItemCount()>0)
            callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
    }

    public void setupFilterLayout() {
        binding.filterCardLayout.dateFilter.setOnCheckedChangeListener((buttonView, isChecked) -> enableFilterDate(isChecked));
        binding.filterCardLayout.pickDateButton.setOnClickListener(v -> {
            if (!(yourListOfItems.size() >0))
                return;
            CallLogItem lastItemLog = (CallLogItem)yourListOfItems.get(yourListOfItems.size()-1);
            // Start date is today
            long startDate = lastItemLog.getTimestampDate();
            // Get the end date (in milliseconds) for today
            long endDate = Calendar.getInstance().getTime().getTime();
            // Set the calendar constraints to restrict the range
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setStart(startDate);
            constraintsBuilder.setEnd(endDate);
            if (binding.filterCardLayout.radioSingleDate.isChecked()) {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                builder.setTitleText(getString(R.string.pick_date));


                // Build and show the date picker
                builder.setCalendarConstraints(constraintsBuilder.build());
                MaterialDatePicker<Long> picker = builder.build();

                picker.addOnPositiveButtonClickListener(date -> {
                    Date dateFilter =new Date(date);
                    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateText = df2.format(dateFilter);
                    binding.filterCardLayout.dateFilterText.setText(dateText);
                    mDatePicked = true;
                    mStartDate = dateFilter;
                    mFilterDateType = FILTER_DATE_TYPE_SINGLE;
                    callLogAdapter.getFilter().filter(MainActivity.searchQuery);
                    callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
                    hideSoftKeyboard();
                    Log.d("NewHome", "date: " + dateText);
                });
                picker.show(getChildFragmentManager(), picker.toString());
            } else {

                MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();

                builder.setSelection(new Pair<>(startDate, endDate));

                builder.setCalendarConstraints(constraintsBuilder.build());

                MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
                picker.addOnPositiveButtonClickListener(dateRange -> {
                    Date dateFilterStart =new Date(dateRange.first);
                    Date dateFilterEnd =new Date(dateRange.second);
                    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateText = df2.format(dateFilterStart) + " - " + df2.format(dateFilterEnd);
                    binding.filterCardLayout.dateFilterText.setText(dateText);
                    mDatePicked = true;
                    mStartDate = dateFilterStart;
                    mEndDate = dateFilterEnd;
                    mFilterDateType = FILTER_DATE_TYPE_RANGE;
                    callLogAdapter.getFilter().filter(MainActivity.searchQuery);
                    callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
                    hideSoftKeyboard();
                    Log.d("NewHome", "date: " + dateText);
                });
                picker.show(getChildFragmentManager(), picker.toString());
            }

        });
        binding.filterCardLayout.directionMenuAutoCompleteTextView.setOnItemClickListener((parent, view1, position, id) -> {
            String mDirection = "";
            switch (position) {
                case 0 -> mDirection = "in";
                case 1 -> mDirection = "out";
                case 2 -> mDirection = "";
            }
            filterCall = !mDirection.isEmpty();
            filterCallQuery = mDirection;
            if (callLogAdapter != null && callStarredLogAdapter != null) {
                callLogAdapter.getFilter().filter(MainActivity.searchQuery);
                callStarredLogAdapter.getFilterStarred().filter(MainActivity.searchQuery);
            }
        });
    }

    public void startUpdater() {
        playPauseButtonIcon(true);
        if (mUpdateProgress == null) {
            mUpdateProgress = new Handler();
        }
        mUpdateProgressTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerService!=null && mediaPlayerService.isPlaying() && binding != null && mUpdateProgress != null) {
                    int currentMillis = mediaPlayerService.getCurrentPosition();
                    int durationMillis = mediaPlayerService.getDuration();
                    int progress = (int) ((long) currentMillis * binding.bottomPlayerLayout.playerProgressBar.getMax() / durationMillis);
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

    @Override
    public void onDestroy() {
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @NonNull
    @Override
    public Loader<JsonFileLoader.TwoListsWrapper> onCreateLoader(int id, @Nullable Bundle args) {
        Log.d("NewHome", "onCreateLoader");
        isRunning = true;
        if (binding!=null) {
            binding.progress.setVisibility(View.VISIBLE);
            binding.chronometer.setBase(SystemClock.elapsedRealtime());
            binding.chronometer.start();
        }

        ((MainActivity)requireActivity()).setupBadge(-1);

        if (yourListOfItems.size()>0) yourListOfItems.clear();
        if (yourListOfItemsFiltered.size()>0) yourListOfItemsFiltered.clear();
        if (yourListOfItemsStarred.size()>0) yourListOfItemsStarred.clear();
        if (yourListOfItemsStarredFiltered.size()>0) yourListOfItemsStarredFiltered.clear();
        if (contactList.size()>0) contactList.clear();
        return new JsonFileLoader(requireContext(),"");
    }

    private int intToDp() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onLoadFinished(@NonNull Loader<JsonFileLoader.TwoListsWrapper> loader, JsonFileLoader.TwoListsWrapper dataWrapper) {
        isRunning = false;
        Log.d("NewHome", "onLoadFinished");
        if (dataWrapper.errorFiles().size() > 0) {
            Log.d("NewHome", "onLoadFinished: errorFiles: " + dataWrapper.errorFiles().size());
            MaterialAlertDialogBuilder builderError = new MaterialAlertDialogBuilder(requireContext());
            builderError.setTitle(getString(R.string.error_files));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
            arrayAdapter.addAll(dataWrapper.errorFiles());

            builderError.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

            builderError.setAdapter(arrayAdapter, (dialog, which) -> {});
            // Hide now, TODO add call log picker for each element here
            //builderError.show();
        }

        yourListOfItems.addAll(dataWrapper.sortedListWithHeaders());
        yourListOfItemsStarred.addAll(dataWrapper.starredItemsList());
        yourListOfItemsFiltered.addAll(yourListOfItems);
        yourListOfItemsStarredFiltered.addAll(yourListOfItemsStarred);
        if (dataWrapper.contactList() != null) {
            contactList.addAll(dataWrapper.contactList());
        }

        mMaxDuration = dataWrapper.maxDuration();

        callLogAdapter.setData(dataWrapper.sortedListWithHeaders());
        callStarredLogAdapter.setData(dataWrapper.starredItemsList());
        callLogAdapter.notifyDataSetChanged(); // Notify the adapter that the data set has changed
        callStarredLogAdapter.notifyDataSetChanged();

        binding.filterCardLayout.durationSlider.setValueFrom(0.0f);
        binding.filterCardLayout.durationSlider.setValueTo((float) mMaxDuration);
        binding.filterCardLayout.durationSlider.setValues((float) 0.0, (float) mMaxDuration);
        mStartDuration = 0.0f;
        mEndDuration = (float) mMaxDuration;
        binding.filterCardLayout.durationStartText.setText(DateUtils.formatDuration((long) 0));
        binding.filterCardLayout.durationEndText.setText(DateUtils.formatDuration((long) mEndDuration));

        if (currentlyPlayingItem != null) {
            if (mediaPlayerService != null) {
                String fileName = currentlyPlayingItem.getFileName();
                for (Object item : yourListOfItemsFiltered) {
                    if (!(item instanceof CallLogItem)) continue;
                    if (((CallLogItem) item).getFileName().equals(fileName)) {
                        currentlyPlayingItem = (CallLogItem) item;
                        break;
                    }
                }
                currentlyPlayingItem.setPlaying(true);
                callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
                if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem))
                    callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
            }
        }

        binding.progress.setVisibility(View.GONE);
        binding.chronometer.stop();

        ((MainActivity)requireActivity()).setupBadge(-1);


    }

    private String getNearestHeader(int position, List<Object> sortedListWithHeaders) {
        if (position < 0 || position >= sortedListWithHeaders.size()) {
            return "";
        }

        // Find the nearest header before or at the given position
        for (int i = position; i >= 0; i--) {
            Object item = sortedListWithHeaders.get(i);
            if (item instanceof DateHeader) {
                if (!Objects.equals(currentHeader, newHeader)) {
                    newHeader = ((DateHeader) item).date();
                    VibratorUtils.vibrate(requireContext(), 50);
                }
                currentHeader = ((DateHeader) item).date();
                return ((DateHeader) item).date();
            }
        }

        // Find the nearest header after the given position
        for (int i = position + 1; i < sortedListWithHeaders.size(); i++) {
            Object item = sortedListWithHeaders.get(i);
            if (item instanceof DateHeader) {
                if (!Objects.equals(currentHeader, newHeader)) {
                    newHeader = ((DateHeader) item).date();
                    VibratorUtils.vibrate(requireContext(), 50);
                }
                currentHeader = ((DateHeader) item).date();
                return ((DateHeader) item).date();
            }
        }

        return "";
    }

    @Override
    public void onLoaderReset(@NonNull Loader<JsonFileLoader.TwoListsWrapper> loader) {
        Log.d("NewHome", "onLoaderReset");
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

    public void setPlayerInfo(CallLogItem item) {
        if (binding == null) return;
        item.setPlaying(true);
        if (item.getContactName()!=null) binding.bottomPlayerLayout.contactNamePlayer.setText(item.getContactName());
        else binding.bottomPlayerLayout.contactNamePlayer.setText(item.getNumberFormatted());
        if (nSim<=1) {
            binding.bottomPlayerLayout.dividerSimPlayer.setVisibility(View.GONE);
            binding.bottomPlayerLayout.simSlotPlayer.setVisibility(View.GONE);
            binding.bottomPlayerLayout.dividerDatePlayer.setVisibility(View.GONE);
        } else binding.bottomPlayerLayout.simSlotPlayer.setText(item.getSimSlot());

        switch (item.getDirection()) {
            case "in" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_in);
            case "out" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_out);
            case "conference" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_conference);
        }

        binding.bottomPlayerLayout.datePlayer.setText(item.getFormattedTimestamp(getAppContext().getString(R.string.today), getAppContext().getString(R.string.yesterday)));

        if (item.getDuration() == 0) binding.bottomPlayerLayout.durationPlayer.setVisibility(View.GONE);
        else binding.bottomPlayerLayout.durationPlayer.setText(item.getFormattedDurationPlayer());


        if (item.getContactIcon()!=null)
            Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.bottomPlayerLayout.contactIconPlayer);
        else if (showTiles)
            binding.bottomPlayerLayout.contactIconPlayer.setImageDrawable(item.getContactDrawable(requireContext()));
        else
            binding.bottomPlayerLayout.contactIconPlayer.setImageResource(R.drawable.ic_default_contact);

        if (item.isStarred()) {
            binding.bottomPlayerLayout.starredIcon.setVisibility(View.VISIBLE);
        } else {
            binding.bottomPlayerLayout.starredIcon.setVisibility(View.GONE);
        }


    }

    @Override
    public void onDataUpdate(@NonNull String newData, List<ContactItem> newContacts) {
        if (!newData.isEmpty() && newContacts != null) {
            binding.progress.setVisibility(View.VISIBLE);
            TaskRunner taskRunner = new TaskRunner();
            taskRunner.executeAsync(() -> {
                        contactList.clear();
                        contactList.addAll(newContacts);
                        String[] changedContacts = newData.split(";");
                        for (String changedContact : changedContacts) {
                            String[] contactInfo = changedContact.split(":");
                            if (contactInfo[0].equals("N")) {
                                for (Object item : yourListOfItems) {
                                    if (item instanceof DateHeader) continue;
                                    CallLogItem callLogItem = (CallLogItem) item;
                                    if (!PhoneNumberUtils.compare(callLogItem.getNumber(), contactInfo[1])) continue;
                                    ContactItem contact = getContactItemFromList(contactInfo[1]);
                                    if (contact == null) {
                                        callLogItem.setContactName(callLogItem.getNumberFormatted());
                                        callLogItem.setContactIcon(null);
                                        callLogItem.setContactSaved(false);
                                        callLogItem.setLookupKey(null);
                                        callLogItem.setContactType(LetterTileDrawable.TYPE_GENERIC_AVATAR);
                                    } else {
                                        callLogItem.setContactName(contact.getContactName());
                                        callLogItem.setContactIcon(contact.getContactImage());
                                        callLogItem.setLookupKey(contact.getLookupKey());
                                        callLogItem.setContactSaved(contact.isContactSaved());
                                        callLogItem.setContactType(contact.getContactType());
                                    }
                                }
                            }
                        }
                        FileUtils.saveObjectList(requireContext(), contactList, FileUtils.STORED_CONTACTS, ContactItem.class);
                        List<CallLogItem> itemHolder = yourListOfItems.stream()
                                .filter(CallLogItem.class::isInstance) // Filter only CallLogItem
                                .map(CallLogItem.class::cast) // Converts Object to CallLogItem
                                .collect(Collectors.toList()); // Collects in a new list of CallLogItem
                        FileUtils.saveObjectList(requireContext(), itemHolder, FileUtils.STORED_REG, CallLogItem.class);
                        return null;
                    },
                    result -> {
                        binding.progress.setVisibility(View.GONE);
                        callLogAdapter.notifyItemRangeChanged(0, yourListOfItems.size());
                        callStarredLogAdapter.notifyItemRangeChanged(0, yourListOfItemsStarred.size());
                    });
        }
        requireContext().getContentResolver().unregisterContentObserver(contentObserver);
    }

    private ContactItem getContactItemFromList(String phoneNumber) {
        for (ContactItem contactItem : contactList) {
            if (PhoneNumberUtils.compare(contactItem.getPhoneNumber(), phoneNumber)) {
                Log.d("NewHome", "getContactItemFromList: " + contactItem.getContactName());
                return contactItem;
            }
        }
        return null;
    }

    public class CallLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        public final List<Object> callLogItems;
        public final List<Object> callLogItemsFiltered;
        public static List<Object> items;
        private int expandedPosition = -1;

        public CallLogAdapter(List<Object> callLogItems) {
            this.callLogItems = callLogItems;
            this.callLogItemsFiltered = new ArrayList<>(callLogItems);
            items = new ArrayList<>(callLogItems);
        }

        public static int getSize() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (callLogItemsFiltered.get(position) instanceof DateHeader) {
                return VIEW_TYPE_HEADER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

        private void pausePlayback() {
            if (mediaPlayerService != null && mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            }
        }

        private void resumePlayback() {
            if (mediaPlayerService != null) {
                mediaPlayerService.resumePlayback();
                startUpdater(); // Update playback time views when resuming playback
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            ItemEntryBinding bindingItem = ItemEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            ItemHeaderBinding bindingHeader = ItemHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            final RecyclerView.ViewHolder result;
            if (viewType == VIEW_TYPE_HEADER) {
                result = new HeaderViewHolder(bindingHeader);
            } else {
                result = new CallLogViewHolder(bindingItem);
                bindingItem.rootLayout.setOnClickListener(v -> toggleExpansion(result.getBindingAdapterPosition()));

                bindingItem.callLogEntryActions.callAction.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber()));
                    startActivity(intent);
                });

                bindingItem.callLogEntryActions.createNewContactAction.setOnClickListener(v -> {
                    contentObserver.setContactNumber(((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber());
                    //requireContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
                    Intent intent1 = new Intent(Intent.ACTION_INSERT);
                    intent1.setType(ContactsContract.Contacts.CONTENT_TYPE);
                    intent1.putExtra(ContactsContract.Intents.Insert.PHONE, ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber());
                    startActivity(intent1);
                });

                bindingItem.callLogEntryActions.editContactAction.setOnClickListener(v -> {
                    CallLogItem clickedItem = ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition()));
                    contentObserver.setContactNumber(clickedItem.getNumber());
                    //requireContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "/" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getLookupKey());
                    intent.setData(contactUri);
                    startActivity(intent);

                });

                bindingItem.callLogEntryActions.sendMessageAction.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("sms:" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber()));
                    startActivity(intent);
                });

                bindingItem.actionPlay.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);

                    bindingItem.actionPlay.setIconTint(ColorStateList.valueOf(colorPrimary));
                    Drawable drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.play_to_pause);

                    CallLogItem clickedItem;
                    CallLogItem currentPlayingHolder = null;
                    if (result.getBindingAdapter() == callLogAdapter) {
                        clickedItem = (CallLogItem) callLogAdapter.callLogItemsFiltered.get(result.getBindingAdapterPosition());
                    } else {
                        clickedItem = (CallLogItem) callStarredLogAdapter.callLogItemsFiltered.get(result.getBindingAdapterPosition());
                    }

                    if (currentlyPlayingItem == clickedItem) {
                        // If it's the same item that is currently playing, pause or resume playback
                        if (mediaPlayerService != null && mediaPlayerService.isPlaying()) {
                            pausePlayback();
                            drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.pause_to_play);
                        } else {
                            resumePlayback();
                            drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.play_to_pause);
                        }
                    } else {

                        ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.pause_to_play);
                        if (currentlyPlayingItem != null) {
                            currentlyPlayingItem.setPlaying(false);
                            callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
                            if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem))
                                callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
                        }
                        currentPlayingHolder = currentlyPlayingItem;
                        currentlyPlayingItem = clickedItem;
                        currentlyPlayingItem.setPlaying(true);

                        if (mediaPlayerService.isPlaying()) {
                            mediaPlayerService.pausePlayback();
                            stopUpdater();
                        }

                        mediaPlayerService.startPlayback(getContext(), Uri.parse(clickedItem.getAudioFilePath()));
                        mediaPlayerService.setOnCompletionListener(mp -> {
                            //finish(); // finish current activity
                            callLogAdapter.notifyItemChanged(yourListOfItemsFiltered.indexOf(currentlyPlayingItem));
                            if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem))
                                callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
                            binding.bottomPlayerLayout.playerProgressBar.setProgressCompat(100, true);
                            stopUpdater();
                        });
                        animateSwipeUpView(binding.playerInfoBarContainer);

                        startUpdater();
                        setPlayerInfo(clickedItem);
                        setupFab();
                        //updatePlayerStatus();
                    }
                    bindingItem.actionPlay.setIcon(drawable);
                    if (drawable instanceof AnimatedVectorDrawable) {
                        ((AnimatedVectorDrawable) drawable).start();
                    }
                    if (currentPlayingHolder != null) {
                        callLogAdapter.notifyItemChanged(yourListOfItems.indexOf(currentPlayingHolder));
                    }
                    callLogAdapter.notifyItemChanged(yourListOfItems.indexOf(currentlyPlayingItem));
                    if (yourListOfItemsStarredFiltered.contains(currentlyPlayingItem))
                        callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentlyPlayingItem));
                    if (yourListOfItemsStarredFiltered.contains(currentPlayingHolder))
                        callStarredLogAdapter.notifyItemChanged(yourListOfItemsStarredFiltered.indexOf(currentPlayingHolder));

                });

                bindingItem.callLogEntryActions.openWithAction.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);
                    int adapterPosition = result.getBindingAdapterPosition();
                    CallLogItem currentPlayingItem = ((CallLogItem)callLogItemsFiltered.get(adapterPosition));
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(currentPlayingItem.getAudioFilePath()), "audio/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                });

                bindingItem.callLogEntryActions.shareAction.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);
                    int adapterPosition = result.getBindingAdapterPosition();
                    CallLogItem currentPlayingItem = ((CallLogItem)callLogItemsFiltered.get(adapterPosition));
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("audio/*");
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPlayingItem.getAudioFilePath()));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Share audio"));
                });

                bindingItem.callLogEntryActions.deleteAction.setOnClickListener(v -> {
                    VibratorUtils.vibrate(requireContext(), 25);
                    if (mediaPlayerService.isPlaying()) {
                        mediaPlayerService.stopPlayback();
                        stopUpdater();
                    }
                    CallLogItem itemToDelete;
                    if (result.getBindingAdapter() == callStarredLogAdapter) {
                        itemToDelete = (CallLogItem) callStarredLogAdapter.callLogItemsFiltered.get(result.getBindingAdapterPosition());
                    } else {
                        itemToDelete = (CallLogItem) callLogAdapter.callLogItemsFiltered.get(result.getBindingAdapterPosition());
                    }
                    FragmentManager fm = requireActivity().getSupportFragmentManager();
                    DeleteDialog custom = new DeleteDialog(itemToDelete, () -> {
                        if (currentlyPlayingItem == itemToDelete) {
                            currentlyPlayingItem = null;
                            binding.playerInfoBarContainer.setVisibility(View.GONE);
                        }
                        deleteCallItem(itemToDelete);
                        List<CallLogItem> itemHolder = yourListOfItems.stream()
                                .filter(CallLogItem.class::isInstance) // Filter only CallLogItem
                                .map(CallLogItem.class::cast) // Converts Object to CallLogItem
                                .collect(Collectors.toList()); // Collects in a new list of CallLogItem

                        FileUtils.saveObjectList(requireContext(), itemHolder, FileUtils.STORED_REG, CallLogItem.class);
                    });
                    Bundle args = new Bundle();
                    args.putString("title", getString(R.string.delete_dialog_title));
                    args.putString("message", getString(R.string.delete_dialog_message));
                    custom.setArguments(args);
                    custom.show(fm,"");
                });
            }
            return result;
        }

        private void setupFab() {
            if (binding.fab.isShown()) {
                if (binding.playerInfoBarContainer.getVisibility() == View.VISIBLE) {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) binding.fab.getLayoutParams();
                    p.setMargins(0, 0, intToDp(), (int) (binding.playerInfoBarContainer.getHeight()*1.1));
                    binding.fab.requestLayout();
                } else {
                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) binding.fab.getLayoutParams();
                    p.setMargins(0, 0, intToDp(), intToDp());
                    binding.fab.requestLayout();
                }
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == VIEW_TYPE_ITEM) {
                CallLogItem callLogItem = ((CallLogItem) callLogItemsFiltered.get(position));
                ((CallLogViewHolder) holder).bind(callLogItem, position);
                ((CallLogViewHolder) holder).binding.setCallLogItem(callLogItem);
            } else if (getItemViewType(position) == VIEW_TYPE_HEADER) {
                DateHeader dateHeader = ((DateHeader)callLogItemsFiltered.get(position));
                ((HeaderViewHolder) holder).bind(dateHeader);
            }
        }

        @Override
        public int getItemCount() {
            return callLogItemsFiltered.size();
        }

        private void toggleExpansion(int position) {
            VibratorUtils.vibrate(requireContext(), 25);
            if (position != expandedPosition) {
                // Expand the clicked item
                int oldExpandedPosition = expandedPosition;
                expandedPosition = position;
                notifyItemChanged(oldExpandedPosition); // Collapse the previously expanded item
                notifyItemChanged(expandedPosition); // Expand the clicked item
            } else {
                // Collapse the clicked item
                expandedPosition = -1;
                notifyItemChanged(position);
            }

        }

        public String removeDiacriticalMarks(String string) {
            return Normalizer.normalize(string, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }

        // Helper function to check if the date falls within the specified range
        private boolean isDateInRange(Date itemDate) {
            if (mDatePicked && mDateEnabled) {
                if (mFilterDateType == FILTER_DATE_TYPE_SINGLE) {
                    // Single date mode
                    return DateUtils.isSameDay(itemDate, mStartDate);
                } else if (mFilterDateType == FILTER_DATE_TYPE_RANGE) {
                    // Date range mode
                    return !itemDate.before(mStartDate) && (!itemDate.after(mEndDate) || DateUtils.isSameDay(itemDate, mEndDate));
                }
            }
            // If no date filtering is applied, return true
            return true;
        }

        private boolean isDurationInRange(double duration) {
            if (binding.filterCardLayout.durationCheckbox.isChecked()) {
                return duration >= (double) mStartDuration && duration <= (double) mEndDuration;
            }
            return true;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {

                    List<Object> filteredList = new ArrayList<>();
                    List<CallLogItem> filteredCallLogItems = new ArrayList<>();
                    String query = charSequence != null ? charSequence.toString().toLowerCase() : "";

                    Date currentDate = Date.from(Calendar.getInstance().getTime().toInstant());
                    Date lastAddedHeaderDate = null;
                    boolean addedMonthHeader = false;
                    String monthHeader = "";

                    for (Object item : callLogItems) {
                        if (item instanceof CallLogItem callLogItem) {
                            if (isDateInRange(new Date(callLogItem.getTimestampDate()))) {
                                if (isDurationInRange((float) ((CallLogItem) item).getDuration())) {
                                    if (isDirectionInList(callLogItem.getDirection())) {
                                        if (!TextUtils.isEmpty(callLogItem.getContactName()) && removeDiacriticalMarks(callLogItem.getContactName().toLowerCase()).contains(removeDiacriticalMarks(query)))
                                            filteredCallLogItems.add(callLogItem);
                                    }
                                }
                            }
                        }
                    }

                    // Add headers to the filtered list
                    if (showHeaders) {
                        for (CallLogItem callLogItem : filteredCallLogItems) {
                            // Check if the date has changed for headers
                            Date itemDate = new Date(callLogItem.getTimestampDate());

                            if (!DateUtils.isSameDay(lastAddedHeaderDate, itemDate)) {
                                // Add a new header if the date is different from the last added header
                                if (DateUtils.isSameDay(currentDate, itemDate)) {
                                    // If the date is today, show 'Today'
                                    filteredList.add(new DateHeader(requireContext().getString(R.string.today)));
                                } else if (DateUtils.isYesterday(currentDate, itemDate)) {
                                    // If the date is yesterday, show 'Yesterday'
                                    filteredList.add(new DateHeader(requireContext().getString(R.string.yesterday)));
                                } else if (DateUtils.isLastWeek(currentDate, itemDate)) {
                                    // If the date is in last week, show the day (e.g., Wednesday)
                                    String dayOfWeek = DateUtils.getDayOfWeek(itemDate);
                                    filteredList.add(new DateHeader(dayOfWeek));
                                } else if (!addedMonthHeader && DateUtils.isLastMonth(currentDate, itemDate)) {
                                    // If the date is in last month, show 'Last month'
                                    filteredList.add(new DateHeader(requireContext().getString(R.string.last_month)));
                                    monthHeader = DateUtils.getMonth(itemDate);
                                    addedMonthHeader = true;
                                } else {
                                    // For other items, group by month
                                    String month = DateUtils.getMonth(itemDate);
                                    if (!Objects.equals(month, monthHeader) && showHeaders) {
                                        monthHeader = month;
                                        filteredList.add(new DateHeader(month));
                                    }
                                }

                                // Update the last added header date
                                lastAddedHeaderDate = itemDate;
                            }

                            // Add the regular item
                            filteredList.add(callLogItem);
                        }
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = filteredList;
                    return filterResults;

                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    yourListOfItemsFiltered.clear();
                    yourListOfItemsFiltered.addAll((List<?>) filterResults.values);
                    ((MainActivity)requireActivity()).setupBadge(getFilteredCount());
                    callLogItemsFiltered.clear();
                    callLogItemsFiltered.addAll((List<?>) filterResults.values);
                    notifyDataSetChanged();
                }
            };
        }

        private boolean isDirectionInList(String direction) {
            if (filterCall) {
                if (filterCallQuery.isEmpty()) return true;
                else return direction.equals(filterCallQuery);
            } else {
                return true;
            }
        }

        public void setData(List<Object> newData) {
            callLogItems.clear();
            callLogItems.addAll(newData);
            callLogItemsFiltered.clear();
            callLogItemsFiltered.addAll(newData);
        }

        public void removeItem(int position) {
            expandedPosition = -1;
            Object item = callLogItemsFiltered.get(position);
            if (item instanceof CallLogItem) {
                callLogItemsFiltered.remove(item);
                callLogItems.remove(item);
            } else {
                callLogItemsFiltered.remove(position);
                callLogItems.remove(position);
            }
            notifyItemRemoved(position);
        }

        public void addItem(Object item) {
            if (getItemCount() == 0) {
                callLogItems.add(new DateHeader(requireContext().getString(R.string.starred)));
                callLogItemsFiltered.add(new DateHeader(requireContext().getString(R.string.starred)));
            }
            callLogItems.add(item);
            callLogItemsFiltered.add(item);
            notifyDataSetChanged();
        }


        public Filter getFilterStarred() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {

                    List<Object> filteredList = new ArrayList<>();
                    List<CallLogItem> filteredCallLogItems = new ArrayList<>();
                    String query = charSequence != null ? charSequence.toString().toLowerCase() : "";


                    for (Object item : callLogItems) {
                        if (item instanceof ContactItem) continue;

                        if (item instanceof CallLogItem callLogItem) {
                            if (isDateInRange(new Date(callLogItem.getTimestampDate()))) {
                                if (isDurationInRange(((CallLogItem) item).getDuration())) {
                                    if (isDirectionInList(callLogItem.getDirection())) {
                                        if (!TextUtils.isEmpty(callLogItem.getContactName()) && removeDiacriticalMarks(callLogItem.getContactName().toLowerCase()).contains(removeDiacriticalMarks(query)))
                                            filteredCallLogItems.add(callLogItem);
                                    }
                                }
                            }
                        }
                    }

                    if (filteredCallLogItems.size() > 0) {
                        filteredList.add(new DateHeader(requireContext().getString(R.string.starred)));
                    }
                    filteredList.addAll(filteredCallLogItems);

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = filteredList;
                    filterResults.count = filteredList.size(); // Set the count to the number of items

                    return filterResults;

                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    yourListOfItemsStarredFiltered.clear();
                    yourListOfItemsStarredFiltered.addAll((List<?>) filterResults.values);
                    ((MainActivity)requireActivity()).setupBadge(getFilteredCount());
                    callLogItemsFiltered.clear();
                    callLogItemsFiltered.addAll((List<?>) filterResults.values);
                    notifyDataSetChanged();
                }
            };
        }

        public class CallLogViewHolder extends RecyclerView.ViewHolder {

            private final ItemEntryBinding binding;

            CallLogViewHolder(@NonNull ItemEntryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.setShowSim(nSim == 2);
            }

            @SuppressLint("SetTextI18n")
            public void bind(CallLogItem item, int position) {

                // Set card stuff
                if (position == expandedPosition) {
                    binding.rootLayout.setCardBackgroundColor(surfaceHighestColor);
                } else {
                    binding.rootLayout.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.trans));
                }

                binding.expandingLayout.setVisibility(position == expandedPosition ? View.VISIBLE : View.GONE);

                // Set Contact Name
                binding.contactName.setSelected(true);

                switch (item.getDirection()) {
                    case "in" -> binding.callIcon.setImageResource(R.drawable.ic_in);
                    case "out" -> binding.callIcon.setImageResource(R.drawable.ic_out);
                    case "conference" -> binding.callIcon.setImageResource(R.drawable.ic_conference);
                }

                if (showHeaders)
                    binding.date.setText(item.getTimeStamp());
                else
                    binding.date.setText(item.getFormattedTimestamp(getAppContext().getString(R.string.today), getAppContext().getString(R.string.yesterday)));

                Log.d("NewHome", "bind: " + item.getContactName() + " | NUMBER " + item.getNumberType());

                switch (item.getNumberType()) {
                    case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> binding.numberIcon.setImageResource(R.drawable.ic_call);
                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> binding.numberIcon.setImageResource(R.drawable.ic_home);
                }

                binding.starredIcon.setVisibility(item.isStarred() ? View.VISIBLE : View.GONE);

                if (item.isPlaying())
                    binding.actionPlay.setIconTint(ColorStateList.valueOf(colorPrimary));
                else
                    binding.actionPlay.setIconTint(ColorStateList.valueOf(colorOnBackground));

                Drawable drawable;
                if (mediaPlayerService != null && mediaPlayerService.isPlaying() && item.isPlaying()) {
                    drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.ic_pause);
                } else {
                    drawable = ContextCompat.getDrawable(getAppContext().getApplicationContext(), R.drawable.ic_play);
                }

                binding.actionPlay.setIcon(drawable);

                binding.contactIcon.setImageDrawable(null);
                Picasso.get().cancelRequest(binding.contactIcon);
                if (item.getContactIcon()!=null)
                    Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.contactIcon);
                else if (showTiles)
                    binding.contactIcon.setImageDrawable(item.getContactDrawable(requireContext()));
                else
                    binding.contactIcon.setImageResource(R.drawable.ic_default_contact);

                binding.callLogEntryActions.createNewContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && !item.isContactSaved() ? View.VISIBLE : View.GONE);
                binding.callLogEntryActions.editContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && item.isContactSaved() ? View.VISIBLE : View.GONE);
                binding.contactIcon.setOnClickListener(v -> {
                    expandedPosition = -1;
                    MainActivity.searchItem.expandActionView();
                    ((MainActivity) requireActivity()).searchView.setQuery(item.getContactName(), true);
                });

                String prefName = item.getFileName();
                String PREF_KEY_BREAKPOINTS = prefName + "_breakpoints";
                // Get a list of all breakpoints
                List<Breakpoints> breakpointsList = BreakpointUtils.loadListBreakpoints(PREF_KEY_BREAKPOINTS);
                // Iterate through the breakpoints and do something with them
                if (breakpointsList.size() > 0 && mediaPlayerService.isPlaying() && currentlyPlayingItem == item) {
                        binding.breakpointsDiv.setVisibility(View.VISIBLE);
                        binding.breakpointsRecycler.setVisibility(View.VISIBLE);
                        BreakpointAdapter breakpointAdapter = new BreakpointAdapter(breakpointsList,
                                (view1, breakPos, breakpoint) -> mediaPlayerService.seekTo(breakpoint.getTime()),
                                (view1, breakPos, breakpoint) -> {});
                        binding.breakpointsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                        binding.breakpointsRecycler.setAdapter(breakpointAdapter);
                } else {
                    binding.breakpointsDiv.setVisibility(View.GONE);
                    binding.breakpointsRecycler.setVisibility(View.GONE);
                }
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {

            private final ItemHeaderBinding binding;

            HeaderViewHolder(@NonNull ItemHeaderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                //binding.rootLayout.setOnClickListener(v -> toggleExpansion(getBindingAdapterPosition()));
            }

            public void bind(DateHeader dateHeader) {
                if (!showHeaders) {
                    binding.headerText.setVisibility(View.GONE);
                    binding.headerText.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                } else binding.headerText.setText(dateHeader.date());
            }
        }
    }

}