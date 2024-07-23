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

class UpdateInfoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UpdateInfoActivity"
    }

    private val PICK_IMAGE_REQUEST = 1
    private var filePath: Uri? = null

    val imageAvatar by lazy {
        findViewById<ImageView>(R.id.image_avatar)
    }

    fun getRealPath(context: Context, uri: Uri): String? {
        var filePath: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        return filePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data
            val localPath = filePath?.let { getRealPath(this, it) }
            Log.d(TAG, "FilePath: ${filePath?.path}")
            Log.d(TAG, "LocalFilePath: $localPath")
            Glide.with(this)
                .load(filePath)
                .transform(CircleCrop())
                .into(imageAvatar)
            val file = localPath?.let {
                File(it)
            }
            if (file != null) {
                if (file.exists()) {
//                    Toast.makeText(this, "文件存在", Toast.LENGTH_SHORT).show()
                }
            }

            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val commonData =
                            ServerApiManager.apiService.userUpdateAvatar(
                                CachedData.token!!,
                                ServerApiManager.UserUpdateAvatarForm(fileBase64)).await()
                        Log.d(TAG, commonData.toString())
                        if (commonData.code == 1) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UpdateInfoActivity, "上传成功", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UpdateInfoActivity, "上传失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UpdateInfoActivity, "上传失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun validateUserUpdate(): ServerApiManager.UserUpdateForm {
        val form = ServerApiManager.UserUpdateForm()

        val nickname = editTextNickname.text.toString()
        val email = editTextEmail.text.toString()
        val personalSignature = editTextPersonalSignature.text.toString()

        if (nickname.isNotBlank()) {
            form.nickname = nickname
        }
        if (nickname.isNotBlank()) {
            form.email = email
        }
        if (personalSignature.isNotBlank()) {
            form.personalSignature = personalSignature
        }
        return form
    }

    val editTextNickname by lazy {
        findViewById<TextView>(R.id.editText_nickname)
    }
    val editTextEmail by lazy {
        findViewById<EditText>(R.id.editText_email)
    }
    val editTextPersonalSignature by lazy {
        findViewById<EditText>(R.id.editText_personal_signature)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPreferences = getSharedPreferences("user", MODE_PRIVATE)
        Glide.with(this)
            .load(CachedData.user?.avatarUrl)
            .transform(CircleCrop())
            .into(imageAvatar)

        imageAvatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        CachedData.user?.let { userInfo ->
            val nickname = userInfo.nickname
            val email = userInfo.email
            val personalSignature = userInfo.personalSignature
            nickname?.let { it ->
                if (it.isNotBlank()) {
                    editTextNickname.text = it
                }
            }
            email?.let { it ->
                if (it.isNotBlank()) {
                    editTextEmail.setText(it)
                }
            }
            personalSignature?.let { it ->
                if (it.isNotBlank()) {
                    editTextPersonalSignature.setText(it)
                }
            }
        }

        val btnUpdateInfo = findViewById<View>(R.id.btn_update_info)
        btnUpdateInfo.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val userUpdate = validateUserUpdate()
                        val commonData = ServerApiManager.apiService.userUpdate(
                            CachedData.token!!, userUpdate
                        ).await()
                        Log.d(TAG, commonData.toString())
                        if (commonData.code == 1) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UpdateInfoActivity, "信息修改成功", Toast.LENGTH_SHORT).show()
                                CachedData.user?.nickname = userUpdate.nickname!!
                                CachedData.user?.email = userUpdate.email
                                CachedData.user?.personalSignature = userUpdate.personalSignature
                                EventBus.getDefault().post(MineFragment.LoginEvent())
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UpdateInfoActivity, "信息修改失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UpdateInfoActivity, "信息修改失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val btnLogout = findViewById<View>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val commonData = ServerApiManager.apiService.userLogout(
                            CachedData.token!!
                        ).await()
                        if (commonData.code==1) {
                            CachedData.token = null
                            CachedData.user = null
                            val editor = sharedPreferences?.edit()
                            if (editor != null) {
                                editor.remove("token")
                                editor.commit()
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@UpdateInfoActivity,
                                    "退出登陆成功",
                                    Toast.LENGTH_SHORT
                                ).show()
                                EventBus.getDefault().post(MineFragment.LogoutEvent())
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this@UpdateInfoActivity,
                                "退出登陆失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }catch (e: Exception) {
                        Log.d(TAG, "request failed: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@UpdateInfoActivity,
                                "退出登陆失败",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}