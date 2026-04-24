package com.example.matchit.ui.socialConnections.friendRequests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.data.friendships.FriendsRepositoryImpl
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.FetchAllFriendshipCreationRequestsApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FriendRequestApiResponse
import com.example.matchit.data.remote.model.FriendsApi.FriendRequestFriendDataApiResponse
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.users.UsersRepository
import com.example.matchit.testutil.MainDispatcherRule
import com.example.matchit.testutil.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestsListViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val friendsRepository: FriendsRepositoryImpl = mockk()
    private val usersRepository: UsersRepository = mockk()
    private val notificationDispatcher = NotificationDispatcher()
    private val viewModel = FriendRequestsListViewModel(
        friendsRepository,
        notificationDispatcher,
        usersRepository
    )

    @Test
    fun updateAllFriendRequests_sets_mapped_list_with_avatar() = runTest {
        coEvery { friendsRepository.getAllFriendshipCreationRequests() } returns Resource.Success(
            FetchAllFriendshipCreationRequestsApiResponse(
                listOf(
                    FriendRequestApiResponse(
                        "r1",
                        FriendRequestFriendDataApiResponse("u1", "John", "j@mail.com")
                    )
                )
            )
        )
        coEvery { usersRepository.fetchUserData("u1") } returns Resource.Success(
            UserProfileApiResponse("u1", "John", "avatar-url")
        )

        viewModel.updateAllFriendRequests()
        advanceUntilIdle()

        val rows = viewModel.allRequestData.getOrAwaitValue()
        assertEquals(1, rows.size)
        assertEquals("r1", rows.first().requestId)
        assertEquals("avatar-url", rows.first().friendsData.thumbnail)
    }

    @Test
    fun updateAllFriendRequests_sets_null_avatar_when_user_lookup_fails() = runTest {
        coEvery { friendsRepository.getAllFriendshipCreationRequests() } returns Resource.Success(
            FetchAllFriendshipCreationRequestsApiResponse(
                listOf(
                    FriendRequestApiResponse(
                        "r1",
                        FriendRequestFriendDataApiResponse("u1", "John", "j@mail.com")
                    )
                )
            )
        )
        coEvery { usersRepository.fetchUserData("u1") } returns Resource.GeneralError("x")

        viewModel.updateAllFriendRequests()
        advanceUntilIdle()

        assertNull(viewModel.allRequestData.getOrAwaitValue().first().friendsData.thumbnail)
    }

    @Test
    fun updateAllFriendRequests_invalidates_friendship_invite_after_refresh() = runTest {
        notificationDispatcher.postNewFriendRequestNotificationEvent(
            com.example.matchit.data.notifications.pushNotifications.model.NewFriendRequestDTO("u1", "John")
        )
        coEvery { friendsRepository.getAllFriendshipCreationRequests() } returns Resource.GeneralError("err")

        viewModel.updateAllFriendRequests()
        advanceUntilIdle()

        assertNull(notificationDispatcher.newFriendRequest.value)
    }

    @Test
    fun admitFriendRequest_sets_success_message_when_repository_succeeds() = runTest {
        coEvery { friendsRepository.admitFriendshipCreationRequest("r1") } returns Resource.Success(Unit)

        viewModel.admitFriendRequest("r1")
        advanceUntilIdle()

        assertEquals("Friend request admitted successfully", viewModel.admitFriendRequestStatus.getOrAwaitValue())
    }

    @Test
    fun admitFriendRequest_sets_failure_message_when_repository_returns_error() = runTest {
        coEvery { friendsRepository.admitFriendshipCreationRequest("r1") } returns Resource.Error(
            "err",
            ApiError("ERR", "x", 400, null)
        )

        viewModel.admitFriendRequest("r1")
        advanceUntilIdle()

        assertEquals("Failed to admit friend request", viewModel.admitFriendRequestStatus.getOrAwaitValue())
    }
}
