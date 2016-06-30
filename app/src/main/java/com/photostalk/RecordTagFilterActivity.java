package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.photostalk.customListeners.OnSwipeTouchListener;
import com.photostalk.customViews.AudioItemSliderView;
import com.photostalk.customViews.AudioVisualizer;
import com.photostalk.customViews.WaveAudioVisualizer;
import com.photostalk.filters.IFAmaroFilter;
import com.photostalk.filters.IFEarlybirdFilter;
import com.photostalk.filters.IFXprollFilter;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.PhotosTalkUtils;
import com.photostalk.utils.Player;
import com.photostalk.utils.recorder.Recorder;
import com.photostalk.utils.recorder.RecorderNewAPI;
import com.photostalk.utils.recorder.RecorderOldAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSepiaFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 * Created by mohammed on 5/30/16.
 */
public class RecordTagFilterActivity extends LoggedInActivity {

    public static final String PHOTO_PATH = "photo_path";
    public static final String IS_LIVE = "is_live";

    private GPUImageView mGPUImageView;
    private FrameLayout mHashtagContainer;
    private FrameLayout mRecordContainer;

    /**
     * fields for the filter
     */
    private final int TOTAL_FILTERS = 7;
    private int mCurrentFilter = 0;

    /**
     * fields for the hashtag
     */
    private EditText mEditText;

    /**
     * fields for the recorder
     */
    private LinearLayout mTimerContainer;
    private TextView mRecordingDuration;
    private View mRecordingIndicator;
    private View mAudioVisualizer;
    private AudioItemSliderView mAudioItemSliderView;
    private FloatingActionButton mRecordFAB;
    private ImageButton mPlayStopButton;
    private Recorder mRecorder;
    private Player mPlayer;
    private String mAudioPath;
    private Button mPostButton;
    private int mPreviousPosition;

    private BroadcastReceiver mBroadcastReceiver;

    private ArrayList<String> mPredefinedAudio = new ArrayList<>();

