package com.example.matchit.data.remote.client

import com.example.matchit.data.remote.Errors.UserIsLoggedOutError
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Provides interface for HTTP clients creation.
 */
class RequestClientFactory @Inject constructor(private val loginSessionManager: LoginSessionManager)
{

    /**
     * Creates and configures a Retrofit client with authentication token.
     *
     * @param baseUrl The base URL for the API.
     * @param connectionTimeout Maximal connection time in seconds, default is set to 10 s.
     * @return A configured Retrofit client for making authenticated API requests.
     */
    suspend fun createAuthenticatedClient(baseUrl: String, lg: LoginSessionManager? = null, gsonParser: Gson = Gson(), connectionTimeout: Long = 30L) : Retrofit
    {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
            .writeTimeout(connectionTimeout, TimeUnit.SECONDS)
            .readTimeout(connectionTimeout, TimeUnit.SECONDS)

        val authenticationToken = loginSessionManager.getAuthToken() ?: throw UserIsLoggedOutError()

        httpClient.addInterceptor(AuthenticationInterceptor(authenticationToken))

        val client = httpClient.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gsonParser))
            .client(client)
            .build();

        return retrofit;
    }


    /**
     * Creates and configures a Retrofit client.
     *
     * @param baseUrl The base URL for the API.
     * @return A configured Retrofit client for making unauthenticated API requests.
     */
    fun createUnauthenticatedClient(baseUrl: String) : Retrofit
    {
        val gson = GsonBuilder().setLenient().create()

        val httpClient = OkHttpClient.Builder()
        val client = httpClient.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build();

        return retrofit;
    }
}