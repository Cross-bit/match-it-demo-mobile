package com.example.matchit.ui.chatting

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.ui.matchingSession.matching.matchedCards.MatchedSummaryView
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// When user interacts with the UI these events are used to inform the outer client
sealed class ChatMessageUiEvent {
    data class MatchedCardClicked(
        val cardData: CardData,
        val sessionType: SessionType,
        val sharedView: View
    ) : ChatMessageUiEvent()
}


class ChatMessagesAdapter (
    private var chatMessages: ArrayList<ChatBubble> = arrayListOf(),
    private val onUiEvent: (ChatMessageUiEvent) -> Unit
) : RecyclerView.Adapter<ChatMessagesAdapter.MessageBubbleViewHolder>()
{

    public fun setMessages(chatBubbles: ArrayList<ChatBubble>) {
        this.chatMessages = chatBubbles
    }

    public fun insertAt(bubble: ChatBubble) {
        val index = chatMessages.indexOfFirst { it.createdAt > bubble.createdAt }
            .let { if (it == -1) chatMessages.size else it }

        chatMessages.add(index, bubble)
        notifyItemInserted(index)
    }

    public fun updateStatusAt(bubble: ChatBubble) {
        val index = chatMessages.indexOfFirst { it.messageUuid == bubble.messageUuid }
        if (index != -1) {
            chatMessages[index] = bubble
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatMessagesAdapter.MessageBubbleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MessageBubbleViewHolder(inflater.inflate(R.layout.chat_message_bubble, parent, false))
    }

    override fun onBindViewHolder(holder: ChatMessagesAdapter.MessageBubbleViewHolder, position: Int) {
        val bubble = chatMessages[position]

        bindBlockMeta(holder, position, bubble)
        bindContent(holder, bubble)
        bindTime(holder, bubble)
        bindStatus(holder, bubble)
    }

    /**
     * Controls display of the meta information of the message.
     *
     */
    private fun bindBlockMeta(
        holder: MessageBubbleViewHolder,
        position: Int,
        bubble: ChatBubble
    ) {
        val firstInBlock = isFirstInBlock(position)

        android.util.Log.d("CHAT", "pos=$position firstInBlock=$firstInBlock userName=${bubble.userName} bubbleType=${bubble.bubbleType} isMine=${bubble.isMine}")

        holder.userAvatar.visibility = View.GONE
        holder.usernameText.visibility = View.GONE

        val rootParams = holder.messageRoot.layoutParams as ViewGroup.MarginLayoutParams
        rootParams.topMargin = if (firstInBlock) {
            holder.itemView.resources.getDimensionPixelSize(R.dimen.chat_block_spacing)
        } else {
            holder.itemView.resources.getDimensionPixelSize(R.dimen.chat_message_spacing)
        }
        holder.messageRoot.layoutParams = rootParams

        if (bubble.isMine) {
            holder.messageContainer.gravity = Gravity.END
            holder.usernameText.visibility = View.GONE
            holder.userAvatar.visibility = View.GONE
            holder.cardView.setCardBackgroundColor(Color.parseColor("#DCEEFF"))
        }
        else if (bubble.bubbleType == ChatBubble.BubbleType.TEXT) // not mine
        {

            holder.messageContainer.gravity = Gravity.START
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0"))

            holder.usernameText.text = bubble.userName
            holder.usernameText.visibility = if (firstInBlock) View.VISIBLE else View.GONE
            holder.userAvatar.visibility = if (firstInBlock) View.VISIBLE else View.INVISIBLE

            Glide.with(holder.userAvatar)
                .load(bubble.userAvatarUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(holder.userAvatar)
        }
    }

    private fun bindStatus(holder: MessageBubbleViewHolder, bubble: ChatBubble) {

        // if not mine we never show it TODO:
        /*if (!bubble.isMine) {
            holder.messageStatus.visibility = View.GONE
            return
        }

        when (bubble.status) {
            MessageStatus.PENDING -> {
                holder.messageStatus.apply {
                    alpha = 1f
                    text = holder.itemView.context.getString(R.string.chat_message_status_sending)
                    visibility = View.VISIBLE
                }
            }

            MessageStatus.SENT -> {
                holder.messageStatus.apply {
                    alpha = 1f
                    text = holder.itemView.context.getString(R.string.chat_message_status_sent)
                    visibility = View.VISIBLE
                }

                holder.messageStatus.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        holder.messageStatus.visibility = View.GONE
                        holder.messageStatus.alpha = 1f
                    }
                    .start()
            }

            MessageStatus.DELIVERED -> TODO()
        }*/
    }

    /**
     * Specific content for given message type
     */
    private fun bindContent(
        holder: MessageBubbleViewHolder,
        bubble: ChatBubble
    ) {
        when (bubble.bubbleType) {

            ChatBubble.BubbleType.TEXT -> {
                holder.matchedSummaryView.visibility = View.GONE
               // holder.cardPreview.visibility = View.GONE

                holder.cardView.visibility = View.VISIBLE
                holder.matchedTitle.visibility = View.GONE
                holder.messageText.text = bubble.text
                adjustMessageText(holder)

            }

            ChatBubble.BubbleType.SYSTEM_EVENT -> {
                when (val s = bubble.content){
                    is ChatBubble.Content.System -> {
                        when (val t = s.event) {
                            is MatchResultUi -> {
                                holder.messageContainer.gravity = Gravity.CENTER
                                holder.cardView.visibility = View.GONE

                                holder.userAvatar.visibility = View.GONE
                                holder.usernameText.visibility = View.GONE
                                holder.matchedTitle.visibility = View.VISIBLE

                                holder.matchedSummaryView.bindCards(cards = t.cards) { it: MatchedItemDTO, view: View ->
                                    it.cardData?.let {
                                        onUiEvent(
                                            ChatMessageUiEvent.MatchedCardClicked(
                                                cardData = it,
                                                sessionType = t.sessionType,
                                                sharedView = view
                                            )
                                        )
                                    }
                                }
                                holder.matchedSummaryView.visibility = View.VISIBLE
                            }
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    /**
     * Adjust of the text for short messages etc.
     */
    private fun adjustMessageText(holder: MessageBubbleViewHolder) {
        holder.messageText.doOnLayout {
            /*val isSingleLine = holder.messageText.lineCount == 1

            val marginEndRes = if (isSingleLine)
                R.dimen.chat_single_line_margin_end
            else
                R.dimen.chat_multi_line_margin_end

            val params = holder.messageText.layoutParams as ViewGroup.MarginLayoutParams
            params.marginEnd = holder.messageText.resources.getDimensionPixelSize(marginEndRes)
            holder.messageText.layoutParams = params*/
        }
    }

    private fun bindTime(holder: MessageBubbleViewHolder, bubble: ChatBubble) {
        holder.messageTime.text = getChatTime(bubble.createdAt)
    }

    private fun getChatTime(timestamp: Long): String {
        return SimpleDateFormat(
            "H:mm",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    private fun isFirstInBlock(position: Int): Boolean {

        if (position == 0) return true
        val current = chatMessages[position]
        val previous = chatMessages[position - 1]

        return current.isMine != previous.isMine
                || current.userName != previous.userName
                || previous.bubbleType != ChatBubble.BubbleType.TEXT
                || current.bubbleType != ChatBubble.BubbleType.TEXT
    }

    private fun isLastInBlock(position: Int): Boolean {
        if (position == chatMessages.lastIndex) return true
        return chatMessages[position].userName != chatMessages[position + 1].userName
    }


    override fun getItemCount(): Int {
        return chatMessages.size
    }

    class MessageBubbleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.message_text)
        val userAvatar: ShapeableImageView = view.findViewById(R.id.message_avatar)
        val usernameText: TextView = view.findViewById(R.id.message_username_text)
        val cardView: CardView = itemView.findViewById(R.id.message_bubble_card)
        val messageContainer: LinearLayout = itemView.findViewById(R.id.message_container)
        val messageRoot: FrameLayout = itemView.findViewById(R.id.message_root)
        val messageTime: TextView = itemView.findViewById(R.id.message_time)
        val messageStatus: TextView = itemView.findViewById(R.id.message_status_text)

        val matchedSummaryView: MatchedSummaryView = itemView.findViewById(R.id.matchedSummaryView)
        val matchedTitle: TextView = itemView.findViewById(R.id.matched_title)


    }
}