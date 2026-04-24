package com.example.matchit.ui.activitiesDashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.data.model.authentication.LoggedInUser
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.userAccount.UserAccountRepository
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesDashboardViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val loginRepository: LoginRepository = mockk()
    private val accountRepository: UserAccountRepository = mockk()
    private val sessionRepository: SessionRepository = mockk()

    private fun createViewModel(): ActivitiesDashboardViewModel {
        every { loginRepository.user } returns flowOf(LoggedInUser("Alice", "a@mail.com", "u1"))
        return ActivitiesDashboardViewModel(loginRepository, accountRepository, sessionRepository)
    }

    @Test
    fun userName_emits_display_name_from_login_repository_flow() {
        val viewModel = createViewModel()
        assertEquals("Alice", viewModel.userName.getOrAwaitValue())
    }

    @Test
    fun openSessionInvitationCreation_sets_session_type() {
        val viewModel = createViewModel()
        justRun { sessionRepository.setLastSessionType(SessionType.MOVIE) }

        viewModel.openSessionInvitationCreation(SessionType.MOVIE)

        verify(exactly = 1) { sessionRepository.setLastSessionType(SessionType.MOVIE) }
    }

    @Test
    fun updateUserProfilePic_sets_url_on_success() = runTest {
        val viewModel = createViewModel()
        coEvery { accountRepository.getUserProfilePictureUrl() } returns Resource.Success("pic-url")

        viewModel.updateUserProfilePic()
        advanceUntilIdle()

        assertEquals("pic-url", viewModel.profilePictureUrl.getOrAwaitValue())
    }

    @Test
    fun updateUserProfilePic_sets_empty_string_on_error() = runTest {
        val viewModel = createViewModel()
        coEvery { accountRepository.getUserProfilePictureUrl() } returns Resource.GeneralError("x")

        viewModel.updateUserProfilePic()
        advanceUntilIdle()

        assertEquals("", viewModel.profilePictureUrl.getOrAwaitValue())
    }
}
