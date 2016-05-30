package com.photostalk.fragments;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.R;
import com.photostalk.customViews.AudioVisualizer;
import com.photostalk.customViews.WaveAudioVisualizer;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.PhotosTalkUtils;
import com.photostalk.utils.recorder.Recorder;
import com.photostalk.utils.recorder.RecorderNewAPI;
import com.photostalk.utils.recorder.RecorderOldAPI;

import java.io.File;


public class RecordFragment extends Fragment {

    private View mView;
    private CoordinatorLayout mWrapper;
    private ImageView mPhoto;
    private FloatingActionButton mRecordFAB;
    private LinearLayout mTimerContainer;
    private TextView mRecordingDuration;
    private View mRecordingIndicator;
    private View mAudioVisualizer;

    private Recorder mRecorder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.record_fragment, container, false);
        init();
        return mView;
    }

    public void setPhoto(String photoPath) {
        Glide.with(getActivity())
                .load(new File(photoPath))
                .into(mPhoto);

        initEvents();
    }

    public void setPhotoBitmap(Bitmap bitmap) {
        mPhoto.setImageBitmap(bitmap);
    }

    public Recorder getRecorder() {
        return mRecorder;
    }

    private void init() {
        initReferences();
        mView.post(new Runnable() {
            @Override
            public void run() {
                initEvents();
            }
        });
    }

    private void initReferences() {
        mWrapper = ((CoordinatorLayout) mView.findViewById(R.id.wrapper));
        mPhoto = ((ImageView) mView.findViewById(R.id.photo));
        mAudioVisualizer = mView.findViewById(Build.VERSION.SDK_INT >= 18 ? R.id.audio_visualizer_wave : R.id.audio_visualizer);
        mRecordFAB = ((FloatingActionButton) mView.findViewById(R.id.fab));
        mTimerContainer = ((LinearLayout) mView.findViewById(R.id.record_indicator_wrapper));
        mRecordingDuration = ((TextView) mView.findViewById(R.id.record_duration));
        mRecordingIndicator = mView.findViewById(R.id.record_flag);

        Recorder.OnRecordingListener onRecordingListener = new Recorder.OnRecordingListener() {
            @Override
            public void onUpdate(int amplitude, int elapsed) {
                if (Build.VERSION.SDK_INT >= 18) return;
                ((AudioVisualizer) mAudioVisualizer).update(amplitude);
                mRecordingDuration.setText(PhotosTalkUtils.getDurationFormatted(elapsed));
            }

            @Override
            public void onUpdate(short[] audioData, int length, int elapsedTime) {
                if (Build.VERSION.SDK_INT < 18) return;
                ((WaveAudioVisualizer) mAudioVisualizer).update(audioData, length);
                mRecordingIndicator.setVisibility(elapsedTime % 2 == 0 ? View.VISIBLE : View.INVISIBLE);
                mRecordingDuration.setText(PhotosTalkUtils.getDurationFormatted(elapsedTime));
            }

            @Override
            public void onMaxDurationReached() {
                Notifications.showSnackbar(mWrapper, getString(R.string.recording_stopped_maximum_20_seconds));
                showRecordUI(false);
            }
        };

        MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

            }
        };

        if (Build.VERSION.SDK_INT >= 18) {
            mRecorder = new RecorderNewAPI(getActivity(), onRecordingListener, onCompletionListener);
        } else {
            mRecorder = new RecorderOldAPI(getActivity(), onRecordingListener, onCompletionListener);
        }
    }

    private void initEvents() {
        mRecordFAB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mRecorder.getState() == Recorder.RECORDING) {
                    mRecorder.stop();
                    showRecordUI(false);
                } else {
                    showRecordUI(true);
                    mRecorder.record(20);
                }
            }
        });
    }

    private void showRecordUI(boolean record) {
        if (!record) {
            mRecordFAB.animate().translationY(0).setDuration(150).start();
            mTimerContainer.animate().alpha(0.0f).setDuration(150).start();
            mAudioVisualizer.setVisibility(View.INVISIBLE);



        } else {
            mRecordingDuration.setText(PhotosTalkUtils.getDurationFormatted(0));
            mRecordFAB.animate().translationY(-MiscUtils.convertDP2Pixel(38)).setDuration(150).start();
            mTimerContainer.animate().alpha(1.0f).setDuration(150).start();
            mAudioVisualizer.setVisibility(View.VISIBLE);
        }
    }
}
