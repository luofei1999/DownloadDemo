package com.example.administrator.downloadtest1;

public interface DownloadListener {

    //下载进度条
    void onProgress (int progress);
    //下载成功
    void onSuccess ();
    //下载失败
    void onFailed ();
    //下载暂停
    void onPause ();
    //下载取消
    void onStop ();

}
