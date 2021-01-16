package com.example.rentradar.view.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import com.example.rentradar.FilterActivity
import com.example.rentradar.R
import com.example.rentradar.utils.RadarController
import kotlinx.android.synthetic.main.activity_filter.*

class RentRangeDialog(private val activity: Activity, private val isStart:Boolean){

    //因為不需要帶parent，因此跟IDE說不用檢查這個錯誤（系統不希望是null）
    @SuppressLint("InflateParams")
    val itemView  = LayoutInflater.from(activity).inflate(R.layout.dialog_filter_rent, null)!!
    private val numberPicker = itemView.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberPick)
    private val tvRentRange = itemView.findViewById<TextView>(R.id.tvRentRange)
    private val btnConfirm = itemView.findViewById<Button>(R.id.btnConfirm)
    private val btnCancel = itemView.findViewById<Button>(R.id.btnCancel)
    //numberPicker 改成文字的資料
    private val rentRange : Array<String> = arrayOf("0", "3000", "4000", "5000", "6000", "7000", "8000",
        "9000", "10000", "12500" , "15000", "20000", "25000", "30000", "35000", "40000", activity.getString(R.string.filter_rent_all))
    //紀錄原始資料，及現在所選的資料
    private lateinit var originalStart :String
    private lateinit var originalEnd:String
    private var startIndex = 0
    private var endIndex = rentRange.size - 1
    private var isConfirm = false

    //設定dialog，按外部不可取消，並監聽被關閉時看是否是確認去改變對應資訊
    val dialog = AlertDialog.Builder(activity)
            .setView(itemView)
            .setCancelable(false)
            .setOnDismissListener {
                //設定取消時要做的事情
                if(activity is FilterActivity){
                    if(isConfirm){
                        //如果使用者是設定結尾時選擇0，自動變成無限，因為不能是0
                        if(!isStart && endIndex == 0){
                            endIndex = rentRange.size - 1
                        }
                        //如果關閉時，頭比尾大，頭自動變成0
                        if(startIndex >= endIndex){
                            startIndex = 0
                        }
                        //更新雷達中的設定，並改變顯示文字
                        RadarController.instance.getRadar().minPrice = rentRange[startIndex].toInt()
                        RadarController.instance.getRadar().maxPrice =
                            if(endIndex == rentRange.size - 1){
                                99999
                            }else{
                                rentRange[endIndex].toInt()
                            }
                        activity.btnStartRent.text = rentRange[startIndex]
                        activity.btnEndRent.text = rentRange[endIndex]
                    }else{
                        //如果是取消，就回歸原樣，並改變顯示文字
                        RadarController.instance.getRadar().minPrice = originalStart.toInt()
                        RadarController.instance.getRadar().maxPrice =
                            if(originalEnd == activity.getString(R.string.filter_rent_all)){
                                99999
                            }else{
                                originalEnd.toInt()
                            }
                        activity.btnStartRent.text = originalStart
                        activity.btnEndRent.text = originalEnd
                    }

                }
            }
            .create()!!

    //dialog的運行判斷
    init {
        btnConfirm.setOnClickListener {
            run{
                isConfirm = true
                dialog.dismiss()
            }
        }
        //取消時就回歸原樣
        btnCancel.setOnClickListener {
            run{
                isConfirm = false
                dialog.dismiss()
            }
        }

        //確認activity是否有實現RentRange
        numberPicker.run {
            //1.將字串清單加入numberPicker、並關閉可手動輸入的功能
            displayedValues = rentRange
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            //2.取出當前的起始、結束位置，用以紀錄使用者上次的操作，
            // 因為有先給初始極值(最小=0，最大=長度-1），因此可以略過判斷
            for(i in 1..displayedValues.size - 2){
                if(RadarController.instance.getRadar().minPrice == displayedValues[i].toInt()){
                    startIndex = i
                }
                if(RadarController.instance.getRadar().maxPrice == displayedValues[i].toInt()){
                    endIndex = i
                }
            }
            //暫存一開始的選項，以利取消時取出
            originalStart = rentRange[startIndex]
            originalEnd = rentRange[endIndex]
            //3.設定起始點及上限值、標題名稱
            minValue = 0
            if(isStart){
                tvRentRange.text = activity.getString(R.string.filter_rent_startrent)
                value = startIndex
                maxValue = endIndex - 1
            }else{
                tvRentRange.text = activity.getString(R.string.filter_rent_endrent)
                value = endIndex
                maxValue = displayedValues.size - 1
            }
            //4.感測目前滑動到哪筆有滑動就改變其值，直到確定被關閉時才儲存
            setOnValueChangedListener { _, _, newVal ->
                if(isStart){
                    startIndex = newVal
                }else{
                    endIndex = newVal
                }
            }
        }
    }

}