package com.qzone.data.repository

import com.qzone.data.model.AuthResult
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.data.model.UserProfile
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaceholderUserRepository : UserRepository {

    private val defaultProfile = PlaceholderDataSource.sampleUser()
    private var storedProfile = defaultProfile
    private val registeredUsers = mutableMapOf<String, PlaceholderCredential>()
    private val userFlow = MutableStateFlow(storedProfile)

    override val currentUser: Flow<UserProfile> = userFlow.asStateFlow()

    override suspend fun signIn(username: String, password: String): AuthResult {
        delay(300)
        if (username.isBlank() || password.isBlank()) {
            return AuthResult(success = false, errorMessage = "Missing credentials")
        }
        val credential = registeredUsers[username.lowercase()]
        if (credential != null && credential.password != password) {
            return AuthResult(success = false, errorMessage = "Incorrect password")
        }
        storedProfile = credential?.profile ?: storedProfile.copy(
            displayName = username,
            email = credential?.profile?.email ?: "${username.lowercase()}@example.com"
        )
        userFlow.value = storedProfile
        return AuthResult(success = true)
    }

    override suspend fun register(username: String, email: String, password: String): AuthResult {
        delay(400)
        if (username.isBlank() || email.isBlank() || password.length < 6) {
            return AuthResult(success = false, errorMessage = "Please fill in all fields")
        }
        val key = username.lowercase()
        if (registeredUsers.containsKey(key)) {
            return AuthResult(success = false, errorMessage = "Username already registered")
        }
        storedProfile = storedProfile.copy(
            displayName = username,
            email = email,
            location = storedProfile.location,
            countryRegion = storedProfile.countryRegion
        )
        registeredUsers[key] = PlaceholderCredential(
            username = username,
            password = password,
            profile = storedProfile
        )
        userFlow.value = storedProfile
        return AuthResult(success = true)
    }

    override suspend fun updateProfile(edit: EditableProfile) {
        delay(200)
        storedProfile = storedProfile.copy(
            displayName = edit.displayName,
            email = edit.email,
            countryRegion = edit.countryRegion
        )
        userFlow.value = storedProfile
    }

    override suspend fun recordSurveyCompletion(survey: Survey) {
        delay(250)
        val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val newHistory = SurveyHistoryItem(
            id = "history_${'$'}{System.currentTimeMillis()}",
            surveyId = survey.id,
            title = survey.title,
            completedAt = formatter.format(Date()),
            pointsEarned = survey.points,
            locationLabel = survey.locationLabel
        )
        val current = userFlow.value
        storedProfile = current.copy(
            totalPoints = current.totalPoints + survey.points,
            history = listOf(newHistory) + current.history
        )
        userFlow.value = storedProfile
    }

    override suspend fun signOut() {
        delay(200)
        storedProfile = defaultProfile
        userFlow.value = storedProfile
    }

    private data class PlaceholderCredential(
        val username: String,
        val password: String,
        val profile: UserProfile
    )

    init {
        registeredUsers[storedProfile.displayName.lowercase()] = PlaceholderCredential(
            username = storedProfile.displayName,
            password = "password",
            profile = storedProfile
        )
    }
}
