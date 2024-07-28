package com.example.messagerapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagerapp.databinding.ItemContainerUserBinding;
import com.example.messagerapp.listeners.UserListener;
import com.example.messagerapp.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> usersFull; // Full list of users before filtering
    private UserListener userListener;

    public UserAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.usersFull = new ArrayList<>(users);
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding binding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void filterList(List<User> filteredList) {
        users = filteredList;
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ItemContainerUserBinding binding;

        public UserViewHolder(ItemContainerUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setUserData(User user) {
            binding.textName.setText(user.getName());
            binding.textEmail.setText(user.getEmail());
            binding.imageProfile.setImageBitmap(getUserImage(user.getImage()));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicker(user));
        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        }
    }
}
