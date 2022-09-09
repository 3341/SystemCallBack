package com.byq.systemcallback.ui;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ServiceUtils;
import com.byq.systemcallback.BuildConfig;
import com.byq.systemcallback.R;
import com.byq.systemcallback.data.DataForm;
import com.byq.systemcallback.data.ServiceListenerJson;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnSelectListener;

import java.util.ArrayList;
import java.util.List;

public class ServiceListenerListAdapter extends MyRecyclerViewAdapter {
    private DataForm dataForm;
    private OnUpdateListener onUpdateListener;

    public OnUpdateListener getOnUpdateListener() {
        return onUpdateListener;
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    public interface OnUpdateListener {
        public void onUpdate();
    }

    public ServiceListenerListAdapter(DataForm dataForm) {
        this.dataForm = dataForm;
    }

    @Override
    public Integer getLayoutId() {
        return R.layout.service_config_list_item;
    }



    @Override
    public MyViewHolder getViewHolder(View view) {
        return new MyViewHolder(view) {
            private ImageView mAppIcon;
            private TextView mServiceName;
            private TextView mDetailServiceName;
            private ImageView mServiceListenerStatusIcon;

            @Override
            public void onCreate() {
                super.onCreate();
                mAppIcon = itemView.findViewById(R.id.appIcon);
                mServiceName = itemView.findViewById(R.id.serviceName);
                mDetailServiceName = itemView.findViewById(R.id.detailServiceName);
                mServiceListenerStatusIcon = itemView.findViewById(R.id.serviceListenerStatusIcon);
            }

            @Override
            public void onBindView(int position) {
                ServiceListenerJson.ConfigDTO configDTO = getConfigs().get(position);
                String appName = AppUtils.getAppName(configDTO.packageName);
                mAppIcon.setImageDrawable(AppUtils.getAppIcon(configDTO.packageName));
                String serviceName = configDTO.serviceName;
                String[] split = serviceName.split(".");
                if (split.length != 0) {
                    serviceName = split[split.length-1];
                }
                mServiceName.setText(appName);
                try {
                    mDetailServiceName.setText(configDTO.serviceName);
                    setStatusIcon(configDTO.isKeepAlive);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(BuildConfig.APPLICATION_ID, "onBindView: parse service failed "+GsonUtils.toJson(configDTO) );
                }
                if (ServiceUtils.isServiceRunning(configDTO.serviceName)) {
                    mServiceName.setTextColor(itemView.getContext().getColor(com.byq.applib.R.color.colorAccent));
                } else {
                    mServiceName.setTextColor(itemView.getContext().getColor(R.color.black));
                }

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        configDTO.isKeepAlive = !configDTO.isKeepAlive;
                        dataForm.updateData(v.getContext());
                        setStatusIcon(configDTO.isKeepAlive);
                        if (onUpdateListener != null) {
                            onUpdateListener.onUpdate();
                        }
                    }
                });

                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        String[] items = {"查询运行状态","更新列表","更新此项"};
                        new XPopup.Builder(itemView.getContext())
                                .asBottomList("操作",items, new OnSelectListener() {
                                    @Override
                                    public void onSelect(int position, String text) {
                                        switch(position) {
                                            case 0 : {
                                                boolean serviceRunning = ServiceUtils.isServiceRunning(configDTO.serviceName);
                                                String content = "运行状态："+(serviceRunning?"正在":"未")+"运行";
                                                new XPopup.Builder(v.getContext())
                                                        .asConfirm("查询结果",content,null)
                                                        .show();
                                                break;
                                            }

                                            case 1 : {
                                                notifyDataSetChanged();
                                                break;
                                            }

                                            case 2 : {
                                                notifyItemChanged(position);
                                                break;
                                            }
                                        }
                                    }
                                }).show();
                        return true;
                    }
                });
            }

            public void setStatusIcon(boolean isActive) {
                if (isActive) {
                    setImageTintColor(mServiceListenerStatusIcon,itemView.getContext().getColor(R.color.green));
                } else {
                    setImageTintColor(mServiceListenerStatusIcon,itemView.getContext().getColor(R.color.uncheck));
                }
            }

            private void setImageTintColor(ImageView image,int color) {
                ValueAnimator valueAnimator = ValueAnimator.ofArgb(image.getImageTintList().getDefaultColor(), color);
                valueAnimator.setDuration(500);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int cc = (int) animation.getAnimatedValue();
                        image.setImageTintList(ColorStateList.valueOf(cc));
                    }
                });
                valueAnimator.start();
            }
        };
    }

    private List<ServiceListenerJson.ConfigDTO> getConfigs() {
        return getServiceConfig().config;
    }

    private ServiceListenerJson getServiceConfig() {
        if (dataForm.getmServiceConfigObject().config == null) {
            dataForm.getmServiceConfigObject().config = new ArrayList<>();
        }
        return dataForm.getmServiceConfigObject();
    }

    @Override
    public int getCount() {
        int size =  getConfigs().size();
        return size;
    }
}
