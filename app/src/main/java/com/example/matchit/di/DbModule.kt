package com.example.matchit.di

import android.content.Context
import androidx.room.Room
import com.example.matchit.data.local.db.MATCH_IT_DB_NAME
import com.example.matchit.data.local.db.MatchItDatabase
import com.example.matchit.data.local.db.dao.ChatMessagesDao
import com.example.matchit.data.local.db.dao.FriendsDao
import com.example.matchit.data.local.db.dao.SessionParametersDao
import com.example.matchit.data.local.db.dao.UserDao
//import com.example.matchit.data.userAuthentication.DataSources.LoginLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides
    @Singleton
    fun provide(@ApplicationContext context: Context): MatchItDatabase = Room.databaseBuilder(
        context, MatchItDatabase::class.java, MATCH_IT_DB_NAME)
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideDao(db: MatchItDatabase): UserDao {
        return db.userDao()
    }

    @Provides
    @Singleton
    fun provideSessionParametersDao(db: MatchItDatabase): SessionParametersDao {
        return db.sessionParametersDao()
    }

    @Provides
    @Singleton
    fun provideFriendsDao(db: MatchItDatabase): FriendsDao {
        return db.friendsDao()
    }

    @Provides
    @Singleton
    fun provideChatMessagesDao(db: MatchItDatabase): ChatMessagesDao {
        return db.chatMessagesDao()
    }
}