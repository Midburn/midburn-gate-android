package com.midburn.gate.midburngate.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import com.midburn.gate.midburngate.R
import kotlinx.android.synthetic.main.cars_dialog.*

class CarsDialog(context: Context, private val mOnCarEnterListener: View.OnClickListener?, private val mOnCarExitListener: View.OnClickListener?) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.cars_dialog)
        if (mOnCarEnterListener != null) {
            carEnter_button.setOnClickListener(mOnCarEnterListener)
        }
        if (mOnCarExitListener != null) {
            carExit_button.setOnClickListener(mOnCarExitListener)
        }
    }
}
