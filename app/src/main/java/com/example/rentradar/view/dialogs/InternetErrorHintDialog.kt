package com.example.rentradar.view.dialogs


import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.rentradar.R
import com.example.rentradar.RadarActivity
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.NetworkStateObserver
import kotlinx.android.synthetic.main.dialog_error_hint.view.*

class InternetErrorHintDialog(private val activity:Activity, private val manager: FragmentManager): DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =inflater.inflate(R.layout.dialog_error_hint, parent, false)
        isCancelable = false
        view.btnRetry.setOnClickListener {
            this.dismiss()
            checkInternetConnect()
        }
        view.tvErrorHint.text = activity.getString(R.string.error_network)

        return view
    }

    //當網路狀態是否有連上、狀態時要做的動作，可以被改寫，預設先寫好
    var result: ((isAvailable: Boolean, type: NetworkStateObserver.ConnectionType?) -> Unit) =
        { isAvailable, type ->
        activity.runOnUiThread {
            when(isAvailable){
                true -> {
                    when(type){
                        NetworkStateObserver.ConnectionType.WIFI,
                        NetworkStateObserver.ConnectionType.CELLULAR,
                        NetworkStateObserver.ConnectionType.VPN -> {
                            Log.d("NetworkState", "網路連線正常！")
                            ActivityController.instance.startActivity(activity, RadarActivity::class.java)
                        }
                        else -> {
                            manager.let {
                                this.show(it, "")
                                Log.d("NetworkState", "網路連線失敗，請連線以繼續使用！")
                            }
                        }
                    }
                }
                false -> {
                    manager.let {
                        this.show(it,"")
                        Log.d("NetworkState", "網路連線失敗，請連線以繼續使用！")
                    }
                }
            }
        }
    }

    //因為有已經被淘汰的資料，所以要添加忽略警告，因為已經有做判斷處理
    @Suppress("DEPRECATION")
    private fun checkInternetConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android9以上要使用NetworkCallback，使用的是networkCallback
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager?
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            if(networkCapabilities == null){
                result.invoke(false, null)
            }
            networkCapabilities?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        // WIFI
                        result.invoke(true, NetworkStateObserver.ConnectionType.WIFI)
                    }
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        // CELLULAR
                        result.invoke(true, NetworkStateObserver.ConnectionType.CELLULAR)
                    }
                    hasTransport(NetworkCapabilities.TRANSPORT_VPN)-> {
                        // VPN
                        result.invoke(true, NetworkStateObserver.ConnectionType.VPN)
                    }
                    else -> {
                        //其他算錯誤
                        result.invoke(false, null)
                    }
                }
            }
        } else {
            // Android8以下拿取網路狀態
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            //null代表沒連到網路
            if (activeNetworkInfo != null) {
                // 判斷是WIFI、連網、其他就當作連線失敗
                when (activeNetworkInfo.type) {
                    // WIFI
                    ConnectivityManager.TYPE_WIFI -> {
                        result.invoke(true, NetworkStateObserver.ConnectionType.WIFI)
                    }
                    // CELLULAR - 手機自己的網路
                    ConnectivityManager.TYPE_MOBILE -> {
                        result.invoke(true, NetworkStateObserver.ConnectionType.CELLULAR)
                    }
                    // VPN - WIFI 或 手機網路可能會用
                    ConnectivityManager.TYPE_VPN -> {
                        result.invoke(true, NetworkStateObserver.ConnectionType.VPN)
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


}