package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
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
		val supportedTypes = adapter.sortTypes /* supported types always contain "None" */
		holder.sortButton.visibility = if (supportedTypes.size > 1) View.VISIBLE else View.GONE
		holder.sortButton.setOnClickListener { view ->
			val popupMenu = PopupMenu(context, view)
			popupMenu.inflate(R.menu.sort_menu)
			val buttonMap = mapOf(
				Pair(R.id.name, BaseAdapter.Sorter.Type.ByTitleAscending),
				Pair(R.id.artist, BaseAdapter.Sorter.Type.ByArtistAscending),
				Pair(R.id.album, BaseAdapter.Sorter.Type.ByAlbumTitleAscending),
				Pair(R.id.size, BaseAdapter.Sorter.Type.BySizeDescending)
			)
			buttonMap.forEach {
				popupMenu.menu.findItem(it.key).isVisible = supportedTypes.contains(it.value)
			}
			when (adapter.sortType) {
				in buttonMap.values -> {
					popupMenu.menu.findItem(buttonMap.entries
						.first { it.value == adapter.sortType }.key).isChecked = true
				}
				else -> throw IllegalStateException("Invalid sortType ${adapter.sortType.name}")
			}
			popupMenu.setOnMenuItemClickListener { menuItem ->
				when (menuItem.itemId) {
					in buttonMap.keys -> {
						if (!menuItem.isChecked) {
							adapter.sort(buttonMap[menuItem.itemId]!!)
							menuItem.isChecked = true
						}
						true
					}

					else -> onExtraMenuButtonPressed(popupMenu, menuItem)
				}
			}
			onSortButtonPressed(popupMenu)
			popupMenu.show()
		}
	}

	protected open fun onSortButtonPressed(popupMenu: PopupMenu) {}
	protected open fun onExtraMenuButtonPressed(popupMenu: PopupMenu, menuItem: MenuItem): Boolean
			= false

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