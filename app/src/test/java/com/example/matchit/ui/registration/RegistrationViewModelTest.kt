package com.example.matchit.ui.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ValidationCode
import com.example.matchit.data.remote.client.ApiErrors.ValidationErrorDetail
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.AuthenticationApi.RegistrationStatus
import com.example.matchit.data.userAuthentication.RegistrationRepository
import com.example.matchit.data.userAuthentication.SignupError
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
class RegistrationViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository: RegistrationRepository = mockk()
    private val viewModel = RegistrationViewModel(repository)

    @Test
    fun signupDataChanged_sets_username_error_for_short_name() {
        viewModel.signupDataChanged("ab", "test@mail.com", "123456")
        assertEquals(R.string.invalid_username_length, viewModel.signupFormState.getOrAwaitValue().usernameError)
    }

    @Test
    fun signupDataChanged_sets_email_error_for_invalid_email() {
        viewModel.signupDataChanged("johnny", "invalid", "123456")
        assertEquals(R.string.invalid_email_general, viewModel.signupFormState.getOrAwaitValue().emailError)
    }

    @Test
    fun signupDataChanged_sets_password_error_for_short_password() {
        viewModel.signupDataChanged("johnny", "test@mail.com", "123")
        assertEquals(R.string.invalid_password_length, viewModel.signupFormState.getOrAwaitValue().passwordError)
    }

    @Test
    fun signupDataChanged_sets_valid_state_for_valid_inputs() {
        viewModel.signupDataChanged("johnny", "test@mail.com", "123456")
        assertTrue(viewModel.signupFormState.getOrAwaitValue().isDataValid)
    }

    @Test
    fun aggregateErrors_maps_email_password_and_username_codes() {
        val result = viewModel.aggregateErrors(
            listOf(
                ValidationErrorDetail("email", ValidationCode.INVALID_EMAIL),
                ValidationErrorDetail("password", ValidationCode.TOO_SHORT),
                ValidationErrorDetail("name", ValidationCode.TOO_SHORT)
            )
        )

        assertEquals(R.string.error_invalid_email, result.emailError)
        assertEquals(R.string.error_too_short, result.passwordError)
        assertEquals(R.string.error_too_short, result.usernameError)
    }

    @Test
    fun handleSignupError_sets_user_exists_error() {
        viewModel.handleSignupError(SignupError.AlreadyExists)
        assertEquals(R.string.user_already_exists, viewModel.signupFormState.getOrAwaitValue().emailError)
    }

    @Test
    fun handleSignupError_sets_network_result_error() {
        viewModel.handleSignupError(SignupError.Network("offline"))
        assertEquals(R.string.connection_error_2, viewModel.signupResult.getOrAwaitValue().error)
    }

    @Test
    fun handleSignupError_sets_validation_errors() {
        viewModel.handleSignupError(
            SignupError.Validation(
                listOf(ValidationErrorDetail("password", ValidationCode.TOO_SHORT))
            )
        )
        assertEquals(R.string.error_too_short, viewModel.signupFormState.getOrAwaitValue().passwordError)
    }

    @Test
    fun signupWithCredentials_sets_success_created() = runTest {
        coEvery { repository.signup(any(), any(), any()) } returns Resource.Success(RegistrationStatus.CREATED)

        viewModel.signupWithCredentials("john", "mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(R.string.signup_successful, viewModel.signupResult.getOrAwaitValue().success)
    }

    @Test
    fun signupWithCredentials_sets_success_verification_mail_sent() = runTest {
        coEvery { repository.signup(any(), any(), any()) } returns Resource.Success(RegistrationStatus.VERIFICATION_MAIL_SEND)

        viewModel.signupWithCredentials("john", "mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(R.string.signup_verification_mail_send, viewModel.signupResult.getOrAwaitValue().success)
    }

    @Test
    fun signupWithCredentials_sets_error_for_already_exists_response() = runTest {
        coEvery { repository.signup(any(), any(), any()) } returns Resource.Success(RegistrationStatus.ALREADY_EXIST)

        viewModel.signupWithCredentials("john", "mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(R.string.signup_failed_already_exist, viewModel.signupResult.getOrAwaitValue().error)
    }

    @Test
    fun signupWithCredentials_sets_error_on_general_error() = runTest {
        coEvery { repository.signup(any(), any(), any()) } returns Resource.GeneralError("boom")

        viewModel.signupWithCredentials("john", "mail@test.com", "123456")
        advanceUntilIdle()

        assertEquals(R.string.signup_failed, viewModel.signupResult.getOrAwaitValue().success)
    }
}
