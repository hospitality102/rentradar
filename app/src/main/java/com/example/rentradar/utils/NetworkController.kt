package com.example.rentradar.utils

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class NetworkController {

    class CallbackAdapter(
        private val onFailure: ((errorCode: Int, msg: String) -> Unit)?,
        private val onResponse: ((res: String) -> Unit)?,
        private val onComplete: (() -> Unit)?
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure?.invoke(500, e.message!!)
            onComplete?.invoke()
        }

        override fun onResponse(call: Call, response: Response) {
            val res = response.body?.string() ?: ""
            onResponse?.invoke(res)
            onComplete?.invoke()
        }
    }

    val client = OkHttpClient()

    inner class CallbackMiddle(private val request: Request) {
        var onFailure: ((errorCode: Int, msg: String) -> Unit)? = null
        var onResponse: ((res: String) -> Unit)? = null
        var onComplete: (() -> Unit)? = null

        fun exec(): Call {
            val call = client.newCall(request)
            call.enqueue(CallbackAdapter(onFailure, onResponse, onComplete))
            return call
        }
    
    }

    // GoogleMap API 地址轉經緯度
    fun getLocationByName(address: String): CallbackMiddle {
        val request = Request.Builder()
            .url("$MAP_URL_ROOT/geocode/json?address=$address&key=${Global.GOOGLE_MAP_API_KEY}&language=zh-TW&components=country:TW")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getLocationByLatlng(lat: Double, lng: Double): CallbackMiddle {
        val request = Request.Builder()
            .url("$MAP_URL_ROOT/geocode/json?latlng=$lat,$lng&key=${Global.GOOGLE_MAP_API_KEY}&language=zh-TW")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getMrtList(lat: Double, lng: Double, range:Int):CallbackMiddle{
        val request = Request.Builder()
            .url("$MAP_URL_ROOT/place/nearbysearch/json?location=$lat,$lng&radius=$range&language=zh-TW&key=${Global.GOOGLE_MAP_API_KEY}&components=country:TW&types=subway_station")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getNextMrtList(pageToken: String):CallbackMiddle{
        val request = Request.Builder()
            .url("$MAP_URL_ROOT/place/nearbysearch/json?pagetoken=$pageToken&language=zh-TW&key=${Global.GOOGLE_MAP_API_KEY}")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun checkUser(userId: String, registerId: String): CallbackMiddle {
        val obj = JSONObject()
        obj.put("userId", userId)
        obj.put("registerId", registerId)

        val requestBody: RequestBody =
            obj.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$APP_URL_ROOT/user/check")
            .post(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun getRadarList(userId: String): CallbackMiddle {
        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/list/?userId=$userId")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getRegion():CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/info/getRegion")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun renameRadar(userId: String,radarId:Int,radarName:String): CallbackMiddle{
        val obj = JSONObject()
        obj.put("userId", userId)
        obj.put("radarId", radarId)
        obj.put("radarName",radarName)

        val requestBody: RequestBody =
            obj.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/rename")
            .patch(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun deleteRadar(userId: String,radarId:Int): CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/delete/?userId=$userId&radarId=$radarId")
            .delete()
            .build()
        return CallbackMiddle(request)

    }

    fun setPushRadar(userId:String,radarId: Int,canPush:Boolean): CallbackMiddle{
        val obj = JSONObject()
        obj.put("userId", userId)
        obj.put("radarId", radarId)
        obj.put("canPush",canPush)

        val requestBody: RequestBody =
            obj.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/setPush")
            .patch(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun getMrt():CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/info/getMrt")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun addRadar(jsonObject: JSONObject):CallbackMiddle{
        val requestBody = jsonObject.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/add")
            .post(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun updateRadar(jsonObject: JSONObject):CallbackMiddle{
        val requestBody = jsonObject.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("$APP_URL_ROOT/radar/update")
            .patch(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun addToFavorite(userId:String,rentalSerialNum:Int):CallbackMiddle{

        val obj = JSONObject()
        obj.put("userId", userId)
        obj.put("rentalSerialNum", rentalSerialNum)
        println(obj.toString())
        val requestBody: RequestBody =
            obj.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$APP_URL_ROOT/favorite/add")
            .post(requestBody)
            .build()
        return CallbackMiddle(request)

    }

    fun deleteFromFavorite(userId: String,rentalSerialNum:Int): CallbackMiddle {
        val request = Request.Builder()
            .url("$APP_URL_ROOT/favorite/delete/?userId=$userId&rentalSerialNum=$rentalSerialNum")
            .delete()
            .build()
        return CallbackMiddle(request)
    }

    fun getRentalInfo(userID: String, rentalSerialNumber: Int): CallbackMiddle {
        val request = Request.Builder()
            .url("$APP_URL_ROOT/rental/info/?userId=$userID&rentalSerialNum=$rentalSerialNumber")
            .get()
            .build()
        return CallbackMiddle(request)

    }

    fun getFavoritelistFirstPage(userId: String,quantity:Int,sortBy: Int=3,sortDirection: Int=2): CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/favorite/listFirstPage/?userId=$userId&quantity=$quantity&sortBy=$sortBy&sortDirection=$sortDirection")
            .get()
            .build()
        println("$APP_URL_ROOT/favorite/listFirstPage/?userId=$userId&quantity=$quantity&sortBy=$sortBy&sortDirection=$sortDirection")
        return CallbackMiddle(request)
    }

    fun getFavoritelist(userId: String,page:Int):CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/favorite/list/?userId=$userId&page=$page")
            .get()
            .build()
        println("$APP_URL_ROOT/favorite/list/?userId=$userId&page=$page")
        return CallbackMiddle(request)
    }

    fun getCompareInfo(rentalSerialNum: Int): CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/favorite/compareinfo/?rentalSerialNum=$rentalSerialNum")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getRentalListFirstPage(userId: String,radarId: Int,quantity:Int,otherConditions:JSONObject= JSONObject(),sortBy:Int=3,sortDirection:Int=2): CallbackMiddle{

        val requestBody: RequestBody =
            otherConditions.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$APP_URL_ROOT/rental/listFirstPage/?userId=$userId&radarId=$radarId&quantity=$quantity&sortBy=$sortBy&sortDirection=$sortDirection")
            .post(requestBody)
            .build()
        println("$APP_URL_ROOT/rental/listFirstPage/?userId=$userId&radarId=$radarId&quantity=$quantity&sortBy=$sortBy&sortDirection=$sortDirection")
        println(otherConditions.toString())
        return CallbackMiddle(request)
    }

    fun  getRentalList(userId: String,radarId: Int,page:Int,otherConditions:JSONObject= JSONObject()):CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/rental/list/?userId=$userId&radarId=$radarId&page=$page")
            .get()
            .build()
        println("$APP_URL_ROOT/rental/list/?userId=$userId&radarId=$radarId&page=$page")
        return CallbackMiddle(request)
    }

    fun addComment(jsonObject: JSONObject):CallbackMiddle{
        val requestBody = jsonObject.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("$APP_URL_ROOT/comment/add")
            .post(requestBody)
            .build()
        return CallbackMiddle(request)
    }

    fun getCommentFirstPage(userId: String, rentalSerialNum:Int, quantity:Int):CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/comment/listFirstPage/?userId=$userId&rentalSerialNum=$rentalSerialNum&quantity=$quantity")
            .get()
            .build()
        return CallbackMiddle(request)
    }

    fun getCommentList(userId: String, rentalSerialNum: Int, page:Int):CallbackMiddle{
        val request = Request.Builder()
            .url("$APP_URL_ROOT/comment/list/?userId=$userId&rentalSerialNum=$rentalSerialNum&page=$page")
            .get()
            .build()
        Log.d("getCommentList",request.url.toString())
        return CallbackMiddle(request)
    }

    companion object {
        val instance: NetworkController by lazy { NetworkController() }
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        const val MAP_URL_ROOT = "https://maps.googleapis.com/maps/api"
        const val APP_URL_ROOT = "http://34.80.82.210:5000"
    }

}