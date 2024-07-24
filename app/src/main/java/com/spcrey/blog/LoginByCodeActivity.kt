package com.spcrey.blog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.CodeTimer
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class LoginByCodeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginByCodeActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_by_code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
        val editTextPhoneNumber = findViewById<EditText>(R.id.editText_phone_number)
        val editTextCode = findViewById<EditText>(R.id.editText_code)
        val btnCodeGet = findViewById<TextView>(R.id.btn_code_get)
        val btnLogin = findViewById<View>(R.id.btn_login)
        var phoneNumber: String? = null
        var code: String? = null

        val btnLoginByPassword = findViewById<TextView>(R.id.btn_login_by_password)
        btnLoginByPassword.setOnClickListener {
            val intent = Intent(this, LoginByPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        CodeTimer.setListener(object : CodeTimer.Listener{
            @SuppressLint("SetTextI18n")
            override suspend fun resumeTimeChange(resumeTime: Int) {
                withContext(Dispatchers.Main) {
                    btnCodeGet.text = "${resumeTime}s后可重新获取"
                    btnCodeGet.alpha = 0.5f
                    btnCodeGet.isEnabled = false
                }
            }
            override suspend fun complete() {
                withContext(Dispatchers.Main) {
                    btnCodeGet.text = getString(R.string.text_code_text)
                    btnCodeGet.alpha = 1.0f
                    btnCodeGet.isEnabled = true
                }
            }
        })

        btnCodeGet.setOnClickListener {
            phoneNumber?.let {
                CodeTimer.start()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            ServerApiManager.apiService.userSendSms(ServerApiManager.UserSendSmsForm(phoneNumber!!)).await().data
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginByCodeActivity, "验证码发送成功", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "request failed: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginByCodeActivity, "验证码发送失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } ?: run {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            phoneNumber?.let { phoneNumber->
                code?.let { code->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val commonData = ServerApiManager.apiService.userLoginByCode(ServerApiManager.UserLoginByCodeForm(
                                    phoneNumber, code
                                )).await()
                                when(commonData.code) {
                                    1 -> {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@LoginByCodeActivity, "登陆成功", Toast.LENGTH_SHORT).show()
                                        }
                                        EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                                        val edit = sharedPreferences.edit()
                                        CachedData.token = commonData.data
                                        Log.d(TAG, "token: ${commonData.data}")
                                        edit.putString("token", commonData.data)
                                        edit.apply()
                                        finish()
                                    } else -> {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@LoginByCodeActivity, "验证码错误", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "request failed: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@LoginByCodeActivity, "请求异常", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }?: run {
                    Toast.makeText(this, "验证码格式不正确", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        editTextPhoneNumber.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                phoneNumber = if (p0 != null && p0.length==11) {
                    p0.toString()
                } else {
                    null
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        editTextCode.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                code = if (p0 != null && p0.length==6) {
                    p0.toString()
                } else {
                    null
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CodeTimer.clearListener()
    }
}