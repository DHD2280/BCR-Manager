package it.dhd.bcrmanager.ui.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.List;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ItemEntryBinding;
import it.dhd.bcrmanager.databinding.ItemHeaderBinding;
import it.dhd.bcrmanager.objects.Breakpoints;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.objects.DateHeader;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;
import it.dhd.bcrmanager.utils.VibratorUtils;

public class CallLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View Types
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    // List of items
    public final List<Object> callLogItems;

    // Expanded Position Holder
    private int expandedPosition = -1;

    // Media Player Service
    private MediaPlayerService mediaPlayerService;
    private final Context mContext;
    private final int nSim;

    // Interfaces for click listeners
    private final onActionPlayListener mPlayListener;
    private final onDeleteListener mDeleteListener;
    private final onContactListener mContactListener;
    private final onContactIconClickListener mContactIconListener;
    private final onTrimListener mTrimClipListener;

    private boolean hasExpansion = true;
    private boolean hasContactsActionsEnabled = true;

    private int highlightPos = RecyclerView.NO_POSITION;

    public void filter(List<Object> filteredList) {
        callLogItems.clear();
        callLogItems.addAll(filteredList);
        notifyDataSetChanged();
    }

    public void highLight(int pos) {
        highlightPos = pos;
        notifyItemChanged(pos);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            highlightPos = RecyclerView.NO_POSITION;
            notifyItemChanged(pos);
        }, 350);
    }

    public interface onActionPlayListener {
        void onActionPlay(MaterialButton playButton, CallLogItem item, RecyclerView.Adapter<? extends RecyclerView.ViewHolder> bindingAdapter);
    }

    public interface onDeleteListener {
        void onDeleteClick(CallLogItem item, RecyclerView.Adapter<? extends RecyclerView.ViewHolder> bindingAdapter);
    }

    public interface onContactListener {
        void onContactClick(CallLogItem item);
    }

    public interface onContactIconClickListener {
        void onContactIconClick(CallLogItem item);
    }

    public interface onTrimListener {
        void onTrimClick(CallLogItem item);
    }

    public CallLogAdapter(Context context, List<Object> callLogItems, boolean hasExpansion) {
        this.mContext = context;
        this.nSim = SimUtils.getNumberOfSimCards(context);
        this.hasExpansion = hasExpansion;
        this.callLogItems = callLogItems;
        this.mediaPlayerService = null;
        this.mPlayListener = null;
        this.mDeleteListener = null;
        this.mContactListener = null;
        this.mContactIconListener = null;
        this.mTrimClipListener = null;
    }

    public CallLogAdapter(Context context, List<Object> callLogItems, MediaPlayerService mps,
                          onActionPlayListener listener, onDeleteListener deleteListener) {
        this.mContext = context;
        this.nSim = SimUtils.getNumberOfSimCards(context);
        this.callLogItems = callLogItems;
        this.mediaPlayerService = mps;
        this.mPlayListener = listener;
        this.mDeleteListener = deleteListener;
        this.mContactListener = null;
        this.mContactIconListener = null;
        this.mTrimClipListener = null;
    }

    public CallLogAdapter(Context context, List<Object> callLogItems, MediaPlayerService mps,
                          onActionPlayListener listener, onDeleteListener deleteListener,
                          onContactListener contactListener, onContactIconClickListener contactIconListener,
                          onTrimListener trimListener) {
        this.mContext = context;
        this.nSim = SimUtils.getNumberOfSimCards(context);
        this.callLogItems = callLogItems;
        this.mediaPlayerService = mps;
        this.mPlayListener = listener;
        this.mDeleteListener = deleteListener;
        this.mContactListener = contactListener;
        this.mContactIconListener = contactIconListener;
        this.mTrimClipListener = trimListener;
    }

    public void setMediaPlayerService(MediaPlayerService mps) {
        this.mediaPlayerService = mps;
    }

    public void setContactActionsEnabled(boolean featuresEnabled) {
        this.hasContactsActionsEnabled = featuresEnabled;
    }

    public void resetExpansion() {
        expandedPosition = -1;
    }

    @Override
    public int getItemViewType(int position) {
        if (callLogItems.get(position) instanceof DateHeader) {
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
            if (hasExpansion) bindingItem.rootLayout.setOnClickListener(v -> toggleExpansion(result.getBindingAdapterPosition()));
            bindingItem.callLogEntryActions.callAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())).getNumber()));
                mContext.startActivity(intent);
            });

            bindingItem.callLogEntryActions.createNewContactAction.setOnClickListener(v -> {
                CallLogItem clickedItem = ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition()));
                if (mContactListener != null) {
                    mContactListener.onContactClick(clickedItem);
                }
                Intent intent1 = new Intent(Intent.ACTION_INSERT);
                intent1.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent1.putExtra(ContactsContract.Intents.Insert.PHONE, ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())).getNumber());
                mContext.startActivity(intent1);
            });

            bindingItem.callLogEntryActions.editContactAction.setOnClickListener(v -> {
                CallLogItem clickedItem = ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition()));
                if (mContactListener != null) {
                    mContactListener.onContactClick(clickedItem);
                }
                Intent intent = new Intent(Intent.ACTION_EDIT);
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, "/" + ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())).getLookupKey());
                intent.setData(contactUri);
                mContext.startActivity(intent);

            });

            bindingItem.callLogEntryActions.sendMessageAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("sms:" + ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())).getNumber()));
                mContext.startActivity(intent);
            });

            if (!hasContactsActionsEnabled) {
                bindingItem.callLogEntryActions.callAction.setVisibility(View.GONE);
                bindingItem.callLogEntryActions.createNewContactAction.setVisibility(View.GONE);
                bindingItem.callLogEntryActions.editContactAction.setVisibility(View.GONE);
                bindingItem.callLogEntryActions.sendMessageAction.setVisibility(View.GONE);
            }

            bindingItem.actionPlay.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                if (mPlayListener != null) {
                    mPlayListener.onActionPlay(bindingItem.actionPlay, ((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())), result.getBindingAdapter());
                }
            });
            if (mPlayListener==null) bindingItem.actionPlay.setVisibility(View.GONE);

            bindingItem.callLogEntryActions.openWithAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                int adapterPosition = result.getBindingAdapterPosition();
                CallLogItem currentPlayingItem = ((CallLogItem)callLogItems.get(adapterPosition));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(currentPlayingItem.getAudioFilePath()), "audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            });

            bindingItem.callLogEntryActions.trimAudioClip.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                if (mTrimClipListener != null) {
                    mTrimClipListener.onTrimClick(((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())));
                }
            });
            bindingItem.callLogEntryActions.trimAudioClip.setVisibility(View.GONE);

            bindingItem.callLogEntryActions.shareAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                int adapterPosition = result.getBindingAdapterPosition();
                CallLogItem currentPlayingItem = ((CallLogItem)callLogItems.get(adapterPosition));
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPlayingItem.getAudioFilePath()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(Intent.createChooser(intent, "Share audio"));
            });

            bindingItem.callLogEntryActions.deleteAction.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                if (mDeleteListener != null) {
                    mDeleteListener.onDeleteClick(((CallLogItem)callLogItems.get(result.getBindingAdapterPosition())), result.getBindingAdapter());
                }
            });
        }
        return result;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            CallLogItem callLogItem = ((CallLogItem) callLogItems.get(position));
            ((CallLogViewHolder) holder).bind(callLogItem, position);
            ((CallLogViewHolder) holder).binding.setCallLogItem(callLogItem);
            ((CallLogViewHolder) holder).binding.setShowIcon(PreferenceUtils.showIcon());
            ((CallLogViewHolder) holder).binding.setShowSim(PreferenceUtils.showSim(nSim));
            ((CallLogViewHolder) holder).binding.setShowLabel(PreferenceUtils.showLabel());
            ((CallLogViewHolder) holder).binding.executePendingBindings();
        } else if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            DateHeader dateHeader = ((DateHeader)callLogItems.get(position));
            ((HeaderViewHolder) holder).bind(dateHeader);
        }
    }

    @Override
    public int getItemCount() {
        return callLogItems.size();
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

    public void setData(List<Object> newData) {
        callLogItems.clear();
        callLogItems.addAll(newData);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        expandedPosition = -1;
        Object item = callLogItems.get(position);
        if (item instanceof CallLogItem) {
            callLogItems.remove(item);
        } else {
            callLogItems.remove(position);
        }
        notifyItemRemoved(position);
    }

    public void restoreItem(Object item, int position) {
        callLogItems.add(position, item);
        notifyItemInserted(position);
    }

    public void addItem(Object item) {
        if (getItemCount() == 0) {
            callLogItems.add(new DateHeader(mContext.getString(R.string.starred)));
        }
        callLogItems.add(item);
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
                binding.rootLayout.setCardBackgroundColor(ThemeUtils.getColorSurfaceContainer(mContext));
            } else {
                binding.rootLayout.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.trans));
            }

            if (position == highlightPos) {
                binding.rootLayout.setCardBackgroundColor(ThemeUtils.getColorSurfaceHighest(mContext));
            } else if (position == expandedPosition) {
                binding.rootLayout.setCardBackgroundColor(ThemeUtils.getColorSurfaceHigh(mContext));
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
            binding.contactIcon.setOnClickListener(v -> {
                VibratorUtils.vibrate(mContext, 25);
                expandedPosition = -1;
                if (mContactIconListener != null) {
                    mContactIconListener.onContactIconClick(item);
                }
            });

            Picasso.get().cancelRequest(binding.contactIcon);
            if (item.getContactIcon()!=null)
                Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(binding.contactIcon);
            else if (PreferenceUtils.showTiles())
                binding.contactIcon.setImageDrawable(item.getContactDrawable(mContext));
            else
                binding.contactIcon.setImageResource(R.drawable.ic_default_contact);

            binding.callLogEntryActions.createNewContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && !item.isContactSaved() ? View.VISIBLE : View.GONE);
            binding.callLogEntryActions.editContactAction.setVisibility(PermissionsUtil.hasReadContactsPermissions() && item.isContactSaved() ? View.VISIBLE : View.GONE);

            String prefName = item.getFileName();
            String PREF_KEY_BREAKPOINTS = prefName + "_breakpoints";
            // Get a list of all breakpoints
            List<Breakpoints> breakpointsList = BreakpointUtils.loadListBreakpoints(mContext, PREF_KEY_BREAKPOINTS);
            // Iterate through the breakpoints and do something with them
            if (breakpointsList.size() > 0 && mediaPlayerService != null && mediaPlayerService.isPlaying() && item.isPlaying()) {
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
        }

        public void bind(DateHeader dateHeader) {
            if (!PreferenceUtils.showHeaders()) {
                binding.headerText.setVisibility(View.GONE);
                binding.headerText.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            } else binding.headerText.setText(dateHeader.date());
        }
    }
}