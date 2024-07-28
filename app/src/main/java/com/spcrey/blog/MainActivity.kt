package com.spcrey.blog

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.HomePageFragment
import com.spcrey.blog.fragment.MessageUserListFragment
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val textTitleBar by lazy {
        findViewById<TextView>(R.id.text_title_bar)
    }
    private val bgHomePage by lazy {
        findViewById<View>(R.id.bg_home_page)
    }
    private val bgMessageList by lazy {
        findViewById<View>(R.id.bg_message_list)
    }
    private val bgMine by lazy {
        findViewById<View>(R.id.bg_mine)
    }
    private val icHomePage by lazy {
        findViewById<ImageView>(R.id.ic_home_page)
    }
    private val icMessageList by lazy {
        findViewById<ImageView>(R.id.ic_message_list)
    }
    private val icMine by lazy {
        findViewById<ImageView>(R.id.ic_mine)
    }
    private val textHomePage by lazy {
        findViewById<TextView>(R.id.text_home_page)
    }
    private val textMessageList by lazy {
        findViewById<TextView>(R.id.text_message_list)
    }
    private val textMine by lazy {
        findViewById<TextView>(R.id.text_mine)
    }

    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MessageReceiving.setListener(object : MessageReceiving.Listener{
            override suspend fun countComplete() {
                withContext(Dispatchers.Main) {
                    EventBus.getDefault().post(MessageUserListFragment.MessageUpdateEvent())
                }
            }
        })

        MessageReceiving.run()
        CachedData.token = sharedPreferences.getString("token", null)

        CachedData.token?.let { token ->
            lifecycleScope.launch {
                userInfo(token)
            }
            lifecycleScope.launch {
                messageList(token)
            }
        }
        lifecycleScope.launch {
            refreshArticleList()
        }

        supportFragmentManager.beginTransaction().setReorderingAllowed(true).add(
            R.id.fragment_content, HomePageFragment::class.java, null, TAG
        ).commit()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                delay(100)
                EventBus.getDefault().post(HomePageFragment.ArticleAdapterUpdateEvent())
            }
        }


        bgHomePage.setOnClickListener {
            textTitleBar.text = getString(R.string.title_home)
            icHomePage.alpha = 0.8f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.8f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.2f
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, HomePageFragment::class.java, null, TAG
            ).commit()
        }

        bgMessageList.setOnClickListener {
            textTitleBar.text = getString(R.string.title_message_list)
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.8f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.8f
            textMine.alpha = 0.2f
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, MessageUserListFragment::class.java, null, TAG
            ).commit()
        }

        bgMine.setOnClickListener {
            textTitleBar.text = getString(R.string.title_mine)
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.8f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.8f
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, MineFragment::class.java, null, TAG
            ).commit()
        }
    }

    private suspend fun userInfo(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userInfo(token).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.user = commonData.data
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity, "参数错误", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun messageList(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData =
                    ServerApiManager.apiService.messageList(token, 0).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.multiUserMessageList.userMessageLists.clear()
                        CachedData.multiUserMessageList.userMessageLists.addAll(commonData.data.userMessageLists)
                        CachedData.multiUserMessageList.lastMessageId = commonData.data.lastMessageId
                        Log.d(TAG, CachedData.multiUserMessageList.toString())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun refreshArticleList() {
        withContext(Dispatchers.IO) {
            try {
                val commonData =
                    ServerApiManager.apiService.articleList(CachedData.token, 1, 10).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.articles.clear()
                        CachedData.articles.addAll(commonData.data.items)
                        CachedData.currentPageNum = 1
                        EventBus.getDefault().post(HomePageFragment.ArticleAdapterUpdateEvent())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}