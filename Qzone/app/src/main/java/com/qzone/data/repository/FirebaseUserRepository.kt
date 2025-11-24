package com.qzone.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.qzone.data.local.UserLocalStorage
import com.google.firebase.auth.GoogleAuthProvider
import com.qzone.data.model.AuthResult
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.RewardRedemption
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.data.model.UserProfile
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.AuthTokenProvider
import com.qzone.data.network.model.LoginRequest
import com.qzone.data.network.model.LoginResponse
import com.qzone.data.network.model.NetworkUserProfile
import com.qzone.data.network.model.RegisterRequest
import com.qzone.data.network.model.ThirdPartyLoginRequest
import com.qzone.data.network.model.UpdateAvatarRequest
import com.qzone.data.network.model.UploadUrlRequest
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val apiService: com.qzone.data.network.QzoneApiService = QzoneApiClient.service
) : UserRepository {

    private val tokens = MutableStateFlow<AuthTokens?>(null)
    private val uploadClient = OkHttpClient()

    private val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())

    private val _currentUser = MutableStateFlow(DEFAULT_PROFILE)
    override val currentUser: Flow<UserProfile> = _currentUser.asStateFlow()

    init {
        UserLocalStorage.load()?.let { cached ->
            val localPath = UserLocalStorage.getAvatarLocalPath()
            _currentUser.value = cached.toUserProfile(_currentUser.value, localPath)
        }
        UserLocalStorage.loadTokens()?.let { stored ->
            val cachedTokens = AuthTokens(
                accessToken = stored.accessToken,
                refreshToken = stored.refreshToken
            )
            tokens.value = cachedTokens
            AuthTokenProvider.accessToken = stored.accessToken
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = auth.currentUser
                ?: return@runCatching AuthResult(success = false, errorMessage = "Firebase user not found")
            refreshSession(firebaseUser, isThirdParty = false)
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
                ?: return@runCatching AuthResult(success = false, errorMessage = "Registration successful but user session not found")
            val tokenResult = firebaseUser.getIdToken(true).await()
            val idToken = tokenResult.token ?: return@runCatching AuthResult(success = false, errorMessage = "Failed to get Firebase Token")
            Log.d(TAG, "Firebase token (register): $idToken")
            val response = runCatching {
                apiService.register(
                    RegisterRequest(
                        firebaseToken = idToken,
                        userName = username,
                        email = email
                    )
                )
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                return@runCatching AuthResult(success = false, errorMessage = throwable.toReadableMessage())
            }
            if (!response.success || response.data == null) {
                return@runCatching AuthResult(success = false, errorMessage = response.msg ?: "Registration failed")
            }
            val data = response.data
            persistTokens(data.accessToken, data.refreshToken)
            if (!fetchAndCacheUserProfile()) {
                updateUserFromFirebase(firebaseUser)
            }
            AuthResult(success = true)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            AuthResult(success = false, errorMessage = throwable.toReadableMessage())
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        Log.d(TAG, "signInWithGoogle: starting with token length=${idToken.length}")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return runCatching {
            Log.d(TAG, "signInWithGoogle: calling auth.signInWithCredential")
            auth.signInWithCredential(credential).await()
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                Log.e(TAG, "signInWithGoogle: auth.currentUser is null after success")
                return@runCatching AuthResult(success = false, errorMessage = "Firebase user not found")
            }
            Log.d(TAG, "signInWithGoogle: Firebase sign-in success, user=${firebaseUser.uid}, email=${firebaseUser.email}")
            refreshSession(firebaseUser, isThirdParty = true)
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            Log.e(TAG, "signInWithGoogle: failed", throwable)
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

    override suspend fun refreshUserProfile(): Boolean {
        return fetchAndCacheUserProfile()
    }

    override suspend fun recordSurveyCompletion(survey: Survey) {
        val current = _currentUser.value
        val newHistory = SurveyHistoryItem(
            id = "history_${System.currentTimeMillis()}",
            surveyId = survey.id,
            title = survey.title,
            completedAt = formatter.format(Date()),
            pointsEarned = survey.points,
            locationLabel = survey.title
        )
        val updated = current.copy(
            totalPoints = current.totalPoints + survey.points,
            history = listOf(newHistory) + current.history
        )
        delay(200)
        _currentUser.emit(updated)
    }

    override suspend fun deductPoints(amount: Int) {
        val current = _currentUser.value
        val updated = current.copy(totalPoints = current.totalPoints - amount)
        delay(200)
        _currentUser.emit(updated)
    }

    override suspend fun updatePoints(totalPoints: Int) {
        val current = _currentUser.value
        val updated = current.copy(totalPoints = totalPoints)
        UserLocalStorage.load()?.let { cached ->
            UserLocalStorage.save(
                cached.copy(currentPoints = totalPoints)
            )
        }
        _currentUser.emit(updated)
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, contentType: String, filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            val uploadResponse = runCatching {
                apiService.getUploadUrl(UploadUrlRequest(filename = filename, contentType = contentType))
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                Log.e(TAG, "Failed to get upload url", throwable)
                return@withContext false
            }
            if (!uploadResponse.success || uploadResponse.data == null) {
                Log.w(TAG, "Upload url request failed: ${uploadResponse.msg}")
                return@withContext false
            }
            val uploadInfo = uploadResponse.data
            val resolvedContentType = uploadInfo.contentType ?: contentType
            val mediaType = resolvedContentType.toMediaType()
            Log.d(TAG, "Uploading avatar to ${uploadInfo.uploadUrl} size=${imageBytes.size} type=$resolvedContentType")
            val requestBody = imageBytes.toRequestBody(mediaType)
            val putRequest = Request.Builder()
                .url(uploadInfo.uploadUrl)
                .put(requestBody)
                .addHeader("Content-Type", resolvedContentType)
                .addHeader("x-amz-acl", "public-read")
                .build()
            val uploadSucceeded = runCatching {
                uploadClient.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        throw IllegalStateException("S3 upload failed code=${response.code} body=$errorBody")
                    }
                }
            }.onFailure { throwable ->
                val maskedUrl = uploadInfo.uploadUrl.substringBefore("?")
                Log.e(TAG, "S3 upload failed for $maskedUrl", throwable)
            }.isSuccess
            if (!uploadSucceeded) {
                return@withContext false
            }
            val avatarResponse = runCatching {
                apiService.updateAvatar(UpdateAvatarRequest(avatarUrl = uploadInfo.publicUrl))
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                Log.e(TAG, "Failed to update avatar url", throwable)
                return@withContext false
            }
            if (!avatarResponse.success) {
                Log.w(TAG, "Avatar update API failed: ${avatarResponse.msg}")
                return@withContext false
            }

            // Update Firebase Auth profile as well to ensure consistency
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setPhotoUri(android.net.Uri.parse(uploadInfo.publicUrl))
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                    Log.d(TAG, "Updated Firebase Auth photo URL")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update Firebase Auth photo URL", e)
                // Continue as backend update was successful
            }

            val current = _currentUser.value
            val localPath = UserLocalStorage.saveAvatarLocal(imageBytes, filename)
            val avatarUri = localPath?.let { File(it).toURI().toString() } ?: uploadInfo.publicUrl
            val updated = current.copy(avatarUrl = avatarUri)
            _currentUser.emit(updated)
            val storedNetwork = UserLocalStorage.load()
            val profileForStorage = (storedNetwork ?: NetworkUserProfile(
                documentId = updated.id,
                userName = updated.displayName,
                email = updated.email,
                avatarUrl = uploadInfo.publicUrl,
                currentPoints = updated.totalPoints,
                rank = updated.levelLabel,
                pointsToNextRank = (updated.tierPointsGoal - updated.totalPoints).coerceAtLeast(0),
                createTime = null,
                updateTime = null
            )).copy(avatarUrl = uploadInfo.publicUrl)
            UserLocalStorage.save(profileForStorage)
            true
        }
    }

    override suspend fun recordRedemption(reward: com.qzone.data.model.Reward) {
        val current = _currentUser.value
        val redemption = RewardRedemption(
            rewardId = reward.id,
            redeemedAt = formatter.format(Date()),
            status = com.qzone.data.model.RedemptionStatus.REDEEMED
        )
        val updated = current.copy(
            redemptions = listOf(redemption) + current.redemptions
        )
        delay(200)
        _currentUser.emit(updated)
    }

    override suspend fun signOut() {
        auth.signOut()
        tokens.emit(null)
        AuthTokenProvider.accessToken = null
        UserLocalStorage.clear()
        _currentUser.emit(DEFAULT_PROFILE)
    }

    override fun hasCachedSession(): Boolean {
        return tokens.value?.accessToken?.isNotBlank() == true
    }

    private suspend fun refreshSession(firebaseUser: FirebaseUser, isThirdParty: Boolean = false): AuthResult {
        val tokenResult = firebaseUser.getIdToken(true).await()
        val idToken = tokenResult.token
        if (idToken.isNullOrBlank()) {
            return AuthResult(success = false, errorMessage = "Failed to get valid Firebase Token")
        }
        Log.d(TAG, "Firebase token: $idToken")
        val response = runCatching {
            if (isThirdParty) {
                apiService.loginThirdParty(ThirdPartyLoginRequest(tokenId = idToken))
            } else {
                apiService.login(LoginRequest(idToken))
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            return AuthResult(success = false, errorMessage = throwable.toReadableMessage())
        }
        if (!response.success || response.data == null) {
            return AuthResult(success = false, errorMessage = response.msg ?: "Login failed")
        }
        val data = response.data
        persistTokens(data.accessToken, data.refreshToken)
        if (!fetchAndCacheUserProfile()) {
            updateUserFromFirebase(firebaseUser)
        }
        return AuthResult(success = true)
    }

    private fun Throwable.toReadableMessage(): String {
        return message ?: "Request failed, please try again later"
    }

    private suspend fun updateUserFromFirebase(firebaseUser: FirebaseUser) {
        val firebaseEmail = firebaseUser.email.orEmpty()
        val firebaseName = firebaseUser.displayName ?: firebaseEmail.substringBefore("@")
        val updatedProfile = _currentUser.value.copy(
            id = firebaseUser.uid,
            displayName = firebaseName.ifBlank { DEFAULT_PROFILE.displayName },
            email = firebaseEmail,
            avatarUrl = firebaseUser.photoUrl?.toString()
        )
        _currentUser.emit(updatedProfile)
        val pointsToNext = (updatedProfile.tierPointsGoal - updatedProfile.totalPoints).coerceAtLeast(0)
        UserLocalStorage.save(
            NetworkUserProfile(
                documentId = firebaseUser.uid,
                userName = updatedProfile.displayName,
                email = updatedProfile.email,
                avatarUrl = updatedProfile.avatarUrl,
                currentPoints = updatedProfile.totalPoints,
                rank = updatedProfile.levelLabel,
                pointsToNextRank = pointsToNext,
                createTime = null,
                updateTime = null
            )
        )
    }

    private suspend fun persistTokens(accessToken: String, refreshToken: String) {
        val newTokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
        tokens.emit(newTokens)
        AuthTokenProvider.accessToken = accessToken
        UserLocalStorage.saveTokens(accessToken, refreshToken)
        Log.d(TAG, "Access token stored: $accessToken")
    }

    private suspend fun fetchAndCacheUserProfile(): Boolean {
        val response = runCatching { apiService.getCurrentUserProfile() }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            Log.e(TAG, "Failed to fetch user profile", throwable)
            return false
        }
        if (!response.success || response.data == null) {
            Log.w(TAG, "User profile API failed: ${response.msg}")
            return false
        }
        Log.d(
            TAG,
            "User profile API success: name=${response.data.userName}, points=${response.data.currentPoints}, rank=${response.data.rank}, pointsToNext=${response.data.pointsToNextRank}"
        )
        var profile = response.data!!

        // Fallback to Firebase Auth photo URL if backend returns empty
        if (profile.avatarUrl.isNullOrBlank()) {
            auth.currentUser?.photoUrl?.toString()?.let { firebasePhoto ->
                if (firebasePhoto.isNotBlank()) {
                    Log.d(TAG, "Using Firebase Auth photo URL as fallback")
                    profile = profile.copy(avatarUrl = firebasePhoto)
                }
            }
        }
        val resolvedId = profile.documentId.ifBlank {
            auth.currentUser?.uid ?: _currentUser.value.id.ifBlank { DEFAULT_PROFILE.id }
        }
        if (profile.documentId != resolvedId) {
            Log.w(TAG, "User profile missing documentId, using fallback id: $resolvedId")
            profile = profile.copy(documentId = resolvedId)
        }

        UserLocalStorage.save(profile)
        val localPath = UserLocalStorage.getAvatarLocalPath()
        _currentUser.emit(profile.toUserProfile(_currentUser.value, localPath))
        return true
    }

    private fun NetworkUserProfile.toUserProfile(existing: UserProfile?, localAvatarPath: String? = null): UserProfile {
        val fallback = existing ?: DEFAULT_PROFILE
        val targetGoal = (currentPoints + pointsToNextRank).coerceAtLeast(0)
        val avatarFromLocal = localAvatarPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.toURI().toString() else null
        }
        val resolvedId = documentId.ifBlank { existing?.id?.takeIf { it.isNotBlank() } ?: fallback.id }
        return fallback.copy(
            id = resolvedId,
            displayName = userName ?: fallback.displayName,
            email = email ?: fallback.email,
            avatarUrl = avatarFromLocal ?: avatarUrl ?: fallback.avatarUrl,
            levelLabel = rank ?: fallback.levelLabel,
            totalPoints = currentPoints,
            tierPointsGoal = if (targetGoal > 0) targetGoal else fallback.tierPointsGoal,
            history = fallback.history,
            redemptions = fallback.redemptions
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

