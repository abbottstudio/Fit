package com.fitcoachpro.app.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a Retrofit client pointed at whatever backend URL/token are
 * currently saved in PrefsRepository. Rebuilt on demand (not cached as a
 * singleton) because the user can change the backend URL from Settings
 * without restarting the app.
 */
object BackendApiClient {

    fun create(baseUrl: String, sharedSecret: String): BackendApi {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $sharedSecret")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            // BASIC, not BODY - avoid logging health/weight data to logcat.
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // Claude API calls can take a while
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
