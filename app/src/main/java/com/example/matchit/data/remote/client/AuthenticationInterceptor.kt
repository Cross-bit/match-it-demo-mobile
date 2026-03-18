package com.example.matchit.data.remote.client

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds provided authorisation access token to the HTTP header.
 */
class AuthenticationInterceptor(
    private val authToken: String

) : Interceptor {
     override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request();

        // Add the authentication header to the request
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")
            .build();

        return chain.proceed(newRequest);
    }
}