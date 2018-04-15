# custom-video-player

CustomVideoPlayer customVideoPlayer = findViewById(R.id.customVideoPlayer);     
customVideoPlayer.setMediaUrl("https://www.rmp-streaming.com/media/bbb-360p.mp4")
                 .enableAutoMute(true)
                 .enableAutoPlay(false)
                 .hideControllers(true)
                 .setOnPlaybackListener(this)
                 .build();
                 <br/>
<b>Play</b><br/>
customVideoPlayer.play();
<br/>
<b>Pause</b><br/>
customVideoPlayer.pause();
<br/>
<b>Stop</b><br/>
customVideoPlayer.stop();
