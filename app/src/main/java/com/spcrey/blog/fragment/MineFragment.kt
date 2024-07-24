package com.spcrey.blog.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.LoginByCodeActivity
import com.spcrey.blog.R
import com.spcrey.blog.InfoModifyActivity
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MineFragment : Fragment() {
    companion object {
        private const val TAG = "MineFragment"
    }

    private var status = Status.NOT_LOGIN

    private lateinit var view: View

    private val btnToLogin by lazy {
        view.findViewById<View>(R.id.btn_to_login)
    }
    private val imgUserAvatar by lazy {
        view.findViewById<ImageView>(R.id.img_user_avatar)
    }
    private val textToLogin by lazy {
        view.findViewById<TextView>(R.id.text_to_login)
    }
    private val textUserNickname by lazy {
        view.findViewById<TextView>(R.id.text_nickname_title)
    }
    private val textUserPhoneNumber by lazy {
        view.findViewById<TextView>(R.id.text_user_phone_number)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view
        EventBus.getDefault().register(this)

        onUserLoginEvent(UserLoginEvent())

        btnToLogin.setOnClickListener {
            when (status) {
                Status.LOGIN -> {
                    val intent = Intent(context, InfoModifyActivity::class.java)
                    startActivity(intent)
                }
                Status.NOT_LOGIN -> {
                    val intent = Intent(context, LoginByCodeActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private suspend fun userInfo(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userInfo(token).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.user = commonData.data
                        withContext(Dispatchers.Main) {
                            onUserLoginEvent(UserLoginEvent())
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context, "参数错误", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserInfoUpdateEvent(event: UserInfoUpdateEvent) {
        CachedData.token?.let { token ->
            lifecycleScope.launch {
                userInfo(token)
            }
        }
    }

    class UserInfoUpdateEvent

    private fun onUserLoginEvent(event: UserLoginEvent) {
        CachedData.user?.let { user ->
            Glide.with(requireContext()).load(user.avatarUrl).transform(CircleCrop())
                .into(imgUserAvatar)
            imgUserAvatar.alpha = 1f
            textUserNickname.text = user.nickname
            textUserPhoneNumber.text = user.phoneNumber
            status = Status.LOGIN
            textToLogin.text = "去修改信息"
        }
    }

    class UserLoginEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserLogoutEvent(event: UserLogoutEvent) {
        textUserNickname.text = getString(R.string.text_not_login)
        textUserPhoneNumber.text = getString(R.string.text_prompt_login)
        textToLogin.text = getString(R.string.text_to_login)
        imgUserAvatar.setImageResource(R.drawable.bg_circle_white)
        imgUserAvatar.alpha = 0.6f
        status = Status.NOT_LOGIN
    }

    class UserLogoutEvent

    enum class Status {
        LOGIN, NOT_LOGIN
    }
}