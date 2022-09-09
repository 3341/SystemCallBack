package com.byq.systemcallback.broadcast;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byq.systemcallback.BuildConfig;

public abstract class CommunicatBroadcastForReplay extends CommunicateBroadcast{
    private Timer mTimer;
    private Context context;

    public CommunicatBroadcastForReplay(BroadcastEvent mEvent) {
        super(mEvent);
    }

    /**
     * 超时计时线程
     */
    private class Timer extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(getReplayMaxDelay());
                //time out
                Log.e(BuildConfig.APPLICATION_ID, "run: receive message timeout :" );
                mTimer = null;
                boolean b = onReceiveTimeout();
                if (b) {
                    startTimer();
                } else if (isAutoUnregister()) {
                    unregister();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAutoUnregister() {
        return true;
    }

    public void unregister() {
        if (context == null) throw new RuntimeException("Illegal param : You are not register by this register method");
        super.unregister(context);
    }

    @Override
    public final String getBroadcastType() {
        return BROADCAST_TYPE_REPLY_RECEIVER;
    }

    public void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.start();
        }
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (mTimer != null && !mTimer.isInterrupted()) {
            mTimer.interrupt();
        }
        if (isAutoUnregister()) {
            unregister();
        }
        onReplayReceived(context,intent);
    }

    public abstract void onReplayReceived(Context context,Intent intent) ;

    /**
     * 接收消息超时
     * @return 是否延长时间
     */
    public abstract boolean onReceiveTimeout();

    /**
     * 必须调用这个方法注册广播，否则无法自动注销
     * @param context
     */
    @Override
    public void register(Context context) {
        super.register(context);
        this.context = context;

    }

    public long getReplayMaxDelay() {
        return 2000; //default 2s
    }
}
