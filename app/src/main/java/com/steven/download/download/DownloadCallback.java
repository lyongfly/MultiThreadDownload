package com.steven.download.download;

import java.io.File;

/**
 * Description:
 * Data：4/19/2018-1:46 PM
 *
 * @author: yanzhiwen
 */
public interface DownloadCallback {

    /**
     * 开始下载
     * @param fileName  文件名称
     */
    void onStart(String fileName);

    /**
     * 下载进度
     *
     * @param progress
     */
    void onProgress(long progress, long currentLength);

    /**
     * 下载成功
     *
     * @param file
     */
    void onSuccess(File file);

    /**
     * 下载失败
     *
     * @param e
     */
    void onFailure(Exception e);

    /**
     * 暂停
     */
    void onPause(File file);
}
