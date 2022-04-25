package com.example.zhongzhiqiang.checkupdate.Utils.Download;

/**
 * Created by TwilightKHQ on 19-3-26.
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();

}
