package com.byq.systemcallback.ui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.byq.systemcallback.BuildConfig;

public abstract class MyRecyclerViewAdapter extends RecyclerView.Adapter {
    private View emptyView;
    private RecyclerView recyclerView;
    private OnItemCountChanged onItemCountChanged;

    public OnItemCountChanged getOnItemCountChanged() {
        return onItemCountChanged;
    }

    public void setOnItemCountChanged(OnItemCountChanged onItemCountChanged) {
        this.onItemCountChanged = onItemCountChanged;
    }

    public void attachEmptyView(View view) {
        emptyView = view;
    }

    public void attachRecy(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public abstract class MyViewHolder extends RecyclerView.ViewHolder {

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            onCreate();
        }

        public void onCreate() {

        }

        public abstract void onBindView(int position);
    }

    public abstract Integer getLayoutId();

    public abstract MyViewHolder getViewHolder(View view);

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(getLayoutId(),parent,false);

        return getViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder mv = (MyViewHolder) holder;
        mv.onBindView(position);
    }

    /**
     * Get item count
     * @return item count
     */
    public abstract int getCount();

    @Override
    public final int getItemCount() {
        int size = getCount();
        if (onItemCountChanged != null) {
            onItemCountChanged.onChanged(size);
        }
        if (emptyView != null && recyclerView != null) {
            if (size == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
        return size;
    }

    public interface OnItemCountChanged {
        public void onChanged(int count);
    }
}
