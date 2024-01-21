package it.dhd.bcrmanager.callbacks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.ui.adapters.CallLogAdapter;

abstract public class SwipeCallback extends ItemTouchHelper.Callback {

    Context mContext;
    private final Paint mClearPaint;
    private final ColorDrawable mBackground;
    private final int backgroundColor;
    private final Drawable starDrawable;
    private final int intrinsicWidth;
    private final int intrinsicHeight;

    public @interface SwipeType {}

    /** Contact type constants */
    public static final int TYPE_STAR = 1;

    public static final int TYPE_DELETE = 2;


    public SwipeCallback(Context context, @SwipeType int swipeType) {
        mContext = context;
        mBackground = new ColorDrawable();
        if (swipeType == TYPE_DELETE)
            backgroundColor = Color.parseColor("#B80F0A");
        else
            backgroundColor = Color.parseColor("#FFC107");
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        if (swipeType == TYPE_DELETE)
            starDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_delete);
        else
            starDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_star);
        intrinsicWidth = Objects.requireNonNull(starDrawable).getIntrinsicWidth();
        intrinsicHeight = starDrawable.getIntrinsicHeight();
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof CallLogAdapter.CallLogViewHolder)
            return makeMovementFlags(0, ItemTouchHelper.LEFT);
        return 0;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();

        boolean isCancelled = dX == 0 && !isCurrentlyActive;

        if (isCancelled) {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        mBackground.setColor(backgroundColor);
        mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        mBackground.draw(c);

        int starIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int starIconMargin = (itemHeight - intrinsicHeight) / 2;
        int starIconLeft = itemView.getRight() - starIconMargin - intrinsicWidth;
        int starIconRight = itemView.getRight() - starIconMargin;
        int starIconBottom = starIconTop + intrinsicHeight;


        starDrawable.setBounds(starIconLeft, starIconTop, starIconRight, starIconBottom);
        starDrawable.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);


    }

    private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
        c.drawRect(left, top, right, bottom, mClearPaint);

    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.7f;
    }
}

