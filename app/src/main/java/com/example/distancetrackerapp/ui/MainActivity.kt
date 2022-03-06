package com.example.distancetrackerapp.ui

import android.app.UiModeManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.util.Permissions.hasLocationPermission
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var uiModeManager : UiModeManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.navHostFragment)

        if (hasLocationPermission(this)) {
            navController.navigate(R.id.action_permissionFragment_to_mapsFragment)
        }

    }
}