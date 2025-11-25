package com.qzone.data.network

import com.qzone.data.model.NearbyLocation
import com.qzone.data.network.model.ApiResult
import com.qzone.data.network.model.NearbyLocationRequest
import com.qzone.data.network.model.LoginRequest
import com.qzone.data.network.model.LoginResponse
import com.qzone.data.network.model.RegisterRequest
import com.qzone.data.network.model.SubmitAnswerItem
import com.qzone.data.network.model.SubmitResponseResult
import com.qzone.data.network.model.NetworkSurveyDetail
import com.qzone.data.network.model.NetworkSurveyQuestion
import com.qzone.data.network.model.NetworkSurveyOption
import com.qzone.data.network.model.NetworkUserProfile
import com.qzone.data.network.model.RedeemCouponRequest
import com.qzone.data.network.model.UploadUrlRequest
import com.qzone.data.network.model.UploadUrlResponse
import com.qzone.data.network.model.UpdateAvatarRequest
import com.qzone.data.network.model.ThirdPartyLoginRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QzoneApiService {

    @POST("/api/user/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    @POST("/api/user/third-party")
    suspend fun loginThirdParty(@Body request: ThirdPartyLoginRequest): ApiResult<LoginResponse>

    @POST("/api/user/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<LoginResponse>

    @POST("/api/response/")
    suspend fun submitResponses(@Body body: List<SubmitAnswerItem>): ApiResult<SubmitResponseResult>

    @POST("/api/location/nearby")
    suspend fun getNearbyLocations(@Body body: NearbyLocationRequest): ApiResult<List<NearbyLocation>>

    @GET("/api/survey/{id}")
    suspend fun getSurveyDetail(@Path("id") id: String): ApiResult<NetworkSurveyDetail>

    @GET("/api/survey/{surveyId}/questions")
    suspend fun getSurveyQuestions(@Path("surveyId") surveyId: String): ApiResult<List<NetworkSurveyQuestion>>

    @GET("/api/survey/question/{questionId}/options")
    suspend fun getQuestionOptions(@Path("questionId") questionId: String): ApiResult<List<NetworkSurveyOption>>

    @GET("/api/user/user/me")
    suspend fun getCurrentUserProfile(): ApiResult<NetworkUserProfile>

    @POST("/api/coupon/redeem")
    suspend fun redeemCoupon(@Body request: RedeemCouponRequest): ApiResult<Int>

    @POST("/api/survey/upload-url")
    suspend fun getUploadUrl(@Body request: UploadUrlRequest): ApiResult<UploadUrlResponse>

    @POST("/api/user/avatar")
    suspend fun updateAvatar(@Body request: UpdateAvatarRequest): ApiResult<String>

    @POST("/api/response/user/surveys")
    suspend fun getUserSurveyHistory(@Body request: com.qzone.data.network.model.UserSurveyHistoryRequest): ApiResult<com.qzone.data.network.model.UserSurveyHistoryResponse>
}

