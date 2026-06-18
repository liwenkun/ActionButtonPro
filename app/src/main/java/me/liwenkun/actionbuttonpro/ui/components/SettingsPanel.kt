package me.liwenkun.actionbuttonpro.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import me.liwenkun.actionbuttonpro.BuildConfig
import me.liwenkun.actionbuttonpro.R
import kotlin.math.roundToInt

class DrawablePainter(private val drawable: android.graphics.drawable.Drawable) : Painter() {
    override val intrinsicSize: Size
        get() = Size(
            width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.toFloat() else Size.Unspecified.width,
            height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.toFloat() else Size.Unspecified.height
        )

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            val width = size.width.roundToInt()
            val height = size.height.roundToInt()
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

private const val  GITHUB_URL: String = "https://github.com/liwenkun/ActionButtonPro"

@Composable
fun SliderSettingItem(
    title: String,
    desc: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 性能优化：引入本地高频拖拽值，并采用非装箱的 Float 特化状态 mutableFloatStateOf 避免频繁自动装箱开销
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${sliderValue.roundToInt()} ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(6.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
            valueRange = range,
            steps = ((range.endInclusive - range.start) / 50).toInt() - 1
        )
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsPanel(
    doubleClickTimeoutMs: Int,
    debounceTimeoutMs: Int,
    vibrateOnLongPress: Boolean,
    onDoubleClickTimeoutMsChange: (Int) -> Unit,
    onDebounceTimeoutMsChange: (Int) -> Unit,
    onVibrateOnLongPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var launcherIconPainter by remember { mutableStateOf<Painter?>(null) }
    LaunchedEffect(context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(context.packageName)
                launcherIconPainter = DrawablePainter(drawable)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. 设置项配置
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SliderSettingItem(
                title = stringResource(R.string.settings_double_click_timeout_title),
                desc = stringResource(R.string.settings_double_click_timeout_desc),
                value = doubleClickTimeoutMs,
                range = 100f..1000f,
                onValueChange = onDoubleClickTimeoutMsChange
            )

            SliderSettingItem(
                title = stringResource(R.string.settings_debounce_timeout_title),
                desc = stringResource(R.string.settings_debounce_timeout_desc),
                value = debounceTimeoutMs,
                range = 0f..1500f,
                onValueChange = onDebounceTimeoutMsChange
            )

            SwitchSettingItem(
                title = stringResource(R.string.settings_vibrate_title),
                desc = stringResource(R.string.settings_vibrate_desc),
                checked = vibrateOnLongPress,
                onCheckedChange = onVibrateOnLongPressChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 2. "关于" Section 标题
        Text(
            text = stringResource(R.string.about_section_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 3. 应用信息头部（Logo + 名称 + 版本）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            val iconPainter = launcherIconPainter
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. 关于详情列表（紧凑，像 Markdown 一样一行行呈现，不含分割线）
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            // 作者：Chance
            Text(
                text = "${stringResource(R.string.about_author_label)}: Chance",
                style = MaterialTheme.typography.bodyLarge
            )

            // 开源协议：GPLv3
            Text(
                text = "${stringResource(R.string.about_license_label)}: GPLv3",
                style = MaterialTheme.typography.bodyLarge
            )

            // 开源仓库：带 Github Icon，点击跳转浏览器，去除了额外的提示文案
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        runCatching {
                            // 暂时留空，点击一键跳转到 Github 主页
                            val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
                            context.startActivity(intent)
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_github),
                    contentDescription = "GitHub Icon",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GITHUB_URL",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}