    private String mPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.record_tag_filter_activity_new);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(0xFFFFFFFF);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.mipmap.back);
            ab.setTitle(R.string.edit_photo);
        }

        init();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mPlayer.stop();
        mRecorder.stop();
        super.onDestroy();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.LOGOUT) || intent.getAction().equals(Broadcasting.PHOTO_POSTED))
                    finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.PHOTO_POSTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record_filter_hashtag_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.hashtag:
                mPlayer.stop();
                mPlayStopButton.setImageResource(R.drawable.play);
                mHashtagContainer.setVisibility(View.VISIBLE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mRecordContainer.getVisibility() == View.VISIBLE && mRecorder.getState() == Recorder.NONE) {
            mRecordContainer.setVisibility(View.GONE);

            // check if previous selection was the recording, if so don't navigate to it again
            // as navigating to it will display the container again.
            if (mPreviousPosition == 1) return;
            mAudioItemSliderView.setSelectedItem(mPreviousPosition);
        } else if (mHashtagContainer.getVisibility() == View.VISIBLE)
            mHashtagContainer.setVisibility(View.GONE);
        else
            super.onBackPressed();
    }

    private void init() {
        initReferences();
        fillFields();
        initEvents();
    }

    private void initReferences() {
        mGPUImageView = ((GPUImageView) findViewById(R.id.photo));
        mHashtagContainer = ((FrameLayout) findViewById(R.id.hashtag_container));
        mEditText = ((EditText) findViewById(R.id.hash_tag_edit_text));
        mAudioVisualizer = findViewById(Build.VERSION.SDK_INT >= 18 ? R.id.audio_visualizer_wave : R.id.audio_visualizer);
        mTimerContainer = ((LinearLayout) findViewById(R.id.record_indicator_wrapper));
        mRecordingDuration = ((TextView) findViewById(R.id.record_duration));
        mRecordingIndicator = findViewById(R.id.record_flag);
        mAudioItemSliderView = ((AudioItemSliderView) findViewById(R.id.audio_item_slider));
        mRecordFAB = ((FloatingActionButton) findViewById(R.id.fab));
        mRecordContainer = ((FrameLayout) findViewById(R.id.recorder));
        mPlayStopButton = ((ImageButton) findViewById(R.id.play_stop_button));
        mPostButton = ((Button) findViewById(R.id.post_button));

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
                Notifications.showSnackbar(RecordTagFilterActivity.this, getString(R.string.recording_stopped_maximum_20_seconds));
                showRecordUI(false);
            }
        };

        MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

            }
        };

        if (Build.VERSION.SDK_INT >= 18) {
            mRecorder = new RecorderNewAPI(this, onRecordingListener, onCompletionListener);
        } else {
            mRecorder = new RecorderOldAPI(this, onRecordingListener, onCompletionListener);
        }

        mPlayer = new Player(null, null, new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mPlayStopButton.setImageResource(R.drawable.play);
            }
        }, new Runnable() {
            @Override
            public void run() {
                Notifications.showSnackbar(RecordTagFilterActivity.this, getString(R.string.an_error_occurred_while_playing_audio));
            }
        }, null);
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

    private void fillFields() {
        mPhotoPath = getIntent().getStringExtra(PHOTO_PATH);

        mGPUImageView.setImage(new File(mPhotoPath));
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

        setupAudiItemSlider();
    }

    private void initEvents() {
        initFilterEvents();
        initRecordEvents();

        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MiscUtils.showKeyboard(false, mEditText);

                String fileName = saveFilteredPhoto();
                String duration = (mRecorder.hasRecorded()) ? mRecorder.getDurationFormatted() : "00:00";
                String description = mEditText.getText().toString().trim();
                boolean isLive = getIntent().getBooleanExtra(IS_LIVE, false);

                Intent i = new Intent(RecordTagFilterActivity.this, PreviewPhotoActivity.class);
                i.putExtra(PreviewPhotoActivity.PHOTO_PATH, fileName);
                i.putExtra(PreviewPhotoActivity.AUDIO_PATH, mAudioPath);
                i.putExtra(PreviewPhotoActivity.DURATION, duration);
                i.putExtra(PreviewPhotoActivity.DESCRIPTION, description);
                i.putExtra(PreviewPhotoActivity.IS_LIVE, isLive);

                startActivity(i);
            }
        });

    }

    /**
     * METHODS RELATED TO THE AUDIO s
     */
    private void setupAudiItemSlider() {

        mPredefinedAudio = new ArrayList<>();
        mPredefinedAudio.add("applause");
        mPredefinedAudio.add("laughing");
        mPredefinedAudio.add("kiss");
        mPredefinedAudio.add("iloveit");
        mPredefinedAudio.add("angry");
        mPredefinedAudio.add("yawn");

        /**
         * check if assets audio are saved to cache
         */
        moveAssetsToCache();

        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.none));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.recording));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.applause));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.laughing));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.kiss));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.iloveit));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.angry));
        mAudioItemSliderView.addAudioItem(new AudioItemSliderView.AudioItem(R.drawable.yawn));

        mAudioItemSliderView.setOnItemClickedListener(new AudioItemSliderView.OnItemClickedListener() {
            @Override
            public void onClicked(int previousPosition, int position) {
                mPlayer.stop();
                mPlayStopButton.setImageResource(R.drawable.play);

                mPreviousPosition = previousPosition == -1 ? 0 : previousPosition;
                switch (position) {
                    // none
                    case 0:
                        mAudioPath = null;
                        refreshPlayButton();
                        break;
                    // recording
                    case 1:
                        mRecordContainer.setVisibility(View.VISIBLE);
                        break;
                    default:
                        mAudioPath = getCacheDir() + "/assets/" + mPredefinedAudio.get(position - 2) + ".mp3";
                        refreshPlayButton();
                }
            }
        });

    }

    private void moveAssetsToCache() {
        AssetManager assetManager = getAssets();
        InputStream in;
        OutputStream out;
        byte[] buffer = new byte[1024];

        File assetDir = new File(getCacheDir() + "/assets/");
        if (!assetDir.exists()) {
            assetDir.mkdir();
        }

        for (String name : mPredefinedAudio) {
            File f = new File(getCacheDir() + "/assets/" + name + ".mp3");
            if (f.exists()) continue;

            try {
                // save it if it is not there:
                in = assetManager.open(name + ".mp3");
                out = new FileOutputStream(new File(getCacheDir() + "/assets/" + name + ".mp3"));
                int readBytes;
                while ((readBytes = in.read(buffer)) != -1) {
                    out.write(buffer, 0, readBytes);
                }
                out.flush();
                out.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void initRecordEvents() {
        mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayer.isPlaying()) {
                    mPlayer.stop();
                    mPlayStopButton.setImageResource(R.drawable.play);
                } else {
                    mPlayer.play(mAudioPath);
                    mPlayStopButton.setImageResource(R.drawable.stop);
                }
            }
        });

        mRecordFAB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mRecorder.getState() == Recorder.RECORDING) {
                    mRecorder.stop();
                    showRecordUI(false);
                    mRecordContainer.setVisibility(View.GONE);
                    mAudioPath = mRecorder.getFileNameIfRecorded();
                    refreshPlayButton();
                } else {
                    showRecordUI(true);
                    mRecorder.record(20);
                }
            }
        });
    }

    private void refreshPlayButton() {
        mPlayStopButton.setVisibility(mAudioPath == null ? View.GONE : View.VISIBLE);
    }

    /**
     * METHODS RELATED TO THE FILTERING
     */
    private void initFilterEvents() {
        mGPUImageView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeTop() {
                if (mCurrentFilter == 0)
                    mCurrentFilter = TOTAL_FILTERS - 1;
                else
                    mCurrentFilter--;

                changeFilter();
            }

            @Override
            public void onSwipeBottom() {
                if (mCurrentFilter + 1 == TOTAL_FILTERS)
                    mCurrentFilter = 0;
                else
                    mCurrentFilter++;
                changeFilter();
            }
        });
    }

    private void changeFilter() {
        switch (mCurrentFilter) {
            case 0:
                mGPUImageView.setFilter(normal());
                break;
            case 1:
                mGPUImageView.setFilter(greyScale());
                break;
            case 2:
                mGPUImageView.setFilter(sepia());
                break;
            case 3:
                mGPUImageView.setFilter(loFi());
                break;
            case 4:
                mGPUImageView.setFilter(amaro());
                break;
            case 5:
                mGPUImageView.setFilter(earlyBird());
                break;
            case 6:
                mGPUImageView.setFilter(xProII());
                break;
        }
    }

    public String saveFilteredPhoto() {
        String fileName = getCacheDir().toString() + "/filtered.jpg";
        try {
            Bitmap bitmap = mGPUImageView.getGPUImage().getBitmapWithFilterApplied();
            FileOutputStream out = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private GPUImageFilter normal() {
        return new GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, GPUImageFilter.NO_FILTER_FRAGMENT_SHADER);
    }

    private GPUImageFilter greyScale() {
        return new GPUImageGrayscaleFilter();
    }

    private GPUImageFilter sepia() {
        return new GPUImageSepiaFilter();
    }

    private GPUImageFilter loFi() {
        GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
        filterGroup.addFilter(new GPUImageBrightnessFilter(0.2f));
        filterGroup.addFilter(new GPUImageContrastFilter(2.3f));
        return filterGroup;
    }

    private GPUImageFilter amaro() {
        return new IFAmaroFilter(this);
    }

    private GPUImageFilter earlyBird() {
        return new IFEarlybirdFilter(this);
    }

    private GPUImageFilter xProII() {
        return new IFXprollFilter(this);
    }


    /**
     * METHODS RELATED TO THE HASHTAG
     */


}
