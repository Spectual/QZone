package com.qzone

import android.app.Application
import com.qzone.data.database.QzoneDatabase
import com.qzone.data.repository.LocationRepositoryImpl
import com.qzone.data.repository.FirebaseUserRepository
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.data.repository.PlaceholderRewardRepository
import com.qzone.data.repository.PlaceholderSurveyRepository
import com.qzone.data.repository.ApiSurveyRepository
import com.qzone.domain.repository.LocationRepository
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository

class QzoneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseUserRepository.ensureFirebaseInitialized(this)
        
        // Initialize Room database
        val database = QzoneDatabase.getInstance(this)
        val localSurveyRepository = LocalSurveyRepository(database)
        
        container = AppContainer(
            surveyRepository = ApiSurveyRepository(),
            rewardRepository = PlaceholderRewardRepository(),
            userRepository = FirebaseUserRepository(),
            locationRepository = LocationRepositoryImpl(this),
            database = database,
            localSurveyRepository = localSurveyRepository
        )
    }
}

class AppContainer(
    val surveyRepository: SurveyRepository,
    val rewardRepository: RewardRepository,
    val userRepository: UserRepository,
    val locationRepository: LocationRepository,
    val database: QzoneDatabase,
    val localSurveyRepository: LocalSurveyRepository
)
