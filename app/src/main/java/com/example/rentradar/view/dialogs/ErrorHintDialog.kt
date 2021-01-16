package com.example.rentradar.view.dialogs


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.rentradar.R
import kotlinx.android.synthetic.main.dialog_error_hint.view.*



class ErrorHintDialog(private val hint:String): DialogFragment() {
    var action:(() -> Unit)? = null
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =inflater.inflate(R.layout.dialog_error_hint, parent, false)
        isCancelable = false
        view.btnRetry.setOnClickListener {
            action?.invoke()
            this.dismiss()
        }
        view.tvErrorHint.text=hint

        return view
    }

}