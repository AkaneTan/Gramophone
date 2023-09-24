package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R

abstract class BaseDecorAdapter<T : BaseAdapter<U>, U>(
	protected val context: Context,
	protected var count: Int,
	protected val adapter: T,
	) : RecyclerView.Adapter<BaseDecorAdapter<T, U>.ViewHolder>() {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.general_decor, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = 1

	inner class ViewHolder(
		view: View,
	) : RecyclerView.ViewHolder(view) {
		val sortButton: MaterialButton = view.findViewById(R.id.sort)
		val counter: TextView = view.findViewById(R.id.song_counter)
	}

	fun updateSongCounter(newCount: Int) {
		count = newCount
		notifyItemChanged(0)
	}

	fun isCounterEmpty(): Boolean = count == 0
}