package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.NumberPicker
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rentradar.models.Region
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.ErrorHintDialog
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import kotlinx.android.synthetic.main.activity_filter_area.*
import kotlinx.android.synthetic.main.activity_filter_area.topAppBar
import kotlinx.android.synthetic.main.item_list_area_region.view.*
import org.json.JSONArray
import org.json.JSONException

class FilterAreaActivity : AppCompatActivity() {

    class RegionItem(val city:String, val name:String, val cityIndex:Int) :IType{
        override val getItemType: Int
            get() = Global.ItemType.FILTER_AREA
    }

    init {
        if(!RadarController.instance.checkRadar()){
            finish()
        }
    }

    //解析區域資料使用，城市名稱表、區域代碼、區域中文表3，不會變動
    private val cityNameArr = arrayListOf<String>()
    private val regionNameMap = hashMapOf<String, MutableList<String>>()
    private val regionIdMap =  hashMapOf<String, MutableList<Int>>()
    //暫存的選擇及計算有無點擊，結束時將這個list替換
    private val regionBooleanMap = hashMapOf<String, BooleanArray>()
    private val regionList: MutableList<Region> =
        RadarController.instance.getRadar().regionList?.toMutableList() ?: mutableListOf()
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)
    //伺服器異常、資料取不到時顯示的畫面
    private lateinit var errorHintDialog: ErrorHintDialog
    private val tag = "FilterAreaActivity"
    //如果有異常，將確認按鈕鎖住，只能返回。
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
        setContentView(R.layout.activity_filter_area)

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        errorHintDialog = ErrorHintDialog(getString(R.string.filter_area_error_info))

        //從FilterAreaPage拿標題資料過來，沒的話重建，以設定選擇numberPicker
        val titleName = intent.extras?.getString(Global.BundleKey.AREA_TITLE_NAME)?.split(",")
        val items = mutableListOf<IType>()
        val commandAdapter = CommonAdapter(items)

        //從後端拿區域清單，並初始化map
        NetworkController.instance.getRegion().run {
            onFailure = {errorCode, msg ->
                errorAction.invoke()
                tvErrorInfo.isVisible = true
                Log.d("OnFailure", "$errorCode:$msg")
            }
            onResponse = {res ->
                try{
                    val regionArray = JSONArray(res)
                    if(regionArray.length() > 0){
                        runOnUiThread {
                            //先將拿到的資料作整理，移到UI是因為資料量大，且畫面需要這些資訊才能顯示，因此跟他搶資源
                            initializeData(regionArray)
                            tvErrorInfo.isVisible = false
                            //初始化booleanArr
                            for(i in cityNameArr.indices){
                                regionBooleanMap[cityNameArr[i]] =
                                    BooleanArray(regionIdMap[cityNameArr[i]]!!.size){false}
                            }
                            //如果之前有選過，則依照之前的標題字串，調整標題顯示畫面
                            if(titleName != null){
                                //先比對城市名，如果有相同，直接給標題（不用再跑邏輯運算）
                                for(i in titleName.indices){
                                    val city = titleName[i].split("(")
                                    for(j in cityNameArr.indices){
                                        if(cityNameArr[j] == city[0]){
                                            cityNameArr[j] = titleName[i]
                                        }
                                    }
                                }
                            }

                            if(cityNameArr.size < 1){
                                errorAction.invoke()
                                tvErrorInfo.isVisible = true
                                return@runOnUiThread
                            }

                            //城市轉盤
                            numberPick.run{
                                displayedValues = cityNameArr.toTypedArray()
                                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                                minValue = 0
                                value = 0
                                maxValue = cityNameArr.size - 1
                                setOnValueChangedListener { _, _, newVal ->
                                    //設定RecycleView、更新其資料
                                    setItems(items, newVal)
                                    rvRegion.layoutManager = getLayout(items.size)
                                    commandAdapter.notifyDataSetChanged()
                                }
                            }
                            //第一次先手動跑一次資料更新
                            setItems(items, 0)
                            rvRegion.layoutManager = getLayout(items.size)
                        }
                    }else{
                        errorAction.invoke()
                        tvErrorInfo.isVisible = true
                    }
                }catch (e : JSONException){
                    //如果解碼錯誤（不做狀態碼404排除，因為不管哪個都要是error畫面)，就出現系統異常，請先用其他的提示
                    Log.d("GetRegionInfo", "JSON FORMAT ERROR! ${e.message}")
                    errorAction.invoke()
                    tvErrorInfo.isVisible = true
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
                //設定完成按鈕，帶地址及範圍回去
                R.id.done->{
                    run{
                        RadarController.instance.getRadar().regionList = regionList
                        RadarController.instance.getRadar().landmark = null
                        RadarController.instance.getRadar().mrtList = null
                        RadarController.instance.getRadar().commute = null
                        finish()
                    }
                   true
                }
                else-> false
            }
        }

        //設定CommandAdapter
        with(commandAdapter){
            addType(Global.ItemType.FILTER_AREA){parent ->
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_area_region, parent, false)
                object : BaseViewHolder<RegionItem>(itemView){
                    override fun bind(item: RegionItem) {
                        itemView.cbName.run{
                            //取得當前item是第幾個，並設定其姓名、是否已被勾選
                            val num = super.getAdapterPosition()
                            text = item.name
                            isChecked = regionBooleanMap[item.city]?.get(num)!!
                            itemView.cbName.setOnClickListener {
                                //當前城市的booleanArr
                                val tmpRegionMap = regionBooleanMap[item.city]!!
                                //對照的ID表格
                                val tmpRegionIdMap =  regionIdMap[item.city]!!
                                //取得當前清單中的所有ID-用來匹配，ID的順序=region的順序
                                val regionIdList = mutableListOf<Int>()
                                for(i in regionList.indices){
                                    regionIdList.add(regionList[i].id)
                                }
                                //如果是第一個全區的話，要把其他的刪除，自己刪或增加
                                if(num == 0){
                                    tmpRegionMap[0] = !tmpRegionMap[0]
                                    if(tmpRegionMap[0]){
                                        //因為第0個是不限，從第一個開始
                                        for(i in 1 until tmpRegionMap.size){
                                            tmpRegionMap[i] = false
                                            //如果區域代號有在清單中，就刪除該區域
                                            if(regionIdList.contains(tmpRegionIdMap[i])){
                                                var count = 1
                                                while(count != regionList.size){
                                                    if(regionList[count].id == tmpRegionIdMap[i] ){
                                                        regionList.removeAt(count)
                                                        continue
                                                    }
                                                    count++
                                                }
                                            }
                                        }
                                        regionList.add(Region(tmpRegionIdMap[0],item.city))
                                    }else{
                                        for(i in regionList.indices){
                                            if(regionList[i].id == tmpRegionIdMap[0]){
                                                regionList.removeAt(i)
                                                break
                                            }
                                        }
                                    }
                                }else{
                                    //如果不是第一位(全區)的話，就判斷全區是否存在，在的話把他從列表刪除，並且增加自己
                                    tmpRegionMap[0] = false
                                    tmpRegionMap[num] = !tmpRegionMap[num]
                                    if(regionIdList.contains(tmpRegionIdMap[0]) ||
                                        regionIdList.contains(tmpRegionIdMap[num])){
                                        //因為for是迭代器（Kotlin)，所以要做刪除的話要刪完馬上中斷，因此用while處理
                                        var count = 0
                                        while(count != regionList.size){
                                            if(regionList[count].id == tmpRegionIdMap[0] ){
                                                regionList.removeAt(count)
                                                continue
                                            }
                                            if(regionList[count].id == tmpRegionIdMap[num]){
                                                regionList.removeAt(count)
                                                continue
                                            }
                                            count++
                                        }
                                    }else{
                                        regionList.add(Region(tmpRegionIdMap[num],item.city))
                                    }
                                }
                                //更新輪盤名稱、縣市資訊
                                setCityName(item.cityIndex, tmpRegionMap)
                                commandAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
        rvRegion.adapter = commandAdapter
    }


    // 拿取所有資料，每次進來時會做一次
    private fun initializeData(jsonArray: JSONArray){
        for(i in 0 until jsonArray.length()){
            try{
                val jsonObject = jsonArray.getJSONObject(i)
                val cityName = jsonObject.getString("cityName")
                val regionName = jsonObject.getString("regionName")
                //如果城市第一次出現，初始化Map、加入城市清單中
                if(!regionIdMap.containsKey(cityName)){
                    cityNameArr.add(cityName)
                    regionIdMap[cityName] = mutableListOf()
                    regionIdMap[cityName]!!.add(0)
                    regionNameMap[cityName] = mutableListOf()
                    regionNameMap[cityName]!!.add("")
                }
                //如果是全區，把他放入第一個（所以上面才會預留0
                if(regionName == "全區"){
                    regionIdMap[cityName]!![0] = jsonObject.getInt("regionId")
                    regionNameMap[cityName]!![0] = regionName
                }else{
                    regionIdMap[cityName]!!.add(jsonObject.getInt("regionId"))
                    regionNameMap[cityName]!!.add(regionName)
                }
            }catch (e : JSONException){
                Log.d("JSON FORMAT ERROR", "${e.message}")
            }
        }
    }

    //設定RecycleView能不能滾動
    private fun getLayout(regionCount: Int): RecyclerView.LayoutManager{
        // 創建recycleView的Manager，如果資料數量少時，讓其無法上下捲動
        return if(regionCount < Global.MagicNumber.MIN_REGION) {
                    object : LinearLayoutManager(this){
                        override fun canScrollVertically(): Boolean {
                            return false
                        }
                    }
                }else{
                    LinearLayoutManager(this)
                }
    }

    //設定物件
    private fun setItems(items: MutableList<IType>, count:Int){
        //現在的城市名稱
        val cityKey = cityNameArr[count].split("(")
        //取得該城市的區域姓名、ID陣列
        val regionNameArr = regionNameMap[cityKey[0]]!!
        val regionIdArr = regionIdMap[cityKey[0]]!!
        //取得當前清單中的所有ID-用來匹配
        val regionIdList = mutableListOf<Int>()
        for(i in regionList.indices){
            regionIdList.add(regionList[i].id)
        }
        //放RecycleView的資料
        items.clear()
        for(i in regionNameArr.indices){
            //將有被選擇到的選項標成true（比對regionId)
            if(regionIdList.contains(regionIdArr[i])){
                regionBooleanMap[cityKey[0]]?.set(i, true)
            }
            items.add(RegionItem(cityKey[0], regionNameArr[i], count))
        }
    }

    //每次點選時，更新現在城市的狀況是幾個區域或全區。
    private fun setCityName(index:Int, tmpRegionMap: BooleanArray){
        var count = 0
        for(i in tmpRegionMap.indices){
            if(tmpRegionMap[i]){
                if(i == 0){
                    count = -1
                    break
                }
                count++
            }
        }
        val cityName = cityNameArr[index].split("(")
        cityNameArr[index] = when(count){
            -1 ->  "${cityName[0]}(全區)"
            0 -> cityName[0]
            else -> "${cityName[0]}($count)"
        }
        //將numberPick重設成新的標題（已顯示全區、已選擇數量）
        numberPick.displayedValues = null
        numberPick.displayedValues = cityNameArr.toTypedArray()
        numberPick.minValue = 0
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