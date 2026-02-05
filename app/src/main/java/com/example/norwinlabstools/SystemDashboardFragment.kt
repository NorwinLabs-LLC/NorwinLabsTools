package com.example.norwinlabstools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.norwinlabstools.databinding.FragmentSystemDashboardBinding
import java.util.Locale

class SystemDashboardFragment : Fragment() {

    private var _binding: FragmentSystemDashboardBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSystemDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        updateRunnable = object : Runnable {
            override fun run() {
                updateStats()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun updateStats() {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem / (1024 * 1024)
        val availMem = memoryInfo.availMem / (1024 * 1024)
        val usedMem = totalMem - availMem
        
        binding.progressRam.max = totalMem.toInt()
        binding.progressRam.progress = usedMem.toInt()
        binding.tvRamStatus.text = String.format(Locale.US, "RAM: %dMB / %dMB used", usedMem, totalMem)

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            requireContext().registerReceiver(null, ifilter)
        }
        
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()

        binding.progressBattery.progress = batteryPct.toInt()
        binding.tvBatteryStatus.text = String.format(Locale.US, "Battery: %.0f%% | Temp: %.1f°C", batteryPct, temp / 10.0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}