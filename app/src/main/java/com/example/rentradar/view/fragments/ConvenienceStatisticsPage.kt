package com.example.rentradar.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rentradar.R
import kotlinx.android.synthetic.main.page_result_obj_nearest.view.*

class ConvenienceStatisticsPage : Fragment(){
    
    var countList = arrayListOf<Int>()
    
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_result_obj_statistics, parent, false)
    }

    override fun onResume() {
        setInfo()
        super.onResume()
    }

    private fun setInfo(){
        val size = countList.size
        view?.tvBank?.text = if(size >= 1) countList[0].toString() else "N/A"
        view?.tvSchool?.text = if(size >= 2) countList[1].toString() else "N/A"
        view?.tvHospital?.text = if(size >= 3) countList[2].toString() else "N/A"
        view?.tvSupermarket?.text = if(size >= 4) countList[3].toString() else "N/A"
        view?.tvStore?.text = if(size >= 5) countList[4].toString() else "N/A"
        view?.tvFood?.text = if(size >= 6) countList[5].toString() else "N/A"
        view?.tvTraffic?.text = if(size >= 7) countList[6].toString() else "N/A"
    }

//    companion object{
//        fun newInstance(lifeCountList:ArrayList<Int>):Fragment{
//            return ConvenienceStatisticsPage().apply {
//                arguments = bundleOf(
//                    Global.BundleKey.LIFE_COUNT to lifeCountList
//                )
//            }
//        }
//    }
    
    
}