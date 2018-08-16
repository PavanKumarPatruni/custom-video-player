# custom-video-player

<h3>Add to layout</h3>
<br/>

    <com.pavanpathro.custom_video_player.CustomVideoPlayer
        android:id="@+id/customVideoPlayer"
        android:layout_width="match_parent"
        android:layout_height="300dp" />

<h3>Initialise Video Player</h3>
<br/>

    CustomVideoPlayer customVideoPlayer = findViewById(R.id.customVideoPlayer);

<b>Single Video</b>

    customVideoPlayer.setMediaUrl("https://www.rmp-streaming.com/media/bbb-360p.mp4");

<b>Multi Video</b>

    List<String> listOfVideos = new ArrayList<>();
    listOfVideos.add("http://mirrors.standaloneinstaller.com/video-sample/jellyfish-25-mbps-hd-hevc.3gp");
    listOfVideos.add("http://mirrors.standaloneinstaller.com/video-sample/star_trails.mkv");
    listOfVideos.add("https://www.rmp-streaming.com/media/bbb-360p.mp4");
    
    customVideoPlayer.setMediaUrls(listOfVideos);

<b>Auto Mute</b> (Default - false)
                       
    customVideoPlayer.enableAutoMute(true);


<b>Auto Play</b> (Default - true)
                       
    customVideoPlayer.enableAutoPlay(false);
    
    
<b>Manage Controllers</b> (Default - false)
                       
    customVideoPlayer.hideControllers(true);
    
    
<b>Example</b>
    
    customVideoPlayer.setMediaUrls(listOfVideos)
        .enableAutoMute(false)
        .enableAutoPlay(false)
        .hideControllers(false)
        .setOnPlaybackListener(this)
        .build();
    
<h3>Video Controllers</h3>
<br/>

<b>Play</b>
      
    customVideoPlayer.play();

<b>Pause</b>
     
    customVideoPlayer.pause();

<b>Stop</b>
 
    customVideoPlayer.stop();
