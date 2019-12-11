package com.nononsenseapps.feeder.db.room

import androidx.room.*
import com.nononsenseapps.feeder.model.FeedUnreadCount
import kotlinx.coroutines.flow.Flow
import java.net.URL

@Dao
interface FeedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed): Long

    @Update
    suspend fun updateFeed(feed: Feed)

    @Delete
    suspend fun deleteFeed(feed: Feed)

    @Query("DELETE FROM feeds WHERE id IS :feedId")
    suspend fun deleteFeedWithId(feedId: Long)

    @Query("SELECT * FROM feeds WHERE id IS :feedId")
    fun loadLiveFeed(feedId: Long): Flow<Feed>

    @Query("SELECT DISTINCT tag FROM feeds ORDER BY tag COLLATE NOCASE")
    suspend fun loadTags(): List<String>

    @Query("SELECT * FROM feeds WHERE id IS :feedId")
    suspend fun loadFeed(feedId: Long): Feed?

    @Query("""
       SELECT * FROM feeds
       WHERE id is :feedId
       AND last_sync < :staleTime
    """)
    suspend fun loadFeedIfStale(feedId: Long, staleTime: Long): Feed?

    @Query("SELECT * FROM feeds WHERE tag IS :tag")
    suspend fun loadFeeds(tag: String): List<Feed>

    @Query("SELECT * FROM feeds WHERE tag IS :tag AND last_sync < :staleTime")
    suspend fun loadFeedsIfStale(tag: String, staleTime: Long): List<Feed>

    @Query("SELECT notify FROM feeds WHERE tag IS :tag")
    fun loadLiveFeedsNotify(tag: String): Flow<List<Boolean>>

    @Query("SELECT notify FROM feeds WHERE id IS :feedId")
    fun loadLiveFeedsNotify(feedId: Long): Flow<List<Boolean>>

    @Query("SELECT notify FROM feeds")
    fun loadLiveFeedsNotify(): Flow<List<Boolean>>

    @Query("SELECT * FROM feeds")
    suspend fun loadFeeds(): List<Feed>

    @Query("SELECT * FROM feeds WHERE last_sync < :staleTime")
    suspend fun loadFeedsIfStale(staleTime: Long): List<Feed>

    @Query("SELECT * FROM feeds WHERE url IS :url")
    suspend fun loadFeedWithUrl(url: URL): Feed?

    @Query("SELECT id FROM feeds WHERE notify IS 1")
    suspend fun loadFeedIdsToNotify(): List<Long>

    @Query("""
        SELECT id, title, url, tag, custom_title, notify, image_url, unread_count
        FROM feeds
        LEFT JOIN (SELECT COUNT(1) AS unread_count, feed_id
          FROM feed_items
          WHERE unread IS 1
          GROUP BY feed_id
        )
        ON feeds.id = feed_id
    """)
    fun loadLiveFeedsWithUnreadCounts(): Flow<List<FeedUnreadCount>>

    @Query("UPDATE feeds SET notify = :notify WHERE id IS :id")
    suspend fun setNotify(id: Long, notify: Boolean)

    @Query("UPDATE feeds SET notify = :notify WHERE tag IS :tag")
    suspend fun setNotify(tag: String, notify: Boolean)

    @Query("UPDATE feeds SET notify = :notify")
    suspend fun setAllNotify(notify: Boolean)
}

/**
 * Inserts or updates feed depending on if ID is valid. Returns ID.
 */
suspend fun FeedDao.upsertFeed(feed: Feed): Long = when (feed.id > ID_UNSET) {
    true -> {
        updateFeed(feed)
        feed.id
    }
    false -> {
        insertFeed(feed)
    }
}
