package com.mimokotlin.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import java.net.URI

class WebViewFragment : Fragment() {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String) = WebViewFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var onPageLoadedCallback: (() -> Unit)? = null
    private var hasNotifiedLoad = false
    private var baseUrl: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)
        baseUrl = arguments?.getString(ARG_URL) ?: ""
        setupWebView()
        val url = arguments?.getString(ARG_URL) ?: "about:blank"
        webView?.loadUrl(url)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = webView ?: return

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                if (!request.isForMainFrame) return false
                val url = request.url.toString()
                if (isExternalLink(url)) {
                    showExternalLinkDialog(url)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                if (!hasNotifiedLoad) {
                    hasNotifiedLoad = true
                    onPageLoadedCallback?.invoke()
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar?.apply {
                    visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                    progress = newProgress
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback
                fileChooserLauncher.launch("*/*")
                return true
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun isExternalLink(url: String): Boolean {
        if (baseUrl.isEmpty()) return false
        return try {
            val targetHost = URI(url).host?.lowercase() ?: return false
            val baseHost = URI(baseUrl).host?.lowercase() ?: return false
            targetHost != baseHost
        } catch (e: Exception) {
            false
        }
    }

    private fun showExternalLinkDialog(url: String) {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_external_link, null)
        dialogView.findViewById<TextView>(R.id.dialogUrl).text = url

        val countdownView = dialogView.findViewById<TextView>(R.id.dialogCountdown)

        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setPositiveButton("打开") { _, _ -> openInBrowser(url) }
            .setNegativeButton("取消", null)
            .create()

        var remaining = 10
        countdownView.text = "${remaining}s 后自动关闭"

        val countdownRunnable = object : Runnable {
            override fun run() {
                remaining--
                if (remaining <= 0) {
                    if (dialog.isShowing) dialog.dismiss()
                } else {
                    countdownView.text = "${remaining}s 后自动关闭"
                    handler.postDelayed(this, 1000)
                }
            }
        }

        dialog.setOnDismissListener {
            handler.removeCallbacks(countdownRunnable)
        }

        dialog.show()
        handler.postDelayed(countdownRunnable, 1000)
    }

    private fun openInBrowser(url: String) {
        val ctx = context ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    fun setOnPageLoadedListener(callback: () -> Unit) {
        onPageLoadedCallback = callback
    }

    private fun handleDownload(
        url: String, userAgent: String, contentDisposition: String, mimeType: String
    ) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("下载文件中...")
                setTitle(fileName)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName
                )
            }

            val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(requireContext(), "开始下载: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun reload() {
        hasNotifiedLoad = false
        val url = arguments?.getString(ARG_URL)
        if (url != null) {
            webView?.loadUrl(url)
        } else {
            webView?.reload()
        }
    }

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack() {
        webView?.goBack()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroyView()
    }
}
