package com.example.distancetrackerapp.util

import android.view.View
import android.widget.Button

object ExtentsionFunctions {

    fun View.hide(){
        this.visibility = View.INVISIBLE
    }

    fun View.show(){
        this.visibility = View.VISIBLE
    }

    fun Button.enable(){
        this.isEnabled = true
    }
    fun Button.disable(){
        this.isEnabled = false
    }
}