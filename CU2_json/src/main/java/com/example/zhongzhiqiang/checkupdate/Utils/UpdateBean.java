package com.example.zhongzhiqiang.checkupdate.Utils;

/**
 * Created by TwilightKHQ on 19-4-16.
 */

public class UpdateBean {
    /**
     * versionCode : 2
     * versionName : 1.0.2
     * downloadUrl : https://github.com/TwilightKHQ/MyPlayAndroid/releases/download/1.0/app-release.apk
     */
    private int versionCode;
    private String versionName;
    private String downloadUrl;

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String url) {
        this.downloadUrl = downloadUrl;
    }
}
