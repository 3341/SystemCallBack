package com.byq.systemcallback;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ServiceUtils;
import com.byq.systemcallback.broadcast.BroadcastEvent;
import com.byq.systemcallback.broadcast.CommunicatBroadcastForReplay;
import com.byq.systemcallback.broadcast.CommunicateBroadcast;
import com.byq.systemcallback.data.DataForm;
import com.byq.systemcallback.data.ServiceListenerJson;
import com.byq.systemcallback.tools.AppInfoTool;

import java.util.ArrayList;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import es.dmoral.toasty.Toasty;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "MainServiceHook";
    public static final String CURRENT_VERSION = "1.4.8";
    private static final String FIELD_DATA_FORM = "dataForm";
    private static final String FIELD_HANDLER = "handler";
    private static final String FIELD_SERVICE_LISTENER_THREAD = "serviceListenerThread";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.miui.securitycenter")) {
            Log.i(BuildConfig.APPLICATION_ID, "handleLoadPackage: Hook start.");
            XposedHelpers.findAndHookMethod("com.miui.securitycenter.Application", lpparam.classLoader
                    , "onCreate", new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            Log.e(TAG, "afterHookedMethod: onCreate Method Hook Succeed" );
                            Context context = (Context) param.thisObject;
                            long createTime = System.currentTimeMillis();
                            ArrayList<String> strings = new ArrayList<>();
                            Handler handler = new Handler();

                            HashMap<String,Object> fieldData = new HashMap<>(); //服务端字段数据将在这里储存
                            fieldData.put(FIELD_HANDLER,handler);

                            CommunicateBroadcast.sendBroadcast(context, BroadcastEvent.RepeatCheck, new Intent()
                                    , new CommunicatBroadcastForReplay(BroadcastEvent.RepeatCheck) {
                                @Override
                                public void onReplayReceived(Context context, Intent intent) {
                                    Toast.makeText(context,"已检测到存在的接收器，取消启动",Toast.LENGTH_SHORT).show();
                                    strings.add("");
                                }

                                @Override
                                public boolean onReceiveTimeout() {
                                    return false;
                                }
                            });

                            CommunicateBroadcast communicateBroadcast = new CommunicateBroadcast() {

                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    BroadcastEvent broadcastEventByExtra = getBroadcastEventByExtra(intent);
                                    switch (broadcastEventByExtra) {
                                        case ConnectResponse: {
                                            Log.i(TAG, "onReceive: received connect require.");
                                            Intent intent1 = new Intent();
                                            intent1.putExtra("version",CURRENT_VERSION);
                                            sendReplay(context,BroadcastEvent.ConnectResponse, intent1);
                                            break;
                                        }

                                        case RepeatCheck: {
                                            Log.i(TAG, "onReceive: detected repeat object , sending to end up");
                                            Intent repeatIntent = new Intent();
                                            sendReplay(context,BroadcastEvent.RepeatCheck, repeatIntent);
                                            break;
                                        }

                                        case DataSync: {
                                            Log.i(TAG, "onReceive: received data sync require.");
                                            String dataFormJson = intent.getStringExtra("dataForm");
                                            Intent replay = new Intent();
                                            int resultCode = 1; //0成功 1失败
                                            try {
                                                DataForm dataForm = GsonUtils.fromJson(dataFormJson, DataForm.class);
                                                dataForm.reduceJson();
                                                fieldData.put(FIELD_DATA_FORM,dataForm);
                                                resultCode = 0;
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                resultCode = 1;
                                                Log.e(TAG, "onReceive: failed to parse data form" );
                                            }
                                            replay.putExtra("resultCode",resultCode);
                                            sendReplay(context,BroadcastEvent.DataSync,replay);
                                            break;
                                        }
                                    }
                                }
                            };


                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (strings.size() != 0) return; //终止
                                    //初始化响应类型
                                    communicateBroadcast.addSupplyEvent(BroadcastEvent.ConnectResponse);
                                    communicateBroadcast.addSupplyEvent(BroadcastEvent.RepeatCheck);
                                    communicateBroadcast.addSupplyEvent(BroadcastEvent.DataSync);
                                    communicateBroadcast.register((Context) param.thisObject);
                                    Toast.makeText(context,"系统框架已启动",Toast.LENGTH_SHORT).show();

                                    //Broadcast Start Successful, start initialize service
                                    processServiceListener(context,fieldData);
                                }
                            },1000);


                            //重复检测
