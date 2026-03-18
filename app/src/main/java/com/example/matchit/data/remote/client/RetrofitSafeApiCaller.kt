package com.example.matchit.data.remote.client

import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.api.AccountApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Inject


/**
 * Manages safe execution API calls. Every error is properly wrapped and parsed to the Resource class.
 */
class RetrofitSafeApiCaller @Inject constructor(
    private val factory: RequestClientFactory,
) {

    suspend fun <T : Any> safeApiCall(
        apiBaseUrl: String,
        authenticated: Boolean = true,
        apiToBeCalled: suspend (retrofitClient: Retrofit) -> Response<T>
    ): Resource<T, ApiError<*>> = withContext(Dispatchers.IO) {
        try {
            val retrofit = if (authenticated) {
                factory.createAuthenticatedClient(apiBaseUrl)
            } else {
                factory.createUnauthenticatedClient(apiBaseUrl)
            }

            val response = apiToBeCalled(retrofit)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error(
                        errorMessage = "Response body is null",
                        ApiError<Unit>(
                            name = "EMPTY_BODY",
                            message = "Response body is null",
                            status = 500,
                            errors = null
                        )
                    )
                }
            } else {
                val type = object : TypeToken<ApiError<T>>() {}.type
                val error = convertErrorBody<Any>(response.errorBody(), type)
                    Resource.Error(
                            errorMessage = "Server returned ${response.code()}",
                            ApiError<Unit>(
                            name = "UNKNOWN",
                            message = "Server returned ${response.code()}",
                            status = response.code(),
                            errors = null
                    )
                )
            }
        } catch (e: IOException) {
            Resource.GeneralError("Problem with network connection", e)
        } catch (e: Exception) {
            Resource.GeneralError("Something went wrong", e)
        }
    }
    /**
     * Performs a safe API call by executing the provided API call function in a suspend context.
     * This function is responsible for making API calls in a safe manner and handling the response.
     *
     * @param apiToBeCalled The suspend function representing the API call.
     * @return A Resource object representing the result of the API call (Success, Error, GeneralError).
     */
    suspend fun <T: Any, E: Any> safeApiCall(
        errorType: Type? = null,
        apiToBeCalled: suspend () -> Response<T>
    ): Resource<T, ApiError<E>> =
        withContext(Dispatchers.IO) {
            try {
                val response: Response<T> = apiToBeCalled()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Resource.Success(body)
                    } else {
                        Resource.Error(
                            errorMessage = "Response body is null",
                            ApiError(
                                name = "EMPTY_BODY",
                                message = "Response body is null",
                                status = 500,
                                errors = null
                            )
                        )
                    }
                } else {
                    val error = convertErrorBody<E>(
                        response.errorBody(),
                        errorType
                    )

                    Resource.Error(
                        errorMessage = "Server returned ${response.code()}",
                        error ?: ApiError(
                            name = "UNKNOWN",
                            message = "Server returned ${response.code()}",
                            status = response.code(),
                            errors = null
                        )
                    )
                }
            } catch (e: IOException) {
                Resource.GeneralError("Problem with network connection", e)
            } catch (e: Exception) {
                Resource.GeneralError("Something went wrong", e)
            }
        }
    /**
     * Converts the error response body into an ApiError object of the specified error type.
     * Parses and maps the error response body to an ApiError class.
     *
     * @param errorBody The response body containing error information.
     * @return An ApiError object representing the parsed error information or null if parsing fails.
     */
    private fun <T> convertErrorBody(
        errorBody: ResponseBody?,
        type: Type? = null
    ): ApiError<T>? {
        val gson = Gson()
        return try {
            errorBody?.charStream()?.let {
                val realType = type ?: object : TypeToken<ApiError<T>>() {}.type
                gson.fromJson<ApiError<T>>(it, realType)
            }
        } catch (exception: Exception) {
            null
        }
    }
}