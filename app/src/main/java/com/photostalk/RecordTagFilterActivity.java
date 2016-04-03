package com.photostalk;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.photostalk.core.Communicator;
import com.photostalk.customViews.AudioVisualizer;
import com.photostalk.customViews.EffectsSlider;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Recorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by mohammed on 2/24/16.
 */
public class RecordTagFilterActivity extends AppCompatActivity {
    public static final String PHOTO_PATH = "photo_path";
    public static final String IS_LIVE = "is_live";

    private final int MODE_NOTHING = 0;
    private final int MODE_RECORD = 1;
    private final int MODE_HASH_TAG = 2;
    private final int MODE_FILTER = 3;

    private AlertDialog mProgressDialog = null;

    private ImageView mPhoto;
    private FloatingActionButton mMicFAB;
    private FloatingActionButton mHashFAB;
    private FloatingActionButton mFilterFAB;
    private FloatingActionsMenu mMenuFAB;
    private android.support.design.widget.FloatingActionButton mRecordButton;
    private AudioVisualizer mAudioVisualizer;
    private TextView mRecordDuration;
    private View mRecordFlag;
    private LinearLayout mRecordIndicatorWrapper;
    //private ImageView mPlayStopButton;
    private ImageView mUploadButton;
    private CoordinatorLayout mCoordinatorLayout;
    private BroadcastReceiver mBroadcastReceiver;

    private Recorder mRecorder;

    private EffectsSlider mEffectsSlider;
    private EditText mHashTagEditText;
    private FrameLayout mRecorderLayout;

