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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.PromptDelegate
import java.net.URI

class PlatformWebViewFragment : Fragment() {

    companion object {
        private const val ARG_URL = "url"

        private var geckoRuntime: GeckoRuntime? = null

        fun getGeckoRuntime(context: Context): GeckoRuntime {
            return geckoRuntime ?: GeckoRuntime.create(context.applicationContext).also {
                geckoRuntime = it
            }
        }

        fun newInstance(url: String) = PlatformWebViewFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }

    private var geckoView: GeckoView? = null
    private var geckoSession: GeckoSession? = null
    private var progressBar: ProgressBar? = null
    private var onPageLoadedCallback: (() -> Unit)? = null
    private var hasNotifiedLoad = false
    private var baseUrl: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private var fileUploadPrompt: PromptDelegate.FilePrompt? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val prompt = fileUploadPrompt ?: return@registerForActivityResult
        if (uris.isNotEmpty()) {
            prompt.confirm(requireContext(), uris.toTypedArray())
        } else {
            prompt.dismiss()
        }
        fileUploadPrompt = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geckoView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)
        baseUrl = arguments?.getString(ARG_URL) ?: ""
        setupGeckoView()
        val url = arguments?.getString(ARG_URL) ?: "about:blank"
        geckoSession?.loadUri(url)
    }

    private fun setupGeckoView() {
        val gv = geckoView ?: return
        val runtime = getGeckoRuntime(requireContext())

        val session = GeckoSession()
        geckoSession = session

        session.promptDelegate = object : PromptDelegate() {
            override fun onFilePrompt(
                session: GeckoSession,
                prompt: PromptDelegate.FilePrompt
            ) {
                fileUploadPrompt = prompt
                handler.post {
                    fileChooserLauncher.launch("*/*")
                }
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate() {
            override fun onLoadRequest(
                session: GeckoSession,
                request: LoadRequest
            ): NavigationDelegate.Response {
                val url = request.uri.toString()
                if (isExternalLink(url)) {
                    handler.post { showExternalLinkDialog(url) }
                    return NavigationDelegate.Response.DENY
                }
                return NavigationDelegate.Response.ALLOW
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate() {
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (!hasNotifiedLoad) {
                    hasNotifiedLoad = true
                    handler.post { onPageLoadedCallback?.invoke() }
                }
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate() {
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                handler.post {
                    progressBar?.apply {
                        visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                        this.progress = progress
                    }
                }
            }
        }

        gv.setSession(session, runtime)
    }

    private val internalDomains = listOf("xiaomimimo.com", "mi.com", "xiaomi.com")

    private fun isExternalLink(url: String): Boolean {
        if (baseUrl.isEmpty()) return false
        return try {
            val targetHost = URI(url).host?.lowercase() ?: return false
            internalDomains.none { targetHost.endsWith(it) }
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

    fun reload() {
        hasNotifiedLoad = false
        val url = arguments?.getString(ARG_URL)
        if (url != null) {
            geckoSession?.loadUri(url)
        }
    }

    fun canGoBack(): Boolean = geckoSession?.canGoBack() == true

    fun goBack() {
        geckoSession?.goBack()
    }

    override fun onResume() {
        super.onResume()
        geckoSession?.activate()
    }

    override fun onPause() {
        geckoSession?.deactivate()
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        geckoSession?.close()
        geckoSession = null
        geckoView = null
        super.onDestroyView()
    }
}
