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

data class RegisterRequest(
    @Json(name = "firebaseToken")
    val firebaseToken: String,
    @Json(name = "userName")
    val userName: String,
    @Json(name = "email")
    val email: String
)

data class LoginResponse(
    @Json(name = "accessToken")
    val accessToken: String,
    @Json(name = "refreshToken")
    val refreshToken: String
)

data class SubmitAnswerItem(
    @Json(name = "questionId")
    val questionId: String,
    @Json(name = "selected")
    val selected: String?,
    @Json(name = "content")
    val content: String?
)

// Nearby location query body
data class NearbyLocationRequest(
    @Json(name = "userLat") val userLat: Double,
    @Json(name = "userLng") val userLng: Double,
    @Json(name = "radiusKm") val radiusKm: Double,
    @Json(name = "precision") val precision: Int,
    @Json(name = "maxResults") val maxResults: Int,
    @Json(name = "includeDistance") val includeDistance: Boolean,
    @Json(name = "sortByDistance") val sortByDistance: Boolean
)

