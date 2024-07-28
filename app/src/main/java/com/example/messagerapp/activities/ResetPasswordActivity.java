package com.example.messagerapp.activities;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagerapp.databinding.ActivityResetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.buttonResetPassword.setOnClickListener(v -> {
            String email = binding.inputEmail.getText().toString().trim();
            if (isValidEmail(email)) {
                resetPassword(email);
            }
        });

        binding.buttonBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
            finish();
        });
    }

    private boolean isValidEmail(String email) {
        if (email.isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Enter valid email");
            return false;
        } else {
            return true;
        }
    }

    private void resetPassword(String email) {
        binding.progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent.");
                    } else {
                        showToast("Failed to send reset email: " + task.getException().getMessage());
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}