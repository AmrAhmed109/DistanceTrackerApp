package com.example.distancetrackerapp.ui.maps

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.model.Result
import com.example.distancetrackerapp.service.TrackerService
import com.example.distancetrackerapp.ui.maps.MapsUtil.calculateDistance
import com.example.distancetrackerapp.ui.maps.MapsUtil.calculateElapsedTime
import com.example.distancetrackerapp.ui.maps.MapsUtil.setCameraPosition
import com.example.distancetrackerapp.util.Constants.ACTION_SERVICE_START
import com.example.distancetrackerapp.util.Constants.ACTION_SERVICE_STOP
import com.example.distancetrackerapp.util.ExtentsionFunctions.disable
import com.example.distancetrackerapp.util.ExtentsionFunctions.enable
import com.example.distancetrackerapp.util.ExtentsionFunctions.hide
import com.example.distancetrackerapp.util.ExtentsionFunctions.show
import com.example.distancetrackerapp.util.Permissions.hasBackgroundLocation
import com.example.distancetrackerapp.util.Permissions.requestBackgroundPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks, GoogleMap.OnMarkerClickListener {
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private var locationList = mutableListOf<LatLng>()
    private val TAG = "MapsFragment"
    var started = MutableLiveData<Boolean>()
    private var startTime = 0L
    private var stopTime = 0L
    private var polylinelist = mutableListOf<Polyline>()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val markers = mutableListOf<Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetButton.setOnClickListener {
            resetMap()
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())


        return binding.root
    }

    private fun alertDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("You have to enable GPS to get you location ")
            .setPositiveButton(
                "OK"
            ) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(
                "Cancel"
            ) { _, _ ->
                // User cancelled the dialog
            }
        // Create the AlertDialog object and return it
        builder.create()
        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun resetMap() {

        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            val lastLocation = LatLng(it.latitude, it.longitude)
            for (polyline in polylinelist) {
                polyline.remove()
            }
            polylinelist.clear()
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(setCameraPosition(lastLocation)),
                1000,
                null
            )
        }



        locationList.clear()
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
        binding.resetButton.hide()
        binding.startButton.show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        map.setOnMarkerClickListener(this)
        map.setOnMyLocationButtonClickListener(this)
        map.isMyLocationEnabled = true
        map.uiSettings.apply {
            isZoomGesturesEnabled = false
            isZoomControlsEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = true
            isCompassEnabled = false
            isScrollGesturesEnabled = true
        }
        observeLocationList()

    }

    private fun drawPolyline(locationLists: MutableList<LatLng>) {
        val polyline = map.addPolyline(PolylineOptions().apply {
            startCap(ButtCap())
            endCap(ButtCap())
            jointType(JointType.ROUND)
            color(ContextCompat.getColor(requireContext(), R.color.polyline))
            width(15f)
            addAll(locationLists)
        })

        polylinelist.add(polyline)
    }

    private fun followPolyline(locationLists: MutableList<LatLng>) {
        if (locationLists.isNotEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(setCameraPosition(locationLists.last())),
                1000,
                null
            )
        }
    }

    private fun showBigPicture() {
        val bound = LatLngBounds.builder()
        for (location in locationList) {
            bound.include(location)
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bound.build(), 100), 2000, null)
        addMarker(
            locationList.first(),
            bitmapDescriptorFromVector(requireContext(), R.drawable.ic_end)
        )
        addMarker(
            locationList.last(),
            bitmapDescriptorFromVector(requireContext(), R.drawable.ic_person_svg)
        )

    }

    private fun addMarker(position: LatLng, icon: BitmapDescriptor?) {
        val marker = map.addMarker(
            MarkerOptions().position(position)
                .icon(icon)
        )
        markers.add(marker)
    }

    private fun observeLocationList() {
        view?.let {
            TrackerService.locationList.observe(viewLifecycleOwner, {
                if (it != null) {
                    locationList = it
                    if (it.size > 1) {
                        binding.stopButton.enable()
                    }
                    drawPolyline(locationList)
                    followPolyline(locationList)
                    Log.v(TAG, locationList.toString())
                }
            })

            TrackerService.started.observe(viewLifecycleOwner, {
                started.value = it
            })

            TrackerService.startTime.observe(viewLifecycleOwner, {
                startTime = it
            })
            TrackerService.stopTime.observe(viewLifecycleOwner, {
                stopTime = it
                if (it > 1) {
                    showBigPicture()
                    displayResult()

                }
            })
        }

    }

    private fun displayResult() {
        val result =
            Result(calculateDistance(locationList), calculateElapsedTime(startTime, stopTime))
        lifecycleScope.launch {
            delay(2000L)
            val direction = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(direction)
            binding.startButton.apply {
                hide()
                enable()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }

    }


    private fun onStartButtonClicked() {
        if (hasBackgroundLocation(requireContext())) {
            startCountDown()
            binding.startButton.hide()
            binding.startButton.disable()
            binding.stopButton.show()

        } else {
            requestBackgroundPermission(this)
        }


    }

    private fun onStopButtonClicked() {
        stopForgroundService()
        binding.stopButton.hide()
        binding.startButton.show()
    }


    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    fun sendActionCommandToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun stopForgroundService() {
        sendActionCommandToService(ACTION_SERVICE_STOP)
        binding.startButton.disable()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
//         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireContext()).build().show()
        }
//        else {
//            requestBackgroundPermission(this)
//        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onMyLocationButtonClick(): Boolean {


//        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            startActivity(intent)
//            findNavController().navigate(R.id.action_mapsFragment_to_permissionFragment)
//            return false
//        }

        val manager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            alertDialog()
        }

        lifecycleScope.launchWhenStarted {
            binding.hintText.animate().alpha(0f).duration = 1500
            delay(2000L)
            binding.hintText.hide()
            binding.startButton.show()
        }

        return false
    }

    private fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorDrawableResourceId: Int
    ): BitmapDescriptor? {

        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }
}