    private int mMode = MODE_NOTHING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.record_tag_filter_activity);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        init();

    }

    @Override
    protected void onPause() {
        mRecorder.stop();
        mRecorder.stopPlaying();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mRecorder.stop();
        mRecorder.stopPlaying();
        super.onDestroy();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.LOGOUT) || intent.getAction().equals(Broadcasting.TERMINATE_CAMERA))
                    finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.TERMINATE_CAMERA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initRecorder();
        initReferences();
        initEvents();
        fill();
    }

    private void initReferences() {
        mPhoto = ((ImageView) findViewById(R.id.photo));
        mMicFAB = ((FloatingActionButton) findViewById(R.id.mic_fab));
        mHashFAB = ((FloatingActionButton) findViewById(R.id.hash_fab));
        mFilterFAB = ((FloatingActionButton) findViewById(R.id.filter_fab));
        mMenuFAB = ((FloatingActionsMenu) findViewById(R.id.menu_fab));
        mRecordButton = ((android.support.design.widget.FloatingActionButton) findViewById(R.id.record_button));
        mAudioVisualizer = ((AudioVisualizer) findViewById(R.id.audio_visualizer));
        mRecordDuration = ((TextView) findViewById(R.id.record_duration));
        mRecordFlag = findViewById(R.id.record_flag);
        mRecordIndicatorWrapper = ((LinearLayout) findViewById(R.id.record_indicator_wrapper));
        //mPlayStopButton = ((ImageView) findViewById(R.id.play_stop_button));
        mUploadButton = ((ImageView) findViewById(R.id.upload_button));
        mCoordinatorLayout = ((CoordinatorLayout) findViewById(R.id.wrapper));

        mEffectsSlider = ((EffectsSlider) findViewById(R.id.effect_slider));
        mHashTagEditText = ((EditText) findViewById(R.id.hash_tag_edit_text));
        mRecorderLayout = ((FrameLayout) findViewById(R.id.recorder));
    }

    private void fill() {
        /*mMenuFAB.addButton(mMicFAB);
        mMenuFAB.addButton(mHashFAB);
        mMenuFAB.addButton(mFilterFAB);*/
        mAudioVisualizer.update(0);

        /**
         * setting the image of the ImageView
         * to the one in the path
         */
        mPhoto.setImageBitmap(MiscUtils.decodeFile(getIntent().getStringExtra(RecordTagFilterActivity.PHOTO_PATH)));
    }

    private void initRecorder() {
        mRecorder = new Recorder(
                this,
                /**
                 * This listener will fire
                 * when an update to the amplitude
                 * occurs
                 */
                new Recorder.OnRecordingListener() {
                    @Override
                    public void onUpdate(int amplitude) {
                        mAudioVisualizer.update(amplitude);
                    }

                    @Override
                    public void onMaxDurationReached() {
                        Notifications.showSnackbar(mCoordinatorLayout, getString(R.string.recording_stopped_maximum_20_seconds));
                        setMode(MODE_NOTHING);
                    }
                },

                /**
                 * This listener will fire when
                 * the playback finishes
                 */
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        //mPlayStopButton.setImageResource(R.drawable.play);
                    }
                });
    }

    private void initEvents() {
        mMicFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenuFAB.collapse();
                setMode(MODE_RECORD);
            }
        });

        mHashFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenuFAB.collapse();
                setMode(MODE_HASH_TAG);
            }
        });

        mFilterFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMenuFAB.collapse();
                setMode(MODE_FILTER);
            }
        });

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mRecorder.getState()) {
                    case Recorder.RECORDING:
                        mRecorder.stop();
                        setMode(MODE_NOTHING);
                        //mPlayStopButton.setVisibility(View.VISIBLE);
                        break;
                    case Recorder.NONE:
                        mRecordDuration.setText("00:00");
                        final long startTime = System.currentTimeMillis();
                        mRecordIndicatorWrapper.setVisibility(View.VISIBLE);
                        mRecordDuration.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mRecorder.getState() != Recorder.RECORDING) {
                                    mRecordIndicatorWrapper.setVisibility(View.GONE);
                                    return;
                                }
                                int seconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
                                mRecordDuration.setText("00:" + (seconds >= 10 ? "" : "0") + seconds);
                                mRecordFlag.setVisibility(seconds % 2 == 0 ? View.VISIBLE : View.INVISIBLE);
                                mRecordDuration.postDelayed(this, 1000);
                            }
                        }, 1000);
                        stopRecorder();
                        mRecorder.record(20);
                        break;
                }
            }
        });

        /*mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mRecorder.getState()) {
                    case Recorder.PLAYING:
                        mRecorder.stop();
                        mPlayStopButton.setImageResource(R.drawable.play);
                        break;
                    case Recorder.NONE:
                        stopRecorder();
                        mRecorder.play();
                        mPlayStopButton.setImageResource(R.drawable.stop);
                        break;
                }
            }
        });*/

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });
    }

    private void stopRecorder() {
        mRecorder.stop();
        mRecorder.stopPlaying();
    }

    private void setMode(int mode) {
        mMode = mode;
        switch (mode) {
            case MODE_RECORD:
                refresh(false, true, false);
                mMenuFAB.setVisibility(View.GONE);
                break;
            case MODE_HASH_TAG:
                refresh(false, false, true);
                mMenuFAB.setVisibility(View.VISIBLE);
                mHashTagEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mHashTagEditText, InputMethodManager.SHOW_FORCED);
                break;
            case MODE_FILTER:
                refresh(true, false, false);
                mMenuFAB.setVisibility(View.VISIBLE);
                break;
            default:
                refresh(false, false, false);
                mMenuFAB.setVisibility(View.VISIBLE);
        }
    }

    private void refresh(boolean showFilters, boolean showRecorder, boolean showHashTag) {
        if (!showHashTag) hideKeyboard();
        mEffectsSlider.setEditing(showFilters);
        mRecorderLayout.setVisibility(showRecorder ? View.VISIBLE : View.GONE);
        mHashTagEditText.setVisibility(showHashTag ? View.VISIBLE : View.GONE);
    }

    private String savePhoto() throws IOException {
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "PhotosTalk" + File.separator + "tmp" + File.separator + "final.jpg";
        File file = new File(fileName);
        if (file.exists()) file.delete();
        Bitmap source = ((BitmapDrawable) mPhoto.getDrawable()).getBitmap();

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap destination = Bitmap.createBitmap(source.getWidth(), source.getHeight(), conf); // this creates a MUTABLE bitmap
        ColorFilter colorFilter = new PorterDuffColorFilter(mEffectsSlider.getCurrentColor(), PorterDuff.Mode.LIGHTEN);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColorFilter(colorFilter);

        Canvas canvas = new Canvas(destination);
        canvas.drawBitmap(source, 0, 0, p);

        FileOutputStream out = new FileOutputStream(fileName);
        destination.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.close();

        return fileName;
    }

    private void upload() {

        try {
            /**
             * first save the filtered image
             */
            String fileName = savePhoto();
            String audioName = mRecorder.hasRecorded() ? mRecorder.getFileName() : null;
            String duration = (mRecorder.hasRecorded()) ? mRecorder.getDurationFormatted() : "00:00";
            String description = mHashTagEditText.getText().toString().trim();
            boolean isLive = getIntent().getBooleanExtra(RecordTagFilterActivity.IS_LIVE, false);

            Intent i = new Intent(this, PreviewPhotoActivity.class);
            i.putExtra(PreviewPhotoActivity.PHOTO_PATH, fileName);
            i.putExtra(PreviewPhotoActivity.AUDIO_PATH, audioName);
            i.putExtra(PreviewPhotoActivity.DURATION, duration);
            i.putExtra(PreviewPhotoActivity.DESCRIPTION, description);
            i.putExtra(PreviewPhotoActivity.IS_LIVE, isLive);

            startActivity(i);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (mMenuFAB.isExpanded()) {
            mMenuFAB.collapse();
        } else if (mProgressDialog != null) {
            Communicator.getInstance().cancelByTag("photo_add");
            mProgressDialog.dismiss();
            mProgressDialog = null;
            Notifications.showSnackbar(this, getString(R.string.uploading_cancelled));
        } else if (mMode != MODE_NOTHING) {
            setMode(MODE_NOTHING);
        } else
            super.onBackPressed();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mHashTagEditText.getWindowToken(), 0);
    }
}
