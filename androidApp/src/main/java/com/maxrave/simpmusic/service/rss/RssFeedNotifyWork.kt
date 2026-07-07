package com.maxrave.simpmusic.service.rss

import android.content.Context
import android.util.Xml
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.domain.extension.epochMillisToLocalDateTime
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.service.test.notification.NotificationHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Periodically scans the personal blog RSS feed and pushes a local notification for each
 * post that is (a) published within the [WINDOW_MS] window relative to the run time AND
 * (b) not already stored in the notification DB.
 *
 * The DB is the source of truth for "already pushed" — see [CommonRepository.isNotificationExists].
 * The time window is intentionally wider than the scheduling interval so a delayed WorkManager
 * run (Doze, constraints) does not skip a post; the DB check still guarantees one push per post.
 */
class RssFeedNotifyWork(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val commonRepository: CommonRepository by inject()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            try {
                Logger.w(TAG, "doWork: fetching $FEED_URL")
                val items = parseRss(fetchFeed(FEED_URL))
                val nowMillis = System.currentTimeMillis()

                // Oldest first so notifications arrive in chronological order.
                items.sortedBy { it.pubMillis }.forEach { item ->
                    val withinWindow = item.pubMillis > 0L && (nowMillis - item.pubMillis) <= WINDOW_MS
                    if (withinWindow && !commonRepository.isNotificationExists(item.link)) {
                        NotificationHandler.createBlogNotificationChannel(applicationContext)
                        NotificationHandler.createBlogNotification(
                            context = applicationContext,
                            title = item.title,
                            text = item.description,
                            url = item.link,
                        )
                        commonRepository.insertNotification(
                            NotificationEntity(
                                channelId = "",
                                name = item.title,
                                type = NotificationEntity.TYPE_BLOG,
                                link = item.link,
                                description = item.description,
                                time = if (item.pubMillis > 0L) epochMillisToLocalDateTime(item.pubMillis) else now(),
                            ),
                        )
                        Logger.w(TAG, "Pushed blog notification: ${item.title}")
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Logger.e(TAG, "doWork failed: ${e.message}")
                Result.retry()
            }
        }

    private fun fetchFeed(urlStr: String): String {
        val connection =
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SimpMusic")
            }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRss(xml: String): List<RssItem> {
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        val items = mutableListOf<RssItem>()
        var insideItem = false
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG ->
                    when (parser.name) {
                        "item" -> {
                            insideItem = true
                            title = ""
                            link = ""
                            description = ""
                            pubDate = ""
                        }
                        "title" -> if (insideItem) title = parser.nextText().trim()
                        "link" -> if (insideItem) link = parser.nextText().trim()
                        "description" -> if (insideItem) description = parser.nextText().trim()
                        "pubDate" -> if (insideItem) pubDate = parser.nextText().trim()
                    }

                XmlPullParser.END_TAG ->
                    if (parser.name == "item") {
                        insideItem = false
                        if (link.isNotEmpty()) {
                            items.add(RssItem(title, link, description, parsePubMillis(pubDate)))
                        }
                    }
            }
            event = parser.next()
        }
        return items
    }

    private fun parsePubMillis(pubDate: String): Long =
        try {
            // RFC-822, e.g. "Fri, 26 Jun 2026 19:05:06 GMT"
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(pubDate)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }

    private data class RssItem(
        val title: String,
        val link: String,
        val description: String,
        val pubMillis: Long,
    )

    companion object {
        private const val TAG = "RssFeedNotifyWork"
        const val FEED_URL = "https://www.maxrave.dev/rss.xml"

        // 48h — wider than the 24h schedule so a delayed run still catches recent posts;
        // the DB dedup prevents any double push.
        private const val WINDOW_MS = 48L * 60L * 60L * 1000L
    }
}
