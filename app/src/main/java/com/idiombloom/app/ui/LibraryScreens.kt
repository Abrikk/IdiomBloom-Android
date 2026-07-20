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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiombloom.app.data.Idiom
import com.idiombloom.app.data.SpeechController
import com.idiombloom.app.data.StudyStore

@Composable
fun LibraryScreen(
    idioms: List<Idiom>,
    store: StudyStore,
    outerPadding: PaddingValues,
    onOpenIdiom: (Idiom) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("全部") }
    val filtered = remember(idioms, query, selectedCategory) {
        idioms.filter { idiom ->
            val matchesCategory = selectedCategory == "全部" || selectedCategory in idiom.tags
            val matchesQuery = query.isBlank() ||
                idiom.text.contains(query, ignoreCase = true) ||
                idiom.pinyin.contains(query, ignoreCase = true) ||
                idiom.meaning.contains(query, ignoreCase = true) ||
                idiom.tags.any { it.contains(query, ignoreCase = true) }
            matchesCategory && matchesQuery
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
            Text(
                text = "成语词库",
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp, bottom = 14.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索成语、拼音或释义") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.80f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.72f),
                    focusedIndicatorColor = Ink,
                    unfocusedIndicatorColor = Ink.copy(alpha = 0.10f),
                ),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dictionaryCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Peach,
                            selectedLabelColor = Ink,
                        ),
                    )
                }
            }
            Text(
                text = "共 ${filtered.size} 条",
                color = SecondaryInk,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "没有找到相关成语",
                    description = "可以尝试搜索成语、拼音、释义或标签。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filtered, key = { it.id }) { idiom ->
                        IdiomRow(
                            idiom = idiom,
                            isFavorite = store.isFavorite(idiom),
                            onClick = { onOpenIdiom(idiom) },
                            showDivider = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    idioms: List<Idiom>,
    store: StudyStore,
    outerPadding: PaddingValues,
    onOpenIdiom: (Idiom) -> Unit,
) {
    val favorites = idioms.filter { it.id in store.state.favoriteIds }

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
            Text(
                text = "我的收藏",
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp, bottom = 14.dp),
            )

            if (favorites.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.StarBorder,
                    title = "还没有收藏",
                    description = "学习或浏览成语时，点按右上角的星标即可收藏。",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(favorites, key = { it.id }) { idiom ->
                        IdiomRow(
                            idiom = idiom,
                            isFavorite = true,
                            onClick = { onOpenIdiom(idiom) },
                            showDivider = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdiomDetailScreen(
    idiom: Idiom,
    store: StudyStore,
    speech: SpeechController,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.72f), CircleShape),
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { store.toggleFavorite(idiom) },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.72f), CircleShape),
                    ) {
                        Icon(
                            imageVector = if (store.isFavorite(idiom)) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (store.isFavorite(idiom)) "取消收藏" else "收藏",
                            tint = if (store.isFavorite(idiom)) Gold else Ink,
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = idiom.text,
                        color = Ink,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        letterSpacing = 3.sp,
                    )
                    Row(
                        modifier = Modifier
                            .background(Color.Transparent, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text("[${idiom.pinyin}]", color = SecondaryInk, fontSize = 19.sp)
                        IconButton(onClick = { speech.speak(idiom.text) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "朗读", tint = SecondaryInk)
                        }
                    }
                    TonePill(idiom)
                }
            }

            item {
                DetailCard(title = "释义", icon = Icons.Filled.AutoStories) {
                    Text(idiom.meaning, color = Ink, lineHeight = 26.sp)
                    if (idiom.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(idiom.note, color = SecondaryInk, lineHeight = 23.sp)
                    }
                }
            }
            item {
                DetailCard(title = "日常表达", icon = Icons.Filled.FormatQuote) {
                    Text(idiom.dailyExample, color = Ink, lineHeight = 26.sp)
                }
            }
            item {
                DetailCard(title = "文学例句", icon = Icons.Filled.FormatQuote) {
                    Text(idiom.literaryExample, color = Ink, lineHeight = 26.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "出处：${idiom.literarySource}",
                        color = SecondaryInk.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item {
                DetailCard(title = "正式表达 · 新闻/杂志/报刊", icon = Icons.Filled.Newspaper) {
                    Text(idiom.formalExample, color = Ink, lineHeight = 26.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "出处：${idiom.formalSource}",
                        color = SecondaryInk.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(idiom.tags) { tag ->
                        Text(
                            text = tag,
                            color = SecondaryInk,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.74f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

private val dictionaryCategories = listOf(
    "全部",
    "高频常用",
    "易错成语",
    "易读错",
    "易写错",
    "望文生义",
    "褒贬误用",
    "对象误用",
    "谦敬误用",
    "近义辨析",
)

@Composable
private fun DetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, tint = SecondaryInk, modifier = Modifier.size(19.dp))
            Text(title, color = SecondaryInk, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = SecondaryInk.copy(alpha = 0.62f), modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(description, color = SecondaryInk, textAlign = TextAlign.Center)
    }
}
