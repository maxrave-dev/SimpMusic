package com.maxrave.simpmusic

import platform.AVFoundation.*
import platform.MediaPlayer.*

import com.maxrave.data.mediaservice.SharedMediaPlayerHandler
import com.maxrave.domain.mediaservice.player.MediaPlayerListener
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.data.io.fileDir
import com.maxrave.data.player.AvPlayerAdapter
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.data.player.PlayerConstants
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin

/** Invoked only by the Debug host's explicit --playback-smoke-test launch argument. */
fun startIosPlaybackSmokeTest(url: String) {
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
        val report = StringBuilder()
        val player = getKoin().get<AvPlayerAdapter>()
        var selected: GenericMediaItem? = null
        val listener = object : MediaPlayerListener {
            override fun onMediaItemTransition(mediaItem: GenericMediaItem?, reason: Int) { selected = mediaItem }
        }
        player.addListener(listener)
        suspend fun await(description: String, condition: () -> Boolean) {
            withTimeout(30_000) { while (!condition()) delay(100) }
            report.appendLine("PASS $description")
        }
        try {
            withTimeout(30_000) {
                (getKoin().get<MediaPlayerHandler>() as SharedMediaPlayerHandler).awaitStartup()
            }
            val first = GenericMediaItem("ios-smoke-1", url, GenericMediaMetadata(title = "Playback verification"))
            val second = first.copy(mediaId = "ios-smoke-2")
            player.clearMediaItems()
            player.addMediaItem(first)
            check(selected == first)
            report.appendLine("PASS first queue item publishes metadata")
            player.addMediaItem(first)
            player.addMediaItem(second)
            player.seekTo(1, 0)
            player.play()
            await("AVPlayer ready") { player.playbackState == PlayerConstants.STATE_READY }
            await("playback clock advances") { player.isPlaying && player.currentPosition > 1_000 }
            await("Now Playing metadata published") {
                MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo?.get(MPMediaItemPropertyTitle) == first.metadata.title
            }
            player.pause()
            delay(400)
            val pausedAt = player.currentPosition
            delay(700)
            check(!player.isPlaying && kotlin.math.abs(player.currentPosition - pausedAt) < 200)
            report.appendLine("PASS pause holds position")
            player.seekTo(6_000)
            await("seek reaches target") { kotlin.math.abs(player.currentPosition - 6_000) < 750 }
            player.moveMediaItem(1, 2)
            check(player.currentMediaItemIndex == 2)
            report.appendLine("PASS duplicate track move preserves identity")
            player.removeMediaItem(0)
            check(player.currentMediaItemIndex == 1)
            report.appendLine("PASS duplicate removal preserves current index")
            player.seekToPreviousMediaItem()
            await("previous selects adjacent track") { player.currentMediaItem?.mediaId == second.mediaId }
            player.seekToNext()
            await("next selects adjacent track") { player.currentMediaItemIndex == 1 }
            player.play()
            await("resumes playback after track change") { player.isPlaying && player.currentPosition > 500 }
            player.clearMediaItems()
            check(player.mediaItemCount == 0 && !player.hasNextMediaItem())
            report.appendLine("PASS empty queue has no next item")
            check(selected == null)
            report.appendLine("PASS empty queue clears metadata")
            report.appendLine("RESULT PASS")
        } catch (error: Throwable) {
            report.appendLine("RESULT FAIL ${error.message}")
            report.appendLine("state=${player.playbackState} intent=${player.playWhenReady} playing=${player.isPlaying} position=${player.currentPosition} rate=${player.nativePlayer.rate}")
        } finally {
            player.removeListener(listener)
            FileSystem.SYSTEM.createDirectories(fileDir().toPath())
            FileSystem.SYSTEM.write((fileDir() + "/ios-playback-smoke.txt").toPath()) { writeUtf8(report.toString()) }
            println(report.toString())
        }
    }
}
