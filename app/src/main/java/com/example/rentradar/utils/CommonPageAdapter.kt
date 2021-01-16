package com.example.rentradar.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class CommonPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle):
    FragmentStateAdapter(fragmentManager, lifecycle){

    //儲放Fragments
    private val fragments = mutableListOf<Fragment>()

    //加入Fragment
    fun add(fragment: Fragment){
        fragments.add(fragment)
    }

    //移除Fragment
    fun remove(fragment: Fragment){
        fragments.remove(fragment)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }


}