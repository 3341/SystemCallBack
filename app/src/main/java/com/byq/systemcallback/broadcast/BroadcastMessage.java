package com.byq.systemcallback.broadcast;

import android.content.Intent;
import android.util.Log;

import com.blankj.utilcode.util.GsonUtils;
import com.byq.systemcallback.BuildConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message Json Format
 * 不再使用此类传递消息
 * 直接使用Intent
 */
@Deprecated
@NoArgsConstructor
@Data
public class BroadcastMessage {
    private static final String MESSAGE_SAVE_KEY= "dataAndMsg";

    @JsonProperty("event")
    private String event;
    @JsonProperty("sendTime")
    private Long sendTime;
    @JsonProperty("data")
    private String data;

    private BroadcastEvent actuallyEvent;
    private Intent intent;

    public BroadcastMessage(BroadcastEvent event) {
        this.event = event.name();
        this.actuallyEvent = event;
    }

    public BroadcastMessage(BroadcastEvent event, String data) {
        this.event = event.name();
        this.actuallyEvent = event;
        this.data = data;
    }

    public static BroadcastMessage parseMessageFromIntent(Intent intent) {
        String stringExtra = intent.getStringExtra(MESSAGE_SAVE_KEY);
        if (stringExtra != null) {
            BroadcastMessage broadcastMessage = GsonUtils.fromJson(stringExtra, BroadcastMessage.class);
            broadcastMessage.intent = intent;
            return broadcastMessage;
        }
        Log.e(BuildConfig.APPLICATION_ID, "parseMessageFromIntent: parse message failed" );
        return null;
    }

    /**
     * 构造传输信息本体
     * @return msg
     */
    public String generateMessage() {
        return GsonUtils.toJson(this);
    }

    /**
     * 还原信息本体
     */
    public static BroadcastMessage recycleMessage(String content) {
        BroadcastMessage broadcastMessage = GsonUtils.fromJson(content, BroadcastMessage.class);
        broadcastMessage.actuallyEvent = broadcastMessage.parseBroadcastEvent();
        return broadcastMessage;
    }

    private BroadcastEvent parseBroadcastEvent() {
        for (BroadcastEvent value : BroadcastEvent.values()) {
            if (value.name().equals(event)) {
                return value;
            }
        }
        return null;
    }

    public BroadcastEvent getActuallyEvent() {
        return actuallyEvent;
    }

    @Override
    public String toString() {
        return "BroadcastMessage{" +
                "event='" + event + '\'' +
                ", sendTime=" + sendTime +
                ", data='" + data + '\'' +
                ", data='" + data + '\'' +
                ", sendTime=" + sendTime +
                '}';
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
