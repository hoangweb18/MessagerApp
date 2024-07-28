package com.example.messagerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messagerapp.R;
import com.example.messagerapp.adapters.UserAdapter;
import com.example.messagerapp.databinding.ActivitySignUpBinding;
import com.example.messagerapp.databinding.ActivityUsersBinding;
import com.example.messagerapp.listeners.UserListener;
import com.example.messagerapp.models.User;
import com.example.messagerapp.utilities.Constants;
import com.example.messagerapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private UserAdapter userAdapter;
    private List<User> usersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        getUsers();
        setListeners();
        setupSearch();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        usersList.clear(); // Clear the list before adding new users
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (currentUserId.equals(document.getId())) {
                                continue;
                            }
                            User user = document.toObject(User.class);
                            user.setId(document.getId());
                            usersList.add(user);
                        }
                        showUsers(usersList); // Show all users initially
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showUsers(List<User> users) {
        if (users.size() > 0) {
            userAdapter = new UserAdapter(users, this);
            binding.userRecyclerView.setAdapter(userAdapter);
            binding.userRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage();
        }
    }

    private void setupSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void filterUsers(String query) {
        List<User> filteredList = new ArrayList<>();
        String trimmedQuery = query.trim().toLowerCase();

        for (User user : usersList) {
            String userName = user.getName().toLowerCase();
            String userEmail = user.getEmail().toLowerCase();

            // Check if the name or email contains the query and ends with the query
            if ((userName.contains(trimmedQuery) || userName.endsWith(trimmedQuery)) ||
                    (userEmail.contains(trimmedQuery) && userEmail.endsWith(trimmedQuery))) {
                filteredList.add(user);
            }
        }

        userAdapter.filterList(filteredList); // Update RecyclerView with filtered list
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText("No users available");
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onUserClicker(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}
