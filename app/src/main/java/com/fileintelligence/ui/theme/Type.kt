package com.fileintelligence.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FontSans = FontFamily.Default
val FontMono = FontFamily.Monospace
val FontCn = FontFamily.Default

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
