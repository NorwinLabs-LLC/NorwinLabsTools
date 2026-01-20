package com.example.norwinlabstools

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.norwinlabstools.databinding.FragmentFirstBinding
import com.example.norwinlabstools.databinding.LayoutAddToolsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ToolsAdapter

    private val PREFS_NAME = "norwin_prefs"
    private val KEY_HOME_TOOLS = "home_tools_ids"

    private val allTools = listOf(
        Tool(1, "Calculator", android.R.drawable.ic_menu_agenda),
        Tool(2, "Converter", android.R.drawable.ic_menu_compass),
        Tool(3, "Notes", android.R.drawable.ic_menu_edit),
        Tool(4, "Settings", android.R.drawable.ic_menu_manage),
        Tool(5, "About", android.R.drawable.ic_menu_info_details),
        Tool(6, "Weather", android.R.drawable.ic_menu_day),
        Tool(7, "Calendar", android.R.drawable.ic_menu_today),
        Tool(8, "Maps", android.R.drawable.ic_menu_mylocation),
        Tool(9, "Haptic Tester", android.R.drawable.ic_menu_send),
        Tool(10, "Color Picker", android.R.drawable.ic_menu_gallery),
        Tool(11, "Dice Roller", android.R.drawable.ic_menu_help),
        Tool(12, "Update", android.R.drawable.ic_menu_upload)
    )

    private var currentTools = mutableListOf<Tool>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadHomeTools()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ToolsAdapter(
            currentTools,
            onToolClick = { tool ->
                if (adapter.isEditMode) {
                    adapter.isEditMode = false
                } else {
                    navigateToTool(tool.id)
                }
            },
            onToolLongClick = { toolView, _ ->
                if (!adapter.isEditMode) {
                    toolView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    adapter.isEditMode = true
                }
            },
            onRemoveClick = { tool ->
                adapter.removeTool(tool)
                saveHomeTools()
            }
        )

        binding.recyclerviewTools.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerviewTools.adapter = adapter

        setupDragAndDrop()

        val fab = activity?.findViewById<View>(R.id.fab)
        fab?.setOnClickListener {
            adapter.isEditMode = false
            showAddToolsBottomSheet()
        }

        binding.layoutContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && adapter.isEditMode) {
                adapter.isEditMode = false
                true
            } else false
        }
        
        binding.scrollviewFirst.setOnTouchListener { _, event ->
             if (event.action == MotionEvent.ACTION_DOWN && adapter.isEditMode) {
                adapter.isEditMode = false
                true
            } else false
        }

        binding.cardWidgetUpdates.setOnClickListener {
            checkForUpdates()
        }

        setupFooter()
        autoCheckForUpdates()
        checkIntentAction()
    }

    fun filterTools(query: String) {
        adapter.filter(query)
    }

    private fun navigateToTool(id: Int) {
        when(id) {
            4 -> findNavController().navigate(R.id.action_FirstFragment_to_SettingsFragment)
            1 -> findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            12 -> checkForUpdates()
            else -> {
                val tool = allTools.find { it.id == id }
                AlertDialog.Builder(requireContext())
                    .setTitle(tool?.name ?: "Tool")
                    .setMessage("${tool?.name ?: "This"} module is coming soon!")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun checkIntentAction() {
        val intent = activity?.intent
        val action = intent?.action ?: return
        
        when {
            action == "OPEN_ADD_TOOLS_ACTION" -> showAddToolsBottomSheet()
            action.startsWith("LAUNCH_TOOL_") -> {
                val toolId = action.removePrefix("LAUNCH_TOOL_").toIntOrNull()
                if (toolId != null) {
                    navigateToTool(toolId)
                }
            }
        }
        intent.action = null
    }

    override fun onResume() {
        super.onResume()
        checkIntentAction()
    }

    private fun autoCheckForUpdates() {
        val updateManager = UpdateManager(requireContext())
        updateManager.checkForUpdates(object : UpdateManager.UpdateCallback {
            override fun onUpdateAvailable(latestVersion: String, downloadUrl: String) {
                activity?.runOnUiThread {
                    binding.textviewUpdateStatus.text = "New version: $latestVersion"
                    binding.textviewUpdateStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
            }

            override fun onNoUpdate() {
                activity?.runOnUiThread {
                    binding.textviewUpdateStatus.text = "Up to date"
                }
            }

            override fun onError(error: String, url: String) {
                activity?.runOnUiThread {
                    binding.textviewUpdateStatus.text = "Check failed"
                }
            }
        })
    }

    private fun checkForUpdates() {
        val updateManager = UpdateManager(requireContext())
        Toast.makeText(requireContext(), "Checking for updates...", Toast.LENGTH_SHORT).show()
        
        updateManager.checkForUpdates(object : UpdateManager.UpdateCallback {
            override fun onUpdateAvailable(latestVersion: String, downloadUrl: String) {
                activity?.runOnUiThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Update Available")
                        .setMessage("A new version ($latestVersion) is available. Would you like to download it?")
                        .setPositiveButton("Download") { _, _ ->
                            if (downloadUrl.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                startActivity(intent)
                            } else {
                                Toast.makeText(requireContext(), "Download URL not found in release", Toast.LENGTH_LONG).show()
                            }
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            }

            override fun onNoUpdate() {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "You are on the latest version", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(error: String, url: String) {
                activity?.runOnUiThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Update Failed")
                        .setMessage("$error\n\nChecked URL:\n$url")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        })
    }

    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                saveHomeTools()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

            override fun isLongPressDragEnabled(): Boolean = adapter.isEditMode
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewTools)
    }

    private fun showAddToolsBottomSheet() {
        val currentToolIds = adapter.getItems().map { it.id }.toSet()
        val availableToAdd = allTools.filter { it.id !in currentToolIds }.toMutableList()

        if (availableToAdd.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Tools Available")
                .setMessage("All tools are already on your home screen.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = LayoutAddToolsBinding.inflate(layoutInflater)
        
        val sheetAdapter = ToolsAdapter(
            availableToAdd,
            onToolClick = { tool ->
                adapter.addTool(tool)
                saveHomeTools()
                dialog.dismiss()
            },
            onToolLongClick = { _, _ -> },
            onRemoveClick = {}
        )

        bottomSheetBinding.recyclerviewAvailableTools.layoutManager = GridLayoutManager(context, 3)
        bottomSheetBinding.recyclerviewAvailableTools.adapter = sheetAdapter

        dialog.setContentView(bottomSheetBinding.root)
        dialog.show()
    }

    private fun saveHomeTools() {
        val ids = adapter.getItems().joinToString(",") { it.id.toString() }
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOME_TOOLS, ids)
            .apply()
    }

    private fun loadHomeTools() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getString(KEY_HOME_TOOLS, null)
        
        currentTools.clear()
        if (savedIds != null) {
            val idList = savedIds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
            idList.forEach { id ->
                allTools.find { id == it.id }?.let { currentTools.add(it) }
            }
        } else {
            currentTools.addAll(allTools.take(4))
        }
    }

    private fun setupFooter() {
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.textviewVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            binding.textviewVersion.text = "Version 1.0"
        }

        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.textviewCopyright.text = "© $year NorwinLabs"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.fab)?.setOnClickListener(null)
        _binding = null
    }
}