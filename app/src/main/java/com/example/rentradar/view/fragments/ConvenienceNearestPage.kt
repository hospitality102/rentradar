package com.example.rentradar.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.rentradar.R
import com.example.rentradar.utils.Global
import kotlinx.android.synthetic.main.page_result_obj_nearest.view.*

class ConvenienceNearestPage : Fragment(){

    var nearestList = arrayListOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_result_obj_nearest, parent,false)
    }

    override fun onResume() {
        setInfo()
        super.onResume()
    }

    fun setInfo(){
        val size = nearestList.size
        view?.tvBank?.text = if(size >= 1) nearestList[0].toString() else "N/A"
        view?.tvSchool?.text = if(size >= 2) nearestList[1].toString() else "N/A"
        view?.tvHospital?.text = if(size >= 3) nearestList[2].toString() else "N/A"
        view?.tvSupermarket?.text = if(size >= 4) nearestList[3].toString() else "N/A"
        view?.tvStore?.text = if(size >= 5) nearestList[4].toString() else "N/A"
        view?.tvFood?.text = if(size >= 6) nearestList[5].toString() else "N/A"
        view?.tvTraffic?.text = if(size >= 7) nearestList[6].toString() else "N/A"
    }

//    companion object{
//        fun newInstance(lifeNearestList:ArrayList<Int>):Fragment{
//            return ConvenienceNearestPage().apply {
//                arguments = bundleOf(
//                    Global.BundleKey.LIFE_NEAREST to lifeNearestList
//                )
//            }
//        }
//    }



}