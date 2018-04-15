package com.pavanpathro.custom_video_player;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.util.Locale;

public class CustomVideoPlayer extends LinearLayout {

    private Context context;

    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer exoPlayer;
    private ProgressBar progressBar;
    private ImageView imageViewVolume;
    private ImageView imageViewPlay;
    private ImageView imageViewPause;
    private LinearLayout linearLayoutControls;
    private TextView textViewPlayBackPosition;
    private TextView textViewPlayBackRemaining;
    private SeekBar seekBarVideo;

    private DefaultBandwidthMeter defaultBandwidthMeter;
    private VideoPlayerListener videoPlayerListener;

    private PowerManager.WakeLock wakeLock;

    private String mediaUrl = "";
    private long playBackPosition = 0;

    private boolean autoPlay;
    private boolean autoMute;
    private boolean hideControllers;

    private boolean playVideo;
    private boolean buffering;

    private boolean isVideoViewClicked;

    private PlaybackListener playbackListener;

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
        this.autoMute = autoMute;
        return this;
    }

    public CustomVideoPlayer hideControllers(boolean hideControllers) {
        if (autoPlay) {
            this.hideControllers = hideControllers;
        } else {
            this.hideControllers = false;
        }
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

    private void init(Context context) {
        this.context = context;

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

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

            if (getResources().getDisplayMetrics().widthPixels < getResources().getDisplayMetrics().heightPixels) {
                constraintLayoutParent.setMinHeight(610);
                constraintLayoutParent.setMaxHeight(610);
            } else {
                constraintLayoutParent.setMinHeight(getResources().getDisplayMetrics().heightPixels);
                constraintLayoutParent.setMaxHeight(getResources().getDisplayMetrics().heightPixels);
            }

            progressBar = view.findViewById(R.id.progressBar);
            progressBar.getIndeterminateDrawable().setColorFilter(context.getResources().getColor(R.color.color_white), PorterDuff.Mode.MULTIPLY);

            imageViewVolume = view.findViewById(R.id.imageViewVolume);
            imageViewPlay = view.findViewById(R.id.imageViewPlay);
            imageViewPause = view.findViewById(R.id.imageViewPause);

            linearLayoutControls = view.findViewById(R.id.linearLayoutControls);
            textViewPlayBackPosition = view.findViewById(R.id.textViewPlayBackPosition);
            textViewPlayBackRemaining = view.findViewById(R.id.textViewPlayBackRemaining);
            seekBarVideo = view.findViewById(R.id.seekBarVideo);

            constraintLayoutParent.setOnClickListener(videoPlayerListener);
            imageViewVolume.setOnClickListener(videoPlayerListener);
            imageViewPlay.setOnClickListener(videoPlayerListener);
            imageViewPause.setOnClickListener(videoPlayerListener);
            seekBarVideo.setOnSeekBarChangeListener(videoPlayerListener);

            simpleExoPlayerView.setUseController(false);
            simpleExoPlayerView.setPlayer(exoPlayer);

            exoPlayer.prepare(buildMediaStore(), true, true);

            if (autoPlay) {
                startPlayBack();
            } else {
                setPlayVisible();
            }

            manageMute();

            this.addView(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPlayVisible() {
        if (!hideControllers) {
            imageViewPlay.setVisibility(VISIBLE);
        }
    }

    private void setPlayInvisible() {
        imageViewPlay.setVisibility(GONE);
    }

    private void setControlsVisible() {
        if (!hideControllers && playVideo && !buffering) {
            if (imageViewPause.getVisibility() != VISIBLE) {
                imageViewPause.setVisibility(VISIBLE);
                linearLayoutControls.setVisibility(VISIBLE);

                setControlsDelayInvisible();
            }
        }
    }

    private void setControlsDelayInvisible() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setControlsInvisible();
                isVideoViewClicked = false;
            }
        }, 10 * 1007);
    }

    private void setControlsInvisible() {
        imageViewPause.setVisibility(GONE);
        linearLayoutControls.setVisibility(GONE);
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
                setPlayInvisible();
                if (!isVideoViewClicked) {
                    setControlsDelayInvisible();
                }
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateTimers();
                }
            }, 1007);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pausePlayBack() {
        if (exoPlayer != null) {

            playVideo = false;

            playbackListener.onPlayEvent();

            setControlsInvisible();
            setPlayVisible();

            playBackPosition = exoPlayer.getCurrentPosition();
            exoPlayer.setPlayWhenReady(false);
        }
    }

    private void startPlayBack() {
        if (exoPlayer != null) {
            playVideo = true;

            playbackListener.onPlayEvent();

            setPlayInvisible();
            setControlsInvisible();
            updateTimers();

            exoPlayer.setPlayWhenReady(true);
            exoPlayer.seekTo(playBackPosition);
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            playVideo = false;

            wakeLock.release();

            exoPlayer.stop();
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.removeListener(videoPlayerListener);
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private MediaSource buildMediaStore() {
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
    }

    private class VideoPlayerListener implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, ExoPlayer.EventListener, VideoRendererEventListener, AudioRendererEventListener {

        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            if (viewId == R.id.imageViewPlay) {
                startPlayBack();
            } else if (viewId == R.id.constraintLayoutParent) {
                isVideoViewClicked = true;
                setControlsVisible();
            } else if (viewId == R.id.imageViewPause) {
                pausePlayBack();
            } else if (viewId == R.id.imageViewVolume) {
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
                    buffering = false;
                    if (progressBar.getVisibility() != GONE) {
                        progressBar.setVisibility(GONE);
                    }
                    wakeLock.acquire(exoPlayer.getDuration());
                    seekBarVideo.setMax((int) exoPlayer.getDuration());
                    setControlsInvisible();
                    if (autoPlay) {
                        setPlayInvisible();
                        updateTimers();
                    } else {
                        setPlayVisible();
                    }
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
                    playVideo = false;
                    playBackPosition = 0;
                    setControlsInvisible();
                    progressBar.setVisibility(GONE);
                    setPlayVisible();

                    playbackListener.onCompleted();

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
        void onPlayEvent();

        void onPauseEvent();

        void onCompleted();
    }

}
