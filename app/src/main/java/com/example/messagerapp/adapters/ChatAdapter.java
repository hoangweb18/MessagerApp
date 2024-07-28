package com.example.messagerapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagerapp.R;
import com.example.messagerapp.databinding.ItemContainerReceivedMessageBinding;
import com.example.messagerapp.databinding.ItemContainerSentMessageBinding;
import com.example.messagerapp.listeners.ChatListener;
import com.example.messagerapp.models.ChatMessage;

import java.io.IOException;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final Bitmap receiverProfileImage;
    private final String senderID;
    private final ChatListener chatListener;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    private static final int MAX_SEEK_BAR_LENGTH_DP = 100;

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderID, ChatListener chatListener) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderID = senderID;
        this.chatListener = chatListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderID.equals(senderID)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;
        private MediaPlayer mediaPlayer;
        private boolean isPlaying = false;
        private Handler handler = new Handler();

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            if (chatMessage.message != null && !chatMessage.message.isEmpty()) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setText(chatMessage.message);
            } else {
                binding.textMessage.setVisibility(View.GONE);
            }
            binding.textDateTime.setText(chatMessage.dateTime);

            if (chatMessage.imageBase64 != null && !chatMessage.imageBase64.isEmpty()) {
                binding.imageAttachment.setVisibility(View.VISIBLE);
                binding.extendImage.setVisibility(View.VISIBLE);
                binding.imageAttachment.setImageBitmap(decodeBase64ToBitmap(chatMessage.imageBase64));
            } else {
                binding.imageAttachment.setVisibility(View.GONE);
                binding.extendImage.setVisibility(View.GONE);
            }

            if (chatMessage.fileName != null && !chatMessage.fileName.isEmpty()) {
                binding.fileAttachment.setVisibility(View.VISIBLE);
                binding.textFileName.setText(chatMessage.fileName);
            } else {
                binding.fileAttachment.setVisibility(View.GONE);
            }

            if (chatMessage.videoPath != null && !chatMessage.videoPath.isEmpty()) {
                binding.videoAttachment.setVisibility(View.VISIBLE);
                binding.videoAttachment.setVideoURI(Uri.parse(chatMessage.videoPath));
                binding.buttonPlayPause.setVisibility(View.VISIBLE);
                binding.buttonFullScreen.setVisibility(View.VISIBLE);

                VideoView videoView = binding.videoAttachment;
                ImageButton buttonPlayPause = binding.buttonPlayPause;
                buttonPlayPause.setImageResource(R.drawable.ic_play_arrow);
                videoView.setOnPreparedListener(mp -> {
                    // Tự động play 1s đầu tiên để load trước video
                    videoView.seekTo(10);  // Load trước 1s
                    videoView.start();

                    // Dừng lại sau khi load 1s
                    new Handler().postDelayed(() -> {
                        videoView.pause();
                        videoView.seekTo(0); // Quay lại đầu video
                        buttonPlayPause.setImageResource(R.drawable.ic_play_arrow); // Đặt lại nút thành play
                    }, 1000);
                });
                buttonPlayPause.setOnClickListener(v -> {
                    if (videoView.isPlaying()) {
                        videoView.pause();
                        buttonPlayPause.setImageResource(R.drawable.ic_play_arrow);
                    } else {
                        videoView.start();
                        buttonPlayPause.setImageResource(R.drawable.ic_pause);
                    }
                });
            } else {
                binding.videoAttachment.setVisibility(View.GONE);
                binding.buttonPlayPause.setVisibility(View.GONE);
                binding.buttonFullScreen.setVisibility(View.GONE);
            }

            if (chatMessage.icon != null && !chatMessage.icon.isEmpty()) {
                binding.imageLike.setVisibility(View.VISIBLE); // Hiển thị view cho icon
                binding.imageLike.setText(chatMessage.icon); // Thiết lập icon
            } else {
                binding.imageLike.setVisibility(View.GONE);
            }

            if(chatMessage.audioPath != null && !chatMessage.audioPath.isEmpty()){
                binding.audioControlsLayout.setVisibility(View.VISIBLE);
                // thêm câu lệnh xử lý các thành phần trong audioControlsLayout
                // buttonPlayPauseAudio
                // seekBarAudio
                //textAudioTime
                initializeMediaPlayer(chatMessage.audioPath);
            } else {
                binding.audioControlsLayout.setVisibility(View.GONE);
            }

            binding.textMessage.setOnClickListener(v -> chatListener.onTextMessageClicked(chatMessage));
            binding.imageAttachment.setOnClickListener(v -> chatListener.onImageAttachmentClicked(chatMessage));
            binding.fileAttachment.setOnClickListener(v -> chatListener.onFileAttachmentClicked(chatMessage));
            //binding.videoAttachment.setOnClickListener(v -> chatListener.onVideoAttachmentClicked(chatMessage));
            binding.iconPickerButton.setOnClickListener(v -> chatListener.onClickChoseIcon(chatMessage));
            binding.extendImage.setOnClickListener(v -> chatListener.onClickImageExtend(chatMessage));
            binding.buttonFullScreen.setOnClickListener(v -> chatListener.onVideoAttachmentClicked(chatMessage));
        }

        private void initializeMediaPlayer(String audioPath) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(audioPath);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int duration = mediaPlayer.getDuration(); // Thời lượng của audio trong milliseconds
            int maxSeekBar = calculateSeekBarLength(duration);
            binding.seekBarAudio.setMax(maxSeekBar);

            binding.textAudioTime.setText(getFormattedTime(duration));

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_play_arrow_24);
            });

            binding.buttonPlayPauseAudio.setOnClickListener(v -> {
                if (isPlaying) {
                    pauseAudio();
                } else {
                    playAudio();
                }
            });

            binding.seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                        binding.textAudioTime.setText(getFormattedTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                binding.seekBarAudio.setMax(mediaPlayer.getDuration());
                updateSeekBar();
            });
        }

        private int calculateSeekBarLength(int duration) {
            // Tính toán độ dài của SeekBar dựa trên độ lượng thực của media
            // Ví dụ: SeekBar có chiều dài tối đa là MAX_SEEK_BAR_LENGTH_DP cho audio dài 20s
            // Thì công thức có thể là: maxSeekBar = (int) (MAX_SEEK_BAR_LENGTH_DP * duration / 20000f);
            // Trong đó 20000f là thời lượng tối đa của media (20 giây)
            return (int) (MAX_SEEK_BAR_LENGTH_DP * duration / 20000f); // 20000f là thời lượng tối đa của media (20 giây)
        }

        private void playAudio() {
            mediaPlayer.start();
            binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_pause_24);
            isPlaying = true;
            updateSeekBar();
        }

        private void pauseAudio() {
            mediaPlayer.pause();
            binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_play_arrow_24);
            isPlaying = false;
        }

        private void updateSeekBar() {
            if (isPlaying) {
                binding.seekBarAudio.setProgress(mediaPlayer.getCurrentPosition());
                binding.textAudioTime.setText(getFormattedTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this::updateSeekBar, 1000);
            }
        }

        private String getFormattedTime(int milliseconds) {
            int minutes = (milliseconds / 1000) / 60;
            int seconds = (milliseconds / 1000) % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }



        private Bitmap decodeBase64ToBitmap(String base64String) {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }
    }

    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;
        private MediaPlayer mediaPlayer;
        private boolean isPlaying = false;
        private Handler handler = new Handler();

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (chatMessage.message != null && !chatMessage.message.isEmpty()) {
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setText(chatMessage.message);
            } else {
                binding.textMessage.setVisibility(View.GONE);
            }
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);

            if (chatMessage.imageBase64 != null && !chatMessage.imageBase64.isEmpty()) {
                binding.imageAttachment.setVisibility(View.VISIBLE);
                binding.extendImage.setVisibility(View.VISIBLE);
                binding.imageAttachment.setImageBitmap(decodeBase64ToBitmap(chatMessage.imageBase64));
            } else {
                binding.imageAttachment.setVisibility(View.GONE);
                binding.extendImage.setVisibility(View.GONE);
            }

            if (chatMessage.fileName != null && !chatMessage.fileName.isEmpty()) {
                binding.fileAttachment.setVisibility(View.VISIBLE);
                binding.textFileName.setText(chatMessage.fileName);
            } else {
                binding.fileAttachment.setVisibility(View.GONE);
            }

            if (chatMessage.videoPath != null && !chatMessage.videoPath.isEmpty()) {
                binding.videoAttachment.setVisibility(View.VISIBLE);
                binding.videoAttachment.setVideoURI(Uri.parse(chatMessage.videoPath));
                binding.buttonPlayPause.setVisibility(View.VISIBLE);
                binding.buttonFullScreen.setVisibility(View.VISIBLE);

                VideoView videoView = binding.videoAttachment;
                ImageButton buttonPlayPause = binding.buttonPlayPause;
                buttonPlayPause.setImageResource(R.drawable.ic_play_arrow);
                videoView.setOnPreparedListener(mp -> {
                    // Tự động play 1s đầu tiên để load trước video
                    videoView.seekTo(10);  // Load trước 1s
                    videoView.start();

                    // Dừng lại sau khi load 1s
                    new Handler().postDelayed(() -> {
                        videoView.pause();
                        videoView.seekTo(0); // Quay lại đầu video
                        buttonPlayPause.setImageResource(R.drawable.ic_play_arrow); // Đặt lại nút thành play
                    }, 1000);
                });
                buttonPlayPause.setOnClickListener(v -> {
                    if (videoView.isPlaying()) {
                        videoView.pause();
                        buttonPlayPause.setImageResource(R.drawable.ic_play_arrow);
                    } else {
                        videoView.start();
                        buttonPlayPause.setImageResource(R.drawable.ic_pause);
                    }
                });
            } else {
                binding.videoAttachment.setVisibility(View.GONE);
                binding.buttonPlayPause.setVisibility(View.GONE);
                binding.buttonFullScreen.setVisibility(View.GONE);
            }

            if (chatMessage.icon != null && !chatMessage.icon.isEmpty()) {
                binding.imageLike.setVisibility(View.VISIBLE); // Hiển thị view cho icon
                binding.imageLike.setText(chatMessage.icon); // Thiết lập icon
            } else {
                binding.imageLike.setVisibility(View.GONE);
            }

            if(chatMessage.audioPath != null && !chatMessage.audioPath.isEmpty()){
                binding.audioControlsLayout.setVisibility(View.VISIBLE);
                // thêm câu lệnh xử lý các thành phần trong audioControlsLayout
                // buttonPlayPauseAudio
                // seekBarAudio
                //textAudioTime
                initializeMediaPlayer(chatMessage.audioPath);
            } else {
                binding.audioControlsLayout.setVisibility(View.GONE);
            }

            binding.textMessage.setOnClickListener(v -> chatListener.onTextMessageClicked(chatMessage));
            binding.imageAttachment.setOnClickListener(v -> chatListener.onImageAttachmentClicked(chatMessage));
            binding.fileAttachment.setOnClickListener(v -> chatListener.onFileAttachmentClicked(chatMessage));
            //binding.videoAttachment.setOnClickListener(v -> chatListener.onVideoAttachmentClicked(chatMessage));
            binding.iconPickerButton.setOnClickListener(v -> chatListener.onClickChoseIcon(chatMessage));
            binding.extendImage.setOnClickListener(v -> chatListener.onClickImageExtend(chatMessage));
            binding.buttonFullScreen.setOnClickListener(v -> chatListener.onVideoAttachmentClicked(chatMessage));
        }

        private void initializeMediaPlayer(String audioPath) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(audioPath);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int duration = mediaPlayer.getDuration(); // Thời lượng của audio trong milliseconds
            int maxSeekBar = calculateSeekBarLength(duration);
            binding.seekBarAudio.setMax(maxSeekBar);

            binding.textAudioTime.setText(getFormattedTime(duration));

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_play_arrow_24);
            });

            binding.buttonPlayPauseAudio.setOnClickListener(v -> {
                if (isPlaying) {
                    pauseAudio();
                } else {
                    playAudio();
                }
            });

            binding.seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                        binding.textAudioTime.setText(getFormattedTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                binding.seekBarAudio.setMax(mediaPlayer.getDuration());
                updateSeekBar();
            });
        }

        private int calculateSeekBarLength(int duration) {
            // Tính toán độ dài của SeekBar dựa trên độ lượng thực của media
            // Ví dụ: SeekBar có chiều dài tối đa là MAX_SEEK_BAR_LENGTH_DP cho audio dài 20s
            // Thì công thức có thể là: maxSeekBar = (int) (MAX_SEEK_BAR_LENGTH_DP * duration / 20000f);
            // Trong đó 20000f là thời lượng tối đa của media (20 giây)
            return (int) (MAX_SEEK_BAR_LENGTH_DP * duration / 20000f); // 20000f là thời lượng tối đa của media (20 giây)
        }

        private void playAudio() {
            mediaPlayer.start();
            binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_pause_24);
            isPlaying = true;
            updateSeekBar();
        }

        private void pauseAudio() {
            mediaPlayer.pause();
            binding.buttonPlayPauseAudio.setImageResource(R.drawable.ic_play_arrow_24);
            isPlaying = false;
        }

        private void updateSeekBar() {
            if (isPlaying) {
                binding.seekBarAudio.setProgress(mediaPlayer.getCurrentPosition());
                binding.textAudioTime.setText(getFormattedTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this::updateSeekBar, 1000);
            }
        }

        private String getFormattedTime(int milliseconds) {
            int minutes = (milliseconds / 1000) / 60;
            int seconds = (milliseconds / 1000) % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        private Bitmap decodeBase64ToBitmap(String base64String) {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }
    }
}
