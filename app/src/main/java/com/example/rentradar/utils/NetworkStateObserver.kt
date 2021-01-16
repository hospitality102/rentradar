package com.example.rentradar.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.IntentFilter
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.Build
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.example.rentradar.utils.NetworkStateObserver.ConnectionType.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog

class NetworkStateObserver(private val activity: Activity,private val manager:FragmentManager) {

    //用來確認連接的類型
    enum class ConnectionType {
        WIFI, CELLULAR , VPN
    }

    //如果有要顯示視窗的話的dialog
    var dialog: InternetErrorHintDialog? = null
    //確認有沒有被訂閱過，因為目前都還寫在onCreate，所以保險起見onResume也要再確認是否有被訂閱過，沒有的話要補訂閱
    var isRegister = false

    //當網路狀態是否有連上、狀態時要做的動作，可以被改寫，預設先寫好
    //邏輯判斷應該寫在別的地方，而不該在監聽這邊做
    var result: ((isAvailable: Boolean, type: ConnectionType?) -> Unit) = {isAvailable, type ->
        activity.runOnUiThread {
            when(isAvailable){
                true -> {
                    when(type){
                        WIFI -> { Log.d("NetworkState", "WIFI網路連線正常！") }
                        CELLULAR -> { Log.d("NetworkState", "手機網路連線正常！") }
                        VPN -> {Log.d("NetworkState", "VPN網路連線正常！") }
                        else -> {
                            manager.let {
                                Log.d("NetworkState", "網路連線失敗，請連線以繼續使用！")
                                dialog?.run{
                                    if(!isAdded){
                                        show(it, "")
                                    }
                                }
                            }
                        }
                    }
                }
                false -> {
                    manager.let {
                        dialog?.run{
                            if(!isAdded){
                                show(it, "")
                            }
                        }
                        Log.d("NetworkState", "網路連線失敗，請連線以繼續使用！")
                    }
                }
            }
        }
    }

    // Android 9 以上接收網路狀態的方式及其判斷，實現NetworkCallback
    private val networkCallback = object : NetworkCallback() {
        //未連結到網路時
        override fun onLost(network: Network) {
            super.onLost(network)
            result.invoke(false, null)
        }
        //網路狀態改變時，確認其狀態
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // WIFI
                    result.invoke(true, WIFI)
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // CELLULAR
                    result.invoke(true, CELLULAR)
                }
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)-> {
                    // VPN
                    result.invoke(true, VPN)
                }
                else -> {
                    //其他算錯誤
                    result.invoke(false, null)
                }
            }
        }
    }

    //因為有已經被淘汰的資料，所以要添加忽略警告，因為已經有做判斷處理
    //給Android 8 接收網路狀態的方式及其判斷，繼承BroadcastReceiver
    @Suppress("DEPRECATION")
    private val networkChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //拿取網路狀態
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            //null代表沒連到網路
            if (activeNetworkInfo != null) {
                // 判斷是WIFI、連網、其他就當作連線失敗
                when (activeNetworkInfo.type) {
                    // WIFI
                    ConnectivityManager.TYPE_WIFI -> {
                        result.invoke(true,  WIFI)
                    }
                    // CELLULAR - 手機自己的網路
                    ConnectivityManager.TYPE_MOBILE -> {
                        result.invoke(true,  CELLULAR)
                    }
                    // VPN - WIFI 或 手機網路可能會用
                    ConnectivityManager.TYPE_VPN -> {
                        result.invoke(true, VPN)
                    }
                    else -> {
                        //其他代碼，算錯誤
                        result.invoke(false, null)
                    }
                }
            } else {
                // 錯誤，沒連上線
                result.invoke(false, null)
            }
        }
    }

    //因為有已經被淘汰的資料，所以要添加忽略警告，因為已經有做判斷處理
    @Suppress("DEPRECATION")
    //訂閱，開始監聽網路狀態
    fun register() {
        isRegister = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android9以上要使用NetworkCallback，使用的是networkCallback
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager?
            //如果activeNetwork是null，代表沒連到網路
            if (connectivityManager == null || connectivityManager.activeNetwork == null) {
                result.invoke(false,null)
            }
            //將網路監聽設定default的networkCallback
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } else {
            // Android8以下要使用Intent Filter，使用的是networkChangeReceiver
            val intentFilter = IntentFilter()
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
            activity.registerReceiver(networkChangeReceiver, intentFilter)
        }
    }

    //解除訂閱網路狀態
    fun unregister() {
        isRegister = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val connectivityManager =
                activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } else {
            activity.unregisterReceiver(networkChangeReceiver)
        }
    }




}

