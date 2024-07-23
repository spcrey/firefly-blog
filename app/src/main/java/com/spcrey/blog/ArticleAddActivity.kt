package com.spcrey.blog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Image
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class ArticleAddActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ArticleAddActivity"
    }

    private val PICK_IMAGE_REQUEST = 1

    val imgLastImage by lazy {
        findViewById<ImageView>(R.id.img_last_image)
    }

    val textImageNum by lazy {
        findViewById<TextView>(R.id.text_image_num)
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
            val filePath = data.data
            val localPath = filePath?.let { getRealPath(this, it) }
            Log.d(TAG, "FilePath: ${filePath?.path}")
            Log.d(TAG, "LocalFilePath: $localPath")
            Glide.with(this)
                .load(filePath)
                .transform(CircleCrop())
                .into(imgLastImage)
            val file = localPath?.let {
                File(it)
            }

            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            imageUrls.add(fileBase64)
            textImageNum.text = imageUrls.size.toString()

        }
    }

    val imageUrls: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article_add)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editTextContent = findViewById<EditText>(R.id.editText_content)
        val btnPublish = findViewById<View>(R.id.btn_publish)
        val btnAddImage = findViewById<TextView>(R.id.btn_add_image)
        btnAddImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }
        btnPublish.setOnClickListener {
            val content = editTextContent.text.toString()
            if (content == "") {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val commonData = ServerApiManager.apiService.articleAdd(
                                CachedData.token!!,
                                ServerApiManager.ArticleAddForm(content, imageUrls)
                            ).await()
                            if (commonData.code == 1) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ArticleAddActivity, "发布成功", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ArticleAddActivity, "发布失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "error: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ArticleAddActivity, "发布失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}