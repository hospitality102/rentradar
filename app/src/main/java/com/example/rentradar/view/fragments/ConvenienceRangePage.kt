package com.example.rentradar.view.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentradar.R
import com.example.rentradar.utils.BaseViewHolder
import com.example.rentradar.utils.CommonAdapter
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.IType
import kotlinx.android.synthetic.main.item_list_convenience_range.view.*
import kotlinx.android.synthetic.main.page_result_obj_range.*
import kotlinx.android.synthetic.main.page_result_obj_range.view.*
import java.lang.StringBuilder
import kotlin.random.Random

class ConvenienceRangePage : Fragment() {

    class ConvenienceItem(val name:String, val distance:Int) : IType{
        override val getItemType: Int
            get() = Global.ItemType.CONVENIENCE_ITEM
    }

    private val items = arrayListOf<IType>()
    private val commonAdapter = CommonAdapter(items)

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.page_result_obj_range, parent, false)

        val layoutManager = LinearLayoutManager(view.context)
        view.rvRangeItem.layoutManager = layoutManager

        //設定RecycleView
        with(commonAdapter){
            apply {
                addType(Global.ItemType.CONVENIENCE_ITEM){parent ->
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_list_convenience_range, parent, false)
                    object : BaseViewHolder<ConvenienceItem>(itemView){
                        @SuppressLint("SetTextI18n")
                        override fun bind(item: ConvenienceItem) {
                            itemView.tvAddress.text = item.name
                            itemView.tvDistance.text = item.distance.toString()
                        }
                    }
                }
            }
        }
        view.rvRangeItem.adapter = commonAdapter

        return view
    }

    fun setInfo(rangeList:MutableList<ConvenienceItem>){
        if(rangeList.size < 1){
            view?.tvError?.isVisible = true
        }else{
            view?.tvError?.isVisible = false
            items.clear()
            items.addAll(rangeList)
            commonAdapter.notifyDataSetChanged()
        }
    }

//    companion object{
//        fun newInstance( nameList:ArrayList<String> , distanceList:ArrayList<Int>):Fragment{
//            return ConvenienceStatisticsPage().apply {
//                arguments = bundleOf(
//                    Global.BundleKey.LIFE_RANGE_NAME to nameList,
//                    Global.BundleKey.LIFE_RANGE_DISTANCE to distanceList
//                )
//            }
//        }
//    }


}