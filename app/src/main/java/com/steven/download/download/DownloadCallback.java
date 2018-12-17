package com.steven.download.download;

import java.io.File;

/**
 * 下载回调
 */
public interface DownloadCallback {

    /**
     * 开始下载
     *
     * @param fileName 文件名称
     * @param status   状态
     */
    void onStart(String fileName, int status);

    /**
     * 下载进度 计算进度 float progress = (float) currentLength / totalLength)
     *
     * @param currentLength 下载长度
     * @param totalLength   文件长度
     */
    void onProgress(long currentLength, long totalLength);

    /**
     * 下载成功
     *
     * @param file 下载成功文件
     */
    void onSuccess(File file);

    /**
     * 下载失败
     *
     * @param e 异常
     */
    void onFailure(Exception e);

    /**
     * 暂停
     *
     * @param file 文件
     */
    void onPause(File file);
}
