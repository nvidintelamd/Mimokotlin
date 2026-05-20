package com.mimokotlin.app

import android.os.Bundle
import android.view.Menu
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

    // 存储 Fragment 引用
    private val fragmentMap = mutableMapOf<Int, WebViewFragment>()

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

        // 单击切换标签页
        bottomNav.setOnItemSelectedListener { item ->
            viewPager.currentItem = item.itemId
            true
        }

        // 再次点击已选中的标签页 = 刷新
        bottomNav.setOnItemReselectedListener { item ->
            val fragment = fragmentMap[item.itemId]
            fragment?.reload()
            Toast.makeText(this, "刷新中...", Toast.LENGTH_SHORT).show()
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
        val currentFragment = fragmentMap[viewPager.currentItem]

        if (currentFragment?.canGoBack() == true) {
            currentFragment.goBack()
        } else if (viewPager.currentItem > 0) {
            viewPager.currentItem = viewPager.currentItem - 1
        } else {
            @Suppress("DEPRECATION")
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
