package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.adapt.api.ApiService;
import com.example.adapt.api.AssistantRequest;
import com.example.adapt.api.AssistantResponse;
import com.example.adapt.api.RetrofitClient;
import com.example.adapt.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssistantViewModel extends ViewModel {
    private final ApiService apiService = RetrofitClient.getApiService();
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private int currentSessionId = -1;

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void sendMessage(String text, int patientId) {
        if (text == null || text.trim().isEmpty()) return;

        List<ChatMessage> currentMessages = messages.getValue();
        if (currentMessages == null) currentMessages = new ArrayList<>();
        currentMessages.add(new ChatMessage(text, true));
        messages.setValue(currentMessages);

        isLoading.setValue(true);

        AssistantRequest request = new AssistantRequest(
                patientId,
                text,
                currentSessionId == -1 ? null : String.valueOf(currentSessionId)
        );

        apiService.askAssistant(request).enqueue(new Callback<AssistantResponse>() {
            @Override
            public void onResponse(Call<AssistantResponse> call, Response<AssistantResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    currentSessionId = response.body().session_id;
                    List<ChatMessage> updatedMessages = messages.getValue();
                    if (updatedMessages != null) {
                        updatedMessages.add(new ChatMessage(response.body().reply, false));
                        messages.setValue(updatedMessages);
                    }
                }
            }

            @Override
            public void onFailure(Call<AssistantResponse> call, Throwable t) {
                isLoading.setValue(false);
                List<ChatMessage> updatedMessages = messages.getValue();
                if (updatedMessages != null) {
                    updatedMessages.add(new ChatMessage("Error: Could not connect to assistant.", false));
                    messages.setValue(updatedMessages);
                }
            }
        });
    }
}
