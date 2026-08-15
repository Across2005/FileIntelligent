package com.crossk.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeTravelSliderTest {

    // B-TTS-1: `Slider(steps = -1)` throws IllegalArgumentException.
    // Guard: steps must be >= 0.
    @Test fun `timeTravelSteps never returns negative`() {
        assertThat(timeTravelSteps(0)).isEqualTo(0)
        assertThat(timeTravelSteps(1)).isEqualTo(0)
        assertThat(timeTravelSteps(2)).isEqualTo(0)
        assertThat(timeTravelSteps(3)).isEqualTo(1)
        assertThat(timeTravelSteps(10)).isEqualTo(8)
    }

    // B-TTS-2: percentage display divides by zero when totalWeeks == 0.
    // Guard: percentage must return 0 instead of NaN/Infinity.
    @Test fun `percentage returns zero when totalWeeks is zero`() {
        assertThat(timeTravelPercentage(currentWeek = 0, totalWeeks = 0)).isEqualTo(0)
        assertThat(timeTravelPercentage(currentWeek = 5, totalWeeks = 0)).isEqualTo(0)
    }

    @Test fun `percentage computes normally with positive totalWeeks`() {
        assertThat(timeTravelPercentage(currentWeek = 1, totalWeeks = 4)).isEqualTo(50)
        assertThat(timeTravelPercentage(currentWeek = 0, totalWeeks = 4)).isEqualTo(25)
        assertThat(timeTravelPercentage(currentWeek = 3, totalWeeks = 4)).isEqualTo(100)
    }
}
