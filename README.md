<div align="center">
	<img src="https://raw.githubusercontent.com/maxrave-dev/SimpMusic/jetpack_compose/fastlane/metadata/android/en-US/images/featureGraphic.png">
<h1>SimpMusic</h1>A FOSS YouTube Music client for Android with many features from<br>Spotify, Musixmatch, SponsorBlock, ReturnYouTubeDislike<br>
<br>
<a href="https://github.com/maxrave-dev/SimpMusic/releases"><img src="https://img.shields.io/github/v/release/maxrave-dev/SimpMusic"></a>
<a href="https://github.com/maxrave-dev/SimpMusic/releases"><img src="https://img.shields.io/github/downloads/maxrave-dev/SimpMusic/total"></a>
<br>
<br>
<a href="https://apt.izzysoft.de/packages/com.maxrave.simpmusic/"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="80"></a>
<a href="https://f-droid.org/en/packages/com.maxrave.simpmusic/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80"></a>
<a href="https://github.com/maxrave-dev/SimpMusic/releases"><img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" height="80"></a>
<h3>Nightly Build<h3>
<a href="https://nightly.link/maxrave-dev/SimpMusic/workflows/android/jetpack_compose/app.zip"><img src="https://github.com/maxrave-dev/SimpMusic/actions/workflows/android.yml/badge.svg"></a><br/>
<a href="https://nightly.link/maxrave-dev/SimpMusic/workflows/android/jetpack_compose/app.zip"><img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" height="80"></a>
</div>
	
## Features ✨️

- Play music from YouTube Music or YouTube free without ads in the background
- Browsing Home, Charts, Podcast, Moods & Genre with YouTube Music data at high speed
- Search everything on YouTube
- Analyze your playing data, create custom playlists, and sync with YouTube Music...
- Spotify Canvas supported
- Play 1080p video option with subtitle
- AI suggestions songs
- Notification from followed artists
- Caching and can save data for offline playback
- Synced lyrics from Musixmatch, LRCLIB, Spotify (require login) and YouTube Transcript and translated lyrics (Community translation from Musixmatch)
- Personalize data (*) and multi-YouTube-account support
- Support SponsorBlock, Return YouTube Dislike
- Sleep Timer
- Android Auto with online content
- And many more!

> (*) For users who chose "Send back to Google" feature

> **Warning**

>This app is in the beta stage, so it may have many bugs and make it crash. If you find any bugs,
> please create an issue or contact me via email or discord sever.

## Screenshots

<p align="center">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/2.png?raw=true" width="200" />  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/3.png?raw=true" width="200" />  
   <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/4.png?raw=true" width="200" />  
   <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/5.png?raw=true" width="200" />  
</p>  
<p align="center">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/6.png?raw=true" width="200" />  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/7.png?raw=true" width="200" />  
   <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/8.png?raw=true" width="200" />  
   <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/9.png?raw=true" width="200" />  
</p> 
<p align="center">  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/10.png?raw=true" width="200" />  
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/11.png?raw=true" width="200" /> 
  <img src="https://github.com/maxrave-dev/SimpMusic/blob/jetpack_compose/asset/screenshot/12.png?raw=true" width="200" /> 
</p>  

#### More [screenshot](https://photos.app.goo.gl/AbieoXG5ctDrpwzp7) here.

## Data

