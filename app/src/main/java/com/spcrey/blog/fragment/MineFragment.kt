package com.spcrey.blog.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.LoginByCodeActivity
import com.spcrey.blog.R
import com.spcrey.blog.UpdateInfoActivity
import com.spcrey.blog.tools.CachedData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MineFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MineFragment : Fragment() {

    companion object {
        private const val TAG = "MineFragment"
    }

    enum class Status {
        LOGIN, NOT_LOGIN
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var status = Status.NOT_LOGIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }
    private lateinit var view: View

    private val btnLogin by lazy {
        view.findViewById<View>(R.id.btn_login)
    }

    private val imageAvatar by lazy {
        view.findViewById<ImageView>(R.id.img_avatar)
    }

    private val textLogin by lazy {
        view.findViewById<TextView>(R.id.text_login)
    }

    private val textNickname by lazy {
        view.findViewById<TextView>(R.id.text_nickname)
    }
    private val textFanNum  by lazy {
        view.findViewById<TextView>(R.id.text_fan_num)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view
        EventBus.getDefault().register(this)
        CachedData.user?.let { it ->
            Glide.with(requireContext())
                .load(it.avatarUrl)
                .transform(CircleCrop())
                .into(imageAvatar)
            if (it.nickname == null) {
                textNickname.text = "未命名"
            } else {
                textNickname.text = it.nickname
            }
            textFanNum.text = it.phoneNumber
            textLogin.text = "修改信息"
            status = Status.LOGIN
        }?: run {
        }

        btnLogin.setOnClickListener {
            when(status) {
                Status.LOGIN -> {
                    val intent = Intent(context, UpdateInfoActivity::class.java)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginEvent(event:  LoginEvent) {
        CachedData.user?.let { it ->
            if (it.nickname == null) {
                textNickname.text = "未命名"
            } else {
                textNickname.text = it.nickname
            }
            textFanNum.text = it.phoneNumber
            Glide.with(requireContext())
                .load(it.avatarUrl)
                .transform(CircleCrop())
                .into(imageAvatar)
        }

        textLogin.text = "修改信息"
        status = Status.LOGIN
    }

    class LoginEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogoutEvent(event:  LogoutEvent) {
        textNickname.text = "请先登录"
        textFanNum.text = "点击头像去登陆"
        textLogin.text = "登录"
        imageAvatar.setImageResource(R.drawable.circle_shape)
        status = Status.NOT_LOGIN
    }

    class LogoutEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun update(event:  Update) {
        CachedData.user?.let { it ->
            if (it.nickname == null) {
                textNickname.text = "未命名"
            } else {
                textNickname.text = it.nickname
            }
            textFanNum.text = it.phoneNumber
        }
    }

    class Update
}