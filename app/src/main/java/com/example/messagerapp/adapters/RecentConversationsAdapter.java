package com.example.messagerapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagerapp.databinding.ItemContainerRecentConversionBinding;
import com.example.messagerapp.listeners.ConversionListener;
import com.example.messagerapp.models.ChatMessage;
import com.example.messagerapp.models.User;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull RecentConversationsAdapter.ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData (ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
// Kiểm tra và cập nhật giao diện dựa trên trạng thái isRead của tin nhắn
            if (!chatMessage.isRead()) {
                // Tin nhắn chưa đọc: làm đậm chữ
                binding.textRecentMessage.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                // Tin nhắn đã đọc: trở về màu bình thường
                binding.textRecentMessage.setTypeface(Typeface.DEFAULT);
            }

            binding.getRoot().setOnClickListener(v -> {
                // Đánh dấu là tin nhắn đã đọc khi người dùng xem
                chatMessage.setRead(true);
                notifyItemChanged(getAdapterPosition()); // Cập nhật lại giao diện

                // Gọi listener để xử lý khi người dùng chọn tin nhắn
                User user = new User();
                user.id = chatMessage.conversionID;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }


    }

    private Bitmap getConversionImage(String encodeImage) {
        byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

}
