package com.example.lightweb20;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TabListAdapter extends RecyclerView.Adapter<TabListAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Tab> tabs;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public TabListAdapter(Context context, ArrayList<Tab> tabs, OnItemClickListener listener) {
        this.context = context;
        this.tabs = tabs;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView nameView;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.tabIcon);
            nameView = itemView.findViewById(R.id.tabName);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    @Override
    public TabListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TabListAdapter.ViewHolder holder, int position) {
        Tab tab = tabs.get(position);
        holder.nameView.setText(tab.getName());
        holder.iconView.setImageResource(R.drawable.language_24dp_e8eaed_fill0_wght400_grad0_opsz24); // デフォルトのアイコン

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));

        holder.deleteButton.setOnClickListener(v -> {
            tabs.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tabs.size());
            TabManager.getInstance().saveTabs(context);
        });
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    public void moveItem(int fromPosition, int toPosition) {
        Tab fromTab = tabs.get(fromPosition);
        tabs.remove(fromPosition);
        tabs.add(toPosition, fromTab);
        notifyItemMoved(fromPosition, toPosition);
    }
}
