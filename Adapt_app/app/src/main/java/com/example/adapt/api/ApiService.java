package com.example.adapt.api;

import com.example.adapt.model.Patient;
import com.example.adapt.model.CareTask;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Authentication ───────────────────────────────────
    @FormUrlEncoded
    @POST("auth/token/")
    Call<AuthResponse> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("auth/token/refresh/")
    Call<AuthResponse> refreshToken(@Field("refresh") String refreshToken);

    @POST("auth/register/")
    Call<AuthResponse> register(@Body Map<String, String> credentials);

    // ── Patients ─────────────────────────────────────────
    @GET("patients/")
    Call<List<Patient>> getPatients();

    @GET("patients/{id}/")
    Call<Patient> getPatient(@Path("id") int id);

    @POST("patients/")
    Call<Patient> addPatient(@Body Patient patient);

    @PUT("patients/{id}/")
    Call<Patient> updatePatient(@Path("id") int id, @Body Patient patient);

    @DELETE("patients/{id}/")
    Call<Void> deletePatient(@Path("id") int id);

    // ── Care Items (Tasks / Routines / Schedules) ────────
    @GET("care-items/")
    Call<List<CareTask>> getCareItems(@Query("patient_id") int patientId);

    @GET("care-items/")
    Call<List<CareTask>> getAllCareItems();

    @POST("care-items/")
    Call<CareTask> addCareTask(@Body CareTask task);

    @PUT("care-items/{id}/")
    Call<CareTask> updateCareTask(@Path("id") int id, @Body CareTask task);

    @PATCH("care-items/{id}/")
    Call<CareTask> patchCareTask(@Path("id") int id, @Body Map<String, Object> fields);

    @DELETE("care-items/{id}/")
    Call<Void> deleteCareTask(@Path("id") int id);

    // ── AI Assistant ─────────────────────────────────────
    @POST("assistant/ask/")
    Call<AssistantResponse> askAssistant(@Body AssistantRequest request);

    @POST("assistant/confirm_action/")
    Call<Map<String, String>> confirmAction(@Body Map<String, Object> payload);
}
