package com.spcrey.blog.fragment

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

class MessageListFragment : Fragment() {

    companion object {
        private const val TAG = "MainActivity"
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
        return inflater.inflate(R.layout.fragment_message_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = messageUserAdapter

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
        }

        messageUserAdapter.setItemOnClickListener(object : MessageUserAdapter.ItemOnClickListener{
            override fun onClick(position: Int) {
                val intent = Intent(context, MessageListActivity::class.java)
                intent.putExtra("position", position)
                startActivity(intent)
            }
        })
    }

    class MessageUserAdapter(data: MutableList<ServerApiManager.UserMessageList>):
        BaseQuickAdapter<ServerApiManager.UserMessageList, BaseViewHolder>(R.layout.item_message_user, data) {

        interface ItemOnClickListener {
            fun onClick(position: Int)
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
                itemOnClickListener?.onClick(holder.layoutPosition)
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }
}