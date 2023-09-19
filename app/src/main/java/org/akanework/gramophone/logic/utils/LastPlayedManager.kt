package org.akanework.gramophone.logic.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import java.nio.charset.StandardCharsets

@androidx.annotation.OptIn(UnstableApi::class)
class LastPlayedManager(private val context: Context, private val mediaSession: MediaSession) {

	private val prefs = context.getSharedPreferences("LastPlayedManager", 0)

	private fun dumpPlaylist(): MediaItemsWithStartPosition {
		val player = mediaSession.player
		val items = mutableListOf<MediaItem>()
		for (i in 0 until player.mediaItemCount) {
			items.add(player.getMediaItemAt(i))
		}
		return MediaItemsWithStartPosition(
			items, player.currentMediaItemIndex, player.currentPosition
		)
	}

	fun save() {
		val data = dumpPlaylist()
		val editor = prefs.edit()
		val lastPlayed = PrefsListUtils.dump(
			data.mediaItems.map {
				val b = SafeDelimitedStringConcat(":")
				// add new entries at the bottom and remember they are null for upgrade path
				b.writeStringUnsafe(it.mediaId)
				b.writeUri(it.localConfiguration?.uri)
				b.writeStringSafe(it.localConfiguration?.mimeType)
				b.writeStringSafe(it.mediaMetadata.title)
				b.writeStringSafe(it.mediaMetadata.artist)
				b.writeStringSafe(it.mediaMetadata.albumTitle)
				b.writeStringSafe(it.mediaMetadata.albumArtist)
				b.writeUri(it.mediaMetadata.artworkUri)
				b.writeInt(it.mediaMetadata.trackNumber)
				b.writeInt(it.mediaMetadata.discNumber)
				b.writeInt(it.mediaMetadata.recordingYear)
				b.writeInt(it.mediaMetadata.releaseYear)
				b.writeBool(it.mediaMetadata.isBrowsable)
				b.writeBool(it.mediaMetadata.isPlayable)
				b.writeLong(it.mediaMetadata.extras?.getLong("AddDate"))
				b.toString()
			})
		editor.putStringSet("last_played_lst", lastPlayed.first)
		editor.putString("last_played_grp", lastPlayed.second)
		editor.putInt("last_played_idx", data.startIndex)
		editor.putLong("last_played_pos", data.startPositionMs)
		editor.putInt("repeat_mode", mediaSession.player.repeatMode)
		editor.putBoolean("shuffle", mediaSession.player.shuffleModeEnabled)
		editor.putFloat("speed", mediaSession.player.playbackParameters.speed)
		editor.putFloat("pitch", mediaSession.player.playbackParameters.pitch)
		editor.apply()
	}

	fun restore(): MediaItemsWithStartPosition? {
		val lastPlayedLst = prefs.getStringSet("last_played_lst", null)
		val lastPlayedGrp = prefs.getString("last_played_grp", null)
		val lastPlayedIdx = prefs.getInt("last_played_idx", 0)
		val lastPlayedPos = prefs.getLong("last_played_pos", 0)
		if (lastPlayedGrp == null || lastPlayedLst == null) {
			return null
		}
		mediaSession.player.repeatMode = prefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF)
		mediaSession.player.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
		mediaSession.player.playbackParameters = PlaybackParameters(
			prefs.getFloat("speed", 1f),
			prefs.getFloat("pitch", 1f)
		)
		return MediaItemsWithStartPosition(
			PrefsListUtils.parse(lastPlayedLst, lastPlayedGrp)
				.map {
					val b = SafeDelimitedStringDecat(":", it)
					val mediaId = b.readStringUnsafe()
					val uri = b.readUri()
					val mimeType = b.readStringSafe()
					val title = b.readStringSafe()
					val artist = b.readStringSafe()
					val album = b.readStringSafe()
					val albumArtist = b.readStringSafe()
					val imgUri = b.readUri()
					val trackNumber = b.readInt()
					val discNumber = b.readInt()
					val recordingYear = b.readInt()
					val releaseYear = b.readInt()
					val isBrowsable = b.readBool()
					val isPlayable = b.readBool()
					val addDate = b.readLong()
					MediaItem.Builder()
						.setUri(uri)
						.setMediaId(mediaId!!)
						.setMimeType(mimeType)
						.setMediaMetadata(
							MediaMetadata
								.Builder()
								.setTitle(title)
								.setArtist(artist)
								.setAlbumTitle(album)
								.setAlbumArtist(albumArtist)
								.setArtworkUri(imgUri)
								.setTrackNumber(trackNumber)
								.setDiscNumber(discNumber)
								.setRecordingYear(recordingYear)
								.setReleaseYear(releaseYear)
								.setIsBrowsable(isBrowsable)
								.setIsPlayable(isPlayable)
								.setExtras(Bundle().apply {
									if (addDate != null) {
										putLong("AddDate", addDate)
									}
								})
								.build())
						.build()
				},
			lastPlayedIdx,
			lastPlayedPos
		)
	}
}

private class SafeDelimitedStringConcat(private val delimiter: String) {
	private val b = StringBuilder()
	private var hadFirst = false

	private fun append(s: String?) {
		if (s?.contains(delimiter, false) == true) {
			throw IllegalArgumentException("argument must not contain delimiter")
		}
		if (hadFirst) {
			b.append(delimiter)
		} else {
			hadFirst = true
		}
		s?.let { b.append(it) }
	}

	override fun toString(): String {
		return b.toString()
	}

	fun writeStringUnsafe(s: CharSequence?) = append(s?.toString())
	fun writeBase64(b: ByteArray?) = append(b?.let { Base64.encodeToString(it, Base64.DEFAULT) })
	fun writeStringSafe(s: CharSequence?) = writeBase64(s?.toString()?.toByteArray(StandardCharsets.UTF_8))
	fun writeInt(i: Int?) = append(i?.toString())
	fun writeLong(i: Long?) = append(i?.toString())
	fun writeBool(b: Boolean?) = append(b?.toString())
	fun writeUri(u: Uri?) = writeStringSafe(u?.toString())
}

private class SafeDelimitedStringDecat(delimiter: String, str: String) {
	private val items = str.split(delimiter)
	private var pos = 0

	private fun read(): String? {
		if (pos == items.size) return null
		return items[pos++].ifEmpty { null }
	}

	fun readStringUnsafe(): String? = read()
	fun readBase64(): ByteArray? = read()?.let { Base64.decode(it, Base64.DEFAULT) }
	fun readStringSafe(): String? = readBase64()?.toString(StandardCharsets.UTF_8)
	fun readInt(): Int? = read()?.toInt()
	fun readLong(): Long? = read()?.toLong()
	fun readBool(): Boolean? = read()?.toBooleanStrict()
	fun readUri(): Uri? = Uri.parse(readStringSafe())
}

private object PrefsListUtils {
	fun parse(stringSet: Set<String>, groupStr: String): List<String> {
		val groups = groupStr.split(",")
		return stringSet.sortedBy { groups.indexOf(it.hashCode().toString()) }
	}

	fun dump(list: List<String>): Pair<Set<String>, String> {
		return Pair(list.toSet(), list.joinToString(",") { it.hashCode().toString() })
	}
}