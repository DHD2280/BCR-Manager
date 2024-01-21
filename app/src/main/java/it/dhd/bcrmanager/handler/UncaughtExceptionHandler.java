package it.dhd.bcrmanager.handler;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

import it.dhd.bcrmanager.ui.activity.ErrorReportActivity;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public UncaughtExceptionHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable exception) {
        try {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));

            Intent intent = new Intent(context, ErrorReportActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("error_message", "Woops. An unexpected error occurred");
            intent.putExtra("stack_trace", sw.toString());

            // Start the new activity directly without using startActivity()
            if (context instanceof AppCompatActivity) {
                context.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(1);
        }
    }
}
