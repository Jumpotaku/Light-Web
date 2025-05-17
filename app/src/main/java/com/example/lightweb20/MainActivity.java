package com.example.lightweb20;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    public static final String HOMEPAGE_URL = "file:///android_asset/index.html";
    public static final String GOOGLE_HOME = "https://www.google.com";
    public static final String EXCEPTION_URL = "https://abehiroshi.la.coocan.jp/";
    private static final String PREFS_NAME = "appPreferences";
    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_TAB = 2;

    // 各ビュー
    private WebView webView;
    private EditText urlInput;
    private ImageButton backButton, forwardButton, settingsButton, tabButton, goButton, refreshButton, homeButton;
    private LinearLayout secureWarningLayout;
    private ImageButton warningIcon;
    private TextView secureWarningMessage;
    private DrawerLayout drawerLayout;
    private ProgressBar progressBar; // 読み込み進捗用 ProgressBar

    private SharedPreferences preferences;
    private boolean jsEnabled;
    private boolean googleLiteEnabled;
    private boolean showHttpWarning = true;

    // ダウンロード進捗確認用ハンドラ
    private Handler downloadHandler = new Handler();

    // コンテキストメニュー用に最後の HitTestResult を保持
    private HitTestResult mHitTestResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 設定読み込み
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        jsEnabled = preferences.getBoolean("jsEnabled", true);
        googleLiteEnabled = preferences.getBoolean("googleLiteEnabled", false);
        showHttpWarning = preferences.getBoolean("showHttpWarning", true);

        // 各ビューの初期化
        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        goButton = findViewById(R.id.goButton);
        backButton = findViewById(R.id.backButton);
        forwardButton = findViewById(R.id.forwardButton);
        refreshButton = findViewById(R.id.refreshButton);
        settingsButton = findViewById(R.id.settingsButton);
        tabButton = findViewById(R.id.tabButton);
        homeButton = findViewById(R.id.homeButton);
        // DrawerLayout はルートレイアウトの ID を利用（XML側で "root_layout" として定義）
        drawerLayout = findViewById(R.id.root_layout);
        // SSL 警告用ビュー
        secureWarningLayout = findViewById(R.id.secureWarningLayout);
        warningIcon = findViewById(R.id.warningIcon);
        secureWarningMessage = findViewById(R.id.secureWarningMessage);
        // ProgressBar（レイアウト側に追加済み）
        progressBar = findViewById(R.id.progressBar);

        // コンテキストメニュー登録（長押し処理）
        registerForContextMenu(webView);

        // JavaScript 有効化
        setJavaScriptEnabled(jsEnabled);

        // 必要なパーミッション
        requestPermissions();

        // 大画面ならデスクトップモードへ
        if (isLargeScreen()) {
            enableDesktopMode();
        }

        // ダウンロード処理（確認後実行）
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, final String userAgent,
                                        final String contentDisposition, final String mimetype, final long contentLength) {
                final String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                showDownloadConfirmation(url, userAgent, contentDisposition, mimetype, contentLength, fileName);
            }
        });

        // WebViewClient の設定
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // URL入力欄の更新
                if (url.equals(HOMEPAGE_URL)) {
                    urlInput.setText("ホーム");
                } else {
                    urlInput.setText(url);
                }
                // SSL 警告：http:// なら表示（例外サイト以外）
                if (url.startsWith("http://") && showHttpWarning && !url.equals(EXCEPTION_URL)) {
                    secureWarningLayout.setVisibility(View.VISIBLE);
                    warningIcon.setOnClickListener(v -> showHttpWarningDialog(url));
                } else {
                    secureWarningLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        // WebChromeClient の設定（パーミッションと進捗表示の統合）
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(android.webkit.PermissionRequest request) {
                boolean permissionsAvailable = true;
                for (String resource : request.getResources()) {
                    if (resource.equals("android.webkit.resource.AUDIO_CAPTURE") &&
                            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                                    != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION);
                        permissionsAvailable = false;
                        break;
                    }
                    if (resource.equals("android.webkit.resource.VIDEO_CAPTURE") &&
                            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
                        permissionsAvailable = false;
                        break;
                    }
                }
                if (permissionsAvailable) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                }
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                // 進捗バー更新
                progressBar.setProgress(progress);
                if (progress < 100 && progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                } else if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
                super.onProgressChanged(view, progress);
            }
        });

        // ホームページ読み込み
        webView.loadUrl(HOMEPAGE_URL);

        // 各ボタンのアクション設定
        homeButton.setOnClickListener(v -> webView.loadUrl(HOMEPAGE_URL));
        backButton.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        forwardButton.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        refreshButton.setOnClickListener(v -> webView.reload());
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        tabButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TabActivity.class);
            startActivityForResult(intent, REQUEST_TAB);
        });
        goButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            // 指定の単語「野獣先輩」が入力された場合は、アセットフォルダー内の HTML ベースのミニゲームを読み込む
            if (url.equals("野獣先輩")) {
                webView.loadUrl("file:///android_asset/minigame.html");
                return;
            }
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webView.loadUrl(url);
            } else {
                Toast.makeText(MainActivity.this, "URLを正しく入力してください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------------------------------------------------
    // コンテキストメニュー（リンク・画像の長押し処理）
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v instanceof WebView) {
            mHitTestResult = webView.getHitTestResult();
            if (mHitTestResult != null) {
                int type = mHitTestResult.getType();
                // 画像のみの場合は画像用オプションを表示
                if (type == HitTestResult.IMAGE_TYPE) {
                    menu.setHeaderTitle("画像オプション");
                    menu.add(Menu.NONE, 3, Menu.NONE, "画像をダウンロード");
                    menu.add(Menu.NONE, 4, Menu.NONE, "画像の URL をコピー");
                    menu.add(Menu.NONE, 5, Menu.NONE, "画像を共有");
                }
                // リンクの場合（リンク単体 or 画像付きリンクの場合）
                else if (type == HitTestResult.SRC_ANCHOR_TYPE || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    menu.setHeaderTitle("リンクオプション");
                    menu.add(Menu.NONE, 1, Menu.NONE, "リンクをコピー");
                    menu.add(Menu.NONE, 2, Menu.NONE, "リンクを共有");
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mHitTestResult == null) return super.onContextItemSelected(item);
        int type = mHitTestResult.getType();
        final String extra = mHitTestResult.getExtra();  // URLなどの文字列
        switch (item.getItemId()) {
            case 1: // リンクをコピー
                if (extra != null) {
                    copyToClipboard("リンク", extra);
                    Toast.makeText(this, "リンクをコピーしました", Toast.LENGTH_SHORT).show();
                }
                return true;
            case 2: // リンクを共有
                if (extra != null) {
                    shareText("リンクを共有", extra);
                }
                return true;
            case 3: // 画像をダウンロード
                if (extra != null) {
                    String fileName = URLUtil.guessFileName(extra, null, null);
                    // URLから拡張子を判定し、適切な MIME タイプを取得する
                    String mimeType = getMimeTypeFromUrl(extra);
                    if (mimeType == null || mimeType.isEmpty()) {
                        mimeType = "image/*";
                    }
                    startDownload(extra, "Mozilla/5.0", "", mimeType, fileName);
                }
                return true;
            case 4: // 画像の URL をコピー
                if (extra != null) {
                    copyToClipboard("画像 URL", extra);
                    Toast.makeText(this, "画像の URL をコピーしました", Toast.LENGTH_SHORT).show();
                }
                return true;
            case 5: // 画像を共有
                if (extra != null) {
                    shareText("画像を共有", extra);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    // ------------------------------------------------------------------

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    private void shareText(String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, subject));
    }

    private void setJavaScriptEnabled(boolean enabled) {
        webView.getSettings().setJavaScriptEnabled(enabled);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("jsEnabled", enabled);
        editor.apply();
        webView.reload();
    }

    private void showHttpWarningDialog(String url) {
        new AlertDialog.Builder(this)
                .setMessage("このウェブサイトは暗号化に対応していません。情報が渡される可能性があります。\n続行しますか？")
                .setCancelable(false)
                .setPositiveButton("続ける", (dialog, id) -> webView.loadUrl(url))
                .setNegativeButton("キャンセル", (dialog, id) -> dialog.dismiss())
                .create()
                .show();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean isLargeScreen() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(displayMetrics);
        float x = (float) Math.pow(displayMetrics.widthPixels / displayMetrics.xdpi, 2);
        float y = (float) Math.pow(displayMetrics.heightPixels / displayMetrics.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        return screenInches >= 10;
    }

    private void enableDesktopMode() {
        String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0";
        webView.getSettings().setUserAgentString(desktopUserAgent);

        if (isLargeScreen()) {
            int buttonWidth = 300;
            int buttonHeight = 80;
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(buttonWidth, buttonHeight);
            buttonParams.setMargins(20, 10, 20, 10);

            if (backButton.getParent() != null) backButton.setLayoutParams(buttonParams);
            if (forwardButton.getParent() != null) forwardButton.setLayoutParams(buttonParams);
            if (refreshButton.getParent() != null) refreshButton.setLayoutParams(buttonParams);
            if (homeButton.getParent() != null) homeButton.setLayoutParams(buttonParams);
            if (settingsButton.getParent() != null) settingsButton.setLayoutParams(buttonParams);
            if (tabButton.getParent() != null) tabButton.setLayoutParams(buttonParams);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    String javascript = "document.body.style.zoom = '80%';";
                    webView.evaluateJavascript(javascript, null);
                }
            });
            webView.getSettings().setBuiltInZoomControls(true);
        }
    }

    // タブアクティビティから返されたタブ URL を反映
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAB && resultCode == RESULT_OK) {
            String url = data.getStringExtra("tab_url");
            if (url != null) {
                webView.loadUrl(url);
            }
        }
    }

    // --------------------------------------------------
    // ダウンロード機能：確認ダイアログ、進捗表示、完了通知
    private void showDownloadConfirmation(final String url, final String userAgent, final String contentDisposition,
                                          final String mimetype, final long contentLength, final String fileName) {
        String fileSize = readableFileSize(contentLength);
        String message = "ファイル名: " + fileName + "\n容量: " + fileSize;
        new AlertDialog.Builder(this)
                .setTitle("ダウンロードの確認")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("続行", (dialog, which) -> {
                    startDownload(url, userAgent, contentDisposition, mimetype, fileName);
                })
                .show();
    }

    private void startDownload(String url, String userAgent, String contentDisposition,
                               String mimetype, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimetype);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(fileName);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            final long downloadId = downloadManager.enqueue(request);
            showDownloadProgressDialog(downloadId, fileName);
        }
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void showDownloadProgressDialog(final long downloadId, final String fileName) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View progressView = inflater.inflate(R.layout.download_progress, null);
        final ProgressBar progressBarDownload = progressView.findViewById(R.id.downloadProgress);
        final TextView statusText = progressView.findViewById(R.id.downloadStatus);
        statusText.setText("ダウンロード中...");

        final AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        downloadHandler.post(new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    Cursor cursor = downloadManager.query(query);
                    if (cursor != null && cursor.moveToFirst()) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        if (bytesTotal > 0) {
                            int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            progressBarDownload.setProgress(progress);
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            statusText.setText("ダウンロードが完了しました");
                            downloadHandler.postDelayed(() -> progressDialog.dismiss(), 1500);
                            cursor.close();
                            return;
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            statusText.setText("ダウンロードに失敗しました");
                            downloadHandler.postDelayed(() -> progressDialog.dismiss(), 1500);
                            cursor.close();
                            return;
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                downloadHandler.postDelayed(this, 500);
            }
        });
    }
    // --------------------------------------------------

    /**
     * 指定された URL の拡張子から MIME タイプを取得する。
     * 対応していない場合は null を返す。
     */
    private String getMimeTypeFromUrl(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null && !extension.isEmpty()) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }
}
