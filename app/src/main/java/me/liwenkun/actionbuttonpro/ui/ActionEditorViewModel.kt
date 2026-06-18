package me.liwenkun.actionbuttonpro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.liwenkun.actionbuttonpro.R
import me.liwenkun.actionbuttonpro.settings.AppSettingStore
import me.liwenkun.actionbuttonpro.settings.GestureSettings
import me.liwenkun.actionbuttonpro.settings.Settings
import me.liwenkun.actionbuttonpro.settings.SettingsManager
import me.liwenkun.actionbuttonpro.systemserver.getAssistantPackage

// ═══════════════════════════════════════════════
// GestureConfigUiState — 单个 Gesture Tab 的 UI
// ═══════════════════════════════════════════════

data class GestureConfigUiState(
    val enabled: Boolean = false,
    val mode: EditorMode = EditorMode.ACTION,
    val selectedPresetAction: String = PresetActions.IMAGE_CAPTURE.action,
    val selectedCustomIndex: Int = -1,
    val customInput: String = "",
    val shellCommand: String = "",
    val showShellOutput: Boolean = false,
    val shellOutputFormat: String = "toast",
    val passThrough: Boolean = false,
)

// ═══════════════════════════════════════════════
// EditorUiState — 顶层 UI 状态
// ═══════════════════════════════════════════════

data class EditorUiState(
    val loaded: Boolean = false,
    val selectedTab: GestureTab = GestureTab.SINGLE,
    val single: GestureConfigUiState = GestureConfigUiState(),
    val double: GestureConfigUiState = GestureConfigUiState(),
    val long: GestureConfigUiState = GestureConfigUiState(),
    val customActions: List<CustomAction> = emptyList(),
    // 总开关
    val masterSwitchEnabled: Boolean = false,
    // 使用须知弹窗
    val showInstructionsDialog: Boolean = false,
    // 添加 / 编辑弹窗
    val showAddDialog: Boolean = false,
    val dialogLabel: String = "",
    val dialogAction: String = "",
    val dialogError: Int? = null,
    val isEditMode: Boolean = false,
    // 删除弹窗
    val showDeleteDialog: Boolean = false,
    val deleteTargetIndex: Int = -1,
    // 额外设置
    val doubleClickTimeoutMs: Int = 400,
    val debounceTimeoutMs: Int = 550,
    val vibrateOnLongPress: Boolean = false,
)

// ═══════════════════════════════════════════════
// ActionEditorViewModel
// ═══════════════════════════════════════════════

class ActionEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        SettingsManager.init(AppSettingStore(application))
        loadSettings(SettingsManager.getSettings())
    }

    // ───────────────────────────────────────────
    // 加载设置
    // ───────────────────────────────────────────

    private fun loadSettings(s: Settings) {
        val showInstructions = !s.hasShownInstructions
        _uiState.update {
            it.copy(
                loaded = true,
                masterSwitchEnabled = s.enabled,
                showInstructionsDialog = showInstructions,
                customActions = s.customActions,
                single = gestureToUiState(s.singlePress, s.customActions),
                double = gestureToUiState(s.doublePress, s.customActions),
                long = gestureToUiState(s.longPress, s.customActions),
                doubleClickTimeoutMs = s.doubleClickTimeoutMs,
                debounceTimeoutMs = s.debounceTimeoutMs,
                vibrateOnLongPress = s.vibrateOnLongPress,
            )
        }
        if (showInstructions) {
            SettingsManager.update { copy(hasShownInstructions = true) }
        }
    }

    private fun gestureToUiState(
        gs: GestureSettings,
        customActions: List<CustomAction>
    ): GestureConfigUiState {
        val mode = EditorMode.fromKey(gs.activeMode)
        val customIdx = if (mode == EditorMode.ACTION) {
            customActions.indexOfFirst { it.action == gs.action }
        } else -1
        return GestureConfigUiState(
            enabled = gs.enabled,
            mode = mode,
            selectedPresetAction = PresetActions.findByAction(gs.action)?.action
                ?: PresetActions.IMAGE_CAPTURE.action,
            selectedCustomIndex = customIdx,
            customInput = gs.action.takeIf { customIdx >= 0 } ?: "",
            shellCommand = gs.shellCommand,
            showShellOutput = gs.showShellOutput,
            shellOutputFormat = gs.shellOutputFormat,
            passThrough = gs.passThrough,
        )
    }

    // ───────────────────────────────────────────
    // Tab & Enabled
    // ───────────────────────────────────────────

    fun selectTab(tab: GestureTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateMasterSwitch(enabled: Boolean) {
        _uiState.update { it.copy(masterSwitchEnabled = enabled) }
        persist()
        val messageRes = if (enabled) R.string.toast_master_switch_on else R.string.toast_master_switch_off
        android.widget.Toast.makeText(getApplication(), messageRes, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun showInstructionsDialog() {
        _uiState.update { it.copy(showInstructionsDialog = true) }
    }

    fun dismissInstructionsDialog() {
        _uiState.update { it.copy(showInstructionsDialog = false) }
    }

    fun updateEnabled(enabled: Boolean) {
        val state = _uiState.value
        val tab = state.selectedTab
        _uiState.update {
            setGesture(it, tab, getGesture(state, tab).copy(enabled = enabled))
        }
        persist()
    }

    fun updatePassThrough(passThrough: Boolean) {
        val state = _uiState.value
        val tab = state.selectedTab
        _uiState.update {
            setGesture(it, tab, getGesture(state, tab).copy(passThrough = passThrough))
        }
        persist()
    }

    // ───────────────────────────────────────────
    // 模式切换（作用于当前 Tab）
    // ───────────────────────────────────────────

    fun selectMode(mode: EditorMode) {
        val state = _uiState.value
        val tab = state.selectedTab
        _uiState.update {
            setGesture(it, tab, getGesture(state, tab).copy(mode = mode))
        }
        persist()
    }

    // ───────────────────────────────────────────
    // 预设 Action（作用于当前 Tab）
    // ───────────────────────────────────────────

    fun selectPreset(pa: PresetActions) {
        val state = _uiState.value
        val tab = state.selectedTab

        if (pa == PresetActions.WEB_SEARCH) {
            val pkg = getAssistantPackage(getApplication())
            SettingsManager.update { copy(assistantPackage = pkg ?: "") }
        }

        val existing = getGesture(state, tab)
        _uiState.update {
            setGesture(it, tab, existing.copy(
                mode = EditorMode.ACTION,
                selectedPresetAction = pa.action,
                selectedCustomIndex = -1,
                customInput = ""
            ))
        }
        persist()
    }

    // ───────────────────────────────────────────
    // 自定义 Action（作用于当前 Tab）
    // ───────────────────────────────────────────

    fun selectCustomAction(index: Int) {
        val state = _uiState.value
        val ca = state.customActions.getOrNull(index) ?: return
        val existing = getGesture(state, state.selectedTab)
        _uiState.update {
            setGesture(it, state.selectedTab, existing.copy(
                mode = EditorMode.ACTION,
                selectedPresetAction = "",
                selectedCustomIndex = index,
                customInput = ca.action
            ))
        }
        persist()
    }

    // ───────────────────────────────────────────
    // 自定义 Action 增/删/改
    // ───────────────────────────────────────────

    fun showAddDialog(initialAction: String = "") {
        _uiState.update { it.copy(showAddDialog = true, dialogLabel = "", dialogAction = initialAction, dialogError = null, isEditMode = false) }
    }

    fun showEditDialog() {
        val state = _uiState.value
        val gesture = getGesture(state, state.selectedTab)
        val idx = gesture.selectedCustomIndex
        if (idx < 0 || idx >= state.customActions.size) return
        val ca = state.customActions[idx]
        _uiState.update {
            it.copy(
                showAddDialog = true,
                dialogLabel = ca.label,
                dialogAction = ca.action,
                dialogError = null,
                isEditMode = true
            )
        }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false, dialogError = null) }
    }

    fun updateDialogLabel(value: String) {
        _uiState.update { it.copy(dialogLabel = value, dialogError = null) }
    }

    fun updateDialogAction(value: String) {
        _uiState.update { it.copy(dialogAction = value, dialogError = null) }
    }

    fun confirmAddCustomAction() {
        val state = _uiState.value
        val label = state.dialogLabel.trim()
        val action = state.dialogAction.trim()
        if (label.isEmpty()) { _uiState.update { it.copy(dialogError = R.string.error_label_empty) }; return }
        if (action.isEmpty()) { _uiState.update { it.copy(dialogError = R.string.error_action_empty) }; return }

        val activeTab = state.selectedTab
        val gesture = getGesture(state, activeTab)
        val selectedIdx = gesture.selectedCustomIndex

        if (state.isEditMode) {
            if (selectedIdx < 0 || selectedIdx >= state.customActions.size) return
            
            // 校验重名（排除当前正在编辑的项）
            if (state.customActions.indices.any { it != selectedIdx && state.customActions[it].label == label }) {
                _uiState.update { it.copy(dialogError = R.string.error_label_duplicate) }
                return
            }

            val newList = state.customActions.toMutableList().also {
                it[selectedIdx] = CustomAction(label, action)
            }

            _uiState.update {
                it.copy(
                    showAddDialog = false,
                    customActions = newList,
                    single = if (state.selectedTab == GestureTab.SINGLE) it.single.copy(customInput = action) else it.single,
                    double = if (state.selectedTab == GestureTab.DOUBLE) it.double.copy(customInput = action) else it.double,
                    long = if (state.selectedTab == GestureTab.LONG) it.long.copy(customInput = action) else it.long,
                )
            }
            SettingsManager.update { copy(customActions = newList) }
            persist()
        } else {
            if (state.customActions.any { it.label == label }) { _uiState.update { it.copy(dialogError = R.string.error_label_duplicate) }; return }

            val newList = state.customActions + CustomAction(label, action)
            val newIdx = newList.lastIndex

            _uiState.update {
                it.copy(
                    showAddDialog = false,
                    customActions = newList,
                    single = if (state.selectedTab == GestureTab.SINGLE) it.single.copy(mode = EditorMode.ACTION, selectedCustomIndex = newIdx, customInput = action) else it.single,
                    double = if (state.selectedTab == GestureTab.DOUBLE) it.double.copy(mode = EditorMode.ACTION, selectedCustomIndex = newIdx, customInput = action) else it.double,
                    long = if (state.selectedTab == GestureTab.LONG) it.long.copy(mode = EditorMode.ACTION, selectedCustomIndex = newIdx, customInput = action) else it.long,
                )
            }
            SettingsManager.update { copy(customActions = newList) }
            persist()
        }
    }

    fun showDeleteDialog(index: Int) {
        _uiState.update { it.copy(showDeleteDialog = true, deleteTargetIndex = index) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDeleteCustomAction() {
        val state = _uiState.value
        val idx = state.deleteTargetIndex
        if (idx < 0 || idx >= state.customActions.size) return
        val newList = state.customActions.toMutableList().also { it.removeAt(idx) }

        _uiState.update {
            it.copy(
                customActions = newList,
                showDeleteDialog = false,
                single = clearCustomIfSelected(it.single, idx),
                double = clearCustomIfSelected(it.double, idx),
                long = clearCustomIfSelected(it.long, idx),
            )
        }
        SettingsManager.update { copy(customActions = newList) }
        persist()
    }

    // ───────────────────────────────────────────
    // Shell（作用于当前 Tab）
    // ───────────────────────────────────────────

    // ───────────────────────────────────────────
    // Shell Settings
    // ───────────────────────────────────────────

    fun updateShowShellOutput(value: Boolean) {
        val state = _uiState.value
        _uiState.update {
            setGesture(it, state.selectedTab, getGesture(it, state.selectedTab).copy(showShellOutput = value))
        }
        persist()
    }

    fun updateShellOutputFormat(value: String) {
        val state = _uiState.value
        _uiState.update {
            setGesture(it, state.selectedTab, getGesture(it, state.selectedTab).copy(shellOutputFormat = value))
        }
        persist()
    }

    fun updateShellCommand(value: String) {
        val state = _uiState.value
        _uiState.update { setGesture(it, state.selectedTab, getGesture(it, state.selectedTab).copy(shellCommand = value)) }
        persist()
    }

    // ───────────────────────────────────────────
    // 内部辅助方法
    // ───────────────────────────────────────────

    private fun getGesture(state: EditorUiState, tab: GestureTab): GestureConfigUiState = when (tab) {
        GestureTab.SINGLE -> state.single
        GestureTab.DOUBLE -> state.double
        GestureTab.LONG -> state.long
        GestureTab.SETTINGS -> GestureConfigUiState()
    }

    private fun setGesture(state: EditorUiState, tab: GestureTab, g: GestureConfigUiState): EditorUiState = when (tab) {
        GestureTab.SINGLE -> state.copy(single = g)
        GestureTab.DOUBLE -> state.copy(double = g)
        GestureTab.LONG -> state.copy(long = g)
        GestureTab.SETTINGS -> state
    }

    fun updateDoubleClickTimeoutMs(value: Int) {
        _uiState.update { it.copy(doubleClickTimeoutMs = value) }
        persist()
    }

    fun updateDebounceTimeoutMs(value: Int) {
        _uiState.update { it.copy(debounceTimeoutMs = value) }
        persist()
    }

    fun updateVibrateOnLongPress(value: Boolean) {
        _uiState.update { it.copy(vibrateOnLongPress = value) }
        persist()
    }

    /** 将当前 UI 状态持久化到 ContentProvider */
    private fun persist() {
        val state = _uiState.value
        SettingsManager.update {
            copy(
                enabled = state.masterSwitchEnabled,
                hasShownInstructions = true,
                singlePress = state.single.toGestureSettings(),
                doublePress = state.double.toGestureSettings(),
                longPress = state.long.toGestureSettings(),
                customActions = state.customActions,
                doubleClickTimeoutMs = state.doubleClickTimeoutMs,
                debounceTimeoutMs = state.debounceTimeoutMs,
                vibrateOnLongPress = state.vibrateOnLongPress
            )
        }
    }

    private fun GestureConfigUiState.toGestureSettings(): GestureSettings = GestureSettings(
        enabled = enabled,
        activeMode = mode.key,
        action = if (selectedCustomIndex >= 0) customInput else selectedPresetAction,
        shellCommand = shellCommand,
        showShellOutput = showShellOutput,
        shellOutputFormat = shellOutputFormat,
        passThrough = passThrough
    )

    companion object {
        private fun clearCustomIfSelected(g: GestureConfigUiState, deleteIdx: Int): GestureConfigUiState =
            if (g.selectedCustomIndex == deleteIdx) {
                g.copy(
                    enabled = false,
                    mode = EditorMode.ACTION,
                    selectedPresetAction = PresetActions.IMAGE_CAPTURE.action,
                    selectedCustomIndex = -1,
                    customInput = ""
                )
            } else if (g.selectedCustomIndex > deleteIdx) {
                g.copy(selectedCustomIndex = g.selectedCustomIndex - 1)
            } else {
                g
            }
    }
}
