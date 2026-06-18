package me.liwenkun.actionbuttonpro.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import me.liwenkun.actionbuttonpro.ui.ActionEditorViewModel
import me.liwenkun.actionbuttonpro.ui.EditorUiState
import me.liwenkun.actionbuttonpro.ui.GestureTab

@Composable
fun GestureConfigPanel(
    state: EditorUiState,
    viewModel: ActionEditorViewModel,
) {
    val focusManager = LocalFocusManager.current

    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.ordinal,
        pageCount = { GestureTab.entries.size }
    )

    // 当 ViewModel 中的 tab 改变时，动画滚动 pager 到对应页面（使用更轻快、响应更迅速的缓动曲线）
    LaunchedEffect(state.selectedTab) {
        focusManager.clearFocus()
        val targetPage = state.selectedTab.ordinal
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(
                page = targetPage,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
        }
    }

    // 当用户手动滑动结束并静止后，同步更新 ViewModel 的 tab 状态，避免在拖动中途频繁触发重组造成卡顿
    val isScrollInProgress = pagerState.isScrollInProgress
    LaunchedEffect(pagerState.currentPage, isScrollInProgress) {
        if (!isScrollInProgress) {
            val targetTab = GestureTab.entries.getOrNull(pagerState.currentPage)
            if (targetTab != null && state.selectedTab != targetTab) {
                viewModel.selectTab(targetTab)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        beyondViewportPageCount = 2,
        verticalAlignment = Alignment.Top
    ) { page ->
        val targetTab = GestureTab.entries[page]
        if (targetTab == GestureTab.SETTINGS) {
            SettingsPanel(
                doubleClickTimeoutMs = state.doubleClickTimeoutMs,
                debounceTimeoutMs = state.debounceTimeoutMs,
                vibrateOnLongPress = state.vibrateOnLongPress,
                onDoubleClickTimeoutMsChange = viewModel::updateDoubleClickTimeoutMs,
                onDebounceTimeoutMsChange = viewModel::updateDebounceTimeoutMs,
                onVibrateOnLongPressChange = viewModel::updateVibrateOnLongPress
            )
        } else {
            val currentGestureConfig = when (targetTab) {
                GestureTab.SINGLE -> state.single
                GestureTab.DOUBLE -> state.double
                else -> state.long
            }
            ConfigPanelDetail(
                currentTab = targetTab,
                currentGestureConfig = currentGestureConfig,
                customActions = state.customActions,
                viewModel = viewModel,
            )
        }
    }
}
