package com.example.rentradar.view.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.rentradar.FilterAreaActivity
import com.example.rentradar.R
import com.example.rentradar.R.color.favority_red
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.DoubleClickGuard
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.RadarController
import kotlinx.android.synthetic.main.page_filter_area.view.*
import java.lang.StringBuilder

class FilterAreaPage : Fragment() {

    //要計算文字，所以不會用string資源，關閉其警告
    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.page_filter_area, parent, false)
        val areaCardView = view.findViewById<CardView>(R.id.areaCardView)

        //拿到區域搜尋的名稱，
        val regionList = RadarController.instance.getRadar().regionList
        val bundle = Bundle()

        regionList?.run{
            val cityName = StringBuilder()
            for(i in regionList.indices){
                val city = regionList[i].cityName
                //如果目前的城市沒有該city，就加入字串
                if(!cityName.contains(city)){
                    //因為全區的id都是1000*city代碼，且如果是全區，就不會有其他選項。
                    if(regionList[i].id % 1000 == 0){
                        if(i == 0){
                            cityName.append("$city(全區)")
                        }else{
                            cityName.append(",$city(全區)")
                        }
                    }else{
                        //如果是其他選項，就在掃描一下後面的有沒有相同城市的，有的話就+count，最後做成字串
                        var count = 1
                        for(j in (i + 1) until regionList.size){
                            if(regionList[j].cityName == city){
                                count++
                            }
                        }
                        if(i == 0){
                            cityName.append("$city(${count})")
                        }else{
                            cityName.append(",$city(${count})")
                        }
                    }
                }
            }
            view.tvArea.text = cityName
            bundle.putString(Global.BundleKey.AREA_TITLE_NAME, String(cityName))
        }

        //設定點擊後跳轉到區域搜尋，並將城市名稱帶過去
        areaCardView.setOnClickListener {
            run{
                //防一下連點
                if(!DoubleClickGuard.isFastDoubleClick()){
                    ActivityController.instance
                        .startActivityCustomAnimation(this.activity!!, FilterAreaActivity::class.java, bundle)
                    activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }

        //如果沒有搜尋，顯示提示文字
        if(!RadarController.instance.checkRadarSearch()){
            val color = activity?.getColor(R.color.orange_500)
            color?.run{
                view.tvArea.setTextColor(color)
            }
            view.tvArea.text = getString(R.string.filter_no_search_hint_area)
        }

        return view
    }
}