package org.akanework.gramophone.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.components.CustomGridLayoutManager
import org.akanework.gramophone.ui.fragments.AdapterFragment
import java.util.Collections

abstract class BaseAdapter<T>(
    val context: Context,
    protected var liveData: MutableLiveData<MutableList<T>>?,
    sortHelper: Sorter.Helper<T>,
    naturalOrderHelper: Sorter.NaturalOrderHelper<T>?,
    initialSortType: Sorter.Type,
    private val pluralStr: Int,
    val ownsView: Boolean,
    defaultLayoutType: LayoutType,
    private val indicatorResource: Int
) : AdapterFragment.BaseInterface<BaseAdapter<T>.ViewHolder>(), Observer<MutableList<T>>,
    PopupTextProvider {

    private val sorter = Sorter(sortHelper, naturalOrderHelper)
    private val decorAdapter by lazy { createDecorAdapter() }
    override val concatAdapter by lazy { ConcatAdapter(decorAdapter, this) }
    private val handler = Handler(Looper.getMainLooper())
    private var bgHandlerThread: HandlerThread? = null
    private var bgHandler: Handler? = null
    private val rawList = ArrayList<T>(liveData?.value?.size ?: 0)
    protected val list = ArrayList<T>(liveData?.value?.size ?: 0)
    private var comparator: Sorter.HintedComparator<T>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    protected var recyclerView: RecyclerView? = null
        private set
    var layoutType: LayoutType? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            if (value != null && ownsView) {
                layoutManager = if (value != LayoutType.GRID
                    && context.resources.configuration.orientation
                    == Configuration.ORIENTATION_PORTRAIT
                )
                    LinearLayoutManager(context)
                else CustomGridLayoutManager(
                    context, if (value != LayoutType.GRID
                        || context.resources.configuration.orientation
                        == Configuration.ORIENTATION_PORTRAIT
                    ) 2 else 4
                )
                if (recyclerView != null) applyLayoutManager()
            }
            notifyDataSetChanged() // we change view type for all items
        }
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
        layoutType = defaultLayoutType
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
        this.recyclerView = recyclerView
        if (ownsView) {
            if (recyclerView.layoutManager != layoutManager) {
                applyLayoutManager()
            }
        }
        liveData?.observeForever(this)
        bgHandlerThread = HandlerThread(BaseAdapter::class.qualifiedName).apply {
            start()
            bgHandler = Handler(looper)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
        if (ownsView) {
            recyclerView.layoutManager = null
        }
        liveData?.removeObserver(this)
        bgHandler = null
        bgHandlerThread!!.quitSafely()
        bgHandlerThread = null
    }

    private fun applyLayoutManager() {
        // If a layout manager has already been set, get current scroll position.
        val scrollPosition = if (recyclerView?.layoutManager != null) {
            (recyclerView!!.layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        } else 0
        recyclerView?.layoutManager = layoutManager
        recyclerView?.scrollToPosition(scrollPosition)
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

    private fun sort(srcList: List<T>? = null): (Boolean, Boolean) -> Unit {
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
            if (!now) decorAdapter.updateSongCounter()
        }
    }

    fun updateList(newList: List<T>, now: Boolean, canDiff: Boolean) {
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
        return BaseDecorAdapter(this, pluralStr, indicatorResource)
    }

    override fun getItemViewType(position: Int): Int {
        return when (layoutType) {
            LayoutType.GRID -> R.layout.adapter_grid_card
            LayoutType.COMPACT_LIST -> R.layout.adapter_list_card
            LayoutType.LIST, null -> R.layout.adapter_list_card_larger
        }
    }

    final override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val item = list[position]
        holder.title.text = titleOf(item) ?: virtualTitleOf(item)
        holder.subTitle.text = subTitleOf(item)
        Glide
            .with(holder.songCover.context)
            .load(coverOf(item))
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(defaultCover)
            .into(holder.songCover)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.moreButton.setOnClickListener {
            val popupMenu = PopupMenu(it.context, it)
            onMenu(item, popupMenu)
            popupMenu.show()
        }
    }

    private fun toId(item: T): String {
        return sorter.sortingHelper.getId(item)
    }

    fun titleOf(item: T): String? {
        return if (sorter.sortingHelper.canGetTitle())
            sorter.sortingHelper.getTitle(item) else "null"
    }

    protected abstract fun virtualTitleOf(item: T): String
    fun subTitleOf(item: T): String {
        return if (sorter.sortingHelper.canGetArtist())
            sorter.sortingHelper.getArtist(item) ?: context.getString(R.string.unknown_artist)
        else if (sorter.sortingHelper.canGetSize()) {
            val s = sorter.sortingHelper.getSize(item)
            return context.resources.getQuantityString(
                R.plurals.songs, s, s
            )
        } else "null"
    }

    fun coverOf(item: T): Uri? {
        return sorter.sortingHelper.getCover(item)
    }

    protected abstract fun onClick(item: T)
    protected abstract fun onMenu(item: T, popupMenu: PopupMenu)
    private fun isPinned(item: T): Boolean {
        return titleOf(item) == null
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

    final override fun getPopupText(view: View, position: Int): CharSequence {
        // position here refers to pos in ConcatAdapter(!)
        return (if (position != 0)
            sorter.getFastScrollHintFor(list[position - 1], sortType)
        else null) ?: "-"
    }

    enum class LayoutType {
        LIST, COMPACT_LIST, GRID
    }

    open class StoreItemHelper<T : MediaStoreUtils.Item>(
        typesSupported: Set<Sorter.Type> = setOf(
            Sorter.Type.ByTitleDescending, Sorter.Type.ByTitleAscending,
            Sorter.Type.BySizeDescending, Sorter.Type.BySizeAscending
        )
    ) : Sorter.Helper<T>(typesSupported) {
        override fun getId(item: T): String {
            return item.id.toString()
        }

        override fun getTitle(item: T): String? {
            return item.title
        }

        override fun getSize(item: T): Int {
            return item.songList.size
        }

        override fun getCover(item: T): Uri? {
            return item.songList.firstOrNull()?.mediaMetadata?.artworkUri
        }
    }
}