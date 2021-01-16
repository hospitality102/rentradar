package com.example.rentradar.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rentradar.FilterCommuteActivity
import com.example.rentradar.R
import com.example.rentradar.utils.ActivityController
import com.example.rentradar.utils.DoubleClickGuard
import com.example.rentradar.utils.RadarController
import kotlinx.android.synthetic.main.page_filter_area.view.*
import kotlinx.android.synthetic.main.page_filter_commute.view.*

class FilterCommutePage : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.page_filter_commute, parent, false)

        //拿到commute資烙，並顯示相關資料，如果沒有的話不顯示
        val commute = RadarController.instance.getRadar().commute
        view.tvMinute.text = if(commute == null){ "" }else{ getString(R.string.filter_commute_minute) }
        view.tvTarget.text = commute?.target?.title ?: ""
        view.tvTransport.text = commute?.transport ?: ""
        view.tvTime.text = commute?.time?.toString() ?: ""

        //設定點擊事件
        view.commuteCardView.setOnClickListener {
            //防一下連點
            if(!DoubleClickGuard.isFastDoubleClick()){
                ActivityController.instance
                    .startActivityCustomAnimation(this.activity!!, FilterCommuteActivity::class.java)
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        //如果沒有搜尋，顯示提示文字
        if(!RadarController.instance.checkRadarSearch()){
            val color = activity?.getColor(R.color.orange_500)
            color?.run{
                view.tvTarget.setTextColor(color)
            }
            view.tvTarget.text = getString(R.string.filter_no_search_hint_commute)
        }

        return view
    }
}