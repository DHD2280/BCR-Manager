package it.dhd.bcrmanager.ui.fragments;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.callbacks.SwipeCallback;
import it.dhd.bcrmanager.databinding.BatchDeleteLayoutBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.runner.TaskRunner;
import it.dhd.bcrmanager.ui.adapters.FilterAdapter;
import it.dhd.bcrmanager.ui.adapters.RegLogAdapter;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.FileUtils;
import it.dhd.bcrmanager.utils.UriUtils;
import me.zhanghai.android.fastscroll.FastScrollerBuilder;

public class BatchDelete extends Fragment implements View.OnClickListener {

    private List<CallLogItem> registrationsItems;
    private RegLogAdapter regLogAdapter;
    ArrayList<String> contactNames;

    private Date mStartDate = null, mEndDate = null;
    private int mDateType = 0;
    private final int DATE_TYPE_SINGLE = 0;
    private final int DATE_TYPE_RANGE = 1;

    private FilterAdapter mFilterAdapter;

    private BatchDeleteLayoutBinding binding;

    public BatchDelete() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        //showTutorial();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = BatchDeleteLayoutBinding.inflate(inflater, container, false);

        View view = binding.getRoot();

        setupViews();

        //showTutorial();

        return view;
    }

    private void setupViews() {

        registrationsItems = new ArrayList<>();


        Log.d("BatchDelete", "setupViews registrationsItems: " + registrationsItems.size());

        List<ContactItem> contactItems = NewHome.contactList;
        contactItems.removeIf(contact -> TextUtils.isEmpty(contact.getContactName()));
        contactItems.sort((o1, o2) -> o1.getContactName().compareToIgnoreCase(o2.getContactName()));
        contactNames = new ArrayList<>();
        List<ContactItem> contactsList = new ArrayList<>();
        contactNames.add(getString(R.string.unsaved_contacts));

        for (ContactItem item : contactItems) {
            if (!contactNames.contains(item.getContactName())) {
                contactNames.add(item.getContactName());
                contactsList.add(item);
            }
        }


        binding.noCriteriaTextView.setVisibility(View.VISIBLE);

        regLogAdapter = new RegLogAdapter(requireContext(), registrationsItems, null, false, false, null);

        binding.batchDeleteFab.hide();
        binding.batchDeleteFab.setOnClickListener(v -> showConfirmation());

        double mStartDuration = 0;
        double mEndDuration = NewHome.mMaxDuration;


        mFilterAdapter = new FilterAdapter(requireContext(), contactsList, contactNames, NewHome.mMaxDuration,
                mStartDuration, mEndDuration,
                new FilterAdapter.onClickListeners() {
                    @Override
                    public void onSingleDateClick(View v) {
                        onClick(v);
                    }

                    @Override
                    public void onRangeDateClick(View v) {
                        onClick(v);
                    }

                    @Override
                    public void onPickDateClick(View v) {
                        onClick(v);
                    }
                },
                isChecked -> reCheckList(),
                this::reCheckList);

        enableSwipeToDelete();

        ConcatAdapter concatAdapter = new ConcatAdapter(mFilterAdapter, regLogAdapter);
        binding.batchDeleteRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false));
        binding.batchDeleteRecyclerView.setAdapter(concatAdapter);

        binding.swipeUpFab.hide();
    }

    private void showConfirmation() {
        MaterialAlertDialogBuilder mBuilder = new MaterialAlertDialogBuilder(requireActivity());
        mBuilder.setTitle(R.string.sure_delete)
                .setMessage(R.string.sure_delete_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // Dismiss the alert dialog
                    dialog.dismiss();
                    // Call the method in MainActivity to delete this item
                    deleteItems();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // Dismiss the alert dialog
                    dialog.dismiss();
                })
                .show();
    }

    private void deleteItems() {

        binding.progress.setVisibility(View.VISIBLE);
        int size = registrationsItems.size();
        String formattedString = getResources().getQuantityString(R.plurals.deleted_registrations, size, size);
        List<CallLogItem> itemsToRemove = new ArrayList<>(registrationsItems);
        TaskRunner taskRunner = new TaskRunner();
        taskRunner.executeAsync(() -> {
            // Esegui il tuo metodo qui

            for (CallLogItem item : registrationsItems) {

                Uri fileUri, audioUri;
                fileUri = Uri.parse(item.getFilePath());
                audioUri = Uri.parse(item.getAudioFilePath());
                if (UriUtils.areEqual(fileUri, audioUri)) {
                    FileUtils.deleteFileUri(requireContext(), audioUri);
                } else {
                    FileUtils.deleteFileUri(requireContext(), fileUri);
                    FileUtils.deleteFileUri(requireContext(), audioUri);
                }
            }
            return null;
        },
                result -> {
                    regLogAdapter.notifyItemRangeRemoved(0, size);
                    registrationsItems.clear();
                    for (CallLogItem item : itemsToRemove) {
                        NewHome.deleteCallItem(item);
                        if (item.isStarred()) {
                            NewHome.deleteItemStarred(NewHome.yourListOfItemsStarred.indexOf(item));
                        }
                    }
                    itemsToRemove.clear();
                    List<CallLogItem> callLogItems = new ArrayList<>();
                    for (Object item : NewHome.yourListOfItems) {
                        if (item instanceof CallLogItem) {
                            callLogItems.add((CallLogItem) item);
                        }
                    }
                    FileUtils.saveObjectList(requireContext(), callLogItems, FileUtils.STORED_REG, CallLogItem.class);
                    binding.progress.setVisibility(View.GONE);
                    Snackbar.make(binding.batchDeleteFab.getRootView(), formattedString, Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.batchDeleteFab)
                            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.black))
                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            .show();
                });

    }


    private void reCheckList() {
        registrationsItems.clear();
        registrationsItems = new ArrayList<>();
        if ((mFilterAdapter.isChecked(mFilterAdapter.CONTACT) && mFilterAdapter.mContactPicked) ||
                (mFilterAdapter.isChecked(mFilterAdapter.DATE) && mStartDate != null) ||
                (mFilterAdapter.isChecked(mFilterAdapter.DIRECTION)) ||
                mFilterAdapter.isChecked(mFilterAdapter.DURATION)) {
            for (Object item : NewHome.yourListOfItems) {
                if (item instanceof CallLogItem currentItem) {
                    if (isDateInRange(new Date(currentItem.getTimestampDate())) &&
                            isContactInList(currentItem) &&
                            isDirectionInList(currentItem.getDirection()) &&
                            isDurationInList(currentItem) && !TextUtils.isEmpty(currentItem.getContactName()))
                        registrationsItems.add((CallLogItem) item);
                }
            }
            if (mFilterAdapter.mUnsaved) registrationsItems.sort((o1, o2) -> o1.getContactName().compareToIgnoreCase(o2.getContactName()));
            Log.d("BatchDelete", "reCheckList: " + registrationsItems.size());
            binding.noCriteriaTextView.setVisibility(View.GONE);
            if (registrationsItems.size() > 0) binding.batchDeleteFab.show();
            else binding.batchDeleteFab.hide();
            binding.noItemsTextView.setVisibility(registrationsItems.size() > 0 ? View.GONE : View.VISIBLE);
        } else binding.noCriteriaTextView.setVisibility(View.VISIBLE);

        if (registrationsItems.size() > 0) binding.batchDeleteFab.show();
        else binding.batchDeleteFab.hide();

        regLogAdapter.setData(registrationsItems);

        binding.batchDeleteRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Show FAB if scrolled beyond the first 20 items
                    if (firstVisibleItemPosition >= 15) {
                        binding.swipeUpFab.show();
                    } else {
                        binding.swipeUpFab.hide();
                    }
                }
            }
        });

        binding.swipeUpFab.setOnClickListener(v -> {
            binding.batchDeleteRecyclerView.smoothScrollToPosition(0);
            binding.swipeUpFab.hide();
        });

        new FastScrollerBuilder(binding.batchDeleteRecyclerView)
                //.setThumbDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.track)))
                .setPadding(0, 0, 10, 0)
                .build();

    }

    private void reCheckItems() {
        binding.noItemsTextView.setVisibility(registrationsItems.size() > 0 ? View.GONE : View.VISIBLE);
        if (registrationsItems.size() > 0) binding.batchDeleteFab.show();
        else binding.batchDeleteFab.hide();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pickDateButton) {
            if (!(NewHome.yourListOfItems.size() >0))
                return;

            CallLogItem lastItemLog = (CallLogItem)NewHome.yourListOfItems.get(NewHome.yourListOfItems.size()-1);
            // Start date is today
            long startDate = lastItemLog.getTimestampDate();
            // Get the end date (in milliseconds) for today
            long endDate = Calendar.getInstance().getTime().getTime();
            // Set the calendar constraints to restrict the range
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setStart(startDate);
            constraintsBuilder.setEnd(endDate);
            if (mDateType == DATE_TYPE_SINGLE) {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                builder.setTitleText(getString(R.string.pick_date));


                // Build and show the date picker
                builder.setCalendarConstraints(constraintsBuilder.build());
                MaterialDatePicker<Long> picker = builder.build();

                picker.addOnPositiveButtonClickListener(date -> {
                    Date dateFilter =new Date(date);
                    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateText = df2.format(dateFilter);
                    mFilterAdapter.setDateText(dateText);
                    mStartDate = dateFilter;
                    mDateType = DATE_TYPE_SINGLE;
                    reCheckList();
                });
                picker.show(getChildFragmentManager(), picker.toString());
            } else {

                // Assuming you have the MaterialDatePicker.Builder instance
                MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();

                // Set the initial selection
                builder.setSelection(new Pair<>(startDate, endDate));

                builder.setCalendarConstraints(constraintsBuilder.build());

                // Build and show the date picker
                MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
                picker.addOnPositiveButtonClickListener(dateRange -> {
                    Date dateFilterStart =new Date(dateRange.first);
                    Date dateFilterEnd =new Date(dateRange.second);
                    SimpleDateFormat df2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String dateText = df2.format(dateFilterStart) + " - " + df2.format(dateFilterEnd);
                    mFilterAdapter.setDateText(dateText);
                    mStartDate = dateFilterStart;
                    mEndDate = dateFilterEnd;
                    mDateType = DATE_TYPE_RANGE;
                    reCheckList();
                });
                picker.show(getChildFragmentManager(), picker.toString());
            }
        } else if (id == R.id.radioSingleDate) {
            mDateType = DATE_TYPE_SINGLE;
        } else if (id == R.id.radioRangeDate) {
            mDateType = DATE_TYPE_RANGE;
        }
    }

    private void enableSwipeToDelete() {
        SwipeCallback swipeToDeleteCallback = new SwipeCallback(requireContext(), SwipeCallback.TYPE_DELETE) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getBindingAdapterPosition();
                final CallLogItem item = regLogAdapter.callLogItemsFiltered.get(position);
                regLogAdapter.removeItem(position);
                registrationsItems.remove(position);

                reCheckItems();
                Snackbar snackbar = Snackbar
                        .make(binding.batchDeleteFab.getRootView(), "Item was removed from the list.", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.batchDeleteFab);
                snackbar.setAction("UNDO", view -> {

                    regLogAdapter.restoreItem(item, position);
                    registrationsItems.add(position, item);
                    binding.batchDeleteRecyclerView.scrollToPosition(position);
                });
                snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.black));
                snackbar.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryAccent));
                snackbar.show();

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(binding.batchDeleteRecyclerView);
    }

    private boolean isDateInRange(Date itemDate) {
        if (mStartDate != null && mFilterAdapter.isChecked(mFilterAdapter.DATE)) {
            if (mDateType == DATE_TYPE_SINGLE) {
                // Single date mode
                return DateUtils.isSameDay(itemDate, mStartDate);
            } else if (mDateType == DATE_TYPE_RANGE) {
                // Date range mode
                return !itemDate.before(mStartDate) && (!itemDate.after(mEndDate) || DateUtils.isSameDay(itemDate, mEndDate));
            }
        }
        // If no date filtering is applied, return true
        return true;
    }

    private boolean isContactInList(CallLogItem currentItem) {
        if (mFilterAdapter.mContactPicked && mFilterAdapter.isChecked(mFilterAdapter.CONTACT)) {
            if (TextUtils.isEmpty(currentItem.getContactName())) return false;
            if (mFilterAdapter.mUnsaved) return !currentItem.isContactSaved();
            else return currentItem.getContactName().equals(mFilterAdapter.getContact());
        }
        return true;
    }

    private boolean isDirectionInList(String direction) {
        if (mFilterAdapter.isChecked(mFilterAdapter.DIRECTION)) {
            if (mFilterAdapter.getDirection().isEmpty()) return true;
            else return direction.equals(mFilterAdapter.getDirection());
        }
        return true;
    }

    private boolean isDurationInList(CallLogItem currentItem) {
        if (mFilterAdapter.isChecked(mFilterAdapter.DURATION)) {
            return currentItem.getDuration() >= mFilterAdapter.getStartDuration() && currentItem.getDuration() <= mFilterAdapter.getEndDuration();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
    }
}
