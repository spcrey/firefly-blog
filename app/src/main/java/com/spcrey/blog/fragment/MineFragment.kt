package com.spcrey.blog.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.spcrey.blog.LoginByCodeActivity
import com.spcrey.blog.R
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

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        CachedData.userInfo?.let { it ->
            if (it.nickname == null) {
                textNickname.text = "未命名"
            } else {
                textNickname.text = it.nickname
            }
            textFanNum.text = it.phoneNumber
        }

        btnLogin.setOnClickListener {
            val intent = Intent(context, LoginByCodeActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginEvent(event:  LoginEvent) {
        CachedData.userInfo?.let { it ->
            if (it.nickname == null) {
                textNickname.text = "未命名"
            } else {
                textNickname.text = it.nickname
            }
            textFanNum.text = it.phoneNumber
        }
    }

    class LoginEvent
}