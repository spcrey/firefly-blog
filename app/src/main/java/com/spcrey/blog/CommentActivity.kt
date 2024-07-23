package com.spcrey.blog

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommentActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CommentActivity"
    }

    fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private val comments: MutableList<ServerApiManager.ArticleComment> = mutableListOf()

    private val commentAdapter by lazy {
        CommentAdapter(comments)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val id = intent.getIntExtra("id", 0)
        val rootView = findViewById<ConstraintLayout>(R.id.main)
        val btnSend = findViewById<TextView>(R.id.btn_send)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height

            // 计算键盘的高度
            val keypadHeight: Int = screenHeight - r.bottom

            // 如果键盘弹出
            if (keypadHeight > screenHeight * 0.15) { // 判断键盘高度是否大于屏幕高度的15%
                // 调整底部视图的约束条件，使其底部与键盘顶部对齐
                val params = btnSend.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = keypadHeight // 根据需要调整底部的 margin
                btnSend.layoutParams = params
            } else {
                // 键盘隐藏时，恢复原始约束条件
                val params = btnSend.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = dpToPx(12) // 恢复为原始的 margin
                btnSend.layoutParams = params
            }
        }

        btnSend.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val token = CachedData.token
                        val editTextContent = findViewById<EditText>(R.id.bg_comment_send)
                        val content = editTextContent.text.toString()
                        Log.d(TAG, content)
                        if (token != null && content!="") {
                            val commonData = ServerApiManager.apiService.articleComment(
                                token, ServerApiManager.ArticleCommentsForm(content, id)
                            ).await()
                            Log.d(TAG, commonData.toString())
                            if (commonData.code==1) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@CommentActivity,
                                        "发布成功",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    editTextContent.text.clear()
                                }
                                if (id != 0) {
                                    val commonDataComments = ServerApiManager.apiService.articleListComments(
                                        id
                                    ).await()
                                    if (commonDataComments.code == 1) {
                                        comments.clear()
                                        comments.addAll(commonDataComments.data)
                                        Log.d(TAG, comments.toString())
                                        withContext(Dispatchers.Main) {
                                            commentAdapter.notifyDataSetChanged()
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@CommentActivity,
                                        "发布失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@CommentActivity, "请先登录", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (e: Exception) {
                        Log.d(TAG, "error: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@CommentActivity,
                                "发布失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }


        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = commentAdapter
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val token = CachedData.token
                    if (id != 0) {
                        val commonData = ServerApiManager.apiService.articleListComments(
                            id
                        ).await()
                        if (commonData.code == 1) {
                            comments.addAll(commonData.data)
                            Log.d(TAG, comments.toString())
                            withContext(Dispatchers.Main) {
                                commentAdapter.notifyDataSetChanged()
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.d(TAG, "error: ${e.message}")
                }
            }
        }
    }

    class CommentAdapter(data: MutableList<ServerApiManager.ArticleComment>):
        BaseQuickAdapter<ServerApiManager.ArticleComment, BaseViewHolder>(R.layout.item_comment, data) {
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.ArticleComment) {
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CircleCrop())
                .into(imgUserAvatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            textUserNickname.text = item.userNickname
            val textContent = holder.getView<TextView>(R.id.text_content)
            textContent.text = item.content
        }
    }
}