package com.example.rentradar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.rentradar.utils.*
import com.example.rentradar.utils.Global.BackIndex.*
import com.example.rentradar.utils.Global.MagicNumber.COMMENT_CONTENT_INPUT_LIMIT
import com.example.rentradar.utils.Global.MagicNumber.COMMENT_NAME_INPUT_LIMIT
import com.example.rentradar.view.dialogs.InternetErrorHintDialog
import com.example.rentradar.view.fragments.ConvenienceNearestPage
import com.example.rentradar.view.fragments.ConvenienceRangePage
import com.example.rentradar.view.fragments.ConvenienceStatisticsPage
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_results_object.*
import kotlinx.android.synthetic.main.activity_results_object.mapView
import kotlinx.android.synthetic.main.activity_results_object.topAppBar
import kotlinx.android.synthetic.main.activity_results_object.tvAddress
import kotlinx.android.synthetic.main.item_list_comment_add.*
import kotlinx.android.synthetic.main.item_list_comment_add.view.*
import kotlinx.android.synthetic.main.item_list_comment_area.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.StringBuilder

class ResultsObjectActivity : AppCompatActivity() {

    //ID為日後有登入系統後，可以用來刪留言ID的
    class Comment(val commentId:Int, val nickName: String, val content: String) : IType{
        override val getItemType: Int
            get() = Global.ItemType.COMMENT_AREA
    }

