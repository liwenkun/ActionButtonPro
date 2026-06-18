package me.liwenkun.actionbuttonpro.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import me.liwenkun.actionbuttonpro.R

/**
 * 底部导航 Tab：单击 / 双击 / 长按
 */
enum class GestureTab(
    val labelRes: Int,
    val iconRes: Int,
    val iconVector: ImageVector? = null
) {
    SINGLE(R.string.tab_single, R.drawable.ic_tap_single),
    DOUBLE(R.string.tab_double, R.drawable.ic_tap_double),
    LONG(R.string.tab_long, R.drawable.ic_tap_long),
    SETTINGS(R.string.tab_settings, 0, Icons.Default.Settings)
}