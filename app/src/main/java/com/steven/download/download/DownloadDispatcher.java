package com.steven.download.download;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.steven.download.okhttp.OkHttpManager;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 下载调度器
 */
public class DownloadDispatcher {
    private static volatile DownloadDispatcher sDownloadDispatcher;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int THREAD_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = THREAD_SIZE;
    /**
     * 同时下载的最大任务数
     */
    private int maxTaskSize = 3;
    /**
     * 线程池
     */
    private ExecutorService mExecutorService;
    /**
     * 准备下载的任务
     */
    private final Deque<DownloadTask> readyTasks = new ArrayDeque<>();
    /**
     * 正在下载的任务
     */
    private final Deque<DownloadTask> runningTasks = new ArrayDeque<>();

    private DownloadDispatcher() {
    }

    /**
     * 获取下载调度实例
     *
     * @return DownloadDispatcher
     */
    public static DownloadDispatcher getInstance() {
        if (sDownloadDispatcher == null) {
            synchronized (DownloadDispatcher.class) {
                if (sDownloadDispatcher == null) {
                    sDownloadDispatcher = new DownloadDispatcher();
                }
            }
        }
        return sDownloadDispatcher;
    }

    /**
     * 设置最大同时下载任务数 默认为3
     *
     * @param maxTaskSize 最大任务数
     * @return
     */
    public DownloadDispatcher setMaxTaskSize(int maxTaskSize) {
        this.maxTaskSize = maxTaskSize < 1 ? 1 : (maxTaskSize > 5 ? 5 : maxTaskSize);
        return this;
    }

    /**
     * 创建线程池
     *
     * @return mExecutorService
     */
    public synchronized ExecutorService executorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(CORE_POOL_SIZE, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(false);
                    return thread;
                }
            });
        }
        return mExecutorService;
    }

    /**
     * @param name     文件名
     * @param url      下载的地址
     * @param callBack 回调接口
     */
    public void startDownload(final String name, final String url, final DownloadCallback callBack) {
        startDownload(Environment.getExternalStorageDirectory().getAbsolutePath(), name, url, null, callBack);
    }

    /**
     * @param name     文件名
     * @param url      下载的地址
     * @param tag      tag
     * @param callBack 回调接口
     */
    public void startDownload(final String name, final String url, final Object tag, final DownloadCallback callBack) {
        startDownload(Environment.getExternalStorageDirectory().getAbsolutePath(), name, url, tag, callBack);
    }

    /**
     * @param folder   文件夹
     * @param name     文件名
     * @param url      下载的地址
     * @param callBack 回调接口
     */
    public void startDownload(final String folder, final String name, final String url, final DownloadCallback callBack) {
        startDownload(folder, name, url, null, callBack);
    }

    /**
     * @param folder   文件夹
     * @param name     文件名
     * @param url      下载的地址
     * @param callBack 回调接口
     */
    public void startDownload(final String folder, final String name, final String url, final Object tag, final DownloadCallback callBack) {
        Call call = OkHttpManager.getInstance().asyncCall(url);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callBack.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                //获取文件的大小
                ResponseBody responseBody = response.body();
                long contentLength = responseBody == null ? -1 : responseBody.contentLength();
                if (contentLength <= -1) {
                    return;
                }
                DownloadTask downloadTask = new DownloadTask(folder, name, url, THREAD_SIZE, contentLength, tag, callBack);
                // 将任务加入下载队列
                if (runningTasks.size() < maxTaskSize) {
                    runningTasks.addLast(downloadTask);
                    callBack.onStart(name, DownloadTask.DownloadStatus.STATUS_DOWNLOADING);
                    downloadTask.init();
                } else {
                    callBack.onStart(name, DownloadTask.DownloadStatus.STATUS_WAITING);
                    readyTasks.addLast(downloadTask);
                }
            }
        });
    }

    /**
     * @param downLoadTask 下载任务
     */
    void recyclerTask(DownloadTask downLoadTask) {
        runningTasks.remove(downLoadTask);
        //从readyTask中取出一个任务加入到runningTask队列中，并开始任务
        DownloadTask task = readyTasks.peekFirst();
        if (task != null) {
            readyTasks.remove(task);
            runningTasks.addLast(task);
            task.init();
        }
    }

    /**
     * 根据url 去暂停那个
     *
     * @param url
     */
    public void stopDownload(String url) {
        //这个停止是不是这个正在下载的
        stopTasks(runningTasks, url);
        stopTasks(readyTasks, url);
    }

    /**
     * 暂停所有的下载
     */
    public void stopAll() {
        stopTasks(runningTasks, null);
        stopTasks(readyTasks, null);
    }

    /**
     * 暂停下载
     *
     * @param tasks
     * @param url
     */
    private void stopTasks(Collection<DownloadTask> tasks, String url) {
        for (DownloadTask task : tasks) {
            if (TextUtils.isEmpty(url)) {
                task.stopDownload();
            } else {
                if (task.getUrl().equals(url)) {
                    task.stopDownload();
                }
            }
        }
    }

    /**
     * 取消某个tag的下载，包括正在下载和准备下载
     *
     * @param tag
     */
    public void cancel(Object tag) {
        cancelTasks(runningTasks, tag);
        cancelTasks(readyTasks, tag);
    }

    /**
     * 取消所有的下载，包括正在下载和准备下载
     */
    public void cancelAll() {
        cancelTasks(runningTasks, null);
        cancelTasks(readyTasks, null);
    }

    /**
     * 删除任务
     *
     * @param collections
     */
    private void cancelTasks(Collection<DownloadTask> collections, Object tag) {
        Iterator<DownloadTask> it = collections.iterator();
        while (it.hasNext()) {
            DownloadTask task = it.next();
            if (tag != null) {
                if (task.getTag() != null && task.getTag().equals(tag)) {
                    task.stopDownload();
                    it.remove();
                }
            } else {
                task.stopDownload();
                it.remove();
            }
        }
    }
}
