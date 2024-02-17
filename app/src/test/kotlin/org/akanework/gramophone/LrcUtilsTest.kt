package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.LrcUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcUtilsTest {

	@Test
	fun emptyInEmptyOut() {
		assertTrue(LrcUtils.parseLrcString("").isEmpty())
	}

}