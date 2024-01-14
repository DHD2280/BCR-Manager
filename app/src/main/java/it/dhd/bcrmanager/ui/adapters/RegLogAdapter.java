package it.dhd.bcrmanager.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ItemEntryBinding;
import it.dhd.bcrmanager.objects.CallLogItem;
import it.dhd.bcrmanager.services.MediaPlayerService;
import it.dhd.bcrmanager.ui.dialogs.DeleteDialog;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.SimUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;

public class RegLogAdapter extends RecyclerView.Adapter<RegLogAdapter.ViewHolder> implements Filterable {

    private final List<CallLogItem> callLogItems;
    public final List<CallLogItem> callLogItemsFiltered;

    private int expandedPosition = -1;
    private final Context mContext;
    private final int nSim;
    private final MediaPlayerService mediaPlayerService;
    private final boolean hasExpansion;
    private final boolean hasPlayButton;
    private final boolean showTiles;
    private final OnClickListener onClickListener;
    boolean callEnabled = true, messageEnabled = true;

    public interface OnClickListener {
        void onActionPlayClick(ItemEntryBinding binding, int position);
    }

    public RegLogAdapter(@NonNull Context context, List<CallLogItem> callLogItems,
                         MediaPlayerService mps, boolean hasExpansion, boolean hasPlayButton,
                         OnClickListener onClickListener) {
        this.mContext = context;
        this.nSim = SimUtils.getNumberOfSimCards(context);
        this.callLogItems = new ArrayList<>(callLogItems);
        this.callLogItemsFiltered = new ArrayList<>(callLogItems);
        this.hasPlayButton = hasPlayButton;
        showTiles = PreferenceUtils.showTiles();
        mediaPlayerService = mps;
        this.hasExpansion = hasExpansion;
        this.onClickListener = onClickListener;
        Log.d("RegLogAdapter", "RegLogAdapter: " + hasExpansion + " " + hasPlayButton + " size " + callLogItems.size());
    }

    public void setCallEnabled(boolean enabled) {
        callEnabled = enabled;
    }

