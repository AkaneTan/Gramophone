package org.akanework.gramophone.ui.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.logic.utils.SupportComparator
import java.util.Collections

abstract class BaseAdapter<T>(
	protected val context: Context,
	initialList: MutableList<T>,
	userSorter: Comparator<T>? = null
) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>() {

	private val rawList = ArrayList<T>(initialList.size)
	private var sorter: Comparator<T>?
	private val defaultSorter: Comparator<T> =
		SupportComparator.createAlphanumericComparator { titleOf(it) }
	protected val list = ArrayList<T>(initialList.size)

	init {
		sorter = userSorter ?: defaultSorter
		updateList(initialList, true)
	}

	protected open val defaultCover: Int = R.drawable.ic_default_cover
	protected abstract val layout: Int

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

	final override fun onBindViewHolder(
		holder: ViewHolder,
		position: Int,
	) {
		val item = list[position]
		holder.title.text = titleOf(item)
		holder.subTitle.text = subTitleOf(item)
		Glide
			.with(holder.songCover.context)
			.load(coverOf(item))
			.placeholder(defaultCover)
			.into(holder.songCover)
		holder.itemView.setOnClickListener { onClick(item) }
		holder.moreButton.setOnClickListener {
			val popupMenu = PopupMenu(it.context, it)
			onMenu(item, popupMenu)
			popupMenu.show()
		}
	}

	abstract fun toId(item: T): String
	abstract fun titleOf(item: T): String
	abstract fun subTitleOf(item: T): String
	abstract fun coverOf(item: T): Uri?
	protected abstract fun onClick(item: T)
	protected abstract fun onMenu(item: T, popupMenu: PopupMenu)
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

	protected fun toRawPos(item: T): Int {
		return rawList.indexOf(item)
	}

	abstract class ItemAdapter<T : MediaStoreUtils.Item>(context: Context,
	                                                     rawList: MutableList<T>
	) : BaseAdapter<T>(context, rawList) {
		override fun toId(item: T): String {
			return item.id.toString()
		}

		override fun subTitleOf(item: T): String {
			return context.resources.getQuantityString(
				R.plurals.songs, item.songList.size, item.songList.size)
		}

		override fun coverOf(item: T): Uri? {
			return item.songList
				.firstOrNull()
				?.mediaMetadata
				?.artworkUri
		}
	}

	open class BasePopupTextProvider<T>(private val adapter: BaseAdapter<T>) : PopupTextProvider {
		final override fun getPopupText(position: Int): CharSequence {
			return (if (position != 0)
				getTitleFor(adapter.list[position])?.first()?.toString()
			else null) ?: "-"
		}

		open fun getTitleFor(item: T): String? {
			return adapter.titleOf(item)
		}
	}

}