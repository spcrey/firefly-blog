package com.spcrey.blog

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.fragment.MessageUserListFragment
import com.spcrey.blog.fragment.MineFragment.Status
import com.spcrey.blog.fragment.MineFragment.UserLogoutEvent
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MessageListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MessageListActivity"
    }

    private val textTitleBar by lazy {
        findViewById<TextView>(R.id.text_title_bar)
    }
    private val withUserId by lazy {
        intent.getIntExtra("withUserId", 0)
    }
    private val userMessageList by lazy {
        CachedData.multiUserMessageList.userMessageLists.find {
            it.withUserId == withUserId
        }
    }
    private val messages by lazy {
        userMessageList?.messages ?: mutableListOf()
    }
    private val messageAdapter by lazy {
        MessageAdapter(messages, userMessageList?.userAvatarUrl)
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = StaggeredGridLayoutManager(
                1, StaggeredGridLayoutManager.VERTICAL
            )
        }
    }
    private val rootView by lazy {
        findViewById<ConstraintLayout>(R.id.main)
    }
    private val btnContentSend by lazy {
        findViewById<Button>(R.id.btn_text_send)
    }
    private val icImageSend by lazy {
        findViewById<ImageView>(R.id.ic_image_send)
    }
    private val editTextContent by lazy {
        findViewById<EditText>(R.id.editText_content)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    class MessageUpdateEvent(val isScrollToPosition: Boolean)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageUpdateEvent(event: MessageUpdateEvent) {

//        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
//        val totalItemCount = layoutManager.itemCount

        if (!recyclerView.canScrollVertically(1)) {
            messageAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
        } else {
            messageAdapter.notifyDataSetChanged()
        }

//        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
//        } else {
//        Toast.makeText(this@MessageListActivity,
//            "$lastVisibleItemPosition/$totalItemCount",
//            Toast.LENGTH_SHORT).show()

//        if (lastVisibleItemPosition != totalItemCount - 1) {
//            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
//            messageAdapter.notifyDataSetChanged()
//            recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
//        } else {

//        }
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
        EventBus.getDefault().register(this)

        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)

        var isChange = true

        rootView.viewTreeObserver.addOnGlobalLayoutListener {

            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight: Int = screenHeight - r.bottom

            val layoutParams = icImageSend.layoutParams as? ConstraintLayout.LayoutParams
            layoutParams?.let { params ->
                if (keypadHeight > screenHeight * 0.15) {
                    if (isChange) {
                        isChange = false
                        params.bottomMargin = keypadHeight + dpToPx(12)
                        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                    }
                } else {
                    if (!isChange) {
                        params.bottomMargin = dpToPx(12)
                        isChange = true
                    }

                }
                icImageSend.layoutParams = params
            }
        }

        btnContentSend.setOnClickListener {
            val content = editTextContent.text.toString()
            when {
                content.isEmpty() -> {
//                    Toast.makeText(this@MessageListActivity, "发送内容不能为空", Toast.LENGTH_SHORT).show()
                    EventBus.getDefault().post(MessageUserListFragment.MessageUpdateEvent())
                }
                content.length > 64 -> {
                    Toast.makeText(this@MessageListActivity, "发送内容不能超过64位", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    CachedData.token?.let { token ->
                        lifecycleScope.launch {
                            messageSendText(token, content, withUserId)
                        }
                    }
                }
            }
        }

        textTitleBar.text = userMessageList?.userNickname

        recyclerView.adapter = messageAdapter
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private suspend fun messageSendText(token: String, textContent: String, receivingUserId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.messageSendText(
                    token, ServerApiManager.MessageSendTextForm(textContent, receivingUserId, CachedData.multiUserMessageList.lastMessageId)
                ).await()
                if (commonData.code == 1) {
                    withContext(Dispatchers.Main) {
                        val addMessages = commonData.data.userMessageLists.find{
                            it.withUserId == withUserId
                        }?.messages ?: mutableListOf()
                        val positionStart = messages.size
                        messages.addAll(addMessages)
                        CachedData.multiUserMessageList.lastMessageId = commonData.data.lastMessageId
                        messageAdapter.notifyItemRangeInserted(positionStart, addMessages.size)
                        editTextContent.text.clear()
                        recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                        EventBus.getDefault().post(MessageUserListFragment.AdapterUpdateEvent()
                        )
                    }

                } else {
                    Toast.makeText(this@MessageListActivity, "参数错误", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessageListActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class MessageAdapter(
        data: MutableList<ServerApiManager.Message>,
        private val userAvatarUrl: String?
    ):
        BaseQuickAdapter<ServerApiManager.Message, BaseViewHolder>(R.layout.item_message, data) {

        private fun createImgUserAvatar(item: ServerApiManager.Message): ImageView{
            val imgUserAvatar = ImageView(context).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(dpToPx(36), dpToPx(36))
                setImageResource(R.drawable.bg_white_r4dp)
            }
            val layoutParams = imgUserAvatar.layoutParams as ConstraintLayout.LayoutParams
            if (item.isSendingUser) {
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                Glide.with(context)
                    .load(CachedData.user?.avatarUrl)
                    .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                    .into(imgUserAvatar)
            } else {
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                Glide.with(context)
                    .load(userAvatarUrl)
                    .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                    .into(imgUserAvatar)
            }
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgUserAvatar.layoutParams = layoutParams
            return imgUserAvatar
        }

        private fun createTextContent(item: ServerApiManager.Message, imgUserAvatar: ImageView): TextView {
            val textContent = TextView(context).apply {
                id = View.generateViewId()
                text = item.textContent
                gravity = Gravity.CENTER_VERTICAL
                textSize = 14f
                setLineSpacing(0f, 1.3f)
                layoutParams = ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                maxWidth = ((context.resources.displayMetrics.widthPixels - dpToPx(132)))
                setPadding(dpToPx(12),dpToPx(9),dpToPx(12),dpToPx(9))
            }
            val layoutParams = textContent.layoutParams as ConstraintLayout.LayoutParams
            if (item.isSendingUser) {
                layoutParams.marginEnd = dpToPx(48)
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                textContent.background = ContextCompat.getDrawable(context, R.drawable.btn_light_blue_r4dp)
                textContent.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                layoutParams.marginStart = dpToPx(48)
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                textContent.background = ContextCompat.getDrawable(context, R.drawable.bg_white_10_r4dp)
                textContent.setTextColor(ContextCompat.getColor(context, R.color.white_80))
            }
            layoutParams.topToTop = imgUserAvatar.id
            layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            textContent.layoutParams = layoutParams
            return textContent
        }


        override fun convert(holder: BaseViewHolder, item: ServerApiManager.Message) {
            val constraintLayout = holder.itemView as ConstraintLayout
            constraintLayout.removeAllViews()

            val imgUserAvatar = createImgUserAvatar(item)
            constraintLayout.addView(imgUserAvatar)

            item.imageUrl?.let {
                val imgImage: ImageView = createImgImage(item)
                constraintLayout.addView(imgImage)
            } ?: let {
                val textContent = createTextContent(item, imgUserAvatar)
                constraintLayout.addView(textContent)
            }
        }

        private fun createImgImage(item: ServerApiManager.Message): ImageView {
            val imgImage = ImageView(context).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(dpToPx(180), dpToPx(120))
                setImageResource(R.drawable.bg_white_r4dp)
            }
            val layoutParams = imgImage.layoutParams as ConstraintLayout.LayoutParams
            Glide.with(context)
                .load(item.imageUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                .into(imgImage)
            if (item.isSendingUser) {
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.marginEnd = dpToPx(48)
            } else {
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.marginStart = dpToPx(48)
            }
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            imgImage.layoutParams = layoutParams
            return imgImage
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }
}