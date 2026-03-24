package com.example.sleeptracker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        preferences = AppPreferences(this)
        historyRecyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.tvEmptyHistory)
        val clearButton = findViewById<Button>(R.id.btnClear)

        historyAdapter = HistoryAdapter()
        historyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        historyRecyclerView.adapter = historyAdapter
        historyRecyclerView.clipToPadding = false
        historyRecyclerView.clipChildren = false
        historyRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        historyRecyclerView.setPadding(120, 0, 120, 0)
        PagerSnapHelper().attachToRecyclerView(historyRecyclerView)
        historyRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                applyCarouselEffect()
            }
        })

        clearButton.setOnClickListener {
            preferences.clearHistoryEntries()
            bindHistory()
        }

        bindHistory()
        historyRecyclerView.post { applyCarouselEffect() }
    }

    private fun bindHistory() {
        val entries = preferences.getHistoryEntries()
        historyAdapter.updateItems(entries)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun applyCarouselEffect() {
        val recyclerCenterX = historyRecyclerView.width / 2f
        for (i in 0 until historyRecyclerView.childCount) {
            val child = historyRecyclerView.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = kotlin.math.abs(recyclerCenterX - childCenterX)
            val normalized = (distance / recyclerCenterX).coerceIn(0f, 1f)
            val scale = 1f - (0.18f * normalized)
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = 0.5f + (1f - normalized) * 0.5f
            child.translationZ = (1f - normalized) * 10f
        }
    }
}
