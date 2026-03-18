package com.example.matchit.data.remote.client

import com.example.matchit.data.remote.model.SimpleApiResponse
import com.example.matchit.data.remote.model.toExternal


/**
 * Maps simple api response resource to external simple boolean result
 */
fun <E> Resource<SimpleApiResponse, E>.mapToBooleanResource(): Resource<Boolean, E> {
    return when (this) {
        is Resource.Success -> Resource.Success(this.data.toExternal())
        is Resource.Error -> Resource.Error(this.errorMessage, this.error)
        is Resource.GeneralError -> Resource.GeneralError(this.message, this.throwable)
    }
}