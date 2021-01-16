package com.example.rentradar.models

class Radar(
    val id: Int,
    var name: String,
    var canPush: Boolean,
    var count: Int,
    var regionList: MutableList<Region>?,
    var mrtList: MutableList<MRT>?,
    var landmark: Landmark?,
    var commute: Commute?,
    var minPrice: Int,
    var maxPrice: Int,
    var typeOfRoom: Int,
    var conditionMap: HashMap<String, Int>,
    var oneChooseMap: HashMap<String, Boolean>
) {


}