package com.steven.download;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.steven.download.download.DownloadCallback;
import com.steven.download.download.DownloadDispatcher;
import com.steven.download.utils.Utils;
import com.steven.download.widget.CircleProgressbar;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_CODE = 0x088;
    private String[] url = {
            "http://gdown.baidu.com/data/wisegame/f170a8c78bcf9aac/QQ_818.apk",
            "http://acj3.pc6.com/pc6_soure/2018-9/com.nuomi_372.apk",
            "http://acj3.pc6.com/pc6_soure/2018-12/com.baidu.lbs.waimai_152.apk",
            "https://downapp.baidu.com/Baidunetdisk/AndroidPhone/9.1.3.0/1/1021768b/20181130185829/Baidunetdisk_AndroidPhone_9-1-3-0_1021768b.apk?responseContentDisposition=attachment%3Bfilename%3D%22Baidunetdisk_AndroidPhone_1021768b.apk%22&responseContentType=application%2Fvnd.android.package-archive&request_id=1544935977_4410740200&type=static",
            "http://acj3.pc6.com/pc6_soure/2018-12/com.baidu.baidutranslate_92.apk",
            "http://gdown.baidu.com/data/wisegame/89eb17d6287ae627/weixin_1300.apk",
            "http://gdown.baidu.com/data/wisegame/89fce26b620d8d43/QQkongjian_109.apk"};
    private String[] names = {"QQ_818.apk", "weixin_13001.apk", "weixin_13002.apk", "weixin_130021.apk", "weixin_13003.apk", "weixin_13004.apk", "QQkongjian_109.apk"};
    private CircleProgressbar mQQpb;
    private CircleProgressbar mWeChatPb;
    private CircleProgressbar mQzonePb;

    private static final String STATUS_DOWNLOADING = "downloading";
    private static final String STATUS_STOP = "stop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mQQpb = findViewById(R.id.qq_pb);
        mQQpb.setTag(STATUS_DOWNLOADING);
        mWeChatPb = findViewById(R.id.wechat_pb);
        mWeChatPb.setTag(STATUS_DOWNLOADING);
        mQzonePb = findViewById(R.id.qzone_pb);
        mQzonePb.setTag(STATUS_DOWNLOADING);
        mQQpb.setOnClickListener(this);
        mWeChatPb.setOnClickListener(this);
        mQzonePb.setOnClickListener(this);
        findViewById(R.id.btn_all).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        int isPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (isPermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, "需要存储权限", Toast.LENGTH_SHORT).show();
                //没有授权的话，直接finish掉Activity
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.qq_pb:
                if (mQQpb.getTag().equals(STATUS_DOWNLOADING)) {
                    mQQpb.setTag(STATUS_STOP);
                    DownloadDispatcher.getInstance().startDownload(names[0], url[0], new DownloadCallback() {

                        @Override
                        public void onFailure(Exception e) {
                            Log.i(TAG, "onFailure: 多线程下载失败 " + e.getMessage());
                        }

                        @Override
                        public void onStart(String fileName) {

                        }

                        @Override
                        public void onSuccess(File file) {
                            Log.i(TAG, "onSuccess:多线程下载成功 " + file.getAbsolutePath());
                        }

                        @Override
                        public void onProgress(final long totalProgress, final long currentLength) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mQQpb.setCurrentProgress(Utils.keepTwoBit((float) totalProgress / currentLength));
                                }
                            });
                        }

                        @Override
                        public void onPause(File file) {
                            Log.i(TAG, "onPause:暂停下载 ");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, names[0] + "暂停下载", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else if (mQQpb.getTag().equals(STATUS_STOP)) {
                    mQQpb.setTag(STATUS_DOWNLOADING);
                    DownloadDispatcher.getInstance().stopDownLoad(url[0]);
                    mQQpb.setText("继续");
                }
                break;
            case R.id.wechat_pb:
                if (mWeChatPb.getTag().equals(STATUS_DOWNLOADING)) {
                    mWeChatPb.setTag(STATUS_STOP);
                    DownloadDispatcher.getInstance().startDownload(names[1], url[1], new DownloadCallback() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.i("DownLoadActivity", "onFailure: 多线程下载失败");
                        }

                        @Override
                        public void onStart(String fileName) {

                        }

                        @Override
                        public void onSuccess(File file) {
                            Log.i("DownLoadActivity", "onSuccess:多线程下载成功 " + file.getAbsolutePath());
                        }

                        @Override
                        public void onProgress(final long totalProgress, final long currentLength) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mWeChatPb.setCurrentProgress(Utils.keepTwoBit((float) totalProgress / currentLength));
                                }
                            });
                        }

                        @Override
                        public void onPause(File file) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, names[1] + "暂停下载", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else if (mWeChatPb.getTag().equals(STATUS_STOP)) {
                    mWeChatPb.setTag(STATUS_DOWNLOADING);
                    DownloadDispatcher.getInstance().stopDownLoad(url[1]);
                    mWeChatPb.setText("继续");
                }
                break;
            case R.id.qzone_pb:
                DownloadDispatcher.getInstance().startDownload(names[2], url[2], new DownloadCallback() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.i(TAG, "onFailure: 多线程下载失败 " + e.getMessage());
                    }

                    @Override
                    public void onStart(String fileName) {

                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.i(TAG, "onSuccess:多线程下载成功 " + file.getAbsolutePath());
                    }

                    @Override
                    public void onProgress(final long totalProgress, final long currentLength) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mQzonePb.setCurrentProgress(Utils.keepTwoBit((float) totalProgress / currentLength));
                            }
                        });
                    }

                    @Override
                    public void onPause(File file) {

                    }
                });
                break;
            case R.id.btn_all:
                String folder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "apk";
                for (int i = 0; i < url.length; i++) {
                    DownloadDispatcher.getInstance().setMaxTaskSize(2).startDownload(folder, names[i], url[i], new DownloadCallback() {
                        @Override
                        public void onStart(String fileName) {
                            Log.i(TAG,"onStart-> " + fileName);
                        }

                        @Override
                        public void onSuccess(File file) {
                            Log.i(TAG, "onSuccess->" + file.getName());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "onFailure->" + e.getMessage());
                        }

                        @Override
                        public void onProgress(long progress, long currentLength) {
//                            Log.i(TAG,fileName +":" + Utils.keepTwoBit((float) progress / currentLength));
                        }

                        @Override
                        public void onPause(File file) {
                            Log.e(TAG, "onPause-> " + file.getName());
                        }
                    });
                }
                break;
            case R.id.btn_delete:
                DownloadDispatcher.getInstance().cancelAll();
                break;
            default:
        }
    }

}