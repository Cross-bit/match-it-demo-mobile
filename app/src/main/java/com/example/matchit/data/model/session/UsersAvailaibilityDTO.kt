package com.example.matchit.data.model.session



data class UsersAvailabilityInfo(
    val uuid: String,
    val state: AvailableState
) {
    public enum class AvailableState(val value: String) {
        AVAILABLE("AVAILABLE"),
        IS_IN_ACTIVE_SESSION("IS_IN_ACTIVE_SESSION"),
        IS_OFFLINE("IS_OFFLINE") // meaning will probably not recieve fcm notification...
    }
}

data class UsersAvailabilityDTO(
    val usersInfo: List<UsersAvailabilityInfo>
)

data class UsersAvailabilityRequest(
    val usersUUIDs: List<String>
)