    //物件編號
    private var resultsId: Int = 0
    //防呆，避免物件內沒有經緯度
    private var myMap:GoogleMap? = null
    private lateinit var latLng: LatLng
    private var isFavorite = false
    //網路狀態的觀察者，在onCreate、onResume訂閱，onStop解除
    private val networkStateObserver  = NetworkStateObserver(this,supportFragmentManager)
    //留言區的RecycleView items、CommonAdapter
    private val items = mutableListOf<IType>()
    private lateinit var commonAdapter: CommonAdapter
    //userID
    private lateinit var androidId:String
    //動態載入用參數（總頁碼、當前頁碼、可否call下一頁）
    private var nowPage = 1
    private var pageCount = 1
    private var canLoadNextPage = false
    //生活機能資料
    private val convenienceRangePage = ConvenienceRangePage()
    private val convenienceNearestPage = ConvenienceNearestPage()
    private val convenienceStatisticsPage = ConvenienceStatisticsPage()
    //物件找不到的錯誤動作
    private val errorAction = {
        if(lifecycle.currentState == Lifecycle.State.RESUMED){
            runOnUiThread {
                errorCardview.isVisible = true
                lyFrom.isVisible = false
            }
        }
    }
    //地標找不到的錯誤動作
    private val locationErrorAction = {
        if(lifecycle.currentState == Lifecycle.State.RESUMED){
            runOnUiThread {
                latLng = LatLng(Global.DEFAULT_LAT, Global.DEFAULT_LNG)
                myMap?.run{
                    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }
                Toast.makeText(this@ResultsObjectActivity,"查無此地點！",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    //新增留言錯誤動作
    private val addCommentErrorAction = { idName:String, comment:String, strID:Int ->
        if(lifecycle.currentState == Lifecycle.State.RESUMED){
            runOnUiThread {
                etIDName.setText(idName)
                etComment.setText(comment)
                Toast.makeText(this@ResultsObjectActivity, getString(strID)
                    ,Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results_object)

        if(!RadarController.instance.checkRadar()){
            finish()
        }

        //網路狀態監聽
        val networkErrorDialog = InternetErrorHintDialog(this, supportFragmentManager)
        networkStateObserver.dialog = networkErrorDialog
        networkStateObserver.register()

        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        errorAction.invoke()

        val resultsBundle = intent.extras
        //存儲物件id
        resultsId = resultsBundle?.getInt(Global.BundleKey.HOUSE_NAME) ?: -1
        val averagePrice = resultsBundle?.getInt(Global.BundleKey.AVERAGEPRICE) ?: 0
        val backIndex = resultsBundle?.getInt(Global.BundleKey.RESULT_OBJECT_BACK_INDEX) ?: -1
        val sp = getSharedPreferences(Global.SharePath.RESULT_OBJECT_BACK, Context.MODE_PRIVATE)
        if (backIndex > 0) {
            //會-1是避免backIndex是預設值0，所以前面有加上去，現在扣回
            sp.edit().putInt(Global.SharePath.RESULT_OBJECT_BACK, backIndex - 1).apply()
            sp.edit().putInt(Global.SharePath.RESULT_HOUSEID, resultsId).apply()
        }

        //假如沒有資料的話，就先看是不是外面點進來，如果不是就檢查是不是資料遺失，從sharePath拿
        if (resultsId < 1) {
            var hasResultID = false
            val uri = intent.data
            uri?.run {
                resultsId = uri.getQueryParameter("houseId")?.toInt() ?: -1
                hasResultID = true
            }
            //如果不是Uri的話，就看看暫存的物件標號，如果也沒有，就顯示找不到
            if (!hasResultID) {
                resultsId = sp.getInt(Global.SharePath.RESULT_HOUSEID, -1)
            }
        }

        // 設定viewPager的Adapter，生活機能的資料邏輯在各page中
        val resultAdapter = CommonPageAdapter(supportFragmentManager, lifecycle)
        with(resultAdapter) {
            add(convenienceRangePage)
            add(convenienceNearestPage)
            add(convenienceStatisticsPage)
        }
        viewPager.adapter = resultAdapter
        viewPager.currentItem = 0

        //製作、設定TabLayout的標題
        val pageTitle = arrayOf(
            getString(R.string.results_object_convenience_tab_range)
            , getString(R.string.results_object_convenience_tab_nearest)
            , getString(R.string.results_object_convenience_tab_statistics)
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pageTitle[position]
        }.attach()

        //返回結果列表
        topAppBar.setNavigationOnClickListener {
            run {
                val bundle = Bundle()
                val index = sp.getInt(Global.SharePath.RESULT_OBJECT_BACK, -1)
                var intent: Intent? = null
                if (index != -1) {
                    intent = when (values()[index]) {
                        RESULTS_LIST -> null
                        RESULTS_MAP_LIST -> {
                            Intent(this, ResultsActivity::class.java)
                        }
                        FAVORITE_LIST -> {
                            bundle.putBoolean(Global.BundleKey.ISFAVORITE, true)
                            Intent(this, RadarActivity::class.java).putExtras(bundle)
                        }
                    }
                }
                when {
                    intent != null -> {
                        startActivity(intent)
                        finish()
                    }
                    //如果是從分享過來(雷達頁->物件內頁，直接返回到雷達頁）
                    !RadarController.instance.checkRadar() -> {
                        ActivityController.instance.startActivity(this, RadarActivity::class.java)
                        finish()
                    }
                    else -> {
                        finish()
                    }
                }
            }
        }

        topAppBar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                //分享功能，目前僅能在已有應用程式上面執行，拿到連結網站文字後，
                //在應用程式打開，可以用line傳但要對方也有應用程式，還沒辦法跳轉到安裝頁面
                R.id.share -> {
                    val uri = getSchemaUri(resultsId)
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享物件")
                    shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString())
                    startActivity(Intent.createChooser(shareIntent, "分享房子"))
                    true
                }

                //收藏按鈕
                R.id.favorite -> {
                    if (resultsId > 0 || !tvRent.text.isNullOrEmpty()) {
                        if (isFavorite) {
                            deleteFromFavorite(androidId, resultsId)
                        } else {
                            addToFavorite(androidId, resultsId)
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        //地圖資料
        mapView.run {
            onCreate(savedInstanceState)
            getMapAsync { googleMap ->
                myMap = googleMap
            }
        }

        //設定房屋資訊
        setHouseInfo(averagePrice)

        //設定留言區的recycleView
        val layoutManager = LinearLayoutManager(this)
        rvComment.layoutManager = layoutManager
        commonAdapter = with(CommonAdapter(items)) {
            apply {
                addFooter { parent ->
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_comment_add, parent, false)

                    //調整鍵盤彈出時的方式，並且在確認後關閉
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                            as InputMethodManager

                    //設定標題只有一行，目前最大字數設25
                    view.etIDName.run {
                        isSingleLine = true
                        //文字可以超過視窗大小，才可以左右滑動看
                        setHorizontallyScrolling(true)
                        inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
                        doOnTextChanged { text, _, _, _ ->
                            when {
                                text.isNullOrEmpty() -> {
                                    error = null
                                }
                                text.length > COMMENT_NAME_INPUT_LIMIT -> {
                                    error = "超出字數限制"
                                }
                                text.length in 1..COMMENT_NAME_INPUT_LIMIT -> {
                                    error = null
                                }
                            }
                        }
                    }
                    //設定內容在最上方，目前最大字數設125，將輸入框跟留言案件綁定。
                    view.etComment.run {
                        gravity = Gravity.TOP
                        inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
                        doOnTextChanged { text, _, _, _ ->
                            when {
                                text.isNullOrEmpty() -> {
                                    error = null
                                }
                                text.length > COMMENT_CONTENT_INPUT_LIMIT -> {
                                    error = "超出字數限制"
                                }
                                text.length in 1..COMMENT_CONTENT_INPUT_LIMIT -> {
                                    error = null
                                }
                            }
                        }
                    }

                    view.ibtnAdd.setOnClickListener {
                        var isCanAdd = true
                        view.etIDName.text.toString().let{
                            if(it.isEmpty() || it.length > COMMENT_NAME_INPUT_LIMIT){
                                isCanAdd = false
                                if(it.isEmpty()){
                                    view.etIDName.error = "暱稱不得為空！"
                                }
                            }
                        }
                        view.etComment.text.toString().let{
                            if(it.isEmpty() || it.length > COMMENT_CONTENT_INPUT_LIMIT){
                                isCanAdd = false
                                if(it.isEmpty()){
                                    view.etComment.error = "內容不得為空！"
                                }
                            }
                        }
                        if (isCanAdd) {
                            //取消輸入框的聚焦、鍵盤關閉
                            if(view.etIDName.isFocused){
                                inputMethodManager.hideSoftInputFromWindow(view.etIDName.windowToken,0)
                            }
                            if(view.etComment.isFocused){
                                inputMethodManager.hideSoftInputFromWindow(view.etComment.windowToken, 0)
                            }
                            addComment(etIDName.text.toString(), etComment.text.toString())
                        } else {
                            Toast.makeText(
                                this@ResultsObjectActivity,
                                getString(R.string.results_object_add_comment_no_enter)
                                ,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    object : BaseViewHolder<IType>(view) {
                        override fun bind(item: IType) {
                        }
                    }
                }
                addType(Global.ItemType.COMMENT_AREA) { parent ->
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_comment_area, parent, false)

                    object : BaseViewHolder<Comment>(view) {
                        override fun bind(item: Comment) {
                            view.tvIDName.text = item.nickName
                            view.tvComment.text = item.content
                        }
                    }
                }
            }
        }
        rvComment.adapter = commonAdapter

        //動態載入設定(當物件位置到達目前底部位置，且沒超過總頁數時，載入下一頁)
        commonAdapter.onGetItemViewType = { position ->
            if (position == (rvComment.layoutManager as LinearLayoutManager).itemCount - 1
                && canLoadNextPage && nowPage < pageCount) {
                canLoadNextPage = false
                getCommentNextPage(++nowPage)
            }
        }

        //第一次抓取資料
        getCommentFirstPage()

    }

    //物件內頁callAPI設定資訊
    @SuppressLint("SetTextI18n")
    private fun setHouseInfo(averagePrice:Int){
        NetworkController.instance.getRentalInfo(androidId, resultsId)
            .run {
                onFailure = {errorCode, msg ->
                    Log.d("OnFailure", "$errorCode:$msg")
                    errorAction.invoke()
                }
                onResponse = { res ->
                    Log.d("OnResponse", res)
                    runOnUiThread {
                        try {
                            val jsonObject = JSONObject(res)
                            if (!jsonObject.isNull("status")) {
                                errorAction.invoke()
                                Log.d(
                                    "OnGetRentalInfoError!",
                                    "status:${jsonObject.getInt("status")}"
                                )
                            } else {
                                errorCardview.isVisible = false
                                lyFrom.isVisible = true
                                //如果物件已下架
                                if (!jsonObject.isNull("isOnShelf") && !jsonObject.getBoolean("isOnShelf")) {
                                    errorAction.invoke()
                                }
                                //設置基本資訊的卡片介面
                                val resultName = jsonObject.getString("title")
                                tvAddress.text = resultName
                                Glide.with(this@ResultsObjectActivity)
                                    .load(jsonObject.getString("imgUrl"))
                                    .error(R.drawable.icon_not_search)
                                    .centerCrop()
                                    .into(ivPicture)
                                val price = jsonObject.getInt("price")
                                tvRent.text = price.toString()
                                if (averagePrice > 0) {
                                    when {
                                        price > averagePrice -> {
                                            tvRent.setTextColor(getColor(R.color.favority_red))
                                        }
                                        price == averagePrice -> {
                                            tvRent.setTextColor(getColor(R.color.orange_500))
                                        }
                                        else -> {
                                            tvRent.setTextColor(getColor(R.color.title_green))
                                        }
                                    }
                                    tvAverageDesc.text =
                                        getString(R.string.results_object_average_rent)
                                    tvAverageRent.text = averagePrice.toString()
                                } else {
                                    //如果沒有平均價格，就不顯示(<話
                                    tvRent.setTextColor(getColor(R.color.orange_500))
                                    tvAverageDesc.text = ""
                                    tvAverageRent.text = ""
                                }
                                val location = StringBuilder()
                                location.append(jsonObject.getString("city"))
                                location.append(jsonObject.getString("region"))
                                location.append(jsonObject.getString("address"))
                                tvLocation.text = String(location)
                                val roomType =
                                    HouseInfoTranslate.getTypeOfRoom(jsonObject.getInt("typeOfRoom"))
                                val houseType =
                                    HouseInfoTranslate.getTypeOfHouse(jsonObject.getInt("typeOfHousing"))
                                tvType.text = "$roomType / $houseType"
                                val currentFloor = jsonObject.getInt("currentFloor")
                                val totalFloor = jsonObject.getInt("totalFloor")
                                tvFloor.text = "${currentFloor}F / ${totalFloor}F"
                                tvSquareKm.text = jsonObject.getInt("ping").toString()
                                var date = jsonObject.getLong("systemUpdateTime") / 1000000
                                val year = date / 10000
                                date %= 10000
                                val month =
                                    if (date / 100 < 10) "0${date / 100}" else "${date / 100}"
                                val day = if (date % 100 < 10) "0${date % 100}" else "${date % 100}"
                                tvUpdateTime.text = "$year-$month-$day"
                                val lat = jsonObject.getDouble("latitude")
                                val lng = jsonObject.getDouble("longitude")
                                latLng = LatLng(lat, lng)
                                //防呆，如果經緯度沒有在台灣的範圍的話，就找一次地址
                                if (lat < Global.TAIWAN_RANGE_LEFT.latitude
                                    || lat > Global.TAIWAN_RANGE_RIGHT.latitude
                                    || lng < Global.TAIWAN_RANGE_LEFT.longitude
                                    || lng > Global.TAIWAN_RANGE_RIGHT.longitude
                                ) {
                                    searchLocation(location.toString(), resultName)
                                } else {
                                    myMap?.run {
                                        //將地圖顯示在設定的座標上
                                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                        //標示物件
                                        addMarker(
                                            MarkerOptions().position(latLng).title(resultName)
                                        )
                                    }
                                }
                                myMap?.run {
                                    //將google工具列隱藏
                                    uiSettings.isMapToolbarEnabled = false
                                    //將點擊地圖從直接開googleMap改成跳轉到地圖Fragment
                                    setOnMapClickListener {
                                        run {
                                            if(!DoubleClickGuard.isFastDoubleClick()){
                                                val bundle = Bundle()
                                                val locals =
                                                    doubleArrayOf(latLng.latitude, latLng.longitude)
                                                bundle.putString(
                                                    Global.BundleKey.HOUSE_NAME,
                                                    resultName
                                                )
                                                bundle.putDoubleArray(
                                                    Global.BundleKey.RESULT_LOCATION,
                                                    locals
                                                )
                                                ActivityController.instance.startActivityCustomAnimation(
                                                    this@ResultsObjectActivity,
                                                    ResultObjectMapActivity::class.java,
                                                    bundle
                                                )
                                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                            }
                                        }
                                    }
                                }

                                //設定跳轉至網頁的按鈕
                                lyFrom.setOnClickListener {
                                    run {
                                        val uri = Uri.parse(jsonObject.getString("url"))
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        startActivity(intent)
                                    }
                                }
                                tvFrom.text = jsonObject.getString("source")

                                //依照有無喜愛設置愛心
                                isFavorite = jsonObject.getBoolean("isFavorite")
                                topAppBar.menu.findItem(R.id.favorite).setIcon(
                                    if (isFavorite) {
                                        R.drawable.ic_favorite_white_36dp
                                    } else {
                                        R.drawable.ic_favorite_border_white_36dp
                                    }
                                )

                                try{
                                    //生活機能最近、統計，先讓其有固定的順序排列
                                    val nameList =
                                        arrayListOf("銀行", "學校", "醫院", "超市", "便利商店", "餐飲店家", "大眾運輸站")
                                    val lifeNearestList = arrayListOf<Int>()
                                    val lifeCountList = arrayListOf<Int>()
                                    val nearestJsonArr = jsonObject.getJSONArray("lifeFunctionNearest")
                                    val countJsonArr = jsonObject.getJSONArray("lifeFunctionCount")
                                    for(i in nameList.indices){
                                        val conditionName = nameList[i]
                                        for(j in 0 until nearestJsonArr.length()){
                                            val tmpNearest = nearestJsonArr.getJSONObject(j)
                                            if(tmpNearest.getString("lifeFunctionName") == conditionName){
                                                lifeNearestList.add(tmpNearest.getInt("distance"))
                                                break
                                            }
                                        }
                                        for(j in 0 until countJsonArr.length()){
                                            val tmpCount = countJsonArr.getJSONObject(j)
                                            if(tmpCount.getString("lifeFunctionName") == conditionName){
                                                lifeCountList.add(tmpCount.getInt("count"))
                                                break
                                            }
                                        }
                                    }
                                    //範圍內的生活機能，取完後再照距離排序一次
                                    val lifeRangeList =
                                        mutableListOf<ConvenienceRangePage.ConvenienceItem>()
                                    val rangeJsonArr = jsonObject.getJSONArray("lifeFunctionRange")
                                    for(i in 0 until rangeJsonArr.length()){
                                        val tmpRange = rangeJsonArr.getJSONObject(i)
                                        lifeRangeList.add(ConvenienceRangePage.ConvenienceItem(
                                            tmpRange.getString("lifeFunctionName"),
                                            tmpRange.getInt("distance")
                                        ))
                                    }
                                    lifeRangeList.sortWith(compareBy{it.distance})
                                    //如果完全沒資料，就直接隱藏生活機能，如果有資料的話，就打開，每個做判斷
                                    if(lifeCountList.size < 1 && lifeNearestList.size < 1 &&
                                            lifeRangeList.size <1){
                                        lifeCardView.isVisible = false
                                    }else{
                                        //有的話則將資料給page並更新裡面的資料
                                        lifeCardView.isVisible = true
                                        convenienceNearestPage.nearestList = lifeNearestList
                                        convenienceStatisticsPage.countList = lifeCountList
                                        convenienceRangePage.setInfo(lifeRangeList)
                                    }
                                }catch (e : JSONException){
                                    Log.d("GetLifeFunctionInfo", "JSON FORMAT ERROR! ${e.message}")
                                    lifeCardView.isVisible = false
                                }
                            }
                       } catch (e: JSONException) {
                            Log.d("GetRentalInfo", "JSON FORMAT ERROR! ${e.message}")
                            errorAction.invoke()
                        }
                    }
                }
                exec()
            }
    }

    //搜尋使用
    private fun searchLocation(address: String, resultName:String){
        NetworkController.instance.getLocationByName(address)
            .run{
                onFailure = {errorCode, msg ->
                    Log.d("OnFailure", "$errorCode: $msg")
                    locationErrorAction.invoke()}
                onResponse = {res ->
                    try {
                        val jsonObject = JSONObject(res)
                            .getJSONArray("results")
                            .getJSONObject(0)
                        val location = jsonObject
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        latLng = LatLng(lat, lng)
                        runOnUiThread {
                            myMap?.run {
                                //將地圖顯示在設定的座標上
                                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                //標示物件
                                addMarker(MarkerOptions().position(latLng).title(resultName))
                            }
                        }
                    }catch(e : JSONException){
                        //網路不穩就先到公司但不標marker
                        Log.d("JSON FORMAT Error!", e.message ?:"error!")
                        locationErrorAction.invoke()
                    }
                }
                exec()
            }
        }

    //新增留言
    private fun addComment(idName: String, comment:String){
        try {
            //先清空文字，避免使用者連點，如果失敗，再幫他還回去！
            etIDName.setText("")
            etComment.setText("")
            val jsonObject = JSONObject().apply{
                put("userId", androidId)
                put("rentalSerialNum", resultsId)
                put("nickname", idName)
                put("content", comment)
            }
            NetworkController.instance.addComment(jsonObject).run {
                onFailure = { errorCode, msg ->
                    Log.d("OnFailure", "$errorCode:$msg")
                    addCommentErrorAction.invoke(idName,comment,R.string.filter_error_not_connect)
                }
                onResponse = {res ->
                    if(lifecycle.currentState == Lifecycle.State.RESUMED){
                        when(JSONObject(res).getInt("status")){
                            201 -> {
                                //在現在的列表中加入自己的留言、清空輸入框文字、更新資料
                                runOnUiThread {
                                    //如果有default值，就把他刪了
                                    if(items.contains(Global.DEFAULT_COMMENT)){
                                        commonAdapter.remove(0)
                                    }
                                    commonAdapter.add(Comment(items.size + 1,idName, comment))
                                }
                            }
                            400 -> {
                                addCommentErrorAction.invoke(idName,comment,R.string.filter_error_json_format_error_internet)
                            }
                            404 -> {
                                addCommentErrorAction.invoke(idName,comment,R.string.filter_error_not_found_user)
                            }
                        }
                    }
                }
                exec()
            }
        }catch (e : JSONException){
            Log.d("AddComment", "${e.message}")
            addCommentErrorAction.invoke(idName,comment,R.string.filter_error_json_format_error)
        }
    }

    //拿取留言第一頁+page
    private fun getCommentFirstPage(){
        NetworkController.instance.getCommentFirstPage(androidId,resultsId,Global.MagicNumber.COMMENT_PAGE_COUNT).run {
            onFailure = { errorCode, msg ->  
                Log.d("OnFailure", "$errorCode:$msg")
            }
            onResponse = {res ->  
                try{
                    val data = JSONObject(res)
                    pageCount = data.getInt("pageCount")
                    //第一次取前要把items清空
                    val jsonArray = data.getJSONArray("data")
                    items.clear()
                    for(i in 0 until jsonArray.length()){
                        val jsonObject = jsonArray.getJSONObject(i)
                        items.add(Comment(jsonObject.getInt("commentId"),
                            jsonObject.getString("nickname"),
                            jsonObject.getString("content")))
                    }
                    if(lifecycle.currentState == Lifecycle.State.RESUMED){
                        runOnUiThread {
                            commonAdapter.notifyDataSetChanged()
                            canLoadNextPage = true
                        }
                    }
                }catch (e : JSONException){
                    //不處理statusCode，因為會直接跳到編譯錯誤。都是失敗
                    Log.d("GetCommentFirstPage", "${e.message}")
                }
            }
            onComplete = {
                if(items.size == 0 && lifecycle.currentState == Lifecycle.State.RESUMED){
                    runOnUiThread {
                        commonAdapter.add(Global.DEFAULT_COMMENT)
                    }
                }
            }
            exec()
        }
    }

    //拿取留言的下一頁
    private fun getCommentNextPage(page:Int){
        NetworkController.instance.getCommentList(androidId, resultsId, page).run { 
            onFailure = { errorCode, msg ->
                Log.d("OnFailure", "$errorCode:$msg")
            }
            onResponse = {res ->
                try{
                    //第二次不用刪除
                    val jsonArray = JSONArray(res)
                    for(i in 0 until jsonArray.length()){
                        val jsonObject = jsonArray.getJSONObject(i)
                        items.add(Comment(jsonObject.getInt("commentId"),
                            jsonObject.getString("nickname"),
                            jsonObject.getString("content")))
                    }
                    if(lifecycle.currentState == Lifecycle.State.RESUMED){
                        runOnUiThread {
                            commonAdapter.notifyDataSetChanged()
                        }
                    }
                }catch (e : JSONException){
                    Log.d("GetCommentFirstPage", "${e.message}")
                }
            }
            onComplete = {
                canLoadNextPage = true
                if(items.size == 0 && lifecycle.currentState == Lifecycle.State.RESUMED){
                    runOnUiThread {
                        commonAdapter.add(Global.DEFAULT_COMMENT)
                    }
                }
            }
            exec()
        }
    }

    //新增收藏
    private fun addToFavorite(userId:String, rentalSerialNum: Int){
        NetworkController.instance
            .addToFavorite(userId, rentalSerialNum).run {
                onFailure = {errorCode, msg ->
                    Log.d("OnFailure", "$errorCode:$msg")
                    if(lifecycle.currentState >= Lifecycle.State.CREATED){
                        runOnUiThread {
                            Toast.makeText(this@ResultsObjectActivity, getString(R.string.common_add_favorite_500), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                onResponse = { res ->
                    runOnUiThread {
                        try {
                            val jsonObject = JSONObject(res)
                            when (jsonObject.getInt("status")) {
                                201 -> {
                                    isFavorite = true
                                    topAppBar.menu.findItem(R.id.favorite)
                                        .setIcon(R.drawable.ic_favorite_white_36dp)
                                    Toast.makeText(
                                        this@ResultsObjectActivity,
                                        "已新增收藏",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                400 ->
                                    Toast.makeText(this@ResultsObjectActivity, getString(R.string.common_add_favorite_400), Toast.LENGTH_SHORT).show()
                                404 ->
                                    Toast.makeText(this@ResultsObjectActivity, getString(R.string.common_add_favorite_404), Toast.LENGTH_SHORT).show()
                            }
                        }catch (e:JSONException){
                            Log.d("AddFromFavorite", "JSON FORMAT ERROR! ${e.message}")
                            if(lifecycle.currentState == Lifecycle.State.RESUMED){
                                Toast.makeText(this@ResultsObjectActivity,
                                    getString(R.string.common_add_favorite_400), Toast.LENGTH_SHORT).show()
                            }

                        }
                    }
                }
                exec()
            }
    }
    //刪除收藏
    private fun deleteFromFavorite(userId:String,  rentalSerialNum: Int){
        NetworkController.instance
            .deleteFromFavorite(userId, rentalSerialNum).run {
                onFailure = {errorCode, msg ->
                    Log.d("OnFailure", "$errorCode:$msg")
                    if(lifecycle.currentState >= Lifecycle.State.CREATED){
                        runOnUiThread {
                            Toast.makeText(this@ResultsObjectActivity, getString(R.string.common_delete_favorite_500), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                onResponse = { res ->
                    runOnUiThread {
                        try {
                            val jsonObject = JSONObject(res)
                            when (jsonObject.getInt("status")) {
                                200 -> {
                                    isFavorite = false
                                    topAppBar.menu.findItem(R.id.favorite)
                                        .setIcon(R.drawable.ic_favorite_border_white_36dp)
                                    Toast.makeText(
                                        this@ResultsObjectActivity,
                                        getString(R.string.common_delete_favorite),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                400 ->
                                    Toast.makeText(this@ResultsObjectActivity,
                                        getString(R.string.common_delete_favorite_400),
                                        Toast.LENGTH_SHORT).show()
                                404 ->
                                    Toast.makeText( this@ResultsObjectActivity,
                                        getString(R.string.common_delete_favorite_404),
                                        Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: JSONException) {
                            Log.d("DeleteFromFavorite", "JSON FORMAT ERROR! ${e.message}")
                            if(lifecycle.currentState == Lifecycle.State.RESUMED){
                                Toast.makeText(this@ResultsObjectActivity,
                                    getString(R.string.common_delete_favorite_400),
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                exec()
            }
    }

    override fun onResume() {
        mapView.onResume()
        //開啟時在最上方
        scResultObject.smoothScrollTo(0,20)
        if(!networkStateObserver.isRegister){
            networkStateObserver.register()
        }
        super.onResume()
    }

    override fun onStart() {
        mapView.onStart()
        super.onStart()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        if(networkStateObserver.isRegister){
            networkStateObserver.unregister()
        }
        super.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        mapView.onLowMemory()
        super.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }
    //拿分享的網址
    private fun getSchemaUri(rentalSerialNum: Int):Uri{
        return Uri.parse("http://com.example.rentradar/resultObject?houseId=$rentalSerialNum")
    }

}