package com.example.distancetrackerapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetrackerapp.ui.maps.MapsUtil
import com.example.distancetrackerapp.ui.maps.MapsUtil.calculateDistance
import com.example.distancetrackerapp.util.Constants.ACTION_SERVICE_START
import com.example.distancetrackerapp.util.Constants.ACTION_SERVICE_STOP
import com.example.distancetrackerapp.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetrackerapp.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService : LifecycleService() {

    private val TAG = "TrackerService"

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val started = MutableLiveData<Boolean>()
        val locationList = MutableLiveData<MutableList<LatLng>>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()
    }

    private fun setInitialValue() {
        started.postValue(true)
        locationList.postValue(mutableListOf())
        startTime.postValue(0L)
        stopTime.postValue(0L)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result!!)
            result.locations.let { locations ->
                for (location in locations) {
                    updateLocationList(location)
                    updateNotificationPeriodically()
                }
            }
        }
    }

    private fun updateLocationList(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        locationList.value?.apply {
            add(newLatLng)
            locationList.postValue(this)
        }
    }


    override fun onCreate() {
        setInitialValue()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SERVICE_START -> {
                    started.postValue(true)
                    startForegroundService()
                    startLocationServicesUpdate()
                }

                ACTION_SERVICE_STOP -> {
                    started.postValue(true)
                    stopForegroundService()
                }

                else -> {
                }

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    @SuppressLint("MissingPermission")
    private fun startLocationServicesUpdate() {

        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        startTime.postValue(System.currentTimeMillis())
    }

    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())

    }

    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
            setContentText(locationList.value?.let { calculateDistance(it) } + "Km")
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun stopForegroundService() {
        removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

    }

    private fun createNotificationChannel() {
        /** Create the NotificationChannel, but only on API 26+ because
         * the NotificationChannel class is new and not in the support library
         * if not android will ignore it
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


}