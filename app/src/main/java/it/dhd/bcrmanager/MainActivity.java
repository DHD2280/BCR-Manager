package it.dhd.bcrmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentTransaction;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import it.dhd.bcrmanager.databinding.ActivityMainBinding;
import it.dhd.bcrmanager.handler.UncaughtExceptionHandler;
import it.dhd.bcrmanager.ui.fragments.BatchDelete;
import it.dhd.bcrmanager.ui.fragments.NewHome;
import it.dhd.bcrmanager.ui.fragments.PermissionsFragment;
import it.dhd.bcrmanager.ui.fragments.Settings;
import it.dhd.bcrmanager.utils.BetterActivityResult;
import it.dhd.bcrmanager.utils.BreakpointUtils;
import it.dhd.bcrmanager.utils.LogUtil;
import it.dhd.bcrmanager.utils.PermissionsUtil;
import it.dhd.bcrmanager.utils.PreferenceUtils;
import it.dhd.bcrmanager.utils.ThemeUtils;

public class MainActivity extends AppCompatActivity {

    public static CharSequence searchQuery;
    public static boolean mHideMenu;
    protected final BetterActivityResult<Intent, ActivityResult> activityLauncher = BetterActivityResult.registerActivityForResult(this);
    public String mDir = "";

    private static Context context;
    public SearchView searchView;
    public TextView textFilterItems;
    public static MenuItem searchItem;
    public static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DynamicColors.applyToActivityIfAvailable(this);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(getApplicationContext()));

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        PreferenceUtils.init(getApplicationContext());
        PermissionsUtil.init(getApplicationContext());
        ThemeUtils.init(this);
        BreakpointUtils.init(getApplicationContext());

        checkBcr();
        checkBcrDirectory();
        setupBadge(-1);

        context = getApplicationContext();

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
            if (savedInstanceState == null && !mDir.equals("")) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new NewHome(), NewHome.class.getSimpleName())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();

            }
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mHideMenu = true;
                Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
                MainActivity.super.supportInvalidateOptionsMenu();
                MainActivity.super.invalidateOptionsMenu();
                MainActivity.super.invalidateMenu();
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStackImmediate();
                } else {
                    finish();
                }
            }
        });

    }

    public static void restartActivity() {
        Context ctx = getAppContext();
        PackageManager pm = ctx.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(ctx.getPackageName());
        Intent mainIntent = null;
        if (intent != null) {
            mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        }
        ctx.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        super.supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mHideMenu) {
            menu.findItem(R.id.menu_search).setVisible(false);
            menu.findItem(R.id.batch_delete).setVisible(false);
            menu.findItem(R.id.menu_filter).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(false);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        } else {
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.batch_delete).setVisible(true);
            menu.findItem(R.id.menu_filter).setVisible(true);
            menu.findItem(R.id.menu_settings).setVisible(true);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        final MenuItem filterItem = menu.findItem(R.id.menu_filter);
        View actionView = filterItem.getActionView();
        assert actionView != null;
        textFilterItems = actionView.findViewById(R.id.cart_badge);
        actionView.setOnClickListener(v -> onOptionsItemSelected(filterItem));
        searchItem = menu.findItem(R.id.menu_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem menuItem) {
                TransitionManager.beginDelayedTransition(findViewById(R.id.toolbar), new Slide(Gravity.END));
                return true;
            }
        });
            searchView = (SearchView) searchItem.getActionView();


            assert searchView != null;
            searchView.setQueryHint(getString(R.string.search));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    LogUtil.i("MainActivity.onQueryTextSubmit", "Query: " + query);
                    if (NewHome.callLogAdapter != null) NewHome.callLogAdapter.getFilter().filter(query);
                    //if (NewHome.callStarredLogAdapter != null) NewHome.callStarredLogAdapter.getFilterStarred().filter(query);
                    searchQuery = query;
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    LogUtil.i("MainActivity.onQueryTextChange", "Query: " + newText);
                    if (NewHome.callLogAdapter != null) NewHome.callLogAdapter.getFilter().filter(newText);
                    if (NewHome.callStarredLogAdapter != null) NewHome.callStarredLogAdapter.getFilterStarred().filter(newText);
                    //if (customAdapter != null) customAdapter.getFilter().filter(newText);
                    searchQuery = newText;
                    return false;
                }
            });

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
        } else if (itemId == R.id.menu_search) {
            TransitionManager.beginDelayedTransition(this.findViewById(R.id.toolbar), new Slide(Gravity.START));
            return true;
        } else if(itemId == R.id.menu_filter) {
            NewHome newHome = (NewHome) getSupportFragmentManager().findFragmentByTag(NewHome.class.getSimpleName());
            if (newHome != null) newHome.showFilterDialog();
        } else if (itemId == R.id.batch_delete) {
            BatchDelete batchDelete = (BatchDelete) getSupportFragmentManager().findFragmentByTag(BatchDelete.class.getSimpleName());
            if (batchDelete == null && !NewHome.isRunning) {
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
            if (settings == null && !NewHome.isRunning) {
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

    public static Context getAppContext() {
        return context;
    }

    /**
     * Check if the bcr directory is set
     */
    private void checkBcrDirectory() {
        mDir = PreferenceUtils.getStoredFolderFromPreference();
        Log.d("BCR", "Directory: " + mDir);
        Log.d("BCR", "Directory: " + Environment.getExternalStorageDirectory().getAbsolutePath());
        if (mDir.equals("")) {
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
            //builder.setOnDismissListener(dialog -> finish());
            builder.setPositiveButton(R.string.bcr_not_installed_positive, (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/chenxiaolong/BCR"));
                //startActivity(browserIntent);
            });
            builder.show();
        }
    }

    /**
     * Setup the badge
     * @param items The number of items
     */
    public void setupBadge(int items) {

        if (textFilterItems != null) {
            if (items == -1) {
                if (textFilterItems.getVisibility() != View.GONE) {
                    textFilterItems.setVisibility(View.GONE);
                }
            } else {
                textFilterItems.setText(String.valueOf(Math.min(items, 999)));
                if (textFilterItems.getVisibility() != View.VISIBLE) {
                    textFilterItems.setVisibility(View.VISIBLE);
                }
            }
        }
    }


}