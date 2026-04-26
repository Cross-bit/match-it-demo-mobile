package com.example.matchit.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.R
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.userAuthentication.LoginError
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository: LoginRepository = mockk()
    private val viewModel = LoginViewModel(repository)

    @Test
    fun loginDataChanged_sets_email_error_for_invalid_email() {
        viewModel.loginDataChanged("", "123456")
        assertEquals(R.string.invalid_email_general, viewModel.loginFormState.getOrAwaitValue().usernameError)
    }

    @Test
    fun loginDataChanged_sets_password_error_for_short_password() {
        viewModel.loginDataChanged("mail", "123")
        assertEquals(R.string.invalid_password_length, viewModel.loginFormState.getOrAwaitValue().passwordError)
    }

    @Test
    fun loginDataChanged_sets_is_data_valid_for_valid_input() {
        viewModel.loginDataChanged("mail", "123456")
        assertTrue(viewModel.loginFormState.getOrAwaitValue().isDataValid)
    }

    @Test
    fun login_sets_success_result_on_successful_repository_response() = runTest {
        coEvery { repository.login(any(), any()) } returns Resource.Success(
            LoggedInUser("John", "mail@test.com", "uuid1")
        )

        viewModel.login("mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals("John", viewModel.loginResult.getOrAwaitValue().success?.displayName)
    }

    @Test
    fun login_sets_invalid_password_error() = assertErrorMapping(LoginError.InvalidPassword, R.string.login_failed_invalid_password)
    @Test
    fun login_sets_already_logged_in_error() = assertErrorMapping(LoginError.AlreadyLoggedIn, R.string.login_failed_already_logged_in)
    @Test
    fun login_sets_not_verified_error() = assertErrorMapping(LoginError.NotVerified, R.string.login_failed_not_verified)
    @Test
    fun login_sets_not_exist_error() = assertErrorMapping(LoginError.NotExist, R.string.login_failed_not_exist)
    @Test
    fun login_sets_network_error_for_network_error() = assertErrorMapping(LoginError.NetworkError("x"), R.string.connection_error)
    @Test
    fun login_sets_invalid_credentials_error_for_validation_error() = assertErrorMapping(LoginError.Validation(emptyList()), R.string.invalid_credentials_error)

    @Test
    fun login_sets_connection_error_for_general_error() = runTest {
        coEvery { repository.login(any(), any()) } returns Resource.GeneralError("network-down")

        viewModel.login("mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(R.string.connection_error, viewModel.loginResult.getOrAwaitValue().error)
    }

    private fun assertErrorMapping(loginError: LoginError, expectedErrorRes: Int) = runTest {
        coEvery { repository.login(any(), any()) } returns Resource.Error("err", loginError)

        viewModel.login("mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(expectedErrorRes, viewModel.loginResult.getOrAwaitValue().error)
    }
}
