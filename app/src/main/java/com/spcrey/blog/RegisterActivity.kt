package com.spcrey.blog

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    val textPhoneInput by lazy{
        findViewById<EditText>(R.id.text_phone_input)
    }
    val editTextPassword by lazy{
        findViewById<EditText>(R.id.edit_text_password)
    }
    val editTextPasswordSecond by lazy{
        findViewById<EditText>(R.id.edit_text_password_second)
    }

    data class PhoneNumberPassword(
        val phoneNumber: String, val password: String, val passwordSecond: String
    )

    fun validatePhoneNumberPassword(): PhoneNumberPassword? {
        val phoneNumber = textPhoneInput.text.toString()
        if (phoneNumber == null) {
            return null
        }
        if (phoneNumber.length != 11) {
            return null
        }
        val password = editTextPassword.text.toString()
        if (password == null) {
            return null
        }
        val passwordSecond = editTextPasswordSecond.text.toString()
        if (passwordSecond == null) {
            return null
        }
        if (!password.equals(passwordSecond)) {
            return null
        }
        if (password.length < 8) {
            return null
        }
        if (password.length > 24) {
            return null
        }
        return PhoneNumberPassword(phoneNumber, password, passwordSecond)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnRegister = findViewById<View>(R.id.btn_register)
        btnRegister.setOnClickListener {
            val phoneNumberPassword = validatePhoneNumberPassword()
            if (phoneNumberPassword == null) {
                Toast.makeText(this@RegisterActivity, "手机号或密码格式错误", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val commonData = ServerApiManager.apiService.userRegister(
                                ServerApiManager.UserRegisterForm(
                                    phoneNumberPassword.phoneNumber,
                                    phoneNumberPassword.password,
                                    phoneNumberPassword.passwordSecond
                                )).await()
                            if (commonData.code == 1) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "注册成功",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                finish()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "注册失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }catch (e: Exception) {
                            Log.d(TAG, "request failed: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "注册失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}