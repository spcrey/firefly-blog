package com.spcrey.blog

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UserInfoActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userId = intent.getIntExtra("userId", 0)
        val textNickname = findViewById<TextView>(R.id.text_nickname)
        val textFanNum = findViewById<TextView>(R.id.text_fan_num)
        val imgAvatar = findViewById<ImageView>(R.id.img_avatar)
        val textEmail = findViewById<TextView>(R.id.text_email)
        val btnChat = findViewById<TextView>(R.id.btn_chat)
        btnChat.setOnClickListener {
            Toast.makeText(this@UserInfoActivity, "该功能暂未实现", Toast.LENGTH_SHORT).show()
        }
        val btnFollow = findViewById<TextView>(R.id.btn_follow)
        var userInfo: ServerApiManager.User? = null

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val token: String = CachedData.token?:""
                    val commonData = ServerApiManager.apiService.userInfoOther(
                        token, userId
                    ).await()
                    if (commonData.code == 1) {
                        userInfo = commonData.data
                        withContext(Dispatchers.Main) {
                            Glide.with(this@UserInfoActivity)
                                .load(commonData.data.avatarUrl)
                                .transform(CircleCrop())
                                .into(imgAvatar)
                            textNickname.text = userInfo!!.nickname
                            textFanNum.text = userInfo!!.personalSignature
                            textEmail.text =   userInfo!!.email?:""
                            Log.d(TAG, userInfo.toString())
                            if ( userInfo!!.isFollowed==null ||  userInfo!!.isFollowed==false) {
                                btnFollow.text = "点击关注"
                            } else {
                                btnFollow.text = "已关注"
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserInfoActivity, "获取信息失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserInfoActivity, "获取信息失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnFollow.setOnClickListener {
            userInfo?.let { ii ->
                val token: String = CachedData.token?:""
                val isFollowed = ii.isFollowed
                Log.d(TAG, isFollowed.toString())
                if (isFollowed==null || token=="") {
                    Toast.makeText(this@UserInfoActivity, "未登录", Toast.LENGTH_SHORT).show()
                } else if (isFollowed==false) {
                    if (userId == CachedData.user!!.id) {
                        Toast.makeText(this@UserInfoActivity, "不能关注自己呀", Toast.LENGTH_SHORT).show()
                    } else {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val commonData = ServerApiManager.apiService.userFollow(
                                        token, ServerApiManager.UserFollowForm(userId)
                                    ).await()
                                    if (commonData.code == 1) {
                                        userInfo!!.isFollowed = true
                                        withContext(Dispatchers.Main) {
                                            btnFollow.text = "已关注"
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.d(TAG, "error: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val commonData = ServerApiManager.apiService.userUnfollow(
                                    token, ServerApiManager.UserFollowForm(userId)
                                ).await()
                                if (commonData.code == 1) {
                                    userInfo!!.isFollowed = false
                                    withContext(Dispatchers.Main) {
                                        btnFollow.text = "点击关注"
                                    }
                                } else {
                                    Log.d(TAG, "error: ${commonData.message}")
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
}