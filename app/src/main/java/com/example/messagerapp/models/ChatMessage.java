package com.example.messagerapp.models;

import android.graphics.Bitmap;

import java.util.Date;

public class ChatMessage {
    public String messageID,senderID, receiverID, message, dateTime;
    public String fileName;
    public String imageBase64;
    public Date dateObject;
    public String conversionID, conversionName, conversionImage;
    public String icon;
    public String uriFile;
    public String videoPath;
    public boolean isRead;
    public String audioPath;
    public ChatMessage() {
    }

    public ChatMessage(boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
