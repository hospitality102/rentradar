package com.example.rentradar.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.rentradar.FilterLandmarkActivity
import com.example.rentradar.R
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.DoubleClickGuard
import com.example.rentradar.utils.RadarController
import kotlinx.android.synthetic.main.page_filter_area.view.*
import kotlinx.android.synthetic.main.page_filter_landmark.view.*

class FilterLandmarkPage : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.page_filter_landmark, parent, false)
        val cardView = view.findViewById<CardView>(R.id.landmarkCardView)

        //拿到landMark的資料，取出裡面的標題名稱及距離
        val landmark = RadarController.instance.getRadar().landmark
        view.tvLandmark.text = landmark?.title ?:""
        view.tvDistance.text = if(landmark != null) {"${landmark.range}" } else {""}

        //設定點擊後跳轉到地標搜尋
        cardView.setOnClickListener {
            run{
                //防止連點
                if(!DoubleClickGuard.isFastDoubleClick()){
                    ActivityController.instance
                        .startActivityCustomAnimation(this.activity!!, FilterLandmarkActivity::class.java)
                    activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }

        //如果沒有搜尋，顯示提示文字
        if(!RadarController.instance.checkRadarSearch()){
            val color = activity?.getColor(R.color.orange_500)
            color?.run{
                view.tvLandmark.setTextColor(color)
            }
            view.tvLandmark.text = getString(R.string.filter_no_search_hint_landmark)
        }

        return view
    }

}