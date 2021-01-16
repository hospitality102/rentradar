package com.example.rentradar.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.rentradar.R
import com.example.rentradar.RadarActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("MyFirebaseServiceData", remoteMessage.data.toString())
        //將從推播(後端)得到的資訊帶給首頁
        val bundle = Bundle()
        bundle.putBoolean(Global.SharePath.PUSH_INFO, true)
        sendNotification("租屋雷達",
            "您的雷達已搜尋到${remoteMessage.data["newRentalCount"]}筆新物件，趕快看看！",
            bundle)
        remoteMessage.notification?.run{
            Log.d("MyFirebaseServiceNotification", "title:$title/body:$body")
            sendNotification(title?:"", body?:"", bundle)
        }

    }

    //拿到本機FCM的token
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFirebaseService", "token: $token")
    }

    //通知欄UI
    private fun sendNotification(messageTitle :String, messageBody:String, bundle: Bundle){
        val intent = Intent(this, RadarActivity::class.java)
        //跳轉過去時將他以上(含)的activity都清除，重新創建
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtras(bundle)
        //點擊跳轉畫面，在被觸發時，會執行startActivity，如果是getService就是startService以此類推
        //OneShot: 此intent只能使用一次
        val pendingIntent =
            PendingIntent.getActivity(this, 0 ,intent, PendingIntent.FLAG_ONE_SHOT)
        //Android8以上 規定要設置channelId
        val channelId = getString(R.string.push_channel_id)
        //取得預設鈴聲
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        //通知欄 -設定抬頭、icon、字體顏色、內容、點選後將通知關閉、提示鈴聲、intent(點擊後的事件-跳去雷達首頁)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(messageTitle)
            .setSmallIcon(R.drawable.rent_radar_icon_push)
            .setColor(getColor(R.color.blue_800))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        //取得系統通知服務(通知控制)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        //Oreo (Android8 = 26)以上版本需要channel Id
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "rent_radar_name",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        //將上面設定好的通知送給android，將訊息顯示在狀態欄上。
        notificationManager.notify(0, notificationBuilder.build())
    }
    
}