//                            Intent intent = new Intent();
//                            intent.putExtra("createTime",createTime);
//                            Log.i(TAG, "afterHookedMethod: try to check repeat object");
//                            CommunicateBroadcast.sendBroadcast(context, BroadcastEvent.RepeatCheck, intent,
//                                    new CommunicatBroadcastForReplay(BroadcastEvent.RepeatCheck) {
//                                @Override
//                                public void onReplayReceived(Context context, Intent intent) {
//                                    long createTime1 = intent.getLongExtra("createTime", 0);
//                                    Log.i(TAG, "onReplayReceived: received end require");
//                                    if (createTime1 != createTime) {
//                                        Log.i(TAG, "onReplayReceived: try to end of "+createTime);
//                                        Toast.makeText(context, "检测到已有接收器存在，注销中...", Toast.LENGTH_SHORT).show();
//                                        communicateBroadcast.unregister(context);
//                                    }
//                                }
//
//                                @Override
//                                public boolean onReceiveTimeout() {
//                                    return false;
//                                }
//                            });
                        }
                    });
        }
    }

    private static class ServiceListenerThread extends Thread {
        private Context context;
        private HashMap<String,Object> datas;
        private long lastTipNoDataTime;

        public ServiceListenerThread(Context context, HashMap<String, Object> datas) {
            this.context = context;
            this.datas = datas;
        }

        @Override
        public void run() {
            super.run();
            try {
                Log.i(TAG, "run: service listener thread start");
                Handler handler = (Handler) datas.get(FIELD_HANDLER);
                if (handler == null) {
                    XposedBridge.log("Error: handler is null");
                    Log.e(TAG, "run: failed to initialize handler");
                    Log.e(TAG, "run: initialize service listener thread failed." );
                    return ;
                }
                while (true) {
                    long delay = 1000;
                    DataForm dataForm = (DataForm) datas.get(FIELD_DATA_FORM);
                    if (dataForm != null) {
                        delay = dataForm.getCheckDelay();
                    }
                    sleep(delay);
                    if (dataForm != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                for (ServiceListenerJson.ConfigDTO configDTO : dataForm.getmServiceConfigObject().config) {
                                    boolean serviceRunning = AppInfoTool.isServiceRunning(context,configDTO.serviceName);
                                    if (!serviceRunning && configDTO.isKeepAlive) {
                                        if (!(System.currentTimeMillis() - configDTO.lastStartTime > 3 * 1000)) {
                                            Log.i(TAG, "run:Starts up too often ： "+(System.currentTimeMillis() - configDTO.lastStartTime)+"\n" +
                                                    "running:"+serviceRunning+"\n" +
                                                    "name:"+String.format("%s/%s",configDTO.packageName,configDTO.serviceName));
                                            continue;
                                        }
                                        Log.i(TAG, "run: try start service :"+String.format("%s/%s",configDTO.packageName,configDTO.serviceName));
                                        Toast.makeText(context,"尝试启动 "+ AppInfoTool.getShortName(configDTO.serviceName),Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent();
                                        intent.setComponent(new ComponentName(configDTO.packageName,configDTO.serviceName));
                                        context.startService(intent);
                                        configDTO.lastStartTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        });

                    } else {
                        Log.e(TAG, "run: data not sync" );
                        if (System.currentTimeMillis() - lastTipNoDataTime >= 3000) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context,"数据丢失，请打开系统框架控制软件同步数据",Toast.LENGTH_SHORT).show();
                                }
                            });
                            lastTipNoDataTime = System.currentTimeMillis();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processServiceListener(Context context,HashMap<String,Object> datas) {
        ServiceListenerThread serviceListenerThread = new ServiceListenerThread(context, datas);
        datas.put(FIELD_SERVICE_LISTENER_THREAD,serviceListenerThread);
        serviceListenerThread.start();
    }
}
