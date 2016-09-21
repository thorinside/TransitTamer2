package org.nsdev.apps.transittamer.utils;

import android.databinding.BindingAdapter;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

/**
 * Created by neal on 2016-09-04.
 */

public class ViewBindingAdapters {

    @BindingAdapter("isGone")
    public static void setIsGone(View view, boolean hide) {
        view.setVisibility(hide ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("isInvisible")
    public static void setIsInvisible(View view, boolean hide) {
        view.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter("isBold")
    public static void setIsBold(TextView view, boolean bold) {
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        } else {
            view.setTypeface(null, Typeface.NORMAL);
        }
    }
}
