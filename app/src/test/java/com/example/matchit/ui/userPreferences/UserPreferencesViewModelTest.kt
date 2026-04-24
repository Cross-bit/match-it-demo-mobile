package com.example.matchit.ui.userPreferences

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.R
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAccount.UserAccountRepository
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val userAccountRepository: UserAccountRepository = mockk()
    private val loginRepository: LoginRepository = mockk()

    private fun createViewModelWithUserData(): UserPreferencesViewModel {
        coEvery { userAccountRepository.getUserProfileData() } returns LoggedInUser("John", "j@mail.com", "u1")
        return UserPreferencesViewModel(userAccountRepository, loginRepository)
    }

    @Test
    fun isValidImageMimeType_returns_true_for_allowed_type() {
        val vm = createViewModelWithUserData()
        assertTrue(vm.isValidImageMimeType("image/png", vm.validProfilePictureFormats))
    }

    @Test
    fun isValidImageMimeType_returns_false_for_non_image_type() {
        val vm = createViewModelWithUserData()
        assertFalse(vm.isValidImageMimeType("text/plain", vm.validProfilePictureFormats))
    }

    @Test
    fun isValidImageMimeType_returns_false_for_disallowed_image_type() {
        val vm = createViewModelWithUserData()
        assertFalse(vm.isValidImageMimeType("image/gif", vm.validProfilePictureFormats))
    }

    @Test
    fun loadUserProfileData_sets_profile_details_when_data_exists() = runTest {
        val vm = createViewModelWithUserData()
        vm.loadUserProfileData()
        advanceUntilIdle()

        assertEquals("John", vm.accountDetails.value.userName)
        assertEquals("j@mail.com", vm.accountDetails.value.userEmail)
    }

    @Test
    fun loadUserProfileData_sets_error_state_when_data_missing() = runTest {
        coEvery { userAccountRepository.getUserProfileData() } returns null
        val vm = UserPreferencesViewModel(userAccountRepository, loginRepository)
        advanceUntilIdle()

        assertEquals(R.string.user_info_could_not_be_loaded, vm.accountDetails.value.error)
    }

    @Test
    fun updateProfilePicture_sets_default_image_when_url_empty() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.getUserProfilePictureUrl() } returns Resource.Success("")

        vm.updateProfilePicture()
        advanceUntilIdle()

        assertEquals(R.drawable.ic_friends_icon, vm.profilePicLoadResult.getOrAwaitValue().defaultImage)
    }

    @Test
    fun updateProfilePicture_sets_result_url_when_url_present() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.getUserProfilePictureUrl() } returns Resource.Success("https://x")

        vm.updateProfilePicture()
        advanceUntilIdle()

        assertEquals("https://x", vm.profilePicLoadResult.getOrAwaitValue().resultUrl)
    }

    @Test
    fun updateProfilePicture_sets_error_on_failure() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.getUserProfilePictureUrl() } returns Resource.GeneralError("x")

        vm.updateProfilePicture()
        advanceUntilIdle()

        assertEquals(R.string.profile_picture_update_failed, vm.profilePicLoadResult.getOrAwaitValue().errorMessage)
    }

    @Test
    fun deleteUserProfile_sets_success_result_and_logs_out_when_delete_successful() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.deleteUserProfile() } returns Resource.Success(true)
        coEvery { loginRepository.logout() } returns Resource.GeneralError("ignored")

        vm.deleteUserProfile()
        advanceUntilIdle()

        assertEquals(null, vm.accountDeletionRes.getOrAwaitValue().error)
        coVerify(exactly = 1) { loginRepository.logout() }
    }

    @Test
    fun deleteUserProfile_sets_normal_delete_error_when_delete_returns_false() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.deleteUserProfile() } returns Resource.Success(false)

        vm.deleteUserProfile()
        advanceUntilIdle()

        assertEquals(R.string.account_delete_error_normal, vm.accountDeletionRes.getOrAwaitValue().message)
    }

    @Test
    fun deleteUserProfile_sets_generic_error_when_delete_fails() = runTest {
        val vm = createViewModelWithUserData()
        coEvery { userAccountRepository.deleteUserProfile() } returns Resource.GeneralError("x")

        vm.deleteUserProfile()
        advanceUntilIdle()

        assertEquals(R.string.account_delete_error, vm.accountDeletionRes.getOrAwaitValue().message)
    }
}
