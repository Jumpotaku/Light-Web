package com.example.lightweb20;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class TabActivity extends AppCompatActivity {

    private RecyclerView tabRecyclerView;
    private FloatingActionButton fabAddTab;
    private TabListAdapter adapter;
    private ArrayList<Tab> tabList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        tabRecyclerView = findViewById(R.id.tabRecyclerView);
        fabAddTab = findViewById(R.id.fabAddTab);

        TabManager.getInstance().loadTabs(this);
        tabList = TabManager.getInstance().getTabs();

        adapter = new TabListAdapter(this, tabList, position -> {
            String selectedTabUrl = tabList.get(position).getUrl();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("tab_url", selectedTabUrl);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        tabRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabRecyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                adapter.moveItem(fromPosition, toPosition);
                TabManager.getInstance().saveTabs(TabActivity.this);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // スワイプ操作は無効
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });

        itemTouchHelper.attachToRecyclerView(tabRecyclerView);

        fabAddTab.setOnClickListener(v -> {
            TabManager.getInstance().addTab(new Tab("ホーム", MainActivity.HOMEPAGE_URL));
            TabManager.getInstance().saveTabs(this);
            adapter.notifyItemInserted(tabList.size() - 1);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        TabManager.getInstance().saveTabs(this);
    }
}
