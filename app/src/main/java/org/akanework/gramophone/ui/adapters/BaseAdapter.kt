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
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.AlphaNumericComparator
import org.akanework.gramophone.logic.utils.MediaStoreUtils

abstract class BaseAdapter<T>(
	private val layout: Int,
	private val rawList: MutableList<T>
) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>() {

	protected val list = ArrayList<T>(rawList.size)

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
		// Sorting in the background using coroutines
		CoroutineScope(Dispatchers.Default).launch {
			val newList = ArrayList(rawList)
			newList.sortWith(selector)
			updateList(newList)
		}
	}


	fun updateList(newList: MutableList<T>) {
		val diffResult = DiffUtil.calculateDiff(SongDiffCallback(list, newList))
		list.clear()
		list.addAll(newList)
		diffResult.dispatchUpdatesTo(this)
	}

	protected abstract fun toId(item: T): String

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

	fun sortAlphanumeric(inverted: Boolean = false, converter: (T) -> CharSequence) {
		sort(SupportComparator(AlphaNumericComparator(), inverted) { converter(it).toString() })
	}

	abstract class ItemAdapter<T : MediaStoreUtils.Item>(layout: Int,
	                                                     rawList: MutableList<T>
	) : BaseAdapter<T>(layout, rawList) {
		override fun toId(item: T): String {
			return item.id.toString()
		}

		init {
			sortAlphanumeric { it.title }
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
		}
	}
}