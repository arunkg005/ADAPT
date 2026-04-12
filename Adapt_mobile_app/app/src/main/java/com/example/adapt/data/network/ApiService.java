package com.example.adapt.data.network;

import com.example.adapt.data.network.dto.AlertCreateRequest;
import com.example.adapt.data.network.dto.AiChatRequest;
import com.example.adapt.data.network.dto.AiChatResponse;
import com.example.adapt.data.network.dto.AssistNextActionRequest;
import com.example.adapt.data.network.dto.AssistNextActionResponse;
import com.example.adapt.data.network.dto.ApiListResponse;
import com.example.adapt.data.network.dto.BackendAlert;
import com.example.adapt.data.network.dto.BackendDevice;
import com.example.adapt.data.network.dto.BackendPatient;
import com.example.adapt.data.network.dto.BackendTelemetry;
import com.example.adapt.data.network.dto.DeviceCreateRequest;
import com.example.adapt.data.network.dto.EvaluateTelemetryRequest;
import com.example.adapt.data.network.dto.EvaluateTelemetryResponse;
import com.example.adapt.data.network.dto.PatientCreateRequest;
import com.example.adapt.data.network.dto.TaskLabGenerateRequest;
import com.example.adapt.data.network.dto.TaskLabGenerateResponse;
import com.example.adapt.data.network.dto.TaskPlanCreateRequest;
import com.example.adapt.data.network.dto.TaskPlanResponse;
import com.example.adapt.data.network.dto.TelemetryIngestRequest;
import com.example.adapt.data.network.dto.TelemetryIngestResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface ApiService {
    // Backend-aligned endpoints.
    @GET("patients")
    Call<ApiListResponse<BackendPatient>> getPatients(
        @Query("limit") int limit,
        @Query("offset") int offset
    );

    @POST("patients")
    Call<BackendPatient> createPatient(@Body PatientCreateRequest request);

    @GET("alerts")
    Call<ApiListResponse<BackendAlert>> getAlerts(
        @Query("limit") int limit,
        @Query("offset") int offset
    );

    @POST("alerts")
    Call<BackendAlert> createAlert(@Body AlertCreateRequest request);

    @PUT("alerts/{alertId}/acknowledge")
    Call<BackendAlert> acknowledgeAlert(@Path("alertId") String alertId);

    @GET("devices")
    Call<ApiListResponse<BackendDevice>> getDevices(
        @Query("limit") int limit,
        @Query("offset") int offset
    );

    @POST("devices")
    Call<BackendDevice> createDevice(@Body DeviceCreateRequest request);

    @POST("telemetry")
    Call<TelemetryIngestResponse> submitTelemetry(@Body TelemetryIngestRequest request);

    @GET("telemetry/{patientId}")
    Call<List<BackendTelemetry>> getTelemetryByPatient(
        @Path("patientId") String patientId,
        @Query("signalType") String signalType,
        @Query("limitMs") Long limitMs
    );

    @POST("telemetry/evaluate/{patientId}")
    Call<EvaluateTelemetryResponse> evaluateTelemetry(
        @Path("patientId") String patientId,
        @Body EvaluateTelemetryRequest request
    );

    @POST("ai/task-lab/generate")
    Call<TaskLabGenerateResponse> generateTaskLabDraft(@Body TaskLabGenerateRequest request);

    @POST("ai/chat")
    Call<AiChatResponse> chatWithAssistant(@Body AiChatRequest request);

    @POST("task-lab/plans")
    Call<TaskPlanResponse> createTaskLabPlan(@Body TaskPlanCreateRequest request);

    @POST("cognitive/assist-mode/next-action")
    Call<AssistNextActionResponse> getAssistModeNextAction(@Body AssistNextActionRequest request);
}
