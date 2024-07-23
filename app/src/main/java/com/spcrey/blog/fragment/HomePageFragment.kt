package com.spcrey.blog.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.CommentActivity
import com.spcrey.blog.R
import com.spcrey.blog.RegisterActivity
import com.spcrey.blog.UserInfoActivity
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.w3c.dom.Text

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomePageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomePageFragment : Fragment() {

    companion object {
        private const val TAG = "HomePageFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    val articleAdapter by lazy {
        ArticleAdapter(CachedData.articles)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().register(this)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val token: String = CachedData.token?: ""
                        CachedData.currentPageNum = 1
                        val commonData = ServerApiManager.apiService.articleList(
                            token,
                            ServerApiManager.ArticleListForm(10, CachedData.currentPageNum)
                        ).await()
                        CachedData.articles.clear()
                        CachedData.articles.addAll(commonData.data.items)
                        CachedData.articles.shuffle()
                        withContext(Dispatchers.Main) {
                            articleAdapter.notifyDataSetChanged()
                            swipeRefreshLayout.isRefreshing = false
                            articleAdapter.loadMoreModule.loadMoreComplete()
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "error: ${e.message}")
                        withContext(Dispatchers.Main) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }

        articleAdapter.loadMoreModule.isAutoLoadMore = true
        articleAdapter.loadMoreModule.isEnableLoadMoreIfNotFullPage = true
        articleAdapter.loadMoreModule.setOnLoadMoreListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val token: String = CachedData.token?: ""
                    try {
                        delay(1000)
                        val commonData = ServerApiManager.apiService.articleList(
                            token,
                            ServerApiManager.ArticleListForm(10, CachedData.currentPageNum + 1)
                        ).await()
                        if (commonData.code == 1) {
                            if (commonData.data.items.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    CachedData.articles.addAll(commonData.data.items)
                                    articleAdapter.notifyDataSetChanged()
                                    articleAdapter.loadMoreModule.loadMoreComplete()
                                }
                                CachedData.currentPageNum += 1
                            } else {
                                withContext(Dispatchers.Main) {
                                    articleAdapter.loadMoreModule.loadMoreEnd()
                                }
                            }
                        }
                    } catch (e: Exception){
                        Log.d(TAG, "error: ${e.message}")
                    }
                }
            }
        }
        recyclerView.adapter = articleAdapter

        articleAdapter.setUserOnClickListener(object : ArticleAdapter.UserOnClickListener{
            override fun onClick(userId: Int) {

                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
        })

        articleAdapter.setIcCommentOnClickListener(object : ArticleAdapter.IcCommentOnClickListener{
            override fun onClick(id: Int) {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("id", id)
                startActivity(intent)
            }
        })

        articleAdapter.setIcLikeOnClickListener(object : ArticleAdapter.IcLikeOnClickListener{
            override fun onClick(id: Int, position: Int, status: Boolean?) {
                if (status == null) {
                    Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                } else if (status==true) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val commonData = ServerApiManager.apiService.articleUnlike(
                                    CachedData.token!!,
                                    ServerApiManager.ArticleLikeForm(id)
                                ).await()
                                if (commonData.code==1) {
                                    withContext(Dispatchers.Main) {
                                        val data = articleAdapter.getItem(position)
                                        data.likeStatus = false
                                        data.likeCount -= 1
                                        articleAdapter.notifyDataSetChanged()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "request failed: ${e.message}")
                            }
                        }
                    }
                }else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val commonData = ServerApiManager.apiService.articleLike(
                                    CachedData.token!!,
                                    ServerApiManager.ArticleLikeForm(id)
                                ).await()
                                if (commonData.code==1) {
                                    withContext(Dispatchers.Main) {
                                        val data = articleAdapter.getItem(position)
                                        data.likeStatus = true
                                        data.likeCount += 1
                                        articleAdapter.notifyDataSetChanged()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "request failed: ${e.message}")
                            }
                        }
                    }
                }
            }
        })
    }

    class ArticleAdapter(data: MutableList<ServerApiManager.Article>):
        BaseQuickAdapter<ServerApiManager.Article, BaseViewHolder>(R.layout.item_article, data),
        LoadMoreModule {

        fun Context.dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        interface IcLikeOnClickListener {
            fun onClick(id: Int, position: Int, status: Boolean?)
        }

        interface IcCommentOnClickListener {
            fun onClick(id: Int)
        }

        interface UserOnClickListener {
            fun onClick(userId: Int)
        }

        private var icLikeOnClickListener: IcLikeOnClickListener? = null

        private var icCommentOnClickListener: IcCommentOnClickListener? = null

        private var userOnClickListener: UserOnClickListener? = null

        fun setIcLikeOnClickListener(listener: IcLikeOnClickListener) {
            icLikeOnClickListener = listener
        }

        fun setIcCommentOnClickListener(listener: IcCommentOnClickListener) {
            icCommentOnClickListener = listener
        }

        fun setUserOnClickListener(listener: UserOnClickListener) {
            userOnClickListener = listener
        }

        override fun convert(holder: BaseViewHolder, item: ServerApiManager.Article) {
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CircleCrop())
                .into(imgUserAvatar)
            imgUserAvatar.setOnClickListener {
                userOnClickListener?.onClick(item.userId)
            }
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            textUserNickname.setOnClickListener {
                userOnClickListener?.onClick(item.userId)
            }
            val textContent = holder.getView<TextView>(R.id.text_content)
            textUserNickname.text = item.userNickname
            textContent.text = item.content
            val icLike = holder.getView<ImageView>(R.id.ic_like)
            if (item.likeStatus != null && item.likeStatus == true) {
                icLike.setImageResource(R.drawable.ic_like)
            } else {
                icLike.setImageResource(R.drawable.ic_nolike)
            }
            val bgLike = holder.getView<View>(R.id.bg_like)
            bgLike.setOnClickListener {
                icLikeOnClickListener?.onClick(item.id, holder.layoutPosition, item.likeStatus)
            }
            val textLikeCount = holder.getView<TextView>(R.id.text_like_count)
            if (item.likeCount > 0) {
                textLikeCount.text = item.likeCount.toString()
            } else {
                textLikeCount.text = "点赞"
            }
            val bgComment = holder.getView<View>(R.id.bg_comment)
            bgComment.setOnClickListener {
                icCommentOnClickListener?.onClick(item.id)
            }
            val textCommentCount = holder.getView<TextView>(R.id.text_comment_count)
            if (item.commentCount > 0) {
                textCommentCount.text = item.commentCount.toString()
            } else {
                textCommentCount.text = "评论"
            }
            val fragmentImages = holder.getView<FrameLayout>(R.id.fragment_images)
            fragmentImages.removeAllViews()
            val imageUrls = item.imageUrls
            if (imageUrls.size == 1) {
                val imageUrl = imageUrls[0]
                val img = ImageView(context).apply {
                    id = View.generateViewId()
                }
                val height = context.dpToPx(216)
                val layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    height
                )
                layoutParams.topMargin = context.dpToPx(9)
                img.layoutParams = layoutParams
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .transform(CenterCrop(), RoundedCorners(context.dpToPx(4)))
                    .into(img)
                fragmentImages.addView(img)
            }

            if (imageUrls.size > 1) {
                val gridLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                gridLayoutParams.topMargin = context.dpToPx(12)
                val gridLayout = GridLayout(context).apply {
                    layoutParams = gridLayoutParams
                    columnCount = 3
                }
                for (i in 0..<item.imageUrls.size) {
                    val imageView = ImageView(context).apply {
                        id = View.generateViewId()
                    }
                    val length = ((context.resources.displayMetrics.widthPixels - context.dpToPx(64)) / 3).toInt()
                    val layoutParams = ConstraintLayout.LayoutParams(length, length)
                    if (i%3 != 0) {
                        layoutParams.marginStart = context.dpToPx(9)
                    }
                    if (i >= 3) {
                        layoutParams.topMargin = context.dpToPx(9)
                    }
                    imageView.layoutParams = layoutParams

                    Glide.with(context)
                        .asBitmap()
                        .load(item.imageUrls[i])
                        .transform(CenterCrop(), RoundedCorners(context.dpToPx(4)))
                        .into(imageView)
                    gridLayout.addView(imageView)
                }
                fragmentImages.addView(gridLayout)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDataLoad(event:  DataLoadEvent) {
        articleAdapter.notifyDataSetChanged()
    }

    class DataLoadEvent

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}