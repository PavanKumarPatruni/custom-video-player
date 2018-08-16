# custom-video-player

<b>Initialise</b>

    CustomVideoPlayer customVideoPlayer = findViewById(R.id.customVideoPlayer);     
    customVideoPlayer.setMediaUrl("https://www.rmp-streaming.com/media/bbb-360p.mp4")
                   .enableAutoMute(true)
                   .enableAutoPlay(false)
                   .hideControllers(true)
                   .setOnPlaybackListener(this)
                   .build();
                   
    List<String> listOfVideos = new ArrayList<>();
    listOfVideos.add("http://mirrors.standaloneinstaller.com/video-sample/jellyfish-25-mbps-hd-hevc.3gp");
    listOfVideos.add("http://mirrors.standaloneinstaller.com/video-sample/star_trails.mkv");
    listOfVideos.add("https://www.rmp-streaming.com/media/bbb-360p.mp4");
    
    customVideoPlayer.setMediaUrls(listOfVideos)
                   .enableAutoMute(true)
                   .enableAutoPlay(false)
                   .hideControllers(true)
                   .setOnPlaybackListener(this)
                   .build();

<b>Play</b>
      
    customVideoPlayer.play();

<b>Pause</b>
     
    customVideoPlayer.pause();

<b>Stop</b>
 
    customVideoPlayer.stop();
