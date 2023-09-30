package org.akanework.gramophone.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.fragments.AdapterFragment

/**
 * This is the ViewPager2 adapter.
 */
class ViewPager2Adapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    companion object {
        val tabs: ArrayList</* res id */ Int> = arrayListOf(
            R.id.songs,
            R.id.albums,
            R.id.artists,
            R.id.genres,
            R.id.dates,
            R.id.folders,
            R.id.playlists
        )

        fun getLabelResId(position: Int): Int =
            when (tabs[position]) {
                R.id.songs -> R.string.category_songs
                R.id.albums -> R.string.category_albums
                R.id.artists -> R.string.category_artists
                R.id.genres -> R.string.category_genres
                R.id.dates -> R.string.category_dates
                R.id.folders -> R.string.folders
                R.id.playlists -> R.string.category_playlists
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
    }

    override fun getItemCount(): Int = tabs.count()

    override fun createFragment(position: Int): Fragment =
        when (tabs[position]) {
            R.id.songs -> AdapterFragment { m, v ->
                SongAdapter(m, v.mediaItemList, true, null, true)
            }
            R.id.albums -> AdapterFragment { m, v -> AlbumAdapter(m, v.albumItemList) }
            R.id.artists -> AdapterFragment { m, v ->
                ArtistAdapter(m, v.artistItemList, v.albumArtistItemList)
            }
            R.id.genres -> AdapterFragment { m, v -> GenreAdapter(m, v.genreItemList) }
            R.id.dates -> AdapterFragment { m, v -> GenreAdapter(m, v.genreItemList) }
            R.id.folders -> AdapterFragment { m, v -> FolderAdapter(m, v.folderStructure) }
            R.id.playlists -> AdapterFragment { m, v -> PlaylistAdapter(m, v.playlistList) }
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
}
