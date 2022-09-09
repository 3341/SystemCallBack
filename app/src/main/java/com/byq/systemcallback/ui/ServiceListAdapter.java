package com.byq.systemcallback.ui;

import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.byq.systemcallback.R;
import com.byq.systemcallback.tools.AppInfoTool;

public class ServiceListAdapter extends MyRecyclerViewAdapter {
    private PackageInfo packageInfo;
    private ServiceInfo[] serviceInfos;
    private OnServiceSelectListener onServiceSelectListener;

    public ServiceListAdapter(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
        serviceInfos = packageInfo.services;
    }

    public OnServiceSelectListener getOnServiceSelectListener() {
        return onServiceSelectListener;
    }

    public void setOnServiceSelectListener(OnServiceSelectListener onServiceSelectListener) {
        this.onServiceSelectListener = onServiceSelectListener;
    }

    @Override
    public Integer getLayoutId() {
        return R.layout.service_list_item_layout;
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
                ServiceInfo serviceInfo = serviceInfos[position];
                mAppName.setText(AppInfoTool.getShortName(serviceInfo.name));
                mPackageName.setText(serviceInfo.name);
                mRtView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onServiceSelectListener != null) {
                            onServiceSelectListener.onSelect(packageInfo,serviceInfo);
                        }
                    }
                });
            }
        };
    }

    public interface OnServiceSelectListener {
        public void onSelect(PackageInfo packageInfo,ServiceInfo serviceInfo) ;
    }

    @Override
    public int getCount() {
        if (serviceInfos == null) {
            return 0;
        }
        return serviceInfos.length;
    }
}
