package com.example.zhongzhiqiang.checkupdate;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhongzhiqiang.checkupdate.Utils.Download.DownloadService;
import com.example.zhongzhiqiang.checkupdate.Utils.MyDialog;
import com.example.zhongzhiqiang.checkupdate.Utils.UpdateBean;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private MyDialog myDialog;

    private int versionCode;
    private String versionName;

    private DownloadService.DownloadBinder downloadBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //获取服务的实例，便于调用服务当中的各种方法
            downloadBinder = (DownloadService.DownloadBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent); //启动服务 保证DownloadService一直在后台运行
        bindService(intent, connection, BIND_AUTO_CREATE); //绑定服务 让活动和服务相互通信
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }

    }

    private void initView() {

        TextView versionInfo = (TextView) findViewById(R.id.version_info);
        //获取PackageManager的实例 获取当前的版本名
        PackageManager packageManager = getPackageManager();
        try {
            //getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            versionName =  packInfo.versionName;
            versionCode = packInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //显示版本信息
        versionInfo.setText("当前版本： " + versionName);

        //点击发送网络请求
        Button checkButton = (Button) findViewById(R.id.check_button);
        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequestWithOkHttp(getString(R.string.update_info));
//                Toast.makeText(MainActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void sendRequestWithOkHttp(final String address){
        //OkHttp 开启线程来发送网络请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(address)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    parseJSONWithGSON(responseData);        //用GSON解析JSON
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //解析Json数据
    private void parseJSONWithGSON(String jsonData) {
        Gson gson = new Gson();
        UpdateBean updateBean = gson.fromJson(jsonData, UpdateBean.class);
        //验证Json解析是否正确
//        Log.d("MainActivity", "VersionCode is " + updateBean.getVersionCode());
//        Log.d("MainActivity", "VersionName is " + updateBean.getVersionName());
//        Log.d("MainActivity", "DownloadUrl is " + updateBean.getDownloadUrl());
        //判断线上代码版本是否大于本地代码版本
        Message message = new Message();
        if (updateBean.getVersionCode() > versionCode) {
            Log.d("MainActivity", "检查到更新");
            //利用Handler传递判断结果及数据
            message.what = 1;
            message.obj = updateBean.getDownloadUrl();
            handler.sendMessage(message);
        } else {
            handler.sendEmptyMessage(2);
        }
    }

    //通过Handler来处理异步消息
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message message) {
            switch (message.what){
                case 1:
                    String downloadUrl = (String) message.obj ;
                    ShowMyDialog(downloadUrl);
                    break;
                case 2:
                    Toast.makeText(MainActivity.this, "您当前已是最新版本了！", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private void ShowMyDialog(final String downloadUrl) {

        //初始化一个Dialog
        myDialog = new MyDialog(MainActivity.this);
        //点击取消直接关闭Dialog
        myDialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDialog.isShowing()){
                    myDialog.dismiss();
                }
            }
        });
        //点击确定开启下载任务
        myDialog.getConfirmedButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartDownload(downloadUrl);
                myDialog.dismiss();
            }
        });

        myDialog.show();

    }

    private void StartDownload(String downloadUrl) {
        downloadBinder.startDownload(downloadUrl);
   }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection); //活动销毁时，需要解绑服务，否则可能会造成内存泄露
    }
}
