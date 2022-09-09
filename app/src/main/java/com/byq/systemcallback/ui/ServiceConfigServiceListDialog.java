package com.byq.systemcallback.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.byq.applib.BottomDialogUtil;
import com.byq.systemcallback.R;

public class ServiceConfigServiceListDialog extends BottomDialogUtil {
    private ImageView mAppIcon;
    private TextView mAppName;
    private TextView mEmptyText;
    private RecyclerView mServiceList;

    private PackageInfo packageInfo;
    private AppUtils.AppInfo appInfo;
    private ResultListener resultListener;

    public ResultListener getResultListener() {
        return resultListener;
    }

    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public ServiceConfigServiceListDialog(@NonNull Context context, PackageInfo packageInfo, AppUtils.AppInfo appInfo) {
        super(context);
        this.packageInfo = packageInfo;
        this.appInfo = appInfo;
    }

    public interface ResultListener {
        public void onReturnResult(PackageInfo packageInfo,ServiceInfo serviceInfo) ;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        getmTitleView().setText("选择Service");
        getmSubTitle().setVisibility(GONE);

        mAppIcon = findViewById(R.id.appIcon);
        mAppName = findViewById(R.id.appName);
        mEmptyText = findViewById(R.id.emptyText);
        mServiceList = findViewById(R.id.serviceList);

        mAppIcon.setImageDrawable(appInfo.getIcon());
        mAppName.setText(appInfo.getName());

        //Load list
        if (!(packageInfo.services == null || packageInfo.services.length == 0)) {
            mEmptyText.setVisibility(GONE);
            mServiceList.setLayoutManager(new LinearLayoutManager(getContext()));
            ServiceListAdapter adapter = new ServiceListAdapter(packageInfo);
            adapter.setOnServiceSelectListener(new ServiceListAdapter.OnServiceSelectListener() {
                @Override
                public void onSelect(PackageInfo packageInfo, ServiceInfo serviceInfo) {
                    dismiss();
                    if (resultListener != null) {
                        resultListener.onReturnResult(packageInfo,serviceInfo);
                    }
                }
            });
            mServiceList.setAdapter(adapter);

            if (packageInfo.services.length > 3) {
                mServiceList.getLayoutParams().height = ConvertUtils.dp2px(200);
                mServiceList.requestLayout();
            }
        }

    }

    @Override
    public int getContentViewImplId() {
        return R.layout.service_list_dialog_layout;
    }
}
