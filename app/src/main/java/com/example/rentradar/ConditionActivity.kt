package com.example.rentradar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentradar.models.ConditionItems
import com.example.rentradar.utils.*
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import kotlinx.android.synthetic.main.activity_condition.*
import org.json.JSONObject
import java.lang.StringBuilder

class ConditionActivity : AppCompatActivity() {

    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)
    private lateinit var commonAdapter:CommonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_condition)

        if(!RadarController.instance.checkRadar()){
            finish()
        }

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        //讀取在raw的json檔，會得到一個String檔，並轉換成JsonObject
        val jsonDataString = Global.readJSONDataFromFile(this, R.raw.condition)
        val jsonObject = JSONObject(jsonDataString)

        //用一個map來暫存現在的操作，確認後才把這個tmpMap跟雷達的換
        val tmpOneChooseMap = hashMapOf<String, Boolean>()
        //防呆如果map是沒資料的話，要初始化MAP，但還是要感測Map是否為空，避免資料被清掉
        if(RadarController.instance.getRadar().conditionMap.isEmpty()){
            val conditionMap = RadarController.instance.getRadar().conditionMap
            val conditionKeyArr = getConditionJSONArray(jsonObject, "allKeys")
            //為了增加初始化快速，直接都先定義為0，所以0、最大數，都會=全選
            conditionKeyArr.forEach { conditionMap[it] = 0 }
            if(RadarController.instance.getRadar().oneChooseMap.isEmpty()){
                val oneChooseMap = RadarController.instance.getRadar().oneChooseMap
                val oneChooseKeyArr = getConditionJSONArray(jsonObject, "oneChooseKeys")
                oneChooseKeyArr.forEach {
                    //不要房仲、頂樓加蓋， ture 表示不要，因此後端會把它相反，false才等於要篩選，因此預設要是true排除掉
                    oneChooseMap[it] = (it == "不要房仲" || it == "不要頂樓加蓋")
                    tmpOneChooseMap[it] = (it == "不要房仲" || it == "不要頂樓加蓋")
                }
            }
        }else{
            RadarController.instance.getRadar().oneChooseMap.forEach { (title, boolean) ->
                tmpOneChooseMap[title] = boolean
            }
        }

        //設定確認按紐
        topAppBar.setNavigationOnClickListener {
            run {
                if(!DoubleClickGuard.isFastDoubleClick()){
                    RadarController.instance.getRadar().oneChooseMap = tmpOneChooseMap
                    Toast.makeText(this, "已儲存篩選條件", Toast.LENGTH_SHORT).show()
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }

        //RecycleView
        val items = mutableListOf<IType>()
        val layoutManager = LinearLayoutManager(this)
        rvCondition.layoutManager = layoutManager

        commonAdapter = with(CommonAdapter(items)){
            apply {
                //header重製
                addHeader { parent ->
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_header, parent, false)
                    val tvReset = itemView.findViewById<TextView>(R.id.tvReset)
                    //設定重置按鈕
                    tvReset.setOnClickListener {
                        run{
                            val conditionMap = RadarController.instance.getRadar().conditionMap
                            val conditionKeyArr = getConditionJSONArray(jsonObject, "allKeys")
                            val oneChooseKeyArr = getConditionJSONArray(jsonObject, "oneChooseKeys")
                            //為了增加初始化快速，直接都先定義為0，所以0、最大數，都會=全選
                            conditionKeyArr.forEach { conditionMap[it] = 0 }
                            oneChooseKeyArr.forEach {
                                //不要房仲、頂樓加蓋， ture 表示不要，因此後端會把它相反，false才等於要篩選，因此預設要是true排除掉
                                tmpOneChooseMap[it] = (it == "不要房仲" || it == "不要頂樓加蓋")
                            }
                            this@with.notifyDataSetChanged()
                        }
                    }
                    object : BaseViewHolder<IType>(itemView){
                        override fun bind(item: IType) {
                        }
                    }
                }
                //抬頭，區隔用，只需設置文字
                addType(Global.ItemType.CONDITION_TITLE){parent ->
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_title, parent, false)
                    val tvName = itemView.findViewById<TextView>(R.id.tvAddress)
                    object : BaseViewHolder<ConditionItems.TitleItem>(itemView){
                        override fun bind(item: ConditionItems.TitleItem) {
                            tvName.text = item.name
                        }
                    }
                }
                //需跳轉到detail的內容
                addType(Global.ItemType.CONDITION_TURN){parent ->
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_turn, parent, false)
                    val lyName = itemView.findViewById<LinearLayout>(R.id.lyName)
                    val tvName = itemView.findViewById<TextView>(R.id.tvAddress)
                    val tvResult = itemView.findViewById<TextView>(R.id.tvResult)
                    object : BaseViewHolder<ConditionItems.TurnItem>(itemView){
                        override fun bind(item: ConditionItems.TurnItem) {
                            val titleName = item.name
                            lyName.setOnClickListener {
                                if(!DoubleClickGuard.isFastDoubleClick()){
                                    //設定跳轉資訊後跳到Detail頁面
                                    turnNextActivity(jsonObject, titleName)
                                }
                            }
                            tvName.text = titleName
                            tvResult.text = getResult(jsonObject, titleName)
                        }
                    }
                }
                //直接單選的內容
                addType(Global.ItemType.CONDITION_CHOOSE){parent ->
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_condition_detail, parent, false)
                    val cbName = itemView.findViewById<CheckBox>(R.id.cbName)
                    object : BaseViewHolder<ConditionItems.ChooseItem>(itemView){
                        override fun bind(item: ConditionItems.ChooseItem) {
                            cbName.run{
                                setOnClickListener {
                                    run{
                                        tmpOneChooseMap[item.name] = !(tmpOneChooseMap[item.name]!!)
                                    }
                                }
                                text = item.name
                                isChecked = tmpOneChooseMap[item.name]!!
                            }
                        }
                    }
                }
            }
        }
        rvCondition.adapter = commonAdapter

        //手動新增列表->依照順序插入
        items.run {
            // 房屋條件：建築物型態、房屋格局、樓層、附車位、不要房仲、不要頂樓加蓋
            add(ConditionItems.TitleItem(getString(R.string.condition_main_small_title_house)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_build_type)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_configuration)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_floor)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_car)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_seller)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_notopfloor)))
            // 承租條件：房客身分、房客性別、可短租、可開伙、可養寵物
            add(ConditionItems.TitleItem(getString(R.string.condition_main_small_title_tenant)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_identity)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_gender)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_rent_time)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_cook)))
            add(ConditionItems.ChooseItem(getString(R.string.condition_main_pet)))
            // 附屬設備：附傢俱、附設備
            add(ConditionItems.TitleItem(getString(R.string.condition_main_small_title_equipment)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_furniture)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_equipment)))
            // 環境交通：周邊機能、交通站點
            add(ConditionItems.TitleItem(getString(R.string.condition_main_small_title_environmental)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_convenience)))
            add(ConditionItems.TurnItem(getString(R.string.condition_main_traffic)))
        }
        commonAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        commonAdapter.notifyDataSetChanged()
    }

    // 從Json檔中抓取對應的名稱的條件細項名稱
    private fun getConditionJSONArray(jsonObject: JSONObject, titleName: String): ArrayList<String>{
        val jsonArray =  jsonObject.getJSONArray(titleName)
        val arrayList = arrayListOf<String>()
        for(i in 0 until jsonArray.length()){
            arrayList.add(jsonArray[i].toString())
        }
        return arrayList
    }

    //設定每個按鈕的跳轉資訊，給予該選項的數字、標題名稱、各選項名稱
    private fun turnNextActivity(jsonObject: JSONObject, titleName: String){
        val bundle = Bundle()
        val arrayList = getConditionJSONArray(jsonObject, titleName)
        //傳送目前的選項名稱及選項名稱列表給下個場景
        bundle.putString(Global.BundleKey.TITLENAME, titleName)
        bundle.putStringArrayList(Global.BundleKey.CONDITION_NAME, arrayList)
        ActivityController.instance.startActivityCustomAnimation(this,
            ConditionDetailActivity::class.java, bundle)
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
    }

    //每次進入畫面時，更新選擇的文字
    private fun getResult(jsonObject: JSONObject, titleName: String) : String{
        //拿到細項，以及複合列舉值
        val num = RadarController.instance.getRadar().conditionMap[titleName]!!
        if(num == 0) {
            return "不限"
        }else{
            val result = StringBuilder()
            //拿到該選項的資料，在依照二進位轉換，有的話加到字串中。
            val conditionDetailArr = getConditionJSONArray(jsonObject, titleName)
            for(i in 1 until conditionDetailArr.size){
                //裡面位數要-1是因為第一個是不限，要去除
                if(num.shr(i - 1) % 2 == 1){
                    if(result.isEmpty()){
                        result.append(conditionDetailArr[i])
                    }else{
                        result.append(",${conditionDetailArr[i]}")
                    }
                }
            }
            return String(result)
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