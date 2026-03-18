package com.example.matchit.ui.matchingSession.creation.invitation.friendList

data class FriendInviteListItem(
    val uuid: String,
    val thumbnail: String,
    val username: String,
    val invitationState: InvitationState
){

    /**
     * Checks if current state is SELECTED
     */
    fun isSelected(): Boolean {
        return invitationState == InvitationState.SELECTED;
    }

    enum class InvitationState {

        /** We chose user from the list user */
        SELECTED,

        /** We sent invitation to the user */
        INVITED,

        /** User can be invited to the session */
        INVITABLE,

        /** User is not invitable to the session (e.g. we exceeded maximal number of invitation etc.) */
        NOT_INVITABLE,

        /** User has connected to the session */
        CONNECTED,

        /** User has rejected to connected to the session */
        REJECTED
    }

}
