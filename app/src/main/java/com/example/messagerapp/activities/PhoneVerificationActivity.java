package com.example.messagerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messagerapp.databinding.ActivityPhoneVerificationBinding;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private ActivityPhoneVerificationBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("USER_ID");

        setListeners();
        setUpCallbacks();
    }

    private void setListeners() {
        binding.buttonSendOtp.setOnClickListener(v -> {
            String phoneNumber = binding.inputPhoneNumber.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                showToast("Enter phone number");
            } else {
                sendVerificationCode(phoneNumber);
            }
        });

        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String code = binding.inputOtp.getText().toString().trim();
            if (code.isEmpty()) {
                showToast("Enter OTP");
            } else {
                verifyCode(code);
            }
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                callbacks
        );
    }

    private void setUpCallbacks() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                showToast("Verification Failed: " + e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                verificationId = s;
            }
        };
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        storeUserData();
                    } else {
                        showToast("Verification Failed: " + task.getException().getMessage());
                    }
                });
    }

    private void storeUserData() {
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_PHONE, binding.inputPhoneNumber.getText().toString().trim());

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(user)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, userId);
                    preferenceManager.putString(Constants.KEY_PHONE, binding.inputPhoneNumber.getText().toString().trim());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to save user data: " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
