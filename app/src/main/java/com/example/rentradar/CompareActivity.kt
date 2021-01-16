package com.example.rentradar

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import kotlinx.android.synthetic.main.activity_compare.*
import kotlinx.android.synthetic.main.activity_compare.errorCardview
import kotlinx.android.synthetic.main.activity_compare.topAppBar
import kotlinx.android.synthetic.main.activity_compare.tvHouse1
import kotlinx.android.synthetic.main.activity_compare.tvHouse2
import org.json.JSONException
import org.json.JSONObject

class CompareActivity : AppCompatActivity() {

    //當物件資訊多個或可文字顯示時
    class CompareStringItem(val conditionName:String,
                            val info1:String,
                            val info2:String) : IType{
        override val getItemType: Int
            get() = Global.ItemType.COMPARE_STRING
    }

    //RecycleView 的條件清單，照選擇時的順序
    private lateinit var conditionNameList : MutableList<String>
    private val resultInfoMap = hashMapOf<String, MutableList<String>>()
    private lateinit var resultInfoList1 : MutableList<String>
    private lateinit var resultInfoList2 : MutableList<String>
    private lateinit var conditionNameJSON: JSONObject
    private val items = mutableListOf<IType>()
    private val commonAdapter = CommonAdapter(items)
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //設定動畫
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        setContentView(R.layout.activity_compare)
        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        //讀取在raw的json檔，會得到一個String檔，並轉換成JsonObject
        conditionNameJSON = JSONObject(Global.readJSONDataFromFile(this, R.raw.condition))

        conditionNameList = mutableListOf(
            getString(R.string.compare_main_address),
            getString(R.string.compare_main_roomtype),
            getString(R.string.compare_main_price),
            getString(R.string.compare_main_ping),
            getString(R.string.condition_main_build_type),
            getString(R.string.condition_main_configuration),
            getString(R.string.condition_main_floor),
            getString(R.string.condition_main_car),
            getString(R.string.compare_main_agent),
            getString(R.string.compare_main_rooftop),
            getString(R.string.condition_main_identity),
            getString(R.string.condition_main_gender),
            getString(R.string.condition_main_rent_time),
            getString(R.string.condition_main_cook),
            getString(R.string.condition_main_pet),
            getString(R.string.condition_main_furniture),
            getString(R.string.condition_main_equipment),
            getString(R.string.condition_main_convenience),
            getString(R.string.condition_main_traffic),
            getString(R.string.compare_main_update_date)
        )

        //返回喜愛列表
        topAppBar.setNavigationOnClickListener {
            run{
                val bundle = Bundle()
                bundle.putBoolean(Global.BundleKey.ISFAVORITE, true)
                ActivityController.instance.startActivity(this, RadarActivity::class.java, bundle)
                finish()
            }
        }

        //從喜愛列表拿到全部的房屋id(call API使用 - default 使用第0.1個）、名字(顯示用)
        val responseBundle = intent.extras
        val houseId = responseBundle?.getIntegerArrayList(Global.BundleKey.COMPARE_ID_ARR)
        val houseList = responseBundle?.getStringArrayList(Global.BundleKey.COMPARE_NAME_ARR)

