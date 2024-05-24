package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.LrcUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcUtilsTest {

	@Test
	fun emptyInEmptyOut() {
		val emptyLrc = LrcUtils.parseLrcString("", trim = false,
			multilineEnable = false)
		assertTrue(emptyLrc == null)
	}

}