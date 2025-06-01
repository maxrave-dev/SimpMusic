package com.maxrave.lyricsproviders.models.response

import com.maxrave.lyricsproviders.models.response.MacroSearchResponse.Message.Body.MacroCalls.CrowdTrackActionsGet.Message.Body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class MacroSearchResponse(
    @SerialName("message")
    val message: Message? = null
) {
    @Serializable
    data class Message(
        @SerialName("body")
        val body: Body? = null,
        @SerialName("header")
        val header: Header? = null
    ) {
        @Serializable
        data class Body(
            @SerialName("macro_calls")
            val macroCalls: MacroCalls? = null
        ) {
            @Serializable
            data class MacroCalls(
                @SerialName("crowd.track.actions.get")
                val crowdTrackActionsGet: CrowdTrackActionsGet? = null,
                @SerialName("matcher.track.get")
                val matcherTrackGet: MatcherTrackGet? = null,
                @SerialName("track.lyrics.get")
                val trackLyricsGet: TrackLyricsGet? = null,
                @SerialName("track.richsync.get")
                val trackRichsyncGet: TrackRichsyncGet? = null,
                @SerialName("track.snippet.get")
                val trackSnippetGet: TrackSnippetGet? = null,
                @SerialName("track.subtitles.get")
                val trackSubtitlesGet: TrackSubtitlesGet? = null,
                @SerialName("userblob.get")
                val userblobGet: UserblobGet? = null
            ) {
                @Serializable
                data class CrowdTrackActionsGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = ListableCrowdTrackActionsGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("requested_actions_list")
                            val requestedActionsList: List<RequestedActions?>? = null
                        ) {
                            @Serializable
                            data class RequestedActions(
                                @SerialName("action")
                                val action: Action? = null
                            ) {
                                @Serializable
                                data class Action(
                                    @SerialName("action_type")
                                    val actionType: String? = null,
                                    @SerialName("action_type_id")
                                    val actionTypeId: String? = null,
                                    @SerialName("question_id")
                                    val questionId: String? = null,
                                    @SerialName("selected_language")
                                    val selectedLanguage: String? = null,
                                    @SerialName("single_answer")
                                    val singleAnswer: Boolean? = null,
                                    @SerialName("skip_intro")
                                    val skipIntro: Boolean? = null,
                                    @SerialName("text")
                                    val text: String? = null,
                                    @SerialName("type_id_list")
                                    val typeIdList: TypeIdList? = null
                                ) {
                                    @Serializable
                                    data class TypeIdList(
                                        @SerialName("lyrics_ai_mood_analysis_v3_value")
                                        val lyricsAiMoodAnalysisV3Value: List<String?>? = null
                                    )
                                }
                            }
                        }

                        @Serializable
                        data class Header(
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class MatcherTrackGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = MatcherTrackGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("track")
                            val track: Track? = null
                        ) {
                            @Serializable
                            data class Track(
                                @SerialName("album_coverart_100x100")
                                val albumCoverart100x100: String? = null,
                                @SerialName("album_coverart_350x350")
                                val albumCoverart350x350: String? = null,
                                @SerialName("album_coverart_500x500")
                                val albumCoverart500x500: String? = null,
                                @SerialName("album_coverart_800x800")
                                val albumCoverart800x800: String? = null,
                                @SerialName("album_id")
                                val albumId: Int? = null,
                                @SerialName("album_name")
                                val albumName: String? = null,
                                @SerialName("album_vanity_id")
                                val albumVanityId: String? = null,
                                @SerialName("artist_id")
                                val artistId: Int? = null,
                                @SerialName("artist_mbid")
                                val artistMbid: String? = null,
                                @SerialName("artist_name")
                                val artistName: String? = null,
                                @SerialName("commontrack_id")
                                val commontrackId: Int? = null,
                                @SerialName("commontrack_isrcs")
                                val commontrackIsrcs: List<List<String?>?>? = null,
                                @SerialName("commontrack_itunes_ids")
                                val commontrackItunesIds: List<Int?>? = null,
                                @SerialName("commontrack_spotify_ids")
                                val commontrackSpotifyIds: List<String?>? = null,
                                @SerialName("commontrack_vanity_id")
                                val commontrackVanityId: String? = null,
                                @SerialName("explicit")
                                val explicit: Int? = null,
                                @SerialName("first_release_date")
                                val firstReleaseDate: String? = null,
                                @SerialName("has_lyrics")
                                val hasLyrics: Int? = null,
                                @SerialName("has_lyrics_crowd")
                                val hasLyricsCrowd: Int? = null,
                                @SerialName("has_richsync")
                                val hasRichsync: Int? = null,
                                @SerialName("has_subtitles")
                                val hasSubtitles: Int? = null,
                                @SerialName("has_track_structure")
                                val hasTrackStructure: Int? = null,
                                @SerialName("instrumental")
                                val instrumental: Int? = null,
                                @SerialName("lyrics_id")
                                val lyricsId: Int? = null,
                                @SerialName("num_favourite")
                                val numFavourite: Int? = null,
                                @SerialName("performer_tagging_misc_tags")
                                val performerTaggingMiscTags: PerformerTaggingMiscTags? = null,
                                @SerialName("primary_genres")
                                val primaryGenres: PrimaryGenres? = null,
                                @SerialName("restricted")
                                val restricted: Int? = null,
                                @SerialName("subtitle_id")
                                val subtitleId: Int? = null,
                                @SerialName("track_edit_url")
                                val trackEditUrl: String? = null,
                                @SerialName("track_id")
                                val trackId: Int? = null,
                                @SerialName("track_isrc")
                                val trackIsrc: String? = null,
                                @SerialName("track_length")
                                val trackLength: Int? = null,
                                @SerialName("track_lyrics_translation_status")
                                val trackLyricsTranslationStatus: List<TrackLyricsTranslationStatu?>? = null,
                                @SerialName("track_mbid")
                                val trackMbid: String? = null,
                                @SerialName("track_name")
                                val trackName: String? = null,
                                @SerialName("track_rating")
                                val trackRating: Int? = null,
                                @SerialName("track_share_url")
                                val trackShareUrl: String? = null,
                                @SerialName("track_soundcloud_id")
                                val trackSoundcloudId: Int? = null,
                                @SerialName("track_spotify_id")
                                val trackSpotifyId: String? = null,
                                @SerialName("track_xboxmusic_id")
                                val trackXboxmusicId: String? = null,
                                @SerialName("updated_time")
                                val updatedTime: String? = null
                            ) {
                                @Serializable
                                data class PerformerTaggingMiscTags(
                                    @SerialName("backing_vocalist")
                                    val backingVocalist: String? = null,
                                    @SerialName("fan_chant")
                                    val fanChant: String? = null,
                                    @SerialName("robotic_vocal")
                                    val roboticVocal: String? = null,
                                    @SerialName("unknown")
                                    val unknown: String? = null,
                                    @SerialName("voice_over")
                                    val voiceOver: String? = null
                                )

                                @Serializable
                                data class PrimaryGenres(
                                    @SerialName("music_genre_list")
                                    val musicGenreList: List<MusicGenre?>? = null
                                ) {
                                    @Serializable
                                    data class MusicGenre(
                                        @SerialName("music_genre")
                                        val musicGenre: MusicGenre? = null
                                    ) {
                                        @Serializable
                                        data class MusicGenre(
                                            @SerialName("music_genre_id")
                                            val musicGenreId: Int? = null,
                                            @SerialName("music_genre_name")
                                            val musicGenreName: String? = null,
                                            @SerialName("music_genre_name_extended")
                                            val musicGenreNameExtended: String? = null,
                                            @SerialName("music_genre_parent_id")
                                            val musicGenreParentId: Int? = null,
                                            @SerialName("music_genre_vanity")
                                            val musicGenreVanity: String? = null
                                        )
                                    }
                                }

                                @Serializable
                                data class TrackLyricsTranslationStatu(
                                    @SerialName("from")
                                    val from: String? = null,
                                    @SerialName("perc")
                                    val perc: Double? = null,
                                    @SerialName("to")
                                    val to: String? = null
                                )
                            }
                        }

                        @Serializable
                        data class Header(
                            @SerialName("cached")
                            val cached: Int? = null,
                            @SerialName("confidence")
                            val confidence: Int? = null,
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("mode")
                            val mode: String? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class TrackLyricsGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = TrackLyricsGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("lyrics")
                            val lyrics: Lyrics? = null
                        ) {
                            @Serializable
                            data class Lyrics(
                                @SerialName("action_requested")
                                val actionRequested: String? = null,
                                @SerialName("backlink_url")
                                val backlinkUrl: String? = null,
                                @SerialName("can_edit")
                                val canEdit: Int? = null,
                                @SerialName("check_validation_overridable")
                                val checkValidationOverridable: Int? = null,
                                @SerialName("explicit")
                                val explicit: Int? = null,
                                @SerialName("html_tracking_url")
                                val htmlTrackingUrl: String? = null,
                                @SerialName("instrumental")
                                val instrumental: Int? = null,
                                @SerialName("locked")
                                val locked: Int? = null,
                                @SerialName("lyrics_body")
                                val lyricsBody: String? = null,
                                @SerialName("lyrics_copyright")
                                val lyricsCopyright: String? = null,
                                @SerialName("lyrics_id")
                                val lyricsId: Int? = null,
                                @SerialName("lyrics_language")
                                val lyricsLanguage: String? = null,
                                @SerialName("lyrics_language_description")
                                val lyricsLanguageDescription: String? = null,
                                @SerialName("lyrics_user")
                                val lyricsUser: LyricsUser? = null,
                                @SerialName("pixel_tracking_url")
                                val pixelTrackingUrl: String? = null,
                                @SerialName("published_status")
                                val publishedStatus: Int? = null,
                                @SerialName("restricted")
                                val restricted: Int? = null,
                                @SerialName("script_tracking_url")
                                val scriptTrackingUrl: String? = null,
                                @SerialName("updated_time")
                                val updatedTime: String? = null,
                                @SerialName("verified")
                                val verified: Int? = null,
                            ) {
                                @Serializable
                                data class LyricsUser(
                                    @SerialName("user")
                                    val user: User? = null
                                ) {
                                    @Serializable
                                    data class User(
                                        @SerialName("academy_completed")
                                        val academyCompleted: Boolean? = null,
                                        @SerialName("academy_completed_date")
                                        val academyCompletedDate: Boolean? = null,
                                        @SerialName("admin")
                                        val admin: Boolean? = null,
                                        @SerialName("artist_manager")
                                        val artistManager: Int? = null,
                                        @SerialName("counters")
                                        val counters: Counters? = null,
                                        @SerialName("has_distributor_profile_page")
                                        val hasDistributorProfilePage: Int? = null,
                                        @SerialName("has_informative_profile_page")
                                        val hasInformativeProfilePage: Int? = null,
                                        @SerialName("has_private_profile")
                                        val hasPrivateProfile: Int? = null,
                                        @SerialName("is_mine")
                                        val isMine: Int? = null,
                                        @SerialName("key")
                                        val key: String? = null,
                                        @SerialName("labels")
                                        val labels: List<String?>? = null,
                                        @SerialName("level")
                                        val level: String? = null,
                                        @SerialName("moderator")
                                        val moderator: Boolean? = null,
                                        @SerialName("next_rank_name")
                                        val nextRankName: String? = null,
                                        @SerialName("points_to_next_level")
                                        val pointsToNextLevel: Int? = null,
                                        @SerialName("position")
                                        val position: Int? = null,
                                        @SerialName("rank_color")
                                        val rankColor: String? = null,
                                        @SerialName("rank_colors")
                                        val rankColors: RankColors? = null,
                                        @SerialName("rank_image_url")
                                        val rankImageUrl: String? = null,
                                        @SerialName("rank_level")
                                        val rankLevel: Int? = null,
                                        @SerialName("rank_name")
                                        val rankName: String? = null,
                                        @SerialName("ratio_to_next_level")
                                        val ratioToNextLevel: Int? = null,
                                        @SerialName("ratio_to_next_rank")
                                        val ratioToNextRank: Int? = null,
                                        @SerialName("score")
                                        val score: Int? = null,
                                        @SerialName("uaid")
                                        val uaid: String? = null,
                                        @SerialName("user_name")
                                        val userName: String? = null,
                                        @SerialName("user_profile_photo")
                                        val userProfilePhoto: String? = null,
                                        @SerialName("weekly_score")
                                        val weeklyScore: Int? = null
                                    ) {
                                        @Serializable
                                        data class Counters(
                                            @SerialName("admin_add_description")
                                            val adminAddDescription: Int? = null,
                                            @SerialName("admin_add_score")
                                            val adminAddScore: Int? = null,
                                            @SerialName("admin_modify_setup_profile")
                                            val adminModifySetupProfile: Int? = null,
                                            @SerialName("lyrics_ai_mood_analysis_v3_value")
                                            val lyricsAiMoodAnalysisV3Value: Int? = null,
                                            @SerialName("lyrics_changed")
                                            val lyricsChanged: Int? = null,
                                            @SerialName("lyrics_missing")
                                            val lyricsMissing: Int? = null,
                                            @SerialName("lyrics_ok")
                                            val lyricsOk: Int? = null,
                                            @SerialName("lyrics_ranking_change")
                                            val lyricsRankingChange: Int? = null,
                                            @SerialName("lyrics_report_freelance_noaudio")
                                            val lyricsReportFreelanceNoaudio: Int? = null,
                                            @SerialName("lyrics_richsync_added")
                                            val lyricsRichsyncAdded: Int? = null,
                                            @SerialName("lyrics_subtitle_added")
                                            val lyricsSubtitleAdded: Int? = null,
                                            @SerialName("save_academy_test_passed")
                                            val saveAcademyTestPassed: Int? = null,
                                            @SerialName("track_complete_metadata")
                                            val trackCompleteMetadata: Int? = null,
                                            @SerialName("track_influencer_bonus_moderator_vote")
                                            val trackInfluencerBonusModeratorVote: Int? = null,
                                            @SerialName("track_performer_tagging")
                                            val trackPerformerTagging: Int? = null,
                                            @SerialName("track_report_saving_error")
                                            val trackReportSavingError: Int? = null,
                                            @SerialName("track_report_wrong_metadata")
                                            val trackReportWrongMetadata: Int? = null,
                                            @SerialName("track_structure")
                                            val trackStructure: Int? = null,
                                            @SerialName("track_translation")
                                            val trackTranslation: Int? = null,
                                            @SerialName("vote_bonuses")
                                            val voteBonuses: Int? = null,
                                            @SerialName("vote_maluses")
                                            val voteMaluses: Int? = null
                                        )

                                        @Serializable
                                        data class RankColors(
                                            @SerialName("rank_color_10")
                                            val rankColor10: String? = null,
                                            @SerialName("rank_color_100")
                                            val rankColor100: String? = null,
                                            @SerialName("rank_color_200")
                                            val rankColor200: String? = null,
                                            @SerialName("rank_color_50")
                                            val rankColor50: String? = null
                                        )
                                    }
                                }
                            }
                        }

                        @Serializable
                        data class Header(
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class TrackRichsyncGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = TrackRichsyncGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("richsync")
                            val richsync: Richsync? = null
                        ) {
                            @Serializable
                            data class Richsync(
                                @SerialName("html_tracking_url")
                                val htmlTrackingUrl: String? = null,
                                @SerialName("lyrics_copyright")
                                val lyricsCopyright: String? = null,
                                @SerialName("pixel_tracking_url")
                                val pixelTrackingUrl: String? = null,
                                @SerialName("restricted")
                                val restricted: Int? = null,
                                @SerialName("richssync_language")
                                val richssyncLanguage: String? = null,
                                @SerialName("richsync_avg_count")
                                val richsyncAvgCount: Int? = null,
                                @SerialName("richsync_body")
                                val richsyncBody: String? = null,
                                @SerialName("richsync_id")
                                val richsyncId: Int? = null,
                                @SerialName("richsync_language_description")
                                val richsyncLanguageDescription: String? = null,
                                @SerialName("richsync_length")
                                val richsyncLength: Int? = null,
                                @SerialName("richsync_user")
                                val richsyncUser: RichsyncUser? = null,
                                @SerialName("script_tracking_url")
                                val scriptTrackingUrl: String? = null,
                                @SerialName("updated_time")
                                val updatedTime: String? = null,
                            ) {
                                @Serializable
                                data class RichsyncUser(
                                    @SerialName("user")
                                    val user: User? = null
                                ) {
                                    @Serializable
                                    data class User(
                                        @SerialName("academy_completed")
                                        val academyCompleted: Boolean? = null,
                                        @SerialName("academy_completed_date")
                                        val academyCompletedDate: String? = null,
                                        @SerialName("artist_manager")
                                        val artistManager: Int? = null,
                                        @SerialName("counters")
                                        val counters: Counters? = null,
                                        @SerialName("has_distributor_profile_page")
                                        val hasDistributorProfilePage: Int? = null,
                                        @SerialName("has_informative_profile_page")
                                        val hasInformativeProfilePage: Int? = null,
                                        @SerialName("has_private_profile")
                                        val hasPrivateProfile: Int? = null,
                                        @SerialName("is_mine")
                                        val isMine: Int? = null,
                                        @SerialName("key")
                                        val key: String? = null,
                                        @SerialName("level")
                                        val level: String? = null,
                                        @SerialName("moderator_eligible")
                                        val moderatorEligible: Boolean? = null,
                                        @SerialName("next_rank_color")
                                        val nextRankColor: String? = null,
                                        @SerialName("next_rank_colors")
                                        val nextRankColors: NextRankColors? = null,
                                        @SerialName("next_rank_image_url")
                                        val nextRankImageUrl: String? = null,
                                        @SerialName("next_rank_name")
                                        val nextRankName: String? = null,
                                        @SerialName("points_to_next_level")
                                        val pointsToNextLevel: Int? = null,
                                        @SerialName("position")
                                        val position: Int? = null,
                                        @SerialName("rank_color")
                                        val rankColor: String? = null,
                                        @SerialName("rank_colors")
                                        val rankColors: RankColors? = null,
                                        @SerialName("rank_image_url")
                                        val rankImageUrl: String? = null,
                                        @SerialName("rank_level")
                                        val rankLevel: Int? = null,
                                        @SerialName("rank_name")
                                        val rankName: String? = null,
                                        @SerialName("ratio_to_next_level")
                                        val ratioToNextLevel: Int? = null,
                                        @SerialName("ratio_to_next_rank")
                                        val ratioToNextRank: Int? = null,
                                        @SerialName("score")
                                        val score: Int? = null,
                                        @SerialName("uaid")
                                        val uaid: String? = null,
                                        @SerialName("user_name")
                                        val userName: String? = null,
                                        @SerialName("user_profile_photo")
                                        val userProfilePhoto: String? = null,
                                        @SerialName("weekly_score")
                                        val weeklyScore: Int? = null
                                    ) {
                                        @Serializable
                                        data class Counters(
                                            @SerialName("evaluation_academy_test")
                                            val evaluationAcademyTest: Int? = null,
                                            @SerialName("evaluation_curator_test")
                                            val evaluationCuratorTest: Int? = null,
                                            @SerialName("lyrics_ai_mood_analysis_v3_value")
                                            val lyricsAiMoodAnalysisV3Value: Int? = null,
                                            @SerialName("lyrics_ai_ugc_language")
                                            val lyricsAiUgcLanguage: Int? = null,
                                            @SerialName("lyrics_changed")
                                            val lyricsChanged: Int? = null,
                                            @SerialName("lyrics_favourite_added")
                                            val lyricsFavouriteAdded: Int? = null,
                                            @SerialName("lyrics_missing")
                                            val lyricsMissing: Int? = null,
                                            @SerialName("lyrics_music_id")
                                            val lyricsMusicId: Int? = null,
                                            @SerialName("lyrics_ok")
                                            val lyricsOk: Int? = null,
                                            @SerialName("lyrics_ranking_change")
                                            val lyricsRankingChange: Int? = null,
                                            @SerialName("lyrics_report_contain_mistakes")
                                            val lyricsReportContainMistakes: Int? = null,
                                            @SerialName("lyrics_report_incomplete_lyrics")
                                            val lyricsReportIncompleteLyrics: Int? = null,
                                            @SerialName("lyrics_richsync_added")
                                            val lyricsRichsyncAdded: Int? = null,
                                            @SerialName("lyrics_subtitle_added")
                                            val lyricsSubtitleAdded: Int? = null,
                                            @SerialName("save_academy_test_passed")
                                            val saveAcademyTestPassed: Int? = null,
                                            @SerialName("subtitle_report_wrong_sync")
                                            val subtitleReportWrongSync: Int? = null,
                                            @SerialName("track_complete_metadata")
                                            val trackCompleteMetadata: Int? = null,
                                            @SerialName("track_report_wrong_metadata")
                                            val trackReportWrongMetadata: Int? = null,
                                            @SerialName("track_structure")
                                            val trackStructure: Int? = null,
                                            @SerialName("track_translation")
                                            val trackTranslation: Int? = null,
                                            @SerialName("translation_ok")
                                            val translationOk: Int? = null,
                                            @SerialName("vote_bonuses")
                                            val voteBonuses: Int? = null,
                                            @SerialName("vote_maluses")
                                            val voteMaluses: Int? = null
                                        )

                                        @Serializable
                                        data class NextRankColors(
                                            @SerialName("rank_color_10")
                                            val rankColor10: String? = null,
                                            @SerialName("rank_color_100")
                                            val rankColor100: String? = null,
                                            @SerialName("rank_color_200")
                                            val rankColor200: String? = null,
                                            @SerialName("rank_color_50")
                                            val rankColor50: String? = null
                                        )

                                        @Serializable
                                        data class RankColors(
                                            @SerialName("rank_color_10")
                                            val rankColor10: String? = null,
                                            @SerialName("rank_color_100")
                                            val rankColor100: String? = null,
                                            @SerialName("rank_color_200")
                                            val rankColor200: String? = null,
                                            @SerialName("rank_color_50")
                                            val rankColor50: String? = null
                                        )
                                    }
                                }
                            }
                        }

                        @Serializable
                        data class Header(
                            @SerialName("available")
                            val available: Int? = null,
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class TrackSnippetGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = TrackSnippetGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("snippet")
                            val snippet: Snippet? = null
                        ) {
                            @Serializable
                            data class Snippet(
                                @SerialName("html_tracking_url")
                                val htmlTrackingUrl: String? = null,
                                @SerialName("instrumental")
                                val instrumental: Int? = null,
                                @SerialName("pixel_tracking_url")
                                val pixelTrackingUrl: String? = null,
                                @SerialName("restricted")
                                val restricted: Int? = null,
                                @SerialName("script_tracking_url")
                                val scriptTrackingUrl: String? = null,
                                @SerialName("snippet_body")
                                val snippetBody: String? = null,
                                @SerialName("snippet_id")
                                val snippetId: Int? = null,
                                @SerialName("snippet_language")
                                val snippetLanguage: String? = null,
                                @SerialName("updated_time")
                                val updatedTime: String? = null
                            )
                        }

                        @Serializable
                        data class Header(
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class TrackSubtitlesGet(
                    @SerialName("message")
                    val message: Message? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("body")
                        @Serializable(with = TrackSubtitlesGetDataFieldJsonSerializer::class)
                        val body: Body? = null,
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Body(
                            @SerialName("subtitle_list")
                            val subtitleList: List<Subtitle?>? = null
                        ) {
                            @Serializable
                            data class Subtitle(
                                @SerialName("subtitle")
                                val subtitle: Subtitle? = null
                            ) {
                                @Serializable
                                data class Subtitle(
                                    @SerialName("html_tracking_url")
                                    val htmlTrackingUrl: String? = null,
                                    @SerialName("lyrics_copyright")
                                    val lyricsCopyright: String? = null,
                                    @SerialName("metadata")
                                    val metadata: Metadata? = null,
                                    @SerialName("pixel_tracking_url")
                                    val pixelTrackingUrl: String? = null,
                                    @SerialName("published_status")
                                    val publishedStatus: Int? = null,
                                    @SerialName("restricted")
                                    val restricted: Int? = null,
                                    @SerialName("script_tracking_url")
                                    val scriptTrackingUrl: String? = null,
                                    @SerialName("subtitle_avg_count")
                                    val subtitleAvgCount: Int? = null,
                                    @SerialName("subtitle_body")
                                    val subtitleBody: String? = null,
                                    @SerialName("subtitle_id")
                                    val subtitleId: Int? = null,
                                    @SerialName("subtitle_language")
                                    val subtitleLanguage: String? = null,
                                    @SerialName("subtitle_language_description")
                                    val subtitleLanguageDescription: String? = null,
                                    @SerialName("subtitle_length")
                                    val subtitleLength: Int? = null,
                                    @SerialName("subtitle_user")
                                    val subtitleUser: SubtitleUser? = null,
                                    @SerialName("updated_time")
                                    val updatedTime: String? = null,
                                ) {
                                    @Serializable
                                    data class Metadata(
                                        @SerialName("track_structure")
                                        val trackStructure: List<TrackStructure?>? = null
                                    ) {
                                        @Serializable
                                        data class TrackStructure(
                                            @SerialName("description")
                                            val description: String? = null,
                                            @SerialName("position")
                                            val position: Int? = null,
                                            @SerialName("snippet")
                                            val snippet: String? = null
                                        )
                                    }

                                    @Serializable
                                    data class SubtitleUser(
                                        @SerialName("user")
                                        val user: User? = null
                                    ) {
                                        @Serializable
                                        data class User(
                                            @SerialName("academy_completed")
                                            val academyCompleted: Boolean? = null,
                                            @SerialName("academy_completed_date")
                                            val academyCompletedDate: Boolean? = null,
                                            @SerialName("admin")
                                            val admin: Boolean? = null,
                                            @SerialName("artist_manager")
                                            val artistManager: Int? = null,
                                            @SerialName("counters")
                                            val counters: Counters? = null,
                                            @SerialName("has_distributor_profile_page")
                                            val hasDistributorProfilePage: Int? = null,
                                            @SerialName("has_informative_profile_page")
                                            val hasInformativeProfilePage: Int? = null,
                                            @SerialName("has_private_profile")
                                            val hasPrivateProfile: Int? = null,
                                            @SerialName("is_mine")
                                            val isMine: Int? = null,
                                            @SerialName("key")
                                            val key: String? = null,
                                            @SerialName("labels")
                                            val labels: List<String?>? = null,
                                            @SerialName("level")
                                            val level: String? = null,
                                            @SerialName("moderator")
                                            val moderator: Boolean? = null,
                                            @SerialName("next_rank_name")
                                            val nextRankName: String? = null,
                                            @SerialName("points_to_next_level")
                                            val pointsToNextLevel: Int? = null,
                                            @SerialName("position")
                                            val position: Int? = null,
                                            @SerialName("rank_color")
                                            val rankColor: String? = null,
                                            @SerialName("rank_colors")
                                            val rankColors: RankColors? = null,
                                            @SerialName("rank_image_url")
                                            val rankImageUrl: String? = null,
                                            @SerialName("rank_level")
                                            val rankLevel: Int? = null,
                                            @SerialName("rank_name")
                                            val rankName: String? = null,
                                            @SerialName("ratio_to_next_level")
                                            val ratioToNextLevel: Int? = null,
                                            @SerialName("ratio_to_next_rank")
                                            val ratioToNextRank: Int? = null,
                                            @SerialName("score")
                                            val score: Int? = null,
                                            @SerialName("uaid")
                                            val uaid: String? = null,
                                            @SerialName("user_name")
                                            val userName: String? = null,
                                            @SerialName("user_profile_photo")
                                            val userProfilePhoto: String? = null,
                                            @SerialName("weekly_score")
                                            val weeklyScore: Int? = null
                                        ) {
                                            @Serializable
                                            data class Counters(
                                                @SerialName("admin_add_description")
                                                val adminAddDescription: Int? = null,
                                                @SerialName("admin_add_score")
                                                val adminAddScore: Int? = null,
                                                @SerialName("admin_modify_setup_profile")
                                                val adminModifySetupProfile: Int? = null,
                                                @SerialName("lyrics_ai_mood_analysis_v3_value")
                                                val lyricsAiMoodAnalysisV3Value: Int? = null,
                                                @SerialName("lyrics_changed")
                                                val lyricsChanged: Int? = null,
                                                @SerialName("lyrics_missing")
                                                val lyricsMissing: Int? = null,
                                                @SerialName("lyrics_ok")
                                                val lyricsOk: Int? = null,
                                                @SerialName("lyrics_ranking_change")
                                                val lyricsRankingChange: Int? = null,
                                                @SerialName("lyrics_report_freelance_noaudio")
                                                val lyricsReportFreelanceNoaudio: Int? = null,
                                                @SerialName("lyrics_richsync_added")
                                                val lyricsRichsyncAdded: Int? = null,
                                                @SerialName("lyrics_subtitle_added")
                                                val lyricsSubtitleAdded: Int? = null,
                                                @SerialName("save_academy_test_passed")
                                                val saveAcademyTestPassed: Int? = null,
                                                @SerialName("track_complete_metadata")
                                                val trackCompleteMetadata: Int? = null,
                                                @SerialName("track_influencer_bonus_moderator_vote")
                                                val trackInfluencerBonusModeratorVote: Int? = null,
                                                @SerialName("track_performer_tagging")
                                                val trackPerformerTagging: Int? = null,
                                                @SerialName("track_report_saving_error")
                                                val trackReportSavingError: Int? = null,
                                                @SerialName("track_report_wrong_metadata")
                                                val trackReportWrongMetadata: Int? = null,
                                                @SerialName("track_structure")
                                                val trackStructure: Int? = null,
                                                @SerialName("track_translation")
                                                val trackTranslation: Int? = null,
                                                @SerialName("vote_bonuses")
                                                val voteBonuses: Int? = null,
                                                @SerialName("vote_maluses")
                                                val voteMaluses: Int? = null
                                            )

                                            @Serializable
                                            data class RankColors(
                                                @SerialName("rank_color_10")
                                                val rankColor10: String? = null,
                                                @SerialName("rank_color_100")
                                                val rankColor100: String? = null,
                                                @SerialName("rank_color_200")
                                                val rankColor200: String? = null,
                                                @SerialName("rank_color_50")
                                                val rankColor50: String? = null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        @Serializable
                        data class Header(
                            @SerialName("available")
                            val available: Int? = null,
                            @SerialName("execute_time")
                            val executeTime: Double? = null,
                            @SerialName("instrumental")
                            val instrumental: Int? = null,
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }
                }

                @Serializable
                data class UserblobGet(
                    @SerialName("message")
                    val message: Message? = null,
                    @SerialName("meta")
                    val meta: Meta? = null
                ) {
                    @Serializable
                    data class Message(
                        @SerialName("header")
                        val header: Header? = null
                    ) {
                        @Serializable
                        data class Header(
                            @SerialName("status_code")
                            val statusCode: Int? = null
                        )
                    }

                    @Serializable
                    data class Meta(
                        @SerialName("last_updated")
                        val lastUpdated: String? = null,
                        @SerialName("status_code")
                        val statusCode: Int? = null
                    )
                }
            }
        }

        @Serializable
        data class Header(
            @SerialName("execute_time")
            val executeTime: Double? = null,
            @SerialName("pid")
            val pid: Int? = null,
            @SerialName("status_code")
            val statusCode: Int? = null
        )
    }
    companion object {
        object ListableCrowdTrackActionsGetDataFieldJsonSerializer :
            JsonTransformingSerializer<Body?>(Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
        object MatcherTrackGetDataFieldJsonSerializer :
            JsonTransformingSerializer<Message.Body.MacroCalls.MatcherTrackGet.Message.Body?>(
                Message.Body.MacroCalls.MatcherTrackGet.Message.Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
        object TrackLyricsGetDataFieldJsonSerializer :
            JsonTransformingSerializer<Message.Body.MacroCalls.TrackLyricsGet.Message.Body?>(Message.Body.MacroCalls.TrackLyricsGet.Message.Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
        object TrackRichsyncGetDataFieldJsonSerializer :
            JsonTransformingSerializer<Message.Body.MacroCalls.TrackRichsyncGet.Message.Body?>(
                Message.Body.MacroCalls.TrackRichsyncGet.Message.Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
        object TrackSnippetGetDataFieldJsonSerializer :
            JsonTransformingSerializer<Message.Body.MacroCalls.TrackSnippetGet.Message.Body?>(
                Message.Body.MacroCalls.TrackSnippetGet.Message.Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
        object TrackSubtitlesGetDataFieldJsonSerializer: JsonTransformingSerializer<Message.Body.MacroCalls.TrackSubtitlesGet.Message.Body?>(
            Message.Body.MacroCalls.TrackSubtitlesGet.Message.Body.serializer().nullable) {
            override fun transformDeserialize(element: JsonElement): JsonElement {
                return if (element is JsonArray) {
                    JsonNull
                } else {
                    element
                }
            }
        }
    }
}