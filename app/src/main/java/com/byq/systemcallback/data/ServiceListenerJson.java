package com.byq.systemcallback.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;

import com.blankj.utilcode.util.GsonUtils;
import com.byq.systemcallback.MainActivity;
import com.byq.systemcallback.R;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ServiceListenerJson {

    @JsonProperty("configCreateTime")
    public Long configCreateTime;
    @JsonProperty("lastChangTime")
    public Long lastChangTime;
    @JsonProperty("enabled")
    public Boolean enabled;
    @JsonProperty("config")
    public List<ConfigDTO> config;

    @NoArgsConstructor
    @Data
    public static class ConfigDTO {
        @JsonProperty("packageName")
        public String packageName;
        @JsonProperty("serviceName")
        public String serviceName;
        @JsonProperty("isKeepAlive")
        public boolean isKeepAlive = true;
        public long lastStartTime;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigDTO)) return false;
            ConfigDTO configDTO = (ConfigDTO) o;
            return packageName.equals(configDTO.packageName) && serviceName.equals(configDTO.serviceName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, serviceName);
        }
    }

    /**
     * 获取service 初始配置
     * @param serviceInfo
     */
    public static ConfigDTO initializeServiceConfig(ServiceInfo serviceInfo) {
        ConfigDTO configDTO = new ConfigDTO();
        configDTO.packageName = serviceInfo.packageName;
        configDTO.serviceName = serviceInfo.name;
        return configDTO;
    }

    private static String generateOnlyString(ConfigDTO c) {
        return c.packageName+"/"+c.serviceName;
    }

    public void addService(ServiceInfo serviceInfo) {
        if (config == null) config = new ArrayList<>();
        ConfigDTO configDTO = initializeServiceConfig(serviceInfo);
        if (!config.contains(configDTO)) {
            config.add(configDTO);
        }
    }
}
