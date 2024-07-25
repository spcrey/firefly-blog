package com.spcrey.blog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.spcrey.blog.fragment.HomePageFragment
import com.spcrey.blog.fragment.HomePageFragment.Companion
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class ArticlePublishActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ArticleAddActivity"
        private const val PICK_IMAGE_REQUEST = 1
    }

    private val imgLastImage by lazy {
        findViewById<ImageView>(R.id.img_last_image)
    }
    private val textImgNum by lazy {
        findViewById<TextView>(R.id.text_img_num)
    }
    private val editTextContent by lazy {
        findViewById<EditText>(R.id.editText_content)
    }
    private val btnPublish by lazy {
        findViewById<View>(R.id.btn_publish)
    }
    private val btnAddImage by lazy {
        findViewById<TextView>(R.id.btn_add_image)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }
    private val imageUrls: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article_publish)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnAddImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        icBack.setOnClickListener {
            finish()
        }

        suspend fun articleAdd(token: String, content: String) {
            withContext(Dispatchers.IO) {
                try {
                    val commonData = ServerApiManager.apiService.articleAdd(
                        token,
                        ServerApiManager.ArticleAddForm(content, imageUrls)
                    ).await()
                    when (commonData.code) {
                        1 -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ArticlePublishActivity, "发布成功", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ArticlePublishActivity, "参数错误", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "request failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ArticlePublishActivity, "请求异常", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnPublish.setOnClickListener {
            val content = editTextContent.text.toString()
            if (content == "") {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
            } else {
                CachedData.token?.let { token ->
                    lifecycleScope.launch {
                        articleAdd(token, content)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val filePath = data.data
            val localPath = filePath?.let { getRealPath(this, it) }
            Log.d(TAG, "FilePath: ${filePath?.path}")
            Log.d(TAG, "LocalFilePath: $localPath")
            val layoutParams = imgLastImage.layoutParams
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp48)
            imgLastImage.layoutParams = layoutParams
            imgLastImage.alpha = 0.4f
            Glide.with(this)
                .asBitmap()
                .load(filePath)
                .transform(CenterCrop(), RoundedCorners(dpToPx(12)))
                .into(imgLastImage)
            val file = localPath?.let {
                File(it)
            }
            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            imageUrls.add(fileBase64)
            textImgNum.text = imageUrls.size.toString()
        }
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getRealPath(context: Context, uri: Uri): String? {
        var filePath: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        return filePath
    }
}