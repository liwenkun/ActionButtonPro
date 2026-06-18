package me.liwenkun.actionbuttonpro.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.liwenkun.actionbuttonpro.R
import me.liwenkun.actionbuttonpro.ui.ActionEditorViewModel
import me.liwenkun.actionbuttonpro.ui.CustomAction
import me.liwenkun.actionbuttonpro.ui.EditorMode
import me.liwenkun.actionbuttonpro.ui.GestureConfigUiState
import me.liwenkun.actionbuttonpro.ui.GestureTab

@Composable
fun ConfigPanelDetail(
    currentTab: GestureTab,
    currentGestureConfig: GestureConfigUiState,
    customActions: List<CustomAction>,
    viewModel: ActionEditorViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 手势接管与透传配置卡片 — 极其精美的 M3 边框卡片设计
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (currentGestureConfig.enabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(currentTab.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentGestureConfig.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = if (currentGestureConfig.enabled) "手势动作拦截接管中" else "未启用本手势",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentGestureConfig.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(
                        checked = currentGestureConfig.enabled,
                        onCheckedChange = viewModel::updateEnabled
                    )
                }
                
                if (!currentGestureConfig.enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(R.string.pass_through),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "未启用本手势拦截时，允许物理按键事件透传给系统处理",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentGestureConfig.passThrough,
                            onCheckedChange = viewModel::updatePassThrough
                        )
                    }
                }
            }
        }

        val isGrayedOut = !currentGestureConfig.enabled

        // 2. 下方的配置区域 — 当手势未启用时，将整体置灰并禁用所有交互
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isGrayedOut) 0.35f else 1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 动作模式选择 Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.section_action_mode),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    ActionMode(
                        selected = currentGestureConfig.mode,
                        onSelect = viewModel::selectMode,
                    )
                }

                // 具体配置内容（Preset, Custom 或 Shell）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentGestureConfig.mode) {
                        EditorMode.ACTION -> {
                            PresetActionSection(
                                selectedPresetAction = currentGestureConfig.selectedPresetAction,
                                selectedCustomIndex = currentGestureConfig.selectedCustomIndex,
                                onSelectPreset = viewModel::selectPreset,
                            )
                            
                            CustomActionSection(
                                customActions = customActions,
                                selectedCustomIndex = currentGestureConfig.selectedCustomIndex,
                                onSelectCustom = viewModel::selectCustomAction,
                                onShowAddDialog = { viewModel.showAddDialog() },
                                onShowEditDialog = viewModel::showEditDialog,
                                onShowDeleteDialog = viewModel::showDeleteDialog,
                            )
                        }

                        EditorMode.SHELL -> {
                            ShellSection(
                                shellCommand = currentGestureConfig.shellCommand,
                                onUpdateShellCommand = viewModel::updateShellCommand,
                                showShellOutput = currentGestureConfig.showShellOutput,
                                onUpdateShowShellOutput = viewModel::updateShowShellOutput,
                                shellOutputFormat = currentGestureConfig.shellOutputFormat,
                                onUpdateShellOutputFormat = viewModel::updateShellOutputFormat,
                            )
                        }
                    }
                }
            }

            if (isGrayedOut) {
                // Completely transparent overlay Box that matches parent size exactly, swallowing taps but letting scrolls pass
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Swallow taps to prevent clicking children, but allow scroll drags to bubble up!
                            }
                        }
                )
            }
        }
    }
}
