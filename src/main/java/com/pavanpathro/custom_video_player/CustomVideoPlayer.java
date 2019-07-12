package com.pavanpathro.custom_video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

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

public class CustomVideoPlayer extends LinearLayout {

    private static final String SHARED_PREF_NAME = "CUSTOM_VIDEO_PLAYER";
    private static final int PRIVATE_MODE = 0;
    private static final String KEY_VOLUME = "VOLUME_SETTINGS";

    private Context context;

    private SimpleExoPlayer exoPlayer;
    private ProgressBar progressBar;
    private ImageView imageViewVolume;

    private DefaultBandwidthMeter defaultBandwidthMeter;
    private VideoPlayerListener videoPlayerListener;

    private long playBackPosition = 0;

    private boolean autoPlay;
    private boolean autoMute;
    private boolean hideControllers;
    private boolean autoMuteSetByUser;

    private boolean playVideo;

    private PlaybackListener playbackListener;

    private SharedPreferences sharedPreferences;

    private int maxHeight;
    private int minHeight;

    private String mediaUrl;

    private SimpleExoPlayerView simpleExoPlayerView;
    private AudioManager audioManager;

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
        this.mediaUrl = mediaUrl;
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

    public CustomVideoPlayer setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public CustomVideoPlayer setMinHeight(int minHeight) {
        this.minHeight = minHeight;
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
        startPlayBack();
    }

    public void setPlayBackPosition(long playBackPosition) {
        this.playBackPosition = playBackPosition;
    }

    private void init(Context context) {
        this.context = context;

        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, PRIVATE_MODE);

        defaultBandwidthMeter = new DefaultBandwidthMeter();
        videoPlayerListener = new VideoPlayerListener();

        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory(defaultBandwidthMeter);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(context),
                new DefaultTrackSelector(trackSelectionFactory), new DefaultLoadControl());
        exoPlayer.addListener(videoPlayerListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.setMeasuredDimension(parentWidth, parentHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initializeVideoPlayerView() {
        try {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.layout_video_player, this, false);

            final ConstraintLayout constraintLayoutParent = view.findViewById(R.id.constraintLayoutParent);

            ViewGroup.LayoutParams layoutParams = constraintLayoutParent.getLayoutParams();
            layoutParams.width = this.getLayoutParams().width;
            layoutParams.height = this.getLayoutParams().height;

            if (minHeight != 0) {
                constraintLayoutParent.setMinHeight(minHeight);
            }

            if (maxHeight != 0) {
                constraintLayoutParent.setMaxHeight(maxHeight);
                layoutParams.height = maxHeight;
            }

            constraintLayoutParent.setLayoutParams(layoutParams);

            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            simpleExoPlayerView = view.findViewById(R.id.simpleExoPlayerView);
            simpleExoPlayerView.setUseController(!hideControllers);
            simpleExoPlayerView.setControllerShowTimeoutMs(1500);
            simpleExoPlayerView.hideController();

            progressBar = view.findViewById(R.id.progressBar);
            progressBar.getIndeterminateDrawable().setColorFilter(context.getResources().getColor(R.color.color_white), PorterDuff.Mode.MULTIPLY);

            imageViewVolume = view.findViewById(R.id.imageViewVolume);
            imageViewVolume.setOnClickListener(videoPlayerListener);

            simpleExoPlayerView.setPlayer(exoPlayer);

            prepareVideoPlayer();

            this.addView(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeUrl(String newMediaUrl) {
        this.mediaUrl = newMediaUrl;

        prepareVideoPlayer();
    }

    private void prepareVideoPlayer() {
        try {
            if (mediaUrl != null) {
                playBackPosition = 0;

                MediaSource mediaSource = buildMediaStore(mediaUrl);
                exoPlayer.prepare(mediaSource, true, true);

                if (autoPlay) {
                    startPlayBack();
                }

                getVolumeSettings();
                manageMute();
            }
        } catch (Exception e) {
            e.printStackTrace();
            resetVideoPlayer();
        }
    }

    private void resetVideoPlayer() {
        playVideo = false;
        playBackPosition = 0;
        progressBar.setVisibility(GONE);
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

    private void pausePlayBack() {
        if (exoPlayer != null) {
            playVideo = false;

            if (playbackListener != null) {
                playbackListener.onPauseEvent();
            }

            playBackPosition = exoPlayer.getCurrentPosition();
            exoPlayer.setPlayWhenReady(false);
        }
    }

    private void startPlayBack() {
        if (exoPlayer != null) {
            playVideo = true;

            if (playbackListener != null) {
                playbackListener.onPlayEvent();
            }

            exoPlayer.setPlayWhenReady(true);
            exoPlayer.seekTo(playBackPosition);
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            playVideo = false;

            context = null;

            audioManager.abandonAudioFocus(focusChangeListener);

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

            audioManager.abandonAudioFocus(focusChangeListener);

        } else {
            exoPlayer.setVolume(1f);

            audioManager.requestAudioFocus(focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        saveVolumeSettings();
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

    private class VideoPlayerListener implements OnClickListener, SeekBar.OnSeekBarChangeListener, ExoPlayer.EventListener, VideoRendererEventListener, AudioRendererEventListener {

        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            if (viewId == R.id.imageViewVolume) {
                playbackListener.onVolumeChange(autoMute);
                autoMute = !autoMute;
                manageMute();
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
                    simpleExoPlayerView.hideController();
                    playVideo = true;
                    progressBar.setVisibility(GONE);
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    if (progressBar.getVisibility() != VISIBLE) {
                        simpleExoPlayerView.hideController();
                        progressBar.setVisibility(VISIBLE);
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    resetVideoPlayer();
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

        void onVolumeChange(boolean volumeOn);

        void onPlayEvent();

        void onPauseEvent();

        void onCompletedEvent();

    }

}
