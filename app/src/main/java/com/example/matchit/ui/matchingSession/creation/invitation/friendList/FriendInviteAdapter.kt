package com.example.matchit.ui.matchingSession.creation.invitation.friendList

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R

class FriendInviteAdapter(
    private val ctx: Context?,
    private val onFriendCheckedChanged: (FriendInviteListItem, state: Boolean) -> Unit
) : RecyclerView.Adapter<FriendInviteAdapter.FriendsViewHolder>() {

    var dataList = listOf<FriendInviteListItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class FriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImage: ImageView
        val username: TextView
        val inviteCheckBox: CheckBox
        val invitationProgress: ProgressBar
        val friendInvitedLabel: CardView

        init {
            thumbnailImage = itemView.findViewById(R.id.thumbnail)
            username = itemView.findViewById(R.id.username)
            inviteCheckBox = itemView.findViewById(R.id.invite_friend_checkbox)
            invitationProgress = itemView.findViewById(R.id.invite_friend_loading)
            friendInvitedLabel = itemView.findViewById(R.id.friend_connected_label)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : FriendsViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_session_invite_friend_item, parent, false)

        return FriendsViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {

        val currentItem = dataList[position]

        Glide.with(holder.thumbnailImage)
            .load(currentItem.thumbnail)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(holder.thumbnailImage)


        holder.username.text = currentItem.username

        // generally loading is gone and checkbox is visible
        holder.inviteCheckBox.visibility = View.VISIBLE
        holder.invitationProgress.visibility = View.GONE
        holder.friendInvitedLabel.visibility = View.GONE

        holder.inviteCheckBox.setOnCheckedChangeListener(null) // THIS LINE HERE IS INCREDIBLY IMPORTANT   !!! CAN'T BE OMITTED !!!
                                                                // IT UNSETS ANY PREVIOUSLY SET LISTENER, THUS
                                                                // ONCE WE EXPLICITLY SET inviteCheckBox.isChecked = false/true
                                                                // THE CALLBACK IS NOT CALLED AND ONLY UI IS AFFECTED
                                                                // OTHERWISE WE WOULD END-UP IN INFINITE LOOP, IF TRYING TO CHANGE THE STATE PROGRAMMATICALLY...
                                                                // AND EVENTUALLY CRASH FROM SOME KIND OF RACE CONDITION (SINCE TWO (BACK)THREADS WOULD TRY TO UPDATE THE LIST VIEW AT THE SAME TIME...)

                                                                // IT IS ALSO IMPORTANT TO SET THE CALLBACK AFTER THE PROGRAMMATIC SET IS PERFORMED

        /**
         * Update the list item view based on updated data:
         */
        when (currentItem.invitationState) {
            FriendInviteListItem.InvitationState.SELECTED -> {

                holder.inviteCheckBox.isChecked = true
                holder.inviteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    onFriendCheckedChanged(currentItem, isChecked)
                }
            }
            FriendInviteListItem.InvitationState.INVITABLE -> {

                holder.inviteCheckBox.isEnabled = true
                holder.inviteCheckBox.isChecked = false

                holder.inviteCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    onFriendCheckedChanged(currentItem, isChecked)
                }

            }
            FriendInviteListItem.InvitationState.INVITED -> {

                holder.inviteCheckBox.isEnabled = false
                holder.inviteCheckBox.visibility = View.INVISIBLE
                holder.invitationProgress.visibility = View.VISIBLE
            }
            FriendInviteListItem.InvitationState.REJECTED -> {

                holder.inviteCheckBox.visibility = View.INVISIBLE
                holder.friendInvitedLabel.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.invite_rejected_label))
                val labelText = holder.friendInvitedLabel.findViewById<TextView>(R.id.friend_connected_label_text)
                labelText.text = ctx?.getText(R.string.request_rejected)

                holder.friendInvitedLabel.visibility = View.VISIBLE
            }
            FriendInviteListItem.InvitationState.NOT_INVITABLE -> {

                holder.inviteCheckBox.isEnabled = false
                holder.inviteCheckBox.isChecked = false
            }
            FriendInviteListItem.InvitationState.CONNECTED -> {

               holder.inviteCheckBox.isEnabled = false
                holder.friendInvitedLabel.visibility = View.VISIBLE
                holder.inviteCheckBox.visibility = View.INVISIBLE
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}