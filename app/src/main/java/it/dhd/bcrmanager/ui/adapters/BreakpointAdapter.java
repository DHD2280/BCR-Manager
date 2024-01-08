package it.dhd.bcrmanager.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import it.dhd.bcrmanager.databinding.BreakpointItemBinding;
import it.dhd.bcrmanager.objects.Breakpoints;

public class BreakpointAdapter extends RecyclerView.Adapter<BreakpointAdapter.ViewHolder> {

    private final List<Breakpoints> breakpoints;

    private final onItemLongClickListener onItemLongClickListener;
    private final onItemClickListener onItemClickListener;

    /**
     * Constructor of the BreakpointAdapter
     * @param breakpoints The list of breakpoints
     * @param onItemClickListener The listener for the click on the item
     * @param onItemLongClickListener The listener for the long click on the item
     */
    public BreakpointAdapter(List<Breakpoints> breakpoints,
                             onItemClickListener onItemClickListener, onItemLongClickListener onItemLongClickListener) {
        this.breakpoints = breakpoints;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BreakpointItemBinding binding = BreakpointItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BreakpointAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Breakpoints breakpoint = breakpoints.get(position);
        holder.binding.breakPointTime.setText(formatTime(breakpoint.getTime()));
        holder.binding.breakPointTitle.setText(breakpoint.getTitle());
        holder.binding.breakPointDescription.setText(breakpoint.getDescription());

        holder.binding.rootLayout.setOnClickListener(v -> onItemClickListener.onItemClick(v, position, breakpoint));
        holder.binding.rootLayout.setOnLongClickListener(v -> {
            onItemLongClickListener.onItemLongClick(v, position, breakpoint);
            return true;
        });
    }

    /**
     * Format the time in milliseconds to a string
     * @param milliseconds The time in milliseconds
     * @return The formatted time (e.g. 00:00)
     */
    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public void setData(List<Breakpoints> newData) {
        breakpoints.clear();
        breakpoints.addAll(newData);
    }

    @Override
    public int getItemCount() {
        return breakpoints.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final BreakpointItemBinding binding;

        ViewHolder(@NonNull BreakpointItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

    }

    /**
     * Interface for the long click on the item
     */
    public interface onItemLongClickListener {
        void onItemLongClick(View view, int position, Breakpoints breakpoint);
    }

    /**
     * Interface for the click on the item
     */
    public interface onItemClickListener {
        void onItemClick(View view, int position, Breakpoints breakpoint);
    }

}
