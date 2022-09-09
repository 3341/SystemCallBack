package com.byq.systemcallback.broadcast;

import android.content.Intent;

import com.blankj.utilcode.util.EncryptUtils;
import com.blankj.utilcode.util.GsonUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 广播事件
 */
public enum BroadcastEvent {
    NormalBroadcast, //通用广播事件，不建议使用
    ConnectResponse, //连接响应
    RepeatCheck, //重复检测广播
    DataSync, //数据同步
    ;

    public static BroadcastEvent getEventFromIntent(Intent intent) {
        String broEvent = intent.getStringExtra("broEvent");
        for (BroadcastEvent value : values()) {
            if (value.name().equals(broEvent)) {
                return value;
            }
        }

        return null;
    }

    public void setToIntent(Intent intent) {
        intent.putExtra("broEvent",name());
    }
}
