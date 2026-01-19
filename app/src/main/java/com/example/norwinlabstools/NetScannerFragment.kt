package com.example.norwinlabstools

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.norwinlabstools.databinding.FragmentNetScannerBinding
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class NetScannerFragment : Fragment() {

    private var _binding: FragmentNetScannerBinding? = null
    private val binding get() = _binding!!
    private val deviceList = mutableListOf<ScannedDevice>()
    private lateinit var deviceAdapter: DeviceAdapter
    
    private var aiManager: SecurityAIManager? = null
    private lateinit var wifiManager: WifiManager

    private val PREFS_NAME = "norwin_prefs"
    private val KEY_AI_ANALYSIS = "enable_ai_analysis"
    private val KEY_API_KEY = "gemini_api_key"

    data class ScannedDevice(
        val ip: String,
        val ssid: String? = null,
        var status: String = "Scanning...",
        var vulnerabilities: MutableList<String> = mutableListOf(),
        var aiAnalysis: String? = null,
        val isWifi: Boolean = false
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startScan()
        } else {
            Toast.makeText(context, "Location permission required for WiFi scan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetScannerBinding.inflate(inflater, container, false)
        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAdapter = DeviceAdapter(deviceList)
        binding.rvDevices.layoutManager = LinearLayoutManager(context)
        binding.rvDevices.adapter = deviceAdapter

        binding.btnScan.setOnClickListener {
            checkPermissionsAndStart()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            startScan()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startScan() {
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        binding.scanProgress.visibility = View.VISIBLE
        binding.scanProgress.progress = 0
        binding.btnScan.isEnabled = false
        binding.tvNetworkInfo.text = "Scanning Network & WiFi..."

        // 1. Scan WiFi Signals
        scanWifiSignals()

        // 2. Scan Local IP Devices
        scanLocalNetwork()
    }

    private fun scanWifiSignals() {
        val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val results = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    wifiManager.scanResults
                } else emptyList()
                
                results.forEach { result ->
                    val security = getWifiSecurity(result)
                    val device = ScannedDevice(
                        ip = result.BSSID,
                        ssid = result.SSID,
                        status = "WiFi Signal Found",
                        isWifi = true
                    )
                    
                    if (security == "Open") {
                        device.vulnerabilities.add("Security Risk: Open WiFi (No Password)")
                    }
                    
                    activity?.runOnUiThread {
                        deviceList.add(0, device) // Put WiFi at top
                        deviceAdapter.notifyItemInserted(0)
                        analyzeDeviceSecurity(device, listOf("WiFi Security: $security"))
                    }
                }
                requireContext().unregisterReceiver(this)
            }
        }
        
        requireContext().registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    private fun getWifiSecurity(result: ScanResult): String {
        val cap = result.capabilities
        return when {
            cap.contains("WPA3") -> "WPA3 (Secure)"
            cap.contains("WPA2") -> "WPA2"
            cap.contains("WPA") -> "WPA (Legacy)"
            else -> "Open"
        }
    }

    private fun scanLocalNetwork() {
        Thread {
            try {
                val subnet = getLocalSubnet()
                if (subnet != null) {
                    for (i in 1..254) {
                        val testIp = "$subnet.$i"
                        if (isIpReachable(testIp)) {
                            val device = ScannedDevice(testIp)
                            activity?.runOnUiThread {
                                deviceList.add(device)
                                deviceAdapter.notifyItemInserted(deviceList.size - 1)
                            }
                            checkPortVulnerabilities(device)
                        }
                        activity?.runOnUiThread {
                            binding.scanProgress.progress = (i * 100 / 254)
                        }
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.tvNetworkInfo.text = "Error: ${e.message}"
                }
            } finally {
                activity?.runOnUiThread {
                    binding.scanProgress.visibility = View.GONE
                    binding.btnScan.isEnabled = true
                    binding.tvNetworkInfo.text = "Scan complete. Found ${deviceList.size} items."
                }
            }
        }.start()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getLocalSubnet(): String? {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        val ipAddress = linkProperties?.linkAddresses?.find { it.address.isSiteLocalAddress }?.address?.hostAddress
        return ipAddress?.substringBeforeLast(".")
    }

    private fun isIpReachable(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isReachable(500)
        } catch (e: Exception) {
            false
        }
    }

    private fun checkPortVulnerabilities(device: ScannedDevice) {
        val ports = mapOf(
            21 to "FTP (Plaintext)",
            22 to "SSH",
            23 to "Telnet (Unsecure)",
            80 to "HTTP",
            443 to "HTTPS",
            445 to "SMB (Samba)",
            3389 to "RDP"
        )

        val openPortsList = mutableListOf<String>()

        ports.forEach { (port, service) ->
            if (isPortOpen(device.ip, port)) {
                device.vulnerabilities.add("Port $port ($service)")
                openPortsList.add("$port ($service)")
            }
        }
        
        device.status = if (device.vulnerabilities.isEmpty()) "Secure" else "Potential Issues Found"
        
        if (openPortsList.isNotEmpty()) {
            analyzeDeviceSecurity(device, openPortsList)
        }

        activity?.runOnUiThread {
            deviceAdapter.notifyDataSetChanged()
        }
    }

    private fun analyzeDeviceSecurity(device: ScannedDevice, findings: List<String>) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val aiEnabled = prefs.getBoolean(KEY_AI_ANALYSIS, true)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""

        if (aiEnabled && findings.isNotEmpty() && apiKey.isNotEmpty()) {
            if (aiManager == null) {
                aiManager = SecurityAIManager(apiKey)
            }
            
            lifecycleScope.launch {
                aiManager?.analyzeVulnerabilities(device.ip, findings, object : SecurityAIManager.SecurityCallback {
                    override fun onSuccess(analysis: String) {
                        device.aiAnalysis = analysis
                        activity?.runOnUiThread {
                            deviceAdapter.notifyDataSetChanged()
                        }
                    }
                    override fun onError(error: String) {
                        device.aiAnalysis = "AI Analysis Error: $error"
                        activity?.runOnUiThread {
                            deviceAdapter.notifyDataSetChanged()
                        }
                    }
                })
            }
        }
    }

    private fun isPortOpen(ip: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 200)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    class DeviceAdapter(private val devices: List<ScannedDevice>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvTitle: android.widget.TextView = view.findViewById(android.R.id.text1)
            val tvInfo: android.widget.TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            
            if (device.isWifi) {
                holder.tvTitle.text = "📶 WiFi: ${device.ssid ?: "Hidden Network"}"
                holder.tvTitle.setTextColor(0xFF2196F3.toInt()) // Blue for WiFi
            } else {
                holder.tvTitle.text = "💻 Device: ${device.ip}"
                holder.tvTitle.setTextColor(0xFF000000.toInt())
            }
            
            val infoText = StringBuilder()
            infoText.append(device.status)
            if (device.vulnerabilities.isNotEmpty()) {
                infoText.append("\nFindings: ").append(device.vulnerabilities.joinToString(", "))
            }
            if (device.aiAnalysis != null) {
                infoText.append("\n\nAI Security Report: ").append(device.aiAnalysis)
            }
            
            holder.tvInfo.text = infoText.toString()
            
            if (device.vulnerabilities.isNotEmpty()) {
                holder.tvInfo.setTextColor(0xFFFF5252.toInt()) // Red for risks
            } else {
                holder.tvInfo.setTextColor(0xFF757575.toInt()) // Gray for safe
            }
        }

        override fun getItemCount() = devices.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}