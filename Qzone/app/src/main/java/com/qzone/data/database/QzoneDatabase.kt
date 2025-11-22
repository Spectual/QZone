package com.qzone.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.qzone.data.database.dao.NearbyLocationDao
import com.qzone.data.database.dao.SurveyDao
import com.qzone.data.database.dao.SurveyOptionDao
import com.qzone.data.database.dao.SurveyQuestionDao
import com.qzone.data.database.entity.NearbyLocationEntity
import com.qzone.data.database.entity.SurveyEntity
import com.qzone.data.database.entity.SurveyOptionEntity
import com.qzone.data.database.entity.SurveyQuestionEntity

@Database(
    entities = [
        SurveyEntity::class,
        SurveyQuestionEntity::class,
        SurveyOptionEntity::class,
        NearbyLocationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QzoneDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun surveyQuestionDao(): SurveyQuestionDao
    abstract fun surveyOptionDao(): SurveyOptionDao
    abstract fun nearbyLocationDao(): NearbyLocationDao

    companion object {
        @Volatile
        private var instance: QzoneDatabase? = null

        fun getInstance(context: Context): QzoneDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QzoneDatabase::class.java,
                    "qzone_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
