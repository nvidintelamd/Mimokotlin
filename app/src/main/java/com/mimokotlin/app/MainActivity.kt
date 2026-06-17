package com.mimokotlin.app

import android.os.Bundle
import android.view.Menu
import android.webkit.CookieManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var config: AppConfig

    private val fragmentMap = mutableMapOf<Int, WebViewFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }

        config = AppConfig.load(this)
        supportActionBar?.title = config.appName

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)

        setupBottomNav()
        setupViewPager()
    }

    private fun setupBottomNav() {
        val menu = bottomNav.menu
        menu.clear()

        val icons = intArrayOf(
            R.drawable.ic_chat,
            R.drawable.ic_claw,
            R.drawable.ic_api_usage,
            R.drawable.ic_token_plan
        )

        config.tabs.forEachIndexed { index, tab ->
            menu.add(Menu.NONE, index, Menu.NONE, tab.label).apply {
                setIcon(icons.getOrElse(index) { android.R.drawable.ic_menu_compass })
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            viewPager.currentItem = item.itemId
            true
        }

        bottomNav.setOnItemReselectedListener { item ->
            val fragment = fragmentMap[item.itemId]
            fragment?.reload()
            Toast.makeText(this, "刷新中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = TabAdapter(this, config.tabs)
        viewPager.offscreenPageLimit = config.tabs.size - 1
        viewPager.isUserInputEnabled = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.selectedItemId = position
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val currentFragment = fragmentMap[viewPager.currentItem]

        if (currentFragment?.canGoBack() == true) {
            currentFragment.goBack()
        } else if (viewPager.currentItem > 0) {
            viewPager.currentItem = viewPager.currentItem - 1
        } else {
            super.onBackPressed()
        }
    }

    private inner class TabAdapter(
        activity: FragmentActivity,
        private val tabs: List<TabConfig>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment {
            val fragment = WebViewFragment.newInstance(tabs[position].url)
            fragmentMap[position] = fragment
            return fragment
        }
    }
}
