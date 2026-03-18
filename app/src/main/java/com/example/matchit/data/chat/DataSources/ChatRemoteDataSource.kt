package com.example.matchit.data.chat.DataSources;

import com.example.matchit.BuildConfig
import com.example.matchit.data.model.chat.ChatSystemEvent
import com.example.matchit.data.remote.api.ChatApiService
import com.example.matchit.data.remote.client.AuthenticatedApiCaller
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.Chat.AllChatMessagesApiResponse
import com.google.gson.GsonBuilder
import java.io.IOException
import javax.inject.Inject

public class ChatRemoteDataSource @Inject constructor(
    private val authenticatedApiCaller: AuthenticatedApiCaller
)  {

    /**
     * Load all messages older than [timestamp] (unix format) from the remote server for given session with [sessionUUID].
     */
    suspend fun loadAllMessageFromTheServer(sessionUUID: String, timestamp: Long) : Resource<AllChatMessagesApiResponse, *> {
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(
                    ChatSystemEvent::class.java,
                    ChatSystemEventJsonAdapter()
                ).create()

            authenticatedApiCaller.makeAuthenticatedApiCall(BuildConfig.DATA_API_URL, customGsonParser = gson) { client ->
                client.create(ChatApiService::class.java).getAllChatMessages(sessionUUID, timestamp)
            }
        } catch (e: Throwable) {
            Resource.GeneralError("Error while sending create session request", IOException("Error create session", e))
        }
    }
}
