package com.example.rentradar


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rentradar.view.fragments.FavoritesPage
import com.example.rentradar.view.fragments.RadarPage

class RadarPageAdapter (fragmentManage: FragmentManager, lifecycle:Lifecycle): FragmentStateAdapter(fragmentManage,lifecycle){
    var fragments:MutableList<Fragment> = mutableListOf()
    init{
        fragments.add(RadarPage())
        fragments.add(FavoritesPage())
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
       return fragments[position]
    }


}