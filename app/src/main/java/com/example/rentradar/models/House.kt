package com.example.rentradar.models

import android.os.Parcel
import android.os.Parcelable
import com.example.rentradar.utils.Global
import com.example.rentradar.utils.IType

class House(
    val serialNum:Int,
    val source: String?,
    val title: String?,
    var systemUpdateTime:Long,
    val address: String?,
    val lat:Double,
    val lng:Double,
    val ImgUrl: String?,
    val price: Int,
    val typeOfRoom: Int,
    val ping:Int,
    var isFavorite:Boolean,
    var isOnShelf:Boolean

) : IType, Parcelable {
    override val getItemType: Int
        get() = Global.ItemType.HOUSE

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(serialNum)
        parcel.writeString(source)
        parcel.writeString(title)
        parcel.writeLong(systemUpdateTime)
        parcel.writeString(address)
        parcel.writeDouble(lat)
        parcel.writeDouble(lng)
        parcel.writeString(ImgUrl)
        parcel.writeInt(price)
        parcel.writeInt(typeOfRoom)
        parcel.writeInt(ping)
        parcel.writeByte(if (isFavorite) 1 else 0)
        parcel.writeByte(if (isOnShelf) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<House> {
        override fun createFromParcel(parcel: Parcel): House {
            return House(parcel)
        }

        override fun newArray(size: Int): Array<House?> {
            return arrayOfNulls(size)
        }
    }
}