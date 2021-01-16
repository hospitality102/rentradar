package com.example.rentradar.utils

import android.content.Context
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.example.rentradar.ResultsObjectActivity
import com.google.android.gms.maps.model.LatLng
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder

object Global {

    enum class BackIndex(val serial:Int){
        RESULTS_LIST(1),
        RESULTS_MAP_LIST(2),
        FAVORITE_LIST(3)
    }

    object ItemType{
        const val FILTER_AREA = 100
        const val FILTER_MRT = 110
        const val COMMENT_AREA = 120
        const val CONDITION_DITAIL = 140
        const val CONVENIENCE_ITEM = 150
        const val HOUSE=160
        const val COMPARE_STRING = 180
        const val CONDITION_TITLE = 200
        const val CONDITION_TURN = 205
        const val CONDITION_CHOOSE = 210
//        const val LIFEFUNCTION = 220
    }

    object BundleKey{
        const val TITLENAME = "TitleName"
        const val RADARID="RadarId"
        const val CONDITION_NAME = "ConditionName"
        const val AREA_TITLE_NAME = "AreaTitleName"
        const val RESULT_LOCATION = "ResultLocation"
        const val AVERAGEPRICE = "AveragePrice"
        const val COMPARE_ID_ARR = "CompareIdArr"
        const val COMPARE_NAME_ARR = "CompareNameArr"
        const val ISFAVORITE = "Isfavorite"
        const val HOUSE_NAME = "HouseName"
        const val RESULT_OBJECT_BACK_INDEX = "ResultObjectBackIndex"
        const val RADAR_TO_RESULT="RadarToResult"
        const val RESULT_TO_MAP="ResultToMap"
//        const val LIFE_RANGE_NAME = "LifeRangeName"
//        const val LIFE_RANGE_DISTANCE = "LifeRangeDistance"
//        const val LIFE_NEAREST = "LifeNearest"
//        const val LIFE_COUNT = "LifeCount"
    }

    object SharePath{
        const val RESULT_OBJECT_BACK = "ResultObjectBack"
        const val RESULT_HOUSEID = "HouseId"
        const val PUSH_INFO = "PushInfo"
    }

    object MagicNumber{
        //篩選條件細項，控制該頁是否可以捲動
        const val MIN_CONDITION = 8
        //區域搜尋，控制該頁是否可以捲動
        const val MIN_REGION = 5
        //捷運搜尋，可以被選擇的站數上限
        const val MAX_MRT_COUNT = 5
        //比較內頁，條件總數，初始化使用
        const val CONDITION_COUNT = 22
        //動態載入每頁筆數
        const val DYNAMIC_ITEM_COUNT=50
        //輸入框最大字數限制
        const val INPUT_LIMIT=20
        //物件內頁暱稱輸入框最大字數
        const val COMMENT_NAME_INPUT_LIMIT = 25
        //物件內頁暱稱輸入框最大字數
        const val COMMENT_CONTENT_INPUT_LIMIT = 120
        //物件內頁評論區載入數量
        const val COMMENT_PAGE_COUNT = 10
        //判斷連點的時間差要多久
        const val DOUBLE_CLICK_TIME_QUICKSORT = 1000
        const val DOUBLE_CLICK_TIME = 700

    }

    //評論區的default留言
    val DEFAULT_COMMENT = ResultsObjectActivity.Comment(-1, "租屋雷達", "目前還沒有任何評論唷～ 趕快來搶頭香～")

    //googleMap API KEY & 地標搜尋、手機定位的權限要求編號
    const val GOOGLE_MAP_API_KEY = "AIzaSyCdrUOkQqXES-NaO7ctVhkhAmUlfczq_8A"
    //自動填入搜尋的requestCode
    const val SEARCH_VIEW = 100
    //取得訂位權限的requestCode
    const val REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION = 200;
    //預設的經緯度CMoney
    const val DEFAULT_LAT = 25.020772731739548
    const val DEFAULT_LNG = 121.46732723504766
    //台灣的經緯度範圍
    val TAIWAN_RANGE_LEFT = LatLng(21.715956, 119.419628)
    val TAIWAN_RANGE_RIGHT = LatLng(25.371160, 122.138744)
    //通勤時間*的公里數開車(700公尺 = 0.7公里）
    const val DRIVE_SPEED = 500
    const val MRT_SPEED = 250


    //通勤時間的誤差值
    fun  getDevationDistance(distance:Int):Int{ return distance / 10 }

    //讀json檔的function，會得到String，在自己轉JSONObject或Arr
    fun <T : Context> readJSONDataFromFile(context: T, rawId: Int) : String{
        // 利用context的方法，從本地資源(raw)拿取JSON檔
        val inputStream  = context.resources.openRawResource(rawId)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        var jsonString  = bufferedReader.readLine()
        val builder = StringBuilder()
        while(jsonString != null){
            builder.append(jsonString)
            jsonString = bufferedReader.readLine()
        }
        inputStream.close()

        return String(builder)
    }

    //輸入框點擊框外，就會自動取消鎖定的設定，需要override dispatchTouchEvent後，ev = 使用後回傳直=值
    fun <T : Context> setClickCancelFocus(ev: MotionEvent, view: View, context: T) :MotionEvent{
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (isHideInput(view, ev)) {
                hideSoftInput(view.windowToken, context)
                view.clearFocus()
            }
        }
        return ev
    }

    private fun isHideInput(v: View?, ev: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom: Int = top + v.getHeight()
            val right: Int = left + v.getWidth()
            return !(ev.x > left && ev.x < right && ev.y > top && ev.y < bottom)
        }
        return false
    }

    private fun  <T : Context> hideSoftInput(token: IBinder?, context: T) {
        if (token != null) {
            val manager: InputMethodManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

}