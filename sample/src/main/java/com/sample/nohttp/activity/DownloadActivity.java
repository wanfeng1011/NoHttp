/*
 * Copyright © YOLANDA. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sample.nohttp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sample.nohttp.Application;
import com.sample.nohttp.R;
import com.sample.nohttp.config.AppConfig;
import com.sample.nohttp.nohttp.CallServer;
import com.sample.nohttp.util.FileUtil;
import com.sample.nohttp.util.ProgressNotify;
import com.yolanda.nohttp.Headers;
import com.yolanda.nohttp.NoHttp;
import com.yolanda.nohttp.download.DownloadListener;
import com.yolanda.nohttp.download.DownloadRequest;
import com.yolanda.nohttp.error.ArgumentError;
import com.yolanda.nohttp.error.ClientError;
import com.yolanda.nohttp.error.NetworkError;
import com.yolanda.nohttp.error.ServerError;
import com.yolanda.nohttp.error.StorageReadWriteError;
import com.yolanda.nohttp.error.StorageSpaceNotEnoughError;
import com.yolanda.nohttp.error.TimeoutError;
import com.yolanda.nohttp.error.URLError;
import com.yolanda.nohttp.error.UnKnownHostError;

import java.io.File;

/**
 * <p>下载件demo.</p>
 * Created in Oct 10, 2015 12:58:25 PM.
 *
 * @author YOLANDA;
 */
public class DownloadActivity extends BaseActivity implements View.OnClickListener, DownloadListener {

    private final static String PROGRESS_KEY = "download_progress";
    /**
     * 下载按钮、暂停、开始等.
     */
    private TextView mBtnStart;
    /**
     * 下载状态.
     */
    private TextView mTvStatus;
    /**
     * 下载进度条.
     */
    private ProgressBar mProgressBar;
    /***
     * 下载地址.
     */
    private String url = "http://m.apk.67mo.com/apk/999129_21769077_1443483983292.apk";
    /**
     * 下载请求.
     */
    private DownloadRequest downloadRequest;

    @Override
    protected void onActivityCreate(Bundle savedInstanceState) {
        setTitle(Application.getInstance().nohttpTitleList[8]);
        setContentView(R.layout.activity_download);

        mProgressBar = findView(R.id.pb_progress);
        mBtnStart = findView(R.id.btn_start_download);
        mTvStatus = findView(R.id.tv_status);
        mBtnStart.setOnClickListener(this);

        // url 下载地址
        // fileFolder 保存的文件夹
        // fileName 文件名
        // isRange 是否断点续传下载
        // isDeleteOld 如果发现文件已经存在是否删除后重新下载
        downloadRequest = NoHttp.createDownloadRequest(url, AppConfig.getInstance().APP_PATH_ROOT, "nohttp.apk", true, false);

        // 检查之前的下载状态
        int beforeStatus = downloadRequest.checkBeforeStatus();
        switch (beforeStatus) {
            case DownloadRequest.STATUS_RESTART:
                mProgressBar.setProgress(0);
                mBtnStart.setText("开始下载");
                break;
            case DownloadRequest.STATUS_RESUME:
                int progress = AppConfig.getInstance().getInt(PROGRESS_KEY, 0);
                mProgressBar.setProgress(progress);
                mBtnStart.setText("已下载: " + progress + "%; 继续下载");
                break;
            case DownloadRequest.STATUS_FINISH:
                mProgressBar.setProgress(100);
                mBtnStart.setText("已下载完成");
                break;
            default:
                break;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(new File("sdcard/es.apk")), "application/vnd.android.package-archive");
        Application.getInstance().startActivity(intent);

    }

    @Override
    public void onClick(View v) {
        if (downloadRequest.isStarted()) {
            // 暂停下载
            downloadRequest.cancel(true);
        } else {
            // what 区分下载
            // downloadRequest 下载请求对象
            // downloadListener 下载监听
            CallServer.getDownloadInstance().add(0, downloadRequest, this);
            notify = new ProgressNotify();
            Intent intent = new Intent(this, DownloadActivity.class);
            notifyBuilder = notify.createNotification(0, intent, null, "正在下载", "正在下载", "正在下载文件，请稍候", R.mipmap.ic_launcher, 0);
        }
    }


    /**
     * 进度通知管理
     */
    private ProgressNotify notify;
    /**
     * 通知
     */
    private NotificationCompat.Builder notifyBuilder;

    @Override
    public void onStart(int what, boolean isResume, long beforeLenght, Headers headers, long allCount) {
        int progress = AppConfig.getInstance().getInt(PROGRESS_KEY, 0);
        notify.update(0, notifyBuilder, "已经下载了" + progress + "%", progress);
        if (allCount != 0) {
            progress = (int) (beforeLenght * 100 / allCount);
            mProgressBar.setProgress(progress);
        }
        mTvStatus.setText("已下载: " + progress + "%");
        mBtnStart.setText("暂停");
    }

    @Override
    public void onDownloadError(int what, Exception exception) {
        notify.cancel(0);
        mBtnStart.setText("再次尝试");

        String message = "下载出错了：";
        if (exception instanceof ClientError) {
            message += "客户端错误";
        } else if (exception instanceof ServerError) {
            message += "服务器发生内部错误";
        } else if (exception instanceof NetworkError) {
            message += "网络不可用，请检查网络";
        } else if (exception instanceof StorageReadWriteError) {
            message += "存储卡错误，请检查存储卡";
        } else if (exception instanceof StorageSpaceNotEnoughError) {
            message += "存储位置空间不足";
        } else if (exception instanceof TimeoutError) {
            message += "下载超时";
        } else if (exception instanceof UnKnownHostError) {
            message += "服务器找不到";
        } else if (exception instanceof URLError) {
            message += "url地址错误";
        } else if (exception instanceof ArgumentError) {
            message += "下载参数错误";
        } else {
            message += "未知错误";
        }
        mTvStatus.setText(message);
    }

    @Override
    public void onProgress(int what, int progress, long fileCount) {
        notify.update(0, notifyBuilder, "已经下载了" + progress + "%", progress);
        mTvStatus.setText("已下载: " + progress + "%");
        mProgressBar.setProgress(progress);
        AppConfig.getInstance().putInt(PROGRESS_KEY, progress);
    }

    @Override
    public void onFinish(int what, String filePath) {
        Intent installIntent = FileUtil.getFileIntent(filePath, "application/vnd.android.package-archive");
        notify.finish(0, notifyBuilder, "下载完成", "点击我安装", installIntent, true);

        mTvStatus.setText("下载完成, 文件保存在: \n" + filePath);
        mBtnStart.setText("重新下载");
    }

    @Override
    public void onCancel(int what) {
        notify.cancel(0);
        mTvStatus.setText("下载被暂停");
        mBtnStart.setText("继续下载");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notify != null)
            notify.cancel(0);
        if (downloadRequest != null)
            downloadRequest.cancel(true);
    }
}