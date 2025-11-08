package com.qzone.data.placeholder

import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.data.model.SurveyOption
import com.qzone.data.model.SurveyQuestion
import com.qzone.data.model.UserProfile
import com.qzone.data.model.Reward
import com.qzone.data.model.RewardRedemption
import com.qzone.data.model.RedemptionStatus

object PlaceholderDataSource {

    fun sampleSurveys(): List<Survey> = listOf(
        mockSurveyPayload().toSurvey(id = "survey_campus_dining")
    )

    fun sampleHistory(): List<SurveyHistoryItem> = listOf(
        SurveyHistoryItem(
            id = "history_1",
            surveyId = "survey_campus_dining",
            title = "Campus Dining Satisfaction",
            completedAt = "03/15/24",
            pointsEarned = 10,
            locationLabel = "Campus Dining"
        )
    )

    fun mockSurveyPayload(): MockSurveyPayload = MockSurveyPayloadFactory.englishCampusDiningSurvey()

    fun sampleRewards(): List<Reward> = listOf(
        Reward(
            id = "reward_mcd",
            brandName = "$10 McDonalds",
            description = "$10 voucher for any menu purchase",
            pointsCost = 150,
            expiryDate = "Valid until February 2026",
            terms = "Redeemable in-store. Not valid with other offers.",
            qrCodePlaceholder = "QR-MCD-001"
        ),
        Reward(
            id = "reward_kfc",
            brandName = "KFC 25% OFF",
            description = "Get 25% off your next KFC meal",
            pointsCost = 120,
            expiryDate = "Valid until March 2026",
            terms = "Single use coupon. Present QR code at checkout.",
            qrCodePlaceholder = "QR-KFC-120"
        ),
        Reward(
            id = "reward_starbucks",
            brandName = "1 Free Coffee",
            description = "One any-size Starbucks beverage",
            pointsCost = 150,
            expiryDate = "Valid until September 2026",
            terms = "Excludes seasonal limited items.",
            qrCodePlaceholder = "QR-SB-150"
        ),
        Reward(
            id = "reward_vapiano",
            brandName = "Pay 1 take 2 Vapiano",
            description = "Buy one get one free pasta",
            pointsCost = 100,
            expiryDate = "Valid until October 2026",
            terms = "Valid on weekdays only.",
            qrCodePlaceholder = "QR-VAP-100"
        )
    )

    fun sampleUser(): UserProfile = UserProfile(
        id = "user_xuetong",
        displayName = "Xuetong Fu",
        email = "fxuetong@bu.edu",
        avatarUrl = null,
        levelLabel = "Gold Level",
        totalPoints = 300,
        tierPointsGoal = 500,
        location = "Boston, MA",
        countryRegion = "China",
        history = sampleHistory(),
        redemptions = listOf(
            RewardRedemption(
                rewardId = "reward_kfc",
                redeemedAt = "23/01/23",
                status = RedemptionStatus.REDEEMED
            )
        )
    )
}

private fun MockSurveyPayload.toSurvey(id: String): Survey {
    return Survey(
        id = id,
        title = title,
        description = description,
        latitude = latitude,
        longitude = longitude,
        points = points,
        questions = questions.mapIndexed { index, question ->
            SurveyQuestion(
                id = question.id,
                type = question.type,
                content = question.content,
                required = question.required,
                options = question.options?.map { option ->
                    SurveyOption(
                        content = option.content,
                        label = option.label
                    )
                }
            )
        }
    )
}
