package com.example.matchit.ui.socialConnections.peopleSearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View, private val viewModel: SearchPeopleViewModel) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnail)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val addToFriendsButton: Button = itemView.findViewById(R.id.add_to_friends_button)

        fun bind(item: SearchPersonItem) {
            thumbnailImage.setImageResource(item.thumbnail)
            username.text = item.username
            addToFriendsButton.visibility = if (item.hasInvitation or item.isFriend) View.GONE else View.VISIBLE

            addToFriendsButton.setOnClickListener {
                viewModel.sendFriendRequest(item.userUUID)
                it.visibility = View.GONE // TODO Will be better it.visibility = if was not request send yet
            }
        }
    }
}