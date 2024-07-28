package com.example.messagerapp.Encrypt;

import android.util.Base64;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class EncryptionManager {

    private static final String COLLECTION_NAME = "encryptionKeys";
    private static final String DOCUMENT_NAME = "encryptionKey";
    private static final String MASTER_KEY = "g0Qsh79kQFsq9Pb+Twmr/z+SLFLaEKHC+ZU6R0egg04="; // Replace with your master key

    private FirebaseFirestore db;
    private String base64Key;

    public EncryptionManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void loadOrCreateEncryptionKey(OnCompleteListener<DocumentSnapshot> onCompleteListener) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(DOCUMENT_NAME);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String encryptedKey = document.getString("key");
                    try {
                        base64Key = decryptBase64Key(encryptedKey);
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(task);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        generateAndStoreEncryptionKey(docRef, onCompleteListener, task);
                    }
                } else {
                    generateAndStoreEncryptionKey(docRef, onCompleteListener, task);
                }
            } else {
                if (onCompleteListener != null) {
                    onCompleteListener.onComplete(task);
                }
            }
        });
    }

    public String getBase64Key() {
        return base64Key;
    }

    private void generateAndStoreEncryptionKey(DocumentReference docRef, OnCompleteListener<DocumentSnapshot> onCompleteListener, Task<DocumentSnapshot> task) {
        try {
            String generatedKey = CryptoUtils.generateAES256Key();
            String encryptedKey = encryptBase64Key(generatedKey);
            docRef.set(new EncryptionKey(encryptedKey))
                    .addOnSuccessListener(aVoid -> {
                        base64Key = generatedKey;
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(task);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(task);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            if (onCompleteListener != null) {
                onCompleteListener.onComplete(task);
            }
        }
    }

    private String encryptBase64Key(String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(MASTER_KEY, Base64.DEFAULT);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(base64Key.getBytes());
        byte[] encryptedMessage = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, encryptedMessage, iv.length, encryptedBytes.length);
        return Base64.encodeToString(encryptedMessage, Base64.DEFAULT);
    }

    private String decryptBase64Key(String encryptedKey) throws Exception {
        byte[] keyBytes = Base64.decode(MASTER_KEY, Base64.DEFAULT);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] decodedMessage = Base64.decode(encryptedKey, Base64.DEFAULT);
        byte[] iv = new byte[12];
        byte[] cipherText = new byte[decodedMessage.length - 12];
        System.arraycopy(decodedMessage, 0, iv, 0, iv.length);
        System.arraycopy(decodedMessage, iv.length, cipherText, 0, cipherText.length);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes);
    }

    private class EncryptionKey {
        public String key;

        public EncryptionKey(String key) {
            this.key = key;
        }
    }
}
