package me.liwenkun.actionbuttonpro.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import me.liwenkun.actionbuttonpro.BuildConfig
import me.liwenkun.actionbuttonpro.R
import me.liwenkun.actionbuttonpro.ui.components.AddActionDialog
import me.liwenkun.actionbuttonpro.ui.components.DeleteConfirmationDialog
import me.liwenkun.actionbuttonpro.ui.components.GestureConfigPanel
import me.liwenkun.actionbuttonpro.ui.theme.ActionButtonInterceptorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
            .edit {
                putString("is_navigation_bar_contrast_enforced", "ddddd")
                putString("is_navigation_bar_contrast", "dccccd")
            }
        window.isNavigationBarContrastEnforced = false
        setContent {
            ActionButtonInterceptorTheme {
                App()
            }
        }
    }
}

@Composable
fun App(
    modifier: Modifier = Modifier,
    viewModel: ActionEditorViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

//    if (!state.loaded) return

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ApplicationAppBar(
                title = stringResource(R.string.app_name),
                masterSwitchEnabled = state.masterSwitchEnabled,
                onMasterSwitchChange = viewModel::updateMasterSwitch,
                onInstructionsClick = viewModel::showInstructionsDialog,
            )
        },
        bottomBar = {
            NavigationBar {
                GestureTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            if (tab.iconVector != null) {
                                Icon(
                                    imageVector = tab.iconVector,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            }
                        },
                        label = { Text(text = stringResource(tab.labelRes)) },
                    )
                }
            }
        }
    ) { innerPadding ->
        val density = LocalDensity.current
        val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
        val bottomPadding = if (isKeyboardOpen) 0.dp else innerPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                )
        ) {
            GestureConfigPanel(
                state = state,
                viewModel = viewModel
            )
        }
    }

    if (state.showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissInstructionsDialog,
            title = { Text(text = stringResource(R.string.dialog_instructions_title)) },
            text = { Text(text = stringResource(R.string.dialog_instructions_message)) },
            confirmButton = {
                Button(onClick = viewModel::dismissInstructionsDialog) {
                    Text(text = stringResource(R.string.btn_got_it))
                }
            }
        )
    }

    if (state.showAddDialog) {
        AddActionDialog(
            initialLabel = state.dialogLabel,
            initialAction = state.dialogAction,
            title = stringResource(if (state.isEditMode) R.string.dialog_edit_title else R.string.dialog_add_title),
            error = state.dialogError?.let { stringResource(it) },
            onDismiss = viewModel::dismissAddDialog,
            onConfirm = { label, action ->
                viewModel.updateDialogLabel(label)
                viewModel.updateDialogAction(action)
                viewModel.confirmAddCustomAction()
            },
        )
    }

    if (state.showDeleteDialog && state.deleteTargetIndex in state.customActions.indices) {
        DeleteConfirmationDialog(
            label = state.customActions[state.deleteTargetIndex].label,
            onDismiss = viewModel::dismissDeleteDialog,
            onConfirm = viewModel::confirmDeleteCustomAction,
        )
    }
}

@Composable
fun ApplicationAppBar(
    title: String,
    masterSwitchEnabled: Boolean,
    onMasterSwitchChange: (Boolean) -> Unit,
    onInstructionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.Transparent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Instructions Button
            IconButton(onClick = onInstructionsClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.menu_instructions),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isDark = isSystemInDarkTheme()
            val backgroundColor = if (masterSwitchEnabled) {
                if (isDark) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
            } else {
                if (isDark) Color(0xFFB71C1C) else Color(0xFFFFCDD2)
            }
            val contentColor = if (masterSwitchEnabled) {
                if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
            } else {
                if (isDark) Color(0xFFEF9A9A) else Color(0xFFB71C1C)
            }

            Surface(
                onClick = { onMasterSwitchChange(!masterSwitchEnabled) },
                shape = CircleShape,
                color = backgroundColor,
                contentColor = contentColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (masterSwitchEnabled) {
                        // Custom Pause Icon: Two vertical rounded lines
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(contentColor)
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(contentColor)
                            )
                        }
                    } else {
                        // Play Icon
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

        }
    }
}


