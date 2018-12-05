package com.example.administrator.downloadtest1;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask <String, Integer, Integer> {

    //下载状态的返回结果
    private static final int TYPE_SUCCESS = 0;         //成功
    private static final int TYPE_FAILED = 1;         //失败
    private static final int TYPE_PAUSE = 2;         //暂停
    private static final int TYPE_STOP = 3;         //取消
    private static final int BUFFED_SIZE = 1024;   //缓冲区大小


    //回调接口
    private DownloadListener listener;

    //获取回调接口的实例
    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    //下载进度条 的进度
    int lastProgress = 0;

    //下载文件的长度
    long downloadedLength = 0;

    //下载文件的总长度
    long contentLength = 0;

    //是否是暂停下载
    boolean isPause = false;

    //是否是取消下载
    boolean isStop = false;

    //下载链接
    String downloadURL = null;

    //无论下载处于什么状态，都会回调 DownloadListener 接口
    @Override
    protected void onPostExecute(Integer integer) {

        switch (integer) {
            case TYPE_SUCCESS :
                listener.onSuccess();
                break;
            case TYPE_FAILED :
                listener.onFailed();
                break;
            case TYPE_PAUSE :
                listener.onPause();
                break;
            case TYPE_STOP :
                listener.onStop();
                break;
        }

    }

    //更新进度条
    @Override
    protected void onProgressUpdate(Integer... values) {

        //获取当前下载进度
        int progress = values[0];

        //如果 当前下载进度 大于 已下载长度，才会更改 进度条进度
        if (progress > lastProgress) {
            //回调接口来更新进度
            listener.onProgress(progress);

            //更新进度条最后的位置大小
            lastProgress = progress;
        }

    }

    @Override
    protected Integer doInBackground(String... strings) {

        //从 OkHttp 获取的 字节流
        InputStream is = null;

        //断点续传，需要使用 RandomAccessFile
        RandomAccessFile saveFile = null;

        //下载的文件
        File file = null;

        try{
            downloadURL = strings[0];

            //获取文件名称
            String fileName = downloadURL.substring( downloadURL.lastIndexOf( "/" ) );

            //获取文件的下载路径
            String directory = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath();

            //创建文件
            file = new File( directory + fileName );

            //判断文件是否已存在，存在直接取出文件的长度
            if (file.exists())
                downloadedLength = file.length();

            //获取文件的总长度
            contentLength = getContentLength (downloadURL);

            //判断从服务器中获取的长度
            if (contentLength == 0)
                return TYPE_FAILED;     //服务器中获取的长度为0，直接返回失败状态
            else if (contentLength == downloadedLength)
                return TYPE_SUCCESS;    //和下载的长度一致，直接返回下载成功

            //开启下载任务
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    //addHeader 从服务器中某个位置开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadURL)
                    .build();

            Response response = client.newCall(request).execute();

            if (response != null) {

                //获取服务器中的字节流
                is = response.body().byteStream();

                //创建 RandomAccessFile
                saveFile = new RandomAccessFile(file, "rw");

                //创建缓冲区
                byte[] b = new byte[BUFFED_SIZE];
                //合计下载数据的大小
                int total = 0;
                //保存每次读到的字节
                int len = 0;

                //循环写入
                while ( (len = is.read(b)) != -1 ) {

                    //判断用户是否暂停
                    if (isPause)
                        return TYPE_PAUSE;
                    else if (isStop)
                        //判断用户是否取消
                        return TYPE_STOP;
                    else {

                        //合计下载数据
                        total += len;

                        //写入文件
                        saveFile.write(b, 0, len);

                        //计算进度条进度
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);

                        //更新进度条
                        publishProgress(progress);
                    }
                }
            }

            //写入时，如果没有问题，就代表下载成功
            return TYPE_SUCCESS;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                //关闭字节流
                if (is != null)
                    is.close();

                //关闭随机访问文件流
                if (saveFile != null)
                    saveFile.close();

                //如果下载发生异常，应该将已下载的文件删除
                if (isStop && file != null)
                    file.delete();

            }catch (IOException io) {
                io.printStackTrace();
            }

        }

        //如果程序执行到这里还没返回，说明下载失败
        return TYPE_FAILED;
    }

    //获取内容的总长度
    private long getContentLength(String downloadURL) {

        //开启 OkHttp 来获取内容的长度
        OkHttpClient client = new OkHttpClient();

        //构建请求
        Request request = new Request.Builder()
                .url(downloadURL)
                .build();

        //获取服务器响应
        Response response = null;

        try {
            response  = client.newCall(request).execute();

            //获取服务器返回的数据长度
            long contentLength = response.body().contentLength();

            //将数据长度返回
            return contentLength;

        } catch (IOException e) {
            e.printStackTrace();
        }

        //如果获取服务器的数据时出现问题，返回 0
        return 0;
    }

    //下载暂停
    public void pause (){
        isPause = true;
    }

    //下载取消
    public void stop (){
        isStop = true;
    }
}
