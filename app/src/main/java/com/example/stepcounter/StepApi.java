package com.example.stepcounter;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface StepApi {
    @POST("steps")
    Call<Void> postStep(@Body StepPayload payload);
}
