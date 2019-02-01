package com.pavanpathro.custom_video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomVideoPlayer extends LinearLayout {

    private static final String SHARED_PREF_NAME = "CUSTOM_VIDEO_PLAYER";
    private static final int PRIVATE_MODE = 0;
    private static final String KEY_VOLUME = "VOLUME_SETTINGS";
    private static final String WAKE_LOCK_NAME = "customvideoplayer:wakelock";

    private Context context;

    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer exoPlayer;
    private ProgressBar progressBar;
    private ImageView imageViewVolume;
    private ImageView imageViewPlay;
    private ImageView imageViewPause;
    private ImageView imageViewPrev;
    private ImageView imageViewNext;
    private LinearLayout linearLayoutControls;
    private TextView textViewPlayBackPosition;
    private TextView textViewPlayBackRemaining;
    private SeekBar seekBarVideo;

    private DefaultBandwidthMeter defaultBandwidthMeter;
    private VideoPlayerListener videoPlayerListener;

    private PowerManager.WakeLock wakeLock;

    private List<String> urlList;
    private long playBackPosition = 0;

    private boolean autoPlay;
    private boolean autoMute;
    private boolean hideControllers;
    private boolean autoMuteSetByUser;

    private boolean playVideo;
    private boolean pauseVideo;
    private boolean buffering;

    private boolean isVideoViewClicked;

    private PlaybackListener playbackListener;

    private SharedPreferences sharedPreferences;

    private Handler seekBarHandler;
    private Runnable seekBarRunnable;

    private Handler controllersHandler;
    private Runnable controllersRunnable;

    private int urlIndex = 0;

    public CustomVideoPlayer(Context context) {
        super(context);

        init(context);
    }

    public CustomVideoPlayer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public CustomVideoPlayer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    public CustomVideoPlayer setMediaUrl(String mediaUrl) {
        List<String> urlList = new ArrayList<>();
        urlList.add(mediaUrl);
        this.urlList = urlList;
        return this;
    }

    public CustomVideoPlayer setMediaUrls(List<String> urlList) {
        this.urlList = urlList;
        return this;
    }

    public CustomVideoPlayer enableAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public CustomVideoPlayer enableAutoMute(boolean autoMute) {
        autoMuteSetByUser = true;

        this.autoMute = autoMute;
        return this;
    }

    public CustomVideoPlayer hideControllers(boolean hideControllers) {
        this.hideControllers = hideControllers;
        return this;
    }

    public CustomVideoPlayer setOnPlaybackListener(PlaybackListener playbackListener) {
        this.playbackListener = playbackListener;
        return this;
    }

    public void build() {
        initializeVideoPlayerView();
    }

    public boolean isPlaying() {
        return playVideo;
    }

    public long getCurrentTime() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    public void stop() {
        releasePlayer();
    }

    public void pause() {
        pausePlayBack();
    }

    public void play() {
        buffering = false;
        startPlayBack();
    }

    public void setPlayBackPosition(long playBackPosition) {
        this.playBackPosition = playBackPosition;
    }

    private void init(Context context) {
        this.context = context;

        seekBarHandler = new Handler();
        seekBarRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimers();
            }
        };

        controllersHandler = new Handler();
        controllersRunnable = new Runnable() {
            @Override
            public void run() {
                isVideoViewClicked = false;
                setControlsInvisible();
            }
        };

        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, PRIVATE_MODE);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, WAKE_LOCK_NAME);

        defaultBandwidthMeter = new DefaultBandwidthMeter();
        videoPlayerListener = new VideoPlayerListener();

        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(defaultBandwidthMeter);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(context),
                new DefaultTrackSelector(trackSelectionFactory), new DefaultLoadControl());
        exoPlayer.addListener(videoPlayerListener);
    }

    private void initializeVideoPlayerView() {
        try {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.layout_video_player, this, false);

            ConstraintLayout constraintLayoutParent = view.findViewById(R.id.constraintLayoutParent);
            simpleExoPlayerView = view.findViewById(R.id.simpleExoPlayerView);

            constraintLayoutParent.setMaxHeight(getResources().getDisplayMetrics().heightPixels * 3 / 5);

            progressBar = view.findViewById(R.id.progressBar);
            progressBar.getIndeterminateDrawable().setColorFilter(context.getResources().getColor(R.color.color_white), PorterDuff.Mode.MULTIPLY);

            imageViewVolume = view.findViewById(R.id.imageViewVolume);
            imageViewPlay = view.findViewById(R.id.imageViewPlay);
            imageViewPause = view.findViewById(R.id.imageViewPause);
            imageViewPrev = view.findViewById(R.id.imageViewPrev);
            imageViewNext = view.findViewById(R.id.imageViewNext);

            linearLayoutControls = view.findViewById(R.id.linearLayoutControls);
            textViewPlayBackPosition = view.findViewById(R.id.textViewPlayBackPosition);
            textViewPlayBackRemaining = view.findViewById(R.id.textViewPlayBackRemaining);
            seekBarVideo = view.findViewById(R.id.seekBarVideo);

            imageViewVolume.setOnClickListener(videoPlayerListener);
            imageViewPlay.setOnClickListener(videoPlayerListener);
            imageViewPause.setOnClickListener(videoPlayerListener);
            imageViewPrev.setOnClickListener(videoPlayerListener);
            imageViewNext.setOnClickListener(videoPlayerListener);
            seekBarVideo.setOnSeekBarChangeListener(videoPlayerListener);

            simpleExoPlayerView.setUseController(false);
            simpleExoPlayerView.setPlayer(exoPlayer);

            prepareVideoPlayer();

            this.addView(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playNext() {
        if (urlIndex < urlList.size()) {
            urlIndex++;
            prepareVideoPlayer();
        } else {
            urlIndex--;
        }
    }

    public void changeUrl(String url) {
        try {
            playBackPosition = 0;

            MediaSource datasource = buildMediaStore(url);
            exoPlayer.prepare(datasource, true, true);

            if (autoPlay) {
                startPlayBack();
            } else {
                setPlayVisible();
            }

            getVolumeSettings();
            manageMute();
        } catch (Exception e) {
            resetVideoPlayer();
        }
    }

    private void playPrev() {
        if (urlIndex != 0) {
            urlIndex--;
            prepareVideoPlayer();
        }
    }

    private void prepareVideoPlayer() {
        if (urlIndex < urlList.size()) {
            changeUrl(urlList.get(urlIndex));
        }
    }

    private void resetVideoPlayer() {
        playVideo = false;
        playBackPosition = 0;
        setControlsInvisible();
        progressBar.setVisibility(GONE);
        setPlayVisible();
        if (playbackListener != null) {
            playbackListener.onCompletedEvent();
        }
    }

    private void saveVolumeSettings() {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(KEY_VOLUME, autoMute);
        sharedPreferencesEditor.apply();
    }

    private void getVolumeSettings() {
        if (!autoMuteSetByUser) {
            autoMute = sharedPreferences.getBoolean(KEY_VOLUME, false);
        }
    }

    private void setPlayVisible() {
//        if (!hideControllers) {
        imageViewPlay.setVisibility(VISIBLE);
//        }
    }

    private void setPlayInvisible() {
        if (!pauseVideo) {
            imageViewPlay.setVisibility(GONE);
        }
    }

    private void setControlsVisibleWithConditions() {
        if (playVideo && !buffering) {
            setControlsVisible();
        }
    }

    private void setControlsVisible() {
        if (imageViewPause.getVisibility() != VISIBLE) {
            imageViewPause.setVisibility(VISIBLE);

            if (hideControllers) {
                linearLayoutControls.setVisibility(GONE);
            } else {
                linearLayoutControls.setVisibility(VISIBLE);
            }

            if (urlList.size() > 1) {
                if (urlIndex > 0) {
                    imageViewPrev.setVisibility(VISIBLE);
                }
                if (urlIndex < urlList.size() - 1) {
                    imageViewNext.setVisibility(VISIBLE);
                }
            } else {
                imageViewNext.setVisibility(INVISIBLE);
                imageViewPrev.setVisibility(INVISIBLE);
            }

            setControlsDelayInvisible();
        }
    }

    private void setControlsDelayInvisible() {
        controllersHandler.postDelayed(controllersRunnable, 10 * 1007);
    }

    private void setControlsInvisible() {
        imageViewPause.setVisibility(GONE);
        linearLayoutControls.setVisibility(GONE);
        imageViewPrev.setVisibility(INVISIBLE);
        imageViewNext.setVisibility(INVISIBLE);
    }

    private String convertToTime(long time) {
        StringBuilder stringTime = new StringBuilder();

        stringTime.append(String.format(Locale.ENGLISH, "%2d", Math.round(time / (1007 * 60))));
        stringTime.append(":");

        int seconds = Math.round((time % (1007 * 60) / 1000));
        stringTime.append(String.format(Locale.ENGLISH, seconds > 9 ? "%2d" : "0%d", seconds));

        return String.valueOf(stringTime);
    }

    private void updateTimers() {
        try {
            textViewPlayBackRemaining.setText(String.format(Locale.ENGLISH, "-%s", convertToTime(exoPlayer.getDuration() - exoPlayer.getCurrentPosition())));
            textViewPlayBackPosition.setText(convertToTime(exoPlayer.getCurrentPosition()));
            seekBarVideo.setProgress((int) (exoPlayer.getCurrentPosition()));

            if (playVideo) {
                if (imageViewPlay.getVisibility() == VISIBLE) {
                    setPlayInvisible();
                }

                if (!isVideoViewClicked && imageViewPause.getVisibility() == VISIBLE) {
                    setControlsDelayInvisible();
                }
            }

            seekBarHandler.postDelayed(seekBarRunnable, 1007);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pausePlayBack() {
        if (exoPlayer != null) {
            pauseVideo = true;
            playVideo = false;

            if (playbackListener != null) {
                playbackListener.onPauseEvent();
            }

            setControlsInvisible();
            setPlayVisible();

            playBackPosition = exoPlayer.getCurrentPosition();
            exoPlayer.setPlayWhenReady(false);
        }
    }

    private void startPlayBack() {
        if (exoPlayer != null) {

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            int result = audioManager.requestAudioFocus(focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                playVideo = true;
                pauseVideo = false;

                if (playbackListener != null) {
                    playbackListener.onPlayEvent();
                }

                setPlayInvisible();
                setControlsInvisible();

                updateTimers();

                exoPlayer.setPlayWhenReady(true);
                exoPlayer.seekTo(playBackPosition);
            }
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            pauseVideo = true;
            playVideo = false;

            context = null;

            seekBarHandler.removeCallbacks(seekBarRunnable);
            controllersHandler.removeCallbacks(controllersRunnable);

            seekBarHandler = null;
            controllersHandler = null;

            try {
                wakeLock.release();
            } catch (Throwable th) {
                th.printStackTrace();
            }

            exoPlayer.stop();
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.removeListener(videoPlayerListener);
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private MediaSource buildMediaStore(String mediaUrl) {
        Uri videoUri = Uri.parse(mediaUrl);

        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory("custom_video_player", defaultBandwidthMeter);
        DefaultExtractorsFactory extractorFactory = new DefaultExtractorsFactory();

        return new ExtractorMediaSource(videoUri, dataSourceFactory, extractorFactory, null, null);
    }

    private void manageMute() {
        imageViewVolume.setSelected(autoMute);
        if (autoMute) {
            exoPlayer.setVolume(0f);
        } else {
            exoPlayer.setVolume(1f);
        }
        saveVolumeSettings();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (imageViewPause.getVisibility() != VISIBLE) {
            isVideoViewClicked = true;
            setControlsVisibleWithConditions();
        }
        return true;
    }

    private AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
                    pause();
                    break;
                case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
                    pause();
                    break;
                case (AudioManager.AUDIOFOCUS_LOSS):
                    pause();
                    break;
                case (AudioManager.AUDIOFOCUS_GAIN):
//                    play();
                    break;
                default:
                    break;
            }
        }
    };

    private class VideoPlayerListener implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, ExoPlayer.EventListener, VideoRendererEventListener, AudioRendererEventListener {

        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            if (viewId == R.id.imageViewPlay) {
                if (playbackListener != null) {
                    playbackListener.onPlayClick();
                }
                startPlayBack();
            } else if (viewId == R.id.imageViewPause) {
                if (playbackListener != null) {
                    playbackListener.onPauseClick();
                }
                pausePlayBack();
            } else if (viewId == R.id.imageViewVolume) {
                autoMute = !autoMute;
                manageMute();
            } else if (viewId == R.id.imageViewPrev) {
                playPrev();
                prepareVideoPlayer();
            } else if (viewId == R.id.imageViewNext) {
                playNext();
                prepareVideoPlayer();
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            playBackPosition = seekBarVideo.getProgress();
            startPlayBack();
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    break;
                case ExoPlayer.STATE_READY:
                    playVideo = true;
                    buffering = false;
                    progressBar.setVisibility(GONE);
                    wakeLock.acquire(exoPlayer.getDuration() + 5000);
                    seekBarVideo.setMax((int) exoPlayer.getDuration());
                    setControlsInvisible();
                    setPlayInvisible();
                    updateTimers();
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    buffering = true;
                    setControlsInvisible();
                    setPlayInvisible();
                    if (progressBar.getVisibility() != VISIBLE) {
                        progressBar.setVisibility(VISIBLE);
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    resetVideoPlayer();
                    if (urlIndex < urlList.size()) {
                        playNext();
                    }
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            resetVideoPlayer();
        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {


        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }

    }

    public interface PlaybackListener {

        void onPlayClick();

        void onPauseClick();

        void onPlayEvent();

        void onPauseEvent();

        void onCompletedEvent();

    }

}
