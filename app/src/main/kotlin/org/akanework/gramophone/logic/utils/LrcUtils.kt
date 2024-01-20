package org.akanework.gramophone.logic.utils

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.io.File
import java.nio.charset.Charset
import kotlin.math.pow

object LrcUtils {
    @OptIn(UnstableApi::class)
    fun extractAndParseLyrics(musicFile: File?, metadata: Metadata): MutableList<MediaStoreUtils.Lyric>? {
        return extractLyrics(musicFile, metadata)?.let { parseLrcString(it) }
    }

    @OptIn(UnstableApi::class)
    fun extractLyrics(musicFile: File?, metadata: Metadata): String? {
        val lrcFile = musicFile
            ?.let { File(musicFile.parentFile, musicFile.nameWithoutExtension + ".lrc") }
        if (lrcFile?.exists() == true) {
            return extractLrcFile(lrcFile)
        }
        for (i in 0..<metadata.length()) {
            val meta = metadata.get(i)
            if (meta is VorbisComment && meta.key == "LYRICS") // ogg / flac
                return meta.value
            if (meta is BinaryFrame && meta.id == "USLT") // mp3 / other id3 based
                return UsltFrameDecoder.decode(ParsableByteArray(meta.data))
        }
        return null
    }

    private fun extractLrcFile(lrcFile: File): String {
        return lrcFile.readBytes().toString(Charset.defaultCharset())
    }

    private fun parseLrcString(lrcContent: String): MutableList<MediaStoreUtils.Lyric> {
        val linesRegex = "\\[(\\d{2}:\\d{2}\\.\\d+)](.*)".toRegex()
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
        val timeRegex = "(\\d{2}):(\\d{2})\\.(\\d+)".toRegex()
        val matchResult = timeRegex.find(timeString)

        val minutes = matchResult?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val seconds = matchResult?.groupValues?.get(2)?.toLongOrNull() ?: 0
        val millisecondsString = matchResult?.groupValues?.get(3)
        val milliseconds = (millisecondsString?.toLongOrNull() ?: 0) *
                10f.pow(3 - (millisecondsString?.length ?: 0)).toInt()

        return minutes * 60000 + seconds * 1000 + milliseconds
    }
}

// Class heavily based on MIT-licensed https://github.com/yoheimuta/ExoPlayerMusic/blob/77cfb989b59f6906b1170c9b2d565f9b8447db41/app/src/main/java/com/github/yoheimuta/amplayer/playback/UsltFrameDecoder.kt
// See http://id3.org/id3v2.4.0-frames
@OptIn(UnstableApi::class)
private class UsltFrameDecoder {
    companion object {
        private const val ID3_TEXT_ENCODING_ISO_8859_1 = 0
        private const val ID3_TEXT_ENCODING_UTF_16 = 1
        private const val ID3_TEXT_ENCODING_UTF_16BE = 2
        private const val ID3_TEXT_ENCODING_UTF_8 = 3

        fun decode(id3Data: ParsableByteArray): String? {
            if (id3Data.limit() < 4) {
                // Frame is malformed.
                return null
            }

            val encoding = id3Data.readUnsignedByte()
            val charset = getCharsetName(encoding)

            val lang = ByteArray(3)
            id3Data.readBytes(lang, 0, 3) // language
            val rest = ByteArray(id3Data.limit() - 4)
            id3Data.readBytes(rest, 0, id3Data.limit() - 4)

            val descriptionEndIndex = indexOfEos(rest, 0, encoding)
            val textStartIndex = descriptionEndIndex + delimiterLength(encoding)
            val textEndIndex = indexOfEos(rest, textStartIndex, encoding)
            return decodeStringIfValid(rest, textStartIndex, textEndIndex, charset)
        }

        private fun getCharsetName(encodingByte: Int): Charset {
            val name = when (encodingByte) {
                ID3_TEXT_ENCODING_UTF_16 -> "UTF-16"
                ID3_TEXT_ENCODING_UTF_16BE -> "UTF-16BE"
                ID3_TEXT_ENCODING_UTF_8 -> "UTF-8"
                ID3_TEXT_ENCODING_ISO_8859_1 -> "ISO-8859-1"
                else -> "ISO-8859-1"
            }
            return Charset.forName(name)
        }

        private fun indexOfEos(data: ByteArray, fromIndex: Int, encoding: Int): Int {
            var terminationPos = indexOfZeroByte(data, fromIndex)

            // For single byte encoding charsets, we're done.
            if (encoding == ID3_TEXT_ENCODING_ISO_8859_1 || encoding == ID3_TEXT_ENCODING_UTF_8) {
                return terminationPos
            }

            // Otherwise ensure an even index and look for a second zero byte.
            while (terminationPos < data.size - 1) {
                if (terminationPos % 2 == 0 && data[terminationPos + 1] == 0.toByte()) {
                    return terminationPos
                }
                terminationPos = indexOfZeroByte(data, terminationPos + 1)
            }

            return data.size
        }

        private fun indexOfZeroByte(data: ByteArray, fromIndex: Int): Int {
            for (i in fromIndex until data.size) {
                if (data[i] == 0.toByte()) {
                    return i
                }
            }
            return data.size
        }

        private fun delimiterLength(encodingByte: Int): Int {
            return if (encodingByte == ID3_TEXT_ENCODING_ISO_8859_1 || encodingByte == ID3_TEXT_ENCODING_UTF_8)
                1
            else
                2
        }

        private fun decodeStringIfValid(data: ByteArray, from: Int, to: Int, charset: Charset): String {
            return if (to <= from || to > data.size) {
                ""
            } else String(data, from, to - from, charset)
        }
    }
}