package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.getFile
import org.akanework.gramophone.logic.ui.placeholderScaleToFit
import org.akanework.gramophone.logic.utils.CalculationUtils.convertDurationToTimeStamp
import org.akanework.gramophone.ui.LibraryViewModel

class DetailDialogFragment : BaseFragment(false) {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_info_song, container, false)
        rootView.findViewById<AppBarLayout>(R.id.appbarlayout).enableEdgeToEdgePaddingListener()
        rootView.findViewById<View>(R.id.scrollView).enableEdgeToEdgePaddingListener()
        rootView.findViewById<MaterialToolbar>(R.id.topAppBar).setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        val mediaItem = libraryViewModel.mediaItemList.value!![requireArguments().getInt("Position")]
        val mediaMetadata = mediaItem.mediaMetadata
        val albumCoverImageView = rootView.findViewById<ImageView>(R.id.album_cover)
        val titleTextView = rootView.findViewById<TextView>(R.id.title)
        val artistTextView = rootView.findViewById<TextView>(R.id.artist)
        val albumArtistTextView = rootView.findViewById<TextView>(R.id.album_artist)
        val discNumberTextView = rootView.findViewById<TextView>(R.id.disc_number)
        val trackNumberTextView = rootView.findViewById<TextView>(R.id.track_num)
        val genreTextView = rootView.findViewById<TextView>(R.id.genre)
        val yearTextView = rootView.findViewById<TextView>(R.id.date)
        val albumTextView = rootView.findViewById<TextView>(R.id.album)
        val durationTextView = rootView.findViewById<TextView>(R.id.duration)
        val mimeTypeTextView = rootView.findViewById<TextView>(R.id.mime)
        val pathTextView = rootView.findViewById<TextView>(R.id.path)
        albumCoverImageView.load(mediaMetadata.artworkUri) {
            placeholderScaleToFit(R.drawable.ic_default_cover)
            crossfade(true)
            error(R.drawable.ic_default_cover)
        }
        titleTextView.text = mediaMetadata.title
        artistTextView.text = mediaMetadata.artist
        albumTextView.text = mediaMetadata.albumTitle
        if (mediaMetadata.albumArtist != null) {
            albumArtistTextView.text = mediaMetadata.albumArtist
        }
        discNumberTextView.text = mediaMetadata.discNumber?.toString()
        trackNumberTextView.text = mediaMetadata.trackNumber?.toString()
        if (mediaMetadata.genre != null) {
            genreTextView.text = mediaMetadata.genre
        }
        if (mediaMetadata.releaseYear != null || mediaMetadata.recordingYear != null) {
            yearTextView.text = (mediaMetadata.releaseYear ?: mediaMetadata.recordingYear)?.toString()
        }
        durationTextView.text = convertDurationToTimeStamp(mediaMetadata.extras!!.getLong("Duration"))
        mimeTypeTextView.text = mediaItem.localConfiguration?.mimeType ?: "(null)"
        pathTextView.text = mediaItem.getFile()?.path
            ?: mediaItem.requestMetadata.mediaUri?.toString() ?: "(null)"
        return rootView
    }
}