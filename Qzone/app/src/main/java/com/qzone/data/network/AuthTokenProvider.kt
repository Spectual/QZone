package com.qzone.data.network

/**
 * Holds the latest access token for authenticated API calls.
 * Updated by repositories after login/register and cleared on sign out.
 */
object AuthTokenProvider {
    @Volatile
    var accessToken: String? = null
}

