package com.spcrey.blog

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager

class MessageListActivity : AppCompatActivity() {

    private val textTitleBar by lazy {
        findViewById<TextView>(R.id.text_title_bar)
    }
    private val position by lazy {
        intent.getIntExtra("position", 0)
    }
    private val messageAdapter by lazy {
        MessageAdapter(CachedData.multiUserMessageList.userMessageLists[position].messages, position)
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recycler_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textTitleBar.text = CachedData.multiUserMessageList.userMessageLists[position].userNickname

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = messageAdapter
    }

    class MessageAdapter(data: MutableList<ServerApiManager.Message>, private val position: Int):
        BaseQuickAdapter<ServerApiManager.Message, BaseViewHolder>(R.layout.item_message, data) {
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.Message) {
            val constraintLayout = holder.itemView as ConstraintLayout
            if(holder.layoutPosition == 0) {
                val layoutParams = constraintLayout.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = dpToPx(12)
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                constraintLayout.layoutParams = layoutParams
            }
            constraintLayout.removeAllViews()

            val imgUserAvatar = ImageView(context).apply {
                id = View.generateViewId()
            }
            imgUserAvatar.setImageResource(R.drawable.bg_white_r4dp)
            imgUserAvatar.layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(36),
                dpToPx(36)
            )
            var layoutParams = imgUserAvatar.layoutParams as ConstraintLayout.LayoutParams
            if (item.isSendingUser) {
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                Glide.with(context)
                    .load(CachedData.user?.avatarUrl)
                    .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                    .into(imgUserAvatar)
            } else {
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                Glide.with(context)
                    .load(CachedData.multiUserMessageList.userMessageLists[position].userAvatarUrl)
                    .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                    .into(imgUserAvatar)
            }
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgUserAvatar.layoutParams = layoutParams
            constraintLayout.addView(imgUserAvatar)

            val textContent = TextView(context).apply {
                id = View.generateViewId()
            }
            textContent.text = item.textContent
            textContent.setTextColor(ContextCompat.getColor(context, R.color.white))
            textContent.gravity = Gravity.CENTER_VERTICAL
            textContent.textSize = 14f
            textContent.setLineSpacing(0f, 1.3f)
            textContent.layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val maxWidth = ((context.resources.displayMetrics.widthPixels - dpToPx(132)))
            textContent.maxWidth = maxWidth
            textContent.setPadding(dpToPx(12),dpToPx(9),dpToPx(12),dpToPx(9))
            layoutParams = textContent.layoutParams as ConstraintLayout.LayoutParams
            if (item.isSendingUser) {
                layoutParams.marginEnd = dpToPx(48)
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                textContent.background = ContextCompat.getDrawable(context, R.drawable.btn_light_blue_r4dp)
            } else {
                layoutParams.marginStart = dpToPx(48)
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                textContent.background = ContextCompat.getDrawable(context, R.drawable.bg_white_10_r4dp)
            }

            layoutParams.topToTop = imgUserAvatar.id
            textContent.layoutParams = layoutParams

            constraintLayout.addView(textContent)
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }
}