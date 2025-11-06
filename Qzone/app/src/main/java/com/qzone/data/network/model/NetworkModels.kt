package com.qzone.data.network.model

import com.squareup.moshi.Json

data class ApiResult<T>(
    val code: Int,
    val msg: String?,
    val data: T?,
    val success: Boolean
)

data class LoginRequest(
    @Json(name = "firebaseToken")
    val firebaseToken: String
)

data class LoginResponse(
    @Json(name = "accessToken")
    val accessToken: String,
    @Json(name = "refreshToken")
    val refreshToken: String,
    @Json(name = "userName")
    val userName: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "avatarUrl")
    val avatarUrl: String?,
    @Json(name = "currentPoints")
    val currentPoints: Int,
    @Json(name = "rank")
    val rank: String?,
    @Json(name = "pointsToNextRank")
    val pointsToNextRank: Int?
)

