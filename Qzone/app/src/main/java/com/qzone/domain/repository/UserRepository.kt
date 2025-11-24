package com.qzone.domain.repository

import com.qzone.data.model.AuthResult
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.Survey
import com.qzone.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUser: Flow<UserProfile>
    suspend fun signIn(username: String, password: String): AuthResult
    suspend fun register(username: String, email: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun updateProfile(edit: EditableProfile)
    suspend fun refreshUserProfile(): Boolean
    suspend fun recordSurveyCompletion(survey: Survey)
    suspend fun deductPoints(amount: Int)
    suspend fun updatePoints(totalPoints: Int)
    suspend fun uploadAvatar(imageBytes: ByteArray, contentType: String, filename: String = "avatar.jpg"): Boolean
    suspend fun recordRedemption(reward: com.qzone.data.model.Reward)
    suspend fun signOut()
    fun hasCachedSession(): Boolean
}
