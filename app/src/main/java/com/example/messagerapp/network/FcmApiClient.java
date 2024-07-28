package com.example.messagerapp.network;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FcmApiClient {

    private static final String BASE_URL = "https://fcm.googleapis.com/v1/projects/messageapp-f8837/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);
        return httpClient.build();
    }

    public static GoogleCredentials getGoogleCredentials(String serviceAccountKeyPath) throws IOException {
        FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);
        return GoogleCredentials.fromStream(serviceAccount)
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
    }
}
