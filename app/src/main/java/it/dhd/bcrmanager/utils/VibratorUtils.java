package it.dhd.bcrmanager.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class VibratorUtils {

    public static void vibrate(Context c, int intensity) {
        Vibrator vib;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vib = vibratorManager.getDefaultVibrator();
        } else {
            vib = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (PreferenceUtils.hasVibrate())
            vib.vibrate(VibrationEffect.createOneShot(intensity, VibrationEffect.DEFAULT_AMPLITUDE));
    }

}
