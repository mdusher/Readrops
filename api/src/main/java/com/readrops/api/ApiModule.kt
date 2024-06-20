package com.readrops.api

import com.readrops.api.localfeed.LocalRSSDataSource
import com.readrops.api.services.Credentials
import com.readrops.api.services.freshrss.FreshRSSDataSource
import com.readrops.api.services.freshrss.NewFreshRSSDataSource
import com.readrops.api.services.freshrss.NewFreshRSSService
import com.readrops.api.services.freshrss.adapters.FreshRSSFeedsAdapter
import com.readrops.api.services.freshrss.adapters.FreshRSSFoldersAdapter
import com.readrops.api.services.freshrss.adapters.FreshRSSItemsAdapter
import com.readrops.api.services.freshrss.adapters.FreshRSSItemsIdsAdapter
import com.readrops.api.services.freshrss.adapters.FreshRSSUserInfoAdapter
import com.readrops.api.services.nextcloudnews.NewNextcloudNewsDataSource
import com.readrops.api.services.nextcloudnews.NewNextcloudNewsService
import com.readrops.api.services.nextcloudnews.NextNewsDataSource
import com.readrops.api.services.nextcloudnews.adapters.NextNewsFeedsAdapter
import com.readrops.api.services.nextcloudnews.adapters.NextNewsFoldersAdapter
import com.readrops.api.services.nextcloudnews.adapters.NextNewsItemsAdapter
import com.readrops.api.utils.AuthInterceptor
import com.readrops.api.utils.ErrorInterceptor
import com.readrops.db.entities.Item
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val apiModule = module {

    single {
        OkHttpClient.Builder()
            .callTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(get<ErrorInterceptor>())
            //.addInterceptor(NiddlerOkHttpInterceptor(get(), "niddler"))
            .build()
    }

    single { AuthInterceptor() }

    single { ErrorInterceptor() }

    single { LocalRSSDataSource(get()) }

    //region freshrss

    factory { params -> FreshRSSDataSource(get(parameters = { params })) }

    factory { params -> NewFreshRSSDataSource(get(parameters = { params })) }

    factory { (credentials: Credentials) ->
        Retrofit.Builder()
            .baseUrl(credentials.url)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get(named("freshrssMoshi"))))
            .build()
            .create(NewFreshRSSService::class.java)
    }

    single(named("freshrssMoshi")) {
        Moshi.Builder()
                .add(Types.newParameterizedType(List::class.java, Item::class.java), FreshRSSItemsAdapter())
                .add(Types.newParameterizedType(List::class.java, String::class.java), FreshRSSItemsIdsAdapter())
                .add(FreshRSSFeedsAdapter())
                .add(FreshRSSFoldersAdapter())
                .add(FreshRSSUserInfoAdapter())
                .build()
    }

    //endregion freshrss

    //region nextcloud news

    factory { params -> NextNewsDataSource(get(parameters = { params })) }

    factory { params -> NewNextcloudNewsDataSource(get(parameters = { params })) }

    factory { (credentials: Credentials) ->
        Retrofit.Builder()
                .baseUrl(credentials.url)
                .client(get())
                .addConverterFactory(MoshiConverterFactory.create(get(named("nextcloudNewsMoshi"))))
                .build()
                .create(NewNextcloudNewsService::class.java)
    }

    single(named("nextcloudNewsMoshi")) {
        Moshi.Builder()
                .add(NextNewsFeedsAdapter())
                .add(NextNewsFoldersAdapter())
                .add(Types.newParameterizedType(List::class.java, Item::class.java), NextNewsItemsAdapter())
                .build()
    }

    //endregion nextcloud news
}