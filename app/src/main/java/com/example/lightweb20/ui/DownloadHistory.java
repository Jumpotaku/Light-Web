package com.example.lightweb20.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DownloadHistory {
    private static final String PREFS = "download_history_prefs";
    private static final String KEY_HISTORY = "download_history_json";
    private static final String TAG = "DownloadHistory";

    public static class Entry {
        public long id;
        public String title;
        public String filename;
        public String url;
        public long size;
        public String when;
    }

    public static void pushHistory(Context ctx, long id, String title, String filename, String url, long size, String when) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = sp.getString(KEY_HISTORY, "[]");
            JSONArray arr = new JSONArray(raw);
            // 新しいものを先頭に入れる
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("title", title != null ? title : filename);
            o.put("filename", filename != null ? filename : "");
            o.put("url", url != null ? url : "");
            o.put("size", size);
            o.put("when", when != null ? when : "");
            arr.put(0, o);
            // 上限（例: 200 件）を超えたら切る
            if (arr.length() > 200) {
                JSONArray newArr = new JSONArray();
                for (int i = 0; i < 200; i++) newArr.put(arr.get(i));
                arr = newArr;
            }
            sp.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "pushHistory: json error", e);
        } catch (Exception e) {
            Log.w(TAG, "pushHistory: error", e);
        }
    }

    public static ArrayList<Entry> getHistory(Context ctx) {
        ArrayList<Entry> list = new ArrayList<>();
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = sp.getString(KEY_HISTORY, "[]");
            if (TextUtils.isEmpty(raw)) return list;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Entry e = new Entry();
                e.id = o.optLong("id", -1L);
                e.title = o.optString("title", "(unknown)");
                e.filename = o.optString("filename", "");
                e.url = o.optString("url", "");
                e.size = o.optLong("size", 0L);
                e.when = o.optString("when", "");
                list.add(e);
            }
        } catch (Exception e) {
            Log.w("DownloadHistory", "getHistory error", e);
        }
        return list;
    }

    public static void removeEntryByIndex(Context ctx, int index) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = sp.getString(KEY_HISTORY, "[]");
            JSONArray arr = new JSONArray(raw);
            if (index < 0 || index >= arr.length()) return;
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (i == index) continue;
                newArr.put(arr.get(i));
            }
            sp.edit().putString(KEY_HISTORY, newArr.toString()).apply();
        } catch (Exception e) {
            Log.w("DownloadHistory", "removeEntryByIndex error", e);
        }
    }
}
