package com.readrops.app;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.Patterns;

import com.readrops.app.database.ItemWithFeed;
import com.readrops.app.database.entities.Feed;
import com.readrops.app.database.entities.Item;
import com.readrops.readropslibrary.HtmlParser;
import com.readrops.readropslibrary.ParsingResult;
import com.readrops.readropslibrary.QueryCallback;
import com.readrops.readropslibrary.Utils.LibUtils;
import com.readrops.readropslibrary.localfeed.AFeed;
import com.readrops.readropslibrary.localfeed.RSSNetwork;
import com.readrops.readropslibrary.localfeed.atom.ATOMFeed;
import com.readrops.readropslibrary.localfeed.json.JSONFeed;
import com.readrops.readropslibrary.localfeed.rss.RSSFeed;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocalFeedRepository extends ARepository implements QueryCallback {

    private static final String TAG = LocalFeedRepository.class.getSimpleName();

    private LiveData<List<ItemWithFeed>> itemsWhithFeed;

    public LocalFeedRepository(Application application) {
        super(application);

        itemsWhithFeed = database.itemDao().getAllItemWithFeeds();
    }

    public LiveData<List<ItemWithFeed>> getItemsWhithFeed() {
        return itemsWhithFeed;
    }

    @Override
    public void sync() {
        executor.execute(() -> {
            RSSNetwork rssNet = new RSSNetwork();
            rssNet.setCallback(this);
            List<Feed> feedList = database.feedDao().getAllFeeds();

            for (Feed feed : feedList) {
                try {
                    HashMap<String, String> headers = new HashMap<>();
                    if (feed.getEtag() != null)
                        headers.put(LibUtils.IF_NONE_MATCH_HEADER, feed.getEtag());
                    if (feed.getLastModified() != null)
                        headers.put(LibUtils.IF_MODIFIED_HEADER, feed.getLastModified());

                    rssNet.request(feed.getUrl(), headers);
                } catch (Exception e) {
                    failureCallBackInMainThread(e);
                }
            }

            postCallBackSuccess();
        });
    }

    @Override
    public void addFeed(ParsingResult result) {
        executor.execute(() -> {
            try {
                RSSNetwork rssNet = new RSSNetwork();
                rssNet.setCallback(this);
                rssNet.request(result.getUrl(), new HashMap<>());

                postCallBackSuccess();
            } catch (Exception e) {
                failureCallBackInMainThread(e);
            }
        });
    }

    @Override
    public void deleteFeed(Item item) {
        executor.execute(() -> {

        });
    }

    @Override
    public void moveFeed(Item item) {

    }


    @Override
    public void onSyncSuccess(AFeed feed, RSSNetwork.RSSType type) {
        switch (type) {
            case RSS_2:
                parseRSSItems(((RSSFeed) feed));
                break;
            case RSS_ATOM:
                parseATOMItems(((ATOMFeed) feed));
                break;
            case RSS_JSON:
                parseJSONItems(((JSONFeed) feed));
                break;
        }
    }

    @Override
    public void onSyncFailure(Exception e) {
        failureCallBackInMainThread(e);
    }

    private void parseRSSItems(RSSFeed rssFeed) {
        try {
            Feed dbFeed = database.feedDao().getFeedByUrl(rssFeed.getChannel().getFeedUrl());
            if (dbFeed == null) {
                dbFeed = Feed.feedFromRSS(rssFeed);

                setFavIconUtils(dbFeed);
                dbFeed.setId((int)(database.feedDao().insert(dbFeed)));
            } else
                database.feedDao().updateHeaders(rssFeed.getEtag(), rssFeed.getLastModified(), dbFeed.getId());

            List<Item> dbItems = Item.itemsFromRSS(rssFeed.getChannel().getItems(), dbFeed);
            insertItems(dbItems, dbFeed);

        } catch (Exception e) {
            failureCallBackInMainThread(e);
        }

    }

    private void parseATOMItems(ATOMFeed feed) {
        try {
            Feed dbFeed = database.feedDao().getFeedByUrl(feed.getUrl());
            if (dbFeed == null) {
                dbFeed = Feed.feedFromATOM(feed);

                setFavIconUtils(dbFeed);
                dbFeed.setId((int)(database.feedDao().insert(dbFeed)));
            } else
                database.feedDao().updateHeaders(feed.getEtag(), feed.getLastModified(), dbFeed.getId());

            List<Item> dbItems = Item.itemsFromATOM(feed.getEntries(), dbFeed);
            insertItems(dbItems, dbFeed);

        } catch (Exception e) {
            failureCallBackInMainThread(e);
        }
    }

    private void parseJSONItems(JSONFeed feed) {
        try {
            Feed dbFeed = database.feedDao().getFeedByUrl(feed.getFeedUrl());
            if (dbFeed == null) {
                dbFeed = Feed.feedFromJSON(feed);

                setFavIconUtils(dbFeed);
                dbFeed.setId((int)(database.feedDao().insert(dbFeed)));
            } else
                database.feedDao().updateHeaders(feed.getEtag(), feed.getLastModified(), dbFeed.getId());

            List<Item> dbItems = Item.itemsFromJSON(feed.getItems(), dbFeed);
            insertItems(dbItems, dbFeed);

        } catch (Exception e) {
            failureCallBackInMainThread(e);
        }
    }

    private void insertItems(List<Item> items, Feed feed) {
        for (Item dbItem : items) {
            if (!Boolean.valueOf(database.itemDao().guidExist(dbItem.getGuid()))) {
                if (dbItem.getDescription() != null) {
                    if (dbItem.getImageLink() == null)
                        dbItem.setImageLink(HtmlParser.getDescImageLink(dbItem.getDescription(), feed.getSiteUrl()));

                    dbItem.setDescription(Jsoup.parse(dbItem.getDescription()).text());
                }

                database.itemDao().insert(dbItem);
                Log.d(TAG, "adding " + dbItem.getTitle());
            }
        }
    }

    private void setFavIconUtils(Feed feed) throws IOException {
        String favUrl = HtmlParser.getFaviconLink(feed.getSiteUrl());
        if (favUrl != null && Patterns.WEB_URL.matcher(favUrl).matches()) {
            feed.setIconUrl(favUrl);
            feed.setColor(getFaviconColor(favUrl));
        }
    }

    private @ColorInt int getFaviconColor(String favUrl) throws IOException {
        Bitmap favicon = getFaviconFromUrl(favUrl);
        Palette palette = Palette.from(favicon).generate();

        return palette.getDominantSwatch().getRgb();
    }

    private Bitmap getFaviconFromUrl(String url) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        Response response = okHttpClient.newCall(request).execute();

        if (response.isSuccessful()) {
            InputStream inputStream = response.body().byteStream();
            return BitmapFactory.decodeStream(inputStream);
        } else
            return null;
    }

}
