package it.dhd.bcrmanager.ui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.FilterBatchDeleteBinding;
import it.dhd.bcrmanager.objects.ContactItem;
import it.dhd.bcrmanager.utils.CircleTransform;
import it.dhd.bcrmanager.utils.DateUtils;
import it.dhd.bcrmanager.utils.PreferenceUtils;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {


    private final Context mContext;
    private final onClickListeners onClickListeners;
    private final onCheckedChangeListener onCheckboxChanged;
    private final runReCheck runReCheck;
    private final ArrayList<String> contactNames;
    private final List<ContactItem> contactList;
    public boolean mUnsaved = false;
    private String mDirection = "";
    private final double mMaxDuration;
    private double mStartDuration;
    private double mEndDuration;

    public final int CONTACT = 0;
    public final int DATE = 1;
    public final int DIRECTION = 2;
    public final int DURATION = 3;

    public boolean mContactPicked;

    private FilterBatchDeleteBinding binding;

    public FilterAdapter(Context context, List<ContactItem> contactList, ArrayList<String> contactNames,
                         double maxDuration, double startDuration, double endDuration,
                         onClickListeners onClickListeners,
                         onCheckedChangeListener onCheckboxChanged,
                         runReCheck runReCheck) {

        this.mContext = context;
        this.contactNames = contactNames;
        this.contactList = contactList;
        this.mMaxDuration = maxDuration;
        this.mStartDuration = startDuration;
        this.mEndDuration = endDuration;
        this.onClickListeners = onClickListeners;
        this.onCheckboxChanged = onCheckboxChanged;
        this.runReCheck = runReCheck;
    }

    public void setDateEnabled(boolean enabled) {
        binding.batchDeleteDateCardView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void setContactEnabled(boolean enabled) {
        binding.batchDeleteContactCardView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void setDirectionEnabled(boolean enabled) {
        binding.batchDeleteDirectionCardView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void setDurationEnabled(boolean enabled) {
        binding.batchDeleteDurationCardView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public boolean isChecked(int checkType) {
        return switch (checkType) {
            case CONTACT -> binding.batchDeleteContactCheckbox.isChecked();
            case DATE -> binding.batchDeleteDateCheckbox.isChecked();
            case DIRECTION -> binding.batchDeleteDirectionCheckbox.isChecked();
            case DURATION -> binding.batchDeleteDurationCheckBox.isChecked();
            default -> false;
        };
    }

    public String getContact() {
        return binding.contactMenuAutoCompleteTextView.getText().toString();
    }

    public String getDirection() {
        return mDirection;
    }

    public void setDateText(String text) {
        binding.batchDeleteDateTextView.setText(text);
    }

    public double getStartDuration() {
        return mStartDuration;
    }

    public double getEndDuration() {
        return mEndDuration;
    }

    public interface onCheckedChangeListener {
        void onCheckboxChanged(boolean isChecked);
    }

    public interface runReCheck {
        void runrecheck();
    }

    public interface onClickListeners {
        void onPickDateClick(View v);
        void onSingleDateClick(View v);
        void onRangeDateClick(View v);
    }

    @NonNull
    @Override
    public FilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = FilterBatchDeleteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterAdapter.ViewHolder holder, int position) {
        holder.binding.batchDeleteContactCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            holder.binding.contactFilterLayout.setVisibility(isChecked ? ViewGroup.VISIBLE : ViewGroup.GONE);
            onCheckboxChanged.onCheckboxChanged(isChecked);
            runReCheck.runrecheck();
        });

        String[] stringArray = contactNames.toArray(new String[0]);
        (((MaterialAutoCompleteTextView) Objects.requireNonNull(holder.binding.contactMenu.getEditText()))).setSimpleItems(stringArray);

        holder.binding.contactMenuAutoCompleteTextView.setOnItemClickListener((parent, view1, pos, id) -> {
            mContactPicked = true;
            mUnsaved = pos == 0;
            if (!mUnsaved) {
                ContactItem currentContact = contactList.get(contactNames.indexOf(holder.binding.contactMenuAutoCompleteTextView.getText().toString())-1);
                if (currentContact.getContactImage() != null) {
                    Picasso.get().load(currentContact.getContactImage()).transform(new CircleTransform()).into(holder.binding.contactImage);
                } else if (PreferenceUtils.showTiles()) {
                    holder.binding.contactImage.setImageDrawable(currentContact.getContactDrawable(mContext));
                } else {
                    holder.binding.contactImage.setImageResource(R.drawable.ic_default_contact);
                }
            } else {
                holder.binding.contactImage.setImageResource(R.drawable.ic_default_contact);
            }

            runReCheck.runrecheck();
        });

        holder.binding.batchDeleteDateCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            holder.binding.dateFilterLayout.setVisibility(isChecked ? ViewGroup.VISIBLE : ViewGroup.GONE);
            onCheckboxChanged.onCheckboxChanged(isChecked);
        });

        holder.binding.radioSingleDate.setOnClickListener(onClickListeners::onSingleDateClick);
        holder.binding.radioRangeDate.setOnClickListener(onClickListeners::onRangeDateClick);
        holder.binding.pickDateButton.setOnClickListener(onClickListeners::onPickDateClick);


        holder.binding.batchDeleteDirectionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> onCheckboxChanged.onCheckboxChanged(isChecked));

        holder.binding.directionMenuAutoCompleteTextView.setOnItemClickListener((parent, view1, pos, id) -> {
            switch (pos) {
                case 0 -> mDirection = "in";
                case 1 -> mDirection = "out";
                case 2 -> mDirection = "";
            }
            Log.d("BatchDelete", "poition: " + position + " onItemClick: " + mDirection);
            runReCheck.runrecheck();
        });

        holder.binding.batchDeleteDurationCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            holder.binding.durationFilterLayout.setVisibility(isChecked ? ViewGroup.VISIBLE : ViewGroup.GONE);
            holder.binding.durationSlider.setVisibility(isChecked ? ViewGroup.VISIBLE : ViewGroup.GONE);
            onCheckboxChanged.onCheckboxChanged(isChecked);
        });

        holder.binding.durationSlider.setValueFrom(0.0f);
        holder.binding.durationSlider.setValueTo((float) mMaxDuration);
        holder.binding.durationSlider.setValues((float) mStartDuration, (float) mEndDuration);
        holder.binding.durationSlider.setLabelFormatter(value -> {
            int minutes = (int) (value / 60);
            int seconds = (int) (value % 60);
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        });
        holder.binding.durationStartText.setText(DateUtils.formatDuration((long) mStartDuration));
        holder.binding.durationEndText.setText(DateUtils.formatDuration((long) mEndDuration));

        holder.binding.durationSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                List<Float> values = slider.getValues();
                mStartDuration = values.get(0);
                mEndDuration = values.get(1);
                holder.binding.durationStartText.setText(DateUtils.formatDuration((long) mStartDuration));
                holder.binding.durationEndText.setText(DateUtils.formatDuration((long) mEndDuration));
                runReCheck.runrecheck();
            }
        });

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final FilterBatchDeleteBinding binding;

        public ViewHolder(@NonNull FilterBatchDeleteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
