package com.example.messagerapp.Encrypt;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoUtils {

    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_SIZE = 256;

    // Generate AES256 key
    public static String generateAES256Key() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
    }

    // Encrypt message
    public static String encrypt(String message, String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(AES_MODE);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        byte[] encryptedMessage = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, encryptedMessage, iv.length, encryptedBytes.length);
        return Base64.encodeToString(encryptedMessage, Base64.DEFAULT);
    }

    // Decrypt message
    public static String decrypt(String encryptedMessage, String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(AES_MODE);
        byte[] decodedMessage = Base64.decode(encryptedMessage, Base64.DEFAULT);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        byte[] cipherText = new byte[decodedMessage.length - IV_LENGTH_BYTE];
        System.arraycopy(decodedMessage, 0, iv, 0, iv.length);
        System.arraycopy(decodedMessage, iv.length, cipherText, 0, cipherText.length);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        return new String(decryptedBytes);
    }
}

