package com.byq.systemcallback.tools;

import android.widget.TextView;

import com.lxj.xpopup.impl.LoadingPopupView;

import java.lang.reflect.Field;

public class LoadingDialogTool {
    public static TextView getLoadingDialogTextView(LoadingPopupView loadingPopupView) {
        try {
            Field tv_title = LoadingPopupView.class.getDeclaredField("tv_title");
            boolean accessible = tv_title.isAccessible();
            tv_title.setAccessible(true);
            Object o = tv_title.get(loadingPopupView);
            tv_title.setAccessible(accessible);

            return (TextView) o;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
