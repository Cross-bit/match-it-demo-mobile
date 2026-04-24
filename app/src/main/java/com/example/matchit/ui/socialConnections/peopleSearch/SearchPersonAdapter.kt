package com.example.matchit.ui.socialConnections.peopleSearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R

class SearchPersonAdapter(private val viewModel: SearchPeopleViewModel) : RecyclerView.Adapter<SearchPersonAdapter.ViewHolder>() {
    var data = listOf<SearchPersonItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.person_item, parent, false)
        return ViewHolder(view, viewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        Glide.with(holder.thumbnailImage)
            .load(item.thumbnail)
            .placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person)
            .into(holder.thumbnailImage)

        holder.username.text = item.username
        holder.addToFriendsButton.visibility = if (item.hasInvitation or item.isFriend) View.GONE else View.VISIBLE

        holder.addToFriendsButton.setOnClickListener {
            viewModel.sendFriendRequest(item.userUUID)
            it.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View, private val viewModel: SearchPeopleViewModel) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnail)
        val username: TextView = itemView.findViewById(R.id.username)
        val addToFriendsButton: Button = itemView.findViewById(R.id.add_to_friends_button)

    }
}