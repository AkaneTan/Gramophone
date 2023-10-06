package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.akanework.gramophone.ui.MainActivity
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.AlbumAdapter
import org.akanework.gramophone.ui.adapters.ArtistAdapter
import org.akanework.gramophone.ui.adapters.DateAdapter
import org.akanework.gramophone.ui.adapters.FolderAdapter
import org.akanework.gramophone.ui.adapters.GenreAdapter
import org.akanework.gramophone.ui.adapters.PlaylistAdapter
import org.akanework.gramophone.ui.adapters.SongAdapter
import org.akanework.gramophone.ui.viewmodels.LibraryViewModel

class AdapterFragment : BaseFragment(null) {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview, container, false)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = createAdapter(requireActivity() as MainActivity, libraryViewModel)
        recyclerView.adapter = adapter.concatAdapter
        FastScrollerBuilder(recyclerView).apply {
            setPopupTextProvider(adapter)
            build()
        }
        return rootView
    }

    private fun createAdapter(m: MainActivity, v: LibraryViewModel): BaseInterface<*> {
        return when (arguments?.getInt("ID", -1)) {
            R.id.songs -> SongAdapter(m, v.mediaItemList, true, null, true)
            R.id.albums -> AlbumAdapter(m, v.albumItemList)
            R.id.artists -> ArtistAdapter(m, v.artistItemList, v.albumArtistItemList)
            R.id.genres -> GenreAdapter(m, v.genreItemList)
            R.id.dates -> DateAdapter(m, v.dateItemList)
            R.id.folders -> FolderAdapter(m, v.folderStructure)
            R.id.playlists -> PlaylistAdapter(m, v.playlistList)
            -1, null -> throw IllegalArgumentException("unset ID value")
            else -> throw IllegalArgumentException("invalid ID value")
        }
    }

    abstract class BaseInterface<T : RecyclerView.ViewHolder>
        : RecyclerView.Adapter<T>(), PopupTextProvider {
        abstract val concatAdapter: ConcatAdapter
    }
}
