package com.example.messagerapp.listeners;

import com.example.messagerapp.models.ChatMessage;

public interface ChatListener {
    void onTextMessageClicked(ChatMessage chatMessage);
    void onImageAttachmentClicked(ChatMessage chatMessage);
    void onFileAttachmentClicked(ChatMessage chatMessage);
    void onClickChoseIcon(ChatMessage chatMessage);
    void onClickImageExtend(ChatMessage chatMessage);
    void onVideoAttachmentClicked(ChatMessage chatMessage);
}
