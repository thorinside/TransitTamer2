package org.nsdev.apps.transittamer.ui;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Provides a base adapter for a recyclerview that handles some of the databinding updates.
 * <p>
 * Created by nealsanche on 15-09-28.
 */
public abstract class BindingAdapter<T extends ViewDataBinding> extends RecyclerView.Adapter<BindingViewHolder<T>> {

    private final int mLayoutResourceId;

    public BindingAdapter(int layoutResourceId) {
        mLayoutResourceId = layoutResourceId;
    }

    @Override
    public BindingViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        T binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), mLayoutResourceId, parent, false);
        return new BindingViewHolder<>(binding);
    }

    @Override
    public void onBindViewHolder(BindingViewHolder<T> holder, int position) {
        T binding = holder.getBinding();
        updateBinding(binding, position);
    }

    @Override
    public void onViewRecycled(BindingViewHolder<T> holder) {
        super.onViewRecycled(holder);
        T binding = holder.getBinding();
        recycleBinding(binding);
    }

    protected abstract void updateBinding(T binding, int position);

    protected abstract void recycleBinding(T binding);
}