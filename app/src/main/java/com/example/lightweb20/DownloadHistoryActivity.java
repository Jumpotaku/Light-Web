package com.example.lightweb20; // ← 必ずプロジェクトのパッケージ名に合わせて変えてください

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class DownloadHistoryActivity extends AppCompatActivity {

    private TextView titleText;
    private ImageView iconDownloads;
    private TextView downloadsTitle;
    private RecyclerView recyclerView;
    private TextView emptyView;

    private DownloadsAdapter adapter;
    private List<DownloadItem> downloadItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_history); // ← この行は実際の XML ファイル名 (例: R.layout.activity_download) に変えてください

        // findViewById（ユーザーが指摘していたクォートミスに注意）
        titleText = findViewById(R.id.titleText);
        iconDownloads = findViewById(R.id.icon_downloads);
        downloadsTitle = findViewById(R.id.downloadsTitle);
        recyclerView = findViewById(R.id.recyclerViewDownloads);
        emptyView = findViewById(R.id.emptyView);

        // RecyclerView 初期化
        adapter = new DownloadsAdapter(this, downloadItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 最初の読み込み
        loadDownloads();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 履歴が変わっている可能性があるので再読み込み
        loadDownloads();
    }

    /**
     * DownloadManager からダウンロード履歴を取得してリスト更新
     */
    private void loadDownloads() {
        downloadItems.clear();

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            showEmpty(true);
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query();
        // 状態フィルタ等を入れるならここで設定（例: query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)）
        Cursor cursor = null;
        try {
            cursor = dm.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int descIdx = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
                int uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_URI);
                int localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int sizeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int dateIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);

                do {
                    long id = idIdx >= 0 ? cursor.getLong(idIdx) : -1;
                    String title = titleIdx >= 0 ? cursor.getString(titleIdx) : null;
                    String desc = descIdx >= 0 ? cursor.getString(descIdx) : null;
                    String uri = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                    String localUri = localUriIdx >= 0 ? cursor.getString(localUriIdx) : null;
                    int status = statusIdx >= 0 ? cursor.getInt(statusIdx) : -1;
                    long size = sizeIdx >= 0 ? cursor.getLong(sizeIdx) : -1;
                    long date = dateIdx >= 0 ? cursor.getLong(dateIdx) : -1;

                    DownloadItem item = new DownloadItem(id, title, desc, uri, localUri, status, size, date);
                    downloadItems.add(item);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        adapter.notifyDataSetChanged();
        showEmpty(downloadItems.isEmpty());
    }

    private void showEmpty(boolean empty) {
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * ダウンロードを表す最小クラス
     */
    private static class DownloadItem {
        long id;
        String title;
        String description;
        String uri;
        String localUri;
        int status;
        long totalBytes;
        long lastModified;

        DownloadItem(long id, String title, String description, String uri, String localUri, int status, long totalBytes, long lastModified) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.uri = uri;
            this.localUri = localUri;
            this.status = status;
            this.totalBytes = totalBytes;
            this.lastModified = lastModified;
        }
    }

    /**
     * RecyclerView Adapter（シンプル表示）
     * ここを拡張してプレビュー、開く、削除ボタン等を追加してください。
     */
    private static class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.VH> {
        private final List<DownloadItem> items;
        private final Context context;

        DownloadsAdapter(Context context, List<DownloadItem> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 標準の2行アイテムを使う（簡潔さ優先）。カスタム item_layout.xml を作れば差し替え可能。
            View v = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DownloadItem it = items.get(position);
            holder.title.setText(!TextUtils.isEmpty(it.title) ? it.title : "（タイトルなし）");
            String subtitle = "";
            if (!TextUtils.isEmpty(it.localUri)) {
                subtitle = "保存先: " + Uri.parse(it.localUri).getLastPathSegment();
            } else if (!TextUtils.isEmpty(it.uri)) {
                subtitle = "元URL: " + it.uri;
            }
            if (it.totalBytes > 0) {
                subtitle = subtitle + (subtitle.length() > 0 ? " • " : "") + readableFileSize(it.totalBytes);
            }
            holder.subtitle.setText(subtitle);
            // クリック処理や長押し処理を追加するならここで設定
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }

        private static String readableFileSize(long size) {
            if (size <= 0) return "0 B";
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }
}
