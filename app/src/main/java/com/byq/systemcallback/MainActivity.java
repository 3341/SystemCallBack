package com.byq.systemcallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.AppUtils;
import com.byq.applib.StatusUtil;
import com.byq.systemcallback.broadcast.BroadcastEvent;
import com.byq.systemcallback.broadcast.CommunicatBroadcastForReplay;
import com.byq.systemcallback.broadcast.CommunicateBroadcast;
import com.byq.systemcallback.data.DataForm;
import com.byq.systemcallback.data.ServiceListenerJson;
import com.byq.systemcallback.tools.AppInfoTool;
import com.byq.systemcallback.tools.LoadingDialogTool;
import com.byq.systemcallback.ui.MyRecyclerViewAdapter;
import com.byq.systemcallback.ui.ServiceConfigAppListDialog;
import com.byq.systemcallback.ui.ServiceListenerListAdapter;
import com.google.android.material.button.MaterialButton;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.lxj.xpopup.interfaces.OnSelectListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    private static final int CONNECT_STATUS_CONNECTED = 0;
    private static final int CONNECT_STATUS_DISCONNECT = 1;
    private static final int CONNECT_STATUS_LOADING = 2;
    private static final int CONNECT_STATUS_NEED_UPDATE = 3;
    private static final int CONNECT_SYNCING = 4;
    public static final String CONFIG_SAVE_KEY = "data";

    private Handler handler = new Handler();
    private DataForm dataForm; //总数据表
    private ServiceListenerListAdapter serviceListenerListAdapter;
    private boolean isSucceedToConnectSystem;
    private String currentSystemVersion;
    private long updateDelay = 1000; //更新时间间隔
    private UpdateThread updateThread;

    private LinearLayout mSystemOkBg;
    private ProgressBar mSystemCallProgress;
    private ImageView mSystemOkIcon;
    private TextView mSystemOkText;
    private TextView mSystemVersionText;
    private TextView mTestText;
    private RecyclerView mServiceList;
    private LinearLayout mServiceEmpty;
    private ImageView mImageView;
    private TextView mErrContent;
    private MaterialButton mAddServiceListener;
    private List<OnUpdateRequireListener> onUpdateRequireListeners = new ArrayList<>();

    public interface OnUpdateRequireListener {
        public void onUpdateRequire() ;
    }

    public void addOnUpdateRequireListener(OnUpdateRequireListener onUpdateRequireListener) {
        onUpdateRequireListeners.add(onUpdateRequireListener);
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                while(true) {
                    Thread.sleep(updateDelay);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (OnUpdateRequireListener listener : onUpdateRequireListeners) {
                                listener.onUpdateRequire();
                            }
                        }
                    });

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startUpdateThread() {
        updateThread = new UpdateThread();
        updateThread.start();
    }

    @Override
    protected void onDestroy() {
        if (updateThread != null) {
            updateThread.interrupted();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusUtil.setWhiteStatusBar(this);
        setContentView(R.layout.activity_main);

        mSystemOkBg = findViewById(R.id.systemOkBg);
        mSystemCallProgress = findViewById(R.id.systemCallProgress);
        mSystemOkIcon = findViewById(R.id.systemOkIcon);
        mSystemOkText = findViewById(R.id.systemOkText);
        mSystemVersionText = findViewById(R.id.systemVersionText);
        mTestText = findViewById(R.id.testText);
        mServiceList = findViewById(R.id.serviceList);
        mServiceEmpty = findViewById(R.id.serviceEmpty);
        mImageView = findViewById(R.id.imageView);
        mErrContent = findViewById(R.id.errContent);
        mAddServiceListener = findViewById(R.id.addServiceListener);

        startConnect();
        mSystemOkBg.setOnClickListener(new View.OnClickListener() {
            private long time;
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - time > 2000) {
                    time = System.currentTimeMillis();
                    startConnect();
                } else {
                    Toasty.error(MainActivity.this,"请不要频繁点击").show();
                }
            }
        });

        mAddServiceListener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceConfigAppListDialog serviceDia = new ServiceConfigAppListDialog(MainActivity.this);
                serviceDia.setResultListener(new ServiceConfigAppListDialog.ResultListener() {
                    @Override
                    public void onResult(PackageInfo packageInfo, ServiceInfo serviceInfo) {
                        //已经选取Service
                        dataForm.getmServiceConfigObject().addService(serviceInfo);
                        mServiceList.getAdapter().notifyDataSetChanged();
                        onUpdateData();
                        Toasty.success(MainActivity.this,"操作有效").show();
                    }
                });
                new XPopup.Builder(MainActivity.this)
                        .dismissOnBackPressed(false)
                         .dismissOnTouchOutside(false)
