package com.photostalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.photostalk.models.Model;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Player;

import java.io.File;

/**
 * Created by mohammed on 3/20/16.
 */
public class PreviewPhotoActivity extends AppCompatActivity {

    public static final String PHOTO_PATH = "photo_path";
    public static final String AUDIO_PATH = "audio_path";
    public static final String DESCRIPTION = "description";
    public static final String DURATION = "duration";
    public static final String IS_LIVE = "is_live";


    private RelativeLayout mHeader;
    private TextView mIsLiveTextView;
    private ImageView mPhoto;
    private ImageView mPlayStopButton;
    private ProgressBar mProgressBar;
    private TextView mLikesTextView;
    private ImageButton mLikeButton;
    private ImageButton mShareButton;
    private ImageView mCommentIcon;
    private TextView mCommentTextView;
    private ImageButton mMicButton;
    private Button mPostButton;
    private TextView mDescriptionTextView;

    private Player mPlayer;
    private String mPhotoPath;
    private String mDescription;
    private String mAudioPath;

    private boolean mIsLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.preview_photo);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.preview_photo_activity);
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
        disableUnnecessaryUI();
        fillFields();
        initEvents();
    }

    private void initReferences() {
        mHeader = ((RelativeLayout) findViewById(R.id.header));
        mIsLiveTextView = ((TextView) findViewById(R.id.is_live_text_view));
        mPhoto = ((ImageView) findViewById(R.id.photo));
        mPlayStopButton = ((ImageView) findViewById(R.id.play_stop_button));
        mProgressBar = ((ProgressBar) findViewById(R.id.progress));
        mLikesTextView = ((TextView) findViewById(R.id.likes_number_text_view));
        mLikeButton = ((ImageButton) findViewById(R.id.like_button));
        mShareButton = ((ImageButton) findViewById(R.id.share_button));
        mCommentIcon = ((ImageView) findViewById(R.id.comments_icon));
        mCommentTextView = ((TextView) findViewById(R.id.comments_number_text_view));
        mMicButton = ((ImageButton) findViewById(R.id.mic_button));
        mPostButton = ((Button) findViewById(R.id.post_button));
        mDescriptionTextView = ((TextView) findViewById(R.id.hash_tags_text_view));
    }

    private void disableUnnecessaryUI() {
        mHeader.setVisibility(View.GONE);
        disableUI(mLikeButton);
        disableUI(mShareButton);
        disableUI(mCommentIcon);
        disableUI(mMicButton);

        mLikesTextView.setTextColor(Color.rgb(127, 127, 127));
        mCommentTextView.setTextColor(Color.rgb(127, 127, 127));
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
                Notifications.showSnackbar(PreviewPhotoActivity.this, getString(R.string.could_not_play_the_audio));
            }
        }, new Player.OnPlayerUpdateListener() {
            @Override
            public void onUpdate(int position) {
                // nothing
            }
        });

        mIsLiveTextView.setVisibility(mIsLive ? View.VISIBLE : View.GONE);

        if (mAudioPath == null || mAudioPath.isEmpty())
            mPlayStopButton.setVisibility(View.GONE);

        if (mDescription != null) {
            mDescription = MiscUtils.convertStringToHashTag(mDescription);
            mDescriptionTextView.setText(mDescription);
        }

        Glide.with(this)
                .load(new File(mPhotoPath))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(mPhoto);
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
                final AlertDialog progress = Notifications.showLoadingDialog(PreviewPhotoActivity.this, getString(R.string.uploading));
                PhotosApi.add(photo, audio, duration, mDescription, mIsLive ? "1" : "0", new ApiListeners.OnItemLoadedListener() {
                    @Override
                    public void onLoaded(Result result, Model item) {
                        progress.dismiss();
                        if (result.isSucceeded()) {
                            Intent i = new Intent(PreviewPhotoActivity.this, PhotoActivity.class);
                            i.putExtra(PhotoActivity.PHOTO_ID, item.getId());
                            startActivity(i);
                            Broadcasting.sendTerminatCamera(PreviewPhotoActivity.this);
                            finish();
                            Log.d("RecordTagFilterActivity", "photo id: " + item.getId());

                        } else {
                            Notifications.showListAlertDialog(PreviewPhotoActivity.this, getString(R.string.error), result.getMessages());
                        }
                    }
                });
            }
        });

    }

    private void disableUI(ImageView button) {
        button.setClickable(false);
        button.setEnabled(false);
        button.setColorFilter(Color.argb(255, 127, 127, 127));
    }
}
