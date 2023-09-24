package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.akanework.gramophone.R

abstract class BaseDecorAdapter<T : BaseAdapter<U>, U>(
	protected val context: Context,
	private var count: Int,
	protected val adapter: T,
	private val canSort: Boolean = true,
	) : RecyclerView.Adapter<BaseDecorAdapter<T, U>.ViewHolder>() {

	protected abstract val pluralStr: Int

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.general_decor, parent, false)
		return ViewHolder(view)
	}

	final override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.counter.text = context.resources.getQuantityString(pluralStr, count, count)
		holder.sortButton.visibility = if (canSort) View.VISIBLE else View.GONE
		holder.sortButton.setOnClickListener {
			val popupMenu = PopupMenu(context, it)
			onSortButtonPressed(popupMenu)
			popupMenu.show()
		}
	}

	protected abstract fun onSortButtonPressed(popupMenu: PopupMenu)

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
}