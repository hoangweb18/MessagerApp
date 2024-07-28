package com.example.messagerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.example.messagerapp.databinding.ActivityPhoneNumberBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PhoneNumberActivity extends AppCompatActivity {
    private ActivityPhoneNumberBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Kiểm tra nếu tài khoản đã có số điện thoại
            if (currentUser.getPhoneNumber() != null) {
                // Gửi OTP trực tiếp
                sendVerificationCode(currentUser.getPhoneNumber());
            } else {
                // Hiển thị màn hình nhập số điện thoại
                binding.buttonSendOtp.setOnClickListener(v -> {
                    String phoneNumber = Objects.requireNonNull(binding.inputPhoneNumber.getText()).toString().trim();
                    if (phoneNumber.isEmpty() || !isValidPhoneNumber(phoneNumber)) {
                        showToast("Check your number phone");
                    } else {
                        //binding.progressBar.setVisibility(View.VISIBLE);
                        sendVerificationCode(phoneNumber);
                    }
                });
            }
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.startsWith("+") && phoneNumber.length() > 1;
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                // Không cần làm gì cả
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                //binding.progressBar.setVisibility(View.GONE);
                                showToast("Verification failed: " + e.getMessage());
                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                super.onCodeSent(s, token);
                                verificationId = s;
                                resendingToken = token;
                                //binding.progressBar.setVisibility(View.GONE);
                                goToEnterOtpActivity(phoneNumber, verificationId);
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void goToEnterOtpActivity(String phoneNumber, String verificationId) {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("verification_id", verificationId);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
