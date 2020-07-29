package org.walleth.security

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SecurityTabFragmentStateAdapter(private val tabItems: List<SecurityTab>, fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = tabItems.size
    override fun createFragment(position: Int): Fragment = tabItems[position].fragment
}

class SecurityTabFragmentActivityStateAdapter(private val tabItems: List<SecurityTab>, activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = tabItems.size
    override fun createFragment(position: Int): Fragment = tabItems[position].fragment
}