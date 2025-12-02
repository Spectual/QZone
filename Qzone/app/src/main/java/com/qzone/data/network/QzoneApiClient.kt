package com.qzone.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.qzone.util.QLog

object QzoneApiClient {

    private const val BASE_URL = "http://52.14.58.34:8082/"
    private const val TAG = "QzoneApiClient"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Use BODY during debugging to capture full request/response payload including query params
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val path = original.url.encodedPath
                val isAuthEndpoint = path == "/api/user/login" || 
                                   path == "/api/user/register" || 
                                   path == "/api/user/third-party"
                val token = AuthTokenProvider.accessToken
                val request = if (!isAuthEndpoint && !token.isNullOrBlank()) {
                    original.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .addInterceptor(ApiCallLoggingInterceptor())
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401) {
                    QLog.w(TAG) { "Received HTTP 401 for ${response.request.url.encodedPath}; token may be missing or expired" }
                }
                response
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val service: QzoneApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(QzoneApiService::class.java)
    }
}

