package com.example.adapt.api;

import android.content.Context;

import com.example.adapt.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client with JWT auth interceptor.
 * Must be initialized with {@link #init(Context)} before first use.
 */
public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;
    private static TokenManager tokenManager = null;

    /** Call once from Application or LoginActivity before any API usage. */
    public static void init(Context context) {
        tokenManager = new TokenManager(context);
        retrofit = null; // Force rebuild with new token manager
    }

    public static TokenManager getTokenManager() {
        return tokenManager;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            // Add auth interceptor if token manager is available
            if (tokenManager != null) {
                clientBuilder.addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = tokenManager.getAccessToken();

                    // Skip auth header for login/register endpoints
                    String path = original.url().encodedPath();
                    if (token != null && !path.contains("/auth/token")) {
                        Request authed = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(authed);
                    }

                    return chain.proceed(original);
                });
            }

            OkHttpClient client = clientBuilder.build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
