package com.qzone.data.placeholder

import com.qzone.data.model.*

object PlaceholderDataSource {

    fun sampleSurveys(): List<Survey> = listOf(
        Survey(
            id = "survey_city_park",
            title = "City Park Feedback",
            subtitle = "Share your thoughts about Boston Common",
            category = SurveyCategory.EXPERIENCE,
            locationLabel = "Boston Common",
            latitude = 42.3551,
            longitude = -71.0656,
            points = 30,
            distanceMeters = 320,
            estimatedMinutes = 4,
            questions = listOf(
                SurveyQuestion(
                    id = "q1",
                    prompt = "How satisfied are you with the cleanliness of Boston Common?",
                    type = SurveyQuestionType.SINGLE_CHOICE,
                    options = listOf("Satisfied", "Neutral", "Dissatisfied")
                ),
                SurveyQuestion(
                    id = "q2",
                    prompt = "What could we improve during your next visit?",
                    type = SurveyQuestionType.SHORT_TEXT,
                    helperText = "Type your answer"
                ),
                SurveyQuestion(
                    id = "q3",
                    prompt = "Rate the park amenities",
                    type = SurveyQuestionType.RATING,
                    options = listOf("1", "2", "3", "4", "5")
                )
            )
        ),
        Survey(
            id = "survey_bu_transport",
            title = "BU Student Transportation",
            subtitle = "Tell us how you commute",
            category = SurveyCategory.TRANSPORT,
            locationLabel = "BU Central",
            latitude = 42.3505,
            longitude = -71.1054,
            points = 25,
            estimatedMinutes = 5,
            questions = listOf(
                SurveyQuestion(
                    id = "q1",
                    prompt = "Primary commute mode",
                    type = SurveyQuestionType.SINGLE_CHOICE,
                    options = listOf("Bus", "Train", "Bike", "Walk", "Rideshare")
                ),
                SurveyQuestion(
                    id = "q2",
                    prompt = "Select issues you've faced recently",
                    type = SurveyQuestionType.MULTI_CHOICE,
                    options = listOf("Delays", "Crowding", "Cost", "Accessibility", "Other")
                ),
                SurveyQuestion(
                    id = "q3",
                    prompt = "Describe your ideal commute",
                    type = SurveyQuestionType.SHORT_TEXT,
                    helperText = "Optional"
                )
            )
        ),
        Survey(
            id = "survey_north_end_food",
            title = "North End Italian Food Review",
            subtitle = "Help us rank local classics",
            category = SurveyCategory.FOOD,
            locationLabel = "North End",
            latitude = 42.3647,
            longitude = -71.0542,
            points = 25,
            estimatedMinutes = 3,
            questions = listOf(
                SurveyQuestion(
                    id = "q1",
                    prompt = "Rate your latest dining experience",
                    type = SurveyQuestionType.RATING,
                    options = (1..5).map(Int::toString)
                ),
                SurveyQuestion(
                    id = "q2",
                    prompt = "What dish would you recommend?",
                    type = SurveyQuestionType.SHORT_TEXT
                )
            )
        ),
        Survey(
            id = "survey_marathon",
            title = "Boston Marathon Experience",
            subtitle = "Tell us about race day",
            category = SurveyCategory.EVENT,
            locationLabel = "Newbury St",
            latitude = 42.3503,
            longitude = -71.0810,
            points = 15,
            estimatedMinutes = 6,
            questions = listOf(
                SurveyQuestion(
                    id = "q1",
                    prompt = "Were you a runner, spectator, or volunteer?",
                    type = SurveyQuestionType.SINGLE_CHOICE,
                    options = listOf("Runner", "Spectator", "Volunteer")
                ),
                SurveyQuestion(
                    id = "q2",
                    prompt = "Would you participate again?",
                    type = SurveyQuestionType.SINGLE_CHOICE,
                    options = listOf("Yes", "Maybe", "No")
                )
            )
        )
    )

    fun sampleHistory(): List<SurveyHistoryItem> = listOf(
        SurveyHistoryItem(
            id = "history_1",
            surveyId = "survey_city_park",
            title = "City Park Feedback",
            completedAt = "23/01/23",
            pointsEarned = 30,
            locationLabel = "Boston, MA"
        ),
        SurveyHistoryItem(
            id = "history_2",
            surveyId = "survey_bu_transport",
            title = "BU Student Transportation",
            completedAt = "23/01/23",
            pointsEarned = 25,
            locationLabel = "Boston, MA"
        ),
        SurveyHistoryItem(
            id = "history_3",
            surveyId = "survey_north_end_food",
            title = "North End Italian Food Review",
            completedAt = "23/01/23",
            pointsEarned = 25,
            locationLabel = "Boston, MA"
        )
    )

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
