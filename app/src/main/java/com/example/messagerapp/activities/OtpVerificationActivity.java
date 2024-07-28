package com.example.messagerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messagerapp.databinding.ActivityOtpVerificationBinding;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {
    private ActivityOtpVerificationBinding binding;
    private String phoneNumber;
    private String verificationId;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        preferenceManager = new PreferenceManager(getApplicationContext());
        getDataIntent();

        mAuth = FirebaseAuth.getInstance();
        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String otp = Objects.requireNonNull(binding.inputOtp.getText()).toString().trim();
            if (otp.isEmpty()) {
                showToast("Vui lòng nhập mã OTP");
            } else {
                //binding.progressBar.setVisibility(View.VISIBLE);
                verifyOtp(otp);
            }
        });

        binding.buttonResendOtp.setOnClickListener(v -> {
            onClickResendOtp();
            showToast("OTP đã được gửi lại");
        });
    }

    private void getDataIntent() {
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        verificationId = getIntent().getStringExtra("verification_id");
    }

    private void verifyOtp(String otp) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        linkCredential(credential);
    }

    private void onClickResendOtp() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setForceResendingToken(resendingToken)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                //linkCredential(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                showToast("Verification failed: " + e.getMessage());
                            }

                            @Override
                            public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                super.onCodeSent(id, token);
                                verificationId = id;
                                resendingToken = token;
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void linkCredential(PhoneAuthCredential credential) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.getPhoneNumber() != null && user.getPhoneNumber().equals(phoneNumber)) {
                // Số điện thoại đã tồn tại, bỏ qua liên kết và chuyển đến MainActivity
                //binding.progressBar.setVisibility(View.GONE);
                goToMainActivity();
            } else {
                // Số điện thoại chưa tồn tại, tiến hành liên kết
                user.linkWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //binding.progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                goToMainActivity();
                            }
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                showToast("The verification code entered was invalid");
                            }
                        }
                    }
                });
            }
        }
    }

    private void goToMainActivity() {
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
