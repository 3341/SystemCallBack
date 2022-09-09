package com.byq.systemcallback.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.blankj.utilcode.util.GsonUtils;
import com.byq.systemcallback.MainActivity;
import com.byq.systemcallback.R;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DataForm {
    private ServiceListenerJson mServiceConfigObject;
    @JsonProperty("serviceConfig")
    private String serviceConfig;
    @JsonProperty("checkDelay")
    private long checkDelay = 1000;

    private String encryptOnTran(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    public long getCheckDelay() {
        return checkDelay;
    }

    public void setCheckDelay(long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public ServiceListenerJson getmServiceConfigObject() {
        return mServiceConfigObject;
    }

    private String decryptOnTran(String content) {
        return new String(Base64.getDecoder().decode(content),StandardCharsets.UTF_8);
    }

    public static DataForm getInstanceFromSharedPreference(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MainActivity.CONFIG_SAVE_KEY, Context.MODE_PRIVATE);
        String dataFormJson = sharedPreferences.getString("dataFormJson", null);
        DataForm dataForm = null;
        if (dataFormJson == null) {
            dataForm = getInitializedConfig(context);
        } else {
            dataForm = GsonUtils.fromJson(dataFormJson,DataForm.class);
            dataForm.reduceJson();
        }
        return dataForm;
    }

    private static DataForm getInitializedConfig(Context context) {
        ServiceListenerJson serviceListenerJson = GsonUtils.fromJson(context.getString(R.string.serviceConfigDefault), ServiceListenerJson.class);
        DataForm dataForm = new DataForm();
        dataForm.mServiceConfigObject = serviceListenerJson;

        return dataForm;
    }

    public void saveDataForm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MainActivity.CONFIG_SAVE_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("dataFormJson",generateJson()).apply();
    }

    /**
     * 已更新数据时调用这个方法来保存
     */
    public void updateData(Context context) {
        saveDataForm(context);
    }

    /**
     * 构建传输本体
     * @return json
     */
    public String generateJson() {
        serviceConfig = encryptOnTran(GsonUtils.toJson(mServiceConfigObject));
        return GsonUtils.toJson(this);
    }

    /**
     * 还原数据本体
     */
    public void reduceJson() {
        mServiceConfigObject = GsonUtils.fromJson(decryptOnTran(serviceConfig),ServiceListenerJson.class);
    }

    /**
     * 将传输后的数据转换成dataform对象
     * @param json json
     * @return object
     */
    public static DataForm parseData(String json) {
        DataForm dataForm = GsonUtils.fromJson(json, DataForm.class);
        dataForm.reduceJson();

        return dataForm;
    }



    /**
     * Now there is a better solution to replace this method
     * 获取从sharedperferance中保存的配置
     * @param context context
     * @param configName config name
     * @param configClass must json format class
     * @param <T>
     * @return
     */
    @Deprecated()
    private static <T> T getConfig(Context context,String configName,String defaultConfigJson,Class<T> configClass) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MainActivity.CONFIG_SAVE_KEY, Context.MODE_PRIVATE);
        String serviceConfig = sharedPreferences.getString(configName, null);
        if (serviceConfig == null) {
            //Initialize
            String string = defaultConfigJson;
            sharedPreferences.edit().putString(configName, string).apply();
            serviceConfig = string;
        }

        return GsonUtils.fromJson(serviceConfig,configClass);
    }

    @Deprecated
    private static void saveConfig(Context context,String configName,Object configData) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(MainActivity.CONFIG_SAVE_KEY, Context.MODE_PRIVATE);
        String s = GsonUtils.toJson(configData);
        sharedPreferences.edit().putString(configName, s).apply();
    }
}
