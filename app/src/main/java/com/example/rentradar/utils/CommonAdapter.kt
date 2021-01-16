package com.example.rentradar.utils

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CommonAdapter(private var items: MutableList<IType>) :
    RecyclerView.Adapter<BaseViewHolder<out IType>>() {

    companion object {
        // 無須被知道的鍵值 僅用做內部Mapping處理
        private const val TYPE_HEADER = -100
        private const val TYPE_FOOTER = -110
    }
    // 利用工廠降低耦合, 避免直接傳入實體的mapping方式，存放工廠list
    private val factories: MutableList<ViewHolderBuilder> = mutableListOf()

    private val modelsViewMap: HashMap<Int, Int> = hashMapOf()

    // mapping item與viewholder
    private var header: IType? = null
    private var footer: IType? = null

    fun addType(itemType: Int, factor: ViewHolderBuilder) {
        factories.add(factor)
        modelsViewMap[itemType] = factories.size - 1
    }

    fun addHeader(factor: ViewHolderBuilder) {
        modelsViewMap[TYPE_HEADER] ?: let {
            factories.add(factor)
            modelsViewMap[TYPE_HEADER] = factories.size - 1
            header = object : IType {
                override val getItemType: Int
                    get() = TYPE_HEADER
            }
        }
    }

    fun addFooter(factor: ViewHolderBuilder) {
        modelsViewMap[TYPE_FOOTER] ?: let {
            factories.add(factor)
            modelsViewMap[TYPE_FOOTER] = factories.size - 1
            footer = object : IType {
                override val getItemType: Int
                    get() = TYPE_FOOTER
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<out IType> {
        return factories[viewType].invoke(parent)
    }

    //動態載入用
    var onGetItemViewType: ((position: Int) -> Unit)? = null
    override fun getItemViewType(position: Int): Int {
        onGetItemViewType?.invoke(position)
        header?.let {
            if (position == 0) {
                return modelsViewMap[TYPE_HEADER] ?: error("Header Type Not Found!")
            }
        }
        footer?.let {
            if (position == itemCount - 1) {
                return modelsViewMap[TYPE_FOOTER] ?: error("Footer Type Not Found!")
            }
        }
        return modelsViewMap[items[position - (if (header == null) {
            0
        } else {
            1
        })].getItemType]
            ?: error(
                "${items[position - (if (header != null) {
                    0
                } else {
                    1
                })].getItemType} Type Not Found!"
            )
    }

    override fun getItemCount(): Int =
        items.size + (if (header != null) {1} else { 0 }) + if (footer != null) { 1 } else { 0 }

    @SuppressWarnings("unchecked")
    override fun onBindViewHolder(holder: BaseViewHolder<out IType>, position: Int) {
        header?.let {
            if (position == 0) {
                (holder as BaseViewHolder<IType>).bind(header!!)
                return
            }
        }
        footer?.let {
            if (position == itemCount - 1) {
                (holder as BaseViewHolder<IType>).bind(footer!!)
                return
            }
        }
        // 隱含轉換
        (holder as BaseViewHolder<IType>).bind(
            items[position - (if (header == null) {
                0
            } else {
                1
            })]
        )
    }

    override fun onViewRecycled(holder: BaseViewHolder<out IType>) {
        holder.onViewRecycled()
    }

    /* data binding */
    fun bind(items: MutableList<IType>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun add(item: IType?) {
        item ?: return
        this.items.add(item)
        notifyItemInserted(
            itemCount - 1 - if (footer != null) { 1 } else { 0 }
        )
    }

    fun add(items: List<IType>?) {
        items ?: return
        this.items.addAll(items)
        notifyItemRangeInserted(
            itemCount - items.size - if (footer != null) { 1 } else { 0 }, items.size
        )
    }

    fun remove(position: Int) {
        this.items.removeAt(position)
        notifyItemRemoved(position + if (header != null) 1 else 0)
    }

    fun clear() {
        val count = itemCount
        this.items.clear()
        notifyItemRangeRemoved(0, count)
    }


}