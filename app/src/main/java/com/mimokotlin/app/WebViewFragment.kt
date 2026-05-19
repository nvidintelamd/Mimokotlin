package com.mimokotlin.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class WebViewFragment : Fragment() {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String) = WebViewFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var swipeRefresh: TopSwipeRefreshLayout? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // 现代文件选择器
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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        setupSwipeRefresh()
        setupWebView()
        val url = arguments?.getString(ARG_URL) ?: "about:blank"
        webView?.loadUrl(url)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh?.setOnRefreshListener {
            webView?.reload()
        }
        // 设置 WebView 引用，用于判断是否在顶部
        webView?.let { swipeRefresh?.setWebView(it) }
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
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
        }

        // Cookie
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                // 所有链接都在 WebView 内打开
                return false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh?.isRefreshing = false
                // 注入 CSS 隐藏广告横幅
                injectAdBlockCss(view)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar?.apply {
                    visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                    progress = newProgress
                }
            }

            // 文件上传
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // 取消之前的回调
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val acceptTypes = fileChooserParams.acceptTypes
                val mimeFilter = if (acceptTypes.isNotEmpty() && acceptTypes[0].isNotBlank()) {
                    acceptTypes[0]
                } else {
                    "*/*"
                }
                fileChooserLauncher.launch(mimeFilter)
                return true
            }
        }

        // 文件下载
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun injectAdBlockCss(webView: WebView) {
        // 注入 JS 隐藏顶部广告横幅（如 "Mimo V2.5 正式上线" 等推广信息）
        val js = """
            (function() {
                // 如果已经执行过，跳过
                if (document.getElementById('mimo-adblock-style')) return;
                
                // 标记已执行
                var marker = document.createElement('meta');
                marker.id = 'mimo-adblock-style';
                document.head.appendChild(marker);
                
                // 广告关键词
                var adTexts = ['正式上线', 'V2.5', '新版本发布', '升级通知', '立即体验', '了解更多'];
                
                // 查找并隐藏包含广告文字的元素
                function hideAds() {
                    var allElements = document.querySelectorAll('div, section, header, aside, nav, p, span, a');
                    allElements.forEach(function(el) {
                        // 只处理直接文本内容（不检查子元素）
                        var directText = Array.from(el.childNodes)
                            .filter(function(n) { return n.nodeType === 3; })
                            .map(function(n) { return n.textContent; })
                            .join('');
                        
                        var fullText = el.textContent || '';
                        var isAd = adTexts.some(function(adText) { 
                            return fullText.includes(adText); 
                        });
                        
                        if (isAd) {
                            // 检查是否是顶部区域的横幅（高度适中，不是整个页面）
                            var rect = el.getBoundingClientRect();
                            if (rect.top < 150 && rect.height > 20 && rect.height < 300) {
                                el.style.display = 'none';
                                el.style.visibility = 'hidden';
                                el.style.height = '0';
                                el.style.overflow = 'hidden';
                                el.style.margin = '0';
                                el.style.padding = '0';
                            }
                        }
                    });
                }
                
                // 立即执行一次
                hideAds();
                
                // 监听 DOM 变化，处理动态加载的内容
                var observer = new MutationObserver(function(mutations) {
                    hideAds();
                });
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
                
                // 延迟再执行一次（等待异步加载的内容）
                setTimeout(hideAds, 1000);
                setTimeout(hideAds, 3000);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
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
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroyView()
    }
}
