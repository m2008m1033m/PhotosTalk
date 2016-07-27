package com.photostalk.utils.recorder;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.photostalk.R;
import com.photostalk.utils.FileUtils;
import com.photostalk.utils.Notifications;

import java.io.File;
import java.io.IOException;

public class RecorderOldAPI extends Recorder {

    private MediaRecorder mMediaRecorder;


    private String mFilename_tmp;
    private boolean mHasRecorded;
    private long mStartingTime;

    public RecorderOldAPI(Activity activity, OnRecordingListener onRecordingListener, final MediaPlayer.OnCompletionListener onCompletionListener) {
        super(activity, onRecordingListener, onCompletionListener);
        mFilename_tmp = getFileName();
        mFilename_tmp = mFilename_tmp.replace(".mp4", "");
        mFilename_tmp = mFilename_tmp + "_tmp.mp4";
    }

    /**
     * @param maxDuration in seconds
     * @return recording started successfully or not
     */
    @Override
    public boolean record(int maxDuration) {
        if (getState() != NONE) return false;
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
            setState(RECORDING);
            startReportingThread();

            mStartingTime = System.currentTimeMillis();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Notifications.showSnackbar(mActivity, mActivity.getString(R.string.could_not_initilaize_the_recorder));
            return false;
        }
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void cancel() {
        stop(true);
    }

    @Override
    public boolean hasRecorded() {
        return mHasRecorded;
    }

    @Override
    public void clean() {
        mHasRecorded = false;
    }

    private void stop(boolean isCancelled) {
        if (getState() != RECORDING) return;
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        setState(NONE);
        if (!isCancelled) {
            try {
                deletePrevious(false);
                FileUtils.copy(mFilename_tmp, getFileName());
                mHasRecorded = true;
            } catch (IOException e) {
                Notifications.showSnackbar(mActivity, mActivity.getString(R.string.error_while_saving_file));
            }
        }
        deletePrevious(true);
    }

    private void deletePrevious(boolean deleteTmp) {
        if (!mHasRecorded) return;
        File f = new File(deleteTmp ? mFilename_tmp : getFileName());
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
                    if (mMediaRecorder != null) {
                        int elapsed = (int) ((System.currentTimeMillis() - mStartingTime) / 1000.0f);
                        mOnRecordingListener.onUpdate(mMediaRecorder.getMaxAmplitude(), elapsed);
                    }
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (getState() == RECORDING) {
                        handler.post(r);
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            Log.d("RecorderOldAPI", "Failed to sleep");
                        }
                    }
                }
            }).start();
        }
    }

}