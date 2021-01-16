package com.example.rentradar

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.rentradar.models.House
import com.example.rentradar.utils.*
import com.example.rentradar.utils.Global.BundleKey.RESULT_TO_MAP
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_results.*
import kotlinx.android.synthetic.main.item_house.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ResultsActivity : AppCompatActivity() {

    init {
        if(!RadarController.instance.checkRadar()){
            finish()
        }
    }

    private var isSortedByUpdateTime = true
    private var isSortedByPrice = false
    private var isSortedByPing = false
    private var canLoadNextPage = true
    private var radar = RadarController.instance.getRadar()
    private var pageCount = 0
    private var nowPage = 1
    private var items = mutableListOf<IType>()
    private var averagePrice = 0
    private var isSettingQuickFilterButton = false
    private lateinit var androidId: String
    private val radarId = RadarController.instance.getRadar().id
    private lateinit var commonAdapter: CommonAdapter
    private var isRadarToResult = false
    private var sortBy = 3
    private var sortDirection = 2


    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver = NetworkStateObserver(this, supportFragmentManager)

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)
        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        isRadarToResult = intent.extras?.getBoolean(Global.BundleKey.RADAR_TO_RESULT) ?: false

        androidId =
            Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)


        //細篩跳轉
        btnCondition.setOnClickListener {
            ActivityController.instance.startActivity(this, ConditionActivity::class.java)
        }

        //tab初始化
        tabTextSet(tlSort.getTabAt(0), getString(R.string.result_tab_update_1))
        tabTextSet(tlSort.getTabAt(1), getString(R.string.result_tab_price_1))
        tabTextSet(tlSort.getTabAt(2), getString(R.string.result_tab_ping_1))
        //搜尋空集合圖標預設為false
        tvItemIsZero.isVisible = false


        rvResultList.layoutManager = LinearLayoutManager(this)
        commonAdapter = CommonAdapter(items)

        //初始化commandAdapter
        with(commonAdapter) {
            addType(Global.ItemType.HOUSE) { parent ->
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_house, parent, false)

                object : BaseViewHolder<House>(view) {
                    override fun bind(item: House) {
                        view.tvHouseName.text = item.title
                        view.tvHouseAddress.text = item.address
                        view.tvHouseType.text = HouseInfoTranslate.getTypeOfRoom(item.typeOfRoom)
                        view.tvPrice.text = "\$${item.price}/月"
                        view.tvSource.text = item.source
                        view.cbFavorite.isChecked = item.isFavorite

                        view.cbFavorite.setOnClickListener {
                            if (view.cbFavorite.isChecked) {
                                addToFavorite(androidId, item, commonAdapter)
                            } else {
                                deleteFromFavorite(androidId, item, commonAdapter)
                            }
                        }

                        //點擊跳轉至結果內頁
                        view.card.setOnClickListener {
                            if (item.isOnShelf) {
                                val bundle = Bundle()
                                if (isOnlyOneRoomType(radar.typeOfRoom)) {
                                    bundle.putInt(Global.BundleKey.AVERAGEPRICE, averagePrice)
                                }
                                bundle.putInt(Global.BundleKey.HOUSE_NAME, item.serialNum)
                                bundle.putInt(
                                    Global.BundleKey.RESULT_OBJECT_BACK_INDEX,
                                    Global.BackIndex.RESULTS_LIST.serial
                                )
                                ActivityController.instance.startActivity(
                                    this@ResultsActivity,
                                    ResultsObjectActivity::class.java, bundle
                                )
                            } else {
                                Toast.makeText(
                                    this@ResultsActivity,
                                    getString(R.string.result_no_item),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        //物件圖片設定
                        if (item.isOnShelf) {
                            Glide.with(view).load(item.ImgUrl).override(1800, 600).centerCrop()
                                .into(view.ivHouseImg)
                        } else {
                            Glide.with(view).load(R.drawable.item_default).centerCrop()
                                .into(view.ivHouseImg)
                        }
                    }
                }
            }

        }
        //動態載入設定(當物件位置到達目前底部位置，且沒超過總頁數，載入下一頁)
        commonAdapter.onGetItemViewType = {
            if (it == (rvResultList.layoutManager as LinearLayoutManager).itemCount - 1 && canLoadNextPage && nowPage < pageCount) {
                canLoadNextPage = false
                getRentalList(items, androidId, radarId, ++nowPage, commonAdapter)
            }
        }

        //快篩初始化
        quickFilterInit()

        rvResultList.adapter = commonAdapter


        //標題設置
        resultsTopAppBar.title = radar.name

        //跳轉頁面
        resultsTopAppBar.setNavigationOnClickListener {
            ActivityController.instance.startActivity(this, RadarActivity::class.java)
            finish()
        }
        resultsTopAppBar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                //地圖列表跳轉
                R.id.map -> {
                    if (items.isNotEmpty()) {
                        val bundle = Bundle()
                        val house: ArrayList<House>? = ArrayList()
                        items.forEach {
                            if (it is House) {
                                house?.add(it)
                            }
                        }
                        if (isOnlyOneRoomType(radar.typeOfRoom)) {
                            bundle.putInt(Global.BundleKey.AVERAGEPRICE, averagePrice)
                        }
                        bundle.putParcelableArrayList(RESULT_TO_MAP, house)
                        ActivityController.instance.startActivity(
                            this,
                            ResultsMapsActivity::class.java, bundle
                        )
                    } else {
                        Toast.makeText(this, getString(R.string.result_no_item), Toast.LENGTH_SHORT)
                            .show()
                    }
                    true
                }
                else -> false
            }
        }

        //排序
        tlSort.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        sortByUpdate()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){



                            pageCount=it
                        }
                        if (isSortedByUpdateTime) {
                            tabTextSet(tab, getString(R.string.result_tab_update_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_update_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        sortByPrice()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){
                            pageCount=it
                        }
                        if (isSortedByPrice) {
                            tabTextSet(tab, getString(R.string.result_tab_price_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_price_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_price),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        sortByPing()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){
                            pageCount=it
                        }
                        if (isSortedByPing) {
                            tabTextSet(tab, getString(R.string.result_tab_ping_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_ping_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_ping),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        sortByUpdate()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){
                            pageCount=it
                        }
                        if (isSortedByUpdateTime) {
                            tabTextSet(tab, getString(R.string.result_tab_update_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_update_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        sortByPrice()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){
                            pageCount=it
                        }
                        if (isSortedByPrice) {
                            tabTextSet(tab, getString(R.string.result_tab_price_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_price_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_price),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        sortByPing()
                        sortResetView()
                        getRentalListFirstPageByCondition(items,androidId,radarId,50,sortBy,sortDirection,commonAdapter){
                            pageCount=it
                        }
                        if (isSortedByPing) {
                            tabTextSet(tab, getString(R.string.result_tab_ping_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_ping_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.result_sort_ping),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })


        btnCanKeepPet.setOnClickListener {
            setButton(radar.oneChooseMap["可養寵物"] ?:false, R.id.btnCanKeepPet)
        }
        btnCanCook.setOnClickListener {
            setButton(radar.oneChooseMap["可開伙"] ?:false, R.id.btnCanCook)
        }
        btnHasParkingSpace.setOnClickListener {
            setButton(radar.oneChooseMap["附車位"] ?:false, R.id.btnHasParkingSpace)
        }
        btnCanShortTerm.setOnClickListener {
            setButton(radar.oneChooseMap["可短租"] ?:false, R.id.btnCanShortTerm)
        }
        btnisAgent.setOnClickListener {
            setButton(radar.oneChooseMap["不要房仲"] ?:false, R.id.btnisAgent)
        }
        btnIsRoofTop.setOnClickListener {
            setButton(radar.oneChooseMap["不要頂樓加蓋"] ?:false, R.id.btnIsRoofTop)
        }

    }

    private fun setButton(isChecked:Boolean, checkedId:Int){
        //點選快篩按鈕
        if (isChecked) {
            when (checkedId) {
                R.id.btnCanCook -> radar.oneChooseMap["可開伙"] = false
                R.id.btnisAgent -> radar.oneChooseMap["不要房仲"] = false
                R.id.btnIsRoofTop -> radar.oneChooseMap["不要頂樓加蓋"] = false
                R.id.btnCanKeepPet -> radar.oneChooseMap["可養寵物"] = false
                R.id.btnCanShortTerm -> radar.oneChooseMap["可短租"] = false
                R.id.btnHasParkingSpace -> radar.oneChooseMap["附車位"] = false
            }
        //取消點選快篩按鈕
        } else {
            when (checkedId) {
                R.id.btnCanCook -> radar.oneChooseMap["可開伙"] = true
                R.id.btnisAgent -> radar.oneChooseMap["不要房仲"] = true
                R.id.btnIsRoofTop -> radar.oneChooseMap["不要頂樓加蓋"] = true
                R.id.btnCanKeepPet -> radar.oneChooseMap["可養寵物"] = true
                R.id.btnCanShortTerm -> radar.oneChooseMap["可短租"] = true
                R.id.btnHasParkingSpace -> radar.oneChooseMap["附車位"] = true
            }
        }
        when (checkedId) {
            R.id.btnCanCook -> btnCanCook.isChecked = radar.oneChooseMap["可開伙"]!!
            R.id.btnisAgent -> btnisAgent.isChecked = radar.oneChooseMap["不要房仲"]!!
            R.id.btnIsRoofTop -> btnIsRoofTop.isChecked = radar.oneChooseMap["不要頂樓加蓋"]!!
            R.id.btnCanKeepPet -> btnCanKeepPet.isChecked = radar.oneChooseMap["可養寵物"]!!
            R.id.btnCanShortTerm -> btnCanShortTerm.isChecked = radar.oneChooseMap["可短租"]!!
            R.id.btnHasParkingSpace -> btnHasParkingSpace.isChecked = radar.oneChooseMap["附車位"]!!
        }
        resetView() //重置畫面參數
    }


    override fun onStart() {
        super.onStart()
        if(!RadarController.instance.checkRadar()){
            finish()
        }
        //載入第一頁
        if (isRadarToResult) {
            isRadarToResult = false
        }
            resetView()

        commonAdapter.notifyDataSetChanged()
    }


    private fun sortByPrice() {
        if (isSortedByPrice) {
            sortDirection
            sortBy = 2
            sortDirection=1
        } else {
            sortBy = 2
            sortDirection=2
        }
        isSortedByPrice = !isSortedByPrice
    }

    private fun sortByUpdate() {
        if (isSortedByUpdateTime) {
            sortBy = 3
            sortDirection=1
        } else {
            sortBy = 3
            sortDirection=2
        }
        isSortedByUpdateTime = !isSortedByUpdateTime
    }

    private fun sortByPing() {
        if (isSortedByPing) {
            sortBy = 1
            sortDirection=1
        } else {
            sortBy = 1
            sortDirection=2
        }
        isSortedByPing = !isSortedByPing
    }

    private fun addToFavorite(userId: String, item: House, commonAdapter: CommonAdapter) {
        NetworkController.instance
            .addToFavorite(userId, item.serialNum).run {
                onFailure = { errorCode, msg ->
                    this@ResultsActivity.runOnUiThread {
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.results_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try{
                        val jsonObject = JSONObject(res)
                        runOnUiThread {
                            when (jsonObject.get("status")) {
                                201 -> {
                                    item.isFavorite = true
                                    Toast.makeText(
                                        this@ResultsActivity,
                                        getString(R.string.favorite_add_200),
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    commonAdapter.notifyDataSetChanged()

                                }
                                400 -> Toast.makeText(
                                    this@ResultsActivity,
                                    getString(R.string.favorite_add_400),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                404 -> Toast.makeText(
                                    this@ResultsActivity,
                                    getString(R.string.favorite_add_404),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }catch (e:JSONException){
                        Log.d("addToFavorite",e.message.toString())
                    }

                }
                exec()
            }
    }

    private fun deleteFromFavorite(
        userId: String,
        item: House,
        commonAdapter: CommonAdapter
    ) {
        NetworkController.instance
            .deleteFromFavorite(userId, item.serialNum).run {
                onFailure = { errorCode, msg ->
                    this@ResultsActivity.runOnUiThread {
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.results_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try{
                        val jsonObject = JSONObject(res)
                        runOnUiThread {
                            when (jsonObject.get("status")) {
                                200 -> {
                                    item.isFavorite = false
                                    Toast.makeText(
                                        this@ResultsActivity,
                                        getString(R.string.favorite_delete_200),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    commonAdapter.notifyDataSetChanged()
                                }
                                400 -> Toast.makeText(
                                    this@ResultsActivity,
                                    getString(R.string.favorite_delete_400),
                                    Toast.LENGTH_SHORT
                                ).show()
                                404 -> Toast.makeText(
                                    this@ResultsActivity,
                                    getString(R.string.favorite_delete_404),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }catch (e:JSONException){
                        Log.d("deleteFromFavorite",e.message.toString())
                    }

                }
                exec()
            }
    }

    private fun getRentalListFirstPage(
        items: MutableList<IType>,
        userId: String,
        radarId: Int,
        quantity: Int
        , commonAdapter: CommonAdapter
        , action: ((int: Int) -> Unit)
    ) {

        NetworkController.instance
            .getRentalListFirstPage(userId, radarId, quantity).run {
                onFailure = { errorCode, msg ->
                    this@ResultsActivity.runOnUiThread {
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.results_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try{
                        val jsonObject = if (res.isEmpty()) {
                            JSONObject()
                        } else {
                            JSONObject(res)
                        }
                        averagePrice = if (jsonObject.isNull("averagePrice")) {
                            0
                        } else {
                            jsonObject.getInt("averagePrice")
                        }

                        pageCount = if (jsonObject.isNull("pageCount")) {
                            0
                        } else {
                            jsonObject.getInt("pageCount")
                        }
                        this@ResultsActivity.runOnUiThread {
                            action.invoke(pageCount)
                        }

                        val data = if (jsonObject.isNull("data")) {
                            null
                        } else {
                            jsonObject.getJSONArray("data")
                        }
                        if (data != null && data.length() > 0) {
                            for (i in 0 until data.length()) {
                                val rentalJsonObject = data.getJSONObject(i)
                                val rentalSerialNum = rentalJsonObject.getInt("rentalSerialNum")
                                val source = rentalJsonObject.getString("source")
                                val title = rentalJsonObject.getString("title")
                                val systemUpdateTime = rentalJsonObject.getLong("systemUpdateTime")
                                val city = rentalJsonObject.getString("city")
                                val region = rentalJsonObject.getString("region")
                                val address = rentalJsonObject.getString("address")
                                val addressTotal = "$city$region$address"
                                val longitude = rentalJsonObject.getDouble("longitude")
                                val latitude = rentalJsonObject.getDouble("latitude")
                                val imgUrl = rentalJsonObject.getString("imgUrl")
                                val price = rentalJsonObject.getInt("price")
                                val ping = rentalJsonObject.getInt("ping")
                                val typeOfRoom = rentalJsonObject.getInt("typeOfRoom")
                                val isFavorite = rentalJsonObject.getBoolean("isFavorite")
                                val isOnShelf = rentalJsonObject.getBoolean("isOnShelf")
                                val house = House(
                                    rentalSerialNum,
                                    source,
                                    title,
                                    systemUpdateTime,
                                    addressTotal,
                                    latitude,
                                    longitude,
                                    imgUrl,
                                    price,
                                    typeOfRoom,
                                    ping,
                                    isFavorite,
                                    isOnShelf
                                )
                                items.add(house)
                            }
                            this@ResultsActivity.runOnUiThread {
                                commonAdapter.notifyDataSetChanged()
                            }
                        } else {
                            this@ResultsActivity.runOnUiThread {
                                tvItemIsZero.isVisible = true
                            }
                        }
                    }catch(e:JSONException){
                        Log.d("getRentalListFirstPage",e.message.toString())
                    }

                }
                exec()
            }

    }

    private fun getRentalList(
        items: MutableList<IType>,
        userId: String,
        radarId: Int,
        page: Int,
        commonAdapter: CommonAdapter
    ) {
        NetworkController.instance.getRentalList(userId, radarId, page).run {
            onFailure = { errorCode, msg ->
                this@ResultsActivity.runOnUiThread {
                    Toast.makeText(
                         this@ResultsActivity,
                        getString(R.string.results_error_not_connect),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OnFailure", "$errorCode:$msg")
                }
            }
            onResponse = { res ->
                try{
                    val data = if (res.isEmpty()) {
                        null
                    } else {
                        JSONArray(res)
                    }

                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val jsonObject = data.getJSONObject(i)
                            val rentalSerialNum = jsonObject.getInt("rentalSerialNum")
                            val source = jsonObject.getString("source")
                            val title = jsonObject.getString("title")
                            val systemUpdateTime = jsonObject.getLong("systemUpdateTime")
                            val city = jsonObject.getString("city")
                            val region = jsonObject.getString("region")
                            val address = jsonObject.getString("address")
                            val addressTotal = "$city$region$address"
                            val longitude = jsonObject.getDouble("longitude")
                            val latitude = jsonObject.getDouble("latitude")
                            val imgUrl = jsonObject.getString("imgUrl")
                            val price = jsonObject.getInt("price")
                            val ping = jsonObject.getInt("ping")
                            val typeOfRoom = jsonObject.getInt("typeOfRoom")
                            val isFavorite = jsonObject.getBoolean("isFavorite")
                            val isOnShelf = jsonObject.getBoolean("isOnShelf")
                            val house = House(
                                rentalSerialNum,
                                source,
                                title,
                                systemUpdateTime,
                                addressTotal,
                                latitude,
                                longitude,
                                imgUrl,
                                price,
                                typeOfRoom,
                                ping,
                                isFavorite,
                                isOnShelf
                            )
                            items.add(house)
                        }
                        this@ResultsActivity.runOnUiThread {
                            commonAdapter.notifyDataSetChanged()
                            canLoadNextPage = true
                        }
                    }
                }catch(e:JSONException){
                    Log.d("getRentalList",e.message.toString())
                }

            }
            exec()
        }
    }

    private fun buttonEnable(isEnable:Boolean){
        btnCanCook.isEnabled = isEnable
        btnCanKeepPet.isEnabled = isEnable
        btnCanShortTerm.isEnabled = isEnable
        btnHasParkingSpace.isEnabled = isEnable
        btnIsRoofTop.isEnabled = isEnable
        btnisAgent.isEnabled = isEnable
    }

    private fun getRentalListFirstPageByCondition(
        items: MutableList<IType>,
        userId: String,
        radarId: Int,
        quantity: Int,
        sortBy: Int = 3,
        sortDirection: Int = 2,
        commonAdapter: CommonAdapter,
        action: ((int: Int) -> Unit)
    ) {
        buttonEnable(false)
        val condition = JSONObject()
        condition.put("typeOfRoom", radar.typeOfRoom)
        condition.put("typeOfHousing", radar.conditionMap["建築物型態"])
        condition.put("spatialLayout", radar.conditionMap["房屋格局"])
        condition.put("floor", radar.conditionMap["樓層"])
        condition.put("identity", radar.conditionMap["房客身分"])
        condition.put("sex", radar.conditionMap["房客性別"])
        condition.put("furniture", radar.conditionMap["附傢俱"])
        condition.put("appliances", radar.conditionMap["附設備"])
        condition.put("lifeFunction", radar.conditionMap["周邊機能"])
        condition.put("traffic", radar.conditionMap["交通站點"])
        condition.put("hasParkingSpace", radar.oneChooseMap["附車位"])
        condition.put("isAgent", !radar.oneChooseMap["不要房仲"]!!)
        condition.put("isRoofTop", !radar.oneChooseMap["不要頂樓加蓋"]!!)
        condition.put("canShortTerm", radar.oneChooseMap["可短租"])
        condition.put("canCook", radar.oneChooseMap["可開伙"])
        condition.put("canKeepPet", radar.oneChooseMap["可養寵物"])


        NetworkController.instance
            .getRentalListFirstPage(userId, radarId, quantity, condition, sortBy, sortDirection)
            .run {
                onFailure = { errorCode, msg ->
                    this@ResultsActivity.runOnUiThread {
                        Toast.makeText(
                            this@ResultsActivity,
                            getString(R.string.results_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try{
                        val jsonObject = if (res.isEmpty()) {
                            JSONObject()
                        } else {
                            JSONObject(res)
                        }
                        averagePrice = if (jsonObject.isNull("averagePrice")) {
                            0
                        } else {
                            jsonObject.getInt("averagePrice")
                        }
                        pageCount = if (jsonObject.isNull("pageCount")) {
                            0
                        } else {
                            jsonObject.getInt("pageCount")
                        }
                        this@ResultsActivity.runOnUiThread {
                            action.invoke(pageCount)
                        }

                        val data = if (jsonObject.isNull("data")) {
                            null
                        } else {
                            jsonObject.getJSONArray("data")
                        }
                        if (data != null && data.length() > 0) {
                            for (i in 0 until data.length()) {
                                val rentalJsonObject = data.getJSONObject(i)
                                val rentalSerialNum = rentalJsonObject.getInt("rentalSerialNum")
                                val source = rentalJsonObject.getString("source")
                                val title = rentalJsonObject.getString("title")
                                val systemUpdateTime = rentalJsonObject.getLong("systemUpdateTime")
                                val city = rentalJsonObject.getString("city")
                                val region = rentalJsonObject.getString("region")
                                val address = rentalJsonObject.getString("address")
                                val addressTotal = "$city$region$address"
                                val longitude = rentalJsonObject.getDouble("longitude")
                                val latitude = rentalJsonObject.getDouble("latitude")
                                val imgUrl = rentalJsonObject.getString("imgUrl")
                                val price = rentalJsonObject.getInt("price")
                                val ping = rentalJsonObject.getInt("ping")
                                val typeOfRoom = rentalJsonObject.getInt("typeOfRoom")
                                val isFavorite = rentalJsonObject.getBoolean("isFavorite")
                                val isOnShelf = rentalJsonObject.getBoolean("isOnShelf")
                                val house = House(
                                    rentalSerialNum,
                                    source,
                                    title,
                                    systemUpdateTime,
                                    addressTotal,
                                    latitude,
                                    longitude,
                                    imgUrl,
                                    price,
                                    typeOfRoom,
                                    ping,
                                    isFavorite,
                                    isOnShelf
                                )
                                items.add(house)
                            }
                            this@ResultsActivity.runOnUiThread {
                                commonAdapter.notifyDataSetChanged()
                            }
                        } else {
                            this@ResultsActivity.runOnUiThread {
                                tvItemIsZero.isVisible = true
                            }
                        }
                    }catch(e:JSONException){
                        Log.d("getRentalListFirstPageByCondition",e.message.toString())
                    }

                }
                onComplete = {
                    runOnUiThread {
                        buttonEnable(true)
                    }
                }
                exec()
            }

    }

    private fun quickFilterInit() {
        isSettingQuickFilterButton = true
        val radar = RadarController.instance.getRadar()

        if (radar.oneChooseMap["可開伙"]!!) {
            btnFilterA.check(R.id.btnCanCook)
        } else {
            btnFilterA.uncheck(R.id.btnCanCook)
        }
        if (radar.oneChooseMap["可養寵物"]!!) {
            btnFilterA.check(R.id.btnCanKeepPet)
        } else {
            btnFilterA.uncheck(R.id.btnCanKeepPet)
        }
        if (radar.oneChooseMap["附車位"]!!) {
            btnFilterA.check(R.id.btnHasParkingSpace)
        } else {
            btnFilterA.uncheck(R.id.btnHasParkingSpace)
        }
        if (radar.oneChooseMap["不要頂樓加蓋"]!!) {
            btnFilterB.check(R.id.btnIsRoofTop)
        } else {
            btnFilterB.uncheck(R.id.btnIsRoofTop)
        }
        if (radar.oneChooseMap["不要房仲"]!!) {
            btnFilterB.check(R.id.btnisAgent)
        } else {
            btnFilterB.uncheck(R.id.btnisAgent)
        }
        if (radar.oneChooseMap["可短租"]!!) {
            btnFilterB.check(R.id.btnCanShortTerm)
        } else {
            btnFilterB.uncheck(R.id.btnCanShortTerm)
        }
        isSettingQuickFilterButton = false
    }

    //複合列舉 判斷是否為同一種房型
    private fun isOnlyOneRoomType(typeOfRoom: Int): Boolean {
        when (typeOfRoom) {
            1 -> return true
            2 -> return true
            4 -> return true
            8 -> return true
            16 -> return true
        }
        return false
    }

    private fun resetView() {
        tvItemIsZero.isVisible = false
        isSortedByUpdateTime = false
        tlSort.selectTab(tlSort.getTabAt(0))
        isSortedByPrice = false
        isSortedByPing = false
        canLoadNextPage = true
        pageCount = 0
        nowPage = 1
        items.clear()
    }
    private  fun sortResetView(){
        tvItemIsZero.isVisible = false
        canLoadNextPage = true
        pageCount = 0
        nowPage = 1
        items.clear()
    }

    private fun tabTextSet(tab: TabLayout.Tab?, str: String) {
        val absoluteSizeSpan = AbsoluteSizeSpan(60)
        val spannableStringBuilder = buildSpannedString {
            append(str)
            setSpan(absoluteSizeSpan, 4, 6, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        tab?.text = spannableStringBuilder
    }

    override fun onResume() {
        if (!networkStateObserver.isRegister) {
            networkStateObserver.register()
        }
        quickFilterInit()
        super.onResume()
    }

    override fun onStop() {
        if (networkStateObserver.isRegister) {
            networkStateObserver.unregister()
        }
        super.onStop()
    }

}
