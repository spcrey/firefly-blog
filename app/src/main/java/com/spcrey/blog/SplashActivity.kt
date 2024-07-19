package com.spcrey.blog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val icSplash = findViewById<ImageView>(R.id.ic_splash);
        val textSplash = findViewById<TextView>(R.id.text_splash)
        val animSplash = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        val titleStatementTerm = findViewById<TextView>(R.id.title_statement_term)
        val textStatementTerm = findViewById<TextView>(R.id.text_statement_term)
        val contentTextStatementTerm = "欢迎使用 YF 博客，我们将严格遵守相关法律和隐私政策保护您的个人隐私，请您阅读并同意《用户协议》与《隐私政策》"
        val linkSpannableStringSetter = LinkSpannableStringSetter(textStatementTerm, contentTextStatementTerm, this)
        val areaDisagree = findViewById<View>(R.id.area_disagree)
        val btnDisagree = findViewById<TextView>(R.id.btn_disagree)
        val btnAgree = findViewById<TextView>(R.id.btn_agree)
        linkSpannableStringSetter.set(LinkSpannableText(44, 49, "查看用户协议"))
        linkSpannableStringSetter.set(LinkSpannableText(50, 56, "查看隐私政策"))
        textStatementTerm.text = linkSpannableStringSetter.text
        btnAgree.setOnClickListener {
            val intent = Intent(this, PageActivity::class.java)
            startActivity(intent)
            finish()
        }
        btnDisagree.setOnClickListener {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            exitProcess(0);
        }

        icSplash.startAnimation(animSplash)
        animSplash.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                textSplash.alpha = 1f;
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        delay(1000);
                        withContext(Dispatchers.Main) {
                            titleStatementTerm.visibility = View.VISIBLE
                            textStatementTerm.visibility = View.VISIBLE
                            areaDisagree.visibility = View.VISIBLE
                            btnDisagree.visibility = View.VISIBLE
                            btnAgree.visibility = View.VISIBLE
                        }
                    }
                }
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private class LinkSpannableText(val posStart: Int, val posEnd: Int, val toastString: String)
    private class LinkSpannableStringSetter(textView: TextView, content: String, val context: Context) {
        private val spannableString = SpannableString(content)
        init {
            textView.movementMethod = LinkMovementMethod.getInstance()
        }

        val text: SpannableString
            get() = spannableString
        private fun setClickableSpan(linkSpannableString: LinkSpannableText) {
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    Toast.makeText(context, linkSpannableString.toastString, Toast.LENGTH_SHORT).show()
                }
            }, linkSpannableString.posStart, linkSpannableString.posEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        private fun setForegroundColorSpan(linkSpannableText: LinkSpannableText) {
            spannableString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.firefly)),
                linkSpannableText.posStart, linkSpannableText.posEnd,
                Spannable.SPAN_COMPOSING
            )
        }
        private fun setBackgroundColorSpan(linkSpannableText: LinkSpannableText) {
            spannableString.setSpan(
                BackgroundColorSpan(context.getColor(R.color.white_10)),
                linkSpannableText.posStart, linkSpannableText.posEnd,
                Spannable.SPAN_COMPOSING
            )
        }
        fun set(linkSpannableText: LinkSpannableText) {
            setClickableSpan(linkSpannableText)
            setForegroundColorSpan(linkSpannableText)
            setBackgroundColorSpan(linkSpannableText)
        }
    }
}