package com.example.rentradar.utils

import com.example.rentradar.models.Radar

class RadarController {

   private lateinit var radar : Radar

    fun select(radar: Radar){
        this.radar=radar
    }

    fun getRadar():Radar{
        return radar
    }

    fun checkRadar():Boolean{
        return this::radar.isInitialized
    }

    fun checkRadarSearch():Boolean{
        var hasSearch = false
        if(this::radar.isInitialized){
            radar.regionList?.run { if(isNotEmpty()){hasSearch = true }}
            radar.landmark?.run { hasSearch = true}
            radar.mrtList?.run {
                if(isNotEmpty()){ hasSearch = true}
            }
            radar.commute?.run{hasSearch = true}
        }
        return hasSearch
    }

    companion object { //單例
        val instance: RadarController by lazy { RadarController() }
    }
}