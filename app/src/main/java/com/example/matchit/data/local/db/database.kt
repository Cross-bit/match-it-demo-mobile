package com.example.matchit.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.matchit.data.local.db.dao.ChatMessagesDao
import com.example.matchit.data.local.db.dao.FriendsDao
import com.example.matchit.data.local.db.dao.SessionParametersDao
import com.example.matchit.data.local.db.dao.UserDao
import com.example.matchit.data.local.db.entities.ChatMessageEntity
import com.example.matchit.data.local.db.entities.FriendEntity
import com.example.matchit.data.local.db.entities.SessionEntity
import com.example.matchit.data.local.db.entities.SessionOptionsConverter
import com.example.matchit.data.local.db.entities.UserEntity

/**
 * The name of the local app database
 */
const val MATCH_IT_DB_NAME = "match_it.db"

/**
 * Main application local database
 */
@Database(entities = [UserEntity::class, SessionEntity::class, FriendEntity::class, ChatMessageEntity::class], version = 8, exportSchema = false)
@TypeConverters(SessionOptionsConverter::class)
abstract class MatchItDatabase : RoomDatabase(){
    abstract fun userDao(): UserDao
    abstract fun sessionParametersDao(): SessionParametersDao
    abstract fun friendsDao(): FriendsDao
    abstract fun chatMessagesDao(): ChatMessagesDao
}