//                        .maxHeight((int) (getResources().getDisplayMetrics().heightPixels*0.6f))
                        .asCustom(serviceDia)
                        .show();
            }
        });
        mSystemOkBg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String[] items = {"重复检测","立即同步数据","查询版本"};
                new XPopup.Builder(MainActivity.this)
                        .asBottomList("操作", items, new OnSelectListener() {
                            private int checkedRepeatCount;

                            @Override
                            public void onSelect(int position, String text) {
                                switch (position) {
                                    case 0: {
                                        LoadingPopupView loadingPopupView = new XPopup.Builder(MainActivity.this).asLoading("0%");
                                        loadingPopupView.show();
                                        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 100);
                                        valueAnimator.setDuration(1500);

                                        CommunicatBroadcastForReplay replay = new CommunicatBroadcastForReplay(BroadcastEvent.ConnectResponse) {
                                            @Override
                                            public void onReplayReceived(Context context, Intent intent) {
                                                checkedRepeatCount++;
                                            }

                                            @Override
                                            public boolean onReceiveTimeout() {
                                                return false;
                                            }

                                            @Override
                                            public boolean isAutoUnregister() {
                                                return false;
                                            }
                                        };
                                        CommunicateBroadcast.sendBroadcast(MainActivity.this,
                                                BroadcastEvent.ConnectResponse, new Intent(), replay);
                                        TextView loadingDialogTextView = LoadingDialogTool.getLoadingDialogTextView(loadingPopupView);
                                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            @Override
                                            public void onAnimationUpdate(ValueAnimator animation) {
                                                float value = (float) animation.getAnimatedValue();
                                                loadingDialogTextView.setText(String.format("%.1f",value)+"%");
                                            }
                                        });
                                        valueAnimator.addListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation, boolean isReverse) {
                                                super.onAnimationEnd(animation, isReverse);
                                                loadingPopupView.dismiss();
                                                replay.unregister();
                                                new XPopup.Builder(MainActivity.this)
                                                        .asConfirm("检测结果"
                                                        ,"共 "+checkedRepeatCount+" 个",null)
                                                        .show();
                                            }
                                        });
                                        valueAnimator.start();
                                        break;
                                    }

                                    case 1: {
                                        onUpdateData();
                                        break;
                                    }

                                    case 2: {
                                        String content = String.format("应用版本：%s\n框架版本：%s\n当前框架版本：%s",
                                                MainHook.CURRENT_VERSION,MainHook.CURRENT_VERSION,currentSystemVersion);
                                        new XPopup.Builder(MainActivity.this)
                                                .asConfirm("查询结果",content,null).show();
                                        break;
                                    }
                                }
                            }
                        }).show();
                return true;
            }
        });

        initializeDataForm();
        loadServiceListenerList();
