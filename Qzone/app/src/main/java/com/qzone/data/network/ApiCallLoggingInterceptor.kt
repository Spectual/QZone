package com.qzone.data.network

import com.qzone.util.QLog
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.system.measureTimeMillis

/**
 * Logs high-level API call metadata (method, url, status, latency) so we can trace
 * network behaviour without dumping full payloads.
 */
class ApiCallLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = System.nanoTime()
        QLog.i(TAG) { "API[$requestId] -> ${request.method} ${request.url}" }

        return try {
            var response: Response
            val duration = measureTimeMillis {
                response = chain.proceed(request)
            }
            QLog.i(TAG) {
                "API[$requestId] <- ${response.code} ${request.method} ${request.url} (${duration}ms)"
            }
            response
        } catch (t: Throwable) {
            QLog.e(TAG, t) { "API[$requestId] xx ${request.method} ${request.url}: ${t.message}" }
            throw t
        }
    }

    companion object {
        private const val TAG = "ApiCall"
    }
}

