package com.mimokotlin.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class PlatformWebViewFragment : Fragment() {

    companion object {
        fun newInstance(url: String) = PlatformWebViewFragment().apply {
            arguments = Bundle().apply { putString("url", url) }
        }
    }

    private var innerFragment: WebViewFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_platform_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val url = arguments?.getString("url") ?: "about:blank"
            innerFragment = WebViewFragment.newInstance(url)
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, innerFragment!!)
                .commit()
        } else {
            innerFragment = childFragmentManager.findFragmentById(R.id.fragmentContainer) as? WebViewFragment
        }
    }

    fun setOnPageLoadedListener(callback: () -> Unit) {
        innerFragment?.setOnPageLoadedListener(callback)
    }

    fun reload() {
        innerFragment?.reload()
    }

    fun canGoBack(): Boolean = innerFragment?.canGoBack() == true

    fun goBack() {
        innerFragment?.goBack()
    }
}
