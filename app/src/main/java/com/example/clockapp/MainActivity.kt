package com.example.clockapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.clockapp.ui.AlarmFragment
import com.example.clockapp.ui.ClockFragment
import com.example.clockapp.ui.TimerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.view_pager)
        bottomNav = findViewById(R.id.bottom_navigation)

        // 设置ViewPager适配器
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        
        // 禁用ViewPager2的滑动功能，只通过底部导航栏切换
        viewPager.isUserInputEnabled = false

        // 设置底部导航栏点击事件
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_clock -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_alarm -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_timer -> {
                    viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }

        // 设置ViewPager页面切换监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.selectedItemId = when (position) {
                    0 -> R.id.nav_clock
                    1 -> R.id.nav_alarm
                    2 -> R.id.nav_timer
                    else -> R.id.nav_clock
                }
            }
        })
    }

    /**
     * ViewPager适配器
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClockFragment()
                1 -> AlarmFragment()
                2 -> TimerFragment()
                else -> ClockFragment()
            }
        }
    }
}