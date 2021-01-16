package com.example.rentradar.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.rentradar.FilterActivity
import com.example.rentradar.view.dialog.RadarOptionDialog
import com.example.rentradar.R
import com.example.rentradar.ResultsActivity
import com.example.rentradar.models.*
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.NetworkController
import com.example.rentradar.utils.RadarController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.dialog_radar_option.view.*
import kotlinx.android.synthetic.main.page_radar.*
import kotlinx.android.synthetic.main.page_radar.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class RadarPage : Fragment() {
    //假資料

    private var radarA: Radar? = null
    private var radarB: Radar? = null
    private var radarC: Radar? = null
    private val inputLimit = Global.MagicNumber.INPUT_LIMIT
    private var isPush: Boolean? = null
    private var isFirstOpen: Boolean = true


    @SuppressLint("HardwareIds")
    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.page_radar, parent, false)

        val androidId =
            Settings.Secure.getString(this.activity?.contentResolver, Settings.Secure.ANDROID_ID)
//判定是否為推播通知
        isPush = activity?.intent?.extras?.getBoolean(Global.SharePath.PUSH_INFO)
        if (isPush == null) {
            isPush = false
        }

        //新增雷達
        view.btn_creatA.setOnClickListener {
            view.cv_addA.visibility = View.GONE
            view.cv_setNameA.visibility = View.VISIBLE
            view.ti_renameA.requestFocus()
        }
        view.btn_creatB.setOnClickListener {
            view.cv_addB.visibility = View.GONE
            view.cv_setNameB.visibility = View.VISIBLE
            view.ti_renameB.requestFocus()
        }
        view.btn_creatC.setOnClickListener {
            view.cv_addC.visibility = View.GONE
            view.cv_setNameC.visibility = View.VISIBLE
            view.ti_renameC.requestFocus()
        }


        view.btn_renameA.setOnClickListener {
            val name = view.ti_renameA.text.toString()
            if (radarA == null) {
                if(name.isEmpty()){
                    tf_renameA.error = getString(R.string.radar_word_empty)
                }else{
                    radarA = newRadar(name)
                    RadarController.instance.select(radarA!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
            } else {
                view.cv_setNameA.visibility = View.GONE
                view.cv_radarA.visibility = View.VISIBLE
                //call reNameAPI
                rename(androidId, radarA!!.id, name, 1)
                radarA?.name = name
            }

        }
        view.btn_renameB.setOnClickListener {
            val name = view.ti_renameB.text.toString()
            if (radarB == null) {
                if(name.isEmpty()){
                    tf_renameB.error = getString(R.string.radar_word_empty)
                }else{
                    radarB = newRadar(name)
                    RadarController.instance.select(radarB!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
            } else {
                view.cv_setNameB.visibility = View.GONE
                view.cv_radarB.visibility = View.VISIBLE
                //call reNameAPI
                rename(androidId, radarB!!.id, name, 2)
                radarB?.name = name
            }
        }
        view.btn_renameC.setOnClickListener {
            val name = view.ti_renameC.text.toString()
            if (radarC == null) {
                if(name.isEmpty()){
                    tf_renameC.error = getString(R.string.radar_word_empty)
                }else{
                    radarC = newRadar(name)
                    RadarController.instance.select(radarC!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
            } else {
                view.cv_setNameC.visibility = View.GONE
                view.cv_radarC.visibility = View.VISIBLE
                //call reNameAPI
                rename(androidId, radarC!!.id, name, 3)
                radarC?.name = name
            }
        }

        //輸入框設置
        view.ti_renameA.doOnTextChanged { text, _, _, _ ->
            btn_renameA.isEnabled = text?.length!! <= inputLimit
            when {
                text.length > inputLimit -> tf_renameA.error =
                    getString(R.string.radar_word_limit_exceeded)
                text.length in 1..inputLimit -> tf_renameA.error = null
            }
        }
        view.ti_renameB.doOnTextChanged { text, _, _, _ ->
            btn_renameB.isEnabled = text?.length!! <= inputLimit
            when {
                text.length > inputLimit -> tf_renameB.error =
                    getString(R.string.radar_word_limit_exceeded)
                text.length in 1..inputLimit -> tf_renameB.error = null
            }
        }
        view.ti_renameC.doOnTextChanged { text, _, _, _ ->
            btn_renameC.isEnabled = text?.length!! <= inputLimit
            when {
                text.length > inputLimit -> tf_renameC.error =
                    getString(R.string.radar_word_limit_exceeded)
                text.length in 1..inputLimit -> tf_renameC.error = null
            }
        }

//在Fragment  用 childFragmentManager 在activity下用supportFragmentManager
        view.ib_radarA.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.dialog_radar_option, parent)
            val radarOptionDialog = RadarOptionDialog(dialogView)
            childFragmentManager.let {
                //重新命名
                dialogView.tv_reName.setOnClickListener {
                    view.cv_radarA.visibility = View.GONE
                    view.ti_renameA.setText(tvRadarA.text.toString())
                    view.cv_setNameA.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //設定篩選
                dialogView.tv_editRadar.setOnClickListener {
                    radarOptionDialog.dismiss()
                    RadarController.instance.select(radarA!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
                //刪除
                dialogView.tv_deleteRadar.setOnClickListener {
                    //刪除api
                    deleteRadar(androidId, radarA!!.id)
                    radarA = null
                    view.cv_radarA.visibility = View.GONE
                    view.cv_addA.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //推播
                dialogView.switch_notification.isChecked = radarA?.canPush!!
                dialogView.switch_notification.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        //推播api
                        setPush(androidId, radarA!!.id, true)
                        radarA!!.canPush = true
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_on),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //推播api
                        setPush(androidId, radarA!!.id, false)
                        radarA!!.canPush = false
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                radarOptionDialog.show(it, "")
            }
        }

        view.ib_radarB.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.dialog_radar_option, parent)
            val radarOptionDialog = RadarOptionDialog(dialogView)
            childFragmentManager.let {
                //重新命名
                dialogView.tv_reName.setOnClickListener {
                    view.cv_radarB.visibility = View.GONE
                    view.ti_renameB.setText(tvRadarB.text.toString())
                    view.cv_setNameB.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //設定篩選
                dialogView.tv_editRadar.setOnClickListener {
                    radarOptionDialog.dismiss()
                    RadarController.instance.select(radarB!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
                //刪除
                dialogView.tv_deleteRadar.setOnClickListener {
                    //刪除api
                    deleteRadar(androidId, radarB!!.id)
                    radarB = null
                    view.cv_radarB.visibility = View.GONE
                    view.cv_addB.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //推播
                dialogView.switch_notification.isChecked = radarB?.canPush!!
                dialogView.switch_notification.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        //推播api
                        setPush(androidId, radarB!!.id, true)
                        radarB!!.canPush = true
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_on),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //推播api
                        setPush(androidId, radarB!!.id, false)
                        radarB!!.canPush = false
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                radarOptionDialog.show(it, "")
            }
        }
        view.ib_radarC.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.dialog_radar_option, parent)
            val radarOptionDialog = RadarOptionDialog(dialogView)
            childFragmentManager.let {
                //重新命名
                dialogView.tv_reName.setOnClickListener {
                    view.cv_radarC.visibility = View.GONE
                    view.ti_renameC.setText(tvRadarC.text.toString())
                    view.cv_setNameC.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //設定篩選
                dialogView.tv_editRadar.setOnClickListener {
                    radarOptionDialog.dismiss()
                    RadarController.instance.select(radarC!!)
                    ActivityController.instance.startActivity(
                        this.activity!!,
                        FilterActivity::class.java
                    )
                }
                //刪除
                dialogView.tv_deleteRadar.setOnClickListener {
                    //刪除api
                    deleteRadar(androidId, radarC!!.id)
                    radarC = null
                    view.cv_radarC.visibility = View.GONE
                    view.cv_addC.visibility = View.VISIBLE
                    radarOptionDialog.dismiss()
                }
                //推播
                dialogView.switch_notification.isChecked = radarC?.canPush!!
                dialogView.switch_notification.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        //推播api
                        setPush(androidId, radarC!!.id, true)
                        radarC!!.canPush = true
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_on),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //推播api
                        setPush(androidId, radarC!!.id, false)
                        radarC!!.canPush = false
                        Toast.makeText(
                            this.context,
                            getString(R.string.radar_push_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                radarOptionDialog.show(it, "")
            }
        }

        //雷達完成時，點選進結果列表頁面
        view.cv_radarA.setOnClickListener {
            run {
                //改變單例的雷達，至這一顆
                RadarController.instance.select(radarA!!)
                val bundle = Bundle()
                bundle.putInt(Global.BundleKey.RADARID, radarA!!.id)
                bundle.putBoolean(Global.BundleKey.RADAR_TO_RESULT, true)
                ActivityController.instance.startActivity(
                    this.activity!!,
                    ResultsActivity::class.java,
                    bundle
                )

            }
        }

        view.cv_radarB.setOnClickListener {
            run {
                //改變單例的雷達，至這一顆
                RadarController.instance.select(radarB!!)
                val bundle = Bundle()
                bundle.putInt(Global.BundleKey.RADARID, radarB!!.id)
                bundle.putBoolean(Global.BundleKey.RADAR_TO_RESULT, true)
                ActivityController.instance.startActivity(
                    this.activity!!,
                    ResultsActivity::class.java,
                    bundle
                )

            }
        }

        view.cv_radarC.setOnClickListener {
            run {
                //改變單例的雷達，至這一顆
                RadarController.instance.select(radarC!!)
                val bundle = Bundle()
                bundle.putInt(Global.BundleKey.RADARID, radarC!!.id)
                bundle.putBoolean(Global.BundleKey.RADAR_TO_RESULT, true)
                ActivityController.instance.startActivity(
                    this.activity!!,
                    ResultsActivity::class.java,
                    bundle
                )

            }
        }

        return view
    }

    @SuppressLint("HardwareIds")
    override fun onStart() {
        super.onStart()
        var registerId: String
        //取得安卓id
        val androidId =
            Settings.Secure.getString(this.activity?.contentResolver, Settings.Secure.ANDROID_ID)
        //推播-取得推播憑證
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            //如果沒成功，印錯誤資訊返回不做事
            if (!task.isSuccessful) {
                Log.d("FirebaseMessaging", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            registerId = task.result

            // Log
            Log.d("FirebaseMessaging", registerId)
            //取得使用者資料
            checkAndGetRadar(androidId, registerId)

        })

    }

    private fun checkAndGetRadar(androidId: String, registerId: String) {
        //取得使用者資料
        NetworkController.instance
            .checkUser(androidId, registerId).run {
                onFailure = { errorCode, msg ->
                    this@RadarPage.activity?.runOnUiThread {
                        Toast.makeText(
                            this@RadarPage.activity,
                            getString(R.string.radar_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try {
                        val jsonObject = JSONObject(res)
                        val status = jsonObject.get("status")
                        this@RadarPage.activity?.runOnUiThread {
                            when (status) {
                                200 -> {
                                    getRadarList(androidId)
                                    Toast.makeText(
                                        this@RadarPage.activity,
                                        getString(R.string.radar_checkuser_200),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                201 -> {
                                    checkAndGetRadar(androidId, registerId)
                                    Toast.makeText(
                                        this@RadarPage.activity,
                                        getString(R.string.radar_checkuser_201),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                400 -> Toast.makeText(
                                    this@RadarPage.activity,
                                    getString(R.string.radar_checkuser_400),
                                    Toast.LENGTH_SHORT
                                ).show()
                                404 -> Toast.makeText(
                                    this@RadarPage.activity,
                                    getString(R.string.radar_checkuser_404),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: JSONException) {
                        Log.d("checkAndGetRadar", "${e.message} / JSON FORMAT ERROR!")
                    }
                }
                exec()
            }
    }

    override fun onStop() {
        resetView()
        super.onStop()
    }

    private fun resetView() {
        cv_radarA.visibility = View.GONE
        cv_radarB.visibility = View.GONE
        cv_radarC.visibility = View.GONE
        cv_setNameA.visibility = View.GONE
        cv_setNameB.visibility = View.GONE
        cv_setNameC.visibility = View.GONE

    }

    private fun viewInit() {
        this@RadarPage.activity?.runOnUiThread {
            //畫面初始化

            if (radarA != null) {
                view?.cv_addA?.visibility = View.GONE
                view?.cv_radarA?.visibility = View.VISIBLE
                view?.tvRadarA?.text = radarA?.name
                //通知紅點
                when {
                    radarA?.count!! <= 0 -> {
                        tvNewCountA.isVisible = false
                    }
                    radarA?.count!! in 1..999 -> {
                        tvNewCountA.text =
                            radarA?.count.toString()
                        tvNewCountA.isVisible = true
                    }
                    radarA?.count!! > 999 -> {
                        tvNewCountA.text = getString(R.string.radar_newcount_limit)
                        tvNewCountA.isVisible = true
                    }
                }
                //搜尋標籤
                when {
                    radarA?.regionList != null -> {
                        searchLabelA.text = getString(R.string.radar_label_region)
                        searchLabelA.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_region)
                    }
                    radarA?.mrtList != null -> {
                        searchLabelA.text = getString(R.string.radar_label_mrt)
                        searchLabelA.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_mrt)
                    }
                    radarA?.landmark != null -> {
                        searchLabelA.text = getString(R.string.radar_label_landmark)
                        searchLabelA.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_landmark)
                    }
                    radarA?.commute != null -> {
                        searchLabelA.text = getString(R.string.radar_label_commute)
                        searchLabelA.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_commute)
                    }
                }
                //租金標籤
                when {
                    radarA?.maxPrice == 99999 && radarA?.minPrice == 0 -> tvPriceLabelA.text =
                        getString(R.string.radar_price_unlimited)
                    radarA?.maxPrice == 99999 && radarA?.minPrice!! > 0 -> tvPriceLabelA.text =
                        getString(
                            R.string.radar_price_range, radarA?.minPrice.toString(), getString(
                                R.string.radar_unlimited
                            )
                        )
                    else -> tvPriceLabelA.text =
                        getString(
                            R.string.radar_price_range,
                            radarA?.minPrice.toString(),
                            radarA?.maxPrice.toString()
                        )
                }
            } else {
                view?.cv_addA?.visibility = View.VISIBLE
                view?.ti_renameA?.setText("")
                tvNewCountA.isVisible = false
            }
            if (radarB != null) {
                view?.cv_addB?.visibility = View.GONE
                view?.cv_radarB?.visibility = View.VISIBLE
                view?.tvRadarB?.text = radarB?.name
                //通知紅點
                when {
                    radarB?.count!! <= 0 -> {
                        tvNewCountB.isVisible = false
                    }
                    radarB?.count!! in 1..999 -> {
                        tvNewCountB.text = radarB?.count.toString()
                        tvNewCountB.isVisible = true
                    }
                    radarB?.count!! > 999 -> {
                        tvNewCountB.text = getString(R.string.radar_newcount_limit)
                        tvNewCountB.isVisible = true
                    }
                }
                //搜尋標籤
                when {
                    radarB?.regionList != null -> {
                        searchLabelB.text = getString(R.string.radar_label_region)
                        searchLabelB.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_region)
                    }
                    radarB?.mrtList != null -> {
                        searchLabelB.text = getString(R.string.radar_label_mrt)
                        searchLabelB.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_mrt)
                    }
                    radarB?.landmark != null -> {
                        searchLabelB.text = getString(R.string.radar_label_landmark)
                        searchLabelB.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_landmark)
                    }
                    radarB?.commute != null -> {
                        searchLabelB.text = getString(R.string.radar_label_commute)
                        searchLabelB.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_commute)
                    }
                }
                //租金標籤
                when {
                    radarB?.maxPrice == 99999 && radarB?.minPrice == 0 -> tvPriceLabelB.text =
                        getString(R.string.radar_price_unlimited)
                    radarB?.maxPrice == 99999 && radarB?.minPrice!! > 0 -> tvPriceLabelB.text =
                        getString(
                            R.string.radar_price_range, radarB?.minPrice.toString(), getString(
                                R.string.radar_unlimited
                            )
                        )
                    else -> tvPriceLabelB.text =
                        getString(
                            R.string.radar_price_range,
                            radarB?.minPrice.toString(),
                            radarB?.maxPrice.toString()
                        )
                }
            } else {
                view?.cv_addB?.visibility = View.VISIBLE
                view?.ti_renameB?.setText("")
                tvNewCountB.isVisible = false
            }
            if (radarC != null) {
                view?.cv_addC?.visibility = View.GONE
                view?.cv_radarC?.visibility = View.VISIBLE
                view?.tvRadarC?.text = radarC?.name
                //通知紅點
                when {
                    radarC?.count!! <= 0 -> {
                        tvNewCountC.isVisible = false
                    }
                    radarC?.count!! in 1..999 -> {
                        tvNewCountC.text = radarC?.count.toString()
                        tvNewCountC.isVisible = true
                    }
                    radarC?.count!! > 999 -> {
                        tvNewCountC.text = getString(R.string.radar_newcount_limit)
                        tvNewCountC.isVisible = true
                    }
                }
                //搜尋標籤
                when {
                    radarC?.regionList != null -> {
                        searchLabelC.text = getString(R.string.radar_label_region)
                        searchLabelC.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_region)
                    }
                    radarC?.mrtList != null -> {
                        searchLabelC.text = getString(R.string.radar_label_mrt)
                        searchLabelC.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_mrt)
                    }
                    radarC?.landmark != null -> {
                        searchLabelC.text = getString(R.string.radar_label_landmark)
                        searchLabelC.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_landmark)
                    }
                    radarC?.commute != null -> {
                        searchLabelC.text = getString(R.string.radar_label_commute)
                        searchLabelC.background =
                            ContextCompat.getDrawable(context!!, R.drawable.shape_label_commute)
                    }
                }
                //租金標籤
                when {
                    radarC?.maxPrice == 99999 && radarC?.minPrice == 0 -> tvPriceLabelC.text =
                        getString(R.string.radar_price_unlimited)
                    radarC?.maxPrice == 99999 && radarC?.minPrice!! > 0 -> tvPriceLabelC.text =
                        getString(
                            R.string.radar_price_range, radarC?.minPrice.toString(), getString(
                                R.string.radar_unlimited
                            )
                        )
                    else -> tvPriceLabelC.text =
                        getString(
                            R.string.radar_price_range,
                            radarC?.minPrice.toString(),
                            radarC?.maxPrice.toString()
                        )
                }
            } else {
                view?.cv_addC?.visibility = View.VISIBLE
                view?.ti_renameC?.setText("")
                tvNewCountC.isVisible = false
            }

        }
    }


    private fun newRadar(name: String): Radar {
        val conditionMap: HashMap<String, Int> = hashMapOf()
        val oneChooseMap: HashMap<String, Boolean> = hashMapOf()

        return Radar(
            -1,
            name,
            true,
            0,
            null,
            null,
            null,
            null,
            0,
            99999,
            0,
            conditionMap,
            oneChooseMap
        )

    }

    private fun setRadar(jsonObject: JSONObject): Radar? {
        try {
            println(jsonObject)
            val radarId = jsonObject.getInt("radarId")
            val radarName = jsonObject.getString("radarName")
            val canPush = jsonObject.getBoolean("canPush")
            val count = jsonObject.getInt("count")
            val jsonData = jsonObject.getJSONObject("data")
            val regionJsonArr: JSONArray? = if (jsonData.isNull("region")) {
                null
            } else {
                jsonData.getJSONArray("region")
            }
            var regionList: MutableList<Region>? = null
            if (regionJsonArr != null) {
                regionList = mutableListOf()
                for (i in 0 until regionJsonArr.length()) {
                    val regionJsonObject = regionJsonArr.getJSONObject(i)
                    val regionId = regionJsonObject.getInt("regionId")
                    val cityName = regionJsonObject.getString("cityName")
                    val region = Region(regionId, cityName)
                    regionList.add(region)
                }
            }
            val mrtJsonArr: JSONArray? = if (jsonData.isNull("mrt")) {
                null
            } else {
                jsonData.getJSONArray("mrt")
            }
            var mrtList: MutableList<MRT>? = null
            if (mrtJsonArr != null) {
                mrtList = mutableListOf()
                for (i in 0 until mrtJsonArr.length()) {
                    val mrtJsonObject = mrtJsonArr.getJSONObject(i)
                    val mrtStationId = mrtJsonObject.getInt("mrtStationId")
                    val mrtStationName = mrtJsonObject.getString("mrtStationName")
                    val mrt = MRT(mrtStationId, mrtStationName)
                    mrtList.add(mrt)
                }
            }

            val landmarkObject: JSONObject? = if (jsonData.isNull("landmark")) {
                null
            } else {
                jsonData.getJSONObject("landmark")
            }
            var landmark: Landmark? = null
            if (landmarkObject != null) {
                val addressTitle = landmarkObject.getString("addressTitle")
                val address = landmarkObject.getString("address")
                val addressRange = landmarkObject.getDouble("addressRange")
                val addressLongitude = landmarkObject.getDouble("addressLongitude")
                val addressLatitude = landmarkObject.getDouble("addressLatitude")
                landmark =
                    Landmark(addressTitle, address, addressRange, addressLongitude, addressLatitude)
            }
            var commute: Commute? = null
            if (!jsonData.isNull("commute")) {
                val commuteObject = jsonData.getJSONObject("commute")
                val transport = commuteObject.getString("transport")
                val time = commuteObject.getInt("lengthOfTime")
                val mrtIdList: MutableList<Int> = mutableListOf()
                if (!commuteObject.isNull("mrt")) {
                    val mrtJSONArr = commuteObject.getJSONArray("mrt")
                    if (mrtJSONArr.length() > 0) {
                        for (i in 0 until mrtJSONArr.length()) {
                            mrtIdList.add(mrtJSONArr.getJSONObject(i).getInt("mrtStationId"))
                        }
                    }
                }
                val targetJson = commuteObject.getJSONObject("landmark")
                val addressTitle = targetJson.getString("addressTitle")
                val address = targetJson.getString("address")
                val addressRange = targetJson.getDouble("addressRange")
                val addressLongitude = targetJson.getDouble("addressLongitude")
                val addressLatitude = targetJson.getDouble("addressLatitude")
                val target =
                    Landmark(addressTitle, address, addressRange, addressLongitude, addressLatitude)
                commute = Commute(target, transport, time, mrtIdList)
            }

            val priceMin = jsonData.getInt("priceMin")
            val priceMax = jsonData.getInt("priceMax")
            val typeOfRoom = jsonData.getInt("typeOfRoom")
            val typeOfHousing = jsonData.getInt("typeOfHousing")
            val spatialLayout = jsonData.getInt("spatialLayout")
            val isAgent = !jsonData.getBoolean("isAgent")
            val floor = jsonData.getInt("floor")
            val isRoofTop = !jsonData.getBoolean("isRoofTop")
            val identity = jsonData.getInt("identity")
            val sex = jsonData.getInt("sex")
            val canShortTerm = jsonData.getBoolean("canShortTerm")
            val canKeepPet = jsonData.getBoolean("canKeepPet")
            val canCook = jsonData.getBoolean("canCook")
            val hasParkingSpace = jsonData.getBoolean("hasParkingSpace")
            val furniture = jsonData.getInt("furniture")
            val appliances = jsonData.getInt("appliances")
            val lifeFunction = jsonData.getInt("lifeFunction")
            val traffic = jsonData.getInt("traffic")

            val conditionMap: HashMap<String, Int> = hashMapOf()
            val oneChooseMap: HashMap<String, Boolean> = hashMapOf()

            //多選Map
            conditionMap["建築物型態"] = typeOfHousing
            conditionMap["房屋格局"] = spatialLayout
            conditionMap["樓層"] = floor
            conditionMap["房客身分"] = identity
            conditionMap["房客性別"] = sex
            conditionMap["附傢俱"] = furniture
            conditionMap["附設備"] = appliances
            conditionMap["周邊機能"] = lifeFunction
            conditionMap["交通站點"] = traffic
            //單選Map
            oneChooseMap["附車位"] = hasParkingSpace
            oneChooseMap["不要房仲"] = isAgent
            oneChooseMap["不要頂樓加蓋"] = isRoofTop
            oneChooseMap["可短租"] = canShortTerm
            oneChooseMap["可開伙"] = canCook
            oneChooseMap["可養寵物"] = canKeepPet

            return Radar(
                radarId,
                radarName,
                canPush,
                count,
                regionList,
                mrtList,
                landmark,
                commute,
                priceMin,
                priceMax,
                typeOfRoom,
                conditionMap,
                oneChooseMap
            )
        } catch (e: JSONException) {
            Log.d("setRadar", "${e.message} / JSON FOMRAT ERROR!")
        }
        return newRadar("Error!")
    }

    private fun getRadarList(androidId: String) {
        //取得雷達列表
        NetworkController.instance
            .getRadarList(androidId).run {
                onFailure = { errorCode, msg ->
                    this@RadarPage.activity?.runOnUiThread {
                        Toast.makeText(
                            this@RadarPage.activity,
                            getString(R.string.radar_error_not_connect),
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("OnFailure", "$errorCode:$msg")
                    }
                }
                onResponse = { res ->
                    try {
                        val jsonArray = JSONArray(res)
                        //設定雷達
                        radarA = if (jsonArray.isNull(0)) {
                            null
                        } else {
                            setRadar(jsonArray.getJSONObject(0))
                        }
                        radarB = if (jsonArray.isNull(1)) {
                            null
                        } else {
                            setRadar(jsonArray.getJSONObject(1))
                        }
                        radarC = if (jsonArray.isNull(2)) {
                            null
                        } else {
                            setRadar(jsonArray.getJSONObject(2))
                        }
                    } catch (e: JSONException) {
                        Log.d("CheckUser", "${e.message}")
                    }
                    viewInit() //畫面初始化
                    this@RadarPage.activity?.runOnUiThread {
                        if (!isPush!! && isFirstOpen && radarA != null &&
                            ContextCompat.checkSelfPermission(
                                this@RadarPage.activity!!,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            isFirstOpen = false
                            RadarController.instance.select(radarA!!)
                            val bundle = Bundle()
                            bundle.putInt(
                                Global.BundleKey.RADARID,
                                radarA!!.id
                            )
                            bundle.putBoolean(
                                Global.BundleKey.RADAR_TO_RESULT,
                                true
                            )
                            ActivityController.instance.startActivity(
                                this@RadarPage.activity!!,
                                ResultsActivity::class.java,
                                bundle
                            )
                        }
                    }
                }
                exec()
            }
    }

    private fun rename(userId: String, radarId: Int, radarName: String, radar: Int) {
        NetworkController.instance.renameRadar(userId, radarId, radarName).run {
            onFailure = { errorCode, msg ->
                this@RadarPage.activity?.runOnUiThread {
                    Toast.makeText(
                        this@RadarPage.activity,
                        getString(R.string.radar_error_not_connect),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OnFailure", "$errorCode:$msg")
                }
            }
            onResponse = { res ->
                try {
                    val jsonObject = JSONObject(res)
                    this@RadarPage.activity?.runOnUiThread {
                        when (jsonObject.get("status")) {
                            200 -> {
                                Toast.makeText(
                                    this@RadarPage.activity,
                                    getString(R.string.radar_rename_200),
                                    Toast.LENGTH_SHORT
                                ).show()
                                when (radar) {
                                    1 -> view?.tvRadarA?.text = radarName
                                    2 -> view?.tvRadarB?.text = radarName
                                    3 -> view?.tvRadarC?.text = radarName
                                }
                            }
                            400 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_rename_400),
                                Toast.LENGTH_SHORT
                            ).show()
                            404 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_rename_404),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.d("rename", "${e.message} / JSON FORMAT ERROR!")
                }
            }
            exec()
        }
    }

    private fun deleteRadar(userId: String, radarId: Int) {
        NetworkController.instance.deleteRadar(userId, radarId).run {
            onFailure = { errorCode, msg ->
                this@RadarPage.activity?.runOnUiThread {
                    Toast.makeText(
                        this@RadarPage.activity,
                        getString(R.string.radar_error_not_connect),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OnFailure", "$errorCode:$msg")
                }
            }
            onResponse = { res ->
                try {
                    val jsonObject = JSONObject(res)
                    this@RadarPage.activity?.runOnUiThread {
                        when (jsonObject.get("status")) {
                            200 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_delete_200),
                                Toast.LENGTH_SHORT
                            ).show()
                            400 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_delete_400),
                                Toast.LENGTH_SHORT
                            ).show()
                            404 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_delete_404),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.d("deleteRadar", "${e.message} / JSON FORMAT ERROR!")
                }
            }
            exec()
        }
    }

    private fun setPush(userId: String, radarId: Int, canPush: Boolean) {
        NetworkController.instance.setPushRadar(userId, radarId, canPush).run {
            onFailure = { errorCode, msg ->
                this@RadarPage.activity?.runOnUiThread {
                    Toast.makeText(
                        this@RadarPage.activity,
                        getString(R.string.radar_error_not_connect),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("OnFailure", "$errorCode:$msg")
                }
            }
            onResponse = { res ->
                try {
                    val jsonObject = JSONObject(res)
                    this@RadarPage.activity?.runOnUiThread {
                        when (jsonObject.get("status")) {

                            400 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_push_400),
                                Toast.LENGTH_SHORT
                            ).show()

                            404 -> Toast.makeText(
                                this@RadarPage.activity,
                                getString(R.string.radar_push_404),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.d("setPush", "${e.message} / JSON FORMAT ERROR!")
                }

            }
            exec()
        }
    }
}