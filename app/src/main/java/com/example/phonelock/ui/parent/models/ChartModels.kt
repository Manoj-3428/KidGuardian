package com.example.phonelock.ui.parent.models

import androidx.compose.ui.graphics.Color

data class SliceData(
    val category: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)

data class LabelPosition(
    val x: Float,
    val y: Float,
    val angle: Float,
    val text: String,
    val color: Color
)

