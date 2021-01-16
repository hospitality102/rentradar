package com.example.rentradar.view.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.rentradar.*
import com.example.rentradar.models.House
import com.example.rentradar.utils.*
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_results.*
import kotlinx.android.synthetic.main.item_house.view.*
import kotlinx.android.synthetic.main.page_favorites.*
import kotlinx.android.synthetic.main.page_favorites.view.*
import org.json.JSONArray
import org.json.JSONObject


@Suppress("SameParameterValue")
class FavoritesPage : Fragment() {
    private var isSortedByUpdateTime = true
    private var isSortedByPrice = false
    private var isSortedByPing = false
    private var canLoadNextPage = true
    private var pageCount = 0
    private var nowPage = 1
    private var items = mutableListOf<IType>()
    private lateinit var commonAdapter: CommonAdapter
    private var sortBy = 3
    private var sortDirection = 2

    @SuppressLint("HardwareIds")
    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView: View = inflater.inflate(R.layout.page_favorites, parent, false)
        val androidId =
            Settings.Secure.getString(this.activity?.contentResolver, Settings.Secure.ANDROID_ID)
        //tab初始化
        tabTextSet(
            fragmentView.tlFavoriteSort.getTabAt(0),
            getString(R.string.favorites_tab_update_1)
        )
        tabTextSet(
            fragmentView.tlFavoriteSort.getTabAt(1),
            getString(R.string.favorites_tab_price_1)
        )
        tabTextSet(
            fragmentView.tlFavoriteSort.getTabAt(2),
            getString(R.string.favorites_tab_ping_1)
        )

//頂部bar事件設置

