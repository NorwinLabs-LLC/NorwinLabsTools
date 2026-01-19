package com.example.norwinlabstools

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.norwinlabstools.databinding.FragmentDevNewsBinding
import kotlinx.coroutines.launch

class DevNewsFragment : Fragment() {

    private var _binding: FragmentDevNewsBinding? = null
    private val binding get() = _binding!!
    private val newsList = mutableListOf<NewsItem>()
    private lateinit var newsAdapter: NewsAdapter
    private var aiManager: SecurityAIManager? = null // Reusing for AI analysis if needed

    data class NewsItem(
        val title: String,
        val description: String,
        val category: String,
        val aiInsight: String? = null
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newsAdapter = NewsAdapter(newsList)
        binding.rvNews.layoutManager = LinearLayoutManager(context)
        binding.rvNews.adapter = newsAdapter

        loadNews()
    }

    private fun loadNews() {
        binding.newsProgress.visibility = View.VISIBLE
        
        // Simulated high-value news for game developers and accessibility
        val items = listOf(
            NewsItem(
                "Unreal Engine 5.4 Accessibility Overhaul",
                "New UI tools make it easier to implement screen readers and high-contrast modes.",
                "Tech",
                "Insight: Use the new Slate accessibility tags to ensure your Windhelm menus are navigable by all."
            ),
            NewsItem(
                "The 'Game Accessibility Guidelines' Updated for 2025",
                "Comprehensive list of features to help players with physical or cognitive impairments.",
                "Accessibility",
                "Insight: Check the 'Motor' section for advice on rebindable controls."
            ),
            NewsItem(
                "Steam Deck Optimization Best Practices",
                "Valve releases new guidance on font sizes and shader pre-caching.",
                "Game Dev",
                "Insight: Ensure Windhelm's text is legible on a 7-inch screen (minimum 12pt)."
            ),
            NewsItem(
                "Color Blindness in Level Design",
                "How to use silhouettes and patterns instead of just color to convey information.",
                "Design",
                "Insight: Test Windhelm's mana/stamina bars with the 'Protanopia' filter."
            )
        )

        newsList.clear()
        newsList.addAll(items)
        newsAdapter.notifyDataSetChanged()
        binding.newsProgress.visibility = View.GONE
    }

    class NewsAdapter(private val items: List<NewsItem>) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: android.widget.TextView = view.findViewById(android.R.id.text1)
            val tvInfo: android.widget.TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = "[${item.category}] ${item.title}"
            holder.tvTitle.setTextColor(0xFF2E7D32.toInt())
            
            val content = "${item.description}\n\nAI Dev Tip: ${item.aiInsight ?: "Loading..."}"
            holder.tvInfo.text = content
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}