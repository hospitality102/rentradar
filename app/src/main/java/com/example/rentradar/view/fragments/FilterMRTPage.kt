package com.example.rentradar.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.rentradar.FilterMrtActivity
import com.example.rentradar.R
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.DoubleClickGuard
import com.example.rentradar.utils.RadarController
import kotlinx.android.synthetic.main.page_filter_area.view.*
import kotlinx.android.synthetic.main.page_filter_mrt.view.*
import java.lang.StringBuilder

class FilterMRTPage : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.page_filter_mrt, parent, false)
        val cardView = view.findViewById<CardView>(R.id.mrtCardView)

        //拿到MRT的資料，取出mrtList裡面的所有站名
        val mrtList = RadarController.instance.getRadar().mrtList
        mrtList?.run{
            val mrtInfoTitle = StringBuilder()
            for(i in mrtList.indices){
                if(i == 0){
                    mrtInfoTitle.append(mrtList[i].name)
                }else{
                    mrtInfoTitle.append(",${mrtList[i].name}")
                }
            }
            view.tvMrt.text = mrtInfoTitle
        }

        //點擊後跳至MRT搜尋頁面
        cardView.setOnClickListener {
            run{
                //防止連點
                if(!DoubleClickGuard.isFastDoubleClick()){
                    ActivityController.instance.
                    startActivityCustomAnimation(this.activity!!, FilterMrtActivity::class.java)
                    activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }

        //如果沒有搜尋，顯示提示文字
        if(!RadarController.instance.checkRadarSearch()){
            val color = activity?.getColor(R.color.orange_500)
            color?.run{
                view.tvMrt.setTextColor(color)
            }
            view.tvMrt.text = getString(R.string.filter_no_search_hint_mrt)
        }

        return view
    }

}