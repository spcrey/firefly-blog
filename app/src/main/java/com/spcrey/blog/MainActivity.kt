package com.spcrey.blog

import android.content.Context
import android.content.Intent
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
import com.spcrey.blog.fragment.MessageListFragment
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PageActivity"
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
        val icArticleAdd = findViewById<ImageView>(R.id.ic_article_add)

        val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)

        CachedData.token = sharedPreferences.getString("token", null)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                CachedData.token?.let { token ->
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
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val commonData =
                        ServerApiManager.apiService.articleList(CachedData.token, 1, 10).await()
                    when (commonData.code) {
                        1 -> {
                            CachedData.articles.clear()
                            CachedData.articles.addAll(commonData.data.items)
                            EventBus.getDefault().post(HomePageFragment.DataLoadEvent())
                        }

                        else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "参数错误", Toast.LENGTH_SHORT)
                                    .show()
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

        supportFragmentManager.beginTransaction().setReorderingAllowed(true).add(
                R.id.fragment_content, HomePageFragment::class.java, null, TAG
            ).commit()

        bgHomePage.setOnClickListener {
            textTitleBar.text = getString(R.string.title_home)
            icHomePage.alpha = 0.8f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.8f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.2f
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                    R.id.fragment_content, HomePageFragment::class.java, null, "TAG_FRAGMENT"
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
                    R.id.fragment_content, MessageListFragment::class.java, null, "TAG_FRAGMENT"
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
                    R.id.fragment_content, MineFragment::class.java, null, "TAG_FRAGMENT"
                ).commit()
        }

        icArticleAdd.setOnClickListener {
            val intent = Intent(this, ArticleAddActivity::class.java)
            startActivity(intent)
        }
    }
}