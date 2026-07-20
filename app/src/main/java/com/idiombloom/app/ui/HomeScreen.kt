package com.idiombloom.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiombloom.app.data.DailyStudyRecord
import com.idiombloom.app.data.Idiom
import com.idiombloom.app.data.StudyStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val LEARNING_TAB = 0
private const val DASHBOARD_TAB = 1

private val dailyHeadlines = listOf(
    "千里之行，始于足下",
    "不积跬步，无以千里",
    "读书百遍，其义自见",
    "精诚所至，金石为开",
    "敏而好学，不耻下问",
    "博观约取，厚积薄发",
    "知行合一，止于至善",
    "温故知新，学以致用",
    "学而不厌，诲人不倦",
    "日拱一卒，功不唐捐",
    "行远自迩，登高自卑",
    "功崇惟志，业广惟勤",
    "君子务本，本立道生",
    "学贵有恒，勤能补拙",
    "心有所信，方能行远",
    "业精于勤，行成于思",
    "见贤思齐，反躬自省",
    "兼听则明，偏信则暗",
    "言必有物，行必有格",
    "好学深思，心知其意",
    "为者常成，行者常至",
    "道阻且长，行则将至",
    "志之所趋，无远弗届",
    "海纳百川，有容乃大",
    "壁立千仞，无欲则刚",
    "持心如水，行稳致远",
    "己所不欲，勿施于人",
    "静以修身，俭以养德",
    "淡泊明志，宁静致远",
    "桃李不言，下自成蹊",
    "前事不忘，后事之师",
    "尺有所短，寸有所长",
    "流水不腐，户枢不蠹",
    "一张一弛，文武之道",
    "生于忧患，死于安乐",
    "但行好事，莫问前程",
    "从善如登，从恶如崩",
    "蓬生麻中，不扶而直",
)

private enum class DashboardMetric(val key: String, val title: String, val description: String) {
    TodayLearned("today_learned", "今日新学", "今天第一次掌握的成语"),
    TodayReviewed("today_reviewed", "今日复习", "今天完成复习的成语"),
    AllLearned("all_learned", "累计掌握", "经过至少五轮成功复习且记忆间隔达到 30 天的成语"),
    AllReviewed("all_reviewed", "累计复习", "历史上复习过的成语，按成语去重显示"),
    Streak("streak", "连续学习", "连续学习期间完成过的成语"),
    Due("due", "当前待复习", "已经到达复习时间的成语"),
}

