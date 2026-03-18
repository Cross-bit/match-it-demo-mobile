package com.example.matchit.data.remote.model

/**
 * General api response for simple api endpoints
 */
data class SimpleApiResponse (val result: Status){
    enum class Status(val value: String)  {
        OK("OK"),
        NOK("NOK")
    }
}


fun SimpleApiResponse.toExternal(): Boolean {
    return this.result == SimpleApiResponse.Status.OK
}