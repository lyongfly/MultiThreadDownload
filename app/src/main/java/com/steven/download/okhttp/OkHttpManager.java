package com.steven.download.okhttp;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttpManager 可以自定义 OkhttpClient,如：设置https
 */
public class OkHttpManager {

    private static OkHttpManager okHttpManager;
    private static OkHttpClient mOkHttpClient;

    /**
     * 设置okHttp
     *
     * @param okHttpClient 自定义okHttp
     */
    public static void setOkhttpClient(OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
    }

    /**
     * 获取 OkHttpClient
     *
     * @return OkHttpClient
     */
    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    private OkHttpManager() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }
    }

    public static OkHttpManager getInstance() {
        if (okHttpManager == null) {
            synchronized (OkHttpManager.class) {
                if (okHttpManager == null) {
                    okHttpManager = new OkHttpManager();
                }
            }
        }
        return okHttpManager;
    }

    /**
     * call
     *
     * @param url url
     * @return 请求
     */
    public Call asyncCall(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return mOkHttpClient.newCall(request);
    }

    /**
     * 请求
     *
     * @param url   url
     * @param start 开始位置
     * @param end   接收位置
     * @return 响应消息体
     * @throws IOException io异常
     */
    public Response syncResponse(String url, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                //Range 请求头格式Range: bytes=start-end
                .addHeader("Range", "bytes=" + start + "-" + end)
                .build();
        return mOkHttpClient.newCall(request).execute();
    }

    /**
     * 上传
     *
     * @param url         url
     * @param requestBody 上传body
     * @return 响应消息体
     * @throws IOException io异常
     */
    public Response syncPostResponse(String url, RequestBody requestBody) throws IOException {
        return syncPostResponse(url, UUID.randomUUID().toString(), requestBody);
    }

    /**
     * 上传
     *
     * @param url         url
     * @param clientId    clientId
     * @param requestBody 上传body
     * @return 响应消息体
     * @throws IOException io异常
     */
    public Response syncPostResponse(String url, String clientId, RequestBody requestBody) throws IOException {
        Request request = new Request
                .Builder()
                .header("Authorization", "Client-ID " + clientId)
                .header("Connection", "keep-alive")
                .url(url)
                .post(requestBody)
                .build();
        return mOkHttpClient.newCall(request).execute();
    }

    /**
     * 异步上传
     *
     * @param url         url
     * @param requestBody 上传body
     * @param callback    回调
     */
    public void asyncPostResponse(String url, RequestBody requestBody, Callback callback) {
        asyncPostResponse(url, UUID.randomUUID().toString(), requestBody, callback);
    }

    /**
     * 异步上传
     *
     * @param url         url
     * @param clientId    clientId
     * @param requestBody 上传body
     * @param callback    回调
     */
    public void asyncPostResponse(String url, String clientId, RequestBody requestBody, Callback callback) {
        Request request = new Request
                .Builder()
                .header("Authorization", "Client-ID " + clientId)
                .header("Connection", "keep-alive")
                .url(url)
                .post(requestBody)
                .build();
        mOkHttpClient.newCall(request).enqueue(callback);
    }

}
