package com.example.rentradar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.example.rentradar.models.Landmark
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.ErrorHintDialog
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_filter_landmark.*
import kotlinx.android.synthetic.main.activity_filter_landmark.mapView
import kotlinx.android.synthetic.main.activity_filter_landmark.topAppBar
import kotlinx.android.synthetic.main.activity_filter_landmark.tvAddress
import org.json.JSONException
import org.json.JSONObject

class FilterLandmarkActivity : AppCompatActivity() , LocationSource, LocationListener{

    init {
        if(!RadarController.instance.checkRadar()){
            finish()
        }
    }

    //地圖圓形、標記使用
    private lateinit var circle: Circle
    private lateinit var myMap: GoogleMap
    private lateinit var marker: Marker
    //從雷達拿地標的相關資料
    private var address = RadarController.instance.getRadar().landmark?.address
    private var addressTitleName = RadarController.instance.getRadar().landmark?.title
    private var lat = RadarController.instance.getRadar().landmark?.latitude ?:0.0
    private var lng = RadarController.instance.getRadar().landmark?.longitude ?:0.0
    private var distance = RadarController.instance.getRadar().landmark?.range ?: 0.5
    //拿到定位變更的監聽、定位系統的服務
    private var locationManager : LocationManager? = null
    private var locationChangedListener: LocationSource.OnLocationChangedListener? = null
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)

    @SuppressLint("MissingPermission", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_landmark)

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        tvDistance.text = distance.toString()

        //取得定位的系統服務
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //設定返回按鈕
        topAppBar.setNavigationOnClickListener {
            run{
                finish()
            }
        }
        topAppBar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                //設定完成按鈕，將選擇的資料存入雷達
                R.id.done->{
                    run{
                        RadarController.instance.getRadar().regionList = null
                        RadarController.instance.getRadar().landmark = Landmark(addressTitleName!!, address!!, distance, lng, lat)
                        RadarController.instance.getRadar().mrtList = null
                        RadarController.instance.getRadar().commute = null
                        finish()
                    }
                    true
                }
                else-> false
            }
        }

        //設定googleApi憑證 - URL API
        Places.initialize(this, Global.GOOGLE_MAP_API_KEY)
        etSearch.run{
            isFocusable = false
            setOnClickListener {
                if(!DoubleClickGuard.isFastDoubleClick()){
//定義要顯示的資訊
                    val fieldList: List<Place.Field> = listOf(
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG,
                        Place.Field.NAME
                    )
                    //將搜尋範圍（自動填入）定位在台灣區域
                    val bounds = RectangularBounds.newInstance(
                        LatLng(Global.TAIWAN_RANGE_LEFT.latitude, Global.TAIWAN_RANGE_LEFT.longitude),
                        LatLng(Global.TAIWAN_RANGE_RIGHT.latitude, Global.TAIWAN_RANGE_RIGHT.longitude)
                    )
                    //跳轉的設定
                    val intent: Intent = Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY,
                        fieldList
                    )
                        .setLocationRestriction(bounds)
                        .setInitialQuery(if(etSearch.text.toString() == "我的位置"){
                            ""
                        }else{
                            etSearch.text.toString()
                        })
                        .build(this@FilterLandmarkActivity)
                    //開啟自動搜尋輸入框
                    startActivityForResult(intent, Global.SEARCH_VIEW)
                    //結果設定在下方的onActivityResults(當按下自動搜尋的資訊後觸發）
                }
            }
        }

        //初始化mapView
        mapView.run {
            onCreate(savedInstanceState)
            getMapAsync { googleMap ->
                myMap = googleMap
                myMap.run {
                    //如果地址是空的、或是不在台灣的話，就用定位找
                    if(address.isNullOrEmpty()
                        || lat < Global.TAIWAN_RANGE_LEFT.latitude
                        || lat > Global.TAIWAN_RANGE_RIGHT.latitude
                        || lng < Global.TAIWAN_RANGE_LEFT.longitude
                        || lng > Global.TAIWAN_RANGE_RIGHT.longitude){
                        searchMyLocation(distance)
                    }else{
                        //不然就直接依照之前的資訊畫在地圖上
                        etSearch.setText(addressTitleName)
                        tvAddress.text = addressTitleName
                        drawLocation(LatLng(lat, lng), distance)
                    }
                    mapType = GoogleMap.MAP_TYPE_NORMAL
                    isIndoorEnabled = false

                    //設定地圖格式、不開啟室內功能、開啟定位功能
                    if( ContextCompat.checkSelfPermission(this@FilterLandmarkActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED){
                        isMyLocationEnabled = true
                        //設定定位功能按下後，自動找自己的位置，並將顯示資訊更新
                        setLocationSource(this@FilterLandmarkActivity)
                        setOnMyLocationButtonClickListener {
                            searchMyLocation(distance)
                            return@setOnMyLocationButtonClickListener true
                        }
                    }

                    //將google工具列隱藏
                    uiSettings.isCompassEnabled = true
                    uiSettings.isZoomControlsEnabled = true
                    uiSettings.isMyLocationButtonEnabled = true
                    uiSettings.isMapToolbarEnabled = false
                    uiSettings.isMyLocationButtonEnabled = true
                }
            }
        }

        //用拉bar選擇距離範圍
        //設定拖曳鈕目前的位置數值- 因為每格都是0.5km，除0.5已反推
        seekBar.progress = (distance / 0.5).toInt()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //距離隨著目前的位置改變， progress.times(0.5) = progress * 0.5；並顯示距離資訊
                distance = seekBar?.progress?.times(0.5) ?: 0.0
                tvDistance.text = distance.toString()
                circle.radius = distance * 1000
            }
        })

    }

    //自動填入選擇後，會得到名字、經緯度、地址，直接用此畫圖不用在Call名字查詢。
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Global.SEARCH_VIEW && resultCode == Activity.RESULT_OK){
            val place = Autocomplete.getPlaceFromIntent(data!!)
            Log.d("FilterLandmark_onActivityResult_place", place.toString())
            //修改抬頭、地址 -> 會變成用雷達去改
            addressTitleName = place.name ?:""
            address = place.address ?:""
            lat = place.latLng?.latitude ?:Global.DEFAULT_LAT
            lng = place.latLng?.longitude ?:Global.DEFAULT_LNG
            val local = LatLng(lat, lng)
            //將文字顯示變更，並標示地圖。
            tvAddress.text = addressTitleName
            etSearch.setText(addressTitleName)
            drawLocation(local, distance)
        }else{
            //如果失敗的話就default值
            addressTitleName = getString(R.string.filter_landmark_default_address)
            address = "220新北市板橋區文化路一段266號15號"
            tvAddress.text = addressTitleName
            drawLocation(LatLng(Global.DEFAULT_LAT, Global.DEFAULT_LNG), distance)
        }
    }

    //搜尋我的位置的地址使用-標示+找詳細地址
    private fun searchMyLocation(distance: Double){
        //檢查有沒有GPS定位、網路定位的權限，沒的話就不能定位
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        //取得最近一次GPS定位點
        var location :Location? = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //如果沒有資料，就改用網路定位找
        if(location == null){
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                5f,
                this@FilterLandmarkActivity
            )
            location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        //將經緯度改成搜尋到的結果
        location?.run{
            lat = latitude
            lng = longitude
        }
        //透過這個經緯度，去拿取地址的詳細資訊（完整地址） -> 要補上error處理
        NetworkController.instance.getLocationByLatlng(lat, lng)
            .run{
                onFailure = {errorCode, msg ->
                    if(lifecycle.currentState == Lifecycle.State.RESUMED){
                        Toast.makeText(this@FilterLandmarkActivity, getString(R.string.filter_landmark_my_location_error),
                            Toast.LENGTH_LONG).show()
                    }
                    Log.d("OnFailure", "$errorCode: $msg") }
                onResponse = {res ->
                    try {
                        //將google的詳細地址記錄下來，因為經緯度都已經有惹，只是沒有詳細的地址。
                        val jsonObject = JSONObject(res)
                            .getJSONArray("results")
                            .getJSONObject(0)
                        address = jsonObject.getString("formatted_address")
                        addressTitleName = getString(R.string.filter_landmark_my_location)
                        val local = LatLng(lat, lng)
                        runOnUiThread {
                            tvAddress.text = addressTitleName
                            etSearch.setText(addressTitleName)
                            drawLocation(local, distance)
                        }
                    }catch(e : JSONException){
                        //目前初始或網路不穩就先到CMONEY
                        Log.d("JSON FORMAT Error!", e.message ?:"error!")
                        runOnUiThread {
                            addressTitleName = getString(R.string.filter_landmark_default_address)
                            address = "220新北市板橋區文化路一段266號15號"
                            tvAddress.text = addressTitleName
                            drawLocation(LatLng(Global.DEFAULT_LAT, Global.DEFAULT_LNG), distance)
                            if(lifecycle.currentState == Lifecycle.State.RESUMED){
                                Toast.makeText(this@FilterLandmarkActivity, getString(R.string.filter_landmark_my_location_error),
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                exec()
            }
    }

    //在地圖上畫座標、圓形
    private fun drawLocation(local: LatLng, distance:Double){
        val circleOptions = CircleOptions()
            .center(local)
            .radius(distance * 1000)
            .strokeWidth(0f)
            .fillColor(getColor(R.color.map_green))
        if(this::circle.isInitialized){
            circle.remove()
        }
        if(this::marker.isInitialized){
            marker.remove()
        }
        if(this::myMap.isInitialized){
            myMap.run {
                //將地圖顯示在設定的座標上
                moveCamera(CameraUpdateFactory.newLatLngZoom(local, 16f))
                //標示物件
                circle = addCircle(circleOptions)
                marker = addMarker(MarkerOptions().position(local).title(addressTitleName).snippet(address))
            }
        }
    }

    //確認要求點選的結果，也就是使用者選的選項，是否有同意
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //檢查收到的權限要求編號跟我們自己設定的相同、並檢查是否同意以開啟定位
        if(requestCode == Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED){
            registerLocationManagerEnableIt(true)
            if(this::myMap.isInitialized){
                checkLocationPermission()
                myMap.run{
                    isMyLocationEnabled = true
                    //設定定位功能按下後，自動找自己的位置，並將顯示資訊更新
                    setLocationSource(this@FilterLandmarkActivity)
                    searchMyLocation(distance)
                    setOnMyLocationButtonClickListener {
                        searchMyLocation(distance)
                        return@setOnMyLocationButtonClickListener true
                    }
                }
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //取得定位權限的function
    private fun checkLocationPermission(){
        //檢查權限是否有開
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            //這項功能尚未取得使用者的同意->開始徵詢使用者的流程
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                val dialog = with(AlertDialog.Builder(this)) {
                    title = "提示"
                    setMessage("App需要啟動定位功能")
                    setIcon(android.R.drawable.ic_dialog_info)
                    setCancelable(false)
                    setPositiveButton("確定"
                    ) { _, _ ->
                        //顯示詢問使用者是否同意功能權限的彈出視窗、確認後會執行onRequestPermissionsResult()
                        ActivityCompat.requestPermissions(
                            this@FilterLandmarkActivity, arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ),
                            Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION
                        )
                    }
                }
                dialog.show()
            } else {
                //顯示詢問使用者是否同意功能權限的彈出視窗、確認後會執行onRequestPermissionsResult()
                ActivityCompat.requestPermissions(
                    this@FilterLandmarkActivity, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION
                )
            }
        }
    }


    private fun registerLocationManagerEnableIt(isStart : Boolean){
        checkLocationPermission()
        //如果有權限、要啟用，就設定locationManaher
        if (isStart && locationManager != null &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            //如果GPS功能有開啟，優先使用GPS定位，否則使用網路定位
            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //服務提供者、更新頻率5s、最短距離、地點改變時呼叫物件
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                Toast.makeText(this, "使用GPS定位", Toast.LENGTH_LONG).show()
            } else {
                if (locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    //服務提供者、更新頻率5s、最短距離、地點改變時呼叫物件
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        5f,
                        this
                    )
                    Toast.makeText(this, "使用網路定位", Toast.LENGTH_LONG).show()
                } else {
                    //將目前的監聽關閉
                    locationManager!!.removeUpdates(this)
                    Toast.makeText(this, "定位功能已經停用", Toast.LENGTH_LONG).show()
                }
            }
        }else{
            //將目前的監聽關閉
            locationManager?.removeUpdates(this)
        }

    }

    //手機的位置改變時會呼叫這個方法，location物件包含最新的定位資訊，第一次定位用
    override fun onLocationChanged(location: Location) {
        //把新的位置傳給GoogleMap的my-location layer，適合用在邊行走邊定位時，也就是導航唷～
//        Log.e("CameraLocation2", "location: ${location.toString()}")
        locationChangedListener?.onLocationChanged(location)
        //移動地圖到新位置
        if(this::myMap.isInitialized){
            myMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
        }
        // mMap.animateCamera(CameraUpdateFactory.zoomTo(16f))
    }

    //手機的定位功能狀態改變會用到這個方法
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onStart() {
        mapView.onStart()
        //啟動定位功能
        registerLocationManagerEnableIt(true)
        super.onStart()
    }

    override fun onResume() {
        if(!networkStateObserver.isRegister){
            networkStateObserver.register()
        }
        mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        if(networkStateObserver.isRegister){
            networkStateObserver.unregister()
        }
        mapView.onStop()
        //停止定位
        registerLocationManagerEnableIt(false)
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        mapView.onLowMemory()
        super.onLowMemory()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun deactivate() {
        //手機定位被關閉會啟用這個方法
        locationChangedListener = null
        registerLocationManagerEnableIt(false)
    }

    override fun activate(p0: LocationSource.OnLocationChangedListener?) {
        //手機定位功能開啟使用這個方法
        locationChangedListener = p0
    }

    //可以查查怎麼改用網路位置定位，避免GPS GG 就完全G
    override fun onProviderDisabled(provider: String) {
        Log.d("onProviderDisabled", "error!")
        if(lifecycle.currentState == Lifecycle.State.RESUMED ){
            val errorHintDialog = ErrorHintDialog(getString(R.string.filter_landmark_no_gps))
            errorHintDialog.action = {
                finish()
            }
            errorHintDialog.show(supportFragmentManager,"OnProviderDisabled!")
        }

    }

    override fun onProviderEnabled(provider: String) {
        Log.d("onProviderEnabled", "connectGPS!")
    }


}