package com.qzone

import android.app.Application
import com.qzone.data.repository.PlaceholderRewardRepository
import com.qzone.data.repository.PlaceholderSurveyRepository
import com.qzone.data.repository.PlaceholderUserRepository
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository

class QzoneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            surveyRepository = PlaceholderSurveyRepository(),
            rewardRepository = PlaceholderRewardRepository(),
            userRepository = PlaceholderUserRepository()
        )
    }
}

class AppContainer(
    val surveyRepository: SurveyRepository,
    val rewardRepository: RewardRepository,
    val userRepository: UserRepository
)
