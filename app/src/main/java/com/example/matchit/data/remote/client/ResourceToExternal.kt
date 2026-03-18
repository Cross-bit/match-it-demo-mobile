package com.example.matchit.data.remote.client


/**
 * Converts a Resource of type T to a Resource of type U using a provided conversion function.
 * This function allows mapping the data within the resource to a different type.
 *
 * @param conversion A function to convert the data of type T to type U.
 * @return A Resource of type U after applying the conversion function.
 */
inline fun <T, U, E> Resource<T, E>.resourceToExternal(
    conversion: (T) -> U
): Resource<U, E> {
    return when (this) {
        is Resource.Success -> Resource.Success(conversion(this.data))
        is Resource.Error -> Resource.Error(this.errorMessage, this.error)
        is Resource.GeneralError -> Resource.GeneralError(this.message, this.throwable)
    }
}