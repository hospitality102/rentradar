package com.example.rentradar.utils

object DoubleClickGuard {
    var lastClickTime :Long = 0

    fun isFastDoubleClick():Boolean{
        val time = System.currentTimeMillis()
        val timeD = time - lastClickTime
        if(timeD in 1..Global.MagicNumber.DOUBLE_CLICK_TIME){
            return true
        }
        lastClickTime = time
        return false
    }

    fun quickSortIsFastClick():Boolean{
        val time = System.currentTimeMillis()
        val timeD = time - lastClickTime
        if(timeD in 1..Global.MagicNumber.DOUBLE_CLICK_TIME_QUICKSORT){
            return true
        }
        lastClickTime = time
        return false
    }
}