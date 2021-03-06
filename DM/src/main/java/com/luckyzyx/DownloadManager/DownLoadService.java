package com.luckyzyx.DownloadManager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DownLoadService extends Service{

    private static final int DOWNLOADFLAG = 1;
    private DownloadManager downloadManager;
    private long downloadId;
    private DownLoadService downLoadService;
    private DownLoadProgressListener downLoadProgressListener;
    private ScheduledExecutorService executor;
    private File file;
    private static final String TAG = "DownLoadService";
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (downLoadProgressListener != null && msg.what == DOWNLOADFLAG) {
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    downLoadProgressListener.onProgress((int) (msg.arg1 * 1f / msg.arg2 * 100f));
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        downLoadService = this;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        String apkUrl = intent.getStringExtra("apkUrl");
        String apkName = intent.getStringExtra("apkName");
        downLoadApk(apkUrl,apkName);
        return new DownLoadBinder();
    }

    public String queryStatus() {
        if (downloadId == 0) {
            return "ζζ ηΆζ";
        }
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        String statusMsg = "";
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_RUNNING:
                    statusMsg = "STATUS_RUNNING";
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    statusMsg = "STATUS_SUCCESSFUL";
                    break;
                case DownloadManager.STATUS_FAILED:
                    statusMsg = "STATUS_FAILED";
                    break;
                default:
                    statusMsg = "ζͺη₯ηΆζ";
                    break;
            }
        }
        return statusMsg;
    }

    public class DownLoadBinder extends Binder {
        public DownLoadService getService() {
            return downLoadService;
        }
    }

    private void downLoadApk(String apkUrl,String apkName) {
        //θ·εDownLoadManagerε―Ήθ±‘
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = getDownloadId(apkUrl, apkName);
        getCurrentProgress();
        registBroadCast();
    }

    private void registBroadCast() {
        //ζ³¨εδΈθ½½ε?ζ―εΉΏζ­
        IntentFilter filter_complete = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver_complete, filter_complete);
        //ζ³¨εδΈθ½½θΏη¨δΈ­ηΉε»εΉΏζ­
        IntentFilter filter_clicked = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        registerReceiver(receiver_clicked, filter_clicked);
    }

    private void getCurrentProgress() {
        //θ½?θ?­θ·εθΏεΊ¦
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int downloadSoFar = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int totalSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    //ηΊΏη¨θΏεΊ¦
                    Message message = Message.obtain();
                    message.what = DOWNLOADFLAG;
                    message.arg1 = downloadSoFar;
                    message.arg2 = totalSize;
                    handler.sendMessage(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: ", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private long getDownloadId(String url, String apkName) {
        //ζε»ΊδΈθ½½θ―·ζ±
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //εε»Ίζδ»ΆδΏε­θ·―εΎ
        file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName);
        //θ?Ύη½?δΈθ½½ιε?η±»εοΌζ­€ε€εθ?ΈwifiδΈθ½½οΌι»θ?€ζ ιεΆ
        //request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        //θ?Ύη½?ιη₯ζ ζ ι’
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);//δΈθ½½θΏη¨δΈ­ζΎη€ΊοΌδΈθ½½ε?ζ―εζΎη€ΊοΌδΈθ½½ε?ζ―εηΉε»η΄ζ₯ε?θ£οΌ
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);//δΈθ½½θΏη¨δΈ­ζΎη€ΊοΌδΈθ½½ε?ζ―εζΆε€±
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);//δΈθ½½θΏη¨δΈ­ιθοΌδΈθ½½ε?ζ―εζΎη€Ί
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);//ε¨η¨δΈζΎη€ΊοΌιθ¦ε¨Menifastζδ»ΆδΈ­ε ε₯οΌDOWNLOAD_WITHOUT_NOTIFICATIONζι
        request.setTitle(apkName);
//        request.setDescription("ζ­£ε¨δΈθ½½");
        //θ?Ύη½?ζδ»Άε­ζΎη?ε½
        request.setDestinationUri(Uri.fromFile(file));
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,appName);
        //ι»θ?€DownLoadManagerδΈθ½½ηζδ»Άζ ζ³θ’«η³»η»εΊη¨ζ«ζε°οΌζ­€θ?Ύη½?δΈΊε―δ»₯ζ«ζε°
        request.setVisibleInDownloadsUi(true);
        //ε ε₯δΈθ½½ιε
        return downloadManager.enqueue(request);
    }

    BroadcastReceiver receiver_clicked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String extraID = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
            long[] references = intent.getLongArrayExtra(extraID);
            for (long reference : references) {
                if (reference == downloadId) {
                    Log.i(TAG, "onReceive: clicked: downLoadId_qq" + downloadId);
                }
            }
        }
    };

    BroadcastReceiver receiver_complete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (reference == downloadId) {
                Log.i(TAG, "onReceive: complete: downLoadId_qq" + downloadId);
                if (downLoadProgressListener != null) {
                    downLoadProgressListener.onProgress(100);
                }
                Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                if (uri != null) {
                    Log.i(TAG, "onReceive: complete: δΈθ½½ε?ζι©¬δΈε?θ£gogogo");
                    APKUtil.installApk(context, file);
                }
                closeDownLoad();
            }
        }
    };

    private void closeDownLoad() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void unregisterReceiver() {
        unregisterReceiver(receiver_clicked);
        unregisterReceiver(receiver_complete);
    }

    public void setDownLoadProgressListener(DownLoadProgressListener downLoadProgressListener) {
        this.downLoadProgressListener = downLoadProgressListener;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        closeDownLoad();
        unregisterReceiver();
        //ζ­€ζΉζ³η»ζ­’ε½εζε‘οΌεθ°onDestroyιζ―ζε‘γε¦εη­ε―ε¨δΈδΌεθ°onBindζΉζ³
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        //θ?Ύη½?εζΆδΈθ½½
        int i = downloadManager.remove(downloadId);//ε€η₯δΌζΈι€apk
        Log.i(TAG, "onDestroy: " + i);
    }
}
interface DownLoadProgressListener { void onProgress(int progress);}

/*
class DownloadedApkInfo {

    public DownloadedApkInfo(String packageName, String version, int versionCode, String appName) {
        this.packageName = packageName;
        this.version = version;
        this.appName = appName;
        this.versionCode = versionCode;
    }

    public String packageName;
    public String version;
    public String appName;
    public int versionCode;

    @Override
    public String toString() {
        return "DownloadedApkInfo{" +
                "packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", appName='" + appName + '\'' +
                ", versionCode=" + versionCode +
                '}';
    }
}*/
