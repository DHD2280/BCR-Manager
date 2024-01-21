package it.dhd.bcrmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import it.dhd.bcrmanager.databinding.ActivityMainBinding;
import it.dhd.bcrmanager.handler.UncaughtExceptionHandler;
import it.dhd.bcrmanager.ui.fragments.BatchDelete;
import it.dhd.bcrmanager.ui.fragments.Home;
import it.dhd.bcrmanager.ui.fragments.PermissionsFragment;
import it.dhd.bcrmanager.ui.fragments.settings.Settings;
import it.dhd.bcrmanager.utils.BetterActivityResult;
import it.dhd.bcrmanager.utils.LogUtil;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;

public class MainActivity extends AppCompatActivity {

    public static boolean mHideMenu;
    protected final BetterActivityResult<Intent, ActivityResult> activityLauncher = BetterActivityResult.registerActivityForResult(this);
    public String mDir = "";

    public static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceUtils.init(getApplicationContext());
        SharedPreferences prefs = PreferenceUtils.getAppPreferences();
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.getDarkTheme());
        if (prefs.getBoolean(PreferenceUtils.Keys.PREFS_KEY_DYNAMIC_COLOR, true)) {
            DynamicColors.applyToActivityIfAvailable(this);
            DynamicColors.applyToActivitiesIfAvailable(getApplication());
        } else {
            getTheme().applyStyle(ThemeUtils.getColorThemeStyleRes(), true);
        }
        active = true;

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(getApplicationContext()));

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        PermissionsUtil.init(getApplicationContext());

        checkBcr();
        checkBcrDirectory();

        if (BuildConfig.DEBUG)
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build());

        setSupportActionBar(findViewById(R.id.toolbar));
        setTitle(R.string.app_name);

        if (PreferenceUtils.isFirstTime()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new PermissionsFragment(), PermissionsFragment.class.getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } else {
            if (savedInstanceState == null && !TextUtils.isEmpty(mDir)) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new Home(), Home.class.getSimpleName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();

            }
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStackImmediate();
                } else {
                    finish();
                }
            }
        });

    }

    @Override
    public void supportInvalidateOptionsMenu() {
        super.supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mHideMenu) {
            menu.findItem(R.id.batch_delete).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(false);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        } else {
            menu.findItem(R.id.batch_delete).setVisible(true);
            menu.findItem(R.id.menu_settings).setVisible(true);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            super.getOnBackPressedDispatcher().onBackPressed();
            if (getSupportFragmentManager().getBackStackEntryCount() == 0 ) mHideMenu = false;
            supportInvalidateOptionsMenu();
            return true;
        }  else if (itemId == R.id.batch_delete) {
            BatchDelete batchDelete = (BatchDelete) getSupportFragmentManager().findFragmentByTag(BatchDelete.class.getSimpleName());
            if (batchDelete == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new BatchDelete())
                        .addToBackStack(BatchDelete.class.getSimpleName())
                        .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                mHideMenu = true;
                supportInvalidateOptionsMenu();
                Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            }
        } else if (itemId == R.id.menu_settings) {
            Settings settings = (Settings) getSupportFragmentManager().findFragmentByTag(Settings.class.getSimpleName());
            if (settings == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new Settings())
                        .addToBackStack(Settings.class.getSimpleName())
                        .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
                mHideMenu = true;
                supportInvalidateOptionsMenu();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check if the bcr directory is set
     */
    private void checkBcrDirectory() {
        mDir = PreferenceUtils.getStoredFolderFromPreference();
        Log.d("BCR", "Directory: " + mDir);
        if (TextUtils.isEmpty(mDir)) {
            LogUtil.i("MainActivity.checkBcrDirectory", "No directory set");
            // set bcr directory
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.bcr_directory_title);
            builder.setMessage(R.string.bcr_directory_message);
            builder.setPositiveButton(R.string.bcr_directory_positive, (dialog, which) -> openSomeActivityForResult());
            builder.show();
        }
    }

    /**
     * Open the directory chooser
     **/
    public void openSomeActivityForResult() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activityLauncher.launch(intent, result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                assert data != null;
                Uri treeUri = data.getData();
                // Persist permissions if you need to access it later
                assert treeUri != null;
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);


                mDir = treeUri.toString();
                PreferenceUtils.saveFolder(mDir);
                Log.d("BCR", "Directory set to " + mDir);
            }
        });
    }

    /**
     * Check if BCR is installed
     */
    private void checkBcr() {
        PackageManager pm = getPackageManager();
        boolean bcrInstalled = false;
        try {
            pm.getPackageInfo("com.chiller3.bcr", PackageManager.GET_ACTIVITIES);
            bcrInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            // not installed
        }
        if (!bcrInstalled) {
            // install bcr
            // https://github.com/chenxiaolong/BCR
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.bcr_not_installed_title);
            builder.setMessage(R.string.bcr_not_installed_message);
            builder.setPositiveButton(R.string.bcr_not_installed_positive, (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chenxiaolong/BCR"));
                startActivity(browserIntent);
            });
            builder.show();
        }
    }


}