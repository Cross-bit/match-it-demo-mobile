package com.example.matchit.data.local.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionParameters
import com.google.gson.Gson
import com.google.gson.JsonParser


// Type converter to serialize to/from GSON string
class SessionOptionsConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromSessionOptions(options: SessionParameters?): String? {
        return options?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toSessionOptions(json: String?): SessionParameters? {
        if (json == null) return null

        val obj = JsonParser.parseString(json).asJsonObject

        return if (obj.has("searchCity")) {
            gson.fromJson(json, SessionParameters.RestaurantParameters::class.java)
        } else {
            gson.fromJson(json, SessionParameters.MovieParameters::class.java)
        }
    }
}


// Entity to store session data
@Entity(
    tableName = "sessionParameters",
    indices = [Index(value = ["userUUID", "sessionType"], unique = true)]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "userUUID") val userUUID: String = "",
    val sessionType: SessionType,
    val options: SessionParameters?
)