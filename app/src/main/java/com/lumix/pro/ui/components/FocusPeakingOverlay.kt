package com.lumix.pro.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Composable
fun FocusPeakingOverlay(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return
    val img = bitmap.asImageBitmap()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = 0.95f
        )
    }
}

@Composable
fun ZebraOverlay(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return
    val img = bitmap.asImageBitmap()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = 0.75f
        )
    }
}
