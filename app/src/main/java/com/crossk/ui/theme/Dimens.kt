package com.crossk.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * v2.0 设计 Token — 维度（间距/圆角/边距）。
 *
 * 原则：
 * - 4dp 基础网格，6 档间距（xs/sm/md/lg/xl/xxl）
 * - 4 档圆角（sm/md/lg/pill）
 * - 4 档 elevation（flat/card/modal/floating）
 * - 全部以 dp 常量形式暴露，避免散落的字面量
 */
object Dimens {
    // Spacing scale — 4dp grid
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp
    val SpaceXxl = 32.dp
    val SpaceXxxl = 48.dp

    // Corner radius
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 20.dp
    val RadiusXl = 28.dp
    val RadiusPill = 999.dp

    // Elevation
    val ElevationFlat = 0.dp
    val ElevationCard = 2.dp
    val ElevationModal = 8.dp
    val ElevationFloating = 12.dp

    // Component-specific
    val TouchTarget = 48.dp
    val IconSm = 16.dp
    val IconMd = 20.dp
    val IconLg = 24.dp
    val IconXl = 32.dp
    val BottomNavHeight = 72.dp
    val TopBarHeight = 64.dp
    val GraphNodeRadius = 16.dp
    val GraphNodeRadiusMax = 32.dp
    val GraphEdgeStroke = 1.2f
    val GraphEdgeStrokeHighlight = 2.4f
}

/**
 * 本地 Composition 容器（为后续主题切换/多套设计 token 预留）。
 * 当前全局使用静态 Dimens，但通过 CompositionLocal 接管便于白标/多主题扩展。
 */
val LocalDimens = compositionLocalOf { Dimens }