@Composable
fun HomeScreen(
    idioms: List<Idiom>,
    store: StudyStore,
    outerPadding: PaddingValues,
    onStartStudy: () -> Unit,
    onOpenIdiom: (Idiom) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    var selectedView by rememberSaveable { mutableIntStateOf(LEARNING_TAB) }
    var selectedMetricKey by rememberSaveable { mutableStateOf<String?>(null) }
    val snapshot = store.state
    val learnedToday = store.todayLearnedCount
    val reviewedToday = store.todayReviewedCount
    val reviewTarget = store.todayReviewPlanCount(idioms)
    val totalTarget = snapshot.dailyNewGoal + reviewTarget
    val totalCompleted = learnedToday + reviewedToday
    val progress = (totalCompleted.toFloat() / totalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)

    val selectedMetric = DashboardMetric.entries.firstOrNull { it.key == selectedMetricKey }
    if (selectedMetric != null) {
        BackHandler { selectedMetricKey = null }
        DashboardDetailScreen(
            metric = selectedMetric,
            idioms = idioms,
            store = store,
            outerPadding = outerPadding,
            onBack = { selectedMetricKey = null },
            onOpenIdiom = onOpenIdiom,
        )
        return
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HomeViewSwitch(
                    selected = selectedView,
                    onSelected = { selectedView = it },
                )
            }

            item {
                HomeGreeting(
                    onSearch = onOpenLibrary,
                    isDashboard = selectedView == DASHBOARD_TAB,
                )
            }

            if (selectedView == LEARNING_TAB) {
                item {
                    DailyPlanCard(
                        learned = learnedToday,
                        newGoal = snapshot.dailyNewGoal,
                        reviewed = reviewedToday,
                        reviewGoal = reviewTarget,
                        reviewIsAutomatic = snapshot.autoReviewEnabled,
                        progress = progress,
                        enabled = idioms.isNotEmpty(),
                        onStartStudy = onStartStudy,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HomeActionCard(
                            icon = Icons.Filled.AutoStories,
                            tint = Color(0xFF2E91DC),
                            value = store.todayLearnedCount.toString(),
                            title = "今日新学",
                            subtitle = "计划 ${snapshot.dailyNewGoal} 个",
                            modifier = Modifier.weight(1f),
                            onClick = onStartStudy,
                        )
                        HomeActionCard(
                            icon = Icons.Filled.Replay,
                            tint = Color(0xFFF08A32),
                            value = store.todayRemainingReviewCount(idioms).toString(),
                            title = "复习成语",
                            subtitle = if (snapshot.autoReviewEnabled) "遗忘曲线自动安排" else "今日自定计划",
                            modifier = Modifier.weight(1f),
                            onClick = onStartStudy,
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HomeActionCard(
                            icon = Icons.Filled.MenuBook,
                            tint = Color(0xFF18AFA8),
                            value = idioms.size.toString(),
                            title = "成语词库",
                            subtitle = "常见与易错成语",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenLibrary,
                        )
                        HomeActionCard(
                            icon = Icons.Filled.Star,
                            tint = Gold,
                            value = snapshot.favoriteIds.size.toString(),
                            title = "我的收藏",
                            subtitle = "随时集中复习",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenFavorites,
                        )
                    }
                }
            } else {
                item {
                    DashboardGrid(
                        idioms = idioms,
                        store = store,
                        onMetricSelected = { selectedMetricKey = it.key },
                    )
                }
                item {
                    StudyHistoryCard(store = store)
                }
            }
        }
    }
}

