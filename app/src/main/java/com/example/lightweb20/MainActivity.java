package com.example.lightweb20;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.lightweb20.ui.DownloadHistory;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String GOOGLE_HOME = "https://www.google.com";
    public static final String HOMEPAGE_URL = "file:///android_asset/index.html";
    public static final String EXCEPTION_URL = "https://abehiroshi.la.coocan.jp/";
    private static final String PREFS_NAME = "appPreferences";
    private static final int REQUEST_PERMISSION = 1;
    private static final int MENU_TOGGLE_JS = 101;

    // UI
    private WebView webView;
    private EditText urlInput;
    private Button goButton;
    private ImageButton backButton, forwardButton, toolbarMenuButton;
    private ImageButton refreshButton, homeButton;
    private LinearLayout secureWarningLayout;
    private ImageButton warningIcon;
    private MaterialCardView floatingToolbar;
    private MaterialCardView urlOverlay;
    private ImageButton searchButton;
    private ImageButton closeSearchButton;
    private ProgressBar progressBar;

    // menuOverlay 内のダウンロードボタンを参照
    private MaterialCardView menuOverlay;
    private ImageButton menuDownloadButton;
    private ImageButton menuSettingsButton;
    // 追加：PCモード用ボタン（menu内）
    private ImageButton pcModeButtonMenu;

    // WebView full-screen
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    // prefs
    private SharedPreferences preferences;
    private boolean jsEnabled;
    private boolean googleLiteEnabled;

    // keyboard
    private InputMethodManager imm;

    // toolbar sliding
    private int toolbarTouchSlop;
    private boolean toolbarCompact = false;
    private static final int TOGGLE_THRESHOLD_DP = 80;

    // PC mode state & original UA
    private boolean isPcMode = false;
    private String originalUserAgent = null;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 安全ハンドラ：未捕捉例外の簡易フィードバック
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "Uncaught exception", throwable);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MainActivity.this, "不明なエラーが発生しました（自動報告済）", Toast.LENGTH_LONG).show()
                );
            } catch (Exception ignored) {}
            // その後はデフォルト処理に任せる（完全回復は困難）
            try {
                Thread.sleep(1200);
            } catch (InterruptedException ignored) {}
            System.exit(2);
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // try/catch で onCreate の主要処理を保護（落ちたときにフィードバックを出すため）
        try {
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            jsEnabled = preferences.getBoolean("jsEnabled", true);
            googleLiteEnabled = preferences.getBoolean("googleLiteEnabled", false);
            Object svc = getSystemService(Context.INPUT_METHOD_SERVICE);
            imm = (svc instanceof InputMethodManager) ? (InputMethodManager) svc : null;

            initViews();
            toolbarTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
            registerForContextMenu(webView);
            configureWebView();
            // originalUserAgent を確保（configureWebView 内で上書き可能だが念のためここで取得）
            try {
                if (webView != null && webView.getSettings() != null) {
                    originalUserAgent = webView.getSettings().getUserAgentString();
                }
            } catch (Exception ignored) {}

            requestPermissions();
            if (isLargeScreen()) enableDesktopMode();
            loadLastUrl();
            setupControls();
            setupFloatingToolbarSliding();
        } catch (Exception e) {
            reportError("onCreate", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (webView != null) {
                WebSettings ws = webView.getSettings();
                if (!ws.getJavaScriptEnabled()) {
                    ws.setJavaScriptEnabled(true);
                    preferences.edit().putBoolean("jsEnabled", true).apply();
                    jsEnabled = true;
                }
                ws.setDomStorageEnabled(true);
                ws.setAllowUniversalAccessFromFileURLs(true);
                ws.setAllowFileAccess(true);
            }
        } catch (Exception e) {
            reportError("onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (webView != null) {
                String current = webView.getUrl();
                if (current != null) preferences.edit().putString("lastUrl", current).apply();
            }
        } catch (Exception e) {
            Log.w(TAG, "onPause: save lastUrl failed", e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // 以前の実装であったダウンロードフローティング関係は削除済み
        } catch (Exception ignored) {}
        try {
            if (webView != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
                webView.removeAllViews();
                webView.destroy();
                webView = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "onDestroy: webView destroy error", e);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_TOGGLE_JS, 0, jsEnabled ? "JS: 無効" : "JS: 有効");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == MENU_TOGGLE_JS) {
            jsEnabled = !jsEnabled;
            if (webView != null) webView.getSettings().setJavaScriptEnabled(jsEnabled);
            preferences.edit().putBoolean("jsEnabled", jsEnabled).apply();
            item.setTitle(jsEnabled ? "JS: 無効" : "JS: 有効");
            Toast.makeText(this, "JavaScript: " + (jsEnabled ? "有効" : "無効"), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Context menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);
        try {
            WebView.HitTestResult hit = webView.getHitTestResult();
            if (hit == null) return;
            int type = hit.getType();
            if (type == WebView.HitTestResult.IMAGE_TYPE) {
                menu.setHeaderTitle("画像オプション");
                menu.add(0, 1, 0, "画像をダウンロード");
                menu.add(0, 2, 1, "画像を共有");
            } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                menu.setHeaderTitle("リンクオプション");
                menu.add(0, 3, 0, "リンクをコピー");
                menu.add(0, 4, 1, "リンクを共有");
            }
        } catch (Exception e) {
            reportError("onCreateContextMenu", e);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {
            WebView.HitTestResult hit = webView.getHitTestResult();
            if (hit == null) return super.onContextItemSelected(item);
            String extra = hit.getExtra();
            switch (item.getItemId()) {
                case 1:
                    showDownloadConfirmation(extra, null, null, null, 0, URLUtil.guessFileName(extra, null, null));
                    return true;
                case 2:
                    shareText("画像を共有", extra);
                    return true;
                case 3:
                    copyToClipboard("リンク", extra);
                    return true;
                case 4:
                    shareText("リンクを共有", extra);
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        } catch (Exception e) {
            reportError("onContextItemSelected", e);
            return super.onContextItemSelected(item);
        }
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        goButton = findViewById(R.id.goButton);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        // toolbar の「3点メニュー」ボタン（ツールバー上）
        toolbarMenuButton = findViewById(R.id.menuButton);
        homeButton = findViewById(R.id.homeButton);
        secureWarningLayout = findViewById(R.id.secureWarningLayout);
        warningIcon = findViewById(R.id.warningIcon);
        progressBar = findViewById(R.id.progressBar);
        floatingToolbar = findViewById(R.id.floatingToolbar);
        urlOverlay = findViewById(R.id.urlOverlay);
        searchButton = findViewById(R.id.searchButton);
        closeSearchButton = findViewById(R.id.closeSearchButton);

        // menuOverlay と内部ボタン
        menuOverlay = findViewById(R.id.menuOverlay);
        menuDownloadButton = findViewById(R.id.menu_download);
        menuSettingsButton = findViewById(R.id.menu_settings);
        // 追加：PCモードボタン取得（存在しない場合は null になる）
        pcModeButtonMenu = findViewById(R.id.pcModeButton_menu);

        if (webView == null) {
            throw new IllegalStateException("activity_main.xml に webView (id=webView) が見つかりません。");
        }
    }

    private void configureWebView() {
        try {
            WebSettings ws = webView.getSettings();
            if (!ws.getJavaScriptEnabled()) {
                ws.setJavaScriptEnabled(true);
                preferences.edit().putBoolean("jsEnabled", true).apply();
                jsEnabled = true;
            }
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setAllowContentAccess(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ws.setAllowUniversalAccessFromFileURLs(true);
            }
            ws.setJavaScriptCanOpenWindowsAutomatically(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            ws.setSupportMultipleWindows(true);
            ws.setSupportZoom(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);

            // 混在コンテンツとサードパーティ Cookie 設定
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                } catch (Exception ignored) {}
            }
            try {
                CookieManager cm = CookieManager.getInstance();
                cm.setAcceptCookie(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cm.setAcceptThirdPartyCookies(webView, true);
                }
            } catch (Exception ignored) {}

            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String ua, String cd, String mime, long len) {
                    try {
                        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
                        if (ext == null) ext = "";
                        String extLower = ext.toLowerCase();
                        String guessedMime = mime;
                        if ((guessedMime == null || guessedMime.isEmpty()) && !extLower.isEmpty()) {
                            String fromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extLower);
                            if (fromExt != null) guessedMime = fromExt;
                        }
                        if (guessedMime == null) guessedMime = "";
                        String fn = URLUtil.guessFileName(url, cd, guessedMime);
                        showDownloadConfirmation(url, ua, cd, guessedMime, len, fn);
                    } catch (Exception e) {
                        reportError("downloadListener", e);
                    }
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView v, String url, Bitmap f) {
                    try {
                        String display = url != null && url.equals(HOMEPAGE_URL) ? (googleLiteEnabled ? GOOGLE_HOME : "ホーム") : url;
                        if (urlInput != null) urlInput.setText(display);
                        if (url != null && url.startsWith("http://") && !url.equals(EXCEPTION_URL)) {
                            if (secureWarningLayout != null) secureWarningLayout.setVisibility(View.VISIBLE);
                            if (warningIcon != null) warningIcon.setOnClickListener(x -> showHttpWarningDialog(url));
                        } else {
                            if (secureWarningLayout != null) secureWarningLayout.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        reportError("onPageStarted", e);
                    }
                    super.onPageStarted(v, url, f);
                }

                @Override
                public void onPageFinished(WebView v, String url) {
                    try {
                        if (urlInput != null) urlInput.setText(url != null && url.equals(HOMEPAGE_URL) ? (googleLiteEnabled ? GOOGLE_HOME : "ホーム") : url);
                        if (url != null) preferences.edit().putString("lastUrl", url).apply();
                    } catch (Exception e) {
                        Log.w(TAG, "onPageFinished: update/display/save failed", e);
                    }
                    super.onPageFinished(v, url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                    Uri u = r.getUrl();
                    if (u == null) return false;
                    String scheme = u.getScheme();
                    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                        return false;
                    }
                    try {
                        Intent ext = new Intent(Intent.ACTION_VIEW, u);
                        ext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(ext);
                    } catch (Exception e) {
                        Log.w(TAG, "shouldOverrideUrlLoading: external intent failed for " + u, e);
                    }
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(PermissionRequest req) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String[] perms = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
                        boolean ok = true;
                        for (String p : perms) {
                            if (ContextCompat.checkSelfPermission(MainActivity.this, p) != PackageManager.PERMISSION_GRANTED) {
                                ok = false;
                                break;
                            }
                        }
                        if (!ok) {
                            ActivityCompat.requestPermissions(MainActivity.this, perms, REQUEST_PERMISSION);
                            req.deny();
                            return;
                        }
                    }
                    req.grant(req.getResources());
                }

                @Override
                public void onProgressChanged(WebView v, int p) {
                    if (progressBar != null) {
                        progressBar.setProgress(p);
                        progressBar.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
                    }
                    super.onProgressChanged(v, p);
                }

                @Override
                public void onShowCustomView(View view, CustomViewCallback cb) {
                    if (floatingToolbar != null) floatingToolbar.setVisibility(View.GONE);
                    if (Build.VERSION.SDK_INT >= 19) {
                        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                        getWindow().getDecorView().setSystemUiVisibility(flags);
                    } else {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    customView = view;
                    customViewCallback = cb;
                    getWindow().addContentView(customView, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                }

                @Override
                public void onHideCustomView() {
                    if (customView != null) {
                        try {
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                            ((ViewGroup) customView.getParent()).removeView(customView);
                            if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                        } catch (Exception ignored) {} finally {
                            customView = null;
                            customViewCallback = null;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            if (floatingToolbar != null) floatingToolbar.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                    WebView newWebView = new WebView(MainActivity.this);
                    WebSettings nws = newWebView.getSettings();
                    nws.setJavaScriptEnabled(true);
                    nws.setDomStorageEnabled(true);
                    nws.setJavaScriptCanOpenWindowsAutomatically(true);
                    nws.setAllowFileAccess(true);
                    newWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageStarted(WebView v, String url, Bitmap favicon) {
                            if (webView != null && url != null) webView.loadUrl(url);
                            try {
                                newWebView.stopLoading();
                                newWebView.setWebViewClient(null);
                            } catch (Exception ignored) {}
                        }
                    });
                    android.webkit.WebView.WebViewTransport transport = (android.webkit.WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(newWebView);
                    resultMsg.sendToTarget();
                    return true;
                }
            });
            // originalUserAgent をここでも確保（configureWebView のあと確実に）
            try {
                originalUserAgent = webView.getSettings().getUserAgentString();
            } catch (Exception ignored) {}
        } catch (Exception e) {
            reportError("configureWebView", e);
        }
    }

    private void showSearchOverlay() {
        try {
            // メニューが出ていれば閉じる
            if (menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE) hideMenuOverlay();

            if (floatingToolbar != null) {
                floatingToolbar.animate()
                        .translationY(floatingToolbar.getHeight() / 2f)
                        .alpha(0f)
                        .setDuration(220)
                        .withEndAction(() -> floatingToolbar.setVisibility(View.GONE))
                        .start();
            }
            if (secureWarningLayout != null) secureWarningLayout.setVisibility(View.GONE);
            if (urlOverlay != null) {
                urlOverlay.setAlpha(0f);
                urlOverlay.setVisibility(View.VISIBLE);
                urlOverlay.animate().translationY(0f).alpha(1f).setDuration(220).start();
            }
            if (urlInput != null) {
                urlInput.requestFocus();
                if (imm != null) {
                    urlInput.postDelayed(() -> {
                        try {
                            imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
                        } catch (Exception ignored) {}
                    }, 150);
                }
            }
        } catch (Exception e) {
            reportError("showSearchOverlay", e);
        }
    }

    private void hideSearchOverlay() {
        try {
            if (imm != null && urlInput != null) {
                try {
                    imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
                } catch (Exception ignored) {}
            }
            if (urlOverlay != null) {
                urlOverlay.animate().alpha(0f).translationY(urlOverlay.getHeight() / 2f).setDuration(180)
                        .withEndAction(() -> urlOverlay.setVisibility(View.GONE)).start();
            }
            if (floatingToolbar != null) {
                floatingToolbar.setAlpha(0f);
                floatingToolbar.setVisibility(View.VISIBLE);
                floatingToolbar.animate().translationY(0f).alpha(1f).setDuration(200).start();
            }
            if (urlInput != null) {
                urlInput.postDelayed(() -> {
                    try {
                        String current = webView.getUrl();
                        if (current != null) urlInput.setText(current.equals(HOMEPAGE_URL) ? (googleLiteEnabled ? GOOGLE_HOME : "ホーム") : current);
                    } catch (Exception ignored) {}
                }, 200);
            }
        } catch (Exception e) {
            reportError("hideSearchOverlay", e);
        }
    }

    // menuOverlay 用の表示・非表示
    private void showMenuOverlay() {
        try {
            // 検索オーバーレイが出ている場合は閉じる
            if (urlOverlay != null && urlOverlay.getVisibility() == View.VISIBLE) hideSearchOverlay();

            if (menuOverlay != null) {
                menuOverlay.setAlpha(0f);
                menuOverlay.setVisibility(View.VISIBLE);
                menuOverlay.animate().translationY(0f).alpha(1f).setDuration(220).start();
            }
            if (floatingToolbar != null) {
                floatingToolbar.animate().translationY(floatingToolbar.getHeight() / 2f).alpha(0f).setDuration(200)
                        .withEndAction(() -> floatingToolbar.setVisibility(View.GONE)).start();
            }
        } catch (Exception e) {
            reportError("showMenuOverlay", e);
        }
    }

    private void hideMenuOverlay() {
        try {
            if (menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE) {
                menuOverlay.animate().alpha(0f).translationY(menuOverlay.getHeight() / 2f).setDuration(180)
                        .withEndAction(() -> menuOverlay.setVisibility(View.GONE)).start();
            }
            if (floatingToolbar != null) {
                floatingToolbar.setAlpha(0f);
                floatingToolbar.setVisibility(View.VISIBLE);
                floatingToolbar.animate().translationY(0f).alpha(1f).setDuration(200).start();
            }
        } catch (Exception e) {
            reportError("hideMenuOverlay", e);
        }
    }

    private void loadLastUrl() {
        try {
            String last = preferences.getString("lastUrl", null);
            if (last != null && !last.isEmpty()) webView.loadUrl(last);
            else webView.loadUrl(googleLiteEnabled ? GOOGLE_HOME : HOMEPAGE_URL);
        } catch (Exception e) {
            reportError("loadLastUrl", e);
        }
    }

    private void setupControls() {
        try {
            if (homeButton != null) homeButton.setOnClickListener(v -> {
                try {
                    webView.loadUrl(googleLiteEnabled ? GOOGLE_HOME : HOMEPAGE_URL);
                } catch (Exception e) {
                    reportError("homeButton", e);
                }
            });
            if (backButton != null) backButton.setOnClickListener(v -> onBackPressed()); // ジェスチャーと同じ挙動に
            if (forwardButton != null) forwardButton.setOnClickListener(v -> {
                try {
                    if (webView != null && webView.canGoForward()) webView.goForward();
                    else Toast.makeText(MainActivity.this, "進めるページがありません", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    reportError("forwardButton", e);
                }
            });
            if (refreshButton != null) refreshButton.setOnClickListener(v -> {
                try {
                    if (webView != null) webView.reload();
                } catch (Exception e) {
                    reportError("refreshButton", e);
                }
            });
            // toolbarMenuButton はツールバー上の3点メニュー。押すと menuOverlay をトグル表示
            if (toolbarMenuButton != null) toolbarMenuButton.setOnClickListener(v -> {
                try {
                    if (menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE) hideMenuOverlay();
                    else showMenuOverlay();
                } catch (Exception e) {
                    reportError("toolbarMenuButton", e);
                }
            });

            if (searchButton != null) searchButton.setOnClickListener(v -> showSearchOverlay());
            if (goButton != null) goButton.setOnClickListener(v -> {
                try {
                    String u = urlInput != null ? urlInput.getText().toString().trim() : "";
                    if ("野獣先輩".equals(u)) {
                        webView.loadUrl("file:///android_asset/minigame.html");
                    } else if (!TextUtils.isEmpty(u)) {
                        if (!u.startsWith("http")) u = "https://" + u;
                        webView.loadUrl(u);
                    } else {
                        Toast.makeText(this, "URLを正しく入力してください", Toast.LENGTH_SHORT).show();
                    }
                    hideSearchOverlay();
                } catch (Exception e) {
                    reportError("goButton", e);
                }
            });
            if (closeSearchButton != null) closeSearchButton.setOnClickListener(v -> hideSearchOverlay());
            if (urlInput != null) {
                urlInput.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                        if (goButton != null) goButton.performClick();
                        return true;
                    }
                    return false;
                });
            }

            // menuOverlay 内のボタン
            if (menuDownloadButton != null) {
                menuDownloadButton.setOnClickListener(v -> {
                    try {
                        hideMenuOverlay();
                        // ここは Intent で DownloadHistoryActivity を起動する（Activity を Java で用意してある）
                        startActivity(new Intent(MainActivity.this, DownloadHistoryActivity.class));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "ダウンロード履歴を開けませんでした", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (menuSettingsButton != null) {
                menuSettingsButton.setOnClickListener(v -> {
                    try {
                        hideMenuOverlay();
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    } catch (Exception ignored) {}
                });
            }

            // 追加：メニュー内の PC モード切替ボタン（存在するなら）
            if (pcModeButtonMenu != null) {
                pcModeButtonMenu.setOnClickListener(v -> {
                    try {
                        togglePcMode();
                        hideMenuOverlay();
                    } catch (Exception e) {
                        reportError("pcModeButtonMenu", e);
                    }
                });
            }
        } catch (Exception e) {
            reportError("setupControls", e);
        }
    }

    private void setupFloatingToolbarSliding() {
        if (floatingToolbar == null) return;
        final float density = getResources().getDisplayMetrics().density;
        final float thresholdPx = TOGGLE_THRESHOLD_DP * density;

        floatingToolbar.setOnTouchListener(new View.OnTouchListener() {
            float downX = 0f;
            float lastX = 0f;
            boolean dragging = false;
            float maxTranslate = 0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (maxTranslate == 0f && v.getWidth() > 0) {
                    maxTranslate = v.getWidth() * 1.05f;
                }
                float rawX = event.getRawX();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = rawX;
                        lastX = rawX;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = rawX - lastX;
                        float totalDx = rawX - downX;
                        if (!dragging) {
                            if (Math.abs(totalDx) > toolbarTouchSlop) {
                                dragging = true;
                            }
                        }
                        if (dragging) {
                            float newTrans = v.getTranslationX() + dx;
                            if (maxTranslate > 0) {
                                if (newTrans < -maxTranslate) newTrans = -maxTranslate;
                                if (newTrans > maxTranslate) newTrans = maxTranslate;
                            }
                            v.setTranslationX(newTrans);
                            float alpha = 1f - (maxTranslate > 0 ? Math.min(1f, Math.abs(newTrans) / (maxTranslate * 0.9f)) : 0f);
                            v.setAlpha(Math.max(0.12f, alpha));
                        }
                        lastX = rawX;
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float total = rawX - downX;
                        dragging = false;
                        float cur = v.getTranslationX();
                        if (Math.abs(total) > thresholdPx) {
                            if (total < 0) {
                                float ratio = (maxTranslate > 0) ? Math.min(1f, Math.abs(cur) / maxTranslate) : 1f;
                                long dur = (long) (300 * (1f - ratio) + 80);
                                v.animate().translationX(-maxTranslate).alpha(0.12f).setDuration(dur).start();
                                toolbarCompact = true;
                            } else {
                                float ratio = (maxTranslate > 0) ? Math.min(1f, Math.abs(cur) / maxTranslate) : 1f;
                                long dur = (long) (300 * (1f - ratio) + 80);
                                v.animate().translationX(0f).alpha(1f).setDuration(dur).start();
                                toolbarCompact = false;
                            }
                        } else {
                            v.animate().translationX(0f).alpha(1f).setDuration(200).start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // Download helpers（拡張子チェックは行わず、MIME/拡張子不明なら「不明な形式」と表示）
    private void showDownloadConfirmation(String url, String ua, String cd, String mime, long len, String fn) {
        try {
            String size = readableFileSize(len);
            String displayType;
            if (mime != null && !mime.isEmpty()) {
                displayType = mime;
            } else {
                String ext = MimeTypeMap.getFileExtensionFromUrl(url);
                if (ext != null && !ext.isEmpty()) displayType = ext.toLowerCase();
                else displayType = "不明な形式";
            }

            new AlertDialog.Builder(this)
                    .setTitle("ダウンロードの確認")
                    .setMessage("ファイル名: " + fn + "\n容量: " + size + "\nファイルタイプ: " + displayType)
                    .setCancelable(false)
                    .setNegativeButton("キャンセル", (d, w) -> d.dismiss())
                    .setPositiveButton("続行", (d, w) -> startDownload(url, ua, cd, mime, fn))
                    .show();
        } catch (Exception e) {
            reportError("showDownloadConfirmation", e);
        }
    }

    private void startDownload(String url, String ua, String cd, String mime, String fn) {
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            if (mime != null && !mime.isEmpty()) r.setMimeType(mime);
            if (ua != null) r.addRequestHeader("User-Agent", ua);
            r.setDescription("Downloading file...");
            r.setTitle(fn);
            r.allowScanningByMediaScanner();
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            // 変更: 共通の Downloads フォルダ（外部パブリックディレクトリ）に保存するようにする
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long id = dm.enqueue(r);

            try {
                String when = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                DownloadHistory.pushHistory(this, id, fn, fn, url, 0L, when);
            } catch (Exception e) {
                Log.w(TAG, "startDownload: history save failed", e);
            }

        } catch (Exception e) {
            reportError("startDownload", e);
            Toast.makeText(this, "ダウンロード中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] u = {"B","KB","MB","GB","TB"};
        int g = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, g)) + " " + u[g];
    }

    private void showHttpWarningDialog(String url) {
        new AlertDialog.Builder(this)
                .setMessage("このウェブサイトは暗号化に対応していません。情報が渡される可能性があります。\n続行しますか？")
                .setCancelable(false)
                .setPositiveButton("続ける", (d, i) -> webView.loadUrl(url))
                .setNegativeButton("キャンセル", (d, i) -> d.dismiss())
                .show();
    }

    private void requestPermissions() {
        try {
            String[] perms = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean need = false;
                for (String p : perms) if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) { need = true; break; }
                if (need) ActivityCompat.requestPermissions(this, perms, REQUEST_PERMISSION);
            }
        } catch (Exception e) {
            reportError("requestPermissions", e);
        }
    }

    private boolean isLargeScreen() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        double inches = Math.sqrt(Math.pow(dm.widthPixels / dm.xdpi, 2) + Math.pow(dm.heightPixels / dm.ydpi, 2));
        return inches >= 10;
    }

    private void enableDesktopMode() {
        try {
            if (webView != null) {
                webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0");
                webView.getSettings().setBuiltInZoomControls(true);
                // マークしておく（手動トグルと状態が食い違わないように）
                isPcMode = true;
                Toast.makeText(this, "PCモード ON（画面幅が大きいため自動適用）", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            reportError("enableDesktopMode", e);
        }
    }

    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
        } catch (Exception e) {
            reportError("copyToClipboard", e);
        }
    }

    private void shareText(String subject, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, subject));
        } catch (Exception e) {
            reportError("shareText", e);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            // 変更点：super.onBackPressed() を呼ばんようにしてアプリが閉じへんようにする
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                Toast.makeText(this, "戻るページがありません", Toast.LENGTH_SHORT).show();
                // 終了させたくないので super は呼ばない
            }
        } catch (Exception e) {
            reportError("onBackPressed", e);
            // 例外時も強制終了は避ける
            Toast.makeText(this, "不明なエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String url = data.toString();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                if (webView != null) webView.loadUrl(url);
            } else {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, data));
                } catch (Exception e) {
                    Log.w(TAG, "外部スキームの起動に失敗: " + data, e);
                }
            }
            return;
        }
        if (Intent.ACTION_SEND.equals(action)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                int idx = text.indexOf("http://");
                if (idx == -1) idx = text.indexOf("https://");
                if (idx != -1) {
                    int end = text.indexOf(' ', idx);
                    String url = end == -1 ? text.substring(idx) : text.substring(idx, end);
                    if (webView != null) webView.loadUrl(url);
                } else {
                    Toast.makeText(this, "共有テキストに有効な URL が見つかりませんでした", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    // 追加：PC モードのトグル
    private void togglePcMode() {
        try {
            WebSettings ws = webView.getSettings();
            if (!isPcMode) {
                // ON
                if (originalUserAgent == null) originalUserAgent = ws.getUserAgentString();
                String desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36";
                ws.setUserAgentString(desktopUA);
                ws.setUseWideViewPort(true);
                ws.setLoadWithOverviewMode(true);
                isPcMode = true;
                Toast.makeText(this, "PCモード ON", Toast.LENGTH_SHORT).show();
            } else {
                // OFF
                if (originalUserAgent != null) {
                    ws.setUserAgentString(originalUserAgent);
                } else {
                    ws.setUserAgentString(WebSettings.getDefaultUserAgent(this));
                }
                ws.setUseWideViewPort(false);
                ws.setLoadWithOverviewMode(false);
                isPcMode = false;
                Toast.makeText(this, "PCモード OFF", Toast.LENGTH_SHORT).show();
            }
            webView.reload();
        } catch (Exception e) {
            reportError("togglePcMode", e);
        }
    }

    // エラー報告用ユーティリティ（ログ & トースト）
    private void reportError(String where, Exception e) {
        try {
            Log.e(TAG, "Error at " + where, e);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(MainActivity.this, "不明なエラーが発生しました (" + where + ")", Toast.LENGTH_LONG).show()
            );
        } catch (Exception ignored) {}
    }
}
