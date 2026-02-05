package com.example.norwinlabstools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.norwinlabstools.databinding.FragmentPortScannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class PortScannerFragment : Fragment() {

    private var _binding: FragmentPortScannerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartScan.setOnClickListener {
            val ip = binding.editIpAddress.text.toString()
            if (ip.isNotBlank()) {
                scanPorts(ip)
            } else {
                Toast.makeText(context, "Please enter an IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scanPorts(ip: String) {
        binding.tvResults.text = "Scanning common ports..."
        binding.progressBar.visibility = View.VISIBLE
        
        val commonPorts = listOf(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 8080)
        
        lifecycleScope.launch {
            val openPorts = mutableListOf<Int>()
            withContext(Dispatchers.IO) {
                commonPorts.forEach { port ->
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, port), 200)
                        socket.close()
                        openPorts.add(port)
                    } catch (e: Exception) {
                        // Port closed
                    }
                }
            }
            
            binding.progressBar.visibility = View.GONE
            if (openPorts.isEmpty()) {
                binding.tvResults.text = "No common ports open on $ip"
            } else {
                binding.tvResults.text = "Open Ports on $ip:\n" + openPorts.joinToString("\n") { "Port $it: OPEN" }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}