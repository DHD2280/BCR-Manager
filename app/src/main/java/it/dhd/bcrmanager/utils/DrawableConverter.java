package it.dhd.bcrmanager.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class DrawableConverter {

    private DrawableConverter() {}

    /** Converts the provided drawable to a bitmap using the drawable's intrinsic width and height. */
    @Nullable
    public static Bitmap drawableToBitmap(@Nullable Drawable drawable) {
        return drawableToBitmap(drawable, 0, 0);
    }

    /**
     * Converts the provided drawable to a bitmap with the specified width and height.
     *
     * <p>If both width and height are 0, the drawable's intrinsic width and height are used (but in
     * that case {@link #drawableToBitmap(Drawable)} should be used).
     */
    @Nullable
    public static Bitmap drawableToBitmap(@Nullable Drawable drawable, int width, int height) {
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            if (width > 0 || height > 0) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } else if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                // Needed for drawables that are just a colour.
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            } else {
                bitmap =
                        Bitmap.createBitmap(
                                drawable.getIntrinsicWidth(),
                                drawable.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);
            }

            LogUtil.i(
                    "DrawableConverter.drawableToBitmap",
                    "created bitmap with width: %d, height: %d",
                    bitmap.getWidth(),
                    bitmap.getHeight());

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }
}
