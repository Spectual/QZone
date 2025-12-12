package com.qzone.data.network.model

import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyOption
import com.qzone.data.model.SurveyQuestion
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

data class PhoneLoginRequest(
    @Json(name = "tokenId")
    val tokenId: String
)

data class RegisterRequest(
    @Json(name = "firebaseToken")
    val firebaseToken: String,
    @Json(name = "userName")
    val userName: String,
    @Json(name = "email")
    val email: String
)

data class PhoneBindingRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "firebaseToken") val firebaseToken: String
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

data class SubmitResponseResult(
    @Json(name = "responseId") val responseId: String? = null,
    @Json(name = "surveyId") val surveyId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "isComplete") val isComplete: Boolean = false,
    @Json(name = "answeredQuestions") val answeredQuestions: Int = 0,
    @Json(name = "totalQuestions") val totalQuestions: Int = 0,
    @Json(name = "completionRate") val completionRate: Double = 0.0,
    @Json(name = "isFirstComplete") val isFirstComplete: Boolean = false
)

// Nearby location query body (POST fallback)
data class NearbyLocationRequest(
    @Json(name = "userLat") val userLat: Double,
    @Json(name = "userLng") val userLng: Double,
    @Json(name = "radiusKm") val radiusKm: Double = 3.0
)

data class NetworkSurveyDetail(
    @Json(name = "documentId") val documentId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "points") val points: Int? = null,
    @Json(name = "questionNumber") val questionNumber: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "questions") val questions: List<NetworkSurveyQuestion>? = null
) {
    fun toSurvey(existingSurvey: Survey? = null): Survey {
        val fallback = existingSurvey ?: Survey(
            id = documentId,
            title = title,
            description = description.orEmpty(),
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            points = points ?: 0,
            questions = emptyList(),
            questionCount = questionNumber ?: 0,
            isCompleted = false
        )
        val questionList = questions?.map { it.toSurveyQuestion() } ?: fallback.questions
        return fallback.copy(
            title = title.ifBlank { fallback.title },
            description = description ?: fallback.description,
            latitude = latitude ?: fallback.latitude,
            longitude = longitude ?: fallback.longitude,
            points = points ?: fallback.points,
            questions = questionList,
            questionCount = questionNumber ?: if (questionList.isNotEmpty()) questionList.size else fallback.questionCount,
            answers = existingSurvey?.answers ?: fallback.answers
        )
    }
}

data class NetworkSurveyQuestion(
    @Json(name = "documentId") val documentId: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "questionId") val questionId: String? = null,
    @Json(name = "surveyId") val surveyId: String? = null,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "required") val required: Boolean,
    @Json(name = "options") val options: List<NetworkSurveyOption>? = null
) {
    fun resolvedId(): String = documentId ?: id ?: questionId ?: content.hashCode().toString()

    fun toSurveyQuestion(resolvedOptions: List<NetworkSurveyOption>? = options): SurveyQuestion {
        return SurveyQuestion(
            id = resolvedId(),
            type = type,
            content = content,
            required = required,
            options = resolvedOptions?.map { it.toSurveyOption() }
        )
    }
}

data class NetworkSurveyOption(
    @Json(name = "documentId") val documentId: String? = null,
    @Json(name = "questionId") val questionId: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "allowFill") val allowFill: Boolean? = null
) {
    fun toSurveyOption(): SurveyOption {
        return SurveyOption(
            content = content,
            label = label ?: content
        )
    }
}

data class NetworkUserProfile(
    @Json(name = "documentId") val documentId: String = "",
    @Json(name = "userName") val userName: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "currentPoints") val currentPoints: Int = 0,
    @Json(name = "rank") val rank: String? = null,
    @Json(name = "pointsToNextRank") val pointsToNextRank: Int = 0,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)

data class RedeemCouponRequest(
    @Json(name = "requiredPoints") val requiredPoints: Int,
    @Json(name = "couponName") val couponName: String
)

data class CouponListRequest(
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "pageSize") val pageSize: Int = 10
)

data class CouponListResponse(
    @Json(name = "total") val total: Int,
    @Json(name = "records") val records: List<NetworkCouponRecord>
)

data class NetworkCouponRecord(
    @Json(name = "documentId") val documentId: String,
    @Json(name = "userId") val userId: String?,
    @Json(name = "couponName") val couponName: String,
    @Json(name = "deductedPoints") val deductedPoints: Int,
    @Json(name = "createTime") val createTime: String,
    @Json(name = "updateTime") val updateTime: String?
)

data class UploadUrlRequest(
    @Json(name = "filename") val filename: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "expiresSeconds") val expiresSeconds: Int = 600
)

data class UploadUrlResponse(
    @Json(name = "uploadUrl") val uploadUrl: String,
    @Json(name = "publicUrl") val publicUrl: String,
    @Json(name = "contentType") val contentType: String? = null
)

data class UpdateAvatarRequest(
    @Json(name = "avatarUrl") val avatarUrl: String
)

data class ThirdPartyLoginRequest(
    @Json(name = "tokenId")
    val tokenId: String
)

data class UserSurveyHistoryRequest(
    @Json(name = "page") val page: Int,
    @Json(name = "pageSize") val pageSize: Int,
    @Json(name = "status") val status: String? = null
)

data class UserSurveyHistoryResponse(
    @Json(name = "total") val total: Int,
    @Json(name = "records") val records: List<UserSurveyRecord>
)

data class UserSurveyRecord(
    @Json(name = "surveyId") val surveyId: String,
    @Json(name = "surveyTitle") val surveyTitle: String,
    @Json(name = "surveyDescription") val surveyDescription: String? = null,
    @Json(name = "surveyImageUrl") val surveyImageUrl: String?,
    @Json(name = "responseId") val responseId: String?,
    @Json(name = "responseTime") val responseTime: String?,
    @Json(name = "answeredQuestions") val answeredQuestions: Int,
    @Json(name = "totalQuestions") val totalQuestions: Int,
    @Json(name = "completionRate") val completionRate: Double,
    @Json(name = "isComplete") val isComplete: Boolean,
    @Json(name = "status") val status: String
)

data class NetworkResponseDetail(
    @Json(name = "responseId") val responseId: String? = null,
    @Json(name = "surveyId") val surveyId: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "answeredQuestions") val answeredQuestions: Int? = null,
    @Json(name = "totalQuestions") val totalQuestions: Int? = null,
    @Json(name = "completionRate") val completionRate: Double? = null,
    @Json(name = "responseTime") val responseTime: String? = null,
    @Json(name = "questionAnswers") val questionAnswers: List<NetworkQuestionAnswer>? = emptyList()
)

data class NetworkQuestionAnswer(
    @Json(name = "documentId") val documentId: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "questionType") val questionType: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "questionContent") val questionContent: String? = null,
    @Json(name = "answered") val answered: Boolean? = null,
    @Json(name = "selectedOptions") val selectedOptions: List<String>? = null,
    @Json(name = "textContent") val textContent: String? = null
)