@Composable
private fun HomeViewSwitch(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 58.dp)
            .background(Color.White.copy(alpha = 0.82f), CircleShape)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("学习", "仪表盘").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected == index) Peach else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected == index) Ink else SecondaryInk,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HomeGreeting(onSearch: () -> Unit, isDashboard: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = if (isDashboard) "学习仪表盘" else dailyHeadline(),
                color = Ink,
                fontSize = if (isDashboard) 30.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isDashboard) 0.sp else 0.8.sp,
            )
            if (isDashboard) {
                Text(
                    text = "新学与复习，一目了然。",
                    color = SecondaryInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.65f), CircleShape)
                .clickable(onClick = onSearch)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = SecondaryInk.copy(alpha = 0.72f),
                modifier = Modifier.size(21.dp),
            )
            Text("搜索成语、拼音或释义", color = SecondaryInk.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun DailyPlanCard(
    learned: Int,
    newGoal: Int,
    reviewed: Int,
    reviewGoal: Int,
    reviewIsAutomatic: Boolean,
    progress: Float,
    enabled: Boolean,
    onStartStudy: () -> Unit,
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = SuccessGreen,
                    trackColor = Ink.copy(alpha = 0.08f),
                    strokeWidth = 8.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = "${learned + reviewed}/${newGoal + reviewGoal}",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (progress >= 1f) "今日计划已完成" else "今日学习计划",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (progress >= 1f) {
                        "做得很好，也可以继续巩固。"
                    } else {
                        "新学与复习穿插进行，减少集中遗忘。"
                    },
                    color = SecondaryInk,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlanProgressTile(
                title = "新学",
                value = "$learned/$newGoal",
                tint = Color(0xFF0A84FF),
                modifier = Modifier.weight(1f),
            )
            PlanProgressTile(
                title = if (reviewIsAutomatic) "复习 · 自动" else "复习 · 自定",
                value = "$reviewed/$reviewGoal",
                tint = Color(0xFFFF9F0A),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onStartStudy,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                text = if (progress >= 1f) "继续学习" else "开始今日学习",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlanProgressTile(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.09f), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(tint, CircleShape))
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, color = SecondaryInk, style = MaterialTheme.typography.labelSmall)
            Text(value, color = tint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    tint: Color,
    value: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 148.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MetricIcon(icon = icon, tint = tint)
                Spacer(Modifier.weight(1f))
                Text(value, color = tint, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = SecondaryInk,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DashboardGrid(
    idioms: List<Idiom>,
    store: StudyStore,
    onMetricSelected: (DashboardMetric) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DashboardMetricCard(
                icon = Icons.Filled.AutoStories,
                tint = Color(0xFF2E91DC),
                value = store.todayLearnedCount.toString(),
                title = "今日新学",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.TodayLearned) },
            )
            DashboardMetricCard(
                icon = Icons.Filled.Replay,
                tint = Color(0xFFF08A32),
                value = store.todayReviewedCount.toString(),
                title = "今日复习",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.TodayReviewed) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DashboardMetricCard(
                icon = Icons.Filled.CheckCircle,
                tint = SuccessGreen,
                value = store.masteredCount(idioms).toString(),
                title = "累计掌握",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.AllLearned) },
            )
            DashboardMetricCard(
                icon = Icons.Filled.MenuBook,
                tint = Color(0xFF9A68D8),
                value = store.totalReviewCount.toString(),
                title = "累计复习",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.AllReviewed) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DashboardMetricCard(
                icon = Icons.Filled.LocalFireDepartment,
                tint = Color(0xFFE76B35),
                value = store.displayedStreak.toString(),
                title = "连续学习/天",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.Streak) },
            )
            DashboardMetricCard(
                icon = Icons.Filled.Replay,
                tint = ErrorRed,
                value = store.dueCount(idioms).toString(),
                title = "当前待复习",
                modifier = Modifier.weight(1f),
                onClick = { onMetricSelected(DashboardMetric.Due) },
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    icon: ImageVector,
    tint: Color,
    value: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 126.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MetricIcon(icon = icon, tint = tint)
                Spacer(Modifier.weight(1f))
                Text(value, color = tint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardDetailScreen(
    metric: DashboardMetric,
    idioms: List<Idiom>,
    store: StudyStore,
    outerPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenIdiom: (Idiom) -> Unit,
) {
    val ids = when (metric) {
        DashboardMetric.TodayLearned -> store.todayLearnedIds
        DashboardMetric.TodayReviewed -> store.todayReviewedIds
        DashboardMetric.AllLearned -> store.masteredIds()
        DashboardMetric.AllReviewed -> store.allReviewedIds
        DashboardMetric.Streak -> store.allCompletedIds
        DashboardMetric.Due -> store.dueIds(idioms)
    }
    val content = idioms
        .filter { it.id in ids }
        .let { values ->
            if (metric == DashboardMetric.Due) {
                values.sortedBy { store.state.records[it.id]?.nextReviewAtMillis ?: Long.MAX_VALUE }
            } else {
                values
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(outerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.padding(top = 14.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.76f), CircleShape),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(metric.title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${metric.description} · ${content.size} 个",
                        color = SecondaryInk,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (content.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .background(Ink.copy(alpha = 0.07f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = SecondaryInk.copy(alpha = 0.60f),
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    Spacer(Modifier.height(15.dp))
                    Text("暂无相关成语", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("完成学习或复习后，这里会自动更新。", color = SecondaryInk)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(content, key = { it.id }) { idiom ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.74f)
                            ),
                        ) {
                            IdiomRow(
                                idiom = idiom,
                                isFavorite = store.isFavorite(idiom),
                                onClick = { onOpenIdiom(idiom) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(tint.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun StudyHistoryCard(store: StudyStore) {
    var selectedDays by rememberSaveable { mutableIntStateOf(7) }
    val history = store.recentHistory(selectedDays).reversed()
    val learnedTotal = history.sumOf { it.learnedCount }
    val reviewedTotal = history.sumOf { it.reviewedCount }
    val rangeText = if (history.isEmpty()) {
        "暂无数据"
    } else {
        val start = LocalDate.parse(history.first().dayKey)
        val end = LocalDate.parse(history.last().dayKey)
        "${start.format(DateTimeFormatter.ofPattern("M月d日"))} — ${end.format(DateTimeFormatter.ofPattern("M月d日"))}"
    }
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("学习趋势", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(rangeText, color = SecondaryInk, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .background(SuccessGreen.copy(alpha = 0.11f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "动态",
                    color = SuccessGreen,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HistoryRangeSelector(selectedDays = selectedDays, onSelected = { selectedDays = it })
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HistoryTotalCard(
                label = "新学",
                value = learnedTotal,
                color = Color(0xFF0A84FF),
                modifier = Modifier.weight(1f),
            )
            HistoryTotalCard(
                label = "复习",
                value = reviewedTotal,
                color = Color(0xFFFF9F0A),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF6F7F9).copy(alpha = 0.92f), RoundedCornerShape(22.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            StudyLineChart(history)
            Spacer(Modifier.height(4.dp))
            ChartDateLabels(history)
        }
    }
}

@Composable
private fun HistoryRangeSelector(selectedDays: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE9EBEF).copy(alpha = 0.86f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(3 to "近3天", 7 to "近7天", 30 to "近1月").forEach { (days, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedDays == days) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                        shape = RoundedCornerShape(11.dp),
                    )
                    .clickable { onSelected(days) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selectedDays == days) Ink else SecondaryInk,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun HistoryTotalCard(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(
                text = label,
                color = SecondaryInk,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = value.toString(),
            color = color,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StudyLineChart(history: List<DailyStudyRecord>) {
    val learnedColor = Color(0xFF0A84FF)
    val reviewedColor = Color(0xFFFF9F0A)
    val animation = remember { Animatable(0f) }
    LaunchedEffect(history) {
        animation.snapTo(0f)
        animation.animateTo(
            1f,
            animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        )
    }
    val maxValue = maxOf(
        1,
        history.maxOfOrNull { maxOf(it.learnedCount, it.reviewedCount) } ?: 1,
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp),
    ) {
        val left = 3.dp.toPx()
        val right = size.width - 3.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val chartHeight = bottom - top

        repeat(3) { index ->
            val y = top + chartHeight * index / 2f
            drawLine(
                color = Ink.copy(alpha = 0.055f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        fun xAt(index: Int): Float = if (history.size <= 1) {
            (left + right) / 2f
        } else {
            left + (right - left) * index / history.lastIndex.toFloat()
        }

        fun yAt(value: Int): Float = bottom -
            (value.toFloat() / maxValue.toFloat()) * chartHeight * animation.value

        fun drawSeries(color: Color, value: (DailyStudyRecord) -> Int) {
            val points = history.mapIndexed { index, record ->
                Offset(xAt(index), yAt(value(record)))
            }
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            val areaPath = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                if (points.isNotEmpty()) {
                    lineTo(points.last().x, bottom)
                    lineTo(points.first().x, bottom)
                    close()
                }
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.015f)),
                    startY = top,
                    endY = bottom,
                ),
            )
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 3.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            if (points.isNotEmpty()) {
                points.forEach { point ->
                    if (history.size <= 7) {
                        drawCircle(color = Color.White, radius = 4.8.dp.toPx(), center = point)
                        drawCircle(color = color, radius = 3.2.dp.toPx(), center = point)
                    }
                }
                val last = points.last()
                drawCircle(color = Color.White, radius = 6.2.dp.toPx(), center = last)
                drawCircle(color = color, radius = 4.1.dp.toPx(), center = last)
            }
        }

        drawSeries(learnedColor) { it.learnedCount }
        drawSeries(reviewedColor) { it.reviewedCount }
    }
}

@Composable
private fun ChartDateLabels(history: List<DailyStudyRecord>) {
    val indices = if (history.size <= 7) {
        history.indices.toList()
    } else {
        listOf(0, 7, 14, 21, history.lastIndex).distinct()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        indices.forEach { index ->
            val date = LocalDate.parse(history[index].dayKey)
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M/d")),
                color = SecondaryInk.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun dailyHeadline(): String = dailyHeadlines[
    (LocalDate.now().dayOfYear - 1) % dailyHeadlines.size
]
