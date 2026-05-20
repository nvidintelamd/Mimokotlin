package com.mimokotlin.app

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class TopSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var webView: WebView? = null

    fun setWebView(wv: WebView) {
        webView = wv
    }

    override fun canChildScrollUp(): Boolean {
        val wv = webView ?: return super.canChildScrollUp()
        // 使用 scrollY 判断，更可靠
        // scrollY > 0 表示页面已经滚动了一段距离，不在顶部
        return wv.scrollY > 0
    }
}
