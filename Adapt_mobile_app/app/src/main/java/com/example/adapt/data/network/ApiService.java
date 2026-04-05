package com.example.adapt.data.network;

import com.example.adapt.data.model.Routine;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @GET("routines")
    Call<List<Routine>> getRoutines();

    @GET("tasks/{routineId}")
    Call<List<Task>> getTasks(@Path("routineId") int routineId);

    @POST("task-log")
    Call<Void> postTaskLog(@Body TaskLog taskLog);
}
