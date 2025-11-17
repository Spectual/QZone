package com.qzone.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qzone.data.database.entity.NearbyLocationEntity
import com.qzone.data.database.entity.SurveyEntity
import com.qzone.data.database.entity.SurveyOptionEntity
import com.qzone.data.database.entity.SurveyQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<SurveyEntity>)

    @Update
    suspend fun updateSurvey(survey: SurveyEntity)

    @Delete
    suspend fun deleteSurvey(survey: SurveyEntity)

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getSurveyById(id: String): SurveyEntity?

    @Query("SELECT * FROM surveys ORDER BY syncedAt DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE isCompleted = 0 ORDER BY syncedAt DESC")
    fun getUncompletedSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng ORDER BY syncedAt DESC")
    suspend fun getSurveysByLocation(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<SurveyEntity>

    @Query("UPDATE surveys SET isCompleted = 1 WHERE id = :id")
    suspend fun markSurveyAsCompleted(id: String)

    @Query("DELETE FROM surveys")
    suspend fun deleteAllSurveys()

    @Query("SELECT COUNT(*) FROM surveys")
    suspend fun getSurveyCount(): Int
}

@Dao
interface SurveyQuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: SurveyQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<SurveyQuestionEntity>)

    @Query("SELECT * FROM survey_questions WHERE surveyId = :surveyId ORDER BY questionIndex")
    suspend fun getQuestionsBySurveyId(surveyId: String): List<SurveyQuestionEntity>

    @Query("DELETE FROM survey_questions WHERE surveyId = :surveyId")
    suspend fun deleteQuestionsBySurveyId(surveyId: String)
}

@Dao
interface SurveyOptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: SurveyOptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<SurveyOptionEntity>)

    @Query("SELECT * FROM survey_options WHERE questionId = :questionId")
    suspend fun getOptionsByQuestionId(questionId: String): List<SurveyOptionEntity>

    @Query("DELETE FROM survey_options WHERE questionId = :questionId")
    suspend fun deleteOptionsByQuestionId(questionId: String)
}

@Dao
interface NearbyLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: NearbyLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<NearbyLocationEntity>)

    @Update
    suspend fun updateLocation(location: NearbyLocationEntity)

    @Delete
    suspend fun deleteLocation(location: NearbyLocationEntity)

    @Query("SELECT * FROM nearby_locations WHERE documentId = :documentId")
    suspend fun getLocationById(documentId: String): NearbyLocationEntity?

    @Query("SELECT * FROM nearby_locations ORDER BY distance ASC, syncedAt DESC")
    fun getAllLocations(): Flow<List<NearbyLocationEntity>>

    @Query("SELECT * FROM nearby_locations WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng ORDER BY distance ASC, syncedAt DESC")
    suspend fun getLocationsByBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<NearbyLocationEntity>

    @Query("DELETE FROM nearby_locations")
    suspend fun deleteAllLocations()

    @Query("SELECT COUNT(*) FROM nearby_locations")
    suspend fun getLocationCount(): Int
}
