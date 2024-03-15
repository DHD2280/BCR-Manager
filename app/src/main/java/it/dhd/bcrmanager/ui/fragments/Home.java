package it.dhd.bcrmanager.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
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

import it.dhd.bcrmanager.MainActivity;
import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.callbacks.SwipeCallback;
import it.dhd.bcrmanager.databinding.FragmentLogBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.observer.ContactObserver;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.activity.ErrorReportActivity;
import it.dhd.bcrmanager.ui.adapters.CallLogAdapter;
import it.dhd.bcrmanager.ui.dialogs.DeleteDialog;
import it.dhd.bcrmanager.ui.fragments.player.PlayerFragment;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;
import it.dhd.bcrmanager.utils.VibratorUtils;
import it.dhd.bcrmanager.utils.WrapContentLinearLayoutManager;
import it.dhd.bcrmanager.viewmodel.FileViewModel;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import me.zhanghai.android.fastscroll.PopupTextProvider;

public class Home extends Fragment implements ContactObserver.DataUpdateListener {

    private FragmentLogBinding binding;
    private FileViewModel fileModel;
    private MediaPlayerService mediaPlayerService;
    private boolean isBound = false;
    private CallLogItem currentlyPlaying;
    private CallLogAdapter mRegAdapter, mStarredAdapter;
    private ConcatAdapter concatAdapter;
    private List<Object> sortedList;
    private List<Object> sortedListFiltered;
    private List<Object> starredList;
    private List<Object> starredListFiltered;
    private List<Object> allList;

    // Menu Items
    private SearchView searchView;
    private TextView textFilterItems;
    private MenuItem searchItem;
    public CharSequence searchQuery;

    // Progress Updater
    private static Handler mUpdateProgress;
    private static Runnable mUpdateProgressTask;

    // Contact Observer
    private ContactObserver contentObserver;

    // Layout Managers
    private LinearLayoutManager mLinearLayout;
    private GridLayoutManager mGridLayout;

    private int nSim = 0;

    // Filter stuff
    private int mFilterDateType;
    public static final int FILTER_DATE_TYPE_SINGLE = 0;
    public static final int FILTER_DATE_TYPE_RANGE = 1;
    private boolean mDateEnabled, mDatePicked;
    private float mStartDuration, mEndDuration;
    private Date mStartDate, mEndDate;
    private String filterDirectionQuery;
    // Fast Scroll Stuff
    private String currentHeader, newHeader;


    public Home() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init ViewModel
        fileModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
        fileModel.fetchData(requireContext(), null);

        // Init Lists
        sortedList = new ArrayList<>();
        sortedListFiltered = new ArrayList<>();
        starredList = new ArrayList<>();
        starredListFiltered = new ArrayList<>();
        allList = new ArrayList<>();
        Intent intent = new Intent(getContext(), MediaPlayerService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        nSim = SimUtils.getNumberOfSimCards(requireContext());

        HandlerThread thread = new HandlerThread("ContentObserverThread");
        thread.start();

        // creates the handler using the passed looper
        Handler handler = new Handler(thread.getLooper());
        contentObserver = new ContactObserver(handler, requireContext());
        ContactObserver.registerDataUpdateListener(this);

        mRegAdapter = new CallLogAdapter(requireContext(),
                sortedListFiltered,
                mediaPlayerService,
                this::onPlay,
                this::onDelete,
                this::onContactClicked,
                this::contactIconClicked,
                this::onTrimAudioClip);
        mStarredAdapter = new CallLogAdapter(requireContext(),
                starredListFiltered,
                mediaPlayerService,
                this::onPlay,
                this::onDelete,
                this::onContactClicked,
                this::contactIconClicked,
                this::onTrimAudioClip);
        concatAdapter = new ConcatAdapter(mStarredAdapter, mRegAdapter);

        // Setup Options Menu
        setHasOptionsMenu(true);
    }

