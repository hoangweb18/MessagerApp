package com.example.messagerapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messagerapp.R;
import com.example.messagerapp.databinding.ActivityUserProfileBinding;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private RoundedImageView imageProfile;
    private TextView textEmail, textPhone;
    private EditText editTextName, editTextBirthdate, editTextAddress;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        preferenceManager = new PreferenceManager(getApplicationContext());

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Initialize views
        imageProfile = findViewById(R.id.imageProfile);
        textEmail = findViewById(R.id.textEmail);
        textPhone = findViewById(R.id.textPhone);
        editTextName = findViewById(R.id.textName);
        editTextBirthdate = findViewById(R.id.textBirthdate);
        editTextAddress = findViewById(R.id.textAddress);
        btnSave = findViewById(R.id.btnEdit);

        setListeners();

        // Load user's current profile data from Firestore and populate the EditTexts
        loadUserProfileData();

        // Save button click listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });
    }

    private void setListeners() {
        imageProfile.setOnClickListener(v -> showImagePicker());
    }

    private void loadUserProfileData() {
        // Retrieve current user's document reference
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = mFirestore.collection(Constants.KEY_COLLECTION_USERS).document(userId);

        // Fetch user data from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);


                String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                String phone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
                String name = documentSnapshot.getString(Constants.KEY_NAME);
                String birthdate = documentSnapshot.getString(Constants.KEY_BIRTHDATE);
                String address = documentSnapshot.getString(Constants.KEY_ADDRESS);

                // Populate EditTexts with user data
                imageProfile.setImageBitmap(bitmap);
                textEmail.setText(email);
                textPhone.setText(phone);
                editTextName.setText(name);
                editTextBirthdate.setText(birthdate);
                editTextAddress.setText(address);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(UserProfile.this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserProfile() {
        // Retrieve updated information from EditTexts
        String name = editTextName.getText().toString().trim();
        String birthdate = editTextBirthdate.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        // Validate inputs (optional)

        // Update user data in Firestore
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = mFirestore.collection(Constants.KEY_COLLECTION_USERS).document(userId);

        // Create a map to update only the specified fields
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_NAME, name);
        updates.put(Constants.KEY_BIRTHDATE, birthdate);
        updates.put(Constants.KEY_ADDRESS, address);
        // Perform the update
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserProfile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                    // Update PreferenceManager with new user data (if necessary)
                    updatePreferenceManager();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserProfile.this, "Failed to update profile!", Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePreferenceManager() {
        FirebaseUser user = mAuth.getCurrentUser();
        String userEmail = user.getEmail();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).whereEqualTo(Constants.KEY_EMAIL,userEmail).get().addOnCompleteListener(task -> {
            if(task.isSuccessful() && task.getResult() != null
                    && task.getResult().getDocuments().size() > 0 ) {
                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                preferenceManager.putString(Constants.KEY_BIRTHDATE,documentSnapshot.getString(Constants.KEY_BIRTHDATE));
                preferenceManager.putString(Constants.KEY_ADDRESS,documentSnapshot.getString(Constants.KEY_ADDRESS));
                updateConversations(documentSnapshot.getId(), documentSnapshot.getString(Constants.KEY_NAME), documentSnapshot.getString(Constants.KEY_IMAGE));
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void showImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                            imageProfile.setImageBitmap(bitmap);
                            updateProfileImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
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
                    imageProfile.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> showToast("Không thể cập nhật ảnh"));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateConversations(String userId, String userName, String userImage) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Cập nhật thông tin người gửi
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                            documentSnapshot.getReference().update(
                                    Constants.KEY_SENDER_NAME, userName,
                                    Constants.KEY_SENDER_IMAGE, userImage
                            );
                        }
                    }
                });

        // Cập nhật thông tin người nhận
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                            documentSnapshot.getReference().update(
                                    Constants.KEY_RECEIVER_NAME, userName,
                                    Constants.KEY_RECEIVER_IMAGE, userImage
                            );
                        }
                    }
                });
    }
}
