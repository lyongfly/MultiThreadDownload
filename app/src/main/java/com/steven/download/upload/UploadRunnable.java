package com.steven.download.upload;


import android.support.annotation.NonNull;

import com.steven.download.okhttp.OkHttpManager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

/**
 * 上传线程
 */
class UploadRunnable implements Runnable {
    /**
     * 文件上传的url
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
     * 终端Id
     */
    private String clientId;
    /**
     * 上传回调
     */
    private UploadCallback mUploadCallback;

    UploadRunnable(String folder, String name, String url, String clientId, UploadCallback uploadCallback) {
        this.folder = folder;
        this.name = name;
        this.url = url;
        this.clientId = clientId;
        this.mUploadCallback = uploadCallback;
    }

    @Override
    public void run() {
        try {
            File file = new File(folder, name);
            RequestBody fileBody = RequestBody
                    .create(MediaType.parse("application/octet-stream"), file);
            MultipartBody requestBody = new MultipartBody
                    .Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"mFile\"; filename=\"" + name + "\""), fileBody)
                    .build();
            Response response = OkHttpManager.getInstance().syncPostResponse(url, clientId, new ProgressRequestBody(requestBody, file, mUploadCallback));
            if (!response.isSuccessful()) {
                String responseContent = (response.body() == null ? "" : response.body().string());
                mUploadCallback.onFailure(new Exception(responseContent));
            }
        } catch (IOException e) {
            mUploadCallback.onFailure(e);
        }
    }

    /**
     * 关闭流
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

    /**
     * 带上传文件的进度条
     */
    private class ProgressRequestBody extends RequestBody {
        private RequestBody mRequestBody;
        private UploadCallback mUploadCallback;
        private File mFile;

        public ProgressRequestBody(RequestBody requestBody, File file, UploadCallback uploadCallback) {
            this.mRequestBody = requestBody;
            this.mFile = file;
            this.mUploadCallback = uploadCallback;
        }

        @Override
        public MediaType contentType() {
            return mRequestBody.contentType();
        }

        @Override
        public long contentLength() {
            try {
                return mRequestBody.contentLength();
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) {
            BufferedSink bufferedSink = null;
            try {
                bufferedSink = Okio.buffer(new ForwardingSink(sink) {
                    @Override
                    public void write(@NonNull Buffer source, long byteCount) throws IOException {
                        super.write(source, byteCount);
                        mUploadCallback.onProgress(byteCount, contentLength());
                    }
                });
                bufferedSink.timeout().timeout(OkHttpManager.getInstance().getOkHttpClient().readTimeoutMillis(), TimeUnit.MILLISECONDS);
                mRequestBody.writeTo(bufferedSink);
                bufferedSink.flush();
                mUploadCallback.onSuccess(mFile);
            } catch (IOException e) {
                mUploadCallback.onFailure(e);
            } finally {
                close(bufferedSink);
                close(sink);
            }
        }
    }
}