        fragmentView.topAppBar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                //點擊比較頁面圖示
                R.id.compare -> {
                    if(!DoubleClickGuard.isFastDoubleClick()){
                        val bundle = Bundle()
                        val titleArr = arrayListOf<String>()
                        val serialNumArr = arrayListOf<Int>()
                        items.forEach {
                            if (it is House && it.isOnShelf) {
                                titleArr.add(it.title!!)
                                serialNumArr.add(it.serialNum)
                            }
                        }

                        bundle.putIntegerArrayList(
                            Global.BundleKey.COMPARE_ID_ARR,
                            serialNumArr
                        )
                        bundle.putStringArrayList(Global.BundleKey.COMPARE_NAME_ARR, titleArr)
                        ActivityController.instance.startActivityCustomAnimation(
                            this.activity!!,
                            CompareActivity::class.java,
                            bundle
                        )
                        activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                    true
                }
                //切換為地圖顯示
                R.id.map -> {
                    if (items.isNotEmpty()) {
                        val bundle = Bundle()
                        val house: ArrayList<House>? = ArrayList()
                        items.forEach {
                            if (it is House) {
                                house?.add(it)
                            }
                        }
                        bundle.putParcelableArrayList(Global.BundleKey.RESULT_TO_MAP, house)
                        ActivityController.instance.startActivity(
                            this.activity!!,
                            FavoritesMapActivity::class.java, bundle
                        )
                        this@FavoritesPage.activity!!.finish()
                    } else {
                        Toast.makeText(
                            this.context,
                            getString(R.string.favorites_no_item),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                else -> false
            }
        }


//recycleView設置
        fragmentView.rvFavorite.layoutManager = LinearLayoutManager(this.context)

        //初始化commandAdapter
        commonAdapter = CommonAdapter(items)

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
                            if (!view.cbFavorite.isChecked) {
                                deleteFromFavorite(androidId, item, commonAdapter, items)
                            }
                        }
                        if (item.isOnShelf) {
                            Glide.with(view).load(item.ImgUrl).override(1800, 600).centerCrop()
                                .into(view.ivHouseImg)
                        } else {
                            Glide.with(view).load(R.drawable.item_default).centerCrop()
                                .into(view.ivHouseImg)
                        }
                        //物件點擊事件
                        view.card.setOnClickListener {
                            if (item.isOnShelf) {
                                val bundle = Bundle()
                                bundle.putInt(Global.BundleKey.HOUSE_NAME, item.serialNum)
                                bundle.putInt(
                                    Global.BundleKey.RESULT_OBJECT_BACK_INDEX,
                                    Global.BackIndex.FAVORITE_LIST.serial
                                )
                                ActivityController.instance.startActivity(
                                    this@FavoritesPage.activity!!,
                                    ResultsObjectActivity::class.java, bundle
                                )
                            } else {
                                Toast.makeText(
                                    this@FavoritesPage.activity,
                                    getString(R.string.favorites_is_not_onshelf),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
        //動態載入設定(當物件位置到達目前底部位置，且沒超過總頁數，載入下一頁)
        commonAdapter.onGetItemViewType = {
            if (it == (rvFavorite.layoutManager as LinearLayoutManager).itemCount - 1 && canLoadNextPage && nowPage < pageCount) {
                canLoadNextPage = false
                getFavoritelist(items, androidId, ++nowPage, commonAdapter)
            }
        }

        fragmentView.rvFavorite.adapter = commonAdapter


        //篩選排序
        fragmentView.tlFavoriteSort.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        sortByUpdate()
                        sortResetView()
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByUpdateTime) {
                            tabTextSet(tab, getString(R.string.result_tab_update_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_update_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.result_sort_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        sortByPrice()
                        sortResetView()
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByPrice) {
                            tabTextSet(tab, getString(R.string.result_tab_price_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_price_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.result_sort_price),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        sortByPing()
                        sortResetView()
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByPing) {
                            tabTextSet(tab, getString(R.string.result_tab_ping_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_ping_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
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
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByUpdateTime) {
                            tabTextSet(tab, getString(R.string.result_tab_update_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_update_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.result_sort_update),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    1 -> {
                        sortByPrice()
                        sortResetView()
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByPrice) {
                            tabTextSet(tab, getString(R.string.result_tab_price_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_price_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.result_sort_price),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        sortByPing()
                        sortResetView()
                        getFavoritelistFirstPage(
                            items,
                            androidId,
                            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
                            sortBy,
                            sortDirection,
                            commonAdapter
                        ) {
                            pageCount = it
                        }
                        if (isSortedByPing) {
                            tabTextSet(tab, getString(R.string.result_tab_ping_1))
                        } else {
                            tabTextSet(tab, getString(R.string.result_tab_ping_2))
                        }
                        commonAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.result_sort_ping),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        return fragmentView //建收藏頁面View

    }

    @SuppressLint("HardwareIds")
    override fun onStart() {
        super.onStart()
        val androidId =
            Settings.Secure.getString(this.activity?.contentResolver, Settings.Secure.ANDROID_ID)
        //載入第一頁
        getFavoritelistFirstPage(
            items,
            androidId,
            Global.MagicNumber.DYNAMIC_ITEM_COUNT,
            commonAdapter=commonAdapter
        ) {
            pageCount = it
        }
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


    private fun deleteFromFavorite(
        userId: String,
        item: House,
        commonAdapter: CommonAdapter,
        items: MutableList<IType>
    ) {
        NetworkController.instance
            .deleteFromFavorite(userId, item.serialNum).run {
                onFailure = { errorCode, msg ->
                    this@FavoritesPage.activity?.runOnUiThread {
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.favorite_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    val jsonObject = JSONObject(res)
                    this@FavoritesPage.activity?.runOnUiThread {
                        when (jsonObject.get("status")) {
                            200 -> {
                                item.isFavorite = false
                                items.remove(item)
                                Toast.makeText(
                                    this@FavoritesPage.activity,
                                    getString(R.string.favorite_delete_200),
                                    Toast.LENGTH_SHORT
                                ).show()
                                commonAdapter.notifyDataSetChanged()
                            }
                            400 -> Toast.makeText(
                                this@FavoritesPage.activity,
                                getString(R.string.favorite_delete_400),
                                Toast.LENGTH_SHORT
                            ).show()
                            404 -> Toast.makeText(
                                this@FavoritesPage.activity,
                                getString(R.string.favorite_delete_404),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                exec()
            }
    }

    private fun getFavoritelistFirstPage(
        items: MutableList<IType>,
        userId: String,
        quantity: Int,
        sortBy:Int=3,
        sortDirection:Int=2,
        commonAdapter: CommonAdapter,
        action: ((int: Int) -> Unit)
    ) {

        NetworkController.instance
            .getFavoritelistFirstPage(userId, quantity,sortBy,sortDirection).run {
                onFailure = { errorCode, msg ->
                    this@FavoritesPage.activity?.runOnUiThread {
                        Toast.makeText(
                            this@FavoritesPage.activity,
                            getString(R.string.favorite_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    val jsonObject = if (res.isEmpty()) {
                        JSONObject()
                    } else {
                        JSONObject(res)
                    }

                    pageCount = if (jsonObject.isNull("pageCount")) {
                        0
                    } else {
                        jsonObject.getInt("pageCount")
                    }
                    this@FavoritesPage.activity?.runOnUiThread {
                        action.invoke(pageCount)
                    }

                    val data = if (jsonObject.isNull("data")) {
                        null
                    } else {
                        jsonObject.getJSONArray("data")
                    }
                    if (data != null) {
                        items.clear() //重置
                        for (i in 0 until data.length()) {
                            val rentalObject = data.getJSONObject(i)
                            val rentalSerialNum = rentalObject.getInt("rentalSerialNum")
                            val source = rentalObject.getString("source")
                            val title = rentalObject.getString("title")
                            val systemUpdateTime = rentalObject.getLong("systemUpdateTime")
                            val city = rentalObject.getString("city")
                            val region = rentalObject.getString("region")
                            val address = rentalObject.getString("address")
                            val addressTotal = "$city$region$address"
                            val longitude = rentalObject.getDouble("longitude")
                            val latitude = rentalObject.getDouble("latitude")
                            val imgUrl = rentalObject.getString("imgUrl")
                            val price = rentalObject.getInt("price")
                            val ping = rentalObject.getInt("ping")
                            val typeOfRoom = rentalObject.getInt("typeOfRoom")
                            val isFavorite = rentalObject.getBoolean("isFavorite")
                            val isOnShelf = rentalObject.getBoolean("isOnShelf")
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
                        this@FavoritesPage.activity?.runOnUiThread {
                            commonAdapter.notifyDataSetChanged()
                        }
                    }
                }
                exec()
            }

    }

    private fun getFavoritelist(
        items: MutableList<IType>,
        userId: String,
        page: Int,
        commonAdapter: CommonAdapter
    ) {
        NetworkController.instance.getFavoritelist(userId, page).run {
            onFailure = { errorCode, msg ->
                this@FavoritesPage.activity?.runOnUiThread {
                    Toast.makeText(
                        this@FavoritesPage.activity,
                      getString(R.string.favorite_error_not_connect),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OnFailure", "$errorCode:$msg")
                }
            }
            onResponse = { res ->
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
                    this@FavoritesPage.activity?.runOnUiThread {
                        commonAdapter.notifyDataSetChanged()
                        canLoadNextPage = true
                    }
                }
            }
            exec()
        }
    }

    private fun tabTextSet(tab: TabLayout.Tab?, str: String) {
        val absoluteSizeSpan = AbsoluteSizeSpan(60)
        val spannableStringBuilder = buildSpannedString {
            append(str)
            setSpan(absoluteSizeSpan, 4, 6, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        tab?.text = spannableStringBuilder
    }
    private  fun sortResetView(){
        canLoadNextPage = true
        pageCount = 0
        nowPage = 1
        items.clear()
    }

}
