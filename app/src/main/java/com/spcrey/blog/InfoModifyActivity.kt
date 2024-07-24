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

class InfoModifyActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UpdateInfoActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
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
    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btn_back)
    }

    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info_modify)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnBack.setOnClickListener {
            finish()
        }

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
            CachedData.token?.let { token ->
                val commonData = validateEditText()
                commonData.data?.let { userUpdateForm ->
                    lifecycleScope.launch {
                        userUpdate(token, userUpdateForm)
                    }
                } ?: run {
                    Toast.makeText(
                        this@InfoModifyActivity,
                        commonData.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnLogout.setOnClickListener {
            CachedData.token?.let { token ->
                lifecycleScope.launch {
                    userLogout(token)
                }
            }
        }
    }

    private suspend fun userUpdate(token: String, userUpdateForm: ServerApiManager.UserUpdateForm) {
        withContext(Dispatchers.IO){
            try {
                val commonDataUserUpdate =
                    ServerApiManager.apiService.userUpdate(
                        token, userUpdateForm
                    ).await()
                when (commonDataUserUpdate.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@InfoModifyActivity,
                                "信息修改成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@InfoModifyActivity,
                                "参数错误",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InfoModifyActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun userLogout(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userLogout(token).await()
                when (commonData.code) {
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
                                this@InfoModifyActivity,
                                "退出登陆成功",
                                Toast.LENGTH_SHORT
                            ).show()
                            EventBus.getDefault().post(MineFragment.UserLogoutEvent())
                            finish()
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@InfoModifyActivity,
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
                        this@InfoModifyActivity,
                        "请求异常",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private suspend fun userUpdateAvatar(token: String, fileBase64: String, filePath: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userUpdateAvatar(
                        token, ServerApiManager.UserUpdateAvatarForm(fileBase64)).await()
                when(commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Glide.with(this@InfoModifyActivity)
                                .load(filePath)
                                .transform(CircleCrop())
                                .into(imageAvatar)
                        }
                        EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent ())
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@InfoModifyActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InfoModifyActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            CachedData.token?.let { token ->
                val filePath = data.data
                filePath?.let {
                    val localPath = getRealPath(filePath)
                    val file = localPath?.let {
                        File(localPath)
                    }
                    val fileBytes = FileInputStream(file).readBytes()
                    val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
                    lifecycleScope.launch {
                        userUpdateAvatar(token, fileBase64, filePath)
                    }
                }
            }
        }
    }
}