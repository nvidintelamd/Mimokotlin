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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PlatformWebViewFragment : Fragment() {

    companion object {
        private const val ARG_URL = "url"

        private var geckoRuntime: GeckoRuntime? = null
        private var hideButtonsExtension: WebExtension? = null

        fun getGeckoRuntime(context: Context): GeckoRuntime {
            return geckoRuntime ?: GeckoRuntime.create(context.applicationContext).also {
                geckoRuntime = it
                it.webExtensionController.ensureBuiltIn(
                    "resource://android/assets/extensions/hide_buttons/",
                    "hide_buttons@mimokotlin.app"
                ).then({ ext ->
                    hideButtonsExtension = ext
                    GeckoResult.fromValue(ext)
                }, { GeckoResult.fromValue(null as WebExtension?) })
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
    private var canGoBackState = false
    private val handler = Handler(Looper.getMainLooper())

    // File upload state — keep GeckoResult alive until user selects files
    private var filePrompt: GeckoSession.PromptDelegate.FilePrompt? = null
    private var fileResult: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val prompt = filePrompt ?: return@registerForActivityResult
        val result = fileResult ?: return@registerForActivityResult
        if (uris.isNotEmpty()) {
            val ctx = requireContext()
            val response = prompt.confirm(ctx, uris.toTypedArray())
            result.complete(response)
        } else {
            prompt.dismiss()
        }
        filePrompt = null
        fileResult = null
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        val prompt = filePrompt ?: return@registerForActivityResult
        val result = fileResult ?: return@registerForActivityResult
        if (treeUri != null) {
            compressAndConfirm(prompt, result, treeUri)
        } else {
            prompt.dismiss()
            filePrompt = null
            fileResult = null
        }
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
        session.open(runtime)
        geckoSession = session

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onFilePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                filePrompt = prompt
                fileResult = result
                handler.post { showUploadOptions() }
                return result
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val url = request.uri.toString()
                if (isExternalLink(url)) {
                    handler.post { showExternalLinkDialog(url) }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return null
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canGoBackState = canGoBack
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                if (!hasNotifiedLoad) {
                    hasNotifiedLoad = true
                    handler.post { onPageLoadedCallback?.invoke() }
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                handler.post {
                    progressBar?.apply {
                        visibility = if (progress in 1..99) View.VISIBLE else View.GONE
                        this.progress = progress
                    }
                }
            }
        }

        gv.setSession(session)
    }

    private val internalDomains = listOf("xiaomimimo.com", "mi.com", "xiaomi.com")

    private fun showUploadOptions() {
        val ctx = context ?: return
        val options = arrayOf("选择文件", "压缩文件夹为DOCX上传")
        AlertDialog.Builder(ctx)
            .setTitle("上传方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> fileChooserLauncher.launch("*/*")
                    1 -> folderPickerLauncher.launch(null)
                }
            }
            .setNegativeButton("取消") { _, _ ->
                filePrompt?.dismiss()
                filePrompt = null
                fileResult = null
            }
            .show()
    }

    private fun compressAndConfirm(
        prompt: GeckoSession.PromptDelegate.FilePrompt,
        result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
        treeUri: Uri
    ) {
        val ctx = context ?: return
        Thread {
            try {
                val docFile = DocumentFile.fromTreeUri(ctx, treeUri) ?: throw Exception("无法访问文件夹")
                val folderName = docFile.name ?: "archive"
                val uploadDir = File(ctx.externalCacheDir, "uploads").apply { mkdirs() }
                val outFile = File(uploadDir, "$folderName.docx")
                ZipOutputStream(FileOutputStream(outFile)).use { zipFolder(docFile, it, "") }
                val fileUri = Uri.fromFile(outFile)
                handler.post {
                    val response = prompt.confirm(ctx, arrayOf(fileUri))
                    result.complete(response)
                    filePrompt = null
                    fileResult = null
                    Toast.makeText(ctx, "已压缩为: ${outFile.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(ctx, "压缩失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    prompt.dismiss()
                    filePrompt = null
                    fileResult = null
                }
            }
        }.start()
    }

    private fun zipFolder(folder: DocumentFile, zos: ZipOutputStream, path: String) {
        for (file in folder.listFiles()) {
            val fileName = file.name ?: continue
            val entryPath = if (path.isEmpty()) fileName else "$path/$fileName"
            if (file.isDirectory) {
                zipFolder(file, zos, entryPath)
            } else {
                zos.putNextEntry(ZipEntry(entryPath))
                context?.contentResolver?.openInputStream(file.uri)?.use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

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
        dialog.setOnDismissListener { handler.removeCallbacks(countdownRunnable) }
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

    fun canGoBack(): Boolean = canGoBackState

    fun goBack() {
        geckoSession?.goBack()
    }

    override fun onResume() {
        super.onResume()
        geckoSession?.setActive(true)
    }

    override fun onPause() {
        geckoSession?.setActive(false)
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
