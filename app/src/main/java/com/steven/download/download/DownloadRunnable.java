package com.steven.download.download;

import android.support.annotation.NonNull;
import android.util.Log;

import com.steven.download.okhttp.OkHttpManager;
import com.steven.download.utils.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import okhttp3.Response;

/**
 * Description:
 * Data：4/19/2018-1:45 PM
 *
 * @author: yanzhiwen
 */
public class DownloadRunnable implements Runnable {
    private static final String TAG = "DownloadRunnable";
    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_STOP = 2;
    /**
     * 线程的状态
     */
    private int mStatus = STATUS_DOWNLOADING;
    /**
     * 文件下载的url
     */
    private String url;
    /**
     * 文件的名称
     */
    private String name;
    /**
     * 文件存储路径
     */
    private String folder;
    /**
     * 线程id
     */
    private int threadId;
    /**
     * 每个线程下载开始的位置
     */
    private long start;
    /**
     * 每个线程下载结束的位置
     */
    private long end;
    /**
     * 每个线程的下载进度
     */
    private long mProgress;
    /**
     * 文件的总大小 content-length
     */
    private long mCurrentLength;
    /**
     * 下载回调
     */
    private DownloadCallback downloadCallback;
    /**
     * 是否下载完成
     */
    private boolean mCompleted = false;
    /**
     * 断点存储文件,文件名称为 .name.apk.1 信息格式为 start + "-" + end
     */
    private File breakPointFile;

    public DownloadRunnable(String folder, String name, String url, long currentLength, int threadId, long start, long end, DownloadCallback downloadCallback) {
        this.folder = folder;
        this.name = name;
        this.url = url;
        this.mCurrentLength = currentLength;
        this.threadId = threadId;
        this.start = start;
        this.end = end;
        this.downloadCallback = downloadCallback;
        this.breakPointFile = new File(folder, "." + name + "." + threadId);
    }

    @Override
    public void run() {
        mStatus = STATUS_DOWNLOADING;
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        try {
            Response response = OkHttpManager.getInstance().syncResponse(url, start, end);
            Log.i(TAG, "fileName=" + name + " 每个线程负责下载文件大小contentLength=" + response.body().contentLength()
                    + " 开始位置start=" + start + "结束位置end=" + end + " threadId=" + threadId);
            inputStream = response.body().byteStream();
            //保存文件的路径
            File file = new File(folder, name);
            randomAccessFile = new RandomAccessFile(file, "rwd");
            //seek从哪里开始
            randomAccessFile.seek(start);
            int length;
            byte[] bytes = new byte[10 * 1024];
            boolean isSuccess = true;
            while ((length = inputStream.read(bytes)) != -1) {
                if (mStatus == STATUS_STOP) {
                    isSuccess = false;
                    downloadCallback.onPause(file);
                    break;
                }
                //写入
                randomAccessFile.write(bytes, 0, length);
                //保存下进度，做断点
                start += length;
                mProgress = mProgress + length;
                //实时去更新下进度条，将每次写入的length传出去
                downloadCallback.onProgress(length, mCurrentLength);
            }
            if (mCompleted = isSuccess) {
                deleteBreakPointFile();
                downloadCallback.onSuccess(file);
            }
        } catch (IOException e) {
            downloadCallback.onFailure(e);
        } finally {
            //保存到文件记录断点
            recordProgress(start, end);
            close(inputStream);
            close(randomAccessFile);
        }
    }

    /**
     * 停止下载
     */
    public void stop() {
        mStatus = STATUS_STOP;
    }


    /**
     * 是否下载完成
     *
     * @return true false
     */
    public boolean isCompleted() {
        return mCompleted;
    }

    /**
     * 记录当前下载的断点
     *
     * @param start
     * @param end
     */
    private void recordProgress(long start, long end) {
        if (start < end) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(breakPointFile);
                String breakPointContent = start + "-" + end;
                fileOutputStream.write(breakPointContent.getBytes("UTF-8"));
                fileOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                close(fileOutputStream);
            }
        }
    }

    /**
     * 删除断点记录文件
     */
    private void deleteBreakPointFile() {
        if (breakPointFile != null && breakPointFile.exists()) {
            breakPointFile.delete();
        }
    }


    /**
     * 关闭流
     *
     * @param closeable
     */
    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
