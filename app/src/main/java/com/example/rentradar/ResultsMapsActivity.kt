package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.rentradar.models.House
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.NetworkStateObserver
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_results_maps.*

class ResultsMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver = NetworkStateObserver(this,supportFragmentManager)
    private var items: ArrayList<House>? = ArrayList()
    private var defaultCamera: LatLng = LatLng(25.033671, 121.564427)
    private var averagePrice=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results_maps)

        //接收物件資料
        val bundle = intent.extras
        items = bundle?.getParcelableArrayList<House>(Global.BundleKey.RESULT_TO_MAP)
        averagePrice=bundle?.getInt(Global.BundleKey.AVERAGEPRICE)?:0

        //移動鏡頭至第一個物件
        if (!items.isNullOrEmpty()) {
            defaultCamera = LatLng(items!![0].lat, items!![0].lng)
        }

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //頂部狀態列點擊事件
        topAppBar.setNavigationOnClickListener {
            ActivityController.instance.startActivity(
                this,
                ResultsActivity::class.java
            )
            finish()
        }
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
        mMap.run {
            setOnInfoWindowClickListener {
                val str = it.title.split("/")
                val itemSerialNum = str[0].toInt()
                val bundle = Bundle()
                bundle.putInt(Global.BundleKey.AVERAGEPRICE, averagePrice)
                bundle.putInt(Global.BundleKey.HOUSE_NAME, itemSerialNum)
                bundle.putInt(
                    Global.BundleKey.RESULT_OBJECT_BACK_INDEX,
                    Global.BackIndex.RESULTS_LIST.serial
                )
                //跳轉物件內頁
                ActivityController.instance.startActivity(
                    this@ResultsMapsActivity,
                    ResultsObjectActivity::class.java, bundle
                )
                finish()
            }
            //呼叫縮放工具列
            uiSettings.isZoomControlsEnabled = true
            setMinZoomPreference(10f)
            setMaxZoomPreference(20f)
        }


        // Add a marker in Sydney and move the camera
        //加入標籤
        items?.forEach {
            val latLng = LatLng(it.lat, it.lng)

            mMap.addMarker(
                MarkerOptions().position(latLng).title("${it.serialNum}/${it.title}")
                    .snippet("\$${it.price}")
            )

        }

//預設鏡頭
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCamera, 16f))


    }

    override fun onResume() {
        if (!networkStateObserver.isRegister) {
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