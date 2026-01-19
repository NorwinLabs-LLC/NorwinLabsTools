package com.example.norwinlabstools

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.norwinlabstools.databinding.FragmentHapticTesterBinding

class HapticTesterFragment : Fragment() {

    private var _binding: FragmentHapticTesterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHapticTesterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnHapticConfirm.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }

        binding.btnHapticReject.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }

        binding.btnHapticClock.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        binding.btnHapticLong.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        binding.btnHapticContext.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }

        binding.btnHapticGesture.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}