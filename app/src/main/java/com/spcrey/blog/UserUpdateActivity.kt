package com.spcrey.blog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
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
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class UserUpdateActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UpdateInfoActivity"
        private const val PICK_IMAGE_REQUEST = 1
    }

    private val imageAvatar by lazy {
        findViewById<ImageView>(R.id.img_avatar)
    }
    private val editTextNickname by lazy {
        findViewById<TextView>(R.id.editText_nickname)
    }
    private val editTextEmail by lazy {
        findViewById<EditText>(R.id.editText_email)
    }
    private val editTextPersonalSignature by lazy {
        findViewById<EditText>(R.id.editText_personal_signature)
    }
    private val btnUpdateInfo by lazy {
        findViewById<View>(R.id.btn_update_info)
    }
    private val btnLogout by lazy {
        findViewById<View>(R.id.btn_logout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_update)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)

        CachedData.user?.let { user ->
            Glide.with(this)
                .load(user.avatarUrl)
                .transform(CircleCrop())
                .into(imageAvatar)
            editTextNickname.text = user.nickname
            editTextEmail.setText(user.email)
            editTextPersonalSignature.setText(user.personalSignature)
        }

        imageAvatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        btnUpdateInfo.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val commonDataValidateEditText = validateEditText()
                    when (commonDataValidateEditText.code) {
                        1 -> {
                            try {
                                CachedData.token?.let { token ->
                                    commonDataValidateEditText.data?.let { userUpdateForm ->
                                        val commonDataUserUpdate =
                                            ServerApiManager.apiService.userUpdate(
                                                token, userUpdateForm
                                            ).await()
                                        when (commonDataUserUpdate.code) {
                                            1 -> {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@UserUpdateActivity,
                                                        "信息修改成功",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                                            }
                                            else -> {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        this@UserUpdateActivity,
                                                        "参数错误",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "request failed: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@UserUpdateActivity, "请求异常", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else -> {
                            withContext(Dispatchers.Main){

                                Toast.makeText(
                                    this@UserUpdateActivity,
                                    commonDataValidateEditText.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                    }
                }
            }
        }

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    CachedData.token?.let { token ->
                        try {
                            val commonData = ServerApiManager.apiService.userLogout(token).await()
                            when(commonData.code) {
                                1 -> {
                                    CachedData.token = null
                                    CachedData.user = null
                                    val editor = sharedPreferences?.edit()
                                    editor?.let {
                                        it.remove("token")
                                        it.apply()
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@UserUpdateActivity,
                                            "退出登陆成功",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        EventBus.getDefault().post(MineFragment.UserLogoutEvent())
                                        finish()
                                    }
                                } else -> {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@UserUpdateActivity,
                                            "参数错误",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "request failed: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@UserUpdateActivity,
                                    "请求异常",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateEditText(): ServerApiManager.CommonData<ServerApiManager.UserUpdateForm?> {
        val nickname = editTextNickname.text.toString()
        if (nickname.length !in 3..8) {
            return ServerApiManager.CommonData(0, "昵称需要3到8位", null)
        }
        var email: String? = editTextEmail.text.toString()
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        email?.let {
            if (it.isNotBlank() && !it.matches(emailPattern)) {
                return ServerApiManager.CommonData(0, "邮箱格式不正确", null)
            } else if (it.isBlank()){
                email = null
            }
        }
        val personalSignature = editTextPersonalSignature.text.toString().takeUnless {
            it.isBlank()
        }
        return ServerApiManager.CommonData(1, "输入正确", ServerApiManager.UserUpdateForm(
            nickname, email, personalSignature
        ))
    }

    private fun Context.getRealPath(uri: Uri): String? {
        var filePath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        return filePath
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val filePath = data.data
            val localPath = filePath?.let {
                getRealPath( it)
            }
            val file = localPath?.let {
                File(it)
            }
            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        CachedData.token?.let { token ->
                            val commonData =
                                ServerApiManager.apiService.userUpdateAvatar(
                                    token,
                                    ServerApiManager.UserUpdateAvatarForm(fileBase64)).await()
                            when(commonData.code) {
                                1 -> {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@UserUpdateActivity, "上传成功", Toast.LENGTH_SHORT).show()
                                        Glide.with(this@UserUpdateActivity)
                                            .load(filePath)
                                            .transform(CircleCrop())
                                            .into(imageAvatar)
                                    }
                                    EventBus.getDefault().post(MineFragment.UserLoginEvent())
                                } else -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@UserUpdateActivity, "参数错误", Toast.LENGTH_SHORT).show()
                                }
                            }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserUpdateActivity, "请求异常", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}