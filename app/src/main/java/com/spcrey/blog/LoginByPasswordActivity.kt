package com.spcrey.blog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class LoginByPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginByPasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_by_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)

        val textPhoneInput = findViewById<EditText>(R.id.text_phone_input)
        val editTextPassword = findViewById<EditText>(R.id.edit_text_password)
        val btnLogin = findViewById<View>(R.id.btn_login)
        val btnToRegister = findViewById<View>(R.id.btn_to_register)
        val btnToLoginByCode = findViewById<View>(R.id.btn_to_login_by_code)

        var phone: String? = null
        var password: String? = null

        btnToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnToLoginByCode.setOnClickListener {
            val intent = Intent(this, LoginByCodeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener{
            if (phone != null && password!= null) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val commonData = ServerApiManager.apiService.userLoginByPassword(ServerApiManager.UserLoginByPasswordForm(phone!!, password!!)).await()
                            if (commonData.code == 1) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@LoginByPasswordActivity, "登陆成功", Toast.LENGTH_SHORT).show()
                                    CachedData.token = commonData.data
                                    val edit = sharedPreferences.edit()
                                    edit.putString("token", commonData.data)
                                    edit.apply()
                                    withContext(Dispatchers.IO) {
                                        try {
                                            CachedData.user = ServerApiManager.apiService.userInfo(CachedData.token!!).await().data
                                            Log.d(TAG, "userInfo: ${CachedData.user.toString()}")
                                            EventBus.getDefault().post(MineFragment.LoginEvent())
                                            withContext(Dispatchers.Main) {
                                                finish()
                                            }

                                        } catch (e: Exception) {
                                            Log.d(TAG, "request failed: ${e.message}")
                                        }
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@LoginByPasswordActivity, "密码错误", Toast.LENGTH_SHORT).show()
                                }
                            }

                        } catch (e: Exception) {
                            Log.d(TAG, "request failed: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@LoginByPasswordActivity, "登录失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "手机号或密码格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        textPhoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length==11) {
                    phone = p0.toString()
                } else {
                    phone = null
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
        editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length >= 8 && p0.length <= 24) {
                    password = p0.toString()
                } else {
                    password = null
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
    }
}