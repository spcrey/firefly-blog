package com.spcrey.blog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.HomePageFragment
import com.spcrey.blog.fragment.MessageListFragment
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class PageActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val textTitleBar = findViewById<TextView>(R.id.text_title_bar)
        val bgHomePage = findViewById<View>(R.id.bg_home_page)
        val bgMessageList = findViewById<View>(R.id.bg_message_list)
        val bgMine = findViewById<View>(R.id.bg_mine)
        val icHomePage = findViewById<ImageView>(R.id.ic_home_page)
        val icMessageList = findViewById<ImageView>(R.id.ic_message_list)
        val icMine = findViewById<ImageView>(R.id.ic_mine)
        val textHomePage = findViewById<TextView>(R.id.text_home_page)
        val textMessageList = findViewById<TextView>(R.id.text_message_list)
        val textMine = findViewById<TextView>(R.id.text_mine)
        val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
        val icArticleAdd = findViewById<ImageView>(R.id.ic_article_add)

        icArticleAdd.setOnClickListener {
            val intent = Intent(this, ArticleAddActivity::class.java)
            startActivity(intent)
        }

        CachedData.token = sharedPreferences.getString("token", null)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (CachedData.token != null) {
                    try {
                        CachedData.user = ServerApiManager.apiService.userInfo(CachedData.token!!).await().data
                        Log.d(TAG, "userInfo: ${CachedData.user.toString()}")
                    } catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                    }
                }
            }
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                    try {
                        val token = CachedData.token
                        val commonData = ServerApiManager.apiService.articleList(token, 1, 10).await()
                        CachedData.articles.addAll(commonData.data.items)
                        EventBus.getDefault().post(HomePageFragment.DataLoadEvent())
                        Log.d(TAG, "article: ${CachedData.articles.toString()}")
                    } catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                    }
            }
        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(
                R.id.fragment_content,
                HomePageFragment::class.java,
                null,
                TAG
            ).commit()

        bgHomePage.setOnClickListener {
            textTitleBar.text = "主页"
            icHomePage.alpha = 0.8f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.8f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.2f
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragment_content,
                    HomePageFragment::class.java,
                    null,
                    "TAG_FRAGMENT"
                )
                .commit()
        }
        bgMessageList.setOnClickListener {
            textTitleBar.text = "消息"
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.8f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.8f
            textMine.alpha = 0.2f
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragment_content,
                    MessageListFragment::class.java,
                    null,
                    "TAG_FRAGMENT"
                )
                .commit()
        }
        bgMine.setOnClickListener {
            textTitleBar.text = "我的"
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.8f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.8f
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragment_content,
                    MineFragment::class.java,
                    null,
                    "TAG_FRAGMENT"
                )
                .commit()
        }
    }
}