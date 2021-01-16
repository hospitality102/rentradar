package com.example.rentradar.models

import com.example.rentradar.utils.Global
import com.example.rentradar.utils.IType

class ConditionItems {

    class TitleItem(val name: String) : IType {
        override val getItemType: Int
            get() = Global.ItemType.CONDITION_TITLE
    }

    class TurnItem(val name: String) : IType {
        override val getItemType: Int
            get() = Global.ItemType.CONDITION_TURN
    }

    class ChooseItem(val name: String) : IType {
        override val getItemType: Int
            get() = Global.ItemType.CONDITION_CHOOSE
    }
}