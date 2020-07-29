package org.walleth.security

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

fun getLayoutMediator(tabs: TabLayout, tabItems: List<SecurityTab>, viewPager: ViewPager2) = TabLayoutMediator(tabs, viewPager) { tab, position ->
    tab.setText(tabItems[position].name)
    tab.setIcon(tabItems[position].Icon)
    viewPager.setCurrentItem(tab.position, true)
}
