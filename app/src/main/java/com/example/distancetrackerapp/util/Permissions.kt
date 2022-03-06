package com.example.distancetrackerapp.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.distancetrackerapp.util.Constants.PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE
import com.example.distancetrackerapp.util.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.example.distancetrackerapp.util.Constants.PERMISSION_foreground_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {


    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)


    fun requestLocationPermission(fragment: Fragment) {
        EasyPermissions.requestPermissions(
            fragment, "This Application Can't work without location Permission",
            PERMISSION_LOCATION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION
        )
    }



    fun hasBackgroundLocation(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.v("MapCheck", "Here")
            return EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

        }
        return true
    }

    fun requestBackgroundPermission(fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return EasyPermissions.requestPermissions(
                fragment,
                "Background Location Permission is essential",
                PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    }
}