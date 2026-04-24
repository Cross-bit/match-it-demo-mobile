package com.example.matchit.ui.socialConnections

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.data.friendships.FriendsRepositoryImpl
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FetchFriendApiResponse
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsListViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val friendsRepository: FriendsRepositoryImpl = mockk()

    @Test
    fun init_loads_mapped_friends_from_repository() = runTest {
        coEvery { friendsRepository.getAllFriends() } returns Resource.Success(
            FetchAllFriendsApiResponse(
                listOf(
                    FetchFriendApiResponse("u1", "John", "j@mail.com", "avatar")
                )
            )
        )

        val viewModel = FriendsListViewModel(friendsRepository)
        advanceUntilIdle()

        val rows = viewModel.allFriendsData.getOrAwaitValue()
        assertEquals(1, rows.size)
        assertEquals("John", rows.first().username)
        assertEquals("j@mail.com", rows.first().email)
    }

    @Test
    fun init_keeps_live_data_empty_when_repository_fails() = runTest {
        coEvery { friendsRepository.getAllFriends() } returns Resource.GeneralError("offline")

        val viewModel = FriendsListViewModel(friendsRepository)
        advanceUntilIdle()

        assertEquals(null, viewModel.allFriendsData.value)
    }
}
