/*
 *     Copyright (C) 2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.sync.Semaphore
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.utils.FileOpUtils
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import org.akanework.gramophone.ui.components.CustomGridLayoutManager
import org.akanework.gramophone.ui.components.GridPaddingDecoration
import org.akanework.gramophone.ui.fragments.AdapterFragment
import java.util.Collections

@Suppress("LeakingThis")
abstract class BaseAdapter<T>(
    val context: Context,
    protected var liveData: MutableLiveData<MutableList<T>>?,
    sortHelper: Sorter.Helper<T>,
    naturalOrderHelper: Sorter.NaturalOrderHelper<T>?,
    initialSortType: Sorter.Type,
    private val pluralStr: Int,
    val ownsView: Boolean,
    defaultLayoutType: LayoutType,
    private val isSubFragment: Boolean = false,
    private val rawOrderExposed: Boolean = false,
    private val allowDiffUtils: Boolean = false,
    private val canSort: Boolean = true
) : AdapterFragment.BaseInterface<BaseAdapter<T>.ViewHolder>(), Observer<MutableList<T>>,
    PopupTextProvider {

    private val sorter = Sorter(sortHelper, naturalOrderHelper, rawOrderExposed)
    private val decorAdapter by lazy { createDecorAdapter() }
    override val concatAdapter by lazy { ConcatAdapter(decorAdapter, this) }
    private val handler = Handler(Looper.getMainLooper())
    private var bgHandlerThread: HandlerThread? = null
    private var bgHandler: Handler? = null
    private val rawList = ArrayList<T>(liveData?.value?.size ?: 0)
    protected val list = ArrayList<T>(liveData?.value?.size ?: 0)
    private var comparator: Sorter.HintedComparator<T>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var listLock = Semaphore(1)
    protected var recyclerView: RecyclerView? = null
        private set

    private var prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private var prefSortType: Sorter.Type = Sorter.Type.valueOf(
        prefs.getString(
            "S" + FileOpUtils.getAdapterType(this).toString(),
            Sorter.Type.None.toString()
        )!!
    )

    private var gridPaddingDecoration = GridPaddingDecoration(context)

    private var prefLayoutType: LayoutType = LayoutType.valueOf(
        prefs.getString(
            "L" + FileOpUtils.getAdapterType(this).toString(),
            LayoutType.NONE.toString()
        )!!
    )

    var layoutType: LayoutType? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == LayoutType.GRID && value != LayoutType.GRID) {
                recyclerView?.removeItemDecoration(gridPaddingDecoration)
            }
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
                if (recyclerView != null) {
                    applyLayoutManager()
                }
            }
            notifyDataSetChanged() // we change view type for all items
        }
    private var reverseRaw = false
    var sortType: Sorter.Type
        get() = if (comparator == null && rawOrderExposed)
                (if (reverseRaw) Sorter.Type.NativeOrderDescending else Sorter.Type.NativeOrder)
        else comparator?.type!!
        private set(value) {
            reverseRaw = value == Sorter.Type.NativeOrderDescending
            if (comparator?.type != value) {
                comparator = sorter.getComparator(value)
            }
        }
    val sortTypes: Set<Sorter.Type>
        get() = if (canSort) sorter.getSupportedTypes() else setOf(Sorter.Type.None)

    init {
        sortType =
            if (prefSortType != Sorter.Type.None && prefSortType != initialSortType
                && sortTypes.contains(prefSortType) && !isSubFragment)
                prefSortType
            else
                initialSortType
        liveData?.value?.let { updateList(it, now = true, canDiff = false) }
        layoutType =
            if (prefLayoutType != LayoutType.NONE && prefLayoutType != defaultLayoutType && !isSubFragment)
                prefLayoutType
            else
                defaultLayoutType
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
            recyclerView.setHasFixedSize(true)
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
        if (layoutType == LayoutType.GRID) {
            recyclerView.removeItemDecoration(gridPaddingDecoration)
        }
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
        if (layoutType == LayoutType.GRID) {
            recyclerView?.addItemDecoration(gridPaddingDecoration)
        }
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
        updateList(null, now = false, canDiff = true)
    }

    private fun sort(srcList: List<T>? = null, canDiff: Boolean, now: Boolean): () -> () -> Unit {
        // Ensure rawList is only accessed on UI thread
        // and ensure calls to this method go in order
        // to prevent funny IndexOutOfBoundsException crashes
        val newList = ArrayList(srcList ?: rawList)
        if (!listLock.tryAcquire()) {
            throw IllegalStateException("listLock already held, add now = true to the caller")
        }
        return {
            val apply = try {
                sortInner(newList, canDiff, now)
            } catch (e: Exception) {
                listLock.release()
                throw e
            }
            {
                try {
                    if (srcList != null) {
                        rawList.clear()
                        rawList.addAll(srcList)
                    }
                    apply()
                } finally {
                    listLock.release()
                }
            }
        }
    }

    private fun sortInner(newList: ArrayList<T>, canDiff: Boolean, now: Boolean): () -> Unit {
        // Sorting in the background using coroutines
        if (sortType == Sorter.Type.NativeOrderDescending) {
            newList.reverse()
        } else if (sortType != Sorter.Type.NativeOrder) {
            newList.sortWith { o1, o2 ->
                if (isPinned(o1) && !isPinned(o2)) -1
                else if (!isPinned(o1) && isPinned(o2)) 1
                else comparator?.compare(o1, o2) ?: 0
            }
        }
        return updateListSorted(newList, canDiff, now)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateListSorted(newList: MutableList<T>, canDiff: Boolean, now: Boolean): () -> Unit {
        val diffResult = if (((list.isNotEmpty() && newList.size != 0) || allowDiffUtils) && canDiff)
            DiffUtil.calculateDiff(SongDiffCallback(list, newList)) else null
        return {
            list.clear()
            list.addAll(newList)
            if (diffResult != null) diffResult.dispatchUpdatesTo(this) else notifyDataSetChanged()
            if (!now) decorAdapter.updateSongCounter()
        }
    }

    fun updateList(newList: List<T>? = null, now: Boolean, canDiff: Boolean) {
        val doSort = sort(newList, canDiff, now)
        if (now || bgHandler == null) doSort()()
        else {
            bgHandler!!.post {
                val apply = doSort()
                handler.post {
                    apply()
                }
            }
        }
    }

    protected open fun createDecorAdapter(): BaseDecorAdapter<out BaseAdapter<T>> {
        return BaseDecorAdapter(this, pluralStr, isSubFragment)
    }

    override fun getItemViewType(position: Int): Int {
        return when (layoutType) {
            LayoutType.GRID -> R.layout.adapter_grid_card
            LayoutType.COMPACT_LIST -> R.layout.adapter_list_card
            LayoutType.LIST, null -> R.layout.adapter_list_card_larger
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(
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

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(context.applicationContext).clear(holder.songCover)
    }

    private fun toId(item: T): String {
        return sorter.sortingHelper.getId(item)
    }

    private fun titleOf(item: T): String? {
        return if (sorter.sortingHelper.canGetTitle())
            sorter.sortingHelper.getTitle(item) else "null"
    }

    protected abstract fun virtualTitleOf(item: T): String
    private fun subTitleOf(item: T): String {
        return if (sorter.sortingHelper.canGetArtist())
            sorter.sortingHelper.getArtist(item) ?: context.getString(R.string.unknown_artist)
        else if (sorter.sortingHelper.canGetSize()) {
            val s = sorter.sortingHelper.getSize(item)
            return context.resources.getQuantityString(
                R.plurals.songs, s, s
            )
        } else "null"
    }

    private fun coverOf(item: T): Uri? {
        return sorter.sortingHelper.getCover(item)
    }

    protected abstract fun onClick(item: T)
    protected abstract fun onMenu(item: T, popupMenu: PopupMenu)
    private fun isPinned(item: T): Boolean {
        return titleOf(item) == null
    }

    private inner class SongDiffCallback(
        private val oldList: List<T>,
        private val newList: List<T>
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
        // 1 == decorAdapter.itemCount
        // if this crashes with IndexOutOfBoundsException, list access isn't guarded enough?
        // lib only ever gets popup text for what RecyclerView believes to be the first view
        return (if (position >= 1)
            sorter.getFastScrollHintFor(list[position - 1], sortType)
        else null) ?: "-"
    }

    enum class LayoutType {
        NONE, LIST, COMPACT_LIST, GRID
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