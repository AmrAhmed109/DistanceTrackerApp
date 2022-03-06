package com.example.distancetrackerapp.ui.result

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.databinding.FragmentResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ResultFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultBinding.inflate(inflater, container, false)

        binding.distanceValueTextView.text = getString(R.string.distance, args.result.distance)
        binding.timeValueTextView.apply {
            text = args.result.time
        }
        binding.shareButton.setOnClickListener {

            shareResult()
        }
        return binding.root
    }

    private fun shareResult() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "I went ${args.result.distance} in ${args.result.time}")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}