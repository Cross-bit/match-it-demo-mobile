package com.example.matchit.data.userAuthentication.DataSources

import com.example.matchit.BuildConfig
import com.example.matchit.data.model.authentication.GoogleAuthRequest
import com.example.matchit.data.model.authentication.UserRegistrationData
import com.example.matchit.data.remote.api.AuthenticationApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.RequestClientFactory
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.client.RetrofitSafeApiCaller
import com.example.matchit.data.remote.model.AuthenticationApi.UserRegistrationApiResponse
import com.google.gson.reflect.TypeToken
import java.io.IOException
import javax.inject.Inject

class RegistrationDataSource @Inject constructor(
    private val factory: RequestClientFactory,
    private val apiCaller: RetrofitSafeApiCaller
) {

    suspend fun signupCredentials(registrationData: UserRegistrationData): Resource<UserRegistrationApiResponse,  ApiError<List<ValidationErrorDetail>>> {
        return try {
            val retrofit = factory.createUnauthenticatedClient(BuildConfig.ACCOUNTS_API_URL);
            val apiService = retrofit.create(AuthenticationApiService::class.java)

            val errorType = object :
                TypeToken<ApiError<List<ValidationErrorDetail>>>() {}.type

            apiCaller.safeApiCall<
                    UserRegistrationApiResponse,
                    List<ValidationErrorDetail>
                    >(errorType) {
                apiService.signupUser(registrationData)
            }

            //apiCaller.safeApiCall<UserRegistrationApiResponse,  List<ValidationErrorDetail>> { apiService.signupUser(registrationData) }

        } catch (e: Throwable) {
            Resource.GeneralError("Error while signing up with credentials", IOException("Error signing up", e))
        }
    }

    suspend fun signupGoogleAuthentication(registrationData: GoogleAuthRequest): Resource<UserRegistrationApiResponse, ApiError<*>> {
        return try {
            val retrofit = factory.createUnauthenticatedClient(BuildConfig.ACCOUNTS_API_URL);
            val apiService = retrofit.create(AuthenticationApiService::class.java)

            apiCaller.safeApiCall<UserRegistrationApiResponse,  Any> { apiService.signupGoogleUser(registrationData) }

        } catch (e: Throwable) {
            Resource.GeneralError("Error while signing up with credentials", IOException("Error signing up", e))
        }
    }
}