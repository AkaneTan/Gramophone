package org.akanework.gramophone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator
import java.util.Collections

abstract class BaseAdapter<T>(
	private val layout: Int,
	initialList: MutableList<T>,
	private var sorter: Comparator<T>?
) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>() {

	private val rawList = ArrayList<T>(initialList.size)
	protected val list = ArrayList<T>(initialList.size)

	init {
		updateList(initialList, true)
	}

	inner class ViewHolder(
		view: View,
	) : RecyclerView.ViewHolder(view) {
		val songCover: ImageView = view.findViewById(R.id.cover)
		val title: TextView = view.findViewById(R.id.title)
		val subTitle: TextView = view.findViewById(R.id.artist)
		val moreButton: MaterialButton = view.findViewById(R.id.more)
	}

	fun getFrozenList(): List<T> {
		return Collections.unmodifiableList(list)
	}

	override fun getItemCount(): Int = list.size

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): ViewHolder =
		ViewHolder(
			LayoutInflater
				.from(parent.context)
				.inflate(layout, parent, false),
		)

	fun sort(selector: Comparator<T>) {
		sorter = selector
		CoroutineScope(Dispatchers.Default).launch {
			val apply = sort()
			withContext(Dispatchers.Main) {
				apply()
			}
		}
	}

	private fun sort(srcList: MutableList<T>? = null): () -> Unit {
		// Sorting in the background using coroutines
		val newList = ArrayList(srcList ?: rawList)
		newList.sortWith { o1, o2 ->
			if (isPinned(o1) && !isPinned(o2)) -1
			else if (!isPinned(o1) && isPinned(o2)) 1
			else sorter?.compare(o1, o2) ?: 0
		}
		val apply = updateListSorted(newList)
		return {
			if (srcList != null) {
				rawList.clear()
				rawList.addAll(srcList)
			}
			apply()
		}
	}

	private fun updateListSorted(newList: MutableList<T>): () -> Unit {
		val diffResult = DiffUtil.calculateDiff(SongDiffCallback(list, newList))
		return {
			list.clear()
			list.addAll(newList)
			diffResult.dispatchUpdatesTo(this)
		}
	}

	fun updateList(newList: MutableList<T>, now: Boolean = false) {
		if (now) sort(newList)()
		else {
			CoroutineScope(Dispatchers.Default).launch {
				val apply = sort(newList)
				withContext(Dispatchers.Main) {
					apply()
				}
			}
		}
	}

	protected abstract fun toId(item: T): String
	protected open fun isPinned(item: T): Boolean {
		return false
	}

	private inner class SongDiffCallback(
		private val oldList: MutableList<T>,
		private val newList: MutableList<T>,
	) : DiffUtil.Callback() {
		override fun getOldListSize() = oldList.size

		override fun getNewListSize() = newList.size

		override fun areItemsTheSame(
			oldItemPosition: Int,
			newItemPosition: Int,
		) = toId(oldList[oldItemPosition]) == toId(newList[newItemPosition])

		override fun areContentsTheSame(
			oldItemPosition: Int,
			newItemPosition: Int,
		) = oldList[oldItemPosition] == newList[newItemPosition]
	}

	protected fun toRawPos(position: Int): Int {
		return rawList.indexOf(list[position])
	}

	abstract class ItemAdapter<T : MediaStoreUtils.Item>(layout: Int,
	                                                     rawList: MutableList<T>,
	                                                     sorter: Comparator<T>
	                                                     = SupportComparator.createAlphanumericComparator { it.title }
	) : BaseAdapter<T>(layout, rawList, sorter) {
		override fun toId(item: T): String {
			return item.id.toString()
		}
	}

}