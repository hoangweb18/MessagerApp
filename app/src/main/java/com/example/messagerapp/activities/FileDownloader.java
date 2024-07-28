package com.example.messagerapp.activities;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public class FileDownloader {

    public static void downloadFile(Context context, String fileName, String fileUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName); // Đặt tiêu đề của file khi tải về
        request.setDescription("Downloading"); // Mô tả quá trình tải về
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName); // Lưu file vào thư mục Download

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }
}
