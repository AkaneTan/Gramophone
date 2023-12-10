package org.akanework.gramophone.logic.lrcdecode

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import org.akanework.gramophone.logic.utils.MediaStoreUtils
import java.io.File
import java.nio.charset.Charset
import kotlin.system.measureTimeMillis

@OptIn(UnstableApi::class)
fun extractAndParseLyrics(musicFile: File?, metadata: Metadata): List<MediaStoreUtils.Lyric>? {
	return extractLyrics(musicFile, metadata)?.let { parseLrcString(it) }
}

@OptIn(UnstableApi::class)
fun extractLyrics(musicFile: File?, metadata: Metadata): String? {
	val lrcFile = musicFile
		?.let { File(musicFile.parentFile, musicFile.nameWithoutExtension + ".lrc") }
	if (lrcFile?.exists() == true) {
		return extractLrcFile(lrcFile)
	}
	for (i in 0 until metadata.length()) {
		val metadataEntry = metadata.get(i)
		if (metadataEntry is BinaryFrame && metadataEntry.id == "USLT") {
			val ba = metadataEntry.data
			return UsltFrameDecoder.decode(ParsableByteArray(ba), ba.size)
		}
	}
	return null
}

private fun extractLrcFile(lrcFile: File): String {
	return lrcFile.readBytes().toString(Charset.defaultCharset())
}

private fun parseLrcString(lrcContent: String): List<MediaStoreUtils.Lyric> {
	val linesRegex = "\\[(\\d{2}:\\d{2}\\.\\d{2})](.*)".toRegex()
	val list = mutableListOf<MediaStoreUtils.Lyric>()
	//val measureTime = measureTimeMillis {
		lrcContent.lines().forEach { line ->
			linesRegex.find(line)?.let { matchResult ->
				val startTime = parseTime(matchResult.groupValues[1])
				val lyricLine = matchResult.groupValues[2]
				val insertIndex = list.binarySearch { it.timeStamp.compareTo(startTime) }
				if (insertIndex < 0) {
					list.add(MediaStoreUtils.Lyric(startTime, lyricLine, false))
				} else {
					list.add(insertIndex + 1, MediaStoreUtils.Lyric(startTime, lyricLine, true))
				}
			}
		}
	//}
	return list
}

private fun parseTime(timeString: String): Long {
	val timeRegex = "(\\d{2}):(\\d{2})\\.(\\d{2})".toRegex()
	val matchResult = timeRegex.find(timeString)

	val minutes = matchResult?.groupValues?.get(1)?.toLongOrNull() ?: 0
	val seconds = matchResult?.groupValues?.get(2)?.toLongOrNull() ?: 0
	val milliseconds = matchResult?.groupValues?.get(3)?.toLongOrNull() ?: 0

	return minutes * 60000 + seconds * 1000 + milliseconds * 10
}