package org.akanework.gramophone.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.ConcatAdapter
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
import org.akanework.gramophone.logic.utils.isSupertypeOrEquals
import java.util.Collections

abstract class BaseAdapter<T>(
	val context: Context,
	protected var liveData: MutableLiveData<MutableList<T>>?,
	private val sorter: Sorter<T> = Sorter.noneSorter(),
	initialSortType: Sorter.Type = Sorter.Type.None,
	private val pluralStr: Int
) : RecyclerView.Adapter<BaseAdapter<T>.ViewHolder>(), Observer<MutableList<T>> {

	open val decorAdapter by lazy { createDecorAdapter() }
	val concatAdapter by lazy { ConcatAdapter(decorAdapter, this) }
	private val handler = Handler(Looper.getMainLooper())
	private var bgHandlerThread: HandlerThread? = null
	protected var bgHandler: Handler? = null
		private set
	private val rawList = ArrayList<T>(liveData?.value?.size ?: 0)
	protected val list = ArrayList<T>(liveData?.value?.size ?: 0)
	private var comparator: Sorter.HintedComparator<T>? = null
	var sortType: Sorter.Type
		get() = comparator?.type!!
		private set(value) {
			if (comparator?.type != value) {
				comparator = sorter.getComparator(value)
			}
		}
	val sortTypes: Set<Sorter.Type>
		get() = sorter.getSupportedTypes()

	init {
		sortType = initialSortType
		liveData?.value?.let { updateList(it, now = true, canDiff = false) }
	}

	protected open val defaultCover: Int = R.drawable.ic_default_cover

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

	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		super.onAttachedToRecyclerView(recyclerView)
		liveData?.observeForever(this)
		bgHandlerThread = HandlerThread(BaseAdapter::class.qualifiedName).apply {
			start()
			bgHandler = Handler(looper)
		}
	}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		super.onDetachedFromRecyclerView(recyclerView)
		liveData?.removeObserver(this)
		bgHandler = null
		bgHandlerThread!!.quitSafely()
		bgHandlerThread = null
	}

	override fun onChanged(value: MutableList<T>) {
		updateList(value, now = false, true)
	}

	override fun getItemCount(): Int = list.size

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): ViewHolder =
		ViewHolder(
			LayoutInflater
				.from(parent.context)
				.inflate(viewType, parent, false),
		)

	fun sort(selector: Sorter.Type) {
		sortType = selector
		CoroutineScope(Dispatchers.Default).launch {
			val apply = sort()
			withContext(Dispatchers.Main) {
				apply(false, true)
			}
		}
	}

	private fun sort(srcList: MutableList<T>? = null): (Boolean, Boolean) -> Unit {
		// Sorting in the background using coroutines
		val newList = ArrayList(srcList ?: rawList)
		newList.sortWith { o1, o2 ->
			if (isPinned(o1) && !isPinned(o2)) -1
			else if (!isPinned(o1) && isPinned(o2)) 1
			else comparator?.compare(o1, o2) ?: 0
		}
		val apply = updateListSorted(newList)
		return { now, canDiff ->
			apply(now, canDiff)
			if (srcList != null) {
				rawList.clear()
				rawList.addAll(srcList)
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun updateListSorted(newList: MutableList<T>): (Boolean, Boolean) -> Unit {
		return { now, canDiff ->
			val diffResult = if (canDiff)
				DiffUtil.calculateDiff(SongDiffCallback(list, newList)) else null
			list.clear()
			list.addAll(newList)
			if (canDiff) diffResult!!.dispatchUpdatesTo(this) else notifyDataSetChanged()
			if (!now) decorAdapter.updateSongCounter(list.size)
		}
	}

	fun updateList(newList: MutableList<T>, now: Boolean, canDiff: Boolean) {
		if (now || bgHandler == null) sort(newList)(true, canDiff)
		else {
			bgHandler!!.post {
				val apply = sort(newList)
				handler.post {
						apply(false, canDiff)
				}
			}
		}
	}

	protected open fun createDecorAdapter(): BaseDecorAdapter<out BaseAdapter<T>> {
		return BaseDecorAdapter(this, pluralStr)
	}

	abstract override fun getItemViewType(position: Int): Int

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

	open class BasePopupTextProvider<T>(private val adapter: BaseAdapter<T>) : PopupTextProvider {
		final override fun getPopupText(position: Int): CharSequence {
			return (if (position != 0)
				getHintFor(adapter.list[position])
			else null) ?: "-"
		}

		open fun getHintFor(item: T): String? {
			return adapter.sorter.getFastScrollHintFor(item, adapter.sortType)
		}
	}

}

abstract class ItemAdapter<T : MediaStoreUtils.Item>(context: Context,
                                                     rawList: MutableLiveData<MutableList<T>>,
		                                             sorter: Sorter<T>,
                                                     initialSortType: Sorter.Type
                                                     = Sorter.Type.ByTitleAscending,
                                                     pluralStr: Int = R.plurals.items
) : BaseAdapter<T>(context, rawList, sorter, initialSortType, pluralStr) {
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

	override fun isPinned(item: T): Boolean {
		return item.title == null
	}
}

class Sorter<T> private constructor(private val sortingHelper: Helper<T>,
                                    private val naturalOrderHelper: NaturalOrderHelper<T>?) {
	companion object {
		fun <T> internalCreateSorter(sortingHelper: Helper<T>, naturalOrderHelper: NaturalOrderHelper<T>?): Sorter<T> {
			return Sorter(sortingHelper, naturalOrderHelper)
		}

		@Suppress("unused", "RedundantSuppression")
		fun <T : MediaStoreUtils.Item> internalFromStoreItem(
			@Suppress("UNUSED_PARAMETER") dummy: T?): Helper<T> {
			return StoreItemHelper()
		}

		fun internalFromStoreAlbum(): Helper<MediaStoreUtils.Album> {
			return StoreAlbumHelper()
		}

		fun internalFromMediaItem(): Helper<MediaItem> {
			return MediaItemHelper()
		}

		@Suppress("UNCHECKED_CAST", "SameParameterValue")
		inline fun <reified T> from(dummy: T?, naturalOrderHelper: NaturalOrderHelper<T>?): Sorter<T> {
			return internalCreateSorter(
				if (T::class.isSupertypeOrEquals(MediaStoreUtils.Album::class)) {
					internalFromStoreAlbum() as Helper<T>
				} else if (T::class.isSupertypeOrEquals(MediaStoreUtils.Item::class)) {
					internalFromStoreItem(dummy as MediaStoreUtils.Item?) as Helper<T>
				} else if (T::class.isSupertypeOrEquals(MediaItem::class)) {
					internalFromMediaItem() as Helper<T>
				} else throw IllegalArgumentException("Unsupported: ${T::class.qualifiedName}"),
				naturalOrderHelper
			)
		}
		inline fun <reified T> from(naturalOrderHelper: NaturalOrderHelper<T>? = null): Sorter<T> {
			return from(dummy = null, naturalOrderHelper = naturalOrderHelper)
		}
		fun <T> noneSorter(): Sorter<T> {
			return internalCreateSorter(NoneHelper(), null)
		}
	}

	abstract class Helper<T>(typesSupported: Set<Type>) {
		init {
			if (typesSupported.contains(Type.NaturalOrder))
				throw IllegalStateException()
		}
		val typesSupported = typesSupported.toMutableSet().apply { add(Type.None) }.toSet()
		abstract fun getTitle(item: T): String
		open fun getArtist(item: T): String = throw UnsupportedOperationException()
		open fun getAlbumTitle(item: T): String = throw UnsupportedOperationException()
		open fun getAlbumArtist(item: T): String = throw UnsupportedOperationException()
		open fun getSize(item: T): Int = throw UnsupportedOperationException()
	}

	fun interface NaturalOrderHelper<T> {
		fun lookup(item: T): Int
	}

	private class NoneHelper<T> : Helper<T>(setOf(Type.None)) {
		override fun getTitle(item: T) = throw UnsupportedOperationException()
	}

	private open class StoreItemHelper<T : MediaStoreUtils.Item>(
		typesSupported: Set<Type> = setOf(
			Type.ByTitleDescending, Type.ByTitleAscending,
			Type.BySizeDescending, Type.BySizeAscending
		)
	) : Helper<T>(typesSupported) {
		override fun getTitle(item: T): String {
			return item.title.toString()
		}

		override fun getSize(item: T): Int {
			return item.songList.size
		}
	}

	private class StoreAlbumHelper : StoreItemHelper<MediaStoreUtils.Album>(
		setOf(
			Type.ByTitleDescending, Type.ByTitleAscending,
			Type.ByArtistDescending, Type.ByArtistAscending,
			Type.BySizeDescending, Type.BySizeAscending
		)
	) {
		override fun getArtist(item: MediaStoreUtils.Album): String {
			return item.artist ?: ""
		}
	}

	private open class MediaItemHelper(types: Set<Type> = setOf(
		Type.ByTitleDescending, Type.ByTitleAscending,
		Type.ByArtistDescending, Type.ByArtistAscending,
		Type.ByAlbumTitleDescending, Type.ByAlbumTitleAscending,
		Type.ByAlbumArtistDescending, Type.ByAlbumArtistAscending)) : Helper<MediaItem>(types) {
		override fun getTitle(item: MediaItem): String {
			return item.mediaMetadata.title.toString()
		}

		override fun getArtist(item: MediaItem): String {
			return item.mediaMetadata.artist?.toString() ?: ""
		}

		override fun getAlbumTitle(item: MediaItem): String {
			return item.mediaMetadata.albumTitle?.toString() ?: ""
		}

		override fun getAlbumArtist(item: MediaItem): String {
			return item.mediaMetadata.albumArtist?.toString() ?: ""
		}
	}

	enum class Type {
		ByTitleDescending, ByTitleAscending,
		ByArtistDescending, ByArtistAscending,
		ByAlbumTitleDescending, ByAlbumTitleAscending,
		ByAlbumArtistDescending, ByAlbumArtistAscending,
		BySizeDescending, BySizeAscending,
		NaturalOrder, None
	}

	fun getSupportedTypes(): Set<Type> {
		return sortingHelper.typesSupported.let {
			if (naturalOrderHelper != null)
				it + Type.NaturalOrder
			else it }
	}

	fun getComparator(type: Type): HintedComparator<T> {
		if (!getSupportedTypes().contains(type))
			throw IllegalArgumentException("Unsupported type ${type.name}")
		return WrappingHintedComparator(type, when (type) {
			Type.ByTitleDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getTitle(it)
				}
			}
			Type.ByTitleAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getTitle(it)
				}
			}
			Type.ByArtistDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getArtist(it)
				}
			}
			Type.ByArtistAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getArtist(it)
				}
			}
			Type.ByAlbumTitleDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getAlbumTitle(it)
				}
			}
			Type.ByAlbumTitleAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getAlbumTitle(it)
				}
			}
			Type.ByAlbumArtistDescending -> {
				SupportComparator.createAlphanumericComparator(true) {
					sortingHelper.getAlbumArtist(it)
				}
			}
			Type.ByAlbumArtistAscending -> {
				SupportComparator.createAlphanumericComparator(false) {
					sortingHelper.getAlbumArtist(it)
				}
			}
			Type.BySizeDescending -> {
				SupportComparator.createInversionComparator(
					compareBy { sortingHelper.getSize(it) }, true
				)
			}
			Type.BySizeAscending -> {
				SupportComparator.createInversionComparator(
					compareBy { sortingHelper.getSize(it) }, false
				)
			}
			Type.NaturalOrder -> {
				SupportComparator.createInversionComparator(
					compareBy { naturalOrderHelper!!.lookup(it) }, false
				)
			}
			Type.None -> SupportComparator.createDummyComparator()
		})
	}

	fun getFastScrollHintFor(item: T, sortType: Type): String? {
		return when (sortType) {
			Type.ByTitleDescending, Type.ByTitleAscending -> {
				sortingHelper.getTitle(item).firstOrNull()?.toString()
			}
			Type.ByArtistDescending, Type.ByArtistAscending -> {
				sortingHelper.getArtist(item).firstOrNull()?.toString()
			}
			Type.ByAlbumTitleDescending, Type.ByAlbumTitleAscending -> {
				sortingHelper.getAlbumTitle(item).firstOrNull()?.toString()
			}
			Type.ByAlbumArtistDescending, Type.ByAlbumArtistAscending -> {
				sortingHelper.getAlbumArtist(item).firstOrNull()?.toString()
			}
			Type.BySizeDescending, Type.BySizeAscending -> {
				sortingHelper.getSize(item).toString()
			}
			Type.NaturalOrder -> {
				naturalOrderHelper!!.lookup(item).toString()
			}
			Type.None -> null
		}?.ifEmpty { null }
	}

	abstract class HintedComparator<T>(val type: Type) : Comparator<T>
	private class WrappingHintedComparator<T>(type: Type, private val comparator: Comparator<T>)
		: HintedComparator<T>(type) {
		override fun compare(o1: T, o2: T): Int {
			return comparator.compare(o1, o2)
		}
	}
}

open class BaseDecorAdapter<T : BaseAdapter<*>>(
	protected val adapter: T,
	private val pluralStr: Int
) : RecyclerView.Adapter<BaseDecorAdapter<T>.ViewHolder>() {

	protected val context: Context = adapter.context
	private var count: Int = adapter.itemCount

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
				Pair(R.id.natural, Sorter.Type.NaturalOrder),
				Pair(R.id.name, Sorter.Type.ByTitleAscending),
				Pair(R.id.artist, Sorter.Type.ByArtistAscending),
				Pair(R.id.album, Sorter.Type.ByAlbumTitleAscending),
				Pair(R.id.size, Sorter.Type.BySizeDescending)
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

					else -> onExtraMenuButtonPressed(menuItem)
				}
			}
			onSortButtonPressed(popupMenu)
			popupMenu.show()
		}
	}

	protected open fun onSortButtonPressed(popupMenu: PopupMenu) {}
	protected open fun onExtraMenuButtonPressed(menuItem: MenuItem): Boolean
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