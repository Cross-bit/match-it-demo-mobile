package com.example.matchit.ui.chatting

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentChatWindowBinding
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatWindowFragment : Fragment()  {

    private var _binding: FragmentChatWindowBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatWindowViewModel by viewModels<ChatWindowViewModel>()

    private val args: ChatWindowFragmentArgs by navArgs()

    private lateinit var chatMessagesAdapter: ChatMessagesAdapter;
    private lateinit var chatMessagesRecyclerview: RecyclerView

    private lateinit var messageTextView: TextView
    private lateinit var sendButton: ImageButton

    private val movieCardHandler = MovieCardHandler(this)

    private val restaurantNavigator: RestaurantNavigator = RestaurantNavigator(this)
    private val restaurantCardHandler = RestaurantCardHandler(this, restaurantNavigator)

    /**
     * Session UUID to which this chat window relates to.
     */
    private lateinit var sessionUUID: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatWindowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sessionUUID = args.sessionUUID

        binding.messageInput.setLayerType(View.LAYER_TYPE_HARDWARE, null);



        initChatControls()
        handleBackPress()
        initViewModelSubscriptions()

        // init all the data (e.g. chat history and new chat observers)
        viewModel.initialize(sessionUUID)
    }

    private fun initViewModelSubscriptions() {

        binding.chatLoadProgressBar.visibility = View.VISIBLE

        chatMessagesRecyclerview = binding.chatRecyclerView
        chatMessagesAdapter = ChatMessagesAdapter(onUiEvent=::handleChatUiEvent)
        chatMessagesRecyclerview.adapter = chatMessagesAdapter

        viewModel.chatMessages.observe(viewLifecycleOwner) { newData: MessagesLoadUpdate ->

            binding.chatLoadProgressBar.visibility = View.GONE

            if (newData.error != null) {
                val message = getString(newData.error)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return@observe
            }

            chatMessagesAdapter.setMessages(ArrayList(newData.chatMessages))

            chatMessagesAdapter?.notifyDataSetChanged()

            chatMessagesRecyclerview.post {
                chatMessagesRecyclerview.scrollToPosition(chatMessagesAdapter.itemCount - 1)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.newIncomingMessageEvent.collectLatest { newMsg ->
                if (newMsg.error != null) {
                    Toast.makeText(context, newMsg.error, Toast.LENGTH_SHORT).show()
                }
                else if(newMsg.message != null) {
                    chatMessagesAdapter.insertAt(newMsg.message)

                    val shouldAutoScroll = isUserNearBottom(chatMessagesRecyclerview)

                    if (shouldAutoScroll) {
                        chatMessagesRecyclerview.scrollToPosition(
                            chatMessagesAdapter.itemCount - 1
                        )
                    }
                }
                else {
                    Log.e("chatWindow", "New incoming message could not be correctly received!")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.newOutgoingMessageEvent.collectLatest { newMsg ->
                if (newMsg.error != null) {
                    Toast.makeText(context, newMsg.error, Toast.LENGTH_SHORT).show()
                }
                else if(newMsg.message != null) {
                    chatMessagesAdapter.insertAt(newMsg.message)
                    chatMessagesRecyclerview.scrollToPosition(chatMessagesAdapter.itemCount - 1)
                }
                else {
                    Log.e("chatWindow", "New outgoing message could not be correctly added to the chat!")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messageUpdatesEvent.collectLatest { updateMsg ->
                chatMessagesAdapter.updateStatusAt(updateMsg.message)
            }
        }
    }

    private fun handleChatUiEvent(eventData: ChatMessageUiEvent) {
        when(eventData){
            is ChatMessageUiEvent.MatchedCardClicked -> {
                when (eventData.sessionType) {
                    SessionType.MOVIE -> movieCardHandler.openCardDetail(eventData.cardData as MovieCardData, eventData.sharedView)
                    SessionType.RESTAURANT -> {
                        restaurantCardHandler.openCardDetail(eventData.cardData as RestaurantCardData, eventData.sharedView)
                    }
                }

            }
        }
    }

    private fun isUserNearBottom(
        recyclerView: RecyclerView,
        threshold: Int = 1 // kolik položek od konce tolerujeme
    ): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            ?: return false

        val lastVisible = layoutManager.findLastVisibleItemPosition()
        val total = layoutManager.itemCount

        return lastVisible >= total - 1 - threshold
    }


    private fun initChatControls() {
        messageTextView = binding.messageInput
        sendButton = binding.sendButton

        val toolbarDotsBtn = binding.btnOverflow
        val exitChatBtn = binding.exitChatBtn

        exitChatBtn.setOnClickListener(){
            findNavController().popBackStack()
        }

        sendButton.setOnClickListener {
            val message = messageTextView.text.toString()
            onSendMessage(message)
        }

        toolbarDotsBtn.setOnClickListener {
            val popup = PopupMenu(requireContext(), toolbarDotsBtn)
            popup.menuInflater.inflate(R.menu.session_chat_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.exit_chat_option -> {
                        findNavController().popBackStack()
                        true
                    }
                    else -> {true}

                }
            }

            popup.show()
        }
    }


    private fun onSendMessage(message: String) {

        if (message.isNotBlank()) {
            viewModel.sendMessage(sessionUUID, message)
            messageTextView.text = ""
        }
    }

    private fun handleBackPress() {

        val backPressCallback = object : OnBackPressedCallback(true) {
            // navigate back to where we came from
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }

        Log.d("NAV", "Controller = ${findNavController()}")
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

    }
}