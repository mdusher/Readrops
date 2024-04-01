package com.readrops.app.compose.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.readrops.api.utils.ApiUtils
import com.readrops.app.compose.R
import com.readrops.app.compose.account.credentials.AccountCredentialsScreen
import com.readrops.app.compose.account.selection.AccountSelectionDialog
import com.readrops.app.compose.account.selection.AccountSelectionScreen
import com.readrops.app.compose.timelime.ErrorListDialog
import com.readrops.app.compose.util.components.ErrorDialog
import com.readrops.app.compose.util.components.SelectableIconText
import com.readrops.app.compose.util.components.TwoChoicesDialog
import com.readrops.app.compose.util.theme.LargeSpacer
import com.readrops.app.compose.util.theme.MediumSpacer
import com.readrops.app.compose.util.theme.spacing

object AccountTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = stringResource(R.string.account)
        )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = getScreenModel<AccountScreenModel>()

        val closeHome by viewModel.closeHome.collectAsStateWithLifecycle()
        val state by viewModel.accountState.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        if (closeHome) {
            navigator.replaceAll(AccountSelectionScreen())
        }

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let { viewModel.parseOPMLFile(uri, context) }
            }

        LaunchedEffect(state.opmlImportError) {
            if (state.opmlImportError != null) {
                val action = snackbarHostState.showSnackbar(
                    message = context.resources.getQuantityString(
                        R.plurals.error_occurred,
                        1
                    ),
                    actionLabel = context.getString(R.string.details),
                    duration = SnackbarDuration.Short
                )

                if (action == SnackbarResult.ActionPerformed) {
                    viewModel.openDialog(DialogState.Error(state.opmlImportError!!))
                } else {
                    viewModel.closeDialog(DialogState.Error(state.opmlImportError!!))
                }
            }
        }

        LaunchedEffect(state.synchronizationErrors) {
            if (state.synchronizationErrors != null) {
                val action = snackbarHostState.showSnackbar(
                    message = context.resources.getQuantityString(
                        R.plurals.error_occurred,
                        state.synchronizationErrors!!.size
                    ),
                    actionLabel = context.getString(R.string.details),
                    duration = SnackbarDuration.Short
                )

                if (action == SnackbarResult.ActionPerformed) {
                    viewModel.openDialog(DialogState.ErrorList(state.synchronizationErrors!!))
                } else {
                    viewModel.closeDialog(DialogState.ErrorList(state.synchronizationErrors!!))
                }
            }
        }

        when (val dialog = state.dialog) {
            is DialogState.DeleteAccount -> {
                TwoChoicesDialog(
                    title = stringResource(R.string.delete_account),
                    text = stringResource(R.string.delete_account_question),
                    icon = rememberVectorPainter(image = Icons.Default.Delete),
                    confirmText = stringResource(R.string.delete),
                    dismissText = stringResource(R.string.cancel),
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = {
                        viewModel.closeDialog()
                        viewModel.deleteAccount()
                    }
                )
            }

            is DialogState.NewAccount -> {
                AccountSelectionDialog(
                    onDismiss = { viewModel.closeDialog() },
                    onValidate = { accountType ->
                        viewModel.closeDialog()
                        navigator.push(AccountCredentialsScreen(accountType, state.account))
                    }
                )
            }

            is DialogState.OPMLImport -> {
                OPMLImportProgressDialog(
                    currentFeed = dialog.currentFeed,
                    feedCount = dialog.feedCount,
                    feedMax = dialog.feedMax
                )
            }

            is DialogState.ErrorList -> {
                ErrorListDialog(
                    errorResult = dialog.errorResult,
                    onDismiss = { viewModel.closeDialog(dialog) }
                )
            }

            is DialogState.Error -> {
                ErrorDialog(
                    exception = dialog.exception,
                    onDismiss = { viewModel.closeDialog(dialog) }
                )
            }

            else -> {}
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.account)) },
                    actions = {
                        IconButton(
                            onClick = { }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openDialog(DialogState.NewAccount) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_account),
                        contentDescription = null
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_freshrss),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )

                    MediumSpacer()

                    Text(
                        text = state.account.accountName!!,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                LargeSpacer()

                SelectableIconText(
                    icon = painterResource(id = R.drawable.ic_add_account),
                    text = stringResource(R.string.credentials),
                    style = MaterialTheme.typography.titleMedium,
                    spacing = MaterialTheme.spacing.mediumSpacing,
                    padding = MaterialTheme.spacing.mediumSpacing,
                    onClick = { }
                )

                SelectableIconText(
                    icon = painterResource(id = R.drawable.ic_notifications),
                    text = stringResource(R.string.notifications),
                    style = MaterialTheme.typography.titleMedium,
                    spacing = MaterialTheme.spacing.mediumSpacing,
                    padding = MaterialTheme.spacing.mediumSpacing,
                    onClick = { }
                )

                SelectableIconText(
                    icon = painterResource(id = R.drawable.ic_import_export),
                    text = stringResource(R.string.opml_import_export),
                    style = MaterialTheme.typography.titleMedium,
                    spacing = MaterialTheme.spacing.mediumSpacing,
                    padding = MaterialTheme.spacing.mediumSpacing,
                    onClick = { launcher.launch(ApiUtils.OPML_MIMETYPES.toTypedArray()) }
                )

                SelectableIconText(
                    icon = rememberVectorPainter(image = Icons.Default.AccountCircle),
                    text = stringResource(R.string.delete_account),
                    style = MaterialTheme.typography.titleMedium,
                    spacing = MaterialTheme.spacing.mediumSpacing,
                    padding = MaterialTheme.spacing.mediumSpacing,
                    color = MaterialTheme.colorScheme.error,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.openDialog(DialogState.DeleteAccount) }
                )
            }
        }
    }
}