package com.example.matchit.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.matchit.data.local.db.entities.FriendEntity
import com.example.matchit.data.local.db.entities.UserEntity

@Dao
interface FriendsDao {

    @Query("SELECT * FROM friends")
    suspend fun getAllFriends(): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE uuid = :uuid LIMIT 1")
    suspend fun getFriendByUUID(uuid: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFriends(friends: List<FriendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Query("DELETE FROM friends")
    suspend fun deleteAllFriends()

    @Query("UPDATE friends SET profile_picture_url = :url WHERE uuid = :uuid")
    suspend fun updateFriendProfilePicture(uuid: String, url: String)
}

