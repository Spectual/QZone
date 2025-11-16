package com.qzone.data.placeholder

data class MockSurveyPayload(
    val id : String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val points: Int = 0,
    val questions: List<MockSurveyQuestion>
)

data class MockSurveyQuestion(
    val id: String,
    val type: String,
    val content: String,
    val required: Boolean,
    val options: List<MockSurveyOption>?
)

data class MockSurveyOption(
    val content: String,
    val label: String
)

object MockSurveyPayloadFactory {
    fun englishCampusDiningSurvey(): MockSurveyPayload {
        return MockSurveyPayload(
            id = "gshRSaqwS3EHOjvqZmKs",
            title = "Campus Dining Satisfaction Survey",
            description = "Share your thoughts on the campus dining experience.",
            latitude = 42.3505,
            longitude = -71.1054,
            points = 10,
            questions = listOf(
                MockSurveyQuestion(
                    id = "q8iEr7xgQ6D9WB2kSjlC",
                    type = "single",
                    content = "How satisfied are you with the overall campus dining experience?",
                    required = true,
                    options = listOf(
                        MockSurveyOption(content = "Very satisfied", label = "A"),
                        MockSurveyOption(content = "Satisfied", label = "B"),
                        MockSurveyOption(content = "Neutral", label = "C"),
                        MockSurveyOption(content = "Dissatisfied", label = "D")
                    )
                ),
                MockSurveyQuestion(
                    id = "BkME6VQnkypkERe56qpa",
                    type = "multiple",
                    content = "Which aspects need improvement? (Select all that apply)",
                    required = false,
                    options = listOf(
                        MockSurveyOption(content = "Food taste", label = "A"),
                        MockSurveyOption(content = "Menu variety", label = "B"),
                        MockSurveyOption(content = "Pricing", label = "C"),
                        MockSurveyOption(content = "Service speed", label = "D")
                    )
                ),
                MockSurveyQuestion(
                    id = "GICxxXWpDq4E6tzOhvz5",
                    type = "text",
                    content = "What suggestions do you have for the dining services?",
                    required = false,
                    options = null
                )
            )
        )
    }
}

