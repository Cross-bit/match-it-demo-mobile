package com.example.matchit.ui.userPreferences

import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAccount.UserAccountRepository
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.ui.userPreferences.models.ProfileDetails
import com.example.matchit.ui.userPreferences.models.ProfilePicUpdateModel
import com.example.matchit.ui.userPreferences.models.ProfilePicUploadModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userAccountRepository: UserAccountRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _profilePicUploadResult = MutableLiveData<ProfilePicUploadModel>()
    val profilePicUploadResult: LiveData<ProfilePicUploadModel> = _profilePicUploadResult

    private val _profilePicLoadResult = MutableLiveData<ProfilePicUpdateModel>()
    val profilePicLoadResult: LiveData<ProfilePicUpdateModel> = _profilePicLoadResult

    private val _accountDetails = MutableStateFlow<ProfileDetails>(ProfileDetails("", ""))
    val accountDetails = _accountDetails.asStateFlow()

    private val _accountDeletionRes = MutableLiveData<AccountDeleteRes>()
    val accountDeletionRes: LiveData<AccountDeleteRes> = _accountDeletionRes

    val validProfilePictureFormats = listOf("image/jpeg", "image/png", "image/webp")

    init {
        this.loadUserProfileData()
    }

    fun isValidImageMimeType(mimeType: String, expectedFormats: List<String>): Boolean {
        if (mimeType.startsWith("image/")) {
            if (expectedFormats.contains(mimeType)) {
                return true
            }
        }
        return false
    }

    /**
     * Uploads users selected profile image to the server. Image is read through [imageStream],
     * for proper processing we also need [fileMime] and fileName.
     */
    fun uploadProfilePicture(imageStream: InputStream, fileMime: String, fileName: String) {

        if (!isValidImageMimeType(fileMime, validProfilePictureFormats)) {
            _profilePicUploadResult.value = ProfilePicUploadModel(errorMessage = R.string.invalid_file_type_error);
            return;
        }

        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(fileMime)

        val fileNameCleared = fileName.substringBeforeLast(".")

        if (fileNameCleared.isEmpty() || fileExtension.isNullOrEmpty()){
            _profilePicUploadResult.value = ProfilePicUploadModel(errorMessage = R.string.invalid_file_type_error);
            return;
        }

        viewModelScope.launch {
            when (userAccountRepository.uploadUserProfilePic(imageStream, fileNameCleared, fileExtension)) {
                is Resource.Success -> {
                     // load the new url from the repository
                    _profilePicUploadResult.value = ProfilePicUploadModel(resultMessage = R.string.profile_picture_upload_success);
                    updateProfilePicture()
                }
                else -> {
                    _profilePicUploadResult.value = ProfilePicUploadModel(errorMessage = R.string.upload_failed_something_error);
                }
            }
        }
    }

    fun loadUserProfileData() {
        viewModelScope.launch {
            userAccountRepository.getUserProfileData()?.let {
                _accountDetails.value = ProfileDetails(it.displayName, it.email)
            } ?: run {
                _accountDetails.value = ProfileDetails("", "", R.string.user_info_could_not_be_loaded)
            }
        }
    }

    fun updateProfilePicture() {
        viewModelScope.launch {
            when (val url = userAccountRepository.getUserProfilePictureUrl()){
                is Resource.Success -> {
                    if (url.data.isEmpty()) {
                        _profilePicLoadResult.value = ProfilePicUpdateModel(defaultImage = R.drawable.ic_friends_icon)
                    }
                    else
                        _profilePicLoadResult.value = ProfilePicUpdateModel(url.data)
                }
                else -> {
                    _profilePicLoadResult.value = ProfilePicUpdateModel(errorMessage = R.string.profile_picture_update_failed)
                }
            }
        }
    }

    fun deleteUserProfile() {
        viewModelScope.launch {
            when (val res = userAccountRepository.deleteUserProfile()) {
                is Resource.Success -> {
                    if (res.data) {
                        loginRepository.logout()
                        _accountDeletionRes.value = AccountDeleteRes()
                    }
                    else
                        _accountDeletionRes.value = AccountDeleteRes(R.string.account_delete_error_normal)
                }
                else -> {
                    _accountDeletionRes.value = AccountDeleteRes(R.string.account_delete_error)
                }
            }
        }
    }
}