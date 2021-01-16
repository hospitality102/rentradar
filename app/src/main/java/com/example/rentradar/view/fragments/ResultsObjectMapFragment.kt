package com.example.rentradar.view.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rentradar.R
import com.example.rentradar.RadarActivity
import com.example.rentradar.utils.ActivityController

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.page_results_object_map.view.*

class ResultsObjectMapFragment : Fragment() {

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        val sydney = LatLng(25.020643301476603, 121.46728727586955)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        googleMap.run {
            addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            //初始時定位+zoom多寡
            moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 18f))
            mapType = GoogleMap.MAP_TYPE_NORMAL
            //取消室內平面圖
            isIndoorEnabled = false
            //呼叫縮放工具列
            uiSettings.isZoomControlsEnabled = true
            setMinZoomPreference(10f)
            setMaxZoomPreference(20f)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.page_results_object_map, container, false)

        view.topAppBar.setNavigationOnClickListener {
            run{
                ActivityController.instance.startActivity(this.activity!!, RadarActivity::class.java)
                this.activity?.finish()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
}