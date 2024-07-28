package com.example.messagerapp.activities;


import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.example.messagerapp.Encrypt.CryptoUtils;
import com.example.messagerapp.Encrypt.EncryptionManager;

import com.example.messagerapp.R;
import com.example.messagerapp.adapters.ChatAdapter;
import com.example.messagerapp.databinding.ActivityChatBinding;
import com.example.messagerapp.listeners.ChatListener;
import com.example.messagerapp.models.ChatMessage;
import com.example.messagerapp.models.User;

import com.example.messagerapp.network.ApiClient;
import com.example.messagerapp.network.ApiService;
import com.example.messagerapp.network.FirebaseAuthHelper;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity implements ChatListener {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    private boolean isUserBlocked = false;

    private EncryptionManager encryptionManager;
    private String base64Key;

    private GestureDetector gestureDetector;

    // test ghi âm
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setListeners();
        loadReceiverDetails();
        init();


        // Load or create encryption key
        encryptionManager = new EncryptionManager();
        encryptionManager.loadOrCreateEncryptionKey(task -> {
            if (task.isSuccessful() && encryptionManager.getBase64Key() != null) {
                base64Key = encryptionManager.getBase64Key();
                listenMessages();
            } else {
                showToast("Failed to load encryption key");
            }
        });



        // Initialize GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int position = binding.chatRecyclerView.getChildAdapterPosition(binding.chatRecyclerView.findChildViewUnder(e.getX(), e.getY()));
                if (position != RecyclerView.NO_POSITION) {
                    ChatMessage chatMessage = chatMessages.get(position);
                    onDoubleClick(chatMessage);
                }
                return true;
            }
        });


    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID),this
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();

    }


    private Bitmap getBitmapFromEncodedString(String encodeImage) {
        byte[] bytes= Base64.decode(encodeImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void sendMessage() {

        String messageText = binding.inputMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            showToast("Please enter a message");
            return;
        }

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);

        // Encrypt message
        String encryptedMessage;
        try {
            encryptedMessage = CryptoUtils.encrypt(messageText, base64Key);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Encryption failed: " + e.getMessage());
            return;
        }
        message.put(Constants.KEY_MESSAGE, encryptedMessage);


        // message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_MESSAGE_TYPE, "text");
        message.put(Constants.KEY_TIMESTAMP, new Date());
        // Add message to Firestore collection
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversionId != null){
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String,Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);

            // ma hoa khi lu vao
