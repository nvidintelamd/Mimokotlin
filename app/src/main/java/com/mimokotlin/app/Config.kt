package com.mimokotlin.app

import android.content.Context
import org.json.JSONObject

data class TabConfig(
    val label: String,
    val icon: String,
    val url: String
)

data class AppConfig(
    val appName: String,
    val tabs: List<TabConfig>
) {
    companion object {
        fun load(context: Context): AppConfig {
            val json = context.assets.open("config.json")
                .bufferedReader().use { it.readText() }
            val obj = JSONObject(json)

            val appName = obj.optString("appName", "App")
            val tabsArray = obj.getJSONArray("tabs")
            val tabs = (0 until tabsArray.length()).map { i ->
                val tab = tabsArray.getJSONObject(i)
                TabConfig(
                    label = tab.optString("label", "Tab ${i + 1}"),
                    icon = tab.optString("icon", "🌐"),
                    url = tab.getString("url")
                )
            }
            return AppConfig(appName, tabs)
        }
    }
}
