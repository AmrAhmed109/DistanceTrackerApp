package com.example.distancetrackerapp.bindingAdapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter

class MapsBindingAdapter {
    companion object{
        @BindingAdapter("observeTracking")
        @JvmStatic
        fun View.observeTracking(
            started : Boolean
        ){
            if (started && this is Button){
                this.visibility = View.VISIBLE
            }else if (started && this is TextView){
                this.visibility = View.INVISIBLE
            }
        }
    }
}