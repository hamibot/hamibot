package com.hamibot.hamibot.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hamibot.hamibot.notification.NotificationUtil;
import com.stardust.app.FragmentPagerAdapterBuilder;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback;
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity;
import com.stardust.autojs.core.permission.RequestPermissionCallbacks;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.pio.PFiles;
import com.stardust.theme.ThemeColorManager;
import com.stardust.util.BackPressedHandler;
import com.stardust.util.DeveloperUtils;
import com.stardust.util.DrawerAutoClose;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import com.hamibot.hamibot.BuildConfig;
import com.hamibot.hamibot.Pref;
import com.hamibot.hamibot.R;
import com.hamibot.hamibot.autojs.AutoJs;
import com.hamibot.hamibot.external.foreground.ForegroundService;
import com.hamibot.hamibot.model.explorer.Explorers;
import com.hamibot.hamibot.services.CommandService;
import com.hamibot.hamibot.tool.AccessibilityServiceTool;
import com.hamibot.hamibot.ui.BaseActivity;
import com.hamibot.hamibot.ui.common.NotAskAgainDialog;
import com.hamibot.hamibot.ui.doc.DocsFragment_;
import com.hamibot.hamibot.ui.log.LogActivity_;
import com.hamibot.hamibot.ui.main.community.CommunityFragment;
import com.hamibot.hamibot.ui.main.community.CommunityFragment_;
import com.hamibot.hamibot.ui.main.scripts.MyScriptListFragment_;
import com.hamibot.hamibot.ui.main.task.TaskManagerFragment_;
import com.hamibot.hamibot.ui.settings.SettingsActivity_;
import com.hamibot.hamibot.ui.widget.CommonMarkdownView;
import com.hamibot.hamibot.ui.widget.SearchViewItem;
import com.stardust.util.IntentUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost, BackPressedHandler.HostActivity, PermissionRequestProxyActivity {

    public static class DrawerOpenEvent {
        static DrawerOpenEvent SINGLETON = new DrawerOpenEvent();
    }

    private static final String LOG_TAG = "[h4m1][MainActivity]";
    private String tempInput = "";

    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @ViewById(R.id.viewpager)
    ViewPager mViewPager;

    @ViewById(R.id.fab)
    FloatingActionButton mFab;

    // 绑定相关按钮
    @ViewById(R.id.bind)
    TextView mBindButton;
    @ViewById(R.id.unbind)
    TextView mUnbindButton;
    @ViewById(R.id.bind_code)
    TextView mBindCode;

    private FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter mPagerAdapter;
    private OnActivityResultDelegate.Mediator mActivityResultMediator = new OnActivityResultDelegate.Mediator();
    private RequestPermissionCallbacks mRequestPermissionCallbacks = new RequestPermissionCallbacks();
    // private VersionGuard mVersionGuard;
    private BackPressedHandler.Observer mBackPressObserver = new BackPressedHandler.Observer();
    private SearchViewItem mSearchViewItem;
    private MenuItem mLogMenuItem;
    private boolean mDocsSearchItemExpanded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        showAccessibilitySettingPromptIfDisabled();
        // mVersionGuard = new VersionGuard(this); // 版本检查
        // showAnnunciationIfNeeded(); 不显示声明（本软件为免费软件）
        EventBus.getDefault().register(this);
        //初始化通知
        NotificationUtil.init(this);
        //applyDayNightMode(); // 应用夜间模式
    }

    @AfterViews
    void setUpViews() {
        setUpToolbar();
        //setUpTabViewPager();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        registerBackPressHandlers();
        ThemeColorManager.addViewBackground(findViewById(R.id.app_bar));
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                EventBus.getDefault().post(DrawerOpenEvent.SINGLETON);
            }
        });
    }

    private void showAnnunciationIfNeeded() {
        if (!Pref.shouldShowAnnunciation()) {
            return;
        }
        new CommonMarkdownView.DialogBuilder(this)
                .padding(36, 0, 36, 0)
                .markdown(PFiles.read(getResources().openRawResource(R.raw.annunciation)))
                .title(R.string.text_annunciation)
                .positiveText(R.string.ok)
                .canceledOnTouchOutside(false)
                .show();
    }


    private void registerBackPressHandlers() {
        mBackPressObserver.registerHandler(new DrawerAutoClose(mDrawerLayout, Gravity.START));
        mBackPressObserver.registerHandler(new BackPressedHandler.DoublePressExit(this, R.string.text_press_again_to_exit));
    }

    private void checkPermissions() {
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void showAccessibilitySettingPromptIfDisabled() {
        if (AccessibilityServiceTool.isAccessibilityServiceEnabled(this)) {
            return;
        }
        new NotAskAgainDialog.Builder(this, "MainActivity.accessibility")
                .title(R.string.text_need_to_enable_accessibility_service)
                .content(R.string.explain_accessibility_permission)
                .positiveText(R.string.text_go_to_setting)
                .negativeText(R.string.text_cancel)
                .onPositive((dialog, which) ->
                        AccessibilityServiceTool.enableAccessibilityService()
                ).show();
    }

    private void setUpToolbar() {
        Toolbar toolbar = $(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_drawer_open,
                R.string.text_drawer_close);
        drawerToggle.syncState();
        mDrawerLayout.addDrawerListener(drawerToggle);
    }

    private void setUpTabViewPager() {
        mPagerAdapter = new FragmentPagerAdapterBuilder(this)
                .build();
        mViewPager.setAdapter(mPagerAdapter);
        setUpViewPagerFragmentBehaviors();
    }

    private void setUpViewPagerFragmentBehaviors() {
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private ViewPagerFragment mPreviousFragment;

            @Override
            public void onPageSelected(int position) {
                Fragment fragment = mPagerAdapter.getStoredFragment(position);
                if (fragment == null)
                    return;
                if (mPreviousFragment != null) {
                    mPreviousFragment.onPageHide();
                }
                mPreviousFragment = (ViewPagerFragment) fragment;
                mPreviousFragment.onPageShow();
            }
        });
    }

    @Click(R.id.exit)
    public void exitCompletely() {
        finish();
        ForegroundService.stop(this);
        stopService(new Intent(this, FloatyService.class));
        AutoJs.getInstance().getScriptEngineService().stopAll();
    }

    @Click(R.id.button)
    void button() {
        IntentUtil.browse(this, "https://hamibot.com/dashboard/robots");
    }
    @Click(R.id.textView4)
    void textView4() {
        IntentUtil.browse(this, "https://hamibot.com/guide");
    }

    private void setUpBindCodeGroup() {
        String token = Pref.getToken();
        if ("".equals(token)) {
            // 未绑定
            mBindButton.setVisibility(View.VISIBLE);
            mUnbindButton.setVisibility(View.GONE);
            mBindCode.setVisibility(View.GONE);
        } else {
            // 已绑定
            String name = Pref.getRobotName();
            mBindCode.setText(name);
            mBindButton.setVisibility(View.GONE);
            mUnbindButton.setVisibility(View.VISIBLE);
            mBindCode.setVisibility(View.VISIBLE);
        }
    }

    @Click(R.id.bind)
    void bind() {
        new MaterialDialog.Builder(this)
                .title(R.string.text_pair_code)
                .input("", tempInput, (dialog, input) -> {
                    String passcode = input.toString();
                    tempInput = passcode;
                    int length = passcode.length();
                    if (length > 0) {
                        if (length == 6) {
                            JSONObject json = new JSONObject();
                            try {
                                json.put("passcode", passcode);
                                EventBus.getDefault().post(new CommandService.MessageEvent("a:pair:pairing", json));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, "配对码失效或不存在", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .neutralText(R.string.text_help)
                .onNeutral((dialog, which) -> {
                    IntentUtil.browse(this, "https://hamibot.com/guide");
                })
                .cancelListener(dialog -> {})
                .show();
    }

    @Click(R.id.unbind)
    void unbind() {
        new NotAskAgainDialog.Builder(this)
                .title(R.string.text_unpair)
                .content(R.string.text_unpair_confirm)
                .positiveText(R.string.text_unpair)
                .negativeText(R.string.text_cancel)
                .onPositive((dialog, which) -> {
                            JSONObject json = new JSONObject();
                            EventBus.getDefault().post(new CommandService.MessageEvent("a:pair:unpair", json));
                        }
                ).show();
    }

    public static class BindEvent {
        public final String status;
        public final String message;

        public BindEvent(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBindResult(BindEvent event) {
        if ("success".equals(event.status)) {
            Toast.makeText(this, event.message, Toast.LENGTH_LONG).show();
            setUpBindCodeGroup();
            tempInput = "";
        } else {
            Toast.makeText(this, event.message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultMediator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mRequestPermissionCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return;
        }
        if (getGrantResult(Manifest.permission.READ_EXTERNAL_STORAGE, permissions, grantResults) == PackageManager.PERMISSION_GRANTED) {
            Explorers.workspace().refreshAll();
        }
    }

    private int getGrantResult(String permission, String[] permissions, int[] grantResults) {
        int i = Arrays.asList(permissions).indexOf(permission);
        if (i < 0) {
            return 2;
        }
        return grantResults[i];
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart()");
        if (!BuildConfig.DEBUG) {
            DeveloperUtils.verifyApk(this, R.string.dex_crcs);
        }
        setUpBindCodeGroup();
        this.startService(new Intent(this, CommandService.class));
    }

    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mActivityResultMediator;
    }

    @Override
    public void addRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        mRequestPermissionCallbacks.addCallback(callback);
    }

    @Override
    public boolean removeRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        return mRequestPermissionCallbacks.removeCallback(callback);
    }

    @Override
    public BackPressedHandler.Observer getBackPressedObserver() {
        return mBackPressObserver;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mLogMenuItem = menu.findItem(R.id.action_log);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_log) {
            if (mDocsSearchItemExpanded) {
                submitForwardQuery();
            } else {
                LogActivity_.intent(this).start();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onLoadUrl(CommunityFragment.LoadUrl loadUrl) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void setUpSearchMenuItem(MenuItem searchMenuItem) {
        mSearchViewItem = new SearchViewItem(this, searchMenuItem) {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (mViewPager.getCurrentItem() == 1) {
                    mDocsSearchItemExpanded = true;
                    mLogMenuItem.setIcon(R.drawable.ic_ali_up);
                }
                return super.onMenuItemActionExpand(item);
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (mDocsSearchItemExpanded) {
                    mDocsSearchItemExpanded = false;
                    mLogMenuItem.setIcon(R.drawable.ic_ali_log);
                }
                return super.onMenuItemActionCollapse(item);
            }
        };
        mSearchViewItem.setQueryCallback(this::submitQuery);
    }

    private void submitQuery(String query) {
        if (query == null) {
            EventBus.getDefault().post(QueryEvent.CLEAR);
            return;
        }
        QueryEvent event = new QueryEvent(query);
        EventBus.getDefault().post(event);
        if (event.shouldCollapseSearchView()) {
            mSearchViewItem.collapse();
        }
    }

    private void submitForwardQuery() {
        QueryEvent event = QueryEvent.FIND_FORWARD;
        EventBus.getDefault().post(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}