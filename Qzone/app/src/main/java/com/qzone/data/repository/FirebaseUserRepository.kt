package com.qzone.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.qzone.data.model.AuthResult
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.RewardRedemption
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.data.model.UserProfile
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.LoginRequest
import com.qzone.data.network.model.LoginResponse
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val apiService: com.qzone.data.network.QzoneApiService = QzoneApiClient.service
) : UserRepository {

    private val tokens = MutableStateFlow<AuthTokens?>(null)

    private val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())

    private val _currentUser = MutableStateFlow(DEFAULT_PROFILE)
    override val currentUser: Flow<UserProfile> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult {
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = auth.currentUser
                ?: return@runCatching AuthResult(success = false, errorMessage = "未找到 Firebase 用户")
            refreshSession(firebaseUser)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            AuthResult(success = false, errorMessage = throwable.toReadableMessage())
        }
    }

    override suspend fun register(username: String, email: String, password: String): AuthResult {
        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
            )?.await()
            val firebaseUser = auth.currentUser
                ?: return@runCatching AuthResult(success = false, errorMessage = "注册成功但未找到用户会话")
            refreshSession(firebaseUser)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            AuthResult(success = false, errorMessage = throwable.toReadableMessage())
        }
    }

    override suspend fun updateProfile(edit: EditableProfile) {
        val current = _currentUser.value
        val updated = current.copy(
            displayName = edit.displayName,
            email = edit.email,
            countryRegion = edit.countryRegion
        )
        _currentUser.emit(updated)
        auth.currentUser?.let { firebaseUser ->
            runCatching {
                if (firebaseUser.email != edit.email && edit.email.isNotBlank()) {
                    firebaseUser.updateEmail(edit.email).await()
                }
                if (edit.displayName.isNotBlank()) {
                    firebaseUser.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(edit.displayName)
                            .build()
                    ).await()
                }
            }
        }
    }

    override suspend fun recordSurveyCompletion(survey: Survey) {
        val current = _currentUser.value
        val newHistory = SurveyHistoryItem(
            id = "history_${System.currentTimeMillis()}",
            surveyId = survey.id,
            title = survey.title,
            completedAt = formatter.format(Date()),
            pointsEarned = survey.points,
            locationLabel = survey.locationLabel
        )
        val updated = current.copy(
            totalPoints = current.totalPoints + survey.points,
            history = listOf(newHistory) + current.history
        )
        delay(200)
        _currentUser.emit(updated)
    }

    override suspend fun signOut() {
        auth.signOut()
        tokens.emit(null)
        _currentUser.emit(DEFAULT_PROFILE)
    }

    private suspend fun refreshSession(firebaseUser: FirebaseUser): AuthResult {
        val tokenResult = firebaseUser.getIdToken(true).await()
        val idToken = tokenResult.token ?: return AuthResult(success = false, errorMessage = "未能获取 Firebase Token")
        Log.d(TAG, "Firebase token: $idToken")
        val response = runCatching { apiService.login(LoginRequest(idToken)) }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            return AuthResult(success = false, errorMessage = throwable.toReadableMessage())
        }
        if (!response.success || response.data == null) {
            return AuthResult(success = false, errorMessage = response.msg ?: "登录失败")
        }
        val data = response.data
        tokens.emit(
            AuthTokens(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken
            )
        )
        Log.d(TAG, "Access token: ${data.accessToken}")
        _currentUser.emit(data.toUserProfile(firebaseUser.uid, firebaseUser.photoUrl?.toString(), firebaseUser.displayName))
        return AuthResult(success = true)
    }

    private fun Throwable.toReadableMessage(): String {
        return message ?: "请求失败，请稍后再试"
    }

    private fun LoginResponse.toUserProfile(
        userId: String,
        avatarFromFirebase: String?,
        firebaseDisplayName: String?
    ): UserProfile {
        val nextRankGap = pointsToNextRank ?: 0
        val tierGoal = (currentPoints + nextRankGap).coerceAtLeast(currentPoints)
        return UserProfile(
            id = userId,
            displayName = userName.ifBlank { firebaseDisplayName ?: email.substringBefore("@") },
            email = email,
            avatarUrl = avatarUrl ?: avatarFromFirebase,
            levelLabel = rank ?: "Member",
            totalPoints = currentPoints,
            tierPointsGoal = tierGoal,
            location = "",
            countryRegion = "",
            history = _currentUser.value.history,
            redemptions = _currentUser.value.redemptions
        )
    }

    private data class AuthTokens(
        val accessToken: String,
        val refreshToken: String
    )

    companion object {
        private const val TAG = "QzoneAuth"
        private val DEFAULT_PROFILE = UserProfile(
            id = "",
            displayName = "Guest",
            email = "",
            avatarUrl = null,
            levelLabel = "Member",
            totalPoints = 0,
            tierPointsGoal = 100,
            location = "",
            countryRegion = "",
            history = emptyList(),
            redemptions = emptyList<RewardRedemption>()
        )

        fun ensureFirebaseInitialized(app: android.app.Application) {
            if (FirebaseApp.getApps(app).isEmpty()) {
                FirebaseApp.initializeApp(app)
            }
        }
    }
}

