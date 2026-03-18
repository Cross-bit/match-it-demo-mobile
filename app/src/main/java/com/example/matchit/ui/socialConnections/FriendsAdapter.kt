package com.example.matchit.ui.socialConnections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R

class FriendsAdapter :
    ListAdapter<FriendItem, FriendsAdapter.FriendsViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friend_item, parent, false)
        return FriendsViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {
        val currentItem = getItem(position)

        Glide.with(holder.thumbnailImage)
            .load(currentItem.profilePicUrl)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(holder.thumbnailImage)

        holder.username.text = currentItem.username
        holder.email.text = currentItem.email
    }

    class FriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnail)
        val username: TextView = itemView.findViewById(R.id.username)
        val email: TextView = itemView.findViewById(R.id.email)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FriendItem>() {
            override fun areItemsTheSame(a: FriendItem, b: FriendItem) =
                a.email == b.email

            override fun areContentsTheSame(a: FriendItem, b: FriendItem) =
                a == b
        }
    }
}