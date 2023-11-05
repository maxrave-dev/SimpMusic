# SimpMusic

<img src="https://raw.githubusercontent.com/maxrave-dev/SimpMusic/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" height="72">  
A simple music app using YouTube Music for backend  

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="80">](https://apt.izzysoft.de/packages/com.maxrave.simpmusic/)  [<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80">](https://f-droid.org/en/packages/com.maxrave.simpmusic/)

[![Latest release](https://img.shields.io/github/v/release/maxrave-dev/SimpMusic)](https://github.com/maxrave-dev/SimpMusic/releases)   [![Downloads](https://img.shields.io/github/downloads/maxrave-dev/SimpMusic/total)](https://github.com/maxrave-dev/SimpMusic/releases)

> **Warning**
>
>This app is in beta stage, so it may have many bugs and make it crash. If you find any bug, please create an issue or contact me via email.

## Features

- Play music from YouTube Music or YouTube free without ads in the background
- Browsing Home, Charts, Podcast, Moods & Genre with YouTube Music data with high speed
- Search everything on YouTube
- Analyze your playing data, create custom playlists and sync with YouTube Music...
- Caching and can save data for offline playback
- Synced lyrics from Musixmatch and YouTube Transcript and translate lyrics (Community translation
  from Musixmatch)
- Personalize data (*)
- Support SponsorBlock
- Sleep Timer
- Android Auto
- And many more

> (*) For users who choosed "Send back to Google" feature

## Screenshot

<p float="left">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/miniplayer_top.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/miniplayer_bottom.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/new_home_ui.jpg" width="200" />  
</p>  
<p float="left">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/moodmoment.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/chart.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/artist_top.jpg" width="200" />  
</p>  
<p float="left">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/radio.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/search_suggest.jpg" width="200" />  
 <img src="https://github.com/maxrave-dev/SimpMusic/blob/main/asset/screenshot/search_result.jpg" width="200" />  
</p>  

More [screenshot](https://photos.app.goo.gl/AbieoXG5ctDrpwzp7) here.

## Data

- This app using hidden API from YouTube Music with some tricks to get data from YouTube Music.
- Thanks for [InnerTune](https://github.com/z-huang/InnerTune/) for the idea to get data from
  YouTube Music. This repo is my inspiration to create this app
- My app is using [SponsorBlock](https://sponsor.ajay.app/) to skip sponsor in YouTube videos.
  Thanks for this great service
- Lyrics data from Musixmatch. More information [Musixmatch](https://developer.musixmatch.com/)

## Privacy

SimpMusic don't have any tracker or third party server for collecting user data. If YouTube
logged-in user enable "Send back to Google" feature, SimpMusic only use YouTube Music Tracking API
to send listening history and listening record of video to Google for better recommendation and
supporting artist or YouTube Creator (For API refference,
see [this](https://github.com/maxrave-dev/SimpMusic/blob/13f7ab6e5fa521b62a9fd31a1cefdc2787a1a8af/kotlinYtmusicScraper/src/main/java/com/maxrave/kotlinytmusicscraper/Ytmusic.kt#L639C4-L666C1)).

## Translation

[![Crowdin](https://badges.crowdin.net/simpmusic/localized.svg)](https://crowdin.com/project/simpmusic)  
You can help me to translate this app to your language by using
Crowdin [SimpMusic on Crowdin](https://crowdin.com/project/simpmusic)

## FAQ

#### 1. Wrong Lyrics?

YouTube Music is not official partner of Musixmatch so can't get lyrics directly if using YouTube "
videoId" parameter. So I need to use some "String Matcher" and "Duration" for search lyrics. So
sometime, some song or video get wrong lyric

#### 2. Why the name or brand is "SimpMusic"?

Simply, because I love this name. It's combination of Simple and Music. But SimpMusic is not simple
app, it's all you need about music streaming app.

## Support

Join my Discord server to update the newest features, tips and report bugs  
|   [<img src="https://upload.wikimedia.org/wikipedia/vi/7/72/Discord_logo.svg.png" height="40">]() | [Discord](https://discord.gg/Rq5tWVM9Hg) |  
|---------|---------|

## Donate

Support me to maintain SimpMusic. Thanks for using <3
|   [<img src="https://upload.wikimedia.org/wikipedia/commons/archive/b/b5/20230314142950%21PayPal.svg" height="40">]()         | [Paypal](https://paypal.me/maxraveofficial) |
|---------|---------|
|   [<img src="https://ucbcd975be5592f4047c73e2240d.previews.dropboxusercontent.com/p/thumb/AB9o8x62hcEshT5d7tJNtLQnWMCTUVcaVUagN-gpUhtPzK6fUmFjGbap39yjEUHl2XdTUR0-VyvB-gmpPLIodk_DuZrqI_cgJZJ7Xp7e9icqm2kkz-I_LR9eWsEX-fHi8eEm-oKXAgYsEljob-R_atzeXNWQRdHMJRXT4WObK4hICakqBP76drrO7qPX9yOl4BJnEey_RopvMIAr3vG17PLeKB5OPwPq16tCT4TE3cJeq_Sn9h-wNS0oYSFY9KcChWctBVwE9COnUbJz-DtVCjLNlyOA1f7m3TdYqQr0Qgysw8Xl3Asmh8PGiMPrCwVntpKD5IMs5UybLJtkzvw_dFe-/p.png" height="40">]()         | [Buy Me A Coffee](https://www.buymeacoffee.com/maxrave) |