        //先做判斷，如果id < 1 、 房屋清單小於1 顯示default畫面
        if(houseId.isNullOrEmpty() || houseList.isNullOrEmpty()){
            errorCardview.isVisible = true
        }else{
            errorCardview.isVisible = false
            //初始化Map資料，避免資料為空，數字跟條件數量一樣多即可，
            val tmpInitArr = mutableListOf<String>()
            for(i in 0 until Global.MagicNumber.CONDITION_COUNT){
                tmpInitArr.add("")
            }
            houseList.forEach {
                resultInfoMap[it] = tmpInitArr
            }
            //製作下拉選單設定，兩個都同樣
            val spinnerAdapter = ArrayAdapter(this,
                R.layout.support_simple_spinner_dropdown_item, houseList)
            //一開始初始先錯開兩個選項
            spinner1.adapter = spinnerAdapter
            spinner1.setSelection(0)
            resultInfoList1 = resultInfoMap[houseList[0]] ?: mutableListOf()
            //如果只有一間房子，第二欄不選資料
            if(houseList.size > 1){
                spinner2.adapter = spinnerAdapter
                spinner2.setSelection(1)
                resultInfoList2 = resultInfoMap[houseList[1]] ?:mutableListOf()
            }else{
                spinner2.isVisible = false
                Glide.with(this).load(R.drawable.icon_not_search).into(ivHouse2)
                resultInfoList2 = mutableListOf()
            }

            //RecycleView設定
            rvCompare.layoutManager = LinearLayoutManager(this)
            with(commonAdapter){
                apply {
                    //如果收到的資料是字串的話。
                    addType(Global.ItemType.COMPARE_STRING){parent ->
                        val itemView = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_list_compare_string_type, parent, false)
                        val lyCompare = itemView.findViewById<LinearLayout>(R.id.lyCompare)
                        val tvName = itemView.findViewById<TextView>(R.id.tvAddress)
                        val tvHouse1 = itemView.findViewById<TextView>(R.id.tvHouse1)
                        val tvHouse2 = itemView.findViewById<TextView>(R.id.tvHouse2)

                        object :BaseViewHolder<CompareStringItem>(itemView){
                            @SuppressLint("UseCompatLoadingForDrawables")
                            override fun bind(item: CompareStringItem) {
                                //依照其位置，判斷應該是什麼顏色，已達成表格效果。
                                val position = super.getAdapterPosition()
                                if(position % 2 == 0){
                                    val white = getColor(R.color.white)
                                    tvName.setTextColor(white)
                                    tvHouse1.setTextColor(white)
                                    tvHouse2.setTextColor(white)
                                    lyCompare.background = getDrawable(R.color.list_one)
                                }else{
                                    val blue = getColor(R.color.blue_800)
                                    tvName.setTextColor(blue)
                                    tvHouse1.setTextColor(blue)
                                    tvHouse2.setTextColor(blue)
                                    lyCompare.background = getDrawable(R.color.list_two)
                                }

                                //如果是家具、設備、周邊機能、交通站點，因為東西可能會比較多，因此行距增加
                                lyCompare.layoutParams.height = when(item.conditionName){
                                    "地址", "附傢俱", "交通站點" -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,70f, resources.displayMetrics).toInt()
                                    "附設備" -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,100f, resources.displayMetrics).toInt()
                                    "周邊機能" -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,125f, resources.displayMetrics).toInt()
                                    else ->  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,40f, resources.displayMetrics).toInt()
                                }
                                tvName.text = item.conditionName
                                setHouseOption(tvHouse1, item.info1, item)
                                setHouseOption(tvHouse2, item.info2, item)
                            }
                        }
                    }
                }
            }
            rvCompare.adapter = commonAdapter

            //暫存列表資訊，使資料可以互通
            var listIndex = -1
            var tmpIdName = ""
            val houseList2: ArrayList<String> = ArrayList(houseList)
            val houseIdList2: MutableList<Int> = houseId.toMutableList()

            //感測放在第一個spinner，每次被點擊時，對應將第二個的spinner列表調整成看不到第一個的資料
            spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {            }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?,position: Int, id: Long) {
                    //將兩個資料的表格先指到地圖的資料
                    resultInfoList1 = resultInfoMap[houseList[position]] ?: mutableListOf()
                    setResultInfoList(1, houseId[position], houseList[position], ivHouse1, tvHouse1)
                    //如果是第一次，不插入資料，否則把之前刪掉的資料補回來
                    if(listIndex != -1){
                        houseList2.add(listIndex, houseList[listIndex])
                        houseIdList2.add(listIndex, houseId[listIndex])
                    }
                    //紀錄選擇的index
                    listIndex = position
                    //將spinner2的資料調整，並重設spinner2的Adapter，並指向原本的位置
                    houseList2.remove(houseList[position])
                    houseIdList2.removeAt(position)
                    val tmpSpinnerAdapter = ArrayAdapter(this@CompareActivity,
                        R.layout.support_simple_spinner_dropdown_item, houseList2)
                    spinner2.adapter = tmpSpinnerAdapter
                    for(i in houseList2.indices){
                        if(houseList2[i] == tmpIdName){
                            spinner2.setSelection(i, true)
                        }
                    }
                }
            }

            spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) { }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?,position: Int, id: Long) {
                    resultInfoList2 = resultInfoMap[houseList2[position]] ?: mutableListOf()
                    //紀錄目前位置的名稱，以利spinner1知道是第幾個，如果暫存的名稱跟自己相通，就不做事，避免一直重摳API
                    if(tmpIdName != houseList2[position]){
                        tmpIdName = houseList2[position]
                        setResultInfoList(2, houseIdList2[position], houseList2[position], ivHouse2, tvHouse2)
                    }
                }
            }
        }
    }

    //設定房屋相片、表格內容、跳轉至網頁的按鈕
    private fun setHouseInfo(listNum:Int, imageView: ImageView, textView: TextView){
       //儲存從網路拿到的連結資訊
        items.clear()
        //根據是哪一張表被按下去，而對應更新相對資訊，非被點選的直接拿取暫存的資料顯示
        //因為條件清單是照順序下來從0開始，而儲存的答案清單則是從2開始（0.1是圖片及來源網址）
        for(i in 2 until conditionNameList.size + 2) {
            //如果有任何一個表超過的話，例外處理的狀況
            if (resultInfoList1.size - 1 < i || resultInfoList2.size - 1 < i) {
                if (resultInfoList1.size - 1 < i && resultInfoList2.size - 1 >= i) {
                    items.add(CompareStringItem(conditionNameList[i - 2],"N/A", resultInfoList2[i]))
                }else if (resultInfoList1.size - 1 >= i && resultInfoList2.size - 1 < i) {
                    items.add(CompareStringItem(conditionNameList[i - 2], resultInfoList1[i],"N/A"))
                }else{
                    items.add(CompareStringItem(conditionNameList[i - 2],"N/A","N/A"))
                }
            } else {
                items.add(CompareStringItem(
                    conditionNameList[i - 2],
                    resultInfoList1[i],
                    resultInfoList2[i]
                ))
            }
        }
        //設定物件圖片
        if(listNum == 1 && resultInfoList1.size > 1){
            Glide.with(this@CompareActivity)
                .load(resultInfoList1[0])
                .error(R.drawable.icon_not_search)
                .into(imageView)
            //設定下方跳轉資訊
            textView.setOnClickListener {
                run{
                    if(resultInfoList1.size < 1 || resultInfoList1[1] == "N/A"){
                        Toast.makeText(this, getString(R.string.compare_from_error), Toast.LENGTH_LONG).show()
                    }else{
                        val uri = Uri.parse(resultInfoList1[1])
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }
                }
            }
        }else if(listNum == 2 && resultInfoList2.size > 1){
            Glide.with(this@CompareActivity)
                .load(resultInfoList2[0])
                .error(R.drawable.icon_not_search)
                .into(imageView)
            //設定下方跳轉資訊
            textView.setOnClickListener {
                run{
                    if(resultInfoList2.size < 1 || resultInfoList2[1] == "N/A"){
                        Toast.makeText(this, getString(R.string.compare_from_error), Toast.LENGTH_LONG).show()
                    }else{
                        val uri = Uri.parse(resultInfoList2[1])
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }
                }
            }
        }
        commonAdapter.notifyDataSetChanged()
    }

    //之後連線到伺服器拿資料，一個一個取出，照資料格式存成arrayList
    private fun setResultInfoList(listNum:Int, houseId:Int, houseName:String,
                                  imageView: ImageView, textView: TextView){
        //如果map裡面有資料都沒有空值，或N/A超過一半(可能資料不齊全)，就不摳資料惹
        var hasInfo = true
        var errorCount = 0
        for(i in resultInfoMap[houseName]!!.indices){
            if(resultInfoMap[houseName]!![i].isEmpty()){
                hasInfo = false
                break
            }
            if(resultInfoMap[houseName]!![i] == "N/A"){
                errorCount++
            }
            if(errorCount > (Global.MagicNumber.CONDITION_COUNT / 2 )){
                hasInfo = false
                break
            }
        }
        if(hasInfo){
            if(listNum == 1){
                resultInfoList1 = resultInfoMap[houseName]!!
            }else{
                resultInfoList2 = resultInfoMap[houseName]!!
            }
            setHouseInfo(listNum, imageView, textView)
            return
        }
        val resultList = mutableListOf<String>()
        if(lifecycle.currentState >= Lifecycle.State.CREATED ){
            NetworkController.instance.getCompareInfo(houseId).run {
                onFailure = {errorCode, msg ->
                    Log.d("OnFailure", "$errorCode:$msg")
                    for(i in resultList.indices){
                        resultList[i] = "N/A"
                    }
                }
                onResponse = {res ->
                    try {
                        val jsonObject = JSONObject(res)
                        if(!jsonObject.isNull("status")){
                            for(i in resultList.indices){
                                resultList[i] = "N/A"
                            }
                            Log.d("OnGetRentalCompareInfoError!", "status:${jsonObject.getInt("status")}")
                        }else{
                            //第0.1個為圖片地址、
                            resultList.add(jsonObject.getString("imgUrl"))
                            resultList.add(jsonObject.getString("url"))
                            val address = StringBuilder()
                            address.append(jsonObject.getString("city"))
                            address.append(jsonObject.getString("region"))
                            address.append(jsonObject.getString("address"))
                            resultList.add(String(address))
                            resultList.add(HouseInfoTranslate.getTypeOfRoom(jsonObject.getInt("typeOfRoom")))
                            val price = jsonObject.getInt("price")
                            resultList.add(if(price == 0){"N/A"} else{price.toString()})
                            val ping = jsonObject.getInt("ping")
                            resultList.add(if(ping == 0 ){"N/A"} else{ping.toString()})
                            resultList.add(HouseInfoTranslate.getTypeOfHouse(jsonObject.getInt("typeOfHousing")))
                            resultList.add(if(jsonObject.isNull("spatialLayout") ||
                                jsonObject.getString("spatialLayout") == "") "N/A" else{
                                jsonObject.getString("spatialLayout")
                            })
                            val currentFloor = if(jsonObject.isNull("currentFloor")){0}else{
                                jsonObject.getInt("currentFloor")
                            }
                            val totalFloor = if(jsonObject.isNull("totalFloor")){0}else{
                                jsonObject.getInt("totalFloor")
                            }
                            resultList.add(if(currentFloor == 0 || totalFloor == 0) {"N/A" }else{
                                "${currentFloor}F / ${totalFloor}F"
                            })
                            resultList.add(jsonObject.getBoolean("hasParkingSpace").toString())
                            resultList.add(jsonObject.getBoolean("isAgent").toString())
                            resultList.add(jsonObject.getBoolean("isRoofTop").toString())
                            resultList.add(HouseInfoTranslate.getTypeName(conditionNameJSON,
                                jsonObject.getInt("identity"),
                                getString(R.string.condition_main_identity)))
                            resultList.add(HouseInfoTranslate.getSex(jsonObject.getInt("sex")))
                            resultList.add(jsonObject.getBoolean("canShortTerm").toString())
                            resultList.add(jsonObject.getBoolean("canCook").toString())
                            resultList.add(jsonObject.getBoolean("canKeepPet").toString())
                            resultList.add(HouseInfoTranslate.getTypeName(conditionNameJSON,
                                jsonObject.getInt("furniture"),
                                getString(R.string.condition_main_furniture)))
                            resultList.add(HouseInfoTranslate.getTypeName(conditionNameJSON,
                                jsonObject.getInt("appliances"),
                                getString(R.string.condition_main_equipment)))
                            resultList.add(HouseInfoTranslate.getTypeName(conditionNameJSON,
                                jsonObject.getInt("lifeFunction"),
                                getString(R.string.condition_main_convenience)))
                            resultList.add(HouseInfoTranslate.getTypeName(conditionNameJSON,
                                jsonObject.getInt("traffic"),
                                getString(R.string.condition_main_traffic)))
                            val dateLong = jsonObject.getLong("systemUpdateTime")
                            var date = (dateLong / 1000000).toInt()
                            val year = date / 10000
                            date %= 10000
                            val month = if(date /100 < 10) "0${date / 100}" else "${date / 100}"
                            val day = if(date % 100 < 10) "0${date % 100}" else "${date % 100}"
                            resultList.add("$year-$month-$day")
                        }
                    }catch (e: JSONException){
                        Log.d("JSON FORMAT ERROR!", "${e.message}")
                    }
                }
                onComplete = {
                    resultInfoMap[houseName] = resultList
                    if(listNum == 1){
                        resultInfoList1 = resultList
                    }else{
                        resultInfoList2 = resultList
                    }
                    runOnUiThread {
                        setHouseInfo(listNum, imageView, textView)
                    }
                }
                exec()
            }
        }
    }

    private fun setHouseOption(tvHouse: TextView, info:String, item: CompareStringItem){
        tvHouse.run{
            text = when(info){
                "true" -> getString(R.string.compare_have)
                "false" ->getString(R.string.compare_no_have)
                else -> info
            }
            //如果是家具、設備、周邊機能、交通站點，因為東西可能會比較多，因此置左
            gravity = when(item.conditionName){
                "附傢俱", "附設備", "周邊機能", "交通站點" -> { Gravity.CENTER_VERTICAL }
                else -> Gravity.CENTER
            }
            //如果是"N/A"，就置中
            if(text == "N/A"){
                gravity = Gravity.CENTER
            }
        }
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


}