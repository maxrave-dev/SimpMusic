package com.maxrave.simpmusic.data.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.maxrave.simpmusic.data.type.PlaylistType
import com.maxrave.simpmusic.data.type.RecentlyType
import java.time.LocalDateTime

@Entity(tableName = "podcast_table")
data class PodcastsEntity(
    @PrimaryKey
    val podcastId: String, // Generating a unique identifier for podcast
    val title: String,
    val authorId: String,
    val authorName: String,
    val authorThumbnail: String?,
    val description: String?,
    val thumbnail: String?,
    val isFavorite: Boolean = false,
    val inLibrary: LocalDateTime = LocalDateTime.now(),
    val favoriteTime: LocalDateTime? = null, // Nullable to allow for non-favorite podcasts
    val listEpisodes: List<String>, // List of episode video IDs
) : RecentlyType,
    PlaylistType {
    override fun objectType(): RecentlyType.Type = RecentlyType.Type.PLAYLIST

    override fun playlistType(): PlaylistType.Type = PlaylistType.Type.PODCAST
}

@Entity(
    tableName = "podcast_episode_table",
    foreignKeys = [
        ForeignKey(
            entity = PodcastsEntity::class,
            parentColumns = ["podcastId"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("podcastId")], // Add this line to create an index
)
data class EpisodeEntity(
    @PrimaryKey
    val videoId: String,
    val podcastId: String, // Foreign key to link to parent podcast
    val title: String,
    val authorName: String,
    val authorId: String,
    val description: String?,
    val createdDay: String?,
    val durationString: String?,
    val thumbnail: String? = null,
)

class PodcastWithEpisodes(
    @Embedded val podcast: PodcastsEntity,
    @Relation(
        parentColumn = "podcastId",
        entityColumn = "podcastId",
    )
    val episodes: List<EpisodeEntity>,
)