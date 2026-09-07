package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.ScanRecord
import com.example.data.repository.ScanResult
import com.example.ui.screens.ScanResultAlertCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleResult = ScanResult(
      record = ScanRecord(
        id = 1,
        code = "JP829104829103",
        courierName = "J&T Express",
        formatName = "CODE_128",
        isDuplicate = true,
        scanCount = 2
      ),
      isDuplicate = true,
      previousScanTime = System.currentTimeMillis() - 120000L,
      totalTimesScanned = 2
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ScanResultAlertCard(
          result = sampleResult,
          onDismiss = {},
          onCopyCode = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

