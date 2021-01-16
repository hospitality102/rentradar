package com.example.rentradar

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.example.rentradar.models.Commute
import com.example.rentradar.models.Landmark
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.shawnlin.numberpicker.NumberPicker
import kotlinx.android.synthetic.main.activity_filter_commute.*
import kotlinx.android.synthetic.main.activity_filter_commute.topAppBar
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.StringBuilder

class FilterCommuteActivity : AppCompatActivity() {

    init {
        if(!RadarController.instance.checkRadar()){
            finish()
        }
    }

    //從雷達拿取目標點的資訊、使用者設定的通勤方式、時間
    private var target = RadarController.instance.getRadar().commute?.target
    private var title = target?.title ?: ""
    private var address = target?.address ?: ""
    private var lat = target?.latitude ?: 0.0
    private var lng = target?.longitude ?: 0.0
    private var distance = target?.range ?: 0.0
    private var transport = RadarController.instance.getRadar().commute?.transport ?:""
    private var lastTime = RadarController.instance.getRadar().commute?.time
    private var time = RadarController.instance.getRadar().commute?.time ?: 5
    private val mrtIdList = RadarController.instance.getRadar().commute?.mrtList ?: mutableListOf()
    //有捷運的城市清單，要做例外、自動化排除
    private val hasMrtCityList = arrayListOf("台北市", "新北市", "桃園市", "高雄市")
    //捷運清單
    private lateinit var mrtJsonArray:JSONArray
    private var mrtList = mutableListOf<String>()
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)
    //捷運搜尋中的dialog
    private lateinit var dialog :AlertDialog


    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_commute)

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()


        dialog = AlertDialog.Builder(this@FilterCommuteActivity)
            .setView(LayoutInflater.from(this@FilterCommuteActivity)
                .inflate(R.layout.dialog_mrt_search,null))
            .setCancelable(false)
            .create()

        //拿取捷運清單
        getMrtJSONArray()

        //設定上方返回鍵、確認鍵
        topAppBar.setNavigationOnClickListener {
            run{
                finish()
            }
        }
        topAppBar.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.done ->{
                    run{
                        if(!DoubleClickGuard.isFastDoubleClick()){
                            transport = tvTransport.text.toString()
                            when(transport){
                                "捷運" -> distance = (time * Global.MRT_SPEED).toDouble() - Global.getDevationDistance((time * Global.MRT_SPEED))
                                "開車" -> distance = (time * Global.DRIVE_SPEED).toDouble() - Global.getDevationDistance((time * Global.DRIVE_SPEED))
                            }
                            if(title.isEmpty() || transport.isEmpty()){
                                Toast.makeText(this, getString(R.string.filter_commute_no_transport_error),
                                    Toast.LENGTH_SHORT).show()
                            }else if(transport == "捷運"){
                                //如果原始的時間不是null，代表曾經存取或跑過捷運，比對是否跟現在的通勤時間有異動，有的話要再重搜尋一次
                                if(lastTime != null && lastTime != time){
                                    if(this@FilterCommuteActivity::mrtJsonArray.isInitialized){
                                        getMrtList(lat, lng, time * Global.MRT_SPEED, true)
                                    }
                                    //如果lastTime = null 代表目標沒變，但改變通勤方式，也要在跑一次
                                }else if(lastTime == null){
                                    if(this@FilterCommuteActivity::mrtJsonArray.isInitialized){
                                        getMrtList(lat, lng, time * Global.MRT_SPEED, true)
                                    }
                                    //如果捷運清單沒資料，提醒使用者更改條件，不給存
                                }else if(mrtIdList.isEmpty()){
                                    Toast.makeText(this,getString(R.string.filter_commute_no_mrt_error)
                                        ,Toast.LENGTH_LONG).show()
                                }else{
                                    saveAndFinish()
                                }
                            }else{
                                //如果是開車，就把捷運清單清空，避免資料異常
                                mrtIdList.clear()
                                saveAndFinish()
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }

        //設定原始資料地標
        etSearch.setText(title)
        tvAddress.text = title
        //設定googlePlaceAPi憑證-自動填入使用
        Places.initialize(this, Global.GOOGLE_MAP_API_KEY)
        etSearch.run{
            //將聚焦功能關閉
            isFocusable = false
            setOnClickListener {
                if(!DoubleClickGuard.isFastDoubleClick()){
                    //定義要顯示的資訊
                    val fieldList: List<Place.Field> = listOf(
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG,
                        Place.Field.NAME
                    )
                    //將搜尋範圍(自動填入)定位在台灣區域
                    val bounds = RectangularBounds.newInstance(
                        LatLng(Global.TAIWAN_RANGE_LEFT.latitude, Global.TAIWAN_RANGE_LEFT.longitude),
                        LatLng(Global.TAIWAN_RANGE_RIGHT.latitude, Global.TAIWAN_RANGE_RIGHT.longitude)
                    )
                    //跳轉的設定-設定顯示資訊、搜尋範圍、預設(上次輸入)
                    val intent = Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY,
                        fieldList
                    )
                        .setLocationRestriction(bounds)
                        .setInitialQuery(etSearch.text.toString())
                        .build(this@FilterCommuteActivity)
                    //開始搜尋(放入上面設定好的跳種設定、定義requestCode
                    startActivityForResult(intent, Global.SEARCH_VIEW)
                }
            }
        }

        //設定單選按鈕點擊事件
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.radioMrt ->{
                    if(mrtIdList.isEmpty() && (!this::mrtJsonArray.isInitialized || mrtJsonArray.length() < 1)){
                        Toast.makeText(this, getString(R.string.filter_mrt_error_info)
                            , Toast.LENGTH_SHORT).show()
                        radioCar.isChecked = true
                    }else{
                        var hasMrt = false
                        if(address.isNotEmpty()){
                            //判斷該程式有沒有捷運可以選擇，沒有就提醒使用者，並且不給選
                            for(it in hasMrtCityList){
                                if(address.contains(it)){
                                    hasMrt = true
                                    break
                                }
                            }
                        }
                        if(hasMrt || address.isEmpty()){
                            cardView.isVisible = true
                            tvTransport.text = "捷運"
                            tvTitle.text = getString(R.string.filter_commmute_mrt_title)
                            tvDesc.text = getString(R.string.filter_commmute_mrt_content)
                        }else{
                            Toast.makeText(this, getString(R.string.filter_commute_city_not_found_mrt)
                                , Toast.LENGTH_SHORT).show()
                            radioCar.isChecked = true
                        }
                    }
                }
                R.id.radioCar ->{
                    cardView.isVisible = true
                    tvTransport.text = "開車"
                    tvTitle.text = getString(R.string.filter_commmute_drive_title)
                    tvDesc.text = getString(R.string.filter_commmute_drive_content)
                }
            }
        }
        //初始化，將使用者資訊顯示出來
        when(transport){
            "捷運" -> {
                radioMrt.isChecked = true
            }
            "開車" -> {
                radioCar.isChecked = true
            }
            else -> cardView.isVisible = false
        }

        //通勤時間數字轉盤設定
        numberPick.run{
            //從5~60，沒有0
            val numberList = Array(12){i -> "${(i + 1) * 5}"}
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            displayedValues = numberList
            minValue = 0
            // -1 是因為索引值從0開始，如果<5代表是第一次來
            value = if(time < 5) 0 else { time / 5 - 1}
            maxValue = numberList.size - 1
            setOnValueChangedListener { _, _, newVal ->
                time = (newVal + 1) * 5
                tvTime.text = time.toString()
            }
            tvTime.text = ((value + 1)  * 5).toString()
        }

    }

    //開啟dialog的動畫效果
    private fun loading(){
        val animation = AnimationUtils.loadAnimation(this,R.anim.loading)
        val lin = LinearInterpolator()
        animation.interpolator = lin
        val img = dialog.findViewById<ImageView>(R.id.loading)
        img.startAnimation(animation)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //如果收到的回傳代碼、事件代碼都是成功的話，將得到的地點資訊設置並儲存
        if(requestCode == Global.SEARCH_VIEW && resultCode == Activity.RESULT_OK){
            val place = Autocomplete.getPlaceFromIntent(data!!)
            Log.d("OnActivityResult_place", place.toString())
            title = place.name ?:""
            address = place.address ?:""
            //依照所選的地址，自動判斷通勤方式，如果有捷運優先捷運，否則開車
            for(it in hasMrtCityList) {
                if(address.contains(it)){
                    radioMrt.isChecked = true
                    break
                }else{
                    radioCar.isChecked = true
                }
            }
            lat = place.latLng?.latitude  ?: Global.DEFAULT_LAT
            lng = place.latLng?.longitude ?: Global.DEFAULT_LNG
            etSearch.setText(title)
            tvAddress.text = title

            if(tvTransport.text == "捷運"){
                lastTime = time
                //如果捷運清單已經跑完了，而且交通工具是捷運，就可以做篩選的判斷
                if(this::mrtJsonArray.isInitialized){
                    getMrtList(lat, lng, (time * Global.MRT_SPEED ), false)
                }
            }
        }
    }

    //目前剩下改遞迴
    private fun getMrtList(lat: Double, lng: Double, range:Int, isFinish: Boolean){
        NetworkController.instance.getMrtList(lat, lng, range).run {
            onFailure = {errorCode, msg ->
                runOnUiThread {
                    Log.d("OnFailure", "$errorCode:$msg")
                    Toast.makeText(this@FilterCommuteActivity,
                        getString(R.string.filter_commute_google_error),Toast.LENGTH_LONG).show()
                }
            }
            onResponse = {res ->
                try {
                    //每次使用時把名稱清單清空
                    mrtList.clear()
                    var jsonObject = JSONObject(res)
                    if(jsonObject.getString("status") != "OK"){
                        Log.d("getMrtList", "Google Error!  / ${jsonObject.getString("status")}")
                        runOnUiThread {
                            setMrtIdList(isFinish)
                        }
                    }else{
                        var jsonArray = jsonObject.getJSONArray("results")
                        for(i in 0 until jsonArray.length()){
                            mrtList.add(jsonArray.getJSONObject(i).getString("name"))
                        }
                        if(!jsonObject.isNull("next_page_token")){
                            if(lifecycle.currentState == Lifecycle.State.RESUMED){
                                runOnUiThread {
                                    dialog.show()
                                    loading()
                                }
                            }
                            object :Thread(){
                                override fun run() {
                                    println(jsonObject.getString("next_page_token"))
                                    sleep(2000)
                                    NetworkController.instance.getNextMrtList(jsonObject.getString("next_page_token")).run {
                                        onFailure = {errorCode, msg ->
                                            runOnUiThread {
                                                Log.d("OnFailure", "$errorCode:$msg")
                                                if(dialog.isShowing){
                                                    dialog.dismiss()
                                                }
                                                Toast.makeText(this@FilterCommuteActivity,
                                                    getString(R.string.filter_commute_google_error),Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        onResponse = {res ->
                                            jsonObject = JSONObject(res)
                                            jsonArray = jsonObject.getJSONArray("results")
                                            for(i in 0 until jsonArray.length()){
                                                mrtList.add(jsonArray.getJSONObject(i).getString("name"))
                                            }
                                            if(!jsonObject.isNull("next_page_token")){
                                                object :Thread(){
                                                    override fun run() {
                                                        println(jsonObject.getString("next_page_token"))
                                                        sleep(2000)
                                                        NetworkController.instance.getNextMrtList(jsonObject.getString("next_page_token")).run {
                                                            onFailure = {errorCode, msg ->
                                                                runOnUiThread {
                                                                    if(dialog.isShowing){
                                                                        dialog.dismiss()
                                                                    }
                                                                    Log.d("OnFailure", "$errorCode:$msg")
                                                                    Toast.makeText(this@FilterCommuteActivity,
                                                                        getString(R.string.filter_commute_google_error),Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                            onResponse = {res ->
                                                                jsonObject = JSONObject(res)
                                                                jsonArray = jsonObject.getJSONArray("results")
                                                                for(i in 0 until jsonArray.length()){
                                                                    mrtList.add(jsonArray.getJSONObject(i).getString("name"))
                                                                }
                                                                runOnUiThread {
                                                                    setMrtIdList(isFinish)
                                                                }
                                                            }
                                                            exec()
                                                        }
                                                    }
                                                }.start()
                                            }else{
                                                runOnUiThread {
                                                    setMrtIdList(isFinish)
                                                }
                                            }
                                        }
                                        exec()
                                    }
                                }
                            }.start()
                        }else{
                            runOnUiThread {
                                setMrtIdList(isFinish)
                            }
                        }
                    }
                }catch (e :JSONException){
                    runOnUiThread {
                        Log.d("JSON FORMAT ERROR!", "${e.message}")
                        if(dialog.isShowing){
                            dialog.dismiss()
                        }
                        Toast.makeText(this@FilterCommuteActivity,
                            getString(R.string.filter_commute_google_error),Toast.LENGTH_LONG).show()
                    }
                }
            }
            exec()
        }
    }

    //設定捷運清單
    private fun setMrtIdList(isFinish : Boolean){
        val tmpList = mrtList.toMutableList()
        //設定時，把IdList清空
        mrtIdList.clear()
        if(this::dialog.isInitialized && dialog.isShowing){
            dialog.dismiss()
        }
        for(i in 0 until mrtJsonArray.length()){
            try{
                if(tmpList.size == 0){
                    break
                }
                val jsonObject = mrtJsonArray.getJSONObject(i)
                for(j in 0 until tmpList.size){
                    if(tmpList[j].contains(jsonObject.getString("mrtStationName"))){
                        if(!mrtIdList.contains(jsonObject.getInt("mrtStationId"))){
                            mrtIdList.add(jsonObject.getInt("mrtStationId"))
                        }
                        tmpList.removeAt(j)
                        break
                    }
                }
            }catch (e :JSONException){
                Toast.makeText(this, getString(R.string.filter_mrt_error_info),
                    Toast.LENGTH_LONG).show()
                Log.d("JSON FORMAT ERROR!", "${e.message}")
            }
        }
        //Debug資訊，可刪除
        val str = StringBuilder()
        mrtList.forEach {
            str.append(it).append(",")
        }
        mrtIdList.forEach {
            str.append(it).append(",")
        }
        Log.d("Choose MrtList", String(str))
        //判斷如果捷運列表是空的話，就不給存
        when {
            mrtIdList.isEmpty() -> {
                Toast.makeText(this,getString(R.string.filter_commute_no_mrt_error)
                    ,Toast.LENGTH_LONG).show()
            }
            isFinish -> {
                saveAndFinish()
            }
            else -> {
                Toast.makeText(this,getString(R.string.filter_commute_mrt_list_ready)
                    ,Toast.LENGTH_SHORT).show()
                lastTime = time
            }
        }
    }

    //拿伺服器的捷運清單，以比對捷運代碼
    private fun getMrtJSONArray(){
        //從後端拿捷運清單，並初始化map
        NetworkController.instance.getMrt().run {
            onFailure = {errorCode, msg ->
                runOnUiThread {
                    Log.d("OnFailure", "$errorCode:$msg")
                    Toast.makeText(this@FilterCommuteActivity,
                        getString(R.string.filter_mrt_error_info),Toast.LENGTH_LONG).show()
                } }
            onResponse = {res ->
                try {
                    val jsonArray = JSONArray(res)
                    if(jsonArray.length() > 0){
                        runOnUiThread {
                            mrtJsonArray = jsonArray
                        }
                    }else{
                        throw JSONException("JSONArray Length is zero!")
                    }
                }catch (e : JSONException  ){
                    runOnUiThread {
                        Log.d("JSON FORMAT ERROR!", "${e.message}")
                        Toast.makeText(this@FilterCommuteActivity,
                            getString(R.string.filter_mrt_error_info),Toast.LENGTH_LONG).show()
                    }
                }
            }
            exec()
        }

    }

    private fun saveAndFinish(){
        val commute = Commute(Landmark(title, address, distance, lng, lat), transport, time , mrtIdList)
        RadarController.instance.getRadar().regionList = null
        RadarController.instance.getRadar().landmark = null
        RadarController.instance.getRadar().mrtList = null
        RadarController.instance.getRadar().commute = commute
        finish()
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}