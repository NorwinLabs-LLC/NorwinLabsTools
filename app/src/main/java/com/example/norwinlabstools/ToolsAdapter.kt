package com.example.norwinlabstools

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.norwinlabstools.databinding.ItemToolBinding
import java.util.Collections

class ToolsAdapter(
    private var tools: MutableList<Tool>,
    private val onToolClick: (Tool) -> Unit,
    private val onToolLongClick: (View, Tool) -> Unit,
    private val onRemoveClick: (Tool) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.ToolViewHolder>() {

    private var toolsFull = ArrayList(tools)
    var isEditMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ToolViewHolder(val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tool: Tool, onToolClick: (Tool) -> Unit, onToolLongClick: (View, Tool) -> Unit, onRemoveClick: (Tool) -> Unit, isEditMode: Boolean) {
            binding.toolName.text = tool.name
            binding.toolIcon.setImageResource(tool.iconRes)
            binding.toolVersion.text = "v${tool.version}"
            
            // Removed custom color overlay as per user request
            // binding.toolColorOverlay.setBackgroundColor(tool.color)
            
            // Load background image if available
            if (tool.imageUrl != null) {
                Glide.with(binding.root.context)
                    .load(tool.imageUrl)
                    .centerCrop()
                    .into(binding.toolImageBackground)
                binding.toolImageBackground.visibility = View.VISIBLE
                binding.toolColorOverlay.visibility = View.VISIBLE // Keep subtle dark tint for readability
            } else {
                binding.toolImageBackground.visibility = View.GONE
                binding.toolColorOverlay.visibility = View.GONE
            }

            binding.buttonRemove.visibility = if (isEditMode) View.VISIBLE else View.GONE
            binding.buttonRemove.setOnClickListener { onRemoveClick(tool) }

            binding.cardTool.setOnClickListener { onToolClick(tool) }
            binding.cardTool.setOnLongClickListener {
                onToolLongClick(binding.cardTool, tool)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(tools[position], onToolClick, onToolLongClick, onRemoveClick, isEditMode)
    }

    override fun getItemCount(): Int = tools.size

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            toolsFull
        } else {
            toolsFull.filter { it.name.lowercase().contains(query.lowercase()) }
        }
        tools.clear()
        tools.addAll(filteredList)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(tools, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(tools, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        toolsFull = ArrayList(tools)
    }

    fun addTool(tool: Tool) {
        tools.add(tool)
        toolsFull = ArrayList(tools)
        notifyItemInserted(tools.size - 1)
    }

    fun removeTool(tool: Tool) {
        val position = tools.indexOf(tool)
        if (position != -1) {
            tools.removeAt(position)
            toolsFull = ArrayList(tools)
            notifyItemRemoved(position)
        }
    }

    fun getItems(): List<Tool> = tools
}