//           String encryptedMessage1 = encryptMessage(binding.inputMessage.getText().toString());
//           conversion.put(Constants.KEY_LAST_MESSAGE, encryptedMessage1);

            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }

        // thông báo
        if (!isReceiverAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA,data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                //sendNotification(body.toString());
            } catch (Exception exception){
                showToast(exception.getMessage());
            }
        }
        binding.inputMessage.setText(null);

    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()){
                    try {
                        if (response.body() != null){
                           JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null){
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                            ).intValue();
                            isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED || documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.messageID = documentChange.getDocument().getId();
                    chatMessage.senderID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                    if (documentChange.getDocument().contains(Constants.KEY_MESSAGE_TYPE)) {
                        String messageType = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                        if (messageType != null && messageType.equals("text")) {
                            // Decrypt message
                            try {
                                String encryptedMessage = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                                chatMessage.message = CryptoUtils.decrypt(encryptedMessage, base64Key);
                            } catch (Exception e) {
                                e.printStackTrace();
                                chatMessage.message = "Error decrypting message";
                            }
                        } else if (messageType != null && messageType.equals("image")) {
                            chatMessage.imageBase64 = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                            getBitmapFromEncodedString(chatMessage.imageBase64);
                        } else if (messageType != null && messageType.equals("file")) {
                            chatMessage.fileName = documentChange.getDocument().getString(Constants.KEY_FILE_NAME);
                            // giải mã fileName này
                            //chatMessage.videoPath = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                        } else if (messageType != null && messageType.equals("Video")) {
                            chatMessage.videoPath = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                        } else if(messageType != null && messageType.equals("audio")) {
                            chatMessage.audioPath = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                        }
                    }

                    chatMessage.icon = documentChange.getDocument().getString(Constants.KEY_REACTION);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);

                    // Kiểm tra xem tin nhắn đã tồn tại trong danh sách hay chưa
                    boolean messageExists = false;
                    for (int i = 0; i < chatMessages.size(); i++) {
                        if (chatMessages.get(i).messageID.equals(chatMessage.messageID)) {
                            chatMessages.set(i, chatMessage); // Cập nhật tin nhắn
                            messageExists = true;
                            break;
                        }
                    }

                    // Nếu tin nhắn chưa tồn tại, thêm mới vào danh sách
                    if (!messageExists) {
                        chatMessages.add(chatMessage);
                    }
                }
            }

            // Sắp xếp lại danh sách tin nhắn theo thời gian
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

            // Thông báo cho Adapter về sự thay đổi
            if (count == 0) {
                chatAdapter.notifyDataSetChanged(); // Nếu danh sách trống, cập nhật toàn bộ
            } else {
                chatAdapter.notifyDataSetChanged(); // Cập nhật một phần tử
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1); // Cuộn đến cuối danh sách
            }

            // Hiển thị RecyclerView và ẩn ProgressBar
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);

            // Kiểm tra nếu conversionId chưa được thiết lập, thực hiện kiểm tra chuyển đổi
            if (conversionId == null) {
                checkForConversion();
            }
        }
    };




    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.imageInfo.setOnClickListener(v -> showSenderReceiverDetails());
        binding.layoutSendAttach.setOnClickListener(v -> showSendOptionsDialog());
    }

    private String getReadableDateTime (Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot>  conversionOnCompleteListener = task ->  {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void confirmBlockUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Block User");
        builder.setMessage("Are you sure you want to block this user?");
        builder.setPositiveButton("Block", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                blockUser();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void blockUser() {
        // Implement logic to block the user
        isUserBlocked = true;
        showToast("User blocked successfully");
    }


    private void showSenderReceiverDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        builder.setTitle("Sender and Receiver Details");

        // Tạo nội dung của Dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sender_receiver_details, null);
        TextView senderName = dialogView.findViewById(R.id.textSenderName);
        TextView senderEmail = dialogView.findViewById(R.id.textSenderEmail);
        ImageView senderImage = dialogView.findViewById(R.id.imageSender);
        TextView receiverName = dialogView.findViewById(R.id.textReceiverName);
        TextView receiverEmail = dialogView.findViewById(R.id.textReceiverEmail);
        ImageView receiverImage = dialogView.findViewById(R.id.imageReceiver);

        // Hiển thị thông tin của người gửi từ PreferenceManager
        String senderNameText = "Sender Name: " + preferenceManager.getString(Constants.KEY_NAME);
        String senderEmailText = "Sender Email: " + currentUser.getEmail();
        String senderImageBase64 = preferenceManager.getString(Constants.KEY_IMAGE);
        senderName.setText(senderNameText);
        senderEmail.setText(senderEmailText);
        senderImage.setImageBitmap(getBitmapFromEncodedString(senderImageBase64));

        // Hiển thị thông tin của người nhận từ receiverUser
        FirebaseFirestore.getInstance().collection("users")
                .document(receiverUser.id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        receiverName.setText("Receiver Name: " + documentSnapshot.getString("name"));
                        receiverEmail.setText("Receiver Email: " + documentSnapshot.getString("email"));
                        String receiverImageBase64 = documentSnapshot.getString("image");
                        if (receiverImageBase64 != null && !receiverImageBase64.isEmpty()) {
                            Bitmap receiverImageBitmap = getBitmapFromEncodedString(receiverImageBase64);
                            receiverImage.setImageBitmap(receiverImageBitmap);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi không thể lấy thông tin người nhận từ Firestore
                    receiverName.setText("Receiver Name: Unknown");
                    receiverEmail.setText("Receiver Email: Unknown");
                });

        builder.setView(dialogView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Xử lý sự kiện khi nhấn nút OK (nếu cần)
        });
        builder.show();
    }

    private void showSendOptionsDialog() {
        // Tạo PopupWindow
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Thiết lập PopupWindow
        final PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        // Thiết lập soft input mode để không ẩn bàn phím
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Khởi tạo các nút
        ImageButton buttonSendImage = popupView.findViewById(R.id.buttonSendImage);
        ImageButton buttonSendVideo = popupView.findViewById(R.id.buttonSendVideo);
        ImageButton buttonAttachFile = popupView.findViewById(R.id.buttonAttachFile);
        ImageButton buttonRecord = popupView.findViewById(R.id.buttonRecord);

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                showRecordAudioLayout();
            }
        });

        buttonSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý gửi ảnh
                popupWindow.dismiss();
                sendImage();
            }
        });

        buttonSendVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý gửi video
                popupWindow.dismiss();
                selectVideo();
            }
        });

        buttonAttachFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Xử lý đính kèm file
                popupWindow.dismiss();
                attachFile();
            }
        });

        // Hiển thị PopupWindow ngay bên trên layoutSendAttach
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        // Lấy kích thước của layoutSendAttach
        int[] location = new int[2];
        binding.layoutSendAttach.getLocationOnScreen(location);
        int anchorWidth = binding.layoutSendAttach.getWidth();
        int anchorHeight = binding.layoutSendAttach.getHeight();

        // Tính toán vị trí hiển thị PopupWindow
        int offsetX = ((anchorWidth - popupWidth) / 2) - 80;
        int offsetY = anchorHeight + 120; // Nếu cần, thay đổi khoảng cách theo chiều dọc

        // Hiển thị PopupWindow tại vị trí tính toán
        popupWindow.showAtLocation(binding.layoutSendAttach, Gravity.NO_GRAVITY, location[0] + offsetX, location[1] - offsetY);
    }


    private void sendImage() {
        // Code để gửi ảnh
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImageLauncher.launch(intent);
    }

    private void attachFile() {
        // Mở Intent để chọn tệp tin
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.setType("*/*"); // Chọn tất cả các loại file
        pickFileLauncher.launch("*/*");
    }

    private void selectVideo() {
        pickVideoLauncher.launch("video/*");
    }


    private ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        // Sử dụng ContentResolver để mở InputStream
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        sendImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        showToast("Failed to pick image");
                    }
                }
            }
    );

    private void sendImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, encodedImage); // Chuyển ảnh thành chuỗi base64
        message.put(Constants.KEY_MESSAGE_TYPE, "image"); // Loại tin nhắn là ảnh
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversionId != null) {
            updateConversion("Image");
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            // Thiết lập thông tin conversion
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, "Image");
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

    }


    // xử lý dinh kem file
    private ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Xử lý và gửi file
                   uploadFileToFirebase(uri);
                } else {
                    showToast("Không thể lấy đường dẫn của tệp tin");
                }
            }
    );


    private void uploadFileToFirebase(Uri fileUri) {
        String fileName = getFileNameFromUri(fileUri);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference fileReference = storageReference.child("uploads/" + fileName);

        fileReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // Gửi thông tin file và tin nhắn đi
                            sendFile(uri.toString(), fileName);
                        })
                        .addOnFailureListener(e -> showToast("Không thể lấy URL tải xuống của tệp tin"))
                )
                .addOnFailureListener(e -> showToast("Tải tệp lên Firebase Storage thất bại"));
    }
    private void sendFile(String fileUrl, String fileName) {
        // Gửi thông tin file và tin nhắn đi
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, fileUrl); // Gửi URL của file
        message.put(Constants.KEY_FILE_NAME, fileName); // Gửi tên của file
        message.put(Constants.KEY_MESSAGE_TYPE, "file");
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        // Cập nhật conversion nếu cần
        if (conversionId != null) {
            updateConversion("File attached");
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            // Thiết lập thông tin conversion
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, "File attached");
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    @Override
    public void onTextMessageClicked(ChatMessage chatMessage) {
    }

    @Override
    public void onImageAttachmentClicked(ChatMessage chatMessage) {
        showToast("Bạn vừa ấn vào ảnh " + chatMessage.messageID);
            // Lấy chuỗi Base64 của ảnh từ chatMessage
            String base64String = chatMessage.imageBase64;

            // Giải mã Base64 thành Bitmap
            Bitmap bitmap = getBitmapFromEncodedString(base64String);

            // Hiển thị hình ảnh trong một Dialog hoặc Activity mới
            showImageInDialog(bitmap);
    }

    @Override
    public void onFileAttachmentClicked(ChatMessage chatMessage) {
        showToast("bạn vừa ấn vào file");
        DownloadFile(chatMessage);

    }

    @Override
    public void onClickChoseIcon(ChatMessage chatMessage) {
        showEmojiPicker(chatMessage);
        addEmojiToMessage(chatMessage);
    }

    @Override
    public void onClickImageExtend(ChatMessage chatMessage) {
        showToast("Bạn vừa ấn vào ảnh " + chatMessage.messageID);

        // Hiển thị hộp thoại xác nhận
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tải xuống ảnh");
        builder.setMessage("Bạn có muốn tải xuống ảnh này không?");
        builder.setPositiveButton("Có", (dialog, which) -> {
            // Gọi phương thức tải xuống ảnh nếu người dùng đồng ý
            downloadImage(chatMessage);
        });
        builder.setNegativeButton("Không", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    @Override
    public void onVideoAttachmentClicked(ChatMessage chatMessage) {
        showFullScreenVideo(chatMessage.videoPath, chatMessage);
    }

    // mới


    private void onDoubleClick(ChatMessage chatMessage) {
        showToast("ban vua nhan 2 lan" + chatMessage);
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev);
    }

    private void showEmojiPicker(ChatMessage chatMessage) {
        BottomSheetDialog emojiPickerDialog = new BottomSheetDialog(this);
        emojiPickerDialog.setContentView(R.layout.layout_emoji_picker); // Tạo layout layout_emoji_picker.xml cho dialog
        emojiPickerDialog.findViewById(R.id.emoji_like).setOnClickListener(view -> {
            addReactionToMessage(chatMessage, "👍");
            emojiPickerDialog.dismiss();
        });
        emojiPickerDialog.findViewById(R.id.emoji_heart).setOnClickListener(view -> {
            addReactionToMessage(chatMessage,  "❤️");
            emojiPickerDialog.dismiss();
        });
        emojiPickerDialog.findViewById(R.id.emoji_haha).setOnClickListener(view -> {
            addReactionToMessage(chatMessage,"😂");
            emojiPickerDialog.dismiss();
        });
        // Thêm các emoji khác tương tự
        emojiPickerDialog.show();
    }

    private void addReactionToMessage(ChatMessage chatMessage, String reaction) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .document(chatMessage.messageID)
                .update(Constants.KEY_REACTION, reaction)
                .addOnSuccessListener(aVoid -> showToast("Đã thêm cảm xúc"))
                .addOnFailureListener(e -> showToast("Thêm cảm xúc thất bại"));
    }

    private void addEmojiToMessage(ChatMessage chatMessage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .document(chatMessage.messageID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String emoji = documentSnapshot.getString(Constants.KEY_REACTION);
                        if (emoji != null && !emoji.isEmpty()) {
                            chatMessage.icon = emoji;// Cập nhật lại RecyclerView
                        }
                    } else {
                        showToast("Không tìm thấy emoji cho tin nhắn này");
                    }
                })
                .addOnFailureListener(e -> showToast("Lỗi khi lấy emoji: " + e.getMessage()));
    }

    private void downloadImage(ChatMessage chatMessage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Truy vấn Firestore để lấy thông tin của tin nhắn dựa trên chatMessageId
        DocumentReference messageRef = db.collection(Constants.KEY_COLLECTION_CHAT).document(chatMessage.messageID);
        messageRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Lấy dữ liệu từ document
                String base64Image = documentSnapshot.getString(Constants.KEY_MESSAGE);

                // Nếu có base64Image, tiến hành tải xuống ảnh
                if (base64Image != null) {
                    // Decode base64 thành byte array
                    byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);

                    // Tạo bitmap từ byte array
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    // Lưu bitmap thành file ảnh trên thiết bị
                    String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                    File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File imageFile = new File(directory, fileName);

                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.flush();
                        showToast("Đã tải xuống ảnh thành công: " + imageFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showToast("Lỗi khi tải xuống ảnh.");
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    showToast("Không tìm thấy dữ liệu ảnh trong tin nhắn.");
                }
            } else {
                showToast("Không tìm thấy tin nhắn trong Firestore.");
            }
        }).addOnFailureListener(e -> {
            showToast("Lỗi khi truy xuất dữ liệu từ Firestore: " + e.getMessage());
        });
    }

    private void showImageInDialog(Bitmap imageBitmap) {
        // Tạo một Dialog để hiển thị hình ảnh
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Tạo một ImageView để hiển thị hình ảnh
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(imageBitmap);

        // Đặt ImageView vào Dialog
        builder.setView(imageView);

        // Hiển thị Dialog
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    // gửi video
    private void sendVideo(String fileUrl, String videoPath) {
        // Gửi thông tin file và tin nhắn đi
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, fileUrl); // Gửi URL của file
        message.put(Constants.KEY_FILE_NAME, videoPath); // Gửi tên của file
        message.put(Constants.KEY_MESSAGE_TYPE, "Video");
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        // Cập nhật conversion nếu cần
        if (conversionId != null) {
            updateConversion("Video");
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            // Thiết lập thông tin conversion
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, "Video");
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
    }

    private void uploadFileToFirebase1(Uri fileUri) {
        String videoPath = getFileNameFromUri(fileUri);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference fileReference = storageReference.child("uploads/" + videoPath);

        fileReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // Gửi thông tin file và tin nhắn đi
                            sendVideo(uri.toString(), videoPath);
                        })
                        .addOnFailureListener(e -> showToast("Không thể lấy URL tải xuống của tệp tin"))
                )
                .addOnFailureListener(e -> showToast("Tải tệp lên Firebase Storage thất bại"));
    }

    private ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Xử lý và gửi file
                    uploadFileToFirebase1(uri);
                } else {
                    showToast("Không thể lấy đường dẫn của tệp tin");
                }
            }
    );

    //full screen video
    private void showFullScreenVideo(String videoPath, ChatMessage chatMessage) {
        // Tạo một Dialog để hiển thị video trong chế độ toàn màn hình
        Dialog fullScreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullScreenDialog.setContentView(R.layout.fullscreen_video_layout);

        VideoView videoView = fullScreenDialog.findViewById(R.id.fullscreenVideoView);
        ImageButton buttonClose = fullScreenDialog.findViewById(R.id.buttonCloseFullScreen);
        ImageButton buttonPlayPause = fullScreenDialog.findViewById(R.id.buttonPlayPause);
        SeekBar videoSeekBar = fullScreenDialog.findViewById(R.id.videoSeekBar);
        ImageButton buttonDown = fullScreenDialog.findViewById(R.id.buttonDownloadVideo);
        ImageButton buttonVolume = fullScreenDialog.findViewById(R.id.buttonVolume);
        TextView textVideoTime = fullScreenDialog.findViewById(R.id.textVideoTime);

        buttonDown.setOnClickListener(v -> DownloadFile(chatMessage));

        Handler handler = new Handler();
        Runnable updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (videoView.isPlaying()) {
                    int currentPosition = videoView.getCurrentPosition();
                    videoSeekBar.setProgress(currentPosition);
                    textVideoTime.setText(getFormattedTime(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };

        // Thiết lập VideoView
        videoView.setVideoURI(Uri.parse(videoPath));
        videoView.setOnPreparedListener(mp -> {
            // Tự động phát video khi đã sẵn sàng
            mp.start();
            buttonPlayPause.setImageResource(R.drawable.ic_pause_48);
            videoSeekBar.setMax(videoView.getDuration());
            textVideoTime.setText(getFormattedTime(videoView.getDuration()));
            handler.postDelayed(updateSeekBar, 1000);
        });

        videoView.setOnCompletionListener(mp -> {
            buttonPlayPause.setImageResource(R.drawable.ic_play_arrow_48);
            videoView.seekTo(0);
            videoSeekBar.setProgress(0);
            handler.removeCallbacks(updateSeekBar);
        });

        // Sự kiện khi nhấn nút đóng
        buttonClose.setOnClickListener(v -> {
            handler.removeCallbacks(updateSeekBar);
            fullScreenDialog.dismiss();
        });

        // Sự kiện khi nhấn nút play/pause
        buttonPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                buttonPlayPause.setImageResource(R.drawable.ic_play_arrow_48);
            } else {
                videoView.start();
                buttonPlayPause.setImageResource(R.drawable.ic_pause_48);
            }
        });

        // Sự kiện khi nhấn nút bật/tắt âm thanh
        buttonVolume.setOnClickListener(v -> {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    buttonVolume.setImageResource(R.drawable.volume_up_24dp_000000);
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    buttonVolume.setImageResource(R.drawable.volume_off_24dp_000000);
                }
            }
        });

        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    textVideoTime.setText(getFormattedTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.postDelayed(updateSeekBar, 1000);
            }
        });

        // Hiển thị Dialog
        fullScreenDialog.show();
    }


    private String getFormattedTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void DownloadFile(ChatMessage chatMessage) {
        if (chatMessage.messageID != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Constants.KEY_COLLECTION_CHAT) // Thay thế bằng tên collection của bạn
                    .document(chatMessage.messageID)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String fileName = document.getString(Constants.KEY_FILE_NAME);
                            String fileUrl = document.getString(Constants.KEY_MESSAGE);
                            if (fileName != null && fileUrl != null) {
                                // Hiển thị dialog xác nhận
                                new AlertDialog.Builder(this)
                                        .setTitle("Tải tệp tin")
                                        .setMessage("Bạn có muốn tải tệp tin này không?")
                                        .setPositiveButton("Có", (dialog, which) -> {
                                            // Tải file từ fileUrl (ví dụ dùng FileDownloader)
                                            FileDownloader.downloadFile(this, fileName, fileUrl);
                                            showToast("Tải file thành công");
                                        })
                                        .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                                        .show();
                            } else {
                                showToast("Không tìm thấy thông tin tệp tin");
                            }
                        } else {
                            showToast("cài đặt file thất bại");
                        }
                    });
        }
    }





