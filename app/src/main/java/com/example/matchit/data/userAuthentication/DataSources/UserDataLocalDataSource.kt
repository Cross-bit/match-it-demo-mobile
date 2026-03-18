package com.example.matchit.data.userAuthentication.DataSources

import com.example.matchit.data.local.db.dao.UserDao
import com.example.matchit.data.local.db.entities.UserEntity
import javax.inject.Inject




class UserDataLocalDataSource @Inject constructor(
    private var dao: UserDao
)  {

    /**
     * Stores user data into local database
     */
    fun storeUser(user: UserEntity) = dao.insertUser(user)

    /**
     * Gets currently logged in user stored in the database.
     */
    fun getCurrentUser(): UserEntity? = dao.getCurrentUser()

    /**
     * Deletes all records from the user database (The database is currently expected to have only one row, containing info about current user)
     */
    fun deleteAll() = dao.deleteAll()

    /**
     * Update user profile picture
     */
    fun updateProfilePicture(url: String) = dao.updateAllUsersProfilePictures(url)

}