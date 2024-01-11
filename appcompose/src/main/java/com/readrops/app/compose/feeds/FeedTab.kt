package com.readrops.app.compose.feeds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.readrops.db.entities.Feed
import org.koin.androidx.compose.getViewModel

object FeedTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Feeds"
        )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val viewModel = getViewModel<FeedViewModel>()

        val state by viewModel.feedsState.collectAsStateWithLifecycle()
        var showDialog by remember { mutableStateOf(false) }

        var selectedFeed by remember { mutableStateOf<Feed?>(null) }
        var showBottomSheet by remember { mutableStateOf(false) }

        if (showBottomSheet) {
            FeedModalBottomSheet(
                feed = selectedFeed!!,
                onDismissRequest = { showBottomSheet = false },
                onOpen = { uriHandler.openUri(selectedFeed!!.siteUrl!!) },
                onModify = { },
                onDelete = {},
            )
        }

        if (showDialog) {
            AddFeedDialog(
                onDismiss = { showDialog = false },
                onValidate = {
                    showDialog = false
                    viewModel.insertFeed(it)
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Feeds")
                    },
                    actions = {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (state) {
                    is FeedsState.LoadedState -> {
                        val feeds = (state as FeedsState.LoadedState).feeds

                        if (feeds.isNotEmpty()) {
                            LazyColumn {
                                items(
                                    items = feeds
                                ) { feed ->
                                    FeedItem(
                                        feed = feed,
                                        onClick = {
                                            selectedFeed = feed
                                            showBottomSheet = true
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            uriHandler.openUri(feed.siteUrl!!)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is FeedsState.ErrorState -> {

                    }

                    else -> {

                    }
                }

                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = { showDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        }
    }
}