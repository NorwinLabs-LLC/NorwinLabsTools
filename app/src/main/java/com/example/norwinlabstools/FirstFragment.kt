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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.norwinlabstools.databinding.FragmentFirstBinding
import com.example.norwinlabstools.databinding.LayoutAddToolsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Calendar

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ToolsAdapter
    private var aiManager: VideoIdeaManager? = null

    private val PREFS_NAME = "norwin_prefs"
    private val KEY_HOME_TOOLS = "home_tools_ids"
    private val KEY_BIOMETRIC = "enable_biometric"
    private val KEY_API_KEY = "gemini_api_key"

    private val allTools = listOf(
        Tool(1, "Calendar", android.R.drawable.ic_menu_today, "1.0.2", 0xFF2E7D32.toInt(), "https://images.unsplash.com/photo-1506784365847-bbad939e9335?q=80&w=500&auto=format&fit=crop"),
        Tool(2, "Converter", android.R.drawable.ic_menu_compass, "NR", 0xFF1565C0.toInt(), "https://images.unsplash.com/photo-1574634534894-89d7576c8259?q=80&w=500&auto=format&fit=crop"),
        Tool(3, "Notes", android.R.drawable.ic_menu_edit, "NR", 0xFFEF6C00.toInt(), "https://images.unsplash.com/photo-1517842645767-c639042777db?q=80&w=500&auto=format&fit=crop"),
        Tool(4, "Settings", android.R.drawable.ic_menu_manage, "1.0.1", 0xFF455A64.toInt(), "https://images.unsplash.com/photo-1581092160562-40aa08e78837?q=80&w=500&auto=format&fit=crop"),
        Tool(5, "About", android.R.drawable.ic_menu_info_details, "NR", 0xFF4527A0.toInt(), "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=500&auto=format&fit=crop"),
        Tool(9, "Idea Generator", R.drawable.ic_lightbulb, "1.0.1", 0xFFF9A825.toInt(), "https://images.unsplash.com/photo-1499750310107-5fef28a66643?q=80&w=500&auto=format&fit=crop"),
        Tool(12, "Update", android.R.drawable.ic_menu_upload, "1.0.1", 0xFFC62828.toInt(), "https://images.unsplash.com/photo-1633356122544-f134324a6cee?q=80&w=500&auto=format&fit=crop"),
        Tool(13, "Windhelm", android.R.drawable.ic_menu_view, "1.0.2", 0xFF283593.toInt(), "http://windhelmthegame.ddns.net/background.png"),
        Tool(15, "UE5 Guide", android.R.drawable.ic_menu_directions, "NR", 0xFF00695C.toInt(), "https://images.unsplash.com/photo-1542831371-29b0f74f9713?q=80&w=500&auto=format&fit=crop"),
        Tool(16, "Trello", android.R.drawable.ic_menu_agenda, "1.0.1", 0xFF0079BF.toInt(), "https://images.unsplash.com/photo-1522071820081-009f0129c71c?q=80&w=500&auto=format&fit=crop"),
        Tool(17, "SSH Client", android.R.drawable.ic_dialog_dialer, "NR", 0xFF37474F.toInt(), "https://images.unsplash.com/photo-1629654297299-c8506221ca97?q=80&w=500&auto=format&fit=crop"),
        Tool(18, "Ping Tool", android.R.drawable.ic_menu_revert, "NR", 0xFF0091EA.toInt(), "https://images.unsplash.com/photo-1558494949-ef010ca73324?q=80&w=500&auto=format&fit=crop"),
        Tool(20, "Net Scanner", android.R.drawable.ic_menu_share, "1.0.2", 0xFF546E7A.toInt(), "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?q=80&w=500&auto=format&fit=crop"),
        Tool(21, "Video Ideas", android.R.drawable.ic_menu_slideshow, "1.0.3", 0xFFE91E63.toInt(), "https://images.unsplash.com/photo-1492724441997-5dc865305da7?q=80&w=500&auto=format&fit=crop"),
        Tool(22, "Dev News", android.R.drawable.ic_menu_recent_history, "1.0.1", 0xFF2E7D32.toInt(), "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?q=80&w=500&auto=format&fit=crop"),
        Tool(23, "Bug Report", android.R.drawable.ic_menu_report_image, "1.0.0", 0xFFC62828.toInt(), "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=500&auto=format&fit=crop")
    )

    private var currentTools = mutableListOf<Tool>()

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (adapter.isEditMode) {
                adapter.isEditMode = false
                updateToolbar()
                isEnabled = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadHomeTools()
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
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
                    updateToolbar()
                } else {
                    when(tool.id) {
                        4 -> findNavController().navigate(R.id.action_FirstFragment_to_SettingsFragment)
                        1 -> findNavController().navigate(R.id.action_FirstFragment_to_CalendarFragment)
                        9 -> showIdeaGenerator()
                        12 -> checkForUpdates()
                        13 -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://windhelmthegame.ddns.net"))
                            startActivity(intent)
                        }
                        16 -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://trello.com/b/SVY6LFSZ/windhelm-main-development"))
                            startActivity(intent)
                        }
                        20 -> checkBiometricAndNavigate(R.id.action_FirstFragment_to_NetScannerFragment)
                        21 -> showVideoIdeaCategoryDialog()
                        22 -> findNavController().navigate(R.id.action_FirstFragment_to_DevNewsFragment)
                        23 -> findNavController().navigate(R.id.action_FirstFragment_to_BugReportFragment)
                        else -> {
                             AlertDialog.Builder(requireContext())
                                .setTitle(tool.name)
                                .setMessage("${tool.name} module is coming soon!")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            },
            onToolLongClick = { toolView, _ ->
                if (!adapter.isEditMode) {
                    toolView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    adapter.isEditMode = true
                    updateToolbar()
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
            if (adapter.isEditMode) {
                adapter.isEditMode = false
                updateToolbar()
            } else {
                showAddToolsBottomSheet()
            }
        }

        binding.layoutContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && adapter.isEditMode) {
                adapter.isEditMode = false
                updateToolbar()
                true
            } else false
        }
        
        binding.scrollviewFirst.setOnTouchListener { _, event ->
             if (event.action == MotionEvent.ACTION_DOWN && adapter.isEditMode) {
                adapter.isEditMode = false
                updateToolbar()
                true
            } else false
        }

        binding.cardWidgetUpdates.setOnClickListener {
            checkForUpdates()
        }

        setupHeaderAndFooter()
        autoCheckForUpdates()
    }

    fun filterTools(query: String) {
        adapter.filter(query)
    }

    private fun checkBiometricAndNavigate(destinationId: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isBiometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false)
        val biometricHelper = BiometricHelper(requireActivity())

        if (isBiometricEnabled && biometricHelper.canAuthenticate()) {
            biometricHelper.showBiometricPrompt(
                "Restricted Tool",
                "Authentication Required",
                "Please authenticate to access this tool.",
                object : BiometricHelper.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        findNavController().navigate(destinationId)
                    }
                    override fun onAuthenticationError(error: String) {
                        Toast.makeText(requireContext(), "Auth Error: $error", Toast.LENGTH_SHORT).show()
                    }
                    override fun onAuthenticationFailed() {
                        Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            findNavController().navigate(destinationId)
        }
    }

    private fun showVideoIdeaCategoryDialog() {
        val categories = arrayOf("Windhelm (Game)", "UE5 / Game Dev")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                val category = if (which == 0) "Windhelm" else "General"
                showVideoIdeaTypeDialog(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVideoIdeaTypeDialog(category: String) {
        val options = arrayOf("YouTube Short", "Long-Form Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Format ($category)")
            .setItems(options) { _, which ->
                generateAIVideoIdea(isShort = (which == 0), category = category)
            }
            .setNegativeButton("Back") { _, _ -> showVideoIdeaCategoryDialog() }
            .show()
    }

    private fun generateAIVideoIdea(isShort: Boolean, category: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        
        if (apiKey.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("API Key Required")
                .setMessage("Please set your Gemini API key in Settings to use AI features.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    findNavController().navigate(R.id.action_FirstFragment_to_SettingsFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        if (aiManager == null) {
            aiManager = VideoIdeaManager(apiKey)
        }

        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Consulting AI...")
            .setMessage("Generating a custom idea for $category...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            aiManager?.generateIdea(isShort, category, object : VideoIdeaManager.VideoIdeaCallback {
                override fun onSuccess(idea: String) {
                    loadingDialog.dismiss()
                    activity?.runOnUiThread {
                        AlertDialog.Builder(requireContext())
                            .setTitle(if (isShort) "AI Short Idea" else "AI Video Idea")
                            .setMessage(idea)
                            .setPositiveButton("Generate Another") { _, _ -> generateAIVideoIdea(isShort, category) }
                            .setNeutralButton("Change Settings") { _, _ -> showVideoIdeaCategoryDialog() }
                            .setNegativeButton("Close", null)
                            .show()
                    }
                }

                override fun onError(error: String) {
                    loadingDialog.dismiss()
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "AI Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
    }

    private fun showIdeaGenerator() {
        val themes = listOf("Cyberpunk", "Medieval", "Underwater", "Space Western", "Post-Apocalyptic")
        val mechanics = listOf("Permadeath", "Time Loop", "Deck Building", "Base Management", "Grappling Hook")
        val goal = listOf("Escaping a prison", "Finding a cure", "Building an empire", "Revenge", "Exploration")

        val idea = "Theme: ${themes.random()}\nMechanic: ${mechanics.random()}\nGoal: ${goal.random()}"

        AlertDialog.Builder(requireContext())
            .setTitle("PC Game Mechanic Idea")
            .setMessage(idea)
            .setPositiveButton("New Idea") { _, _ -> showIdeaGenerator() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun updateToolbar() {
        val activity = activity as? MainActivity ?: return
        if (adapter.isEditMode) {
            activity.supportActionBar?.title = "Edit Home"
            backPressedCallback.isEnabled = true
        } else {
            activity.supportActionBar?.title = getString(R.string.app_name)
            backPressedCallback.isEnabled = false
        }
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
                activity?.runOnUiThread { binding.textviewUpdateStatus.text = "Up to date" }
            }
            override fun onError(error: String, url: String) {
                activity?.runOnUiThread { binding.textviewUpdateStatus.text = "Check failed" }
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
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                            } else {
                                Toast.makeText(requireContext(), "Download URL not found", Toast.LENGTH_LONG).show()
                            }
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            }
            override fun onNoUpdate() {
                activity?.runOnUiThread { Toast.makeText(requireContext(), "You are on the latest version", Toast.LENGTH_SHORT).show() }
            }
            override fun onError(error: String, url: String) {
                activity?.runOnUiThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Update Failed")
                        .setMessage("$error\n\nChecked URL:\n$url")
                        .setPositiveButton("OK", null).show()
                }
            }
        })
    }

    private fun setupDragAndDrop() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                adapter.onItemMove(v.adapterPosition, t.adapterPosition)
                saveHomeTools()
                return true
            }
            override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}
            override fun isLongPressDragEnabled(): Boolean = adapter.isEditMode
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerviewTools)
    }

    private fun showAddToolsBottomSheet() {
        val currentToolIds = adapter.getItems().map { it.id }.toSet()
        val availableToAdd = allTools.filter { it.id !in currentToolIds }.toMutableList()
        if (availableToAdd.isEmpty()) {
            AlertDialog.Builder(requireContext()).setTitle("No Tools Available").setMessage("All tools are already on your home screen.").setPositiveButton("OK", null).show()
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = LayoutAddToolsBinding.inflate(layoutInflater)
        val sheetAdapter = ToolsAdapter(availableToAdd, { tool ->
            adapter.addTool(tool)
            saveHomeTools()
            dialog.dismiss()
        }, { _, _ -> }, {})
        bottomSheetBinding.recyclerviewAvailableTools.layoutManager = GridLayoutManager(context, 3)
        bottomSheetBinding.recyclerviewAvailableTools.adapter = sheetAdapter
        dialog.setContentView(bottomSheetBinding.root)
        dialog.show()
    }

    private fun saveHomeTools() {
        val ids = adapter.getItems().joinToString(",") { it.id.toString() }
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HOME_TOOLS, ids).apply()
    }

    private fun loadHomeTools() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getString(KEY_HOME_TOOLS, null)
        currentTools.clear()
        if (savedIds != null) {
            val idList = savedIds.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
            idList.forEach { id -> allTools.find { it.id == id }?.let { currentTools.add(it) } }
        } else {
            currentTools.addAll(allTools.take(4))
        }
    }

    private fun setupHeaderAndFooter() {
        val versionName = try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        
        binding.textviewHeaderVersion.text = "v$versionName"
        binding.textviewVersion.text = "Version $versionName"

        val year = Calendar.getInstance().get(Calendar.YEAR)
        binding.textviewCopyright.text = "© $year NorwinLabs"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.fab)?.setOnClickListener(null)
        _binding = null
    }
}