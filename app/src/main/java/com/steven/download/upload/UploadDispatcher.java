package com.steven.download.upload;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 上传调度器
 */
public class UploadDispatcher {
    private static volatile UploadDispatcher sUploadDispatcher;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    /**
     * 同时上传的最大任务数
     */
    private int maxTaskSize = 3;
    /**
     * 线程池
     */
    private ExecutorService mExecutorService;
    /**
     * 准备上传的任务
     */
    private final Deque<UploadTask> readyTasks = new ArrayDeque<>();
    /**
     * 正在上传的任务
     */
    private final Deque<UploadTask> runningTasks = new ArrayDeque<>();

    /**
     * 获取上传调度实例
     *
     * @return UploadDispatcher
     */
    public static UploadDispatcher getInstance() {
        if (sUploadDispatcher == null) {
            synchronized (UploadDispatcher.class) {
                if (sUploadDispatcher == null) {
                    sUploadDispatcher = new UploadDispatcher();
                }
            }
        }
        return sUploadDispatcher;
    }

    /**
     * 设置最大同时上传任务数 默认为3
     *
     * @param maxTaskSize 最大任务数
     * @return
     */
    public UploadDispatcher setMaxTaskSize(int maxTaskSize) {
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
     * @param folder   文件夹
     * @param name     文件名
     * @param url      上传的地址
     * @param callBack 回调接口
     */
    public void startUpload(final String folder, final String name, final String url, final UploadCallback callBack) {
        startUpload(folder, name, url, null, callBack);
    }

    /**
     * @param folder   文件夹
     * @param name     文件名
     * @param url      上传的地址
     * @param callBack 回调接口
     */
    public void startUpload(final String folder, final String name, final String url, final Object tag, final UploadCallback callBack) {
        UploadTask uploadTask = new UploadTask(folder, name, url, tag, callBack);
        // 将任务加入上传队列
        if (runningTasks.size() < maxTaskSize) {
            runningTasks.addLast(uploadTask);
            callBack.onStart(name, UploadTask.UploadStatus.STATUS_UPLOADING);
            uploadTask.init();
        } else {
            callBack.onStart(name, UploadTask.UploadStatus.STATUS_WAITING);
            readyTasks.addLast(uploadTask);
        }
    }

    /**
     * @param uploadTask 上传任务
     */
    void recyclerTask(UploadTask uploadTask) {
        runningTasks.remove(uploadTask);
        //从readyTask中取出一个任务加入到runningTask队列中，并开始任务
        UploadTask task = readyTasks.peekFirst();
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
    public void stopUpload(String url) {
        stopTasks(readyTasks, url);
    }

    /**
     * 暂停所有的上传
     */
    public void stopAll() {
        stopTasks(readyTasks, null);
    }

    /**
     * 暂停上传
     *
     * @param tasks
     * @param url
     */
    private void stopTasks(Collection<UploadTask> tasks, String url) {
        for (UploadTask task : tasks) {
            if (TextUtils.isEmpty(url)) {
                task.stopUpload();
            } else {
                if (task.getUrl().equals(url)) {
                    task.stopUpload();
                }
            }
        }
    }

    /**
     * 取消某个tag的上传，包括正在上传和准备上传
     *
     * @param tag
     */
    public void cancel(Object tag) {
        cancelTasks(runningTasks, tag);
        cancelTasks(readyTasks, tag);
    }

    /**
     * 取消所有的上传，包括正在上传和准备上传
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
    private void cancelTasks(Collection<UploadTask> collections, Object tag) {
        Iterator<UploadTask> it = collections.iterator();
        while (it.hasNext()) {
            UploadTask task = it.next();
            if (tag != null) {
                if (task.getTag() != null && task.getTag().equals(tag)) {
                    task.stopUpload();
                    it.remove();
                }
            } else {
                task.stopUpload();
                it.remove();
            }
        }
    }
}
