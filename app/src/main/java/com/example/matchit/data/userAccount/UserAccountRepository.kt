package com.example.matchit.data.userAccount

import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.client.mapToBooleanResource
import com.example.matchit.data.userAccount.DataSources.UserAccountDataSource
import com.example.matchit.data.userAuthentication.DataSources.LocalSessionCleanup
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import javax.inject.Inject

class UserAccountRepository @Inject constructor(
    private var dbDataSource: UserDataLocalDataSource,
    private val dataSource: UserAccountDataSource,
    private val localSessionCleanup: LocalSessionCleanup
) {

    suspend fun getUserProfileData() : LoggedInUser? {
        val currentUser = dbDataSource.getCurrentUser()

        return currentUser?.let {
            LoggedInUser(it.displayName, it.email, it.uuid)
        }
    }

    /**
     * Uploads profile picture from provided [inputStream] to the server with provided [fileName] and appropriate [fileExtension].
     */
    suspend fun uploadUserProfilePic(inputStream: InputStream, fileName: String, fileExtension: String) : Resource<String, ApiError<*>> {
        val uploadResult = dataSource.uploadUserProfilePicture(inputStream, fileName, fileExtension)

        return when(uploadResult) {
            is Resource.Success -> {
                val picUrl = uploadResult.data.url
                dbDataSource.updateProfilePicture(picUrl)
                Resource.Success(picUrl)
            }
            is Resource.GeneralError -> Resource.GeneralError(uploadResult.message, uploadResult.throwable)
            is Resource.Error -> Resource.Error(uploadResult.errorMessage, uploadResult.error)
        }
    }

    /**
     * Tries to get user profile picture url
     */
    suspend fun getUserProfilePictureUrl() : Resource<String, ApiError<*>> {
        return try {

            val userData = dbDataSource.getCurrentUser()

            return userData?.let {
                if (userData.profilePic.isNotEmpty())
                    return Resource.Success<String, ApiError<*>>(userData.profilePic)

                // if empty -> we try to load from API (besteffort)
                return when (val apiUserProfilePic = dataSource.getUserProfilePicture()){
                    is Resource.Success -> Resource.Success(apiUserProfilePic.data.url)
                    is Resource.GeneralError -> Resource.GeneralError(apiUserProfilePic.message, apiUserProfilePic.throwable)
                    is Resource.Error -> Resource.Error(apiUserProfilePic.errorMessage, apiUserProfilePic.error)
                }

            } ?:
                Resource.GeneralError(
                    "Missing user data in local database!",
                    Exception("Missing user data")
                )

        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }

    /**
     * Requests user profile deletion.
     */
    suspend fun deleteUserProfile() : Resource<Boolean, ApiError<*>> {
        return try {
            val apiUserProfilePic = dataSource.deleteUserProfilePicture()
            val result = apiUserProfilePic.mapToBooleanResource()

            if (result is Resource.Success && result.data) {
                // if success ==> all clear local user data (logout)
                localSessionCleanup.clear()

                Resource.Success(true)
            } else {
                Resource.Success(false)
            }

        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }
}