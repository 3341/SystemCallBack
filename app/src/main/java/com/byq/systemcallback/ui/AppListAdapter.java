package com.byq.systemcallback.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.AppUtils;
import com.byq.systemcallback.BuildConfig;
import com.byq.systemcallback.R;
import com.byq.systemcallback.tools.AppInfoTool;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends MyRecyclerViewAdapter {
    private int getAppInfoFlags = PackageManager.GET_SERVICES;
    private List<PackageInfo> datas;
    private List<AppUtils.AppInfo> appInfos;
    private List<AppUtils.AppInfo> filteredAppInfos = new ArrayList<>();
    private AppFilter appFilter; //初始化时的过滤：用于过滤系统应用之类的
    private OnAppClickListener onAppClickListener;
    private Filter searchFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppUtils.AppInfo> result = new ArrayList<>();
            if (appInfos != null) {
                filteredAppInfos.clear();
                for (AppUtils.AppInfo info : appInfos) {
                    if ((info.getName() + info.getPackageName()).contains(constraint)) {
                        result.add(info);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = result;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredAppInfos = (List<AppUtils.AppInfo>) results.values;
            notifyDataSetChanged();
        }
    }; //搜索过滤

    public OnAppClickListener getOnAppClickListener() {
        return onAppClickListener;
    }

    public void setOnAppClickListener(OnAppClickListener onAppClickListener) {
        this.onAppClickListener = onAppClickListener;
    }

    public void filter(String content) {
        searchFilter.filter(content);
    }

    public interface OnAppClickListener {
        public void onAppClicked(PackageInfo packageInfo, AppUtils.AppInfo appInfo);
    }


    public AppFilter getAppFilter() {
        return appFilter;
    }

    public void setAppFilter(AppFilter appFilter) {
        this.appFilter = appFilter;
    }

    public interface AppFilter {
        public boolean filter(PackageInfo info, AppUtils.AppInfo appInfo);
    }

    @Override
    public Integer getLayoutId() {
        return R.layout.app_list_item_layout;
    }

    public int getGetAppInfoFlags() {
        return getAppInfoFlags;
    }

    public void setGetAppInfoFlags(int getAppInfoFlags) {
        this.getAppInfoFlags = getAppInfoFlags;
    }

    @Override
    public MyViewHolder getViewHolder(View view) {
        return new MyViewHolder(view) {
            private LinearLayout mRtView;
            private ImageView mAppIcon;
            private TextView mAppName;
            private TextView mPackageName;

            @Override
            public void onCreate() {
                super.onCreate();
                mRtView = itemView.findViewById(R.id.rtView);
                mAppIcon = itemView.findViewById(R.id.appIcon);
                mAppName = itemView.findViewById(R.id.appName);
                mPackageName = itemView.findViewById(R.id.packageName);
            }

            @Override
            public void onBindView(int position) {
                AppUtils.AppInfo appInfo = filteredAppInfos.get(position);
                Drawable icon = appInfo.getIcon();
                mAppIcon.setImageDrawable(icon);
                mAppName.setText(appInfo.getName());
                mPackageName.setText(appInfo.getPackageName());

                mRtView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onAppClicked(searchPackInfoByPackageName(appInfo.getPackageName()),filteredAppInfos.get(position));
                    }
                });
            }
        };
    }

    private PackageInfo searchPackInfoByPackageName(String packageName) {
        for (PackageInfo info : datas) {
            if (info.packageName.equals(packageName)) {
                return info;
            }
        }
        Log.e(BuildConfig.APPLICATION_ID, "searchPackInfoByPackageName: not found package info "+packageName );
        return null;
    }

    /**
     * 实现这个方法以重写app的点击事件
     * Implement this method to override the click event
     */
    private void onAppClicked(PackageInfo packageInfo, AppUtils.AppInfo appInfo) {
        if (onAppClickListener != null) {
            onAppClickListener.onAppClicked(packageInfo,appInfo);
        }
    }

    private List<PackageInfo> getAppInfos(Context context) {
        List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(getAppInfoFlags);
        return installedPackages;
    }

    public void startSearchApp(Context context,Runnable searchDone) {
        Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                super.run();
                List<PackageInfo> appInfos = getAppInfos(context);
                datas = appInfos;

                convertAppInfo();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (searchDone != null) {
                            searchDone.run();
                        }
                        filter("");
                    }
                });
            }
        }.start();
    }

    private void convertAppInfo() {
        if (appInfos != null) {
            appInfos.clear();
        }
        appInfos = new ArrayList<>();
        if (datas != null) {
            for (PackageInfo data : datas) {
                AppUtils.AppInfo appInfo = AppUtils.getAppInfo(data.packageName);
                if (appFilter == null || appFilter.filter(data,appInfo)) {
                    appInfos.add(appInfo);
                }
            }
        }
    }

    @Override
    public int getCount() {
        if (datas  == null || appInfos == null || filteredAppInfos == null) return 0;
        return filteredAppInfos.size();
    }
}
