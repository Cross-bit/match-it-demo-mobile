package com.example.matchit.data.remote.client

import com.example.matchit.data.remote.client.ApiErrors.ApiError



/**
 * Sealed class representing different states of a resource that might be returned from an operation.
 * It can either be Success, GeneralError, or Error, each carrying specific data or error information.
 *
 * @param T The type of data associated with the resource.
 */
sealed class Resource<out T,out E> {

    /**
     * Represents a successful state of the operation.
     *
     * @param data The data resulting from the successful operation (e.g. api call).
     */
    data class Success<out T, out E>(val data: T) : Resource<T, E>()

    /**
     * Represents an error state of the operation with an error message and optional API error details.
     *
     * @param errorMessage A message describing the encountered error.
     * @param error Optional API-specific error details encapsulated in an ApiError object.
     */
    data class Error<out E>(val errorMessage: String, val error: E) : Resource<Nothing, E>()

    /**
     * Represents a general error state of the operation with an error message and an exception.
     *
     * @param message A message describing the encountered error.
     * @param throwable The exception associated with the error state.
     */
    data class GeneralError(val message: String, val throwable: Throwable? = null) : Resource<Nothing, Nothing>()
}