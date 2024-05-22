package org.akanework.gramophone.logic.utils

import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.Metadata
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.io.File
import java.nio.charset.Charset
import kotlin.math.pow

object LrcUtils {

    private const val TAG = "LrcUtils"

    @OptIn(UnstableApi::class)
    fun extractAndParseLyrics(metadata: Metadata, trim: Boolean, multilineEnable: Boolean): MutableList<MediaStoreUtils.Lyric>? {
        for (i in 0..<metadata.length()) {
            val meta = metadata.get(i)
            val data =
                if (meta is VorbisComment && meta.key == "LYRICS") // ogg / flac
                    meta.value
                else if (meta is BinaryFrame && (meta.id == "USLT" || meta.id == "SYLT")) // mp3 / other id3 based
                    UsltFrameDecoder.decode(ParsableByteArray(meta.data))
                else if (meta is TextInformationFrame && (meta.id == "USLT" || meta.id == "SYLT")) // m4a
                    meta.values.joinToString("\n")
                else null
            val lyrics = data?.let {
                try {
                    parseLrcString(it, trim, multilineEnable)
                } catch (e: Exception) {
                    Log.e(TAG, Log.getStackTraceString(e))
                    null
                }
            }
            return lyrics ?: continue
        }
        return null
    }

    @OptIn(UnstableApi::class)
    fun loadAndParseLyricsFile(musicFile: File?, trim: Boolean, multilineEnable: Boolean): MutableList<MediaStoreUtils.Lyric>? {
        val lrcFile = musicFile?.let { File(it.parentFile, it.nameWithoutExtension + ".lrc") }
        return loadLrcFile(lrcFile)?.let {
            try {
                parseLrcString(it, trim, multilineEnable)
            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
                null
            } }
    }

    private fun loadLrcFile(lrcFile: File?): String? {
        return try {
            if (lrcFile?.exists() == true)
                lrcFile.readBytes().toString(Charset.defaultCharset())
            else null
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: Log.getStackTraceString(e))
            null
        }
    }


    /*
     * Formats we have to consider in this method are:
     *  - Simple LRC files (ref Wikipedia) ex: [00:11.22] hello i am lyric
     *  - "compressed LRC" with >1 tag for repeating line ex: [00:11.22][00:15.33] hello i am lyric
     *  - Invalid LRC with all-zero tags [00:00.00] hello i am lyric
     *  - Lyrics that aren't synced and have no tags at all
     *  - Translations, type 1 (ex: pasting first japanese and then english lrc file into one file)
     *  - Translations, type 2 (ex: translated line directly under previous non-translated line)
     *  - The timestamps can variate in the following ways: [00:11] [00:11:22] [00:11.22] [00:11.222] [00:11:222]
     *
     * Multiline format:
     * - This technically isn't part of any listed guidelines, however is allows for
     *      reading of otherwise discarded lyrics
     * - All the lines between sync point A and B are read as lyric text of A
     *
     * In the future, we also want to support:
     *  - Extended LRC (ref Wikipedia) ex: [00:11.22] <00:11.22> hello <00:12.85> i am <00:13.23> lyric
     *  - Wakaloke gender extension (ref Wikipedia)
     *  - [offset:] tag in header (ref Wikipedia)
     * We completely ignore all ID3 tags from the header as MediaStore is our source of truth.
     */
    @VisibleForTesting
    fun parseLrcString(lrcContent: String, trim: Boolean, multilineEnable: Boolean): MutableList<MediaStoreUtils.Lyric>? {
        if (lrcContent.isBlank()) return null
        val timeMarksRegex = "\\[(\\d{2}:\\d{2})([.:]\\d+)?]".toRegex()
        val list = mutableListOf<MediaStoreUtils.Lyric>()
        var foundNonNull = false
        var lyricsText: StringBuilder? = StringBuilder()
        //val measureTime = measureTimeMillis {
        // Add all lines found on LRC (probably will be unordered because of "compression" or translation type)
        lrcContent.lines().forEach { line ->
            timeMarksRegex.findAll(line).let { sequence ->
                if (sequence.count() == 0) {
                    return@let
                }
                var lyricLine : String
                sequence.forEach { match ->
                    val firstSync = match.groupValues.subList(1, match.groupValues.size)
                        .joinToString("")

                    val ts = parseTime(firstSync)
                    if (!foundNonNull && ts > 0) {
                        foundNonNull = true
                        lyricsText = null
                    }

                    if (multilineEnable) {
                        val startIndex = lrcContent.indexOf(line) + firstSync.length+1
                        var endIndex = lrcContent.length // default to end
                        var nextSync = ""

                        // track next sync point if found
                        if (timeMarksRegex.find(lrcContent, startIndex)?.value != null) {
                            nextSync = timeMarksRegex.find(lrcContent, startIndex)?.value!!
                            endIndex = lrcContent.indexOf(nextSync) - 1 // delete \n at end
                        }

                        // read as single line *IF* this is a single line lyric
                        if (nextSync == "[$firstSync]") {
                            lyricLine = line.substring(sequence.last().range.last + 1)
                                .let { if (trim) it.trim() else it }
                        }
                        else {
                            lyricLine = lrcContent.substring(startIndex + 1, endIndex)
                                .let { if (trim) it.trim() else it }
                        }
                    }
                    else {
                        lyricLine = line.substring(sequence.last().range.last + 1)
                            .let { if (trim) it.trim() else it }
                    }

                    lyricsText?.append(lyricLine + "\n")
                    list.add(MediaStoreUtils.Lyric(ts, lyricLine))
                }
            }
        }
        // Sort and mark as translations all found duplicated timestamps (usually one)
        list.sortBy { it.timeStamp }
        var previousTs = -1L
        list.forEach {
            it.isTranslation = (it.timeStamp!! == previousTs)
            previousTs = it.timeStamp
        }
        //}
        if (list.isEmpty() && lrcContent.isNotEmpty()) {
            list.add(MediaStoreUtils.Lyric(null, lrcContent, false))
        } else if (!foundNonNull) {
            list.clear()
            list.add(MediaStoreUtils.Lyric(null, lyricsText!!.toString(), false))
        }
        return list
    }

    private fun parseTime(timeString: String): Long {
        val timeRegex = "(\\d{2}):(\\d{2})[.:](\\d+)".toRegex()
        val matchResult = timeRegex.find(timeString)

        val minutes = matchResult?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val seconds = matchResult?.groupValues?.get(2)?.toLongOrNull() ?: 0
        val millisecondsString = matchResult?.groupValues?.get(3)
        // if one specifies micro/pico/nano/whatever seconds for some insane reason,
        // scrap the extra information
        val milliseconds = (millisecondsString?.substring(0,
            millisecondsString.length.coerceAtMost(3)) ?.toLongOrNull() ?: 0) *
                10f.pow(3 - (millisecondsString?.length ?: 0)).toLong()

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