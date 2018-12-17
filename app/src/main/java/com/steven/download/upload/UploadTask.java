package com.steven.download.upload;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 上传任务
 */
public class UploadTask {
    /**
     * 上传状态
     */
    private int mStatus = UploadStatus.STATUS_UPLOADING;
    /**
     * 文件上传的url
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
     * 总进度=每个线程的进度的和
     */
    private long mTotalLength;
    /**
     * 上传回调
     */
    private UploadCallback mCallback;
    /**
     * 记录是否暂停上传
     */
    private AtomicBoolean atomicIsStoped = new AtomicBoolean(false);
    /**
     * 设置tag，用于取消
     */
    private Object tag;

    UploadTask(String folder, String name, String url, Object tag, UploadCallback callBack) {
        this.folder = folder;
        this.name = name;
        this.url = url;
        this.tag = tag;
        this.mCallback = callBack;
    }

    void init() {
        //当前任务状态是否停止
        if (mStatus == UploadStatus.STATUS_STOP) {
            mCallback.onPause(new File(folder, name));
            UploadDispatcher.getInstance().recyclerTask(UploadTask.this);
            return;
        }
        initUploadRunnable();
    }

    /**
     * 初始化上传线程
     */
    private void initUploadRunnable() {
        UploadRunnable uploadRunnable = new UploadRunnable(folder, name, url, UUID.randomUUID().toString(), new UploadCallback() {

            @Override
            public void onStart(String fileName, int status) {

            }

            @Override
            public void onFailure(Exception e) {
                //有一个线程发生异常，上传失败，需要把其它线程停止掉
                if (!atomicIsStoped.get()) {
                    atomicIsStoped.set(true);
                    mCallback.onFailure(e);
                    stopUpload();
                    //上传失败回收任务，继续上传后面等待的任务
                    UploadDispatcher.getInstance().recyclerTask(UploadTask.this);
                }
            }

            @Override
            public void onSuccess(File file) {
                mCallback.onSuccess(file);
                //上传成功回收任务，继续上传后面等待的任务
                UploadDispatcher.getInstance().recyclerTask(UploadTask.this);
            }

            @Override
            public void onProgress(long currentLength, long totalLength) {
                //叠加下progress，实时去更新进度条
                //这里需要synchronized下
                synchronized (UploadTask.this) {
                    mCallback.onProgress(mTotalLength += currentLength, totalLength);
                }
            }

            @Override
            public void onPause(File file) {
                if (!atomicIsStoped.get()) {
                    atomicIsStoped.set(true);
                    mCallback.onPause(file);
                }
            }
        });
        //通过线程池去执行
        UploadDispatcher.getInstance().executorService().execute(uploadRunnable);
    }

    /**
     * 停止上传 上传线程只有一个，停止任务即停止上传
     */
    public void stopUpload() {
        mStatus = UploadStatus.STATUS_STOP;
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
     * 上传状态
     */
    public interface UploadStatus {
        /**
         * 正在上传
         */
        int STATUS_UPLOADING = 1;
        /**
         * 停止上传
         */
        int STATUS_STOP = 2;
        /**
         * 等待上传
         */
        int STATUS_WAITING = 3;
    }
}
