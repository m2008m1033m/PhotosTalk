package com.photostalk.utils;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

/**
 * Created by mohammed on 3/4/16.
 */
public class Player {

    public interface OnPlayerUpdateListener {
        void onUpdate(int position);
    }

    private MediaPlayer mMediaPlayer;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private Runnable mOnFailedToLoadRunnable;

    private Handler mHandler;

    private Runnable mPlayRunnable;
    private Runnable mSeekRunnable;
    private Runnable mUpdateRunnable;

    private String mAudioUrl;
    private int mSeekTo;
    private boolean mIsPlaying = false;

    public Player(
            MediaPlayer.OnPreparedListener onPreparedListener,
            MediaPlayer.OnSeekCompleteListener onSeekCompleteListener,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            Runnable onFailedToLoadRunnable,
            final OnPlayerUpdateListener onPlayerUpdateListener) {
        mOnPreparedListener = onPreparedListener;
        mOnSeekCompleteListener = onSeekCompleteListener;
        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
                if (onCompletionListener != null)
                    onCompletionListener.onCompletion(mediaPlayer);
            }
        };
        mOnFailedToLoadRunnable = onFailedToLoadRunnable;

        mUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer == null) return;
                synchronized (mMediaPlayer) {
                    if (onPlayerUpdateListener != null)
                        onPlayerUpdateListener.onUpdate(mMediaPlayer.getCurrentPosition());
                    mHandler.postDelayed(this, 10);
                }
            }
        };

        init();
    }

    public void init() {
        mHandler = new Handler(Looper.getMainLooper());

        mPlayRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mMediaPlayer == null) return;
                    synchronized (mMediaPlayer) {
                        mMediaPlayer.setDataSource(mAudioUrl);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    }
                    mHandler.post(mUpdateRunnable);
                } catch (IOException e) {
                    mHandler.post(mOnFailedToLoadRunnable);
                }
            }
        };

        mSeekRunnable = new Runnable() {
            @Override
            public void run() {
                mMediaPlayer.seekTo(mSeekTo);
            }
        };
    }

    public void play(String audioUrl) {
        stop();
        if (mMediaPlayer != null) return;
        mMediaPlayer = new MediaPlayer();
        synchronized (mMediaPlayer) {
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mAudioUrl = audioUrl;
            mIsPlaying = true;
        }
        new Thread(mPlayRunnable).start();
    }

    public void stop() {

        if (mMediaPlayer == null) return;
        synchronized (mMediaPlayer) {
            mIsPlaying = false;
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
    }

    public void seek(int seekTo) {
        mSeekTo = seekTo;
        new Thread(mSeekRunnable).start();
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public void pause() {
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) return;
        synchronized (mMediaPlayer) {
            mMediaPlayer.pause();
        }
    }

    public void resume() {
        if (mMediaPlayer == null || mMediaPlayer.isPlaying()) return;
        synchronized (mMediaPlayer) {
            mMediaPlayer.start();
        }
    }

    public boolean isPlaying() {
        if (mMediaPlayer == null) return false;
        synchronized (mMediaPlayer) {
            return mMediaPlayer.isPlaying();
        }
    }

}
