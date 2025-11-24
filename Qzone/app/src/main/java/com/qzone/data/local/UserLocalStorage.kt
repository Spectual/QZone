package com.qzone.data.local

import android.content.Context
import android.content.SharedPreferences
import com.qzone.data.network.model.NetworkUserProfile
import java.io.File

object UserLocalStorage {
    private const val PREF_NAME = "qzone_user_storage"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_AVATAR_LOCAL = "avatar_local_path"
    private const val KEY_POINTS = "points"
    private const val KEY_RANK = "rank"
    private const val KEY_POINTS_TO_NEXT = "points_to_next"
    private const val KEY_CREATED = "created_at"
    private const val KEY_UPDATED = "updated_at"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    val ctx = context.applicationContext
                    prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    appContext = ctx
                }
            }
        }
    }

    fun save(profile: NetworkUserProfile) {
        val id = profile.documentId.takeIf { it.isNotBlank() } ?: return
        val editor = prefs()?.edit() ?: return
        editor.putString(KEY_ID, id)
        editor.putString(KEY_NAME, profile.userName)
        editor.putString(KEY_EMAIL, profile.email)
        editor.putString(KEY_AVATAR, profile.avatarUrl)
        editor.putInt(KEY_POINTS, profile.currentPoints)
        editor.putString(KEY_RANK, profile.rank)
        editor.putInt(KEY_POINTS_TO_NEXT, profile.pointsToNextRank)
        editor.putString(KEY_CREATED, profile.createTime)
        editor.putString(KEY_UPDATED, profile.updateTime)
        editor.apply()
    }

    fun load(): NetworkUserProfile? {
        val preferences = prefs() ?: return null
        val id = preferences.getString(KEY_ID, null) ?: return null
        return NetworkUserProfile(
            documentId = id,
            userName = preferences.getString(KEY_NAME, null),
            email = preferences.getString(KEY_EMAIL, null),
            avatarUrl = preferences.getString(KEY_AVATAR, null),
            currentPoints = preferences.getInt(KEY_POINTS, 0),
            rank = preferences.getString(KEY_RANK, null),
            pointsToNextRank = preferences.getInt(KEY_POINTS_TO_NEXT, 0),
            createTime = preferences.getString(KEY_CREATED, null),
            updateTime = preferences.getString(KEY_UPDATED, null)
        )
    }

    fun getAvatarLocalPath(): String? {
        val path = prefs()?.getString(KEY_AVATAR_LOCAL, null) ?: return null
        val file = File(path)
        return if (file.exists()) path else null
    }

    fun saveAvatarLocal(bytes: ByteArray, fileName: String): String? {
        val context = appContext ?: return null
        return try {
            val file = File(context.filesDir, fileName)
            file.outputStream().use { it.write(bytes) }
            prefs()?.edit()?.putString(KEY_AVATAR_LOCAL, file.absolutePath)?.apply()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun clearAvatarLocal() {
        prefs()?.edit()?.remove(KEY_AVATAR_LOCAL)?.apply()
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        val editor = prefs()?.edit() ?: return
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    fun loadTokens(): StoredTokens? {
        val preferences = prefs() ?: return null
        val access = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return StoredTokens(access, refresh)
    }

    fun clear() {
        prefs()?.edit()?.clear()?.apply()
    }

    private fun prefs(): SharedPreferences? {
        return prefs
    }

    data class StoredTokens(
        val accessToken: String,
        val refreshToken: String
    )
}
