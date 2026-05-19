package com.mimokotlin.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config = AppConfig.load(this)

        // 设置标题
        supportActionBar?.title = config.appName

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)

        setupBottomNav()
        setupViewPager()
    }

    private fun setupBottomNav() {
        // 动态构建菜单
        val menu = bottomNav.menu
        menu.clear()

        val icons = intArrayOf(
            android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_info_details,
            android.R.drawable.ic_menu_manage
        )

        config.tabs.forEachIndexed { index, tab ->
            menu.add(Menu.NONE, index, Menu.NONE, tab.label).apply {
                setIcon(icons.getOrElse(index) { android.R.drawable.ic_menu_compass })
            }
        }

        // 图标着色
        bottomNav.setOnItemSelectedListener { item ->
            viewPager.currentItem = item.itemId
            true
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = TabAdapter(this, config.tabs)
        viewPager.offscreenPageLimit = config.tabs.size - 1
        viewPager.isUserInputEnabled = false  // 禁用左右滑动

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.selectedItemId = position
            }
        })
    }

    @Deprecated("Use OnBackPressedCallback")
    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        val currentFragment = fragments.find { it.isVisible } as? WebViewFragment

        if (currentFragment?.canGoBack() == true) {
            currentFragment.goBack()
        } else if (viewPager.currentItem > 0) {
            viewPager.currentItem = viewPager.currentItem - 1
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private class TabAdapter(
        activity: FragmentActivity,
        private val tabs: List<TabConfig>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment =
            WebViewFragment.newInstance(tabs[position].url)
    }
}
