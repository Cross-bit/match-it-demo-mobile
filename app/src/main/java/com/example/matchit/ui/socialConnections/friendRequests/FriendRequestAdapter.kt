package com.example.matchit.ui.socialConnections.friendRequests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R


data class RequestItem(val requestId: String, val friendsData: RequestPersonData)
data class RequestPersonData(val thumbnail: Int, val username: String, val email: String)
class IncomingFriendsRequestsAdapter(private val dataList: ArrayList<RequestItem>, private val viewModel: FriendRequestsListViewModel) : RecyclerView.Adapter<IncomingFriendsRequestsAdapter.RequestViewHolder>() {
    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val thumbnailImage: ImageView
        val username: TextView
        val admitRequestButton: Button

        init {
            thumbnailImage = itemView.findViewById(R.id.thumbnail)
            username = itemView.findViewById(R.id.username)
            admitRequestButton = itemView.findViewById(R.id.admit_friend_request_button)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_request_item, parent, false)

        return RequestViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val currentItem = dataList[position]

        holder.thumbnailImage.setImageResource(currentItem.friendsData.thumbnail)
        holder.username.text = currentItem.friendsData.username

        holder.admitRequestButton.setOnClickListener {
            it.visibility = View.GONE
            viewModel.admitFriendRequest(currentItem.requestId)
        }
    }
}