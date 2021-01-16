package com.example.rentradar.utils


import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.lang.StringBuilder
import kotlin.math.pow

object HouseInfoTranslate {

    //因為物件內頁、比較列表、收藏列表只需要簡單的對照表，因此另外開function給摳，不用跑讀檔
    fun getTypeOfHouse(serial:Int):String{
        return when(serial){
            1 -> "公寓"
            2 -> "電梯大樓"
            3 -> "透天厝"
            4 -> "別墅"
            else -> "N/A"
        }
    }

    //因為物件內頁、比較列表、收藏列表只需要簡單的對照表，因此另外開function給摳，不用跑讀檔
    fun getTypeOfRoom(serial:Int):String{
        return when(serial){
            1 -> "整層住家"
            2 -> "獨立套房"
            3 -> "分租套房"
            4 -> "雅房"
            5 -> "住辦"
            else -> "N/A"
        }
    }

    fun getSex(serial: Int): String{
        return when(serial){
            0 -> "男女皆可"
            1 -> "男生"
            2 -> "女生"
            else -> "N/A"
        }
    }

    //依照Json檔做比較
    fun getTypeName(jsonObject :JSONObject, serial: Int, titleName:String):String{
        try {
            val strArr = jsonObject.getJSONArray(titleName)
            val result = StringBuilder()
            var count = 0
            //如果2的x次方大於數字，代表後面都沒了，就結束
            while (2.0.pow(count) < serial){
                if(serial.shr(count) % 2 == 1){
                    //避免indexOutOff，做個保險
                    if(count < strArr.length()){
                        //串字串
                        if(result.isEmpty()){
                            //因為在jsonObject裡面，count0是不限，因此要加一
                            result.append(strArr[count + 1].toString())
                        }else{
                            result.append(",${strArr[count + 1 ]}")
                        }
                    }
                }
                count++
            }
            if(result.isEmpty()){
                return "N/A"
            }
            return String(result)
        }catch (e : JSONException){
            //錯誤，回傳N
            Log.d("GetTypeName", "${e.message}")
            return "N/A"
        }
    }


}