package com.steven.download.okhttp;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Description:
 * Data：11/29/2017-3:36 PM
 *
 * @author: yanzhiwen
 */
public class OkHttpManager {

    private static OkHttpManager okHttpManager;
    private final OkHttpClient okHttpClient;

    private OkHttpManager(OkHttpClient okHttpClient) {
        if (okHttpClient == null) {
            this.okHttpClient = new OkHttpClient();
        } else {
            this.okHttpClient = okHttpClient;
        }
    }

    public static OkHttpManager getInstance() {
        return getInstance(null);
    }

    public static OkHttpManager getInstance(OkHttpClient okHttpClient) {
        if (okHttpManager == null) {
            synchronized (OkHttpManager.class) {
                if (okHttpManager == null) {
                    okHttpManager = new OkHttpManager(okHttpClient);
                }
            }
        }
        return okHttpManager;
    }

    public Call asyncCall(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        return okHttpClient.newCall(request);
    }

    public Response syncResponse(String url, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                //Range 请求头格式Range: bytes=start-end
                .addHeader("Range", "bytes=" + start + "-" + end)
                .build();
        return okHttpClient.newCall(request).execute();
    }
}
