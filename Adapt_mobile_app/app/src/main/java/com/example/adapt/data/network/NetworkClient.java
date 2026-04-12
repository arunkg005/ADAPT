package com.example.adapt.data.network;

import android.content.Context;

import com.example.adapt.BuildConfig;
import com.example.adapt.utils.PrefManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NetworkClient {

    private static volatile Retrofit retrofit;

    private NetworkClient() {
    }

    public static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            synchronized (NetworkClient.class) {
                if (retrofit == null) {
                    retrofit = createRetrofit(context.getApplicationContext());
                }
            }
        }

        return retrofit;
    }

    private static Retrofit createRetrofit(Context context) {
        PrefManager prefManager = new PrefManager(context);

        Interceptor authInterceptor = chain -> {
            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder();

            String token = prefManager.getToken();
            if (token != null && !token.trim().isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + token.trim());
            }

            return chain.proceed(requestBuilder.build());
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    android.util.Log.d("ADAPT_NETWORK", String.format("--> %s %s", request.method(), request.url()));
                    
                    try {
                        okhttp3.Response response = chain.proceed(request);
                        android.util.Log.d("ADAPT_NETWORK", String.format("<-- %d %s %s", response.code(), request.method(), request.url()));
                        return response;
                    } catch (java.io.IOException e) {
                        android.util.Log.e("ADAPT_NETWORK", String.format("<-- ERROR %s %s: %s", request.method(), request.url(), e.getMessage()));
                        throw e;
                    }
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://10.0.2.2:3001/api/";
        }

        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    public static void reset() {
        retrofit = null;
    }
}
