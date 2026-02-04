package com.example.norwinlabstools

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.norwinlabstools.databinding.FragmentBudgetBinding
import com.example.norwinlabstools.databinding.ItemBudgetCategoryBinding
import com.google.android.material.textfield.TextInputLayout

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val categories = mutableListOf(
        BudgetCategory("Rent/Mortgage", 0.0, 0xFFE91E63.toInt()),
        BudgetCategory("Food", 0.0, 0xFF2196F3.toInt()),
        BudgetCategory("Utilities", 0.0, 0xFFFFC107.toInt()),
        BudgetCategory("Entertainment", 0.0, 0xFF9C27B0.toInt())
    )
    
    private var monthlyIncome = 0.0
    private lateinit var allocationAdapter: AllocationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allocationAdapter = AllocationAdapter(categories) { updateCalculations() }
        binding.rvAllocations.layoutManager = LinearLayoutManager(context)
        binding.rvAllocations.adapter = allocationAdapter

        binding.editIncome.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                monthlyIncome = s.toString().toDoubleOrNull() ?: 0.0
                updateCalculations()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        updateCalculations()
    }

    private fun updateCalculations() {
        var totalAllocated = 0.0
        categories.forEach { totalAllocated += it.amount }
        
        val extra = monthlyIncome - totalAllocated
        binding.tvExtra_to_invest.text = "Extra to Invest: $${String.format("%.2f", if (extra > 0) extra else 0.0)}"
        
        if (extra < 0) {
            binding.tvExtra_to_invest.setTextColor(0xFFFF5252.toInt()) // Red for over-budget
        } else {
            binding.tvExtra_to_invest.setTextColor(0xFF4CAF50.toInt()) // Green for healthy budget
        }

        binding.pieChart.setData(monthlyIncome, categories)
    }

    private fun showAddCategoryDialog() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val nameInput = EditText(context).apply { hint = "Category Name" }
        layout.addView(nameInput)

        AlertDialog.Builder(requireContext())
            .setTitle("New Budget Category")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    val randomColor = (0xFF000000.toInt() until 0xFFFFFFFF.toInt()).random()
                    categories.add(BudgetCategory(name, 0.0, randomColor or 0xFF000000.toInt()))
                    allocationAdapter.notifyItemInserted(categories.size - 1)
                    updateCalculations()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class AllocationAdapter(
        private val items: MutableList<BudgetCategory>,
        private val onAmountChanged: () -> Unit
    ) : RecyclerView.Adapter<AllocationAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvCategoryName.text = item.name
            holder.binding.viewColorTag.setBackgroundColor(item.color)
            
            // Remove text watcher before setting text to avoid infinite loop
            holder.binding.editAmount.removeTextChangedListener(holder.binding.editAmount.tag as? TextWatcher)
            holder.binding.editAmount.setText(if (item.amount > 0) item.amount.toString() else "")
            
            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newVal = s.toString().toDoubleOrNull() ?: 0.0
                    items[holder.adapterPosition] = item.copy(amount = newVal)
                    onAmountChanged()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            holder.binding.editAmount.addTextChangedListener(watcher)
            holder.binding.editAmount.tag = watcher
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}