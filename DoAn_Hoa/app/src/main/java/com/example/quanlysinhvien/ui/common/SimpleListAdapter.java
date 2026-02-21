package com.example.quanlysinhvien.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.VH> {
    private final List<String> items;
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longListener;

    public interface OnItemClickListener { void onItemClick(int position); }
    public interface OnItemLongClickListener { boolean onItemLongClick(int position); }

    public SimpleListAdapter(List<String> items, OnItemClickListener listener) {
        this(items, listener, null);
    }

    public SimpleListAdapter(List<String> items, OnItemClickListener listener, OnItemLongClickListener longListener) {
        this.items = items;
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tv.setText(items.get(position));
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onItemClick(position);
        });
        if (longListener != null) {
            holder.itemView.setOnLongClickListener(view -> longListener.onItemLongClick(position));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        public VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
        }
    }
}
