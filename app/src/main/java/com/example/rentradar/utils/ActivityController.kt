package com.example.rentradar.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle

class ActivityController {


    fun startActivity(context: Activity, clazz: Class<out Activity>){
        val intent = Intent(context, clazz)
        context.startActivity(intent,ActivityOptions.makeSceneTransitionAnimation(context). toBundle())
    }

    fun startActivity(context: Activity, clazz: Class<out Activity>, bundle: Bundle){
        val intent = Intent(context, clazz)
        intent.putExtras(bundle)
        context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(context). toBundle())
    }

    fun startActivityCustomAnimation(context: Activity, clazz: Class<out Activity>){
        val intent = Intent(context, clazz)
        context.startActivity(intent)
    }

    fun startActivityCustomAnimation(context: Activity, clazz: Class<out Activity>, bundle:Bundle){
        val intent = Intent(context, clazz)
        intent.putExtras(bundle)
        context.startActivity(intent)
    }

    companion object{
        val instance : ActivityController by lazy{ ActivityController() }
    }
}