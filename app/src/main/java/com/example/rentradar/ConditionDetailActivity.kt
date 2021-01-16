package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import kotlinx.android.synthetic.main.activity_condition_detail.*
import kotlinx.android.synthetic.main.item_list_condition_detail.view.*
import kotlinx.android.synthetic.main.item_list_condition_ditail_header.view.*
import kotlin.math.pow

class ConditionDetailActivity : AppCompatActivity() {


    //用於RecycleView item使用
    class Detail(val name:String) : IType{
        override val getItemType: Int
            get() = Global.ItemType.CONDITION_DITAIL
    }

    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_condition_detail)

        if(!RadarController.instance.checkRadar()){
            finish()
        }

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        //獲得前個場景傳進來的condition複合列舉、抬頭名稱，以呼叫對應的細項條件
        val responseBundle = intent.extras
        val conditionName = responseBundle?.getStringArrayList(Global.BundleKey.CONDITION_NAME)
            ?: arrayListOf()
        val titleName   = responseBundle?.getString(Global.BundleKey.TITLENAME)
            ?: "系統異常，請返回並告知客服人員，感謝您！"
        //雷達的map設定
        var condition = RadarController.instance.getRadar().conditionMap[titleName] ?:0
        topAppBar.title= titleName

        //設定返回按鈕，將資料傳回去
        topAppBar.setNavigationOnClickListener {
            run{
                if(RadarController.instance.getRadar().conditionMap.containsKey(titleName)){
                    RadarController.instance.getRadar().conditionMap[titleName] = condition
                }
                finish()
                //返回時左進右出
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }

        // 創建recycleView的Manager，如果資料數量少時，讓其無法上下捲動
        val layoutManager =
            if(conditionName.size < Global.MagicNumber.MIN_CONDITION) {
                object : LinearLayoutManager(this){
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
            }else{
                LinearLayoutManager(this)
            }
        rvDetail.layoutManager = layoutManager

        val items = mutableListOf<IType>()
        // 0都為不限，因此從第一個開始
        for(i in 1 until conditionName.size){
            items.add(Detail(conditionName[i].toString()))
        }

        val commandAdapter = with(CommonAdapter(items)){
            apply {
                //第一個為不限，條件設定跟其他不同，因取名相同在此用findViewById避免衝突
                addHeader{parent ->
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_ditail_header, parent, false)
                    view.cbAll.setOnClickListener {
                        condition = 0
                        this@with.notifyDataSetChanged()
                    }
                    object :BaseViewHolder<IType>(view){
                        override fun bind(item: IType) {
                            //更改按鈕狀態
                            view.cbAll.isChecked = (condition == 0)
                        }
                    }
                }
                //之後的，設定名稱及點擊事件
                addType(Global.ItemType.CONDITION_DITAIL){parent ->
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_detail, parent, false)
                    object : BaseViewHolder<Detail>(view){
                        override fun bind(item: Detail) {
                            // -1 是因為第一個是不限，要去除
                            val serial = super.getAdapterPosition() - 1
                            view.cbName.setOnClickListener {
                                //用二進位的方式判段目前該欄位是否處於被選擇的狀態，是的話=取消(-)，否則=增加(+)
                                if(condition.shr(serial) % 2 == 1){
                                    condition -=2.0.pow(serial).toInt()
                                }else{
                                    condition += 2.0.pow(serial).toInt()
                                }
                                this@with.notifyDataSetChanged()
                            }
                            view.cbName.isChecked = (condition.shr(serial) % 2 == 1)
                            view.cbName.text = item.name
                        }
                    }
                }
            }
        }
        rvDetail.adapter = commandAdapter
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

    override fun onBackPressed() {
        super.onBackPressed()
        //返回時左進右出
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

}