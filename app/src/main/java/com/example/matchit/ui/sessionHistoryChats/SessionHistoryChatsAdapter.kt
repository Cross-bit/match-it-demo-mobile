package com.example.matchit.ui.sessionHistoryChats

import android.graphics.Color
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
import com.example.matchit.data.model.session.SessionState
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.google.android.material.imageview.ShapeableImageView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


data class HistorySessionItem(
    val sessionUUID: String,
    val sessionType: SessionType,
    val state: SessionState,
    val createdAt: Long,
    val size: Int,
    val users: List<UserProfileApiResponse>
)


class SessionHistoryChatsAdapter(
    private val onItemClick: (HistorySessionItem) -> Unit
) :
ListAdapter<HistorySessionItem, SessionHistoryChatsAdapter.SessionsHistoryViewHolders>(DIFF)
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsHistoryViewHolders {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_sessions_item, parent, false)

        return SessionsHistoryViewHolders(view)
    }

    override fun onBindViewHolder(holder: SessionsHistoryViewHolders, position: Int) {

        val item = getItem(position)

        val typeImg = if (item.sessionType == SessionType.MOVIE) R.drawable.ic_movie2 else R.drawable.ic_restaurant

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())

        val millis = item.createdAt
        holder.sessionDatetime.text = formatter.format(Instant.ofEpochMilli(millis))
        holder.sessionType.text = item.sessionType.toString()

        val color = when (item.sessionType) {
            SessionType.MOVIE -> Color.parseColor("#455AFF")
            SessionType.RESTAURANT -> Color.parseColor("#F28A3A")// orange
            else -> Color.GRAY
        }

        holder.sessionType.setBackgroundColor(color)


        Glide.with(holder.sessionTypeImg)
            .load(typeImg)
            .placeholder(typeImg)
            .error(typeImg)
            .circleCrop()
            .into(holder.sessionTypeImg)

        val avatarViews = listOf(holder.avatar1, holder.avatar2, holder.avatar3)

        // reset state
        avatarViews.forEach { it.visibility = View.GONE }
        holder.avatarMore.visibility = View.GONE

        val usersWithAvatar = item.users
            .filter { !it.avatarUrl.isNullOrBlank() }

        val maxAvatars = 3
        val avatarsToShow = usersWithAvatar.take(maxAvatars)

        avatarsToShow.forEachIndexed { index, user ->
            val imageView = avatarViews[index]
            imageView.visibility = View.VISIBLE

            Glide.with(imageView.context)
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(imageView)
        }

        // if more than 3 => show circle with count
        if (usersWithAvatar.size > maxAvatars) {
            holder.avatar3.visibility = View.GONE
            holder.avatarMore.visibility = View.VISIBLE
            holder.avatarMore.text = "+${usersWithAvatar.size - 2}"
        }

        holder.itemView.setOnClickListener {
            onItemClick.invoke(item)
        }
    }


    class SessionsHistoryViewHolders(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val sessionTypeImg: ShapeableImageView = itemView.findViewById(R.id.sessionTypeImg)
        val sessionType: TextView = itemView.findViewById(R.id.sesTypeTitle)
        val sessionDatetime: TextView = itemView.findViewById(R.id.sessionDatetime)

        val avatar1: ImageView = itemView.findViewById(R.id.avatar1)
        val avatar2: ImageView = itemView.findViewById(R.id.avatar2)
        val avatar3: ImageView = itemView.findViewById(R.id.avatar3)
        val avatarMore: TextView = itemView.findViewById(R.id.avatarMore)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HistorySessionItem>() {
            override fun areItemsTheSame(a: HistorySessionItem, b: HistorySessionItem) =
                a.sessionUUID == b.sessionUUID

            override fun areContentsTheSame(a: HistorySessionItem, b: HistorySessionItem) =
                a == b
        }
    }
}