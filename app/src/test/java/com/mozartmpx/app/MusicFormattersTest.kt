package com.mozartmpx.app

import com.mozartmpx.app.data.model.formatDuration
import com.mozartmpx.app.data.model.formatFileSize
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MusicFormattersTest {

  private val originalLocale = Locale.getDefault()

  @Before
  fun useDecimalPoint() {
    Locale.setDefault(Locale.US)
  }

  @After
  fun restoreLocale() {
    Locale.setDefault(originalLocale)
  }

  @Test
  fun formatDurationShowsMinutesAndSeconds() {
    assertEquals("0:00", formatDuration(0L))
    assertEquals("3:45", formatDuration(225_000L))
  }

  @Test
  fun formatDurationShowsHoursWhenNeeded() {
    assertEquals("1:01:01", formatDuration(3_661_000L))
  }

  @Test
  fun formatFileSizeUsesMegabytesAndKilobytes() {
    assertEquals("—", formatFileSize(0L))
    assertEquals("512 KB", formatFileSize(512L * 1_024L))
    assertEquals("1.5 MB", formatFileSize(1_572_864L))
  }
}
