package com.steven.download.okhttp;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Description: OkHttpManager 可以自定义 OkhttpClient,如：设置https
 */
public class OkHttpManager {

    private static OkHttpManager okHttpManager;
    private static OkHttpClient mOkhttpClient;

    /**
     * 初始化okHttp
     * @param okHttpClient
     */
    public static void setOkhttpClient(OkHttpClient okHttpClient){
        mOkhttpClient = okHttpClient;
    }

    private OkHttpManager() {
        if (mOkhttpClient == null) {
            mOkhttpClient = new OkHttpClient();
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
     * @param url
     * @return
     */
    public Call asyncCall(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return mOkhttpClient.newCall(request);
    }

    /**
     * 请求
     * @param url
     * @param start
     * @param end
     * @return
     * @throws IOException
     */
    public Response syncResponse(String url, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                //Range 请求头格式Range: bytes=start-end
                .addHeader("Range", "bytes=" + start + "-" + end)
                .build();
        return mOkhttpClient.newCall(request).execute();
    }
}
