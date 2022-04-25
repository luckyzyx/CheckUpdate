package com.luckyzyx.DownloadManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity implements DownLoadProgressListener {

    private static final String TAG = "MainActivity";
    public static final String apkName = "QQ.apk";
    private ProgressBar progressBar;
    private TextView textView;
    private File file;
    private DownLoadService downLoadService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //进度条
        progressBar = findViewById(R.id.progressBar);
        //百分比
        textView = findViewById(R.id.text);
        //文件路径
        file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName);
    }

    //查询状态
    public void queryStatus(View view) {
        //如果服务为null
        if (downLoadService == null) return;

        String msgStatus = downLoadService.queryStatus();
        Toast.makeText(this, msgStatus, Toast.LENGTH_SHORT).show();

    }

    //开始下载
    public void startDownLoad(View view) {
        //检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            startService();
        }
    }

    public void cancleDownLoad(View view) {
        progressBar.setProgress(0);
        textView.setText("0%");
        if (downLoadService != null) {
            unbindService(connection);
            downLoadService = null;
        }
    }

    public void clearApk(View view) {
        if (file.exists()) {
            boolean delete = file.delete();
            if (delete) {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void loadInstalled(View view) {

        if (file.exists()) {
            Log.i(TAG, "loadInstalled: 之前已经下载过最新版本，直接安装gogo");
            APKUtil.installApk(this, file);
        } else {
            Toast.makeText(this, "本地无最新文件请下载", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startService();
            } else {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startService() {
        String apkUrl = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";

        Intent serviceIntent = new Intent(this, DownLoadService.class);
        serviceIntent.putExtra("apkUrl", apkUrl);
        serviceIntent.putExtra("apkName", apkName);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Service.BIND_AUTO_CREATE);
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownLoadService.DownLoadBinder downLoadBinder = (DownLoadService.DownLoadBinder) service;
            downLoadService = downLoadBinder.getService();
            downLoadService.setDownLoadProgressListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgress(int progress) {
        textView.setText(progress + "%");
        progressBar.setProgress(progress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downLoadService != null) {
            unbindService(connection);
        }
    }
}
