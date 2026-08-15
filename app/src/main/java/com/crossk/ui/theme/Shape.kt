package com.crossk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * v2.0 设计 Token — 形状系统。
 *
 * 统一 Material3 Shapes 语义映射到 Dimens.token：
 * - small (组件内) → RadiusSm
 * - medium (卡片) → RadiusMd
 * - large (大容器) → RadiusLg
 * - extraLarge (BottomSheet/Modal) → RadiusXl
 */
val CrossKShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.RadiusSm),
    small = RoundedCornerShape(Dimens.RadiusSm),
    medium = RoundedCornerShape(Dimens.RadiusMd),
    large = RoundedCornerShape(Dimens.RadiusLg),
    extraLarge = RoundedCornerShape(Dimens.RadiusXl),
)
