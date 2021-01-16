package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentradar.models.MRT
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.ErrorHintDialog
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.activity_filter_mrt.*
import kotlinx.android.synthetic.main.activity_filter_mrt.topAppBar
import org.json.JSONArray
import org.json.JSONException
import java.lang.StringBuilder

class FilterMrtActivity : AppCompatActivity() {

    init {
        if(!RadarController.instance.checkRadar()){
            finish()
        }
    }

    //因為捷運線要顯示的名稱、實際key名諱不同，且要給顯示內頁表示各線路的捷運顏色不同，因此用enum整合
    enum class MrtLine(val zhName:String,val lineName:String , val color:Int){
        ZHONGHE_XINLU_LINE("北捷-中和新蘆線", "中和新蘆線",  R.color.mrt_zhonghe_xinlu_line),
        WENHU_LINE("北捷-文湖線", "文湖線",  R.color.mrt_wenhu_line),
        SONGSHAN_XINDIAN_LINE("北捷-松山新店線", "松山新店線", R.color.mrt_songshan_xindian_line),
        BANNAN_LINE("北捷-板南線","板南線",R.color.mrt_bannan_line),
//        CIRCULAR_LINE("北捷-環狀線", "環狀線", R.color.mrt_circular_line),
        TAMSUI_XINYI_LINE("北捷-淡水信義線","淡水信義線", R.color.mrt_tamsui_xinyi_line),
        XINBEITOU_BRANCH_LINE("北捷-新北投支線", "新北投支線", R.color.favority_red),
        GREEN_MOUNTAIN_LINE("新北捷-綠山線/淡海輕軌", "綠山線", R.color.title_green),
        AIRPORT_LINE("桃捷-桃園機場捷運線","機場線",  R.color.mrt_airport_line),
        ORANGE_LINE("高捷-橘線", "橘線", R.color.mrt_orange_line),
        RED_LINE("高捷-紅線", "紅線", R.color.mrt_red_line),
        CIRCULAR_LIGHT_RAIL("高雄-環狀輕軌","環狀輕軌", R.color.light_green_200)
    }

    class MrtItem(val lineInfo: MrtLine , val name:String , val isLast:Boolean, val stationID: Int):IType{
        override val getItemType: Int
            get() = Global.ItemType.FILTER_MRT
    }

