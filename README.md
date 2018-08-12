# custom-video-player

<b>initialize</b>

    CustomVideoPlayer customVideoPlayer = findViewById(R.id.customVideoPlayer);     
    customVideoPlayer.setMediaUrl("https://www.rmp-streaming.com/media/bbb-360p.mp4")
                   .enableAutoMute(true)
                   .enableAutoPlay(false)
                   .hideControllers(true)
                   .setOnPlaybackListener(this)
                   .build();

<b>to play</b>
      
    customVideoPlayer.play();

<b>to pause</b>
     
    customVideoPlayer.pause();

<b>Stop</b>
 
    customVideoPlayer.stop();
