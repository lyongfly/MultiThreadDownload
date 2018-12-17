package com.steven.download.download;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下载任务
 */
public class DownloadTask {
    /**
     * 下载状态
     */
    private int mStatus = DownloadStatus.STATUS_DOWNLOADING;
    /**
     * 文件下载的url
     */
    private String url;
    /**
     * 文件的名称
     */
    private String name;
    /**
     * 文件保存的文件夹
     */
    private String folder;
    /**
     * 文件的大小
     */
    private long mContentLength;
    /**
     * 下载文件的线程的个数
     */
    private int mThreadSize;
    /**
     * 线程下载成功的个数,变量加个volatile，多线程保证变量可见性以及原子性
     */
    private AtomicInteger mSuccessNumber;
    /**
     * 总进度=每个线程的进度的和
     */
    private long mTotalLength;
    /**
     * 下载的线程集合
     */
    private List<DownloadRunnable> mDownloadRunnables;
    /**
     * 下载回调
     */
    private DownloadCallback mCallback;
    /**
     * 记录是否暂停下载
     */
    private AtomicBoolean atomicIsStoped = new AtomicBoolean(false);
    /**
     * 设置tag，用于取消
     */
    private Object tag;

    DownloadTask(String folder, String name, String url, int threadSize, long contentLength, Object tag, DownloadCallback callBack) {
        this.folder = folder;
        this.name = name;
        this.url = url;
        this.mThreadSize = threadSize;
        this.mContentLength = contentLength;
        this.mDownloadRunnables = new ArrayList<>();
        this.tag = tag;
        this.mCallback = callBack;
        this.mSuccessNumber = new AtomicInteger(0);
    }

    void init() {
        //任务状态是否停止
        if (mStatus == DownloadStatus.STATUS_STOP) {
            mCallback.onPause(new File(folder, name));
            DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
            return;
        }
        //检查是否有记录的断点信息，如果有则读取断点信息继续下载
        List<BreakPoint> points = getBreakPoints(folder, name);
        if (!points.isEmpty()) {
            mThreadSize = points.size();
            long leaveLength = 0;
            for (int i = 0; i < mThreadSize; i++) {
                BreakPoint point = points.get(i);
                leaveLength += (point.end - point.start);
                mTotalLength = mContentLength - leaveLength;
                initDownloadRunnable(point.threadId, point.start, point.end);
            }
            return;
        }
        for (int i = 0; i < mThreadSize; i++) {
            //初始化的时候，需要读取数据库
            //每个线程的下载的大小threadSize
            long threadSize = mContentLength / mThreadSize;
            //开始下载的位置
            long start = i * threadSize;
            //结束下载的位置
            long end = start + threadSize - 1;
            if (i == mThreadSize - 1) {
                end = mContentLength - 1;
            }
            initDownloadRunnable(i, start, end);
        }
    }

    /**
     * 初始化下载线程
     */
    private void initDownloadRunnable(int theadId, long start, long end) {
        DownloadRunnable downloadRunnable = new DownloadRunnable(folder, name, url, mContentLength, theadId, start, end, new DownloadCallback() {

            @Override
            public void onStart(String fileName, int status) {

            }

            @Override
            public void onFailure(Exception e) {
                //有一个线程发生异常，下载失败，需要把其它线程停止掉
                if (!atomicIsStoped.get()) {
                    atomicIsStoped.set(true);
                    mCallback.onFailure(e);
                    stopDownload();
                    //下载失败回收任务，继续下载后面等待的任务
                    DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
                }
            }

            @Override
            public void onSuccess(File file) {
                mSuccessNumber.addAndGet(1);
                if (mSuccessNumber.intValue() == mThreadSize) {
                    mCallback.onSuccess(file);
                    //下载成功回收任务，继续下载后面等待的任务
                    DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
                }
            }

            @Override
            public void onProgress(long currentLength, long totalLength) {
                //叠加下progress，实时去更新进度条
                //这里需要synchronized下
                synchronized (DownloadTask.this) {
                    mCallback.onProgress(mTotalLength += currentLength, totalLength);
                }
            }

            @Override
            public void onPause(File file) {
                if (!atomicIsStoped.get()) {
                    atomicIsStoped.set(true);
                    mCallback.onPause(file);
                    //暂停任务回收任务
                    DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
                }
            }
        });
        //通过线程池去执行
        DownloadDispatcher.getInstance().executorService().execute(downloadRunnable);
        mDownloadRunnables.add(downloadRunnable);
    }

    /**
     * 获取断点记录
     */
    private List<BreakPoint> getBreakPoints(String folder, final String fileName) {
        List<BreakPoint> points = new ArrayList<>();
        File[] files = new File(folder).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("." + fileName + ".");
            }
        });
        for (File file : files) {
            String content = getFileContent(file);
            String name = file.getName();
            String threadId = name.substring(name.lastIndexOf(".") + 1);
            if (!TextUtils.isEmpty(content)) {
                String[] arrays = content.split("-");
                if (arrays.length == 2) {
                    points.add(new BreakPoint(Long.parseLong(arrays[0]), Long.parseLong(arrays[1]), Integer.parseInt(threadId)));
                }
            }
        }
        Collections.sort(points, new Comparator<BreakPoint>() {
            @Override
            public int compare(BreakPoint o1, BreakPoint o2) {
                return o1.start.compareTo(o2.start);
            }
        });
        return points;
    }

    private static class BreakPoint {
        Long start;
        Long end;
        int threadId;

        BreakPoint(long start, long end, int threadId) {
            this.start = start;
            this.end = end;
            this.threadId = threadId;
        }
    }

    /**
     * 读取断点内容
     */
    private String getFileContent(@NonNull File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[fileInputStream.available()];
            int length = fileInputStream.read(bytes);
            if (length > 0) {
                return new String(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * 停止下载
     */
    public void stopDownload() {
        mStatus = DownloadStatus.STATUS_STOP;
        for (DownloadRunnable runnable : mDownloadRunnables) {
            runnable.stop();
        }
    }

    /**
     * 获取url
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取tag
     */
    public Object getTag() {
        return this.tag;
    }

    /**
     * 下载状态
     */
    public interface DownloadStatus {
        /**
         * 正在下载
         */
        int STATUS_DOWNLOADING = 1;
        /**
         * 停止下载
         */
        int STATUS_STOP = 2;
        /**
         * 等待下载
         */
        int STATUS_WAITING = 3;
    }
}
