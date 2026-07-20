package com.idiombloom.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiombloom.app.data.DictionaryManager
import com.idiombloom.app.data.DictionaryUpdateStatus
import com.idiombloom.app.data.StudyStore
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DictionarySettingsScreen(
    manager: DictionaryManager,
    store: StudyStore,
    outerPadding: PaddingValues,
) {
    val info = manager.info
    val status = manager.status
    val scope = rememberCoroutineScope()
    var urlDraft by rememberSaveable(manager.manifestUrl) {
        mutableStateOf(manager.manifestUrl)
    }
    var newGoalDraft by rememberSaveable(store.state.dailyNewGoal) {
        mutableFloatStateOf(store.state.dailyNewGoal.toFloat())
    }
    var reviewGoalDraft by rememberSaveable(store.state.dailyReviewGoal) {
        mutableFloatStateOf(store.state.dailyReviewGoal.toFloat())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(outerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = "我的",
                    color = Ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "管理词库与自动更新",
                    color = SecondaryInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Peach, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = null,
                                tint = Ink,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "每日新学计划",
                                color = Ink,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "每天新学 ${newGoalDraft.roundToInt()} 个 · 今日已学 ${store.todayLearnedCount} 个",
                                color = SecondaryInk,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Slider(
                        value = newGoalDraft,
                        onValueChange = {
                            newGoalDraft = ((it / 5f).roundToInt() * 5).toFloat().coerceIn(5f, 100f)
                        },
                        onValueChangeFinished = {
                            store.setDailyNewGoal(newGoalDraft.roundToInt())
                        },
                        valueRange = 5f..100f,
                        steps = 18,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("5", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.weight(1f))
                        Text("拖动调整", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.weight(1f))
                        Text("100", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFFFF9F0A).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Replay,
                                contentDescription = null,
                                tint = Color(0xFFFF9F0A),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "每日复习计划",
                                color = Ink,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (store.state.autoReviewEnabled) {
                                    "系统根据遗忘曲线自动安排"
                                } else {
                                    "每天复习 ${reviewGoalDraft.roundToInt()} 个 · 今日已复习 ${store.todayReviewedCount} 个"
                                },
                                color = SecondaryInk,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = store.state.autoReviewEnabled,
                            onCheckedChange = store::setAutoReviewEnabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (store.state.autoReviewEnabled) {
                            "默认模式会把当天到期的成语全部加入计划。连续答对至少 5 轮且记忆间隔达到 30 天后，才会进入“已掌握”；以后答错会重新进入复习。"
                        } else {
                            "自定义模式优先选择已经到期的内容，不足时按下一次复习时间补足；掌握标准仍与自动模式相同。"
                        },
                        color = SecondaryInk,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (!store.state.autoReviewEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Slider(
                            value = reviewGoalDraft,
                            onValueChange = {
                                reviewGoalDraft = ((it / 5f).roundToInt() * 5)
                                    .toFloat()
                                    .coerceIn(5f, 100f)
                            },
                            onValueChangeFinished = {
                                store.setDailyReviewGoal(reviewGoalDraft.roundToInt())
                            },
                            valueRange = 5f..100f,
                            steps = 18,
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("5", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.weight(1f))
                            Text("拖动调整", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.weight(1f))
                            Text("100", color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(SuccessGreen.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = SuccessGreen,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${info.source.title} · v${info.version}",
                                color = Ink,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${info.entryCount} 条成语${updatedAtSuffix(info.updatedAt)}",
                                color = SecondaryInk,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Ink.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动更新词库", color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                "启动 App 时检查，每 12 小时最多一次",
                                color = SecondaryInk,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = manager.autoUpdateEnabled,
                            onCheckedChange = manager::updateAutoUpdateEnabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { scope.launch { manager.checkForUpdates() } },
                        enabled = status !is DictionaryUpdateStatus.Checking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    ) {
                        if (status is DictionaryUpdateStatus.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(9.dp))
                            Text("正在检查…")
                        } else {
                            androidx.compose.material3.Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(7.dp))
                            Text("立即检查更新", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (status is DictionaryUpdateStatus.Message) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = status.text,
                            color = if (status.isError) ErrorRed else SuccessGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    info.lastCheckedAtMillis?.let { checkedAt ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "上次检查：${formatTime(checkedAt)}",
                            color = SecondaryInk.copy(alpha = 0.76f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Text(
                        text = "更新来源",
                        color = Ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "正式发布时可在工程中预置地址，普通用户无需设置。这里也支持临时切换到其他 HTTPS 词库源。",
                        color = SecondaryInk,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = urlDraft,
                        onValueChange = { urlDraft = it },
                        label = { Text("manifest.json 地址") },
                        placeholder = { Text("https://example.com/manifest.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { manager.updateManifestUrl(urlDraft) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Text("保存地址", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(
                            onClick = {
                                manager.useDefaultManifestUrl()
                                urlDraft = manager.manifestUrl
                            },
                        ) {
                            Text("恢复默认", color = SecondaryInk)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = manager::restoreBundledDictionary,
                    enabled = status !is DictionaryUpdateStatus.Checking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Text("恢复内置词库", color = SecondaryInk)
                }
            }
        }
    }
}

private fun updatedAtSuffix(updatedAt: String): String =
    updatedAt.takeIf { it.isNotBlank() }?.let { " · 更新于 $it" }.orEmpty()

private fun formatTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
