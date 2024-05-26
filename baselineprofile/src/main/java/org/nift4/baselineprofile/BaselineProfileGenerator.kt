package org.nift4.baselineprofile

import android.annotation.SuppressLint
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

	@get:Rule
	val rule = BaselineProfileRule()

	@SuppressLint("SdCardPath")
	@Test
	fun generate() {
		// The application id for the running build variant is read from the instrumentation arguments.
		rule.collect(
			packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
				?: throw Exception("targetAppId not passed as instrumentation runner arg"),

			// See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
			includeInStartupProfile = true
		) {
			// This block defines the app's critical user journey. Here we are interested in
			// optimizing for app startup. But you can also navigate and scroll through your most important UI.

			// Start default activity for your app
			pressHome()
			device.executeShellCommand("pm clear org.akanework.gramophone")
			device.executeShellCommand("pm grant org.akanework.gramophone android.permission.READ_MEDIA_AUDIO")
			startActivityAndWait()

			// More interactions to optimize advanced journeys of your app.
			// 1. Wait until the content is asynchronously loaded
			device.wait(Until.findObject(By.text("8 Songs")), 30L)
			// 2. Scroll the tabs
			device.swipe(800, 1000, 200, 1000, 20)
			device.waitForIdle(20L)
			device.swipe(200, 1000, 800, 1000, 20)
			device.waitForIdle(20L)
			// 3. Play a song
			device.findObject(By.text("Summit (feat. Ellie Goulding)")).click()
			Thread.sleep(5000)


			// Check UiAutomator documentation for more information how to interact with the app.
			// https://d.android.com/training/testing/other-components/ui-automator
		}
	}
}