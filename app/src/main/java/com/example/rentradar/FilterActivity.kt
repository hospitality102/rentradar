package com.example.rentradar

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.example.rentradar.view.dialogs.RentRangeDialog
import com.example.rentradar.view.fragments.FilterAreaPage
import com.example.rentradar.view.fragments.FilterCommutePage
import com.example.rentradar.view.fragments.FilterLandmarkPage
import com.example.rentradar.view.fragments.FilterMRTPage
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_filter.*
import kotlinx.android.synthetic.main.activity_filter.topAppBar
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.pow


class FilterActivity : AppCompatActivity(){

    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

    }

    @SuppressLint("HardwareIds")
    override fun onStart() {
        super.onStart()
        if(!RadarController.instance.checkRadar()){
            finish()
        }

        //區域、地標、捷運搜尋的tab標籤起始頁面
        val tabIndex: Int = when{
            (RadarController.instance.getRadar().regionList != null) ->  {  0  }
            (RadarController.instance.getRadar().landmark != null) ->  { 1 }
            (RadarController.instance.getRadar().mrtList != null) ->  {  2 }
            (RadarController.instance.getRadar().commute != null) ->  {  3 }
            else -> {  0  }
        }

        //確認搜尋是否已有選擇
        var isConfirm = false

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        //讀取在raw的條件 json檔，會得到一個String檔，並轉換成JsonObject
        val jsonDataString = Global.readJSONDataFromFile(this, R.raw.condition)
        val jsonObject = JSONObject(jsonDataString)

        //每次要先判斷，如果雷達ID是-1 代表是新增雷達、防呆如果map是沒資料的話，要初始化MAP，
        // 但還是要感測Map是否為空，避免資料被清掉，會在這邊先做一次是避免沒選擇時，資料庫存入資料為空，下次使用會有問題
        if(RadarController.instance.getRadar().id == -1 &&
            RadarController.instance.getRadar().conditionMap.isEmpty()){
            val conditionMap = RadarController.instance.getRadar().conditionMap
            val conditionKeyArr = getConditionJSONArray(jsonObject, "allKeys")
            //初始化map
            conditionKeyArr.forEach { conditionMap[it] = 0 }
            if(RadarController.instance.getRadar().oneChooseMap.isEmpty()){
                val oneChooseMap = RadarController.instance.getRadar().oneChooseMap
                val oneChooseKeyArr = getConditionJSONArray(jsonObject, "oneChooseKeys")
                oneChooseKeyArr.forEach {
                    //不要房仲、頂樓加蓋， ture 表示不要，因此後端會把它相反，false才等於要篩選，因此預設要是true排除掉
                    oneChooseMap[it] = (it == "不要房仲" || it == "不要頂樓加蓋")
                }
            }
        }

        //設定ViewPager的Adapter（控制viewPager的Fragment）
        val pageAdapter = CommonPageAdapter(supportFragmentManager, lifecycle)
        with(pageAdapter){
            add(FilterAreaPage())
            add(FilterLandmarkPage())
            add(FilterMRTPage())
            add(FilterCommutePage())
        }
        viewPager.run{
            adapter = pageAdapter
            currentItem = tabIndex
        }

        //製作、設定TabLayout的標題
        val pageTitle = arrayOf(getString(R.string.filter_top_area)
            , getString(R.string.filter_top_landmark)
            , getString(R.string.filter_top_mrt)
            ,getString(R.string.filter_top_commute))
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = pageTitle[position]
        }.attach()

        //設定上方返回鍵
        topAppBar.setNavigationOnClickListener {
            run{
                ActivityController.instance.startActivity(this, RadarActivity::class.java)
                finish()
            }
        }

        if(RadarController.instance.getRadar().maxPrice == 0){
            RadarController.instance.getRadar().maxPrice = 99999
        }
        //設定起始金額、結束金額按紐
        btnStartRent.text = RadarController.instance.getRadar().minPrice.toString()
        btnStartRent.setOnClickListener {
            run{
                RentRangeDialog(this, true).dialog.show()
            }
        }
        //調整結束金額的顯示字，因為可能會有不限的問題
        val endRent = RadarController.instance.getRadar().maxPrice
        btnEndRent.text = if(endRent == 0 || endRent > 40000){
            getString(R.string.filter_rent_all)
        }else{
            endRent.toString()
        }
        btnEndRent.setOnClickListener {
            run{
                RentRangeDialog(this, false).dialog.show()
            }
        }

        //初始化房屋類型按鈕，Toast之後要拔掉
        setAllButtonView(this)
        btnAll.setOnClickListener {
            RadarController.instance.getRadar().typeOfRoom = 0
            setAllButtonView(this)
        }
        btnShareSuite.setOnClickListener {
            btnAll.isChecked = false
            setRoomType(btnShareSuite.isChecked, 0)
        }
        btnSingleSuite.setOnClickListener {
            btnAll.isChecked = false
            setRoomType(btnSingleSuite.isChecked, 1)
        }
        btnWholeFloorHouse.setOnClickListener {
            btnAll.isChecked = false
            setRoomType(btnWholeFloorHouse.isChecked, 2)
        }
        btnBedsit.setOnClickListener {
            btnAll.isChecked = false
            setRoomType(btnBedsit.isChecked, 3)
        }
        btnHouseAndOffice.setOnClickListener {
            btnAll.isChecked = false
            setRoomType(btnHouseAndOffice.isChecked, 4)
        }

        //設定過濾條件按鈕->跳轉至篩選條件頁面
        conditionCardView.setOnClickListener {
            ActivityController.instance.
                startActivityCustomAnimation(this, ConditionActivity::class.java)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        //如果四個搜尋有設定到，才可以按按鈕，包含空的判斷
        if(RadarController.instance.getRadar().regionList != null &&
            RadarController.instance.getRadar().regionList!!.isNotEmpty()){
            isConfirm= true
        }
        if(RadarController.instance.getRadar().landmark != null){
            isConfirm = true
        }
        if(RadarController.instance.getRadar().mrtList != null &&
            RadarController.instance.getRadar().mrtList!!.isNotEmpty()){
            isConfirm = true
        }
        if(RadarController.instance.getRadar().commute != null){
            isConfirm = true
        }

        //創建及確認修改鍵，後面要在判別是新增還是修改，更改成對應的文字
        btnConfirm.setOnClickListener {
            run {
                if(isConfirm){
                    val radarInfo = JSONObject()
                    val data = JSONObject()
                    RadarController.instance.getRadar().run{
                        try {
                            //data資料
                            regionList?.run{
                                val regionList = JSONArray()
                                this.forEach {
                                    regionList.put(it.id)
                                }
                                data.put("region", regionList)
                            }
                            landmark?.run{
                                val landmark = JSONObject().run {
                                    put("addressTitle", title)
                                    put("address", address)
                                    put("addressRange", range)
                                    put("addressLongitude", longitude)
                                    put("addressLatitude", latitude)
                                }
                                data.put("landmark", landmark)
                            }
                            mrtList?.run{
                                val mrtList = JSONArray()
                                this.forEach {
                                    mrtList.put(it.id)
                                }
                                data.put("mrt", mrtList)
                            }
                            commute?.let {
                                val community = JSONObject()
                                val mrtList = JSONArray(it.mrtList)
                                val landmark = JSONObject()
                                landmark.run{
                                    put("addressTitle", it.target.title)
                                    put("address", it.target.address)
                                    put("addressRange", it.target.range)
                                    put("addressLongitude", it.target.longitude)
                                    put("addressLatitude", it.target.latitude)
                                }
                                community.run{
                                    put("mrt", mrtList)
                                    put("landmark", landmark)
                                    put("transport", it.transport)
                                    put("lengthOfTime", it.time)
                                }
                                data.put("commute", community)
                            }
                            data.put("priceMin", minPrice)
                            data.put("priceMax", maxPrice)
                            data.put("typeOfRoom", typeOfRoom)
                            data.put("typeOfHousing", conditionMap["建築物型態"])
                            data.put("spatialLayout", conditionMap["房屋格局"])
                            data.put("floor", conditionMap["樓層"])
                            data.put("identity", conditionMap["房客身分"])
                            data.put("sex", conditionMap["房客性別"])
                            data.put("furniture", conditionMap["附傢俱"])
                            data.put("appliances", conditionMap["附設備"])
                            data.put("lifeFunction", conditionMap["周邊機能"])
                            data.put("traffic", conditionMap["交通站點"])
                            data.put("hasParkingSpace", oneChooseMap["附車位"])
                            data.put("isAgent", !oneChooseMap["不要房仲"]!!)
                            data.put("isRoofTop", !oneChooseMap["不要頂樓加蓋"]!!)
                            data.put("canShortTerm", oneChooseMap["可短租"])
                            data.put("canCook", oneChooseMap["可開伙"])
                            data.put("canKeepPet", oneChooseMap["可養寵物"])
                            //實際傳送-名字 or ID、UserID、data 之後放androidId
                            val androidId: String =
                                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                            radarInfo.put("userId", androidId)
                            radarInfo.put("data", data)
                            radarInfo.put("radarName", name)
                            if(RadarController.instance.getRadar().id == -1){
                                Log.d("OnAddRadar", "OnAddRadar")
                                //新增雷達API
                                addRadar(radarInfo)
                            }else{
                                Log.d("OnUpdateRadar", "OnUpdateRadar")
                                radarInfo.put("radarId", id)
                                //更新雷達API
                                updateRadar(radarInfo)
                            }
                            println(radarInfo.toString())
                        } catch (e: JSONException) {
                            Toast.makeText(this@FilterActivity, getString(R.string.filter_error_json_format_error), Toast.LENGTH_LONG).show()
                            Log.d("FilterActivty", "JSON FORMAT ERROR! ${e.message}")
                        }
                    }
                }else{
                    Toast.makeText(this, getString(R.string.filter_error_no_search), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //新增雷達
    private fun addRadar(jsonObject: JSONObject){
        NetworkController.instance.addRadar(jsonObject)
            .run {
                onFailure = {errorCode, msg ->
                    runOnUiThread{
                        Toast.makeText(this@FilterActivity, getString(R.string.filter_error_not_connect), Toast.LENGTH_SHORT).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = {res ->
                    runOnUiThread {
                        try{
                            when(JSONObject(res).getInt("status")){
                                201 -> {
                                    ActivityController.instance.startActivity(this@FilterActivity,
                                        RadarActivity::class.java)
                                    finish()
                                }
                                //格式錯誤，toast提醒，並告知錯誤代碼
                                400 ->{
                                    Log.d("OnResponse", "datatype error")
                                    Toast.makeText(this@FilterActivity, getString(R.string.filter_error_json_format_error_internet), Toast.LENGTH_LONG).show()
                                }
                                //找不到User，toast提醒，並告知錯誤代碼
                                404 ->{
                                    Log.d("OnResponse", "data not found")
                                    Toast.makeText(this@FilterActivity, getString(R.string.filter_error_not_found_user), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }catch (e:JSONException){
                            Log.d("OnAddRadar", "${e.message}")
                            Toast.makeText(this@FilterActivity, getString(R.string.filter_error_not_connect), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                exec()
            }
    }

    //修改雷達
    private fun updateRadar(jsonObject: JSONObject){
        NetworkController.instance.updateRadar(jsonObject)
            .run {
                onFailure = {errorCode, msg ->
                    runOnUiThread{
                        Toast.makeText(this@FilterActivity, getString(R.string.filter_error_not_connect), Toast.LENGTH_SHORT).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = {res ->
                    runOnUiThread {
                        try{
                            when(JSONObject(res).getInt("status")){
                                200 -> {
                                    ActivityController.instance.startActivity(this@FilterActivity,
                                        RadarActivity::class.java)
                                    finish()
                                }
                                //格式錯誤目前沒處哩，先印資訊
                                400 ->{
                                    Log.d("OnResponse", "datatype error")
                                    Toast.makeText(this@FilterActivity, getString(R.string.filter_error_json_format_error_internet), Toast.LENGTH_SHORT).show()
                                }
                                //如果修改雷達找不到該雷達，直接改成幫他建立一顆。
                                404 ->{
                                    Log.d("OnResponse", "data not found")
                                    addRadar(jsonObject)
                                }
                            }
                        }catch (e:JSONException){
                            Log.d("OnUpdateRadar", "${e.message} $res")
                            Toast.makeText(this@FilterActivity, getString(R.string.filter_error_not_connect), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                exec()
            }
    }

    //更新全部的按鈕狀態
    private fun setAllButtonView(activity: FilterActivity){
        val roomType = RadarController.instance.getRadar().typeOfRoom
        activity.btnAll.isChecked = (roomType == 0)
        activity.btnShareSuite.isChecked =  ((roomType.shr(0) % 2) == 1)
        activity.btnSingleSuite.isChecked =  ((roomType.shr(1) % 2) == 1)
        activity.btnWholeFloorHouse.isChecked =  ((roomType.shr(2) % 2) == 1)
        activity.btnBedsit.isChecked =  ((roomType.shr(3) % 2) == 1)
        activity.btnHouseAndOffice.isChecked =  ((roomType.shr(4) % 2) == 1)
    }
    //調整數值(複合列舉）
    private fun setRoomType(isChecked:Boolean, power:Int){
        if(isChecked){
            RadarController.instance.getRadar().typeOfRoom += 2.0.pow(power).toInt()
        }else{
            RadarController.instance.getRadar().typeOfRoom -= 2.0.pow(power).toInt()
        }
        if(RadarController.instance.getRadar().typeOfRoom == 0){
            btnAll.isChecked = true
        }
    }

    // 從Json檔中抓取對應的名稱的條件細項名稱
    private fun getConditionJSONArray(jsonObject: JSONObject, titleName: String): ArrayList<String>{
        val jsonArray =  jsonObject.getJSONArray(titleName)
        val arrayList = arrayListOf<String>()
        for(i in 0 until jsonArray.length()){
            arrayList.add(jsonArray[i].toString())
        }
        return arrayList
    }

    override fun onResume() {
        if(!networkStateObserver.isRegister){
            networkStateObserver.register()
        }
        super.onResume()
    }

    override fun onStop() {
        if(networkStateObserver.isRegister){
            networkStateObserver.unregister()
        }
        super.onStop()
    }


}