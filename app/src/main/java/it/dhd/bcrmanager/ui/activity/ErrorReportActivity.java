package it.dhd.bcrmanager.ui.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import it.dhd.bcrmanager.R;
import it.dhd.bcrmanager.databinding.ErrorReportBinding;

public class ErrorReportActivity extends AppCompatActivity {

    private static final int CREATE_FILE_REQUEST_CODE = 999;

    private ErrorReportBinding binding;
    private String stackTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ErrorReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationIcon(android.R.drawable.ic_dialog_alert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(R.string.error_report_title);
        binding.toolbar.setSubtitle(R.string.error_report_message);
        binding.saveToFile.setOnClickListener(v -> {
            openFilePicker();
        });
        handleUncaughtException();
    }

    // Inizializza il launcher per l'Intent.ACTION_CREATE_DOCUMENT
    private final ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
            result -> handleCreateDocumentResult(result.getData())
    );

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        Date d = new Date();
        CharSequence s  = DateFormat.format("yyyy_MM_dd_HH_mm_ss ", d.getTime());
        intent.putExtra(Intent.EXTRA_TITLE, "BCRManager_error_report_" + s + ".txt");

        createDocumentLauncher.launch(intent);
    }

    private void handleCreateDocumentResult(Intent data) {
        if (data != null) {
            Uri uri = data.getData();
            try {
                ContentResolver contentResolver = getContentResolver();

                assert uri != null;
                try (OutputStream outputStream = contentResolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(stackTrace.getBytes());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUncaughtException() {
        String error = getIntent().getStringExtra("error_message");
        stackTrace = getIntent().getStringExtra("stack_trace");

        setView(error, stackTrace);
    }

    private void setView(String error, String stackTrace) {
        binding.errorReportText.setText(Html.fromHtml("<h2><font color=\"#FF0000\">" + error + "</h2><br><p> " + stackTrace + "</p>", Html.FROM_HTML_MODE_COMPACT));
        binding.errorReportText.setMovementMethod(new ScrollingMovementMethod());
    }

}
