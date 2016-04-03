package com.photostalk.utils;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.photostalk.R;

import java.io.File;
import java.io.IOException;

public class Recorder {

    public interface OnRecordingListener {
        void onUpdate(int amplitude);

        void onMaxDurationReached();
    }

    public static final int RECORDING = 0;
    public static final int PLAYING = 1;
    public static final int NONE = 2;

    private OnRecordingListener mOnRecordingListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaRecorder mMediaRecorder;
    private MediaPlayer mMediaPlayer;

    private String mFilename;
    private String mFilename_tmp;
    private boolean mHasRecorded;
    private int mState = NONE;
    private int mDuration = 0;
    private int mMaxDuration;

    private AppCompatActivity mActivity;

    public Recorder(AppCompatActivity activity, OnRecordingListener onRecordingListener, final MediaPlayer.OnCompletionListener onCompletionListener) {
        mFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp4";
        mFilename_tmp = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + System.currentTimeMillis() + "_tmp.mp4";
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
    }

    /**
     * @param maxDuration in seconds
     * @return
     */
    public boolean record(int maxDuration) {
        if (mState != NONE) return false;
        deletePrevious(true);
        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setMaxDuration(maxDuration * 1000);
        mMediaRecorder.setOutputFile(mFilename_tmp);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mOnRecordingListener.onMaxDurationReached();
                    stop();
                }

            }
        });
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mState = RECORDING;
            startReportingThread();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Notifications.showSnackbar(mActivity, mActivity.getString(R.string.could_not_initilaize_the_recorder));
            return false;
        }
    }

    public void stop() {
        stop(false);
    }

    public void cancel() {
        stop(true);
    }

    public boolean play() {
        if (mState != NONE) return false;
        if (hasRecorded()) {
            mMediaPlayer = loadMediaPlayer();
            mMediaPlayer.start();
            if (mOnCompletionListener != null)
                mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mState = PLAYING;
            return true;
        }
        return false;
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

    public void stopPlaying() {
        if (mState != PLAYING) return;
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mState = NONE;
    }

    public boolean hasRecorded() {
        return mHasRecorded;
    }

    public int getState() {
        return mState;
    }

    public String getFileName() {
        return mFilename;
    }

    public void clean() {
    }

    private void stop(boolean isCancelled) {
        if (mState != RECORDING) return;
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mState = NONE;
        if (!isCancelled) {
            try {
                deletePrevious(false);
                FileUtils.copy(mFilename_tmp, mFilename);
                mHasRecorded = true;
            } catch (IOException e) {
                Notifications.showSnackbar(mActivity, mActivity.getString(R.string.error_while_saving_file));
            }
        }
        deletePrevious(true);
    }

    private void deletePrevious(boolean deleteTmp) {
        if (!mHasRecorded) return;
        File f = new File(deleteTmp ? mFilename_tmp : mFilename);
        if (f.exists()) {
            f.delete();
        }
    }

    private void startReportingThread() {
        if (mOnRecordingListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (mMediaRecorder != null)
                        mOnRecordingListener.onUpdate(mMediaRecorder.getMaxAmplitude());
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mState == RECORDING) {
                        handler.post(r);
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            Log.d("Recorder", "Failed to sleep");
                        }
                    }
                }
            }).start();
        }
    }

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