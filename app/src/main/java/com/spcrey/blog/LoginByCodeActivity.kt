package com.spcrey.blog

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
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.CodeTimer
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginByCodeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginByCodeActivity"
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
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        val textPhoneInput = findViewById<EditText>(R.id.text_phone_input)
        val textCodeInput = findViewById<EditText>(R.id.text_code_input)
        val btnCodeGet = findViewById<TextView>(R.id.btn_code_get)
        val btnLogin = findViewById<View>(R.id.btn_login)
        var phone: String? = null
        var code: String? = null
        val btnLoginByPassword = findViewById<TextView>(R.id.btn_login_by_password)
        btnLoginByPassword.setOnClickListener {
            val intent = Intent(this, LoginByPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnCodeGet.setOnClickListener {
            if (phone == null) {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            } else {

                class CodeTimerListener: CodeTimer.Listener {
                    override suspend fun resumeTimeChange(resumeTime: Int) {
                        withContext(Dispatchers.Main) {
                            btnCodeGet.text = "${resumeTime}s后可重新获取"
                            btnCodeGet.alpha = 0.5f
                            btnCodeGet.isEnabled = false
                        }
                    }

                    override suspend fun complete() {
                        withContext(Dispatchers.Main) {
                            btnCodeGet.text = "获取验证码"
                            btnCodeGet.alpha = 1.0f
                            btnCodeGet.isEnabled = true
                        }
                    }
                }

                CodeTimer.listener = CodeTimerListener()
                CodeTimer.start()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            ServerApiManager.apiService.userSendSms(ServerApiManager.UserSendSmsForm(phone!!)).await().data
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
            }
        }
        btnLogin.setOnClickListener {
            if (phone == null || code == null) {
                Toast.makeText(this, "手机号或验证码格式不正确", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val commonData = ServerApiManager.apiService.userLoginByCode(ServerApiManager.UserLoginByCodeForm(
                                phone!!, code!!
                            )).await()
                            if (commonData.code == 1) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@LoginByCodeActivity, "登陆成功", Toast.LENGTH_SHORT).show()
                                }
                                val edit = sharedPreferences.edit()
                                CachedData.token = commonData.data
                                Log.d(TAG, "token: ${commonData.data}")
                                edit.putString("token", commonData.data)
                                edit.apply()
                                finish()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@LoginByCodeActivity, "验证码错误", Toast.LENGTH_SHORT).show()
                                }
                                Log.d(TAG, "验证码错误")
                            }

                        } catch (e: Exception) {
                            Log.d(TAG, "request failed: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginByCodeActivity, "登录失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        textPhoneInput.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length==11) {
                    phone = p0.toString()
                    btnCodeGet.isEnabled = true;
                } else {
                    phone = null
                    btnCodeGet.isEnabled = false;
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
        textCodeInput.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length==6) {
                    code = p0.toString()
                } else {
                    code = null
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CodeTimer.listener = null
    }
}