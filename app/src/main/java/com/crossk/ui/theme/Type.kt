package com.crossk.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppFonts {
    val Sans = FontFamily.Default
    val Mono = FontFamily.Monospace
    val Serif = FontFamily.Serif
    val Cursive = FontFamily.Cursive

    /**
     * Available font options for the postcard editor.
     */
    data class FontOption(
        val name: String,
        val displayName: String,
        val fontFamily: FontFamily,
    )

    val editorFonts = listOf(
        FontOption("default", "系统默认", Sans),
        FontOption("serif", "衬线体", Serif),
        FontOption("mono", "等宽体", Mono),
        FontOption("cursive", "手写体", Cursive),
    )

    fun getFont(name: String): FontFamily = when (name) {
        "serif" -> Serif
        "mono" -> Mono
        "cursive" -> Cursive
        else -> Sans
    }
}

val FontSans = AppFonts.Sans
val FontMono = AppFonts.Mono
val FontCn = AppFonts.Sans

val TextXl = TextStyle(
    fontSize = 20.sp,
    letterSpacing = (-0.02).sp,
    fontWeight = FontWeight(600),
    fontFamily = FontSans,
)

val TextTitle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight(700),
    letterSpacing = (-0.02).sp,
    fontFamily = FontSans,
)

val TextBody = TextStyle(
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight(400),
    fontFamily = FontSans,
)

val TextSmall = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight(500),
    fontFamily = FontSans,
)

val TextCaption = TextStyle(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight(400),
    fontFamily = FontSans,
)

val TextMono = TextStyle(
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight(500),
    fontFamily = FontMono,
)
