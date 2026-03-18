package com.example.matchit.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.matchit.data.local.db.entities.UserEntity

@Dao
interface UserDao { // we actually keep only single record of the current user

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(noteEntity: UserEntity)

    @Query("DELETE FROM users")
    fun deleteAll()

    @Query("UPDATE users SET profile_picture_url = :url") // updates all the records => only the current user
    fun updateAllUsersProfilePictures(url: String)
}