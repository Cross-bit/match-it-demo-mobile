package com.example.matchit.data.userAccount.DataSources

import com.example.matchit.BuildConfig
import com.example.matchit.data.remote.api.AccountApiService
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AccountApi.ProfilePicApiResponse
import com.example.matchit.data.remote.model.SimpleApiResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class UserAccountDataSource @Inject constructor(
    private val authenticatedApiCaller: AuthenticatedApiCaller
)
{
    suspend fun uploadUserProfilePicture(inputStream: InputStream, fileNameWithExtension: String, fileExtension: String) : Resource<ProfilePicApiResponse, ApiError<*>>
    {
        return try {

            val mediaType: String = "image/$fileExtension"
            val requestBody = inputStream.readBytes().toRequestBody(mediaType.toMediaTypeOrNull())
            val profilePic = MultipartBody.Part.createFormData("profilePic", fileNameWithExtension, requestBody)

            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL) {
                it.create(AccountApiService::class.java).uploadUserProfilePicture(profilePic)
            }

        } catch (e: Throwable) {
            Resource.GeneralError("Error while uploading users profile picture", IOException("Error reject session connection", e))
        }
    }

    suspend fun getUserProfilePicture() : Resource<ProfilePicApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL) {
                it.create(AccountApiService::class.java).getUserProfilePicture()
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }

    suspend fun deleteUserProfilePicture() : Resource<SimpleApiResponse, ApiError<*>> {
        return try {
            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.ACCOUNTS_API_URL) {
                it.create(AccountApiService::class.java).deleteUser()
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }
}
