package com.qzone.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion
import com.qzone.data.model.SurveyOption
import com.qzone.data.model.SurveyStatus

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val points: Int = 0,
    val isCompleted: Boolean = false,
    val status: String = SurveyStatus.EMPTY.name,
    val currentQuestionIndex: Int = 0,
    val questionCount: Int = 0,
    val answersJson: String = "", // Store answers as JSON
    val questionsJson: String = "", // Store questions as JSON
    val syncedAt: Long = System.currentTimeMillis()
) {
    fun toSurvey(questions: List<SurveyQuestion> = emptyList()): Survey {
        val surveyStatus = try {
            SurveyStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            if (isCompleted) SurveyStatus.COMPLETE else SurveyStatus.EMPTY
        }

        return Survey(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
            points = points,
            questions = questions,
            questionCount = if (questionCount > 0) questionCount else questions.size,
            isCompleted = isCompleted,
            status = surveyStatus,
            currentQuestionIndex = currentQuestionIndex,
            // Simple JSON parsing for answers map - in a real app use Gson/Moshi
            answers = if (answersJson.isNotEmpty()) {
                try {
                    // Very basic parsing for "key:val1,val2|key2:val3" format or similar
                    // For now, let's assume empty map if we don't have a proper parser here
                    // Or better, let's just use an empty map for now and rely on the Repository to handle it if needed
                    // But wait, we need to persist it.
                    // Let's use a simple convention: key=val1,val2;key2=val3
                    answersJson.split(";").filter { it.isNotEmpty() }.associate {
                        val parts = it.split("=")
                        if (parts.size == 2) {
                            parts[0] to parts[1].split(",")
                        } else {
                            "" to emptyList()
                        }
                    }.filterKeys { k -> k.isNotEmpty() }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        )
    }

    companion object {
        fun fromSurvey(survey: Survey): SurveyEntity {
            return SurveyEntity(
                id = survey.id,
                title = survey.title,
                description = survey.description,
                imageUrl = survey.imageUrl,
                latitude = survey.latitude,
                longitude = survey.longitude,
                points = survey.points,
                isCompleted = survey.isCompleted,
                status = survey.status.name,
                currentQuestionIndex = survey.currentQuestionIndex,
                questionCount = if (survey.questionCount > 0) survey.questionCount else survey.questions.size,
                answersJson = survey.answers.entries.joinToString(";") { (k, v) -> "$k=${v.joinToString(",")}" },
                questionsJson = "", // Will be handled by QuestionEntity
                syncedAt = System.currentTimeMillis()
            )
        }
    }
}

@Entity(
    tableName = "survey_questions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SurveyEntity::class,
            parentColumns = ["id"],
            childColumns = ["surveyId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["surveyId"])
    ]
)
data class SurveyQuestionEntity(
    @PrimaryKey
    val id: String,
    val surveyId: String,
    val type: String,
    val content: String,
    val required: Boolean,
    val optionsJson: String = "", // Store options as JSON
    val questionIndex: Int = 0
) {
    fun toSurveyQuestion(options: List<SurveyOption> = emptyList()): SurveyQuestion {
        return SurveyQuestion(
            id = id,
            type = type,
            content = content,
            required = required,
            options = options
        )
    }

    companion object {
        fun fromSurveyQuestion(surveyId: String, question: SurveyQuestion, index: Int): SurveyQuestionEntity {
            return SurveyQuestionEntity(
                id = question.id,
                surveyId = surveyId,
                type = question.type,
                content = question.content,
                required = question.required,
                optionsJson = "", // Will be handled by OptionEntity
                questionIndex = index
            )
        }
    }
}

@Entity(
    tableName = "survey_options",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SurveyQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["questionId"])
    ]
)
data class SurveyOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: String,
    val content: String,
    val label: String
) {
    fun toSurveyOption(): SurveyOption {
        return SurveyOption(
            content = content,
            label = label
        )
    }

    companion object {
        fun fromSurveyOption(questionId: String, option: SurveyOption): SurveyOptionEntity {
            return SurveyOptionEntity(
                questionId = questionId,
                content = option.content,
                label = option.label
            )
        }
    }
}

@Entity(tableName = "nearby_locations")
data class NearbyLocationEntity(
    @PrimaryKey
    val documentId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null,
    val syncedAt: Long = System.currentTimeMillis()
) {
    fun toNearbyLocation(): com.qzone.data.model.NearbyLocation {
        return com.qzone.data.model.NearbyLocation(
            documentId = documentId,
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            distance = distance
        )
    }

    companion object {
        fun fromNearbyLocation(location: com.qzone.data.model.NearbyLocation): NearbyLocationEntity {
            return NearbyLocationEntity(
                documentId = location.documentId,
                title = location.title,
                description = location.description.orEmpty(),
                latitude = location.latitude,
                longitude = location.longitude,
                distance = location.distance,
                syncedAt = System.currentTimeMillis()
            )
        }
    }
}
