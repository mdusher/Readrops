package com.readrops.app.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.readrops.app.R
import com.readrops.app.util.components.dialog.BaseDialog
import com.readrops.app.util.components.RefreshIndicator

@Composable
fun OPMLImportProgressDialog(
    currentFeed: String,
    feedCount: Int,
    feedMax: Int,
) {
    BaseDialog(
        title = stringResource(id = R.string.opml_import),
        icon = painterResource(R.drawable.ic_import_export),
        onDismiss = {}
    ) {
        RefreshIndicator(
            currentFeed = currentFeed,
            feedCount = feedCount,
            feedMax = feedMax
        )
    }
}