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
        // 只有当 WebView 不能继续向上滚动时（即已经在顶部），才允许下拉刷新
        return webView?.canScrollVertically(-1) ?: super.canChildScrollUp()
    }
}