//        startUpdateThread();
    }

    private void switchConnectStatusBgColor(int color) {
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(mSystemOkBg.getBackgroundTintList().getDefaultColor(), color);
        valueAnimator.setDuration(500);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int color = (int) animation.getAnimatedValue();
                mSystemOkBg.setBackgroundTintList(ColorStateList.valueOf(color));
            }
        });
        valueAnimator.start();
    }

    private void switchConnectStatus(int status) {
        switch (status) {
            case CONNECT_STATUS_LOADING:
                switchConnectStatusBgColor(getColor(R.color.yellow));
                mSystemOkText.setText("正在尝试连接框架");
                mSystemOkIcon.setVisibility(View.GONE);
                mSystemVersionText.setVisibility(View.GONE);
                mSystemCallProgress.setVisibility(View.VISIBLE);
                break;

            case CONNECT_STATUS_DISCONNECT:
                switchConnectStatusBgColor(getColor(R.color.red));
                mSystemOkText.setText("系统框架无响应");
                mSystemOkIcon.setVisibility(View.VISIBLE);
                mSystemCallProgress.setVisibility(View.GONE);
                mSystemVersionText.setVisibility(View.GONE);
                mSystemOkIcon.setImageResource(R.drawable.ic_twotone_error_24);
                break;

            case CONNECT_STATUS_CONNECTED:
                if (!currentSystemVersion.equals(MainHook.CURRENT_VERSION)) {
                    switchConnectStatus(CONNECT_STATUS_NEED_UPDATE);
                    return;
                }
                switchConnectStatusBgColor(getColor(R.color.green));
                boolean equals = currentSystemVersion.equals(MainHook.CURRENT_VERSION);

                mSystemOkText.setText("系统框架已响应");
                mSystemVersionText.setText("框架版本 "+currentSystemVersion+(equals?"":"(不同步 需更新)"));
                mSystemOkIcon.setVisibility(View.VISIBLE);
                mSystemCallProgress.setVisibility(View.GONE);
                mSystemVersionText.setVisibility(View.VISIBLE);
                mSystemOkIcon.setImageResource(R.drawable.ic_twotone_check_circle_24);
                break;

            case CONNECT_STATUS_NEED_UPDATE:
                switchConnectStatusBgColor(getColor(R.color.yellow));
                mSystemOkText.setText("系统框架已响应");
                mSystemOkIcon.setVisibility(View.VISIBLE);
                mSystemCallProgress.setVisibility(View.GONE);
                mSystemVersionText.setVisibility(View.VISIBLE);
                mSystemOkIcon.setImageResource(es.dmoral.toasty.R.drawable.ic_error_outline_white_24dp);
                break;

            case CONNECT_SYNCING:
                switchConnectStatusBgColor(getColor(R.color.yellow));
                mSystemOkText.setText("正在同步数据");
                mSystemOkIcon.setVisibility(View.GONE);
                mSystemCallProgress.setVisibility(View.VISIBLE);
                mSystemVersionText.setVisibility(View.VISIBLE);
                mSystemOkIcon.setImageResource(es.dmoral.toasty.R.drawable.ic_error_outline_white_24dp);
                break;
        }
    }

    private void startConnect() {
        isSucceedToConnectSystem = false;
        switchConnectStatus(CONNECT_STATUS_LOADING);
        CommunicateBroadcast.sendBroadcast(this, BroadcastEvent.ConnectResponse, new Intent(), new CommunicatBroadcastForReplay(BroadcastEvent.ConnectResponse) {
            @Override
            public void onReplayReceived(Context context, Intent intent) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(BuildConfig.APPLICATION_ID, "startConnect: connected broadcast");

                        String intentVersion = intent.getExtras().getString("version");
                        currentSystemVersion = intentVersion;
                        String version = "框架版本 " + intentVersion;
                        if (!intentVersion.equals(MainHook.CURRENT_VERSION)) {
                            version = version + " (不同步 需更新)";
                        }
                        mSystemVersionText.setText(version);
                        switchConnectStatus(CONNECT_STATUS_CONNECTED);
                        isSucceedToConnectSystem = true;
                        onUpdateData();
                    }
                },1000);
            }

            @Override
            public boolean onReceiveTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(BuildConfig.APPLICATION_ID, "startConnect: failed to connect broadcast");
                        switchConnectStatus(CONNECT_STATUS_DISCONNECT);
                    }
                });
                return false;
            }
        });
        Log.i(BuildConfig.APPLICATION_ID, "startConnect: send connect broadcast");
    }

    private void test() {
        CommunicateBroadcast communicateBroadcast = new CommunicateBroadcast(BroadcastEvent.NormalBroadcast) {
            @Override
            public void onReceive(Context context, Intent intent) {
                printlnToTestText("Received : "+intent.getStringExtra("data"));
                printlnToTestText("Try send reply...");
                Intent aIntent = new Intent();
                aIntent.putExtra("data","replay send");
                sendReplay(context,BroadcastEvent.NormalBroadcast,aIntent);
            }
        };
        communicateBroadcast.register(this);

        Intent sendIntent = new Intent();
        sendIntent.putExtra("data","SendMessage");
        CommunicateBroadcast.sendBroadcast(this,BroadcastEvent.NormalBroadcast,sendIntent
                , new CommunicatBroadcastForReplay(BroadcastEvent.NormalBroadcast) {
                    @Override
                    public void onReplayReceived(Context context, Intent intent) {

                    }

                    @Override
                    public boolean onReceiveTimeout() {
                        return false;
                    }
                });
    }

    private void initializeDataForm() {
        dataForm = DataForm.getInstanceFromSharedPreference(this); //Read config
    }

    private void loadServiceListenerList() {
        mServiceList.setLayoutManager(new LinearLayoutManager(this)); //set layout manager
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int bindingAdapterPosition = viewHolder.getBindingAdapterPosition();
                mServiceList.getAdapter().notifyItemChanged(bindingAdapterPosition);
                ServiceListenerJson.ConfigDTO configDTO = dataForm.getmServiceConfigObject().config.get(viewHolder.getBindingAdapterPosition());
                new XPopup.Builder(MainActivity.this)
                        .asConfirm("删除", "你确定删除 " + configDTO.serviceName + " 吗？", new OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                dataForm.getmServiceConfigObject().config.remove(bindingAdapterPosition);
                                mServiceList.getAdapter().notifyItemRemoved(bindingAdapterPosition);
                                onUpdateData();
                            }
                        }).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(mServiceList);

        ServiceListenerListAdapter adapter = new ServiceListenerListAdapter(dataForm);
        adapter.attachRecy(mServiceList);
        adapter.attachEmptyView(findViewById(R.id.serviceEmpty));

        mServiceList.setAdapter(adapter);
    }

    private void switchSyncStatus(boolean isSyncing) {
        if (isSyncing) {
            switchConnectStatus(CONNECT_SYNCING);
        } else {
            switchConnectStatus(CONNECT_STATUS_CONNECTED);
        }
    }

    /**
     * 启动数据同步
     */
    public void onUpdateData() {
        dataForm.updateData(this);

        if (isSucceedToConnectSystem) {
            switchSyncStatus(true);
            Intent intent = new Intent();
            intent.putExtra("dataForm",dataForm.generateJson());
            CommunicateBroadcast.sendBroadcast(this,BroadcastEvent.DataSync,intent,new CommunicatBroadcastForReplay(BroadcastEvent.DataSync) {

                @Override
                public void onReplayReceived(Context context, Intent intent) {
                    int resultCode = intent.getIntExtra("resultCode", 1);
                    if (resultCode == 0) {
                        //null
                    } else {
                        Toasty.error(MainActivity.this,"错误：向系统框架同步数据出错").show();
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switchSyncStatus(false);
                        }
                    },500);
                }

                @Override
                public boolean onReceiveTimeout() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toasty.error(MainActivity.this,"错误：向系统框架同步数据无响应").show();
                        }
                    });
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switchSyncStatus(false);
                            startConnect();
                        }
                    },500);
                    return false;
                }
            });
        } else {
            switchConnectStatusBgColor(getColor(R.color.yellow));
            Toasty.error(this,"未连接系统框架，无法进行数据同步").show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    switchConnectStatusBgColor(getColor(R.color.red));
                }
            },500);
        }
    }

    private void printlnToTestText(String content) {
        mTestText.append(content+"\n");
    }
}