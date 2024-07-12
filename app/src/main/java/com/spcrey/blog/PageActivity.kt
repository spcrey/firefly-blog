package com.spcrey.blog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class PageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)
        val textTitleBar = findViewById<TextView>(R.id.text_title_bar)
        val bgHomePage = findViewById<View>(R.id.bg_home_page)
        val bgMessageList = findViewById<View>(R.id.bg_message_list)
        val bgMine = findViewById<View>(R.id.bg_mine)
        val icHomePage = findViewById<ImageView>(R.id.ic_home_page)
        val icMessageList = findViewById<ImageView>(R.id.ic_message_list)
        val icMine = findViewById<ImageView>(R.id.ic_mine)
        val textHomePage = findViewById<TextView>(R.id.text_home_page)
        val textMessageList = findViewById<TextView>(R.id.text_message_list)
        val textMine = findViewById<TextView>(R.id.text_mine)
        bgHomePage.setOnClickListener {
            textTitleBar.text = "主页"
            icHomePage.alpha = 0.8f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.8f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.2f
        }
        bgMessageList.setOnClickListener {
            textTitleBar.text = "消息"
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.8f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.8f
            textMine.alpha = 0.2f
        }
        bgMine.setOnClickListener {
            textTitleBar.text = "我的"
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.8f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.8f
        }
    }
}