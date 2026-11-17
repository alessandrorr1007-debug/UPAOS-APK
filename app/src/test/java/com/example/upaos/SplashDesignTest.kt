package com.example.upaos

import androidx.compose.ui.graphics.Color
import com.example.upaos.ui.splash.SplashColors
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashDesignTest {

    @Test
    fun splashColorsMatchUnifiedDarkNavyDesign() {
        assertEquals(Color(0xFF031238), SplashColors.NavyTop)
        assertEquals(Color(0xFF072260), SplashColors.NavyCenter)
        assertEquals(Color(0xFF020B24), SplashColors.NavyBottom)
        assertEquals(Color(0xFF1A73F8), SplashColors.ProgressActive)
        assertEquals(Color(0xFFFFFFFF), SplashColors.TitleWhite)
    }
}
