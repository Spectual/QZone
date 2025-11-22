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
            questionCount = questionNumber ?: if (questionList.isNotEmpty()) questionList.size else fallback.questionCount
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
    @Json(name = "documentId") val documentId: String,
    @Json(name = "userName") val userName: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "currentPoints") val currentPoints: Int = 0,
    @Json(name = "rank") val rank: String? = null,
    @Json(name = "pointsToNextRank") val pointsToNextRank: Int = 0,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)

data class PointsDeductRequest(
    @Json(name = "requiredPoints") val requiredPoints: Int
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

