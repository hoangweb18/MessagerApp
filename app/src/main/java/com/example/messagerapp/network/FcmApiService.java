package com.example.messagerapp.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface FcmApiService {

    @Headers({
            "Content-Type: application/json",
            "Authorization: Bearer " // Thay thế bằng mã token FCM thực tế của bạn
    })
    @POST("send")
    Call<String> sendMessage(@Body JsonObject messageBody);
}