    //解析捷運資料使用，捷運線名表、站名代碼、站名中文表3，不會變動
    private val lineNameArr = mutableListOf<String>()
    private val stationNameMap = hashMapOf<String, MutableList<String>>()
    private val stationIdMap = hashMapOf<String, MutableList<Int>>()
    //暫存的選擇及計算有無點擊，結束時將這個list替換
    private val mrtBooleanMap =  hashMapOf<String, BooleanArray>()
    private val mrtList : MutableList<MRT> = RadarController.instance.getRadar().mrtList?.toMutableList() ?: mutableListOf()
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)
    //伺服器異常、資料取不到時顯示的畫面
    private lateinit var errorHintDialog: ErrorHintDialog
    private val tag = "FilterMrtActivity"
    private var errorAction = {
        if(lifecycle.currentState == Lifecycle.State.RESUMED){
            runOnUiThread {
                errorHintDialog.action = { finish() }
                errorHintDialog.show(supportFragmentManager, tag)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_mrt)

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()
        errorHintDialog = ErrorHintDialog(getString(R.string.filter_mrt_error_info))

        val items = mutableListOf<IType>()
        val commandAdapter = CommonAdapter(items)

        //從後端拿捷運清單，並初始化map
        NetworkController.instance.getMrt().run {
            onFailure = {errorCode, msg ->
                errorAction.invoke()
                Log.d("OnFailure", "$errorCode:$msg")
            }
            onResponse = {res ->
                try{
                    val mrtLineArray = JSONArray(res)
                    if(mrtLineArray.length() > 0){
                        runOnUiThread {
                            initializeData(mrtLineArray)
                            if(stationIdMap.isEmpty()){
                                errorAction.invoke()
                                return@runOnUiThread
                            }
                            //spinner要顯示的捷運線名子
                            val mrtlineList = arrayListOf<String>()
                            MrtLine.values().forEach {
                                mrtlineList.add(it.zhName)
                                lineNameArr.add(it.lineName)
                            }
                            //初始化booleanArr
                            for(i in lineNameArr.indices){
                                mrtBooleanMap[lineNameArr[i]] =
                                    BooleanArray(stationIdMap[lineNameArr[i]]!!.size){false}
                            }
                            tvName.text = getInfo()
                            tvCount.text = mrtList.size.toString()

                            //設置spinner
                            spinner.run{
                                adapter = ArrayAdapter(this@FilterMrtActivity,
                                    R.layout.support_simple_spinner_dropdown_item, mrtlineList)
                                tooltipText = "請選擇捷運路線"
                            }
                            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                                override fun onNothingSelected(parent: AdapterView<*>?) {      }
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                                            position: Int, id: Long) {
                                    //取得enum是哪一個～
                                    var lineInfo = MrtLine.AIRPORT_LINE
                                    for(i in MrtLine.values().indices){
                                        if(MrtLine.values()[i].lineName == lineNameArr[position]){
                                            lineInfo = MrtLine.values()[i]
                                        }
                                    }
                                    //取得該線的站名、id清單
                                    val stationNameList = stationNameMap[lineInfo.lineName]!!
                                    val stationIdList = stationIdMap[lineInfo.lineName]!!
                                    //把這些資料都灌進去
                                    items.clear()
                                    for(i in stationNameList.indices){
                                        //如果跟目前所選的有符合，boolean = true
                                        mrtList.forEach {
                                            if(stationIdList[i] == it.id){
                                                mrtBooleanMap[lineInfo.lineName]!![i] = true
                                            }
                                        }
                                        //如果最後一個，因為顯示不同要給boolean
                                        if(i == stationNameList.size - 1){
                                            items.add(MrtItem(lineInfo, stationNameList[i], true, stationIdList[i]))
                                        }else{
                                            items.add(MrtItem(lineInfo, stationNameList[i], false, stationIdList[i]))
                                        }
                                    }
                                    commandAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }else{
                        errorAction.invoke()
                    }
                }catch (e:JSONException){
                    //如果解碼錯誤（不做狀態碼排除，因為不管哪個都要是error畫面)，就出現系統異常，請先用其他的提示
                    Log.d("GetMrtInfo", "JSON FORMAT ERROR! ${e.message}")
                    errorAction.invoke()
                }
            }
            exec()
        }

        //設定返回按鈕
        topAppBar.setNavigationOnClickListener {
            run{
                finish()
            }
        }

        topAppBar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                //設定完成按鈕，帶捷運線名回去
                R.id.done->{
                    run{
                        RadarController.instance.getRadar().regionList = null
                        RadarController.instance.getRadar().landmark = null
                        RadarController.instance.getRadar().mrtList = mrtList
                        RadarController.instance.getRadar().commute = null
                        finish()
                    }
                    true
                }
                else-> false
            }
        }

        rvMrt.layoutManager = LinearLayoutManager(this)
        with(commandAdapter){
            addType(Global.ItemType.FILTER_MRT) { parent ->
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_mrt, parent, false)
                val btnName = itemView.findViewById<MaterialButton>(R.id.btnName)
                val tvLink = itemView.findViewById<TextView>(R.id.tvLink)
                object : BaseViewHolder<MrtItem>(itemView) {
                    override fun bind(item: MrtItem) {
                        //拿到列舉值
                        val lineInfo = item.lineInfo
                        //拿到該列的booleanArray
                        val mrtMap = mrtBooleanMap[lineInfo.lineName]!!
                        //知道是第幾個
                        val num = super.getAdapterPosition()
                        btnName.run {
                            //設定站名、顏色、是否被選取
                            strokeColor = getColorStateList(lineInfo.color)
                            text = item.name
                            backgroundTintList = if (mrtMap[num]) {
                                getColorStateList(R.color.mrt_choosed)
                            } else {
                                getColorStateList(R.color.white)
                            }
                            setOnClickListener {
                                run {
                                    //如果該item ID 相同，代表有選過，就取消刪除，相反增加
                                    var isAction = false
                                    for(i in mrtList.indices){
                                        if(mrtList[i].id == item.stationID){
                                            mrtMap[num] = false
                                            mrtList.removeAt(i)
                                            isAction = true
                                            break
                                        }
                                    }
                                    //如果已經有刪除的動作，就略過，否則就是看有沒有超過上限新增
                                    if(!isAction){
                                        if (mrtList.size < Global.MagicNumber.MAX_MRT_COUNT) {
                                            mrtMap[num] = true
                                            mrtList.add(MRT(item.stationID, item.name))
                                        }
                                    }
                                    this@with.notifyDataSetChanged()
                                    tvName.text = getInfo()
                                    tvCount.text = (mrtList.size).toString()
                                }
                            }
                        }
                        if (item.isLast) {
                            tvLink.backgroundTintList = getColorStateList(R.color.no_color)
                        } else {
                            tvLink.backgroundTintList = getColorStateList(lineInfo.color)
                        }
                    }
                }
            }
        }
        rvMrt.adapter = commandAdapter
    }

    // 拿取所有資料，每次進來時會做一次
    private fun initializeData(jsonArray: JSONArray){
        for(i in 0 until jsonArray.length()){
            try{
                val jsonObject = jsonArray.getJSONObject(i)
                val lineName = jsonObject.getString("mrtLineName")
                val stationName = jsonObject.getString("mrtStationName")
                //如果線名第一次出現，初始化Map、加入捷運清單中
                if(!stationIdMap.containsKey(lineName)){
                    stationIdMap[lineName] = mutableListOf()
                    stationNameMap[lineName] = mutableListOf()
                }
                stationIdMap[lineName]!!.add(jsonObject.getInt("mrtStationId"))
                stationNameMap[lineName]!!.add(stationName)
            }catch (e : JSONException){
                Log.d("JSON FORMAT ERROR", "${e.message}")
            }
        }
    }

    //串接字串，顯示在文字上用
    private fun getInfo():String{
        val tmpInfo = StringBuilder()
        for(i in mrtList.indices){
            if(i == 0){
                tmpInfo.append(mrtList[i].name)
            }else{
                tmpInfo.append(",${mrtList[i].name}")
            }
        }
        return tmpInfo.toString()
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