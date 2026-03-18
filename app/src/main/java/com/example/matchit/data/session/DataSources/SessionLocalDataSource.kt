package com.example.matchit.data.session.DataSources

import com.example.matchit.data.local.db.dao.SessionParametersDao
import com.example.matchit.data.local.db.entities.SessionEntity
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionParameters
import javax.inject.Inject

class SessionLocalDataSource @Inject constructor(
    private var sessionParamsDao: SessionParametersDao
)  {

    /**
     * Stores user data into local database
     */
    suspend fun storeSessionParameters(userUUID: String, sessionType: SessionType, sessionParameters: SessionParameters) {
        sessionParamsDao.insert(
                SessionEntity(
                    userUUID = userUUID,
                    sessionType = sessionType,
                    options = sessionParameters
            ))
    }

    /**
     * Gets currently logged in user stored in the database.
     */
    suspend fun getSessionParameters(userUUID: String, sessionType: SessionType): SessionEntity? {
        val res = sessionParamsDao.getSession(userUUID, sessionType)
        return res
    }
}