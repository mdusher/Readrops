package com.readrops.app

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.readrops.api.services.Credentials
import com.readrops.app.account.AccountScreenModel
import com.readrops.app.account.credentials.AccountCredentialsScreenMode
import com.readrops.app.account.credentials.AccountCredentialsScreenModel
import com.readrops.app.account.selection.AccountSelectionScreenModel
import com.readrops.app.feeds.FeedScreenModel
import com.readrops.app.item.ItemScreenModel
import com.readrops.app.more.preferences.PreferencesScreenModel
import com.readrops.app.notifications.NotificationsScreenModel
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.repositories.FreshRSSRepository
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.repositories.LocalRSSRepository
import com.readrops.app.repositories.NextcloudNewsRepository
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.app.util.DataStorePreferences
import com.readrops.app.util.Preferences
import com.readrops.db.entities.account.Account
import com.readrops.db.entities.account.AccountType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {

    factory { TimelineScreenModel(get(), get(), get()) }

    factory { FeedScreenModel(get(), get(), get(), androidContext()) }

    factory { AccountSelectionScreenModel(get()) }

    factory { AccountScreenModel(get()) }

    factory { (itemId: Int) -> ItemScreenModel(get(), itemId, get()) }

    factory { (accountType: Account, mode: AccountCredentialsScreenMode) ->
        AccountCredentialsScreenModel(accountType, mode, get())
    }

    factory { (account: Account) -> NotificationsScreenModel(account, get(), get(), get()) }

    factory { PreferencesScreenModel(get()) }

    single { GetFoldersWithFeeds(get()) }

    factory<BaseRepository> { (account: Account) ->
        when (account.accountType) {
            AccountType.LOCAL -> LocalRSSRepository(get(), get(), account)
            AccountType.FRESHRSS -> FreshRSSRepository(
                get(), account,
                get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )
            AccountType.NEXTCLOUD_NEWS -> NextcloudNewsRepository(
                get(), account,
                get(parameters = { parametersOf(Credentials.toCredentials(account)) })
            )
            else -> throw IllegalArgumentException("Unknown account type")
        }
    }

    single {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            "account_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(SharedPreferencesMigration(get(),"settings")),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { get<Context>().preferencesDataStoreFile("settings") }
        )
    }

    single { DataStorePreferences(get()) }

    single { Preferences(get()) }

    single { NotificationManagerCompat.from(get()) }
}