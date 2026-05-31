package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import com.example.data.PotionDatabase
import com.example.data.PotionRepository
import com.example.ui.CauldronApp
import com.example.ui.PotionViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Potion Cauldron", appName)
  }

  @Test
  fun `launch app and verify layout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = PotionDatabase.getDatabase(context)
    val repository = PotionRepository(database)
    val viewModel = PotionViewModel(repository)

    composeTestRule.setContent {
      MyApplicationTheme {
        CauldronApp(viewModel = viewModel)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onRoot().printToLog("CauldronAppTest")
  }
}
