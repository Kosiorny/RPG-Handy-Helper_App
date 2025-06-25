package com.example.rpgdiceapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class RegisterWebViewActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_web_view)

        val webView = findViewById<WebView>(R.id.registerWebView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        webView.webViewClient = WebViewClient()
        webView.loadUrl("http://0:8888/register")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.contains("/login")) {
                        val intent = Intent(this@RegisterWebViewActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                        return true
                    }
                }
                return false
            }
        }

    }


}
