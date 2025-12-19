package com.example.lightweb20;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class TabListActivity extends Activity {

    private ListView listView;
    private Button addButton;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> tabTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_list);

        listView = findViewById(R.id.listViewTabs);
        addButton = findViewById(R.id.buttonAddTab);

        tabTitles = new ArrayList<>();
        updateTabTitles();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, tabTitles);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // 現在のタブを選択状態にする
        int current = TabManager.getInstance().getCurrentTabIndex();
        if (current != -1) {
            listView.setItemChecked(current, true);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            TabManager.getInstance().setCurrentTabIndex(position);

            // メインアクティビティに戻して選択したタブを開く指示を送る
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedTabIndex", position);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        addButton.setOnClickListener(v -> {
            // 新規タブ追加（初期は空のタブ）
            TabManager.getInstance().addTab(new TabInfo("新しいタブ", "about:blank"));
            updateTabTitles();
            adapter.notifyDataSetChanged();

            // 新しいタブを選択状態にする
            int newIndex = TabManager.getInstance().getTabs().size() - 1;
            listView.setItemChecked(newIndex, true);

            // メインアクティビティに通知
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedTabIndex", newIndex);
            setResult(RESULT_OK, resultIntent);
        });
    }

    private void updateTabTitles() {
        tabTitles.clear();
        for (TabInfo tab : TabManager.getInstance().getTabs()) {
            tabTitles.add(tab.getTitle());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTabTitles();
        adapter.notifyDataSetChanged();

        int current = TabManager.getInstance().getCurrentTabIndex();
        if (current != -1) {
            listView.setItemChecked(current, true);
        }
    }
}
