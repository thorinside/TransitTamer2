package org.nsdev.apps.transittamer.ui;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

/**
 * Created by nealsanche on 15-09-28.
 */
public class BindingViewHolder<T extends ViewDataBinding> extends RecyclerView.ViewHolder {
    final T mLayoutBinding;

    public BindingViewHolder(T layoutBinding) {
        super(layoutBinding.getRoot());
        mLayoutBinding = layoutBinding;
    }

    public T getBinding() {
        return mLayoutBinding;
    }
}