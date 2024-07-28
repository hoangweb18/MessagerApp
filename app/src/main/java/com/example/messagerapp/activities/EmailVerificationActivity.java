package com.example.messagerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messagerapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button buttonCheckEmailVerified, buttonResendEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        buttonCheckEmailVerified = findViewById(R.id.buttonCheckEmailVerified);
        buttonResendEmail = findViewById(R.id.buttonResendEmail);

        buttonCheckEmailVerified.setOnClickListener(v -> checkEmailVerified());
        buttonResendEmail.setOnClickListener(v -> resendVerificationEmail());
    }

    private void checkEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(this, "Email đã được xác thực", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Email chưa được xác thực", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Đã gửi lại email xác thực", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể gửi lại email xác thực", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}