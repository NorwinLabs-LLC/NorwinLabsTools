package com.example.norwinlabstools

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val aiManager = SecurityAIManager()

    data class ScannedDevice(
        val ip: String,
        val hostname: String = "Unknown",
        var status: String = "Scanning...",
        var vulnerabilities: MutableList<String> = mutableListOf(),
        var aiAnalysis: String? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAdapter = DeviceAdapter(deviceList)
        binding.rvDevices.layoutManager = LinearLayoutManager(context)
        binding.rvDevices.adapter = deviceAdapter

        binding.btnScan.setOnClickListener {
            startNetworkScan()
        }
    }

    private fun startNetworkScan() {
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        binding.scanProgress.visibility = View.VISIBLE
        binding.scanProgress.progress = 0
        binding.btnScan.isEnabled = false

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
                            // Simulate vulnerability check for open common ports
                            checkVulnerabilities(device)
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
                    binding.tvNetworkInfo.text = "Scan complete. Found ${deviceList.size} devices."
                }
            }
        }.start()
    }

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

    private fun checkVulnerabilities(device: ScannedDevice) {
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
            lifecycleScope.launch {
                aiManager.analyzeVulnerabilities(device.ip, openPortsList, object : SecurityAIManager.SecurityCallback {
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

        activity?.runOnUiThread {
            deviceAdapter.notifyDataSetChanged()
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
            val tvIp: android.widget.TextView = view.findViewById(android.R.id.text1)
            val tvStatus: android.widget.TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.tvIp.text = device.ip
            
            val statusText = StringBuilder()
            statusText.append(device.status)
            if (device.vulnerabilities.isNotEmpty()) {
                statusText.append("\nPorts: ").append(device.vulnerabilities.joinToString(", "))
            }
            if (device.aiAnalysis != null) {
                statusText.append("\n\nAI Analysis: ").append(device.aiAnalysis)
            }
            
            holder.tvStatus.text = statusText.toString()
            
            val color = if (device.vulnerabilities.isNotEmpty()) 0xFFFF5252.toInt() else 0xFF4CAF50.toInt()
            holder.tvStatus.setTextColor(color)
        }

        override fun getItemCount() = devices.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}