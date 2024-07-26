package com.spcrey.blog.fragment

import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
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
    private val articleAdapter by lazy {
        ArticleAdapter(CachedData.multiUserMessageList.userMessageLists)
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
        recyclerView.adapter = articleAdapter
    }
    class ArticleAdapter(data: MutableList<ServerApiManager.UserMessageList>):
        BaseQuickAdapter<ServerApiManager.UserMessageList, BaseViewHolder>(R.layout.item_chat, data) {
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
            val content = item.messages.lastOrNull()?.textContent?:""
            textLastMessage.text = content

            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                .into(imgUserAvatar)
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }
}