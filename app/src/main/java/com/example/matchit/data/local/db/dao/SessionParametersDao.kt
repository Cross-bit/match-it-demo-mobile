package com.example.matchit.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.matchit.data.local.db.entities.SessionEntity
import com.example.matchit.data.model.session.SessionType


@Dao
interface SessionParametersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessionParameters WHERE userUUID = :userUUID AND sessionType = :sessionType")
    suspend fun getSession(userUUID: String, sessionType: SessionType): SessionEntity?

    @Query("DELETE FROM sessionParameters")
    fun deleteAll()

    @Query("DELETE FROM sessionParameters WHERE userUUID = :id")
    fun deleteRecordForUser(id: String)
}