    public void setMessageEnabled(boolean enabled) {
        messageEnabled = enabled;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemEntryBinding binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        CallLogItem item = callLogItemsFiltered.get(position);

        holder.binding.setShowSim(nSim > 1);
        holder.binding.setCallLogItem(item);

        // Set click listener for the entire item (excluding contact icon)
        if (hasExpansion) holder.binding.rootLayout.setOnClickListener(v -> toggleExpansion(position));
        if (!hasPlayButton) holder.binding.actionPlay.setVisibility(View.GONE);

        holder.binding.expandingLayout.setVisibility(position == expandedPosition ? View.VISIBLE : View.GONE);
        if (position == expandedPosition) {
            holder.binding.rootLayout.setCardBackgroundColor(ContextCompat.getColor(mContext, com.google.android.material.R.color.material_dynamic_primary10));
        } else {
            holder.binding.rootLayout.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.trans));
        }

        // Set click listener for the expand button
        //holder.rootLayout.setOnClickListener(v -> toggleExpansion(position));

        if(item.getDirection().contains("in")) holder.binding.callIcon.setImageResource(R.drawable.ic_in);
        else holder.binding.callIcon.setImageResource(R.drawable.ic_out);

        holder.binding.date.setText(item.getFormattedTimestamp(mContext.getString(R.string.today), mContext.getString(R.string.yesterday)));

        holder.binding.contactIcon.setImageDrawable(null);

        if (item.isStarred())
            holder.binding.starredIcon.setVisibility(View.VISIBLE);
        else
            holder.binding.starredIcon.setVisibility(View.GONE);

        if (item.getContactIcon()!=null)
            Picasso.get().load(item.getContactIcon()).transform(new CircleTransform()).placeholder(R.drawable.ic_default_contact).into(holder.binding.contactIcon);
        else if (showTiles)
            holder.binding.contactIcon.setImageDrawable(item.getContactDrawable(mContext));
        else
            holder.binding.contactIcon.setImageResource(R.drawable.ic_default_contact);

        holder.binding.callLogEntryActions.createNewContactAction.setVisibility(item.isContactSaved() ? View.GONE : View.VISIBLE);

        if (item.isPlaying())
            holder.binding.actionPlay.setIconTint(ColorStateList.valueOf(ThemeUtils.getPrimaryColor()));
        else
            holder.binding.actionPlay.setIconTint(ColorStateList.valueOf(ThemeUtils.getOnBackgroundColor()));

        Log.d("RegLogAdapter", "colorPrimary: " + ThemeUtils.getPrimaryColor());

        Drawable drawable;
        if (mediaPlayerService != null && mediaPlayerService.isPlaying() && item.isPlaying()) {
            drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_pause);
        } else {
            drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_play);
        }

        holder.binding.actionPlay.setIcon(drawable);
        //setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return callLogItemsFiltered.size();
    }

    public void removeItem(int position) {
        callLogItemsFiltered.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(CallLogItem item, int position) {
        callLogItemsFiltered.add(position, item);
        notifyItemInserted(position);
    }

    private void toggleExpansion(int position) {
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

                List<CallLogItem> filteredList = new ArrayList<>();
                String query = charSequence.toString().toLowerCase();

                for (CallLogItem item : callLogItems) {
                    if (query.equals("in") || query.equals("out")) {
                        if (item.getDirection().equals(query))
                            filteredList.add(item);
                    } else {
                        if (item.getContactName() != null && removeDiacriticalMarks(item.getContactName().toLowerCase()).contains(removeDiacriticalMarks(query))) {
                            filteredList.add(item);
                        } else if (item.getNumber().contains(query)) {
                            filteredList.add(item);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;

            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                callLogItemsFiltered.clear();
                callLogItemsFiltered.addAll((List<CallLogItem>) filterResults.values);
                notifyDataSetChanged();
            }
        };
    }

    public void setData(List<CallLogItem> newData) {
        callLogItems.clear();
        callLogItemsFiltered.clear();
        callLogItems.addAll(newData);
        callLogItemsFiltered.addAll(newData);
        notifyDataSetChanged();
        Log.d("RegLogAdapter", "RegLogAdapter: " + hasExpansion + " " + hasPlayButton + " size " + callLogItems.size() + " newData " + newData.size());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemEntryBinding binding;

        ViewHolder(@NonNull ItemEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;


            binding.callLogEntryActions.callAction.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + callLogItemsFiltered.get(getBindingAdapterPosition()).getNumber()));
                mContext.startActivity(intent);
            });

            if (!callEnabled) binding.callLogEntryActions.callAction.setVisibility(View.GONE);

            binding.callLogEntryActions.sendMessageAction.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("sms:" + callLogItemsFiltered.get(getBindingAdapterPosition()).getNumber()));
                mContext.startActivity(intent);
            });

            if (!messageEnabled) binding.callLogEntryActions.sendMessageAction.setVisibility(View.GONE);

            binding.actionPlay.setOnClickListener(v -> onClickListener.onActionPlayClick(binding, getBindingAdapterPosition()));

            binding.callLogEntryActions.openWithAction.setOnClickListener(v -> {
                int adapterPosition = getBindingAdapterPosition();
                CallLogItem currentPlayingItem = callLogItemsFiltered.get(adapterPosition);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(currentPlayingItem.getAudioFilePath()), "audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            });
            binding.callLogEntryActions.deleteAction.setOnClickListener(v -> {
                if (mediaPlayerService.isPlaying()) {
                    mediaPlayerService.stopPlayback();
                    //stopUpdater();
                }
                CallLogItem itemToDelete = callLogItemsFiltered.get(getBindingAdapterPosition());
                FragmentManager fm = ((FragmentActivity)mContext).getSupportFragmentManager();
                DeleteDialog custom = new DeleteDialog(itemToDelete, () -> {
                    callLogItems.remove(itemToDelete);
                    callLogItemsFiltered.remove(getBindingAdapterPosition());
                    notifyItemRemoved(getBindingAdapterPosition());
                });
                Bundle args = new Bundle();
                args.putString("title", mContext.getString(R.string.delete_dialog_title));
                args.putString("message", mContext.getString(R.string.delete_dialog_message));
                custom.setArguments(args);
                custom.show(fm,"");
            });
        }

    }
}
