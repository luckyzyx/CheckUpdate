package com.luckyzyx.DownloadManager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;


public class APKUtil {

    /**
     * 获取已经下载的apk包文件的信息
     *
     */
/*    public static DownloadedApkInfo apkInfo(String absPath, Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = absPath;
            appInfo.publicSourceDir = absPath;
            String appName = pm.getApplicationLabel(appInfo).toString();// 得到应用名
            String packageName = appInfo.packageName; // 得到包名
            String version = pkgInfo.versionName; // 得到版本信息
            int versionCode = pkgInfo.versionCode;
            String pkgInfoStr = String.format("PackageName:%s, Vesion: %s, AppName: %s", packageName, version, appName);
            Log.e("apkInfo", pkgInfoStr);
            return new DownloadedApkInfo(packageName, version, versionCode, appName);
        } else {
            return null;
        }
    }*/

    /**
     * 安装apk，兼容Android N
     *
     */
    public static void installApk(Context context, File file) {
        Intent intentInstall = new Intent();
        intentInstall.setAction(Intent.ACTION_VIEW);
        intentInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //above7.0
        Uri uri = FileProvider.getUriForFile(context.getApplicationContext(), context.getApplicationContext().getPackageName() + ".provider", file);
        //给目标应用设置权限（必要）
        intentInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentInstall.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intentInstall);
    }
}
