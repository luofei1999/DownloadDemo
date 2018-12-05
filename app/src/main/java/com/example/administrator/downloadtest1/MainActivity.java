package com.example.administrator.downloadtest1;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button start;
    private Button pause;
    private Button stop;
    private EditText downloadURL;

    //通信类的引用
    private DownloadService.DownloadBinder binder;

    //获取 通信 类的实例
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (DownloadService.DownloadBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = findViewById(R.id.start);
        pause = findViewById(R.id.pause);
        downloadURL = findViewById(R.id.downloadURL);
        stop = findViewById(R.id.stop);
        start.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);

        //运行时权限处理
        List<String> permissionList = new ArrayList<>();
        
        //访问手机网络
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED)
            permissionList.add( Manifest.permission.INTERNET );
        
        //写入 SD 卡权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            permissionList.add( Manifest.permission.WRITE_EXTERNAL_STORAGE );
        
        //对权限授权
        if ( !permissionList.isEmpty() ) {
            //集合转数组
            String[] permiss = permissionList.toArray(new String[permissionList.size()]);
            
            ActivityCompat.requestPermissions(this, permiss, 1);
        }

        //开启服务
        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);

        //绑定服务
        bindService(intent, connection, BIND_AUTO_CREATE);
        
    }

    //授权回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1 :
                if (grantResults.length >= 0 ) {

                    for (int result :
                            grantResults) {
                        
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "permissions is not granted", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        
                    }


                }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start :
                //从 EditText 中获取下载链接
                String url = downloadURL.getText().toString();
                binder.startDownload(url);
                break;
            case R.id.pause:
                binder.pauseDownload();
                break;
            case R.id.stop :
                binder.stopDownload();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //因为 Android 服务的限制，在调用过 startService 和 bindService 下
        //想释放资源就必须同时调用 unbindService() 和 stopService()

        //解绑服务
        unbindService(connection);

        //停止服务
        Intent intent = new Intent(this, DownloadService.class);
        stopService(intent);
    }
}
