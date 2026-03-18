package com.example.matchit.data.session

import com.example.matchit.data.local.db.entities.SessionEntity
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.session.DataSources.SessionLocalDataSource
import com.example.matchit.data.userAuthentication.DataSources.UserDataLocalDataSource
import javax.inject.Inject


class SessionParametersRepository @Inject constructor(
    private val userDataSource: UserDataLocalDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource
) {

    /**
     * Updates only local state of the session options.
     */
    suspend fun updateSessionParameters(sessionParameters: SessionParameters, sessionType: SessionType) {
        val userData = userDataSource.getCurrentUser() ?: return

        sessionLocalDataSource.storeSessionParameters(userData.uuid, sessionType, sessionParameters)
    }

    suspend fun getSessionParameters(sessionType: SessionType) : SessionParameters? {
        return getSessionParametersRecord(sessionType)?.options
    }

    private suspend fun getSessionParametersRecord(sessionType: SessionType) : SessionEntity? {
        val userData = userDataSource.getCurrentUser() ?: return null
        val result = sessionLocalDataSource.getSessionParameters(userData.uuid, sessionType)
        return result
    }
}