package com.example.lightweb20;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * ViewPager2 用アダプタ。要素は "res:<id>" か "file:<path>" か "ADD_BUTTON"
 */
public class PresetPagerAdapter extends RecyclerView.Adapter<PresetPagerAdapter.VH> {

    public interface Callback {
        void onAddButtonClicked();
        void onPresetClicked(int position, String item);
    }

    private final List<String> items;
    private final Context context;
    private final Callback callback;

    public PresetPagerAdapter(Context ctx, List<String> items, Callback cb) {
        this.context = ctx;
        this.items = items;
        this.callback = cb;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_preset_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String item = items.get(position);
        holder.plusOverlay.setVisibility(View.GONE);
        holder.imageView.setImageDrawable(null);

        if (item == null) {
            holder.imageView.setImageResource(android.R.color.darker_gray);
            holder.itemView.setOnClickListener(null);
            return;
        }

        if ("ADD_BUTTON".equals(item)) {
            holder.imageView.setImageResource(android.R.color.transparent);
            holder.plusOverlay.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onAddButtonClicked();
            });
            return;
        }

        if (item.startsWith("res:")) {
            try {
                int resId = Integer.parseInt(item.substring(4));
                holder.imageView.setImageResource(resId);
            } catch (Exception e) {
                holder.imageView.setImageResource(android.R.color.darker_gray);
            }
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onPresetClicked(position, item);
            });
            return;
        }

        if (item.startsWith("file:")) {
            String path = item.substring(5);
            // 非同期でデコードして ImageView にセット
            new BitmapLoadTask(holder.imageView).execute(path);
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onPresetClicked(position, item);
            });
            return;
        }

        // それ以外: try URI 表示
        try {
            holder.imageView.setImageURI(Uri.parse(item));
            holder.itemView.setOnClickListener(v -> {
                if (callback != null) callback.onPresetClicked(position, item);
            });
        } catch (Exception e) {
            holder.imageView.setImageResource(android.R.color.darker_gray);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView plusOverlay;
        VH(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.preset_image);
            plusOverlay = itemView.findViewById(R.id.plus_overlay);
        }
    }

    /**
     * 画像読み込みを非同期で行い ImageView にセットする簡易タスク。
     * 大きな画像でも inSampleSize を使って縮小してからセットする。
     */
    private static class BitmapLoadTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewRef;
        private String path;

        BitmapLoadTask(ImageView iv) {
            imageViewRef = new WeakReference<>(iv);
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            path = strings[0];
            if (path == null) return null;

            try {
                // 1) サイズ取得
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);

                // 2) 目標サイズは ImageView のレイアウトサイズ（fallback）
                ImageView iv = imageViewRef.get();
                int reqW = 0, reqH = 0;
                if (iv != null) {
                    reqW = iv.getWidth();
                    reqH = iv.getHeight();
                }
                if (reqW <= 0) reqW = 400; // fallback
                if (reqH <= 0) reqH = 300;

                // 3) inSampleSize 計算
                opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // メモリ節約
                return BitmapFactory.decodeFile(path, opts);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView iv = imageViewRef.get();
            if (iv == null) return;
            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
            } else {
                iv.setImageResource(android.R.color.darker_gray);
            }
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }
}
