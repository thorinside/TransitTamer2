package org.nsdev.apps.transittamer.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;

/**
 * Implementation of the {@link SnapHelper} supporting snapping in either vertical or horizontal
 * orientation.
 * <p>
 * The implementation will snap the center of the target child view to the center of
 * the attached {@link RecyclerView}. If you intend to change this behavior then override
 * {@link SnapHelper#calculateDistanceToFinalSnap}.
 */
public class StartLinearSnapHelper extends LinearSnapHelper {

    @Override
    public int[] calculateDistanceToFinalSnap(
            @NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] out = super.calculateDistanceToFinalSnap(layoutManager, targetView);

        if (out == null) return new int[2];

        if (layoutManager.canScrollHorizontally()) {
            out[0] = out[0] + targetView.getMeasuredWidth() / 2;
        } else {
            out[0] = 0;
        }

        if (layoutManager.canScrollVertically()) {
            out[1] = out[1] + targetView.getMeasuredHeight() / 2;
        } else {
            out[1] = 0;
        }

        return out;
    }
}
