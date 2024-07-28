package com.spcrey.blog.fragment

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.MessageListActivity
import com.spcrey.blog.R
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MessageUserListFragment : Fragment() {

    companion object {
        private const val TAG = "MessageUserListFragment"
    }

    private lateinit var view: View

    private val recyclerView by lazy {
        view.findViewById<RecyclerView>(R.id.recycler_view)
    }
    private val swipeRefreshLayout by lazy {
        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
    }
    private val messageUserAdapter by lazy {
        MessageUserAdapter(CachedData.multiUserMessageList.userMessageLists)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_user_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        this.view = view

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = messageUserAdapter

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                CachedData.token?.let { token ->
                    messageList(token)
                }
                withContext(Dispatchers.Main) {
                    messageUserAdapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        messageUserAdapter.setItemOnClickListener(object : MessageUserAdapter.ItemOnClickListener{
            override fun onClick(withUserId: Int) {
                val intent = Intent(context, MessageListActivity::class.java)
                intent.putExtra("withUserId", withUserId)
                startActivity(intent)
            }
        })
    }

    private suspend fun messageList(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData =
                    ServerApiManager.apiService.messageList(token, CachedData.multiUserMessageList.lastMessageId?:0).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.multiUserMessageList.userMessageLists.addAllByWithUserId(commonData.data.userMessageLists)
                        commonData.data.lastMessageId?.let { lastMessageId ->
                            CachedData.multiUserMessageList.lastMessageId = lastMessageId
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class MessageUpdateEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageUpdateEvent(event: MessageUpdateEvent) {
        lifecycleScope.launch {
            CachedData.token?.let { token ->
                messageList(token)
            }
            withContext(Dispatchers.Main) {
                messageUserAdapter.notifyDataSetChanged()
            }
            EventBus.getDefault().post(MessageListActivity.MessageUpdateEvent(true))
        }
    }

    class AdapterUpdateEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAdapterUpdateEventEvent(event: AdapterUpdateEvent) {
        messageUserAdapter.notifyDataSetChanged()
    }

    class MessageUserAdapter(data: MutableList<ServerApiManager.UserMessageList>):
        BaseQuickAdapter<ServerApiManager.UserMessageList, BaseViewHolder>(R.layout.item_message_user, data) {

        interface ItemOnClickListener {
            fun onClick(withUserId: Int)
        }
        private var itemOnClickListener: ItemOnClickListener? = null

        fun setItemOnClickListener(listener: ItemOnClickListener) {
            itemOnClickListener = listener
        }
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.UserMessageList) {
            if(holder.layoutPosition == 0) {
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = dpToPx(6)
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = dpToPx(66)
                holder.itemView.layoutParams = layoutParams
            }
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            val textLastMessage = holder.getView<TextView>(R.id.text_last_message)

            textUserNickname.text = item.userNickname
            val content = item.messages.lastOrNull()?.textContent?:"[图片]"
            textLastMessage.text = content

            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                .into(imgUserAvatar)

            holder.itemView.setOnClickListener {
                itemOnClickListener?.onClick(item.withUserId)
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}

private fun MutableList<ServerApiManager.UserMessageList>.addAllByWithUserId(userMessageLists: MutableList<ServerApiManager.UserMessageList>) {
    for (addUserMessageList in userMessageLists) {
        val existingMessageList = this.find { it.withUserId  == addUserMessageList.withUserId }
        existingMessageList?.let {
            existingMessageList.messages.addAll(addUserMessageList.messages)
            existingMessageList.lastMessageId = addUserMessageList.lastMessageId
        } ?: run {
            this.add(addUserMessageList)
        }

        this.sortByDescending {
            it.lastMessageId
        }
    }
}
