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
import org.akanework.gramophone.logic.utils.AlphaNumericComparator
import org.akanework.gramophone.logic.utils.MediaStoreUtils

abstract class BaseAdapter<T>(
	private val layout: Int,
	initialList: MutableList<T>,
	private var sorter: Comparator<T>
) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>() {

	private val rawList = ArrayList<T>(initialList.size)
	protected val list = ArrayList<T>(initialList.size)

	init {
		updateList(initialList)
	}

	inner class ViewHolder(
		view: View,
	) : RecyclerView.ViewHolder(view) {
		val songCover: ImageView = view.findViewById(R.id.cover)
		val title: TextView = view.findViewById(R.id.title)
		val subTitle: TextView = view.findViewById(R.id.artist)
		val moreButton: MaterialButton = view.findViewById(R.id.more)
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
		sort()
	}

	private fun sort() {
		// Sorting in the background using coroutines
		CoroutineScope(Dispatchers.Default).launch {
			val newList = ArrayList(rawList)
			newList.sortWith { o1, o2 ->
				if (isPinned(o1) && !isPinned(o2)) -1
				else if (!isPinned(o1) && isPinned(o2)) 1
				else sorter.compare(o1, o2)
			}
			val apply = updateListSorted(newList)
			withContext(Dispatchers.Main) {
				apply()
			}
		}
	}

	private fun updateListSorted(newList: MutableList<T>): () -> Unit {
		val diffResult = DiffUtil.calculateDiff(SongDiffCallback(list, newList))
		list.clear()
		list.addAll(newList)
		return { diffResult.dispatchUpdatesTo(this) }
	}

	fun updateList(newList: MutableList<T>) {
		rawList.clear()
		rawList.addAll(newList)
		sort()
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

	abstract class ItemAdapter<T : MediaStoreUtils.Item>(layout: Int,
	                                                     rawList: MutableList<T>,
	                                                     sorter: Comparator<T>
	                                                     = SupportComparator.createAlphanumericComparator { it.title }
	) : BaseAdapter<T>(layout, rawList, sorter) {
		override fun toId(item: T): String {
			return item.id.toString()
		}
	}

	class SupportComparator<T, U>(private val cmp: Comparator<U>,
	                              private val invert: Boolean,
	                              private val convert: (T) -> U)
		: Comparator<T> {
		override fun compare(o1: T, o2: T): Int {
			return cmp.compare(convert(o1), convert(o2)) * (if (invert) -1 else 1)
		}

		companion object {
			fun <T> createInversionComparator(cmp: Comparator<T>, invert: Boolean = false):
					Comparator<T> {
				return SupportComparator(cmp, invert) { it }
			}

			fun <T> createAlphanumericComparator(inverted: Boolean = false, converter: (T) -> CharSequence): Comparator<T> {
				return SupportComparator(AlphaNumericComparator(), inverted) { converter(it).toString() }
			}
		}
	}
}