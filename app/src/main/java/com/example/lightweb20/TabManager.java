package com.example.lightweb20;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class TabManager {
    private static TabManager instance;
    private ArrayList<Tab> tabs;
    private static final String PREF_NAME = "TabPrefs";
    private static final String KEY_TABS = "tabs";

    private TabManager() {
        tabs = new ArrayList<>();
    }

    public static TabManager getInstance() {
        if (instance == null) {
            instance = new TabManager();
        }
        return instance;
    }

    public ArrayList<Tab> getTabs() {
        return tabs;
    }

    public void addTab(Tab tab) {
        tabs.add(tab);
    }

    public void removeTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.remove(index);
        }
    }

    public void saveTabs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();

        for (Tab tab : tabs) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", tab.getName());
                obj.put("url", tab.getUrl());
                jsonArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString(KEY_TABS, jsonArray.toString());
        editor.apply();
    }

    public void loadTabs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_TABS, null);
        tabs.clear();

        if (jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String name = obj.getString("name");
                    String url = obj.getString("url");
                    tabs.add(new Tab(name, url));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 初回起動時に空ならホームタブ追加
        if (tabs.isEmpty()) {
            tabs.add(new Tab("ホーム", MainActivity.HOMEPAGE_URL));
        }
    }
}
