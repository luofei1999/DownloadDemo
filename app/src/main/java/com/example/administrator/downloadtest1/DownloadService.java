package com.example.administrator.downloadtest1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {

    //LOG TAG
    private static final String TAG = "DownloadService";

    //下载处理的对象
    private DownloadTask downloadTask;

    //获取 DownloadListener 对象
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            //弹出下载成功的 通知
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            //如果不重置为 null，只能执行一次下载，无法开始的二次下载
            downloadTask = null;

            //停止前台服务
            stopForeground(true);

            //下载成功的通知
            getNotificationManager().notify(1, getNotification("Download Success...", 0));
            Toast.makeText(DownloadService.this, "Download Success...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask = null;

            getNotificationManager().notify(1, getNotification("Download Failed...", -1));
            Toast.makeText(DownloadService.this, "Download Failed...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPause() {
            //只有将 downloadTask 置为空，暂停后，才能在开始
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Download Paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStop() {
            downloadTask = null;

            //停止前台服务
            stopForeground(true);

            Toast.makeText(DownloadService.this, "Download Stop...", Toast.LENGTH_SHORT).show();
        }
    };

    //获取 NotificationManager
    private NotificationManager getNotificationManager (){
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE );
    }

    //构建通知
    private Notification getNotification (String title, int progress){

        //点击进入主页面
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentText(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource( getResources(), R.mipmap.ic_launcher ))
                .setContentIntent(pi);

        //进度条大于 0 时，才显示
        if (progress >= 0) {
            builder.setProgress( 100, progress, false );
            builder.setContentText( progress + "%" );
        }

        //返回构建后的通知
        return builder.build();
    }

    // 服务 - 活动 通信
    class DownloadBinder extends Binder {

        private String downloadURL = null;

        //开始下载
        public void startDownload (String downloadURL){

            //获取URL,用于判断是否要删除文件
            this.downloadURL = downloadURL;

            if (downloadTask == null) {

                //执行下载任务
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadURL);

                //开启前台服务，后台下载
                startForeground(1, getNotification("Download...", 0));

                Toast.makeText(DownloadService.this, "Download start...", Toast.LENGTH_SHORT).show();

            }

        }

        //暂停下载
        public void pauseDownload (){

            if (downloadTask != null)
                downloadTask.pause();

        }

        //取消下载
        public void stopDownload (){
            if (downloadTask != null)
                downloadTask.stop();

            //对已下载文件进行清除
            if (downloadURL != null) {
                String fileName = downloadURL.substring( downloadURL.lastIndexOf("/") );
                String directory = Environment.getExternalStoragePublicDirectory
                        (Environment.DIRECTORY_DOWNLOADS).getPath();

                //构建文件
                File file = new File( directory + fileName );

                //判断文件是否已经存在，如果存在直接删除
                if (file.exists()) {
                    file.delete();
                }

            }

            //停止前台服务
            stopForeground(true);

            //关闭通知
            getNotificationManager().cancel(1);

            Toast.makeText(DownloadService.this, "Download stop...", Toast.LENGTH_SHORT).show();
        }

    }

    //创建通信的实例
    private DownloadBinder binder = new DownloadBinder();

    //活动绑定服务时，返回 binder 实例
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //服务销毁时回调该方法
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: run");
    }
}
