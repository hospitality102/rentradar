package com.example.rentradar.utils

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

//類型別名 讓類別可以有別名(用新的變數定義，但卻是類別），因此可以放function或資料，當作工廠建立
typealias ViewHolderBuilder = (parent: ViewGroup) -> BaseViewHolder<out IType>

abstract class BaseViewHolder<T : IType>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun bind(item: T)

    //如果有要在view被回收時做事的話，可以改寫這方法
    open fun onViewRecycled(){}

}