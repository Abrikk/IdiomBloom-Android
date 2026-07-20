package com.idiombloom.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.idiombloom.app.data.DictionaryManager
import com.idiombloom.app.data.Idiom
import com.idiombloom.app.data.SpeechController
import com.idiombloom.app.data.StudyStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun IdiomBloomApp() {
    val context = LocalContext.current
    val dictionaryManager = remember { DictionaryManager(context) }
    val idioms = dictionaryManager.idioms
    val store = remember { StudyStore(context) }
    val speech = remember { SpeechController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedIdiom by remember { mutableStateOf<Idiom?>(null) }
    var studyItems by remember { mutableStateOf<List<Idiom>?>(null) }

    DisposableEffect(speech) {
        onDispose { speech.close() }
    }

    DisposableEffect(lifecycleOwner, dictionaryManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                scope.launch { dictionaryManager.autoCheckIfNeeded() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        studyItems != null -> {
            BackHandler { studyItems = null }
            StudyScreen(
                initialItems = studyItems.orEmpty(),
                store = store,
                speech = speech,
                onClose = { studyItems = null },
            )
        }

        selectedIdiom != null -> {
            BackHandler { selectedIdiom = null }
            IdiomDetailScreen(
                idiom = selectedIdiom!!,
                store = store,
                speech = speech,
                onBack = { selectedIdiom = null },
            )
        }

        else -> {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(27.dp),
                                ambientColor = Ink.copy(alpha = 0.07f),
                                spotColor = Ink.copy(alpha = 0.09f),
                            )
                            .clip(RoundedCornerShape(27.dp)),
                        containerColor = Color.White.copy(alpha = 0.91f),
                        tonalElevation = 0.dp,
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("今日") },
                            colors = navigationColors(),
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
                            label = { Text("词库") },
                            colors = navigationColors(),
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            label = { Text("收藏") },
                            colors = navigationColors(),
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("我的") },
                            colors = navigationColors(),
                        )
                    }
                },
            ) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(
                        idioms = idioms,
                        store = store,
                        outerPadding = padding,
                        onStartStudy = { studyItems = store.makeDailyStudyQueue(idioms) },
                        onOpenIdiom = { selectedIdiom = it },
                        onOpenLibrary = { selectedTab = 1 },
                        onOpenFavorites = { selectedTab = 2 },
                    )

                    1 -> LibraryScreen(
                        idioms = idioms,
                        store = store,
                        outerPadding = padding,
                        onOpenIdiom = { selectedIdiom = it },
                    )

                    2 -> FavoritesScreen(
                        idioms = idioms,
                        store = store,
                        outerPadding = padding,
                        onOpenIdiom = { selectedIdiom = it },
                    )

                    else -> DictionarySettingsScreen(
                        manager = dictionaryManager,
                        store = store,
                        outerPadding = padding,
                    )
                }
            }
        }
    }
}

@Composable
private fun navigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Ink,
    selectedTextColor = Ink,
    indicatorColor = Lavender.copy(alpha = 0.92f),
    unselectedIconColor = SecondaryInk.copy(alpha = 0.62f),
    unselectedTextColor = SecondaryInk,
)
