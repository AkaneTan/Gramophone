package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcUtilsTest {

	private fun parse(lrcContent: String, trim: Boolean? = null, multiline: Boolean? = null, mustSkip: Boolean? = false): SemanticLyrics? {
		if (trim == null) {
			val a = parse(lrcContent, true, multiline, mustSkip)
			val b = parse(lrcContent, false, multiline, mustSkip)
			assertFalse("trim true and false should result in same type of lyrics", a is SemanticLyrics.SyncedLyrics != b is SemanticLyrics.SyncedLyrics)
			if (a is SemanticLyrics.SyncedLyrics)
				assertEquals("trim true and false should result in same list for this string", (b as SemanticLyrics.SyncedLyrics).text, a.text)
			else
				assertEquals("trim true and false should result in same list for this string", b?.unsyncedText, a?.unsyncedText)
			return a
		}
		if (multiline == null) {
			val a = parse(lrcContent, trim, true, mustSkip)
			val b = parse(lrcContent, trim, false, mustSkip)
			assertFalse("multiline true and false should result in same type of lyrics (trim=$trim)", a is SemanticLyrics.SyncedLyrics != b is SemanticLyrics.SyncedLyrics)
			if (a is SemanticLyrics.SyncedLyrics)
				assertEquals("multiline true and false should result in same list for this string (trim=$trim)", (b as SemanticLyrics.SyncedLyrics).text, a.text)
			else
				assertEquals("multiline true and false should result in same list for this string (trim=$trim)", b?.unsyncedText, a?.unsyncedText)
			return a
		}
		val a = SemanticLyrics.parse(lrcContent, trim, multiline)
		if (mustSkip != null) {
			if (mustSkip) {
				assertTrue("excepted skip (trim=$trim multiline=$multiline)", a is SemanticLyrics.UnsyncedLyrics)
			} else {
				assertFalse("excepted no skip (trim=$trim multiline=$multiline)", a is SemanticLyrics.UnsyncedLyrics)
			}
		}
		return a
	}

	private fun parseSynced(lrcContent: String, trim: Boolean? = null, multiline: Boolean? = null): List<Pair<SemanticLyrics.LyricLine, Boolean>>? {
		return (parse(lrcContent, trim, multiline, mustSkip = false) as SemanticLyrics.SyncedLyrics?)?.text
	}

	private fun lyricArrayToString(lrc: List<Pair<SemanticLyrics.LyricLine, Boolean>>?): String {
		val str = StringBuilder("val testData = ")
		if (lrc == null) {
			str.appendLine("null")
		} else {
			str.appendLine("listOf(")
			for (j in lrc) {
				val i = j.first
				str.appendLine("\tPair(LyricLine(start = ${i.start}uL, text = \"\"\"${i.text}\"\"\", " +
						"words = ${i.words?.let { "listOf(${it.map { "SemanticLyrics.Word(" +
								"timeRange = ${it.timeRange.first}uL..${it.timeRange.last}uL, " +
								"charRange = ${it.charRange.first}uL..${it.charRange.last}uL)" }
							.joinToString()})" } ?: "null"}, speaker = ${i.speaker?.name?.let
						{ "SpeakerEntity.$it" } ?: "null"}), ${j.second}),")
			}
			str.appendLine(")")
		}
		return str.toString()
	}

	@Test
	fun emptyInEmptyOut() {
		val emptyLrc = parse("")
		assertNull(emptyLrc)
	}

	@Test
	fun blankInEmptyOut() {
		val blankLrc = parse("   \t  \n    \u00A0")
		assertNull(blankLrc)
	}

	@Test
	fun testPrintUtility() {
		val lrc = lyricArrayToString(parseSynced(LrcTestData.AS_IT_WAS))
		assertEquals(LrcTestData.AS_IT_WAS_PARSED_STR, lyricArrayToString(LrcTestData.AS_IT_WAS_PARSED))
		assertEquals(LrcTestData.AS_IT_WAS_PARSED_STR, lrc)
		val lrc2 = lyricArrayToString(parseSynced(LrcTestData.AM_I_DREAMING, trim = false))
		assertEquals(LrcTestData.AM_I_DREAMING_PARSED_NO_TRIM_STR, lyricArrayToString(LrcTestData.AM_I_DREAMING_PARSED_NO_TRIM))
		assertEquals(LrcTestData.AM_I_DREAMING_PARSED_NO_TRIM_STR, lrc2)
	}

	@Test
	fun testTemplateLrc1() {
		val lrc = parseSynced(LrcTestData.AS_IT_WAS)
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	/*
	 * Test the synthetic newline feature. If this is intentionally broken, it's not a big deal.
	 * But don't break it accidentally.
	 */
	@Test
	fun testTemplateLrcSyntheticNewlines() {
		val lrc = parseSynced(LrcTestData.AS_IT_WAS.replace("\n", ""))
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrc2() {
		val lrc = parseSynced(LrcTestData.AS_IT_WAS + "\n")
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED, lrc)
	}

	@Test
	fun testTemplateLrcTrimToggle() {
		val a = parseSynced(LrcTestData.AS_IT_WAS_NO_TRIM, trim = false)
		val b = parseSynced(LrcTestData.AS_IT_WAS_NO_TRIM, trim = true)
		assertNotEquals(b, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_FALSE, a)
		assertEquals(LrcTestData.AS_IT_WAS_NO_TRIM_PARSED_TRUE, b)
	}

	@Test
	fun testTemplateLrcTranslate2Compressed() {
		val lrc = parseSynced(LrcTestData.DREAM_THREAD)
		assertNotNull(lrc)
		assertEquals(LrcTestData.DREAM_THREAD_PARSED, lrc)
	}

	@Test
	fun testTemplateLrcZeroTimestamps() {
		val lrc = parse(LrcTestData.AS_IT_WAS.replace("\\[(\\d{2}):(\\d{2})([.:]\\d+)?]".toRegex(), "[00:00.00]"), mustSkip = true)
		assertNotNull(lrc)
		assertEquals(LrcTestData.AS_IT_WAS_PARSED.map { it.first.text }, lrc!!.unsyncedText)
	}

	@Test
	fun testSyntheticNewLineMultiLineParser() {
		val lrcS = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = false)
		assertNotNull(lrcS)
		val lrcM = parseSynced("[11:22.33]hello\ngood morning[33:44.55]how are you?", multiline = true)
		assertNotNull(lrcM)
		assertNotEquals(lrcS!!, lrcM!!)
		assertEquals(2, lrcS.size)
		assertEquals(2, lrcM.size)
		assertEquals("hello", lrcS[0].first.text)
		assertEquals("hello\ngood morning", lrcM[0].first.text)
		assertEquals("how are you?", lrcS[1].first.text)
		assertEquals("how are you?", lrcM[1].first.text)
	}

	@Test
	fun testSimpleMultiLineParser() {
		val lrcS = parseSynced("[11:22.33]hello\ngood morning\n[33:44.55]how are you?", multiline = false)
		assertNotNull(lrcS)
		val lrcM = parseSynced("[11:22.33]hello\ngood morning\n[33:44.55]how are you?", multiline = true)
		assertNotNull(lrcM)
		assertNotEquals(lrcS!!, lrcM!!)
		assertEquals(2, lrcS.size)
		assertEquals(2, lrcM.size)
		assertEquals("hello", lrcS[0].first.text)
		assertEquals("hello\ngood morning", lrcM[0].first.text)
		assertEquals("how are you?", lrcS[1].first.text)
		assertEquals("how are you?", lrcM[1].first.text)
	}

	@Test
	fun testOffsetMultiLineParser() {
		val lrc = parseSynced("[offset:+3][00:00.004]hello\ngood morning\n[00:00.005]how are you?", multiline = true)
		assertNotNull(lrc)
		assertEquals(2, lrc!!.size)
		assertEquals("hello\ngood morning", lrc[0].first.text)
		assertEquals(1uL, lrc[0].first.start)
		assertEquals("how are you?", lrc[1].first.text)
		assertEquals(2uL, lrc[1].first.start)
	}

	@Test
	fun testBogusOffsetMultiLineParser() {
		val lrc = parseSynced("[offset:+200][00:00.004]hello\ngood morning\n[00:00.005]how are you?", multiline = true)
		assertNotNull(lrc)
		assertEquals(2, lrc!!.size)
		assertEquals("hello\ngood morning", lrc[0].first.text)
		assertEquals(0uL, lrc[0].first.start)
		assertEquals("how are you?", lrc[1].first.text)
		assertEquals(0uL, lrc[1].first.start)
	}

	@Test
	fun testNegativeOffsetMultiLineParser() {
		val lrc = parseSynced("[offset:-200][00:00.004]hello\ngood morning\n[00:00.005]how are you?", multiline = true)
		assertNotNull(lrc)
		assertEquals(2, lrc!!.size)
		assertEquals("hello\ngood morning", lrc[0].first.text)
		assertEquals(204uL, lrc[0].first.start)
		assertEquals("how are you?", lrc[1].first.text)
		assertEquals(205uL, lrc[1].first.start)
	}

	@Test
	fun testDualOffsetMultiLineParser() {
		val lrc = parseSynced("[offset:-200][00:00.004]hello\ngood morning\n[offset:+3][00:00.005]how are you?", multiline = true)
		assertNotNull(lrc)
		assertEquals(2, lrc!!.size)
		// Order is swapped because second timestamp is smaller thanks to offset
		assertEquals("how are you?", lrc[0].first.text)
		assertEquals(2uL, lrc[0].first.start)
		assertEquals("hello\ngood morning", lrc[1].first.text)
		assertEquals(204uL, lrc[1].first.start)
	}

	@Test
	fun testOnlyWordSyncPoints() {
		val lrc = parseSynced("<00:00.02>a<00:01.00>l\n<00:03.00>b")
		assertNotNull(lrc)
		assertEquals(2, lrc!!.size)
		assertEquals("al", lrc[0].first.text)
		assertEquals(20uL, lrc[0].first.start)
		assertEquals("b", lrc[1].first.text)
		assertEquals(3000uL, lrc[1].first.start)
	}

	@Test
	fun testTemplateLrcTranslationType1() {
		val lrc = parseSynced(LrcTestData.ALL_STAR)
		assertNotNull(lrc)
		assertEquals(LrcTestData.ALL_STAR_PARSED, lrc)
	}

	@Test
	fun testTemplateLrcExtendedAppleTrimToggle() {
		val lrc = parseSynced(LrcTestData.AM_I_DREAMING, trim = false)
		val lrc2 = parseSynced(LrcTestData.AM_I_DREAMING, trim = true)
		assertNotNull(lrc)
		assertEquals(LrcTestData.AM_I_DREAMING_PARSED_NO_TRIM, lrc)
		assertEquals(LrcTestData.AM_I_DREAMING_PARSED_TRIM, lrc2)
	}

	@Test
	fun testTemplateLrcWakaloke() {
		val lrc = parseSynced(LrcTestData.WAKALOKE_TEST)
		assertNotNull(lrc)
		assertEquals(LrcTestData.WAKALOKE_TEST_PARSED, lrc)
	}

	@Test
	fun testParserSkippedHello() {
		parse("hello", mustSkip = true)
	}

	@Test
	fun testParserSkipped2() {
		parse("2", mustSkip = true)
	}
}