- This app uses hidden API from YouTube Music with some tricks to get data from YouTube Music.
- Use Spotify Web API and some tricks to get Spotify Canvas and Lyrics 
- Thanks to [InnerTune](https://github.com/z-huang/InnerTune/) for the idea to get data from YouTube Music. This repo is my inspiration to create this app
- My app is using [SponsorBlock](https://sponsor.ajay.app/) to skip sponsor in YouTube videos.
- ReturnYouTubeDislike for getting information on votes
- Lyrics data from Musixmatch and LRCLIB. More information [Musixmatch](https://developer.musixmatch.com/), [LRCLIB](https://lrclib.net/)

## Privacy

SimpMusic doesn't have any tracker or third-party server for collecting user data. If YouTube
logged-in users enable "Send back to Google" feature, SimpMusic only uses YouTube Music Tracking API
to send listening history and listening record of video to Google for better recommendations and
supporting artist or YouTube Creator (For API reference,
see [this](https://github.com/maxrave-dev/SimpMusic/blob/13f7ab6e5fa521b62a9fd31a1cefdc2787a1a8af/kotlinYtmusicScraper/src/main/java/com/maxrave/kotlinytmusicscraper/Ytmusic.kt#L639C4-L666C1)).

## Translation

[![Crowdin](https://badges.crowdin.net/simpmusic/localized.svg)](https://crowdin.com/project/simpmusic)  
You can help me translate this app into your language by using Crowdin [SimpMusic on Crowdin](https://crowdin.com/project/simpmusic)

<details>

  <summary>Top Contributors</summary>

| Image | Username | Full Name | Language | Translated Words |
|-------|----------|-----------|----------|------------------|
| ![maxrave](https://crowdin-static.downloads.crowdin.com/avatar/14178407/medium/070e1d1286e9bb49bad0266a19315f12.jpeg) | maxrave | Minh (maxrave) | Arabic, Chinese Simplified, Chinese Traditional, Finnish, French, German, Indonesian, Italian, Japanese, Polish, Portuguese, Russian, Spanish (Modern), Turkish, Vietnamese,  | 4505 |
| ![eric100lin](https://crowdin-static.downloads.crowdin.com/avatar/16329472/medium/e246dc09dd9034e20c4451a3e1d9e476.jpeg) | eric100lin | Eric Lin (Tzu Hsiang Lin) (eric100lin) | Chinese Traditional,  | 1128 |
| ![AmrEraky](https://crowdin-static.downloads.crowdin.com/avatar/15904523/medium/87578e72fa90778859373c2319a603bd.png) | AmrEraky | AmrEraky | Arabic,  | 1112 |
| ![clxf12](https://crowdin-static.downloads.crowdin.com/avatar/15817767/medium/7908bdeb8f8a9662a912dca8b6057e99.jpeg) | clxf12 | Clxff H3r4ld0 (clxf12) | Indonesian,  | 997 |
| ![Cp0204](https://crowdin-static.downloads.crowdin.com/avatar/12904036/medium/61d5ff8ad17f02c74b25a4e3a6dd9f67_default.png) | Cp0204 | Cp0204 | Chinese Simplified,  | 982 |
| ![komputerX](https://crowdin-static.downloads.crowdin.com/avatar/15913787/medium/02e80f2376c45924c26f24464aa77e4e.png) | komputerX | komputerX | Turkish,  | 979 |
| ![andronedev](https://crowdin-static.downloads.crowdin.com/avatar/14635726/medium/56e04bb02ace2c8d4e1241e91354e4f4.png) | andronedev | andronedev | French,  | 945 |
| ![Zbynius](https://crowdin-static.downloads.crowdin.com/avatar/16051900/medium/28695595dbebdfc36be1191ed8f0aacb.png) | Zbynius | Zbynius | Polish,  | 942 |
| ![s.reszkojr](https://crowdin-static.downloads.crowdin.com/avatar/15131829/medium/07d7420c2d13bce1f7a6f11a02aec38e.png) | s.reszkojr | Fabio S. Reszko Jr. (s.reszkojr) | Portuguese,  | 854 |
| ![FileX](https://crowdin-static.downloads.crowdin.com/avatar/16014929/medium/afc91f10a28a18a7b74676eda79d59e4_default.png) | FileX | FileX | German, Japanese,  | 844 |
| ![teemue](https://crowdin-static.downloads.crowdin.com/avatar/13301586/medium/29845b9bc870769f9f447f1e09a38c8d.png) | teemue | teemue | Finnish,  | 829 |
| ![AndresQO](https://crowdin-static.downloads.crowdin.com/avatar/16046670/medium/e5b809b41b6f6e25eafe404c94955c43.jpeg) | AndresQO | Miguel Quicaño (AndresQO) | Spanish (Modern),  | 770 |
| ![Atuy1219](https://crowdin-static.downloads.crowdin.com/avatar/15747579/medium/86816e9e73cc890d6b4f7928369700a2_default.png) | Atuy1219 | Atuy (Atuy1219) | Japanese,  | 714 |
| ![Ronner231](https://crowdin-static.downloads.crowdin.com/avatar/16021342/medium/7734d550df2de5a2fec2ffff33e7024c.jpeg) | Ronner231 | Ronner (Ronner231) | Russian,  | 650 |
| ![BabyBenefactor](https://crowdin-static.downloads.crowdin.com/avatar/15977263/medium/6e2c41d65d0a3b0bde3190b4cd861fec.png) | BabyBenefactor | BabyBenefactor | Dutch,  | 481 |
| ![gaker19](https://crowdin-static.downloads.crowdin.com/avatar/15722805/medium/a0648c617565e26011dc6e17b491f8b0.png) | gaker19 | gaker19 | Dutch, German,  | 385 |
| ![ghostnear](https://crowdin-static.downloads.crowdin.com/avatar/14949851/medium/eb634c29f022de8fd90147a45b2b45d4_default.png) | ghostnear | ゴーストニアー (ghostnear) | Romanian,  | 328 |
| ![MONE_FIERA](https://crowdin-static.downloads.crowdin.com/avatar/15626257/medium/a27bb4112166ef9877bb1765745e69eb_default.png) | MONE_FIERA | MONE_FIERA | Japanese,  | 198 |
| ![Mora04](https://crowdin-static.downloads.crowdin.com/avatar/16291164/medium/d3b5acaf7706b7ddf497e9d55bf9d8bb.jpeg) | Mora04 | Santiago Moreno (Mora04) | Spanish (Modern),  | 164 |
| ![bkrucarci](https://crowdin-static.downloads.crowdin.com/avatar/12412213/medium/607e1e98ba8d34da038d86f7a0bb07b7_default.png) | bkrucarci | Bekir UÇARCI (bkrucarci) | Turkish,  | 160 |
| ![egpheel](https://crowdin-static.downloads.crowdin.com/avatar/16301586/medium/703735bada5f9aee60cf0eeaa4852679.jpeg) | egpheel | Eduardo Gil (egpheel) | Portuguese,  | 142 |
| ![milena-kos](https://crowdin-static.downloads.crowdin.com/avatar/15891805/medium/0ebd3d7f628e42995270613db4992c68.jpeg) | milena-kos | Milenakos (milena-kos) | Russian,  | 136 |
| ![stepersy](https://crowdin-static.downloads.crowdin.com/avatar/16198720/medium/b3b8d47d35498b04d293bf766d6e1842.jpeg) | stepersy | Stefano Persano Adorno (stepersy) | Italian,  | 123 |
| ![siggi1984](https://crowdin-static.downloads.crowdin.com/avatar/15963157/medium/af6f94fe7cea3595b77a2a8b41433d03.png) | siggi1984 | siggi1984 | German,  | 97 |
| ![espiondev](https://crowdin-static.downloads.crowdin.com/avatar/16248398/medium/0cad75225d2b737f3f0b9b0996b576f6.jpeg) | espiondev | Espion (espiondev) | French,  | 85 |
| ![Paxsenix0](https://crowdin-static.downloads.crowdin.com/avatar/16048552/medium/870a6f6d49e3c325058aa88e55bac81b.jpeg) | Paxsenix0 | Alex (Paxsenix0) | Indonesian,  | 77 |
| ![rikalaj](https://crowdin-static.downloads.crowdin.com/avatar/15079923/medium/317150e188196d6f8f4baa1a599acc65_default.png) | rikalaj | rikalaj | Finnish,  | 61 |
| ![BrightDV](https://crowdin-static.downloads.crowdin.com/avatar/15021663/medium/ad932c65a6328c1da3c125b0a8119eac.png) | BrightDV | BrightDV | French,  | 55 |
| ![RD3V](https://crowdin-static.downloads.crowdin.com/avatar/15973217/medium/dcd5a3a9ab51dc285ad89ded3cdb0c38.png) | RD3V | RD3V | Polish,  | 48 |
| ![krist7169](https://crowdin-static.downloads.crowdin.com/avatar/14290958/medium/acd35924b7b6710b3890a369ff507153.jpeg) | krist7169 | Šimon Krištufek (krist7169) | Czech,  | 42 |
| ![GeovaneDev](https://crowdin-static.downloads.crowdin.com/avatar/16119164/medium/a81e574f2dcbc6de7a8f10cc22c0fcb8.png) | GeovaneDev | Geovane (GeovaneDev) | Portuguese,  | 35 |
| ![chatinteligence](https://crowdin-static.downloads.crowdin.com/avatar/16121068/medium/f8814b63a40d7335d8f3eaf5b3b0176a.jpg) | chatinteligence | neonhack (chatinteligence) | Spanish (Modern),  | 33 |
| ![Egor418](https://crowdin-static.downloads.crowdin.com/avatar/15377634/medium/9ef6ca09b23addbc96c9e22009d50d25.jpeg) | Egor418 | Egor418 | Russian,  | 31 |
| ![cstogmuller](https://crowdin-static.downloads.crowdin.com/avatar/15983147/medium/f668476225ef71b84c96917a1fac7426_default.png) | cstogmuller | cstogmuller | German,  | 30 |
| ![REMOVED_USER](https://crowdin-static.downloads.crowdin.com/avatar/16214652/medium/c3b736a35e21d346ae618c6822cfc5bf_default.png) | REMOVED_USER | REMOVED_USER | Portuguese,  | 28 |
| ![tralalax](https://crowdin-static.downloads.crowdin.com/avatar/15996263/medium/a801a853776ac5462ceb89d5baf242b2.jpg) | tralalax | tralalax | French,  | 27 |
| ![hudiei52](https://crowdin-static.downloads.crowdin.com/avatar/13341742/medium/7628d04303020d499eaf5c7d4118ee2a.jpg) | hudiei52 | PH Pang (hudiei52) | Chinese Simplified,  | 19 |
| ![haudek](https://crowdin-static.downloads.crowdin.com/avatar/13995011/medium/9d2ea24d044e08d8776f4ca474bed239.jpeg) | haudek | Maciej Haudek (haudek) | Polish,  | 6 |
| ![bigstern54](https://crowdin-static.downloads.crowdin.com/avatar/16276884/medium/83ad048e5421d9b38257080fa6bb3ef2.png) | bigstern54 | big stern (bigstern54) | Russian,  | 5 |
| ![tyy2750709](https://crowdin-static.downloads.crowdin.com/avatar/16346776/medium/158f91f3a723d7b5ff2a873e70ea072f.png) | tyy2750709 | yyin tao (tyy2750709) | Chinese Simplified,  | 4 |
| ![chrisarabagas](https://crowdin-static.downloads.crowdin.com/avatar/16215132/medium/41aa129c66e8d728b513990478711bf6.png) | chrisarabagas | Argo Carpathians (chrisarabagas) | Indonesian,  | 2 |
| ![enescelikbas873](https://crowdin-static.downloads.crowdin.com/avatar/16220370/medium/6ebf150736d25e875a5d9bb464cfebac.png) | enescelikbas873 | Enes Çelikbaş (enescelikbas873) | Turkish,  | 2 |
| ![davidevol](https://crowdin-static.downloads.crowdin.com/avatar/14878728/medium/3026648aea7853e299fc782ff8bb9b2c.jpg) | davidevol | David (davidevol) | Portuguese,  | 2 |
| ![sujitfg1](https://crowdin-static.downloads.crowdin.com/avatar/16084474/medium/7c23adcfa96e3791f8cbc4a1a4518c10.png) | sujitfg1 | sujitfg1 | Hindi,  | 1 |

</details>

## FAQ

#### 1. Wrong Lyrics?

YouTube Music is not an official partner of Musixmatch so you can't get lyrics directly if using YouTube"
videoId" parameter. So I need to use some "String Matcher" and "Duration" for search lyrics. So
sometimes, some songs or videos get the wrong lyric's

#### 2. Why the name or brand is "SimpMusic"?

Simply, because I love this name. It's a combination of Simple and Music. But SimpMusic is not a simple app, it's all you need for a powerful music streaming app.

## Developer/Team

### [maxrave-dev](https://github.com/maxrave-dev/SimpMusic): Founder/Developer/Designer 

### [Owen Connor](https://github.com/owencz1998): Discord Server Admin. 

## Support & Donations
<div align="left">
<a href="https://simpmusic.tech/"><img alt="Visit the website" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/website_vector.svg"></a>
&nbsp;
<a href="https://discord.gg/Rq5tWVM9Hg"><img alt="Discord Server" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg"></a>
&nbsp;
<br>
<a href="https://www.buymeacoffee.com/maxrave"><img alt="Buy me a Coffee" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg"></a>
&nbsp;
<a href="https://liberapay.com/maxrave/"><img alt="liberapay" height="50"
src="https://raw.githubusercontent.com/liberapay/liberapay.com/master/www/assets/liberapay/logo-v2_black-on-yellow.svg"></a>
</div>

### MOMO or Vietnamese banking
<p float="left">
	<img src="https://github.com/maxrave-dev/SimpMusic/blob/dev/asset/52770992.jpg?raw=true" width="300">
</p>
