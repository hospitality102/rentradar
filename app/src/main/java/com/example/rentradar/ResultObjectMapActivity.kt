package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.NetworkStateObserver
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_result_object_map.*

class ResultObjectMapActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private lateinit var resultName :String
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_object_map)

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        resultName = intent.extras?.getString(Global.BundleKey.HOUSE_NAME) ?:"CMoney"
        //設定返回鈕
        topAppBar.setNavigationOnClickListener {
            run{
                finish()
            }
        }
        topAppBar.title = resultName

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //接收從物件內頁傳過來的經緯度、名稱
        val locals = intent.extras?.getDoubleArray(Global.BundleKey.RESULT_LOCATION)
            ?: doubleArrayOf(Global.DEFAULT_LAT, Global.DEFAULT_LNG)
        //增加地標
        val latLng = LatLng(locals[0], locals[1])
        mMap.run{
            //新增標示
            addMarker(MarkerOptions().position(latLng).title(resultName))
            //初始時定位+zoom多寡
            moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            mapType = GoogleMap.MAP_TYPE_NORMAL
            //取消室內平面圖
            isIndoorEnabled = false
            //呼叫縮放工具列
            uiSettings.isZoomControlsEnabled = true
            setMinZoomPreference(10f)
            setMaxZoomPreference(20f)
        }

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