// gửi tin nhắn thoại

    private void uploadAudioToFirebase(Uri audioUri) {
        String fileName = getFileNameFromUri(audioUri);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference audioReference = storageReference.child("uploads/" + fileName);

        audioReference.putFile(audioUri)
                .addOnSuccessListener(taskSnapshot -> audioReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // Gửi thông tin audio và tin nhắn đi
                            sendAudio(uri.toString(), fileName);
                        })
                        .addOnFailureListener(e -> showToast("Không thể lấy URL tải xuống của âm thanh"))
                )
                .addOnFailureListener(e -> showToast("Tải âm thanh lên Firebase Storage thất bại"));
    }

    private void sendAudio(String audioUrl, String fileName) {
        // Gửi thông tin audio và tin nhắn đi
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, audioUrl); // Gửi URL của audio
        message.put(Constants.KEY_FILE_NAME, fileName); // Gửi tên của audio
        message.put(Constants.KEY_MESSAGE_TYPE, "audio");
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        // Cập nhật conversion nếu cần
        if (conversionId != null) {
            updateConversion("Audio attached");
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            // Thiết lập thông tin conversion
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, "Audio attached");
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
    }

    // thử ghi am

    // Phương thức hiển thị layout ghi âm
    private void showRecordAudioLayout() {
        // Inflate layout ghi âm vào một PopupWindow
        View popupView = getLayoutInflater().inflate(R.layout.record_audio_layout, null);

        // Khởi tạo PopupWindow với kích thước và hiển thị
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Tìm các thành phần UI trong record_audio_layout.xml
        ImageButton buttonStartRecording = popupView.findViewById(R.id.buttonStartRecording);
        ImageButton buttonStopRecording = popupView.findViewById(R.id.buttonStopRecording);
        ImageButton buttonSendVoiceMessage = popupView.findViewById(R.id.buttonSendVoiceMessage);
        ImageButton buttonDeleteRecording = popupView.findViewById(R.id.buttonDeleteRecording);
        Chronometer chronometer = popupView.findViewById(R.id.chronometer);

        // Xử lý sự kiện khi bắt đầu ghi âm
        buttonStartRecording.setOnClickListener(v -> {
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            // Thêm mã để bắt đầu ghi âm tại đây
            startRecording();
            buttonStopRecording.setVisibility(View.VISIBLE);
            buttonDeleteRecording.setVisibility(View.VISIBLE);
            buttonSendVoiceMessage.setVisibility(View.VISIBLE);
        });

        // Xử lý sự kiện khi dừng ghi âm
        buttonStopRecording.setOnClickListener(v -> {
            // Thêm mã để dừng ghi âm tại đây
            stopRecording();
            chronometer.stop();
        });

        // Xử lý sự kiện khi gửi tin nhắn thoại
        buttonSendVoiceMessage.setOnClickListener(v -> {
            // Thêm mã để gửi tin nhắn thoại và đóng popup
            sendAudioMessage();
            popupWindow.dismiss();
        });

        // Xử lý sự kiện khi xóa ghi âm
        buttonDeleteRecording.setOnClickListener(v -> {
            // Thêm mã để xóa ghi âm và đóng popup
            new File(audioFilePath).delete();
            popupWindow.dismiss();
        });

        // Hiển thị PopupWindow tại vị trí dưới cùng của màn hình
        popupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0);
    }

    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }

        audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/audio_" + System.currentTimeMillis() + ".wav";
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);  // Ghi âm thành WAV hoặc PCM
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            showToast("Bắt đầu ghi âm...");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Không thể bắt đầu ghi âm. Vui lòng thử lại.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            showToast("Lỗi trạng thái MediaRecorder. Vui lòng thử lại.");
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    private void uploadRecordedAudio(String mp3FilePath) {
        Uri audioUri = Uri.fromFile(new File(mp3FilePath)); // mp3FilePath là đường dẫn đến file MP3
        uploadAudioToFirebase(audioUri);
    }

    private void convertToMP3(String wavFilePath) {
        String mp3FilePath = wavFilePath.replace(".wav", ".mp3");

        // Lệnh FFmpeg để chuyển đổi từ WAV sang MP3
        String[] command = {"-y", "-i", wavFilePath, "-codec:a", "libmp3lame", "-qscale:a", "2", mp3FilePath};

        // Chạy FFmpeg
        FFmpeg.executeAsync(command, (executionId, returnCode) -> {
            if (returnCode == RETURN_CODE_SUCCESS) {
                showToast("gửi thành công");
                uploadRecordedAudio(mp3FilePath);
            } else if (returnCode == RETURN_CODE_CANCEL) {
                showToast("Chuyển đổi bị hủy.");
            } else {
                showToast("Chuyển đổi thất bại.");
            }
        });
    }
    private void sendAudioMessage() {
        stopRecording(); // Dừng ghi âm trước khi chuyển đổi và gửi
        convertToMP3(audioFilePath);// Chuyển đổi từ WAV sang MP3 sau khi ghi âm đã dừng lại
    }
}