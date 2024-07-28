package com.example.messagerapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.messagerapp.R;
import com.example.messagerapp.adapters.RecentConversationsAdapter;
import com.example.messagerapp.databinding.ActivityMainBinding;
import com.example.messagerapp.listeners.ConversionListener;
import com.example.messagerapp.models.ChatMessage;
import com.example.messagerapp.models.User;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversation;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private String encodedImage;
    private User user;
    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        init();
        loadUserDetail();
        getToken();
        setListeners();
        listenConversations();
    }

    private void init() {
        conversation = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversation, this);
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.faNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
        binding.imageProfile.setOnClickListener(v -> showUserInfoDialog());
    }

    private void loadUserDetail() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        conversationsAdapter.notifyDataSetChanged();
    }

    private Bitmap getBitmapFromEncodedString(String encodeImage) {
        byte[] bytes= Base64.decode(encodeImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderID = senderId;
                    chatMessage.receiverID = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    } else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionID = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversation.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversation.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversation.get(i).senderID.equals(senderId) && conversation.get(i).receiverID.equals(receiverId)) {
                            conversation.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversation.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversation, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecyclerView.smoothScrollToPosition(0);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut() {
        showToast("Signing Out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    private void showOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Lấy thông tin người dùng hiện tại từ Firebase Authentication
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String currentEmail = currentUser.getEmail();

            // Hiển thị email trong tiêu đề của dialog
            builder.setTitle("Email Account:\n" + currentEmail);
        } else {
            // Xử lý trường hợp không có người dùng đăng nhập
            builder.setTitle("Email not available");
        }

        builder.setItems(new String[]{"Đổi ảnh", "Đổi tên"}, (dialog, which) -> {
            if (which == 0) {
                showImagePicker();
            } else if (which == 1) {
                showChangeNameDialog();
            }
        }).show();
    }

    private void showUserInfoDialog() {
        // Initialize AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Retrieve user information from PreferenceManager
        String userName = preferenceManager.getString(Constants.KEY_NAME);
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String userPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        String userAddress = preferenceManager.getString(Constants.KEY_ADDRESS);
        String userBirthdate = preferenceManager.getString(Constants.KEY_BIRTHDATE);
        String userImage = preferenceManager.getString(Constants.KEY_IMAGE); // Assuming image is stored as a URL

        // Inflate custom layout for the dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_profile_info, null);

        // Bind views from the custom layout
        ImageView imageProfile = dialogView.findViewById(R.id.imageProfile);
        TextView textName = dialogView.findViewById(R.id.textName);
        TextView textEmail = dialogView.findViewById(R.id.textEmail);
        TextView textPhone = dialogView.findViewById(R.id.textPhone);
        TextView textAddress = dialogView.findViewById(R.id.textAddress);
        TextView textBirthdate = dialogView.findViewById(R.id.textBirthdate);

        // Set user information to views
        byte[] bytes = Base64.decode(userImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imageProfile.setImageBitmap(bitmap);
        textName.setText(userName);
        textEmail.setText(userEmail);
        textPhone.setText(userPhone);
        textAddress.setText(userAddress);
        textBirthdate.setText(userBirthdate);

        // Set custom view to dialog
        builder.setView(dialogView);

        // Set Edit button and its click listener
        builder.setPositiveButton("Chỉnh sửa", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Navigate to EditProfileActivity or any other edit profile screen
                Intent intent = new Intent(getApplicationContext(), UserProfile.class);
                startActivity(intent);
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }




    private void showImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            updateProfileImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void showChangeNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi tên");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Đổi", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                updateUserName(newName);
            } else {
                Toast.makeText(MainActivity.this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserName(String newName) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_NAME, newName)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_NAME, newName);
                    binding.textName.setText(newName);
                    Toast.makeText(MainActivity.this, "Tên đã được cập nhật", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> showToast("Không thể cập nhật tên"));
    }

    private void updateProfileImage(Bitmap bitmap) {
        String encodedImage = encodeImage(bitmap);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_IMAGE, encodedImage)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    binding.imageProfile.setImageBitmap(bitmap);
                    Toast.makeText(MainActivity.this, "Ảnh đã được cập nhật", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> showToast("Không thể cập nhật ảnh"));
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

}