    // Create Options Menu
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        final MenuItem filterItem = menu.findItem(R.id.menu_filter);
        View actionView = filterItem.getActionView();
        assert actionView != null;
        textFilterItems = actionView.findViewById(R.id.cart_badge);
        actionView.setOnClickListener(v -> onOptionsItemSelected(filterItem));
        searchItem = menu.findItem(R.id.menu_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem menuItem) {
                TransitionManager.beginDelayedTransition(requireActivity().findViewById(R.id.toolbar), new Slide(Gravity.END));
                searchQuery = "";
                filter();
                return true;
            }
        });
        searchView = (SearchView) searchItem.getActionView();


        assert searchView != null;
        searchView.setQueryHint(getString(R.string.search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                filter();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                filter();
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    // On Menu Item Click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_search) {
            TransitionManager.beginDelayedTransition(requireActivity().findViewById(R.id.toolbar), new Slide(Gravity.START));
            return true;
        } else if(itemId == R.id.menu_filter) {
            showFilterDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            mRegAdapter.setMediaPlayerService(mediaPlayerService);
            mStarredAdapter.setMediaPlayerService(mediaPlayerService);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLogBinding.inflate(inflater, container, false);
        requireActivity().setTitle(R.string.app_name);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        fileModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.progress.setVisibility(View.VISIBLE);
                binding.chronometer.setBase(SystemClock.elapsedRealtime());
                binding.chronometer.start();
            } else {
                binding.progress.setVisibility(View.GONE);
                binding.chronometer.stop();
            }
        });

        fileModel.getParsingError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Intent intent = new Intent(requireContext(), ErrorReportActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("error_message", "Woops. An unexpected error occurred");
                intent.putExtra("stack_trace", error);

                // Start the new activity directly without using startActivity()
                if (requireContext() instanceof AppCompatActivity) {
                    requireContext().startActivity(intent);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    requireContext().startActivity(intent);
                }
            }
        });

        // Set RecyclerView
        mLinearLayout = new WrapContentLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        mGridLayout = new GridLayoutManager(requireContext(), 2);
        mGridLayout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mRegAdapter.getItemViewType(position) == CallLogAdapter.VIEW_TYPE_HEADER)
                    return 2;
                else
                    return 1;
            }
        });

        setupRecyclerView();
        setupFabView();
        setupBottomPlayer();
        setupFilterView();

        fileModel.getDataList().observe(getViewLifecycleOwner(), dataWrapper -> {
            // Reset First
            resetItems();

            // Add new Data
            sortedList.addAll(dataWrapper.sortedListWithHeaders());
            sortedListFiltered.addAll(dataWrapper.sortedListWithHeaders());
            starredList.addAll(dataWrapper.starredListWithHeader());
            starredListFiltered.addAll(dataWrapper.starredListWithHeader());
            allList.addAll(starredList);
            allList.addAll(sortedList);

            // Notify Adapters
            mRegAdapter.notifyItemRangeInserted(0, sortedListFiltered.size());
            mStarredAdapter.notifyItemRangeInserted(0, starredListFiltered.size());

            // Set Max Duration
            double max = dataWrapper.maxDuration();
            binding.filterCardLayout.durationSlider.setValueTo((float) max);
            binding.filterCardLayout.durationSlider.setValues((float) 0.0, (float) max);
            binding.filterCardLayout.durationEndText.setText(DateUtils.formatDuration((long) max));
            mEndDuration = (float) max;

            if (fileModel.getPlayingItem().getValue() != null) {
                for(Object item : sortedListFiltered) {
                    if (item instanceof CallLogItem callLogItem) {
                        if (TextUtils.equals(callLogItem.getFileName(),fileModel.getPlayingItem().getValue().getFileName())) {
                            currentlyPlaying = callLogItem;
                            callLogItem.setPlaying(true);
                            fileModel.setPlayingItem(currentlyPlaying);
                            break;
                        }
                    }
                }
            }
        });

        fileModel.getPlayingItem().observe(getViewLifecycleOwner(), this::setPlayerInfo);
    }

    private void setupRecyclerView() {
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
                            p.setMargins(0, 0, intToDp(), (binding.playerInfoBarContainer.getHeight() + binding.playerInfoBarContainer.getPaddingBottom()));
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

        setupRecyclerOrientation(getResources().getConfiguration().orientation);

        PopupTextProvider popupTextProvider;
        if (PreferenceUtils.showHeaders()) popupTextProvider = (view1, position) -> Objects.requireNonNull(getNearestHeader(position, allList));
        else popupTextProvider = null;

        new FastScrollerBuilder(binding.recyclerView)
                .setPadding(0, 0, 10, 0)
                .setPopupTextProvider(popupTextProvider)
                .build();

        enableSwipeToStar();
    }

    private String getNearestHeader(int position, List<Object> list) {
        if (position < 0 || position >= list.size()) {
            return "";
        }

        // Find the nearest header before or at the given position
        for (int i = position; i >= 0; i--) {
            Object item = list.get(i);
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
        for (int i = position + 1; i < list.size(); i++) {
            Object item = list.get(i);
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

    private void enableSwipeToStar() {
        SwipeCallback swipeToDeleteCallback = new SwipeCallback(requireContext(), SwipeCallback.TYPE_STAR) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getBindingAdapterPosition();
                CallLogItem item;
                if (viewHolder.getBindingAdapter() == mRegAdapter)
                    item = (CallLogItem) sortedListFiltered.get(position);
                else
                    item = (CallLogItem) starredListFiltered.get(position);

                if (item.isStarred()) {
                    item.setStarred(false);
                    // Notify Reg Adapter that item is not starred
                    mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(item));
                    // Remove item from starred list
                    mStarredAdapter.removeItem(starredListFiltered.indexOf(item));
                    starredListFiltered.remove(item);
                    // If the last item is removed, remove the header too
                    if (mStarredAdapter.getItemCount() == 1 && mStarredAdapter.getItemViewType(0) == CallLogAdapter.VIEW_TYPE_HEADER) {
                        mStarredAdapter.removeItem(0);
                        starredList.clear();
                    }
                } else {
                    item.setStarred(true);
                    mStarredAdapter.addItem(item);
                    starredList.add(item);
                }

                PreferenceUtils.setStarred(item.getFileName(), item.isStarred());

                if (binding.playerInfoBarContainer.getVisibility() == View.VISIBLE) {
                    binding.bottomPlayerLayout.starredIcon.setVisibility(currentlyPlaying.isStarred() ? View.VISIBLE : View.GONE);
                }

                resetAllList();

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(binding.recyclerView);
    }

    private void resetAllList() {
        allList.clear();
        allList.addAll(starredListFiltered);
        allList.addAll(sortedListFiltered);
    }

    private void setupRecyclerOrientation(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (!mLinearLayout.isAttachedToWindow()) binding.recyclerView.setLayoutManager(mLinearLayout);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!mGridLayout.isAttachedToWindow()) binding.recyclerView.setLayoutManager(mGridLayout);
        }
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setupRecyclerOrientation(newConfig.orientation);
    }

    private void setupFabView() {
        binding.fab.hide();
        binding.fab.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            binding.recyclerView.smoothScrollToPosition(0);
            binding.fab.hide();
        });
    }

    private void setupBottomPlayer() {
        binding.playerInfoBarContainer.setOnClickListener(v -> showPlayer());
        binding.bottomPlayerLayout.playPauseButton.setOnClickListener(v -> {
            if (currentlyPlaying != null) {
                if (!currentlyPlaying.isPlaying()) currentlyPlaying.setPlaying(true);
                onPlay(binding.bottomPlayerLayout.playPauseButton, currentlyPlaying, null);
            }
        });
        binding.bottomPlayerLayout.playPauseButton.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            if (mediaPlayerService == null) return;
            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            } else {
                mediaPlayerService.resumePlayback();
                startUpdater();
            }
            mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(currentlyPlaying));
            if (starredList.contains(currentlyPlaying))
                mStarredAdapter.notifyItemChanged(starredListFiltered.indexOf(currentlyPlaying));
        });
        binding.playerInfoBarContainer.setOnLongClickListener(v -> {
            binding.recyclerView.smoothScrollToPosition(sortedListFiltered.indexOf(currentlyPlaying));
            return true;
        });
    }

    private void setupFilterView() {
        // Date Filter
        binding.filterCardLayout.dateFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.filterCardLayout.dateFilterLayoutRadio.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            mDateEnabled = isChecked;
            filter();
        });
        binding.filterCardLayout.radioSingleDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mFilterDateType = FILTER_DATE_TYPE_SINGLE;
            filter();
        });
        binding.filterCardLayout.radioRangeDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mFilterDateType = FILTER_DATE_TYPE_RANGE;
            filter();
        });
        binding.filterCardLayout.pickDateButton.setOnClickListener(v -> {
            VibratorUtils.vibrate(requireContext(), 25);
            showDatePicker();
        });

        // Direction Filter
        binding.filterCardLayout.directionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.filterCardLayout.directionMenu.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            filter();
        });
        binding.filterCardLayout.directionMenuAutoCompleteTextView.setOnItemClickListener((parent, view1, position, id) -> {
            switch (position) {
                case 0 -> filterDirectionQuery = "in";
                case 1 -> filterDirectionQuery = "out";
                case 2 -> filterDirectionQuery = "conference";
                case 3 -> filterDirectionQuery = "";
            }
            filter();
        });

        // Duration Filter
        binding.filterCardLayout.durationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.filterCardLayout.durationLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            filter();
        });
        binding.filterCardLayout.durationSlider.setValueFrom(0.0f);
        binding.filterCardLayout.durationSlider.setLabelFormatter(value -> {
            int minutes = (int) (value / 60);
            int seconds = (int) (value % 60);
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
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
                filter();
            }
        });

        // Sim Filter
        if (nSim>1){
            binding.filterCardLayout.simFilter.setVisibility(View.VISIBLE);
            binding.filterCardLayout.simDiv.setVisibility(View.VISIBLE);
        }

        binding.filterCardLayout.simCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.filterCardLayout.simFilter.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.filterCardLayout.simDiv.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            filter();
        });
        binding.filterCardLayout.simMenuAutoCompleteTextView.setOnItemClickListener((parent, view1, position, id) -> filter());

    }

    private void filter() {
        List<Object> filteredRegList = new ArrayList<>();
        List<Object> filteredStarredList = new ArrayList<>();
        List<CallLogItem> filteredCallLogItems = new ArrayList<>();
        String query = searchQuery != null ? searchQuery.toString().toLowerCase() : "";

        Date currentDate = Date.from(Calendar.getInstance().getTime().toInstant());
        Date lastAddedHeaderDate = null;
        boolean addedMonthHeader = false;
        String monthHeader = "";

        for (Object item : sortedList) {
            if (item instanceof CallLogItem callLogItem) {
                if (isDateInRange(new Date(callLogItem.getTimestampDate()))) {
                    if (isDurationInRange((float) ((CallLogItem) item).getDuration())) {
                        if (isDirectionInList(callLogItem.getDirection())) {
                            if (isSimInList(callLogItem.getSimSlot())) {
                                if (!TextUtils.isEmpty(callLogItem.getContactName()) && removeDiacriticalMarks(callLogItem.getContactName().toLowerCase()).contains(removeDiacriticalMarks(query)))
                                    filteredCallLogItems.add(callLogItem);
                            }
                        }
                    }
                }
            }
        }

        // Add headers to the filtered list
        for (CallLogItem callLogItem : filteredCallLogItems) {
            // Check if the date has changed for headers
            Date itemDate = new Date(callLogItem.getTimestampDate());

            if (callLogItem.isStarred()) filteredStarredList.add(callLogItem);

            if (!DateUtils.isSameDay(lastAddedHeaderDate, itemDate)) {
                // Add a new header if the date is different from the last added header
                if (DateUtils.isSameDay(currentDate, itemDate)) {
                    // If the date is today, show 'Today'
                    filteredRegList.add(new DateHeader(requireContext().getString(R.string.today)));
                } else if (DateUtils.isYesterday(currentDate, itemDate)) {
                    // If the date is yesterday, show 'Yesterday'
                    filteredRegList.add(new DateHeader(requireContext().getString(R.string.yesterday)));
                } else if (DateUtils.isLastWeek(currentDate, itemDate)) {
                    // If the date is in last week, show the day (e.g., Wednesday)
                    String dayOfWeek = DateUtils.getDayOfWeek(itemDate);
                    filteredRegList.add(new DateHeader(dayOfWeek));
                } else if (!addedMonthHeader && DateUtils.isLastMonth(currentDate, itemDate)) {
                    // If the date is in last month, show 'Last month'
                    filteredRegList.add(new DateHeader(requireContext().getString(R.string.last_month)));
                    monthHeader = DateUtils.getMonth(itemDate);
                    addedMonthHeader = true;
                } else {
                    // For other items, group by month
                    String month = DateUtils.getMonth(itemDate);
                    if (!Objects.equals(month, monthHeader)) {
                        monthHeader = month;
                        filteredRegList.add(new DateHeader(month));
                    }
                }

                // Update the last added header date
                lastAddedHeaderDate = itemDate;
            }

            // Add the regular item
            filteredRegList.add(callLogItem);
        }

        if (filteredStarredList.size() > 0) filteredStarredList.add(0, new DateHeader(requireContext().getString(R.string.starred)));

        mRegAdapter.filter(filteredRegList);
        mStarredAdapter.filter(filteredStarredList);
        resetAllList();
        setupBadge(getFilteredCount());
    }

    // Filter Functions
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

    private boolean isDirectionInList(String direction) {
        if (binding.filterCardLayout.directionCheckBox.isChecked()) {
            if (TextUtils.isEmpty(filterDirectionQuery)) return true;
            else return direction.equals(filterDirectionQuery);
        } else {
            return true;
        }
    }

    private boolean isSimInList(int simSlot) {
        String sim = String.valueOf(binding.filterCardLayout.simMenuAutoCompleteTextView.getText());
        if (binding.filterCardLayout.simCheckbox.isChecked()) {
            if (TextUtils.isEmpty(sim)) return true;
            else return simSlot == Integer.parseInt(sim);
        } else {
            return true;
        }
    }

    public String removeDiacriticalMarks(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    private void showDatePicker() {
        CallLogItem lastItemLog = (CallLogItem)sortedList.get(sortedList.size()-1);
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
                filterDate(new Date[]{dateFilter}, dateText);
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
                filterDate(new Date[]{dateFilterStart, dateFilterEnd}, dateText);
            });
            picker.show(getChildFragmentManager(), picker.toString());
        }
    }

    /**
     * Helper method to apply the date filter,
     * Called when the user picks a date or date range
     * @param dates The dates to filter
     * @param dateText The text to show in the date filter card
     */
    private void filterDate(Date[] dates, String dateText) {
        if (dates.length == 1) {
            mStartDate = dates[0];
            mFilterDateType = FILTER_DATE_TYPE_SINGLE;
        } else {
            mStartDate = dates[0];
            mEndDate = dates[1];
            mFilterDateType = FILTER_DATE_TYPE_RANGE;
        }
        mDatePicked = true;
        binding.filterCardLayout.dateFilterText.setText(dateText);
        hideSoftKeyboard();
        filter();
    }

    /**
     * Utility method to hide the soft keyboard if visible
     */
    public void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
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

    /**
     * Helper for showing filter dialog or dismissing it
     */
    public void showFilterDialog() {

        if (binding.filterLayout.getVisibility() != View.VISIBLE) {
            binding.filterLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (nSim > 1) {
                binding.filterCardLayout.simFilter.setVisibility(View.VISIBLE);
                binding.filterCardLayout.simDiv.setVisibility(View.VISIBLE);
            }

            binding.filterLayout.setVisibility(View.VISIBLE);
            binding.filterLayout.setTranslationY(-binding.filterLayout.getHeight());
            binding.filterLayout.animate()
                    .translationY(0)
                    .setDuration(500)
                    .start();
        } else {
            binding.filterLayout.animate()
                    .translationY(-binding.filterLayout.getHeight())
                    .setDuration(500)
                    .withEndAction(() -> {
                        binding.filterLayout.setVisibility(View.GONE);
                        binding.filterLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                    })
                    .start();
        }

    }

    /**
     * Callback when we must play an item
     * This method is passed to {@link CallLogAdapter} and for handle the click of Play button in Bottom Player
     * @param playButton The {@link MaterialButton} that called the method
     * @param item The {@link CallLogItem} to play
     * @param adapter The {@link CallLogAdapter} that called the method, in this case mStarredAdapter or mRegAdapter
     *
     * Is passed the play button to change the icon and the tint instead of notify the adapter.
     */
    private void onPlay(MaterialButton playButton, CallLogItem item, RecyclerView.Adapter<?> adapter) {

        if (Boolean.TRUE.equals(fileModel.getIsLoading().getValue())) return;
        playButton.setIconTint(ColorStateList.valueOf(ThemeUtils.getPrimaryColor(requireContext())));
        Drawable drawable = ContextCompat.getDrawable(requireContext().getApplicationContext(), R.drawable.play_to_pause);

        if (currentlyPlaying == item) {
            // If it's the same item that is currently playing, pause or resume playback
            if (mediaPlayerService != null) {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.pausePlayback();
                    drawable = ContextCompat.getDrawable(requireContext().getApplicationContext(), R.drawable.pause_to_play);
                } else {
                    mediaPlayerService.resumePlayback();
                    drawable = ContextCompat.getDrawable(requireContext().getApplicationContext(), R.drawable.play_to_pause);
                }
            }
        } else {
            ContextCompat.getDrawable(requireContext().getApplicationContext(), R.drawable.pause_to_play);
            if (currentlyPlaying != null) {
                currentlyPlaying.setPlaying(false);
                mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(currentlyPlaying));
                if (starredList.contains(currentlyPlaying))
                    mStarredAdapter.notifyItemChanged(starredListFiltered.indexOf(currentlyPlaying));
            }
            currentlyPlaying = item;
            currentlyPlaying.setPlaying(true);

            if (mediaPlayerService.isPlaying()) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            }

            mediaPlayerService.startPlayback(item);
            mediaPlayerService.setOnCompletionListener(() -> {
                binding.bottomPlayerLayout.playerProgressBar.setProgressCompat(100, true);
                stopUpdater();
                mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(currentlyPlaying));
                if (starredList.contains(currentlyPlaying))
                    mStarredAdapter.notifyItemChanged(starredListFiltered.indexOf(currentlyPlaying));
            });
        }
        playButton.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
        if (adapter != null) {
            if (adapter == mRegAdapter) {
                mStarredAdapter.notifyItemChanged(starredListFiltered.indexOf(item));
            } else {
                mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(item));
            }
        }
        if (starredList.contains(currentlyPlaying))
            mStarredAdapter.notifyItemChanged(starredListFiltered.indexOf(currentlyPlaying));
        fileModel.setPlayingItem(item);
    }

    /**
     * Callback when is clicked Delete button in {@link CallLogAdapter}
     * @param item The {@link CallLogItem} to delete
     * @param adapter The {@link CallLogAdapter} that called the method, in this case mStarredAdapter or mRegAdapter
     * It first check if {@link FileViewModel#getIsLoading()} is true, if it is, it returns
     * Then checks if the {@link MediaPlayerService} is not null and if it's playing and if the item is playing
     * So it will show the {@link DeleteDialog} to confirm the delete
     */
    private void onDelete(CallLogItem item, RecyclerView.Adapter<?> adapter) {
        if (Boolean.TRUE.equals(fileModel.getIsLoading().getValue())) return;
        mRegAdapter.resetExpansion();
        mStarredAdapter.resetExpansion();
        if (mediaPlayerService != null) {
            if (mediaPlayerService.isPlaying() && item.isPlaying() && currentlyPlaying == item) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            }
        }
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        DeleteDialog deleteDialog = new DeleteDialog(item, () -> {
            if (TextUtils.equals(currentlyPlaying.getFileName(), item.getFileName())) {
                fileModel.setPlayingItem(null);
            }
            List<CallLogItem> items = new ArrayList<>();
            items.add(item);
            fileModel.deleteItems(requireContext(), items);
        });
        deleteDialog.show(fm,DeleteDialog.class.getSimpleName());

    }

    /**
     * Sets the {@link CallLogItem} for the Player and update UI
     * @param item The {@link CallLogItem} to set
     * Called when is set a different {@link CallLogItem} in {@link FileViewModel}
     * @see FileViewModel#setPlayingItem(CallLogItem)
     */
    private void setPlayerInfo(CallLogItem item) {
        if (binding == null) return;
        if (item == null) {
            binding.playerInfoBarContainer.setVisibility(View.GONE);
            stopUpdater();
            return;
        }
        if (mUpdateProgress == null) startUpdater();

        if (binding.playerInfoBarContainer.getVisibility() != View.VISIBLE) {
            binding.playerInfoBarContainer.setVisibility(View.VISIBLE);
        }
        binding.bottomPlayerLayout.setCallLogItem(item);
        binding.bottomPlayerLayout.setShowSim(PreferenceUtils.showSimPlayer(nSim));
        binding.bottomPlayerLayout.setShowLabel(PreferenceUtils.showLabelPlayer());

        switch (item.getDirection()) {
            case "in" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_in);
            case "out" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_out);
            case "conference" -> binding.bottomPlayerLayout.callIconPlayer.setImageResource(R.drawable.ic_conference);
        }

        binding.bottomPlayerLayout.datePlayer.setText(item.getFormattedTimestamp(requireContext().getString(R.string.today), requireContext().getString(R.string.yesterday)));

        if (item.getContactIcon()!=null)
            Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.bottomPlayerLayout.contactIconPlayer);
        else if (PreferenceUtils.showTiles())
            binding.bottomPlayerLayout.contactIconPlayer.setImageDrawable(item.getContactDrawable(requireContext()));
        else
            binding.bottomPlayerLayout.contactIconPlayer.setImageResource(R.drawable.ic_default_contact);

        if (TextUtils.isEmpty(item.getNumberLabel())) {
            binding.bottomPlayerLayout.dividerLabelPlayer.setVisibility(View.GONE);
            binding.bottomPlayerLayout.numberLabelPlayer.setVisibility(View.GONE);
        }

        setupFab();
    }

    /**
     * We setup FAB if the Bottom Player is shown
     * If the Bottom Player is shown, we need to add a margin to the FAB so it doesn't overlap
     */
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

    private int intToDp() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
    }

    /**
     * Simple method to reset all lists and notify adapters
     */
    private void resetItems() {
        // Reset First
        if (sortedList.size() > 0) {
            sortedList.clear();
        }
        if (sortedListFiltered.size() > 0) {
            sortedListFiltered.clear();
            mRegAdapter.notifyDataSetChanged();
        }
        if (starredList.size() > 0) {
            starredList.clear();
        }
        if (starredListFiltered.size() > 0) {
            starredListFiltered.clear();
            mStarredAdapter.notifyDataSetChanged();
        }

        if (allList.size() > 0) allList.clear();
    }

    /**
     * Here we check if the data of the last modified folder has changed since the last time we parsed the folder
     * If the two dates are different, we parse again
     * In case of a new file when the app was already open, we parse again
     */
    @Override
    public void onResume() {
        super.onResume();
        MainActivity.mHideMenu = false;
        ((MainActivity) requireActivity()).supportInvalidateOptionsMenu();
        if (FileUtils.getLastModifiedFolder(requireContext()) != PreferenceUtils.getLastTime()) {
            Log.d(this.getClass().getSimpleName(), "onResume: lastModified changed");
            fileModel.fetchData(requireContext(), "");
        }
    }

    /**
     * This is called when the user clicks on the rootLayout of Bottom Player
     */
    private void showPlayer() {
        PlayerFragment playerFragment = new PlayerFragment(allList, currentlyPlaying, mediaPlayerService,
                this::stopUpdater, this::startUpdater,
                new PlayerFragment.StarredInterface() {
                    @Override
                    public void setStarred(CallLogItem item) {
                        item.setStarred(true);
                        mStarredAdapter.addItem(item);
                        if (starredList.size() == 0)
                            starredList.add(new DateHeader(getString(R.string.starred)));
                        starredList.add(item);
                        mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(item));
                        resetAllList();
                    }

                    @Override
                    public void setUnstarred(CallLogItem item) {
                        item.setStarred(false);
                        mStarredAdapter.removeItem(starredListFiltered.indexOf(item));
                        starredList.remove(item);
                        if (mStarredAdapter.getItemCount() == 1) {
                            mStarredAdapter.removeItem(0);
                            starredList.clear();
                        }
                        mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(item));
                        resetAllList();
                    }
                },
                (oldItem, newItem) -> {
                    oldItem.setPlaying(false);
                    mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(oldItem));
                    newItem.setPlaying(true);
                    currentlyPlaying = newItem;
                    mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(newItem));
                    fileModel.setPlayingItem(newItem);
                    playPauseButtonIcon(mediaPlayerService.isPlaying());
                },
                (item) -> {
                    if (BreakpointUtils.loadListBreakpoints(requireContext(), item.getFileName()).size() > 0) {
                        mRegAdapter.notifyItemChanged(sortedListFiltered.indexOf(item));
                    }
                });
        playerFragment.show(
                getChildFragmentManager(), PlayerFragment.class.getSimpleName()
        );
    }

    /**
     * Start the progress updater for Bottom Player
     */
    private void startUpdater() {
        playPauseButtonIcon(true);
        if (mUpdateProgress == null) {
            mUpdateProgress = new Handler();
        }
        mUpdateProgressTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerService!=null && binding != null && mUpdateProgress != null) {
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

    /**
     * Stop the progress updater for Bottom Player
     */
    private void stopUpdater() {
        if (mUpdateProgress == null) return;
        mUpdateProgress.removeCallbacks(mUpdateProgressTask, null);
        mUpdateProgress = null;
        mUpdateProgressTask = null;
        playPauseButtonIcon(false);
        Log.d("Updater", "playPause false");
    }

    /**
     * Change the icon of the play/pause button
     * @param isPlaying True for the play icon, false for the pause icon
     */
    private void playPauseButtonIcon(boolean isPlaying) {
        Drawable drawable;
        try{
            requireActivity();
        } catch (IllegalStateException e) {
            return;
        }
        if (isPlaying) {
            drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.play_to_pause);
        } else {
            drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.pause_to_play);
        }
        if (binding != null) binding.bottomPlayerLayout.playPauseButton.setIcon(drawable);
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).start();
        }
    }

    /**
     * Callback on {@link CallLogAdapter} when is clicked
     * an Action for a contact (edit/create new contact)
     * @param item The {@link CallLogItem} that was clicked
     */
    private void onContactClicked(CallLogItem item) {
        Toast.makeText(requireContext(), "onContactClicked", Toast.LENGTH_SHORT).show();
        contentObserver.setContactNumber(item.getNumber());
        requireContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);

    }

    /**
     * Callback on {@link CallLogAdapter} when the contact icon is clicked
     * @param item The {@link CallLogItem} that was clicked
     */
    private void contactIconClicked(CallLogItem item) {
        searchItem.expandActionView();
        searchView.setQuery(item.getContactName(), true);
    }

    /**
     * Callback of the ContactObserver {@link ContactObserver}
     * @param requireUpdate True if the data needs to be updated
     *                      we will fetch data again, but now is faster because it only updates contacts.
     */
    @Override
    public void onDataUpdate(boolean requireUpdate) {
        if (requireUpdate) {
            requireActivity().runOnUiThread(() -> fileModel.fetchData(requireContext(), null));
        }
        requireContext().getContentResolver().unregisterContentObserver(contentObserver);
    }

    /**
     * Get the number of items in the filtered list
     * @return The number of items actually filtered
     */
    private int getFilteredCount() {
        int countFiltered = countItems(sortedListFiltered) + countItems(starredListFiltered);
        int countNonFiltered = countItems(sortedList) + countItems(starredList);

        return countNonFiltered == countFiltered ? -1 : countFiltered;
    }

    /**
     * Count CallLogItem in a List
     * @param itemList The list to count
     * @return The number of CallLogItem in the list
     */
    private int countItems(List<Object> itemList) {
        int count = 0;
        for (Object item : itemList) {
            if (item instanceof CallLogItem) {
                count++;
            }
        }
        return count;
    }

    /**
     * Setup the filter badge
     * @param items The number of items
     *              -1 to hide the badge
     *              The badge shows the min between the number of items and 999,
     *              so for example if there are 1000 items, the badge will show 999+
     */
    public void setupBadge(int items) {

        if (textFilterItems != null) {
            if (items == -1) {
                if (textFilterItems.getVisibility() != View.GONE) {
                    textFilterItems.setVisibility(View.GONE);
                }
            } else {
                textFilterItems.setText(String.valueOf(Math.min(items, 999)));
                if (textFilterItems.getVisibility() != View.VISIBLE) {
                    textFilterItems.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void onTrimAudioClip(CallLogItem item) {
        if (Boolean.TRUE.equals(fileModel.getIsLoading().getValue())) return;
        if (mediaPlayerService != null) {
            if (mediaPlayerService.isPlaying() && item.isPlaying() && currentlyPlaying == item) {
                mediaPlayerService.pausePlayback();
                stopUpdater();
            }
        }
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.frame_layout, new TrimAudioFragment(item)).addToBackStack(null).commit();
    }

}
