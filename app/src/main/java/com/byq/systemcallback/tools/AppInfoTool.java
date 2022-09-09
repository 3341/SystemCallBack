package com.byq.systemcallback.tools;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import com.blankj.utilcode.util.ServiceUtils;
import com.blankj.utilcode.util.Utils;

import java.util.List;

public class AppInfoTool {
    public static ServiceInfo[] getAppServices(Context context,String packageName) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES | PackageManager.GET_ACTIVITIES);
            return packageInfo.services;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getShortName(String name) {
        String[] split = name.split("\\.");
        if (split == null || split.length == 0) return name;
        return split[split.length-1];
    }

    public static boolean isServiceRunning(Context context,String name) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> info = am.getRunningServices(0x7FFFFFFF);
            if (info == null || info.size() == 0) return false;
            for (ActivityManager.RunningServiceInfo aInfo : info) {
                if (name.equals(aInfo.service.getClassName())) return true;
            }
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }
}
