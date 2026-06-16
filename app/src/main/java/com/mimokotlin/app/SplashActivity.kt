package com.mimokotlin.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var startTime = 0L
    private var pageLoaded = false
    private var minTimePassed = false
    private val handler = Handler(Looper.getMainLooper())
    private val minDelay = 1200L

    private val finishRunnable = Runnable {
        navigateToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_splash)

        startTime = System.currentTimeMillis()

        val webView = findViewById<WebView>(R.id.splashWebView)
        val progressBar = findViewById<ProgressBar>(R.id.splashProgress)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageLoaded = true
                checkAndFinish()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }

        webView.loadUrl("https://aistudio.xiaomimimo.com/#/c")

        handler.postDelayed({
            minTimePassed = true
            checkAndFinish()
        }, minDelay)
    }

    private fun checkAndFinish() {
        if (pageLoaded && minTimePassed) {
            handler.removeCallbacks(finishRunnable)
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
