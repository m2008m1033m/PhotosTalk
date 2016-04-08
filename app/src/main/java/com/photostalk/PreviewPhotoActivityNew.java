package com.photostalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.photostalk.models.Model;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Player;

import java.io.File;

public class PreviewPhotoActivityNew extends AppCompatActivity {

    public static final String PHOTO_PATH = "photo_path";
    public static final String AUDIO_PATH = "audio_path";
    public static final String DESCRIPTION = "description";
    public static final String DURATION = "duration";
    public static final String IS_LIVE = "is_live";

    private SubsamplingScaleImageView mPhotoImageView;
    private LinearLayout mDetailsContainer;
    private Toolbar mToolbar;
    private ImageView mPlayStopButton;
    private ProgressBar mProgressBar;
    private TextView mLiveTextView;
    private TextView mPostButton;
    private TextView mHashTagsTextView;

    private Player mPlayer;
    private String mPhotoPath;
    private String mDescription;
    private String mAudioPath;
    private boolean mIsLive;

    private boolean mIsOverlayShown = true;
    private boolean mOverlayAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preview_photo_activity_new);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextColor(0xFFFFFFFF);
        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.mipmap.back);
        }
        setTitle(R.string.preview_photo);

        init();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        mPlayer.stop();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        mPlayer.stop();
        super.onResume();
    }

    private void init() {
        initReferences();
        fillFields();
        initEvents();
    }

    private void initReferences() {
        mDetailsContainer = (LinearLayout) findViewById(R.id.details_container);
        mPostButton = ((TextView) findViewById(R.id.post_button));
        mLiveTextView = ((TextView) findViewById(R.id.is_live_text_view));
        mHashTagsTextView = ((TextView) findViewById(R.id.hash_tags_text_view));
        mPhotoImageView = ((SubsamplingScaleImageView) findViewById(R.id.photo));
        mPlayStopButton = ((ImageView) findViewById(R.id.play_stop_button));
        mProgressBar = ((ProgressBar) findViewById(R.id.progress));
    }

    private void fillFields() {

        mPhotoPath = getIntent().getStringExtra(PHOTO_PATH);
        mDescription = getIntent().getStringExtra(DESCRIPTION);
        mIsLive = getIntent().getBooleanExtra(IS_LIVE, false);
        mAudioPath = getIntent().getStringExtra(AUDIO_PATH);

        mPlayer = new Player(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mProgressBar.setVisibility(View.GONE);
                mPlayStopButton.setImageResource(R.drawable.stop_blue);
            }
        }, new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
                // nothing
            }
        }, new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mPlayStopButton.setImageResource(R.drawable.play_blue);
            }
        }, new Runnable() {
            @Override
            public void run() {
                Notifications.showSnackbar(PreviewPhotoActivityNew.this, getString(R.string.could_not_play_the_audio));
            }
        }, new Player.OnPlayerUpdateListener() {
            @Override
            public void onUpdate(int position) {
                // nothing
            }
        });

        mLiveTextView.setVisibility(mIsLive ? View.VISIBLE : View.GONE);

        if (mAudioPath == null || mAudioPath.isEmpty())
            mPlayStopButton.setVisibility(View.GONE);

        if (mDescription != null) {
            mDescription = MiscUtils.convertStringToHashTag(mDescription);
            mHashTagsTextView.setText(mDescription);
        }

        mPhotoImageView.setImage(ImageSource.uri(mPhotoPath));

    }

    private void initEvents() {

        mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayStopButton.setImageResource(R.drawable.crystal_button);
                mProgressBar.setVisibility(View.VISIBLE);
                mPlayer.play(mAudioPath);
            }
        });

        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.stop();
                /**
                 * preparing the parameters
                 */
                File photo = new File(mPhotoPath);
                File audio = (mAudioPath != null) ? new File(mAudioPath) : null;
                String duration = getIntent().getStringExtra(DURATION);
                final AlertDialog progress = Notifications.showLoadingDialog(PreviewPhotoActivityNew.this, getString(R.string.uploading));
                PhotosApi.add(photo, audio, duration, mDescription, mIsLive ? "1" : "0", new ApiListeners.OnItemLoadedListener() {
                    @Override
                    public void onLoaded(Result result, Model item) {
                        progress.dismiss();
                        if (result.isSucceeded()) {
                            Intent i = new Intent(PreviewPhotoActivityNew.this, PhotoActivity.class);
                            i.putExtra(PhotoActivity.PHOTO_ID, item.getId());
                            startActivity(i);
                            Broadcasting.sendTerminatCamera(PreviewPhotoActivityNew.this);
                            finish();
                            Log.d("RecordTagFilterActivity", "photo id: " + item.getId());

                        } else {
                            Notifications.showListAlertDialog(PreviewPhotoActivityNew.this, getString(R.string.error), result.getMessages());
                        }
                    }
                });
            }
        });

        mPhotoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOverlayAnimating) return;
                mOverlayAnimating = true;

                Animation animation = new AlphaAnimation(mIsOverlayShown ? 1.0f : 0.0f, mIsOverlayShown ? 0.0f : 1.0f);
                mToolbar.setVisibility(View.VISIBLE);
                mDetailsContainer.setVisibility(View.VISIBLE);
                animation.setDuration(150);
                animation.setFillAfter(true);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mOverlayAnimating = false;
                        mIsOverlayShown = !mIsOverlayShown;
                        if (mIsOverlayShown) return;
                        mToolbar.setVisibility(View.GONE);
                        mDetailsContainer.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                mToolbar.startAnimation(animation);
                mDetailsContainer.startAnimation(animation);
            }
        });

    }
}
