package com.qzone.data.network

import com.qzone.data.network.model.ApiResult
import com.qzone.data.network.model.LoginRequest
import com.qzone.data.network.model.LoginResponse
import com.qzone.data.network.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface QzoneApiService {

    @POST("/api/user/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    @POST("/api/user/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<LoginResponse>
}

