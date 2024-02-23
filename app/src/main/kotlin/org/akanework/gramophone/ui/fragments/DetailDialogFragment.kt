package org.akanework.gramophone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getFile
import org.akanework.gramophone.logic.utils.CalculationUtils.convertDurationToTimeStamp
import org.akanework.gramophone.ui.LibraryViewModel

class DetailDialogFragment : BaseFragment(false) {

    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.dialog_info_song, container, false)
        val collapsingToolbarLayout = rootView.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        val appBarLayout = rootView.findViewById<AppBarLayout>(R.id.appBarLayout)
        val nestedScrollView = rootView.findViewById<NestedScrollView>(R.id.scrollView)

        // https://github.com/material-components/material-components-android/issues/1310
        ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbarLayout, null)

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
            v.updatePadding(inset.left, inset.top, inset.right, 0)
            return@setOnApplyWindowInsetsListener insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView) { v, insets ->
            val inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()))
            v.updatePadding(inset.left, 0, inset.right, inset.bottom)
            return@setOnApplyWindowInsetsListener insets
        }

        val mediaMetadata = libraryViewModel.mediaItemList.value!![requireArguments().getInt("Position")].mediaMetadata
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
        Glide
            .with(requireContext())
            .load(mediaMetadata.artworkUri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.ic_default_cover)
            .into(albumCoverImageView)
        titleTextView.text = mediaMetadata.title
        artistTextView.text = mediaMetadata.artist
        albumTextView.text = mediaMetadata.albumTitle
        if (mediaMetadata.albumArtist != null) {
            albumArtistTextView.text = mediaMetadata.albumArtist
        }
        discNumberTextView.text = mediaMetadata.discNumber.toString()
        trackNumberTextView.text = mediaMetadata.trackNumber.toString()
        if (mediaMetadata.genre != null) {
            genreTextView.text = mediaMetadata.genre
        }
        if (mediaMetadata.releaseYear != null) {
            yearTextView.text = mediaMetadata.releaseYear.toString()
        }
        durationTextView.text = convertDurationToTimeStamp(mediaMetadata.extras!!.getLong("Duration"))
        mimeTypeTextView.text = mediaMetadata.extras!!.getString("MimeType")
        pathTextView.text = libraryViewModel.mediaItemList.value!![requireArguments().getInt("Position")].getFile()?.path
        return rootView
    }
}