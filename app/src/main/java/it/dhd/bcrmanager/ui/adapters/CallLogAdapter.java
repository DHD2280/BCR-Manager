package it.dhd.bcrmanager.ui.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ItemEntryBinding;
import it.dhd.bcrmanager.databinding.ItemHeaderBinding;
import it.dhd.bcrmanager.objects.Breakpoints;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;
import it.dhd.bcrmanager.utils.VibratorUtils;

public class CallLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public final List<Object> callLogItems;
    public final List<Object> callLogItemsFiltered;
    public static List<Object> items;
    private int expandedPosition = -1;
    private final MediaPlayerService mediaPlayerService;
    private CallLogItem currentlyPlayingItem;
    private final Context mContext;
    private final int nSim;

    public interface onActionPlayListener {
        public void onActionPlay(CallLogItem item);
    }

    public CallLogAdapter(Context context, List<Object> callLogItems, MediaPlayerService mps) {
        this.mContext = context;
        this.nSim = SimUtils.getNumberOfSimCards(context);
        this.callLogItems = callLogItems;
        this.callLogItemsFiltered = new ArrayList<>(callLogItems);
        this.mediaPlayerService = mps;
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
                VibratorUtils.vibrate(mContext, 25);
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber()));
                mContext.startActivity(intent);
            });

            bindingItem.callLogEntryActions.createNewContactAction.setOnClickListener(v -> {
                //contentObserver.setContactNumber(((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber());
                //mContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
                Intent intent1 = new Intent(Intent.ACTION_INSERT);
                intent1.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent1.putExtra(ContactsContract.Intents.Insert.PHONE, ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber());
                mContext.startActivity(intent1);
            });

            bindingItem.callLogEntryActions.editContactAction.setOnClickListener(v -> {
                CallLogItem clickedItem = ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition()));
                //contentObserver.setContactNumber(clickedItem.getNumber());
                //mContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
                Intent intent = new Intent(Intent.ACTION_EDIT);
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "/" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getLookupKey());
                intent.setData(contactUri);
                mContext.startActivity(intent);

            });

            bindingItem.callLogEntryActions.sendMessageAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("sms:" + ((CallLogItem)callLogItemsFiltered.get(result.getBindingAdapterPosition())).getNumber()));
                mContext.startActivity(intent);
            });

            bindingItem.actionPlay.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);


            });

            bindingItem.callLogEntryActions.openWithAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                int adapterPosition = result.getBindingAdapterPosition();
                CallLogItem currentPlayingItem = ((CallLogItem)callLogItemsFiltered.get(adapterPosition));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(currentPlayingItem.getAudioFilePath()), "audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            });

            bindingItem.callLogEntryActions.shareAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                int adapterPosition = result.getBindingAdapterPosition();
                CallLogItem currentPlayingItem = ((CallLogItem)callLogItemsFiltered.get(adapterPosition));
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPlayingItem.getAudioFilePath()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(Intent.createChooser(intent, "Share audio"));
            });

            bindingItem.callLogEntryActions.deleteAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);

            });
        }
        return result;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            CallLogItem callLogItem = ((CallLogItem) callLogItemsFiltered.get(position));
            ((CallLogViewHolder) holder).bind(callLogItem, position);
            ((CallLogViewHolder) holder).binding.setCallLogItem(callLogItem);
            ((CallLogViewHolder) holder).binding.setShowIcon(PreferenceUtils.showIcon());
            ((CallLogViewHolder) holder).binding.setShowSim(PreferenceUtils.showSim(nSim));
            ((CallLogViewHolder) holder).binding.setShowLabel(PreferenceUtils.showLabel());
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
        VibratorUtils.vibrate(mContext, 25);
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
                        if (!TextUtils.isEmpty(callLogItem.getContactName()) && removeDiacriticalMarks(callLogItem.getContactName().toLowerCase()).contains(removeDiacriticalMarks(query)))
                            filteredCallLogItems.add(callLogItem);
                    }
                }

                // Add headers to the filtered list
                    for (CallLogItem callLogItem : filteredCallLogItems) {
                        // Check if the date has changed for headers
                        Date itemDate = new Date(callLogItem.getTimestampDate());

                        if (!DateUtils.isSameDay(lastAddedHeaderDate, itemDate)) {
                            // Add a new header if the date is different from the last added header
                            if (DateUtils.isSameDay(currentDate, itemDate)) {
                                // If the date is today, show 'Today'
                                filteredList.add(new DateHeader(mContext.getString(R.string.today)));
                            } else if (DateUtils.isYesterday(currentDate, itemDate)) {
                                // If the date is yesterday, show 'Yesterday'
                                filteredList.add(new DateHeader(mContext.getString(R.string.yesterday)));
                            } else if (DateUtils.isLastWeek(currentDate, itemDate)) {
                                // If the date is in last week, show the day (e.g., Wednesday)
                                String dayOfWeek = DateUtils.getDayOfWeek(itemDate);
                                filteredList.add(new DateHeader(dayOfWeek));
                            } else if (!addedMonthHeader && DateUtils.isLastMonth(currentDate, itemDate)) {
                                // If the date is in last month, show 'Last month'
                                filteredList.add(new DateHeader(mContext.getString(R.string.last_month)));
                                monthHeader = DateUtils.getMonth(itemDate);
                                addedMonthHeader = true;
                            } else {
                                // For other items, group by month
                                String month = DateUtils.getMonth(itemDate);
                                if (!Objects.equals(month, monthHeader)) {
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


                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;

            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                callLogItemsFiltered.clear();
                callLogItemsFiltered.addAll((List<?>) filterResults.values);
                notifyDataSetChanged();
            }
        };
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
            callLogItems.add(new DateHeader(mContext.getString(R.string.starred)));
            callLogItemsFiltered.add(new DateHeader(mContext.getString(R.string.starred)));
        }
        callLogItems.add(item);
        callLogItemsFiltered.add(item);
        notifyDataSetChanged();
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
                binding.rootLayout.setCardBackgroundColor(ThemeUtils.getColorSurfaceHighest(mContext));
            } else {
                binding.rootLayout.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.trans));
            }

            binding.expandingLayout.setVisibility(position == expandedPosition ? View.VISIBLE : View.GONE);

            // Set Contact Name
            binding.contactName.setSelected(true);

            switch (item.getDirection()) {
                case "in" -> binding.callIcon.setImageResource(R.drawable.ic_in);
                case "out" -> binding.callIcon.setImageResource(R.drawable.ic_out);
                case "conference" -> binding.callIcon.setImageResource(R.drawable.ic_conference);
            }


            if (PreferenceUtils.showHeaders())
                binding.date.setText(item.getTimeStamp(mContext));
            else
                binding.date.setText(item.getFormattedTimestamp(mContext.getString(R.string.today), mContext.getString(R.string.yesterday)));

            Log.d("NewHome", "bind: " + item.getContactName() + " | NUMBER " + item.getNumberType());

            switch (item.getNumberType()) {
                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> binding.numberIcon.setImageResource(R.drawable.ic_call);
                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> binding.numberIcon.setImageResource(R.drawable.ic_home);
                case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
                        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> binding.numberIcon.setImageResource(R.drawable.ic_fax);
                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> binding.numberIcon.setImageResource(R.drawable.ic_work);
                case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> binding.numberIcon.setImageResource(R.drawable.ic_business);
            }

            binding.starredIcon.setVisibility(item.isStarred() ? View.VISIBLE : View.GONE);

            if (item.isPlaying())
                binding.actionPlay.setIconTint(ColorStateList.valueOf(ThemeUtils.getPrimaryColor(mContext)));
            else
                binding.actionPlay.setIconTint(ColorStateList.valueOf(ThemeUtils.getOnBackgroundColor(mContext)));

            Drawable drawable;
            if (mediaPlayerService != null && mediaPlayerService.isPlaying() && item.isPlaying()) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_pause);
            } else {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_play);
            }

            binding.actionPlay.setIcon(drawable);

            binding.contactIcon.setImageDrawable(null);
            Picasso.get().cancelRequest(binding.contactIcon);
            if (item.getContactIcon()!=null)
                Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.contactIcon);
            else if (PreferenceUtils.showTiles())
                binding.contactIcon.setImageDrawable(item.getContactDrawable(mContext));
            else
                binding.contactIcon.setImageResource(R.drawable.ic_default_contact);

            binding.callLogEntryActions.createNewContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && !item.isContactSaved() ? View.VISIBLE : View.GONE);
            binding.callLogEntryActions.editContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && item.isContactSaved() ? View.VISIBLE : View.GONE);
            binding.contactIcon.setOnClickListener(v -> {
                expandedPosition = -1;
                //searchItem.expandActionView();
                //searchView.setQuery(item.getContactName(), true);
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
                binding.breakpointsRecycler.setLayoutManager(new LinearLayoutManager(mContext));
                binding.breakpointsRecycler.setAdapter(breakpointAdapter);
            } else {
                binding.breakpointsDiv.setVisibility(View.GONE);
                binding.breakpointsRecycler.setVisibility(View.GONE);
            }
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final ItemHeaderBinding binding;

        HeaderViewHolder(@NonNull ItemHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            //binding.rootLayout.setOnClickListener(v -> toggleExpansion(getBindingAdapterPosition()));
        }

        public void bind(DateHeader dateHeader) {
            if (!PreferenceUtils.showHeaders()) {
                binding.headerText.setVisibility(View.GONE);
                binding.headerText.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            } else binding.headerText.setText(dateHeader.date());
        }
    }
}