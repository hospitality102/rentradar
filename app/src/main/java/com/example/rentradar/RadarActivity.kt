package com.example.rentradar

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.transition.Fade
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.NetworkStateObserver
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class RadarActivity : AppCompatActivity() {
    private var lastTime: Long = 0

    //新增觀察者，在Resume及stop時訂閱及取消訂閱
    private val networkStateObserver = NetworkStateObserver(this, supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)


        //確認定位權限
        checkLocationPermissionAndEnableIt()

        //因為創建時就會用到網路，因此在onCreate訂閱
        //監聽網路事件，之後改成品駿寫好的dialog，因為實體都是同一個，因此dialog設定一次即可。

        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()
        val viewPagerFragments: ViewPager2 = findViewById(R.id.vp_radar)
        val adapter = RadarPageAdapter(supportFragmentManager, lifecycle)
        val tabLayout: TabLayout = findViewById(R.id.tb_main)
        val title: ArrayList<String> = arrayListOf("雷達列表", "收藏列表")
        val bundle = intent.extras
        val isFavorite = bundle?.getBoolean(Global.BundleKey.ISFAVORITE)

        //設定viewPager2的轉接器
        viewPagerFragments.adapter = adapter
        //將viewPager2與tab連結一起
        TabLayoutMediator(tabLayout, viewPagerFragments) { tab, position ->
            tab.text = title[position]
        }.attach()

        if (isFavorite != null && isFavorite) {
            viewPagerFragments.currentItem = 1
        }

    }

    override fun finish() {
        // 記錄每次觸發的時間
        val currentTime = System.currentTimeMillis()
       // 計算時間差
        if (currentTime - lastTime > 3 * 1000) {
            // 儲存這一次的時間
            lastTime = currentTime
            Toast.makeText(this, "再按一下確認離開！", Toast.LENGTH_SHORT).show()
        } else {
            // 離開
            super.finish()
        }
    }

    //點擊輸入框外 取消焦點
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view: View? = currentFocus
            if (isHideInput(view, ev)) {
                hideSoftInput(view!!.windowToken)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
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

    private fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val manager: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //檢查收到的權限要求編號跟我們自己設定的相同、並檢查是否同意以開啟定位
        if (requestCode == Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.recreate()
            return
        }else{
            checkLocationPermissionAndEnableIt()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    //取得訂位權限的function
    private fun checkLocationPermissionAndEnableIt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            //這項功能尚未取得使用者的同意->開始徵詢使用者的流程
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                val dialog = with(AlertDialog.Builder(this)) {
                    title = "提示"
                    setMessage("App需要啟動定位功能")
                    setIcon(android.R.drawable.ic_dialog_info)
                    setCancelable(false)
                    setPositiveButton(
                        "確定"
                    ) { _, _ -> //顯示詢問使用者是否同意功能權限的彈出視窗、確認後會執行onRequestPermissionsResult()
                        ActivityCompat.requestPermissions(
                            this@RadarActivity, arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ),
                            Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION
                        )
                    }
                }
                dialog.show()
                return
            } else {
                //顯示詢問使用者是否同意功能權限的彈出視窗、確認後會執行onRequestPermissionsResult()
                ActivityCompat.requestPermissions(
                    this@RadarActivity, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    Global.REQUEST_PERMISSION_FOR_ACCESS_FINE_LOCATION
                )
                return
            }
        }
    }

    override fun onResume() {
        if (!networkStateObserver.isRegister) {
            networkStateObserver.register()
        }
        super.onResume()
    }

    override fun onStop() {
        if (networkStateObserver.isRegister) {
            networkStateObserver.unregister()
        }
        super.onStop()
    }


}