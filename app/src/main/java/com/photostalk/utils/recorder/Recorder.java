package com.photostalk.utils.recorder;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Environment;

import com.photostalk.R;
import com.photostalk.utils.Notifications;

import java.io.File;
import java.io.IOException;


public abstract class Recorder {

    public interface OnRecordingListener {
        void onUpdate(int amplitude, int elapsed);

        void onUpdate(short[] audioData, int length, int timeElapsed);

        void onMaxDurationReached();
    }

    public static final int RECORDING = 0;
    public static final int PLAYING = 1;
    public static final int NONE = 2;

    protected Activity mActivity;
    protected OnRecordingListener mOnRecordingListener;
    protected MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaPlayer mMediaPlayer;

    private String mFilename;
    private int mState;

    public Recorder(Activity activity, OnRecordingListener onRecordingListener, final MediaPlayer.OnCompletionListener onCompletionListener) {
        //mFilename = PhotosTalkApplication.getContext().getCacheDir() + File.separator + System.currentTimeMillis() + ".mp4";
        mFilename = Environment.getExternalStorageDirectory() + File.separator + System.currentTimeMillis() + ".mp4";

        mOnRecordingListener = onRecordingListener;
        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (onCompletionListener != null)
                    onCompletionListener.onCompletion(mediaPlayer);
                stopPlaying();
            }
        };
        mActivity = activity;

        setState(NONE);
    }

    public String getFileName() {
        return mFilename;
    }

    public int getDuration() {
        return loadMediaPlayer().getDuration();
    }

    public String getDurationFormatted() {
        int seconds = loadMediaPlayer().getDuration() / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return ((minutes < 100) ? "0" : "") + minutes + ":" + ((seconds < 10) ? "0" : "") + seconds;
    }

    public abstract boolean record(int maxDuration);

    public boolean play() {
        if (getState() != NONE) return false;
        if (hasRecorded()) {
            mMediaPlayer = loadMediaPlayer();
            mMediaPlayer.start();
            if (mOnCompletionListener != null)
                mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            setState(PLAYING);
            return true;
        }
        return false;
    }

    public void stopPlaying() {
        if (getState() != PLAYING) return;
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        setState(NONE);
    }

    public int getState() {
        return mState;
    }

    protected void setState(int state) {
        mState = state;
    }

    public abstract void stop();

    public abstract void cancel();

    public abstract boolean hasRecorded();

    public abstract void clean();

    private MediaPlayer loadMediaPlayer() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mFilename);
            mediaPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();
            Notifications.showSnackbar(mActivity, mActivity.getString(R.string.could_not_play_the_audio));
        }
        return mediaPlayer;
    }

}
