package com.byq.systemcallback.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Filter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.byq.applib.BottomDialogUtil;
import com.byq.systemcallback.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lxj.xpopup.XPopup;

public class ServiceConfigAppListDialog extends BottomDialogUtil {
    private boolean isInitializedAppList;
    private ResultListener resultListener;

    //Views
    private TextInputLayout mSearchLayout;
    private TextInputEditText mSearchText;
    private LinearLayout mLoadingLayout;
    private RecyclerView mAppList;

    public ResultListener getResultListener() {
        return resultListener;
    }

    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }

    public ServiceConfigAppListDialog(@NonNull Context context) {
        super(context);

    }

    public interface ResultListener {
        public void onResult(PackageInfo packageInfo, ServiceInfo serviceInfo);
    }

    @Override
    public int getContentViewImplId() {
        return R.layout.service_config_list_dialog_layout;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        getmTitleView().setText("选择应用");
        getmSubTitle().setVisibility(GONE);

        mSearchLayout = findViewById(R.id.searchLayout);
        mSearchText = findViewById(R.id.searchText);
        mLoadingLayout = findViewById(R.id.loadingLayout);
        mAppList = findViewById(R.id.appList);


        //Loading app list
        mAppList.setLayoutManager(new LinearLayoutManager(getContext()));
        AppListAdapter adapter = new AppListAdapter();
        adapter.attachRecy(mAppList);
        adapter.attachEmptyView(mLoadingLayout);
        mAppList.setAdapter(adapter);
        adapter.setAppFilter(new AppListAdapter.AppFilter() {
            @Override
            public boolean filter(PackageInfo info, AppUtils.AppInfo appInfo) {
                return !appInfo.isSystem();
            }
        });
        adapter.startSearchApp(getContext(), new Runnable() {
            @Override
            public void run() {
                mAppList.getLayoutParams().height = ConvertUtils.dp2px(300);
                mLoadingLayout.getLayoutParams().height = 0;
                mAppList.requestLayout();
                mSearchLayout.setVisibility(VISIBLE);
                mSearchText.setEnabled(true);
                isInitializedAppList = true;
            }
        });
        adapter.setOnAppClickListener(new AppListAdapter.OnAppClickListener() {
            @Override
            public void onAppClicked(PackageInfo packageInfo, AppUtils.AppInfo appInfo) {
                ServiceConfigServiceListDialog serviceConfigServiceListDialog = new ServiceConfigServiceListDialog(getContext(), packageInfo, appInfo);
                serviceConfigServiceListDialog.setResultListener(new ServiceConfigServiceListDialog.ResultListener() {
                    @Override
                    public void onReturnResult(PackageInfo packageInfo, ServiceInfo serviceInfo) {
                        dismiss();
                        if (resultListener != null) {
                            resultListener.onResult(packageInfo,serviceInfo);
                        }
                    }
                });
                new XPopup.Builder(getContext())
                        .asCustom(serviceConfigServiceListDialog)
                        .show();
            }
        });

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isInitializedAppList) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
