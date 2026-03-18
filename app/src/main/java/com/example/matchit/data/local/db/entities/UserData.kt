package com.example.matchit.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uuid: String = "",
    @ColumnInfo(name = "name") val displayName: String = "",
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "profile_picture_url") val profilePic: String = ""
)