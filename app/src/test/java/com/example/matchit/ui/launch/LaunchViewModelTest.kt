package com.example.matchit.ui.launch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.data.userAuthentication.LoginRepository
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository: LoginRepository = mockk()
    private val viewModel = LaunchViewModel(repository)

    @Test
    fun tryAuthenticateUser_sets_true_when_repository_returns_true() = runTest {
        coEvery { repository.checkUserIsLoggedIn() } returns true

        viewModel.tryAuthenticateUser()
        advanceUntilIdle()

        assertTrue(viewModel.authenticationResult.getOrAwaitValue())
    }

    @Test
    fun tryAuthenticateUser_sets_false_when_repository_returns_false() = runTest {
        coEvery { repository.checkUserIsLoggedIn() } returns false

        viewModel.tryAuthenticateUser()
        advanceUntilIdle()

        assertFalse(viewModel.authenticationResult.getOrAwaitValue())
    }
}
