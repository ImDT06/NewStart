package com.example.newstart.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.example.newstart",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()

        // Thao tác cuộn thử để hệ thống ghi lại cách render danh sách
        device.waitForIdle()
        // Giả lập vuốt lên xuống
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 2 / 3,
            device.displayWidth / 2,
            device.displayHeight / 3,
            20
        )
        device.waitForIdle()
    }
}
