package com.idiombloom.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiombloom.app.data.Idiom
import com.idiombloom.app.data.ReviewOutcome
import com.idiombloom.app.data.SpeechController
import com.idiombloom.app.data.StudyStore
import kotlinx.coroutines.launch

private const val REVEAL_NONE = 0
private const val REVEAL_FROM_KNOWN = 1
private const val REVEAL_FROM_LOOKUP = 2

@Composable
fun StudyScreen(
    initialItems: List<Idiom>,
    store: StudyStore,
    speech: SpeechController,
    onClose: () -> Unit,
) {
    val queue = remember(initialItems) { initialItems.toMutableStateList() }
    var completed by remember { mutableIntStateOf(0) }
    var revealedId by rememberSaveable { mutableStateOf<String?>(null) }
    var revealMode by rememberSaveable { mutableIntStateOf(REVEAL_NONE) }
    var isTransitioning by remember { mutableStateOf(false) }
    val transitionProgress = remember { Animatable(1f) }
    val transitionScope = rememberCoroutineScope()
    val contentOffsetPx = with(LocalDensity.current) { 18.dp.toPx() }
    val total = initialItems.size
    val current = queue.firstOrNull()
    val isRevealed = current?.id == revealedId

    fun runStudyTransition(changeContent: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true
        transitionScope.launch {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 125, easing = FastOutSlowInEasing),
            )
            changeContent()
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 310, easing = FastOutSlowInEasing),
            )
            isTransitioning = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (current != null) {
                if (isRevealed) {
                    if (revealMode == REVEAL_FROM_KNOWN) {
                        AnswerBar(
                            leftTitle = "记错了",
                            leftIcon = Icons.Filled.Replay,
                            leftAccent = ErrorRed,
                            enabled = !isTransitioning,
                            motionProgress = transitionProgress.value,
                            onLeft = {
                                runStudyTransition {
                                    store.review(current, ReviewOutcome.Forgot)
                                    val reviewed = queue.removeAt(0)
                                    queue.add(minOf(3, queue.size), reviewed)
                                    revealedId = null
                                    revealMode = REVEAL_NONE
                                }
                            },
                            rightTitle = "我会了",
                            rightIcon = Icons.Filled.Check,
                            rightAccent = SuccessGreen,
                            onRight = {
                                runStudyTransition {
                                    store.review(current, ReviewOutcome.Remembered)
                                    queue.removeAt(0)
                                    completed += 1
                                    revealedId = null
                                    revealMode = REVEAL_NONE
                                }
                            },
                        )
                    } else {
                        AnswerBar(
                            leftTitle = "陌生",
                            leftIcon = Icons.Filled.Replay,
                            leftAccent = ErrorRed,
                            enabled = !isTransitioning,
                            motionProgress = transitionProgress.value,
                            onLeft = {
                                runStudyTransition {
                                    store.review(current, ReviewOutcome.Forgot)
                                    val reviewed = queue.removeAt(0)
                                    queue.add(minOf(3, queue.size), reviewed)
                                    revealedId = null
                                    revealMode = REVEAL_NONE
                                }
                            },
                            rightTitle = "模糊",
                            rightIcon = Icons.Filled.AutoStories,
                            rightAccent = Color(0xFFF08A32),
                            onRight = {
                                runStudyTransition {
                                    store.review(current, ReviewOutcome.Fuzzy)
                                    queue.removeAt(0)
                                    completed += 1
                                    revealedId = null
                                    revealMode = REVEAL_NONE
                                }
                            },
                        )
                    }
                } else {
                    RecallBar(
                        enabled = !isTransitioning,
                        motionProgress = transitionProgress.value,
                        onReveal = {
                            runStudyTransition {
                                revealedId = current.id
                                revealMode = REVEAL_FROM_LOOKUP
                            }
                        },
                        onClaimKnown = {
                            runStudyTransition {
                                revealedId = current.id
                                revealMode = REVEAL_FROM_KNOWN
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = transitionProgress.value
                        translationY = (1f - transitionProgress.value) * contentOffsetPx
                        val scale = 0.988f + transitionProgress.value * 0.012f
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                if (current == null) {
                    CompletionView(total = total, onClose = onClose)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 780.dp),
                    ) {
                    Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                        StudyTopBar(
                            completed = completed,
                            total = total,
                            itemKind = if (store.isReviewItem(current)) "复习" else "新学",
                            isFavorite = store.isFavorite(current),
                            onClose = onClose,
                            onFavorite = { store.toggleFavorite(current) },
                        )
                    }

                    if (!isRevealed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 22.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = current.text,
                                color = Ink,
                                fontSize = 42.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                            )
                            TextButton(onClick = { speech.speak(current.text) }) {
                                Text("[${current.pinyin}]", color = SecondaryInk, fontSize = 19.sp)
                                Spacer(Modifier.width(7.dp))
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "朗读",
                                    tint = SecondaryInk,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = current.text,
                                        color = Ink,
                                        fontSize = 38.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 3.sp,
                                    )
                                    TextButton(onClick = { speech.speak(current.text) }) {
                                        Text("[${current.pinyin}]", color = SecondaryInk, fontSize = 18.sp)
                                        Spacer(Modifier.width(7.dp))
                                        Icon(
                                            Icons.Filled.VolumeUp,
                                            contentDescription = "朗读",
                                            tint = SecondaryInk,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                    TonePill(current)
                                }
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SectionTitle("释义", Icons.Filled.AutoStories)
                                    Text(
                                        text = current.meaning,
                                        color = Ink.copy(alpha = 0.92f),
                                        lineHeight = 27.sp,
                                    )
                                    if (current.note.isNotBlank()) {
                                        Text(
                                            text = current.note,
                                            color = SecondaryInk,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 23.sp,
                                        )
                                    }
                                }
                            }

                            item {
                                StudyExampleCard(
                                    title = "日常表达",
                                    icon = Icons.Filled.FormatQuote,
                                    text = current.dailyExample,
                                )
                            }
                            item {
                                StudyExampleCard(
                                    title = "文学例句",
                                    icon = Icons.Filled.FormatQuote,
                                    text = current.literaryExample,
                                    source = current.literarySource,
                                )
                            }
                            item {
                                StudyExampleCard(
                                    title = "正式表达 · 新闻/杂志/报刊",
                                    icon = Icons.Filled.Newspaper,
                                    text = current.formalExample,
                                    source = current.formalSource,
                                )
                            }
                            item { Spacer(Modifier.height(6.dp)) }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun RecallBar(
    enabled: Boolean,
    motionProgress: Float,
    onReveal: () -> Unit,
    onClaimKnown: () -> Unit,
) {
    val offsetPx = with(LocalDensity.current) { 10.dp.toPx() }
    Surface(
        modifier = Modifier.graphicsLayer {
            alpha = motionProgress
            translationY = (1f - motionProgress) * offsetPx
        },
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecallButton(
                title = "查看释义",
                icon = Icons.Filled.AutoStories,
                modifier = Modifier.weight(2f),
                containerColor = Color.White,
                contentColor = Ink,
                enabled = enabled,
                onClick = onReveal,
            )
            RecallButton(
                title = "我会",
                icon = Icons.Filled.Check,
                modifier = Modifier.weight(1f),
                containerColor = SuccessGreen,
                contentColor = Color.White,
                enabled = enabled,
                onClick = onClaimKnown,
            )
        }
    }
}

@Composable
private fun RecallButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 180),
        label = "recallButtonScale",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.55f),
            disabledContentColor = contentColor.copy(alpha = 0.72f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StudyTopBar(
    completed: Int,
    total: Int,
    itemKind: String,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(icon = Icons.Filled.Close, description = "退出学习", onClick = onClose)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$itemKind · ${minOf(completed + 1, maxOf(total, 1))} / ${maxOf(total, 1)}",
                color = SecondaryInk,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { completed.toFloat() / maxOf(total, 1) },
                modifier = Modifier.width(94.dp),
                color = SuccessGreen,
                trackColor = Ink.copy(alpha = 0.08f),
            )
        }
        RoundIconButton(
            icon = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            description = if (isFavorite) "取消收藏" else "收藏",
            tint = if (isFavorite) Gold else Ink,
            onClick = onFavorite,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Ink,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(Color.White.copy(alpha = 0.74f), CircleShape),
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SecondaryInk, modifier = Modifier.size(19.dp))
        Text(title, color = SecondaryInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StudyExampleCard(
    title: String,
    icon: ImageVector,
    text: String,
    source: String? = null,
) {
    GlassCard {
        SectionTitle(title, icon)
        Spacer(Modifier.height(12.dp))
        Text(text, color = Ink.copy(alpha = 0.92f), lineHeight = 26.sp)
        if (!source.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "出处：$source",
                color = SecondaryInk.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun AnswerBar(
    leftTitle: String,
    leftIcon: ImageVector,
    leftAccent: Color,
    onLeft: () -> Unit,
    rightTitle: String,
    rightIcon: ImageVector,
    rightAccent: Color,
    enabled: Boolean,
    motionProgress: Float,
    onRight: () -> Unit,
) {
    val offsetPx = with(LocalDensity.current) { 10.dp.toPx() }
    Surface(
        modifier = Modifier.graphicsLayer {
            alpha = motionProgress
            translationY = (1f - motionProgress) * offsetPx
        },
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnswerButton(
                title = leftTitle,
                icon = leftIcon,
                accent = leftAccent,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = onLeft,
            )
            AnswerButton(
                title = rightTitle,
                icon = rightIcon,
                accent = rightAccent,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = onRight,
            )
        }
    }
}

@Composable
private fun AnswerButton(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 190),
        label = "answerButtonScale",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = accent.copy(alpha = 0.55f),
            disabledContentColor = Color.White.copy(alpha = 0.72f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompletionView(total: Int, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(SuccessGreen.copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(68.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (total == 0) "今天没有待学习内容" else "本轮学习完成",
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (total == 0) "回来看看词库，或明天继续复习。" else "完成了 $total 个成语，继续保持。",
            color = SecondaryInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp),
        ) {
            Text("回到首页", fontWeight = FontWeight.Bold)
        }
    }
}
