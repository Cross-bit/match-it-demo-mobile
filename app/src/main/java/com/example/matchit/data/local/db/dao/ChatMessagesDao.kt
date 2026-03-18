package com.example.matchit.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.matchit.data.local.db.entities.ChatMessageEntity
import com.example.matchit.data.local.db.entities.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(msgs: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_messages WHERE session_uuid = :sessionUuid ORDER BY created_at ASC")
    fun getMessagesForSession(sessionUuid: String): Flow<List<ChatMessageEntity>>

    @Query(value = "UPDATE chat_messages SET status = :status WHERE message_uuid = :messageUuid")
    suspend fun updateMessageStatus(messageUuid: String, status: MessageStatus)

    @Query("DELETE FROM chat_messages WHERE session_uuid = :sessionUuid")
    suspend fun deleteMessagesForSession(sessionUuid: String)
}