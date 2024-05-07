package com.example.geoguesser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreAdapter(private val scoreList: List<Score>) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scoreboard, parent, false)
        return ScoreViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val currentItem = scoreList[position]
        holder.textViewRank.text = (position + 1).toString()
        holder.textViewName.text = currentItem.playerName
        holder.textViewScore.text = currentItem.score.toString()
    }

    override fun getItemCount() = scoreList.size

    class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewRank: TextView = itemView.findViewById(R.id.tvRank)
        val textViewName: TextView = itemView.findViewById(R.id.tvUserName)
        val textViewScore: TextView = itemView.findViewById(R.id.tvUserPoints)
    }
}
