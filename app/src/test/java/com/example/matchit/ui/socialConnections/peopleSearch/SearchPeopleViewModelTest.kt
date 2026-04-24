package com.example.matchit.ui.socialConnections.peopleSearch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.matchit.R
import com.example.matchit.data.remote.client.ApiErrors.ApiError
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.FriendsApi.PersonFetchApiResponse
import com.example.matchit.data.remote.model.FriendsApi.PersonSearchApiResponse
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.search.SearchPersonRepositoryImpl
import com.example.matchit.data.users.UsersRepository
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
class SearchPeopleViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val searchRepo: SearchPersonRepositoryImpl = mockk()
    private val usersRepository: UsersRepository = mockk()
    private val viewModel = SearchPeopleViewModel(searchRepo, usersRepository)

    @Test
    fun updateSearchPersonByEmail_sets_person_result_on_success() = runTest {
        coEvery { searchRepo.searchPerson(any()) } returns Resource.Success(
            PersonFetchApiResponse(
                PersonSearchApiResponse("u1", "John", "j@mail.com", false, false)
            )
        )
        coEvery { usersRepository.fetchUserData("u1") } returns Resource.Success(
            UserProfileApiResponse("u1", "John", "https://avatar")
        )

        viewModel.updateSearchPersonByEmail("j@mail.com")
        advanceUntilIdle()

        val result = viewModel.searchResult.getOrAwaitValue()
        assertEquals("John", result.data?.username)
        assertEquals("https://avatar", result.data?.thumbnail)
    }

    @Test
    fun updateSearchPersonByEmail_uses_null_avatar_when_user_fetch_fails() = runTest {
        coEvery { searchRepo.searchPerson(any()) } returns Resource.Success(
            PersonFetchApiResponse(PersonSearchApiResponse("u1", "John", "j@mail.com", false, false))
        )
        coEvery { usersRepository.fetchUserData("u1") } returns Resource.GeneralError("x")

        viewModel.updateSearchPersonByEmail("j@mail.com")
        advanceUntilIdle()

        assertEquals("null", viewModel.searchResult.getOrAwaitValue().data?.thumbnail)
    }

    @Test
    fun updateSearchPersonByEmail_sets_not_found_error_for_404() = runTest {
        coEvery { searchRepo.searchPerson(any()) } returns Resource.Error(
            "not found",
            ApiError("NOT_FOUND", "no", 404, null)
        )

        viewModel.updateSearchPersonByEmail("not@found.com")
        advanceUntilIdle()

        assertEquals(R.string.person_not_found_message, viewModel.searchResult.getOrAwaitValue().error)
    }

    @Test
    fun updateSearchPersonByEmail_sets_search_failed_for_non_404_api_error() = runTest {
        coEvery { searchRepo.searchPerson(any()) } returns Resource.Error(
            "boom",
            ApiError("X", "x", 500, null)
        )

        viewModel.updateSearchPersonByEmail("j@mail.com")
        advanceUntilIdle()

        assertEquals(R.string.search_failed, viewModel.searchResult.getOrAwaitValue().error)
    }

    @Test
    fun updateSearchPersonByEmail_sets_connection_error_on_general_error() = runTest {
        coEvery { searchRepo.searchPerson(any()) } returns Resource.GeneralError("offline")

        viewModel.updateSearchPersonByEmail("j@mail.com")
        advanceUntilIdle()

        assertEquals(R.string.connection_error, viewModel.friendRequestStatus.getOrAwaitValue())
    }

    @Test
    fun sendFriendRequest_sets_success_message_on_success() = runTest {
        coEvery { searchRepo.sendFriendRequest(any()) } returns Resource.Success(Unit)

        viewModel.sendFriendRequest("u1")
        advanceUntilIdle()

        assertEquals(R.string.successfully_send_request, viewModel.friendRequestStatus.getOrAwaitValue())
    }

    @Test
    fun sendFriendRequest_sets_failed_message_on_api_error() = runTest {
        coEvery { searchRepo.sendFriendRequest(any()) } returns Resource.Error(
            "fail",
            ApiError("X", "x", 400, null)
        )

        viewModel.sendFriendRequest("u1")
        advanceUntilIdle()

        assertEquals(R.string.failed_to_send_request, viewModel.friendRequestStatus.getOrAwaitValue())
    }

    @Test
    fun sendFriendRequest_sets_connection_error_on_general_error() = runTest {
        coEvery { searchRepo.sendFriendRequest(any()) } returns Resource.GeneralError("offline")

        viewModel.sendFriendRequest("u1")
        advanceUntilIdle()

        assertEquals(R.string.connection_error, viewModel.friendRequestStatus.getOrAwaitValue())
    }
}
