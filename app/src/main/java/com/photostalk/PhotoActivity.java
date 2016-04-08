package com.photostalk;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.photostalk.adapters.PhotoActivityAdapterNew;
import com.photostalk.core.Communicator;
import com.photostalk.customViews.AudioVisualizer;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.models.Comment;
import com.photostalk.models.Model;
import com.photostalk.models.Photo;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Player;
import com.photostalk.utils.Recorder;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhotoActivity extends AppCompatActivity {

    public final static String PHOTO_ID = "photo_id";
    private final static int REQUEST_AUDIO_PERMISSION = 0;

    private final static int HEADER = 0;
    private final static int COMMENT = 1;
    private final static int NONE = 2;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;
    private LinearLayout mDetailsContainer;
    private Toolbar mToolbar;
    private TextView mShowCommentsButton;
    private TextView mLikesCount;
    private TextView mLikeButton;
    private TextView mRecordComment;
    private TextView mShareButton;
    private TextView mLiveTextView;
    private TextView mHashTagsTextView;
    private SubsamplingScaleImageView mPhotoImageView;
    private ImageView mPlayStopButton;
    private ImageButton mMicButton;
    private ProgressBar mProgressBar;
    private FrameLayout mRecorderContainer;
    private AudioVisualizer mAudioVisualizer;
    private TextView mRecordDuration;
    private View mRecordFlag;
    private LinearLayout mRecordIndicatorWrapper;
    private ImageView mCancelRecordingButton;
    private FloatingActionButton mRecordButton;
    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private TextView mCommentsLayoutCommentCount;
    private AlertDialog mProgressDialog;

    private PhotoActivityAdapterNew mAdapter;
    private PhotoActivityAdapterNew.OnActionListener mOnActionListener;

    private BroadcastReceiver mBroadcastReceiver;
    private Recorder mRecorder;
    private boolean mArePermissionsGranted = false;

    private Player mPlayer;
    private Photo mPhoto;
    private int mPlayingComment = -1;
    private boolean mIsHeaderPlaying = false;
    private boolean mIsOverlayShown = true;
    private boolean mOverlayAnimating = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * load the state of the audio permission
         */
        mArePermissionsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        setContentView(R.layout.photo_activity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.mipmap.back);
        }
        setTitle("");

        setupBroadcastReceiver();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.delete:
                playHeader(false);
                PhotosApi.delete(mPhoto.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            Broadcasting.sendPhotoDelete(PhotoActivity.this, mPhoto.getId());
                            finish();
                        } else
                            Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
            case R.id.report:
                PhotosApi.report(mPhoto.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded())
                            Notifications.showSnackbar(PhotoActivity.this, getString(R.string.a_report_has_been_sent_successfully));
                        else
                            Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mProgressDialog != null) {
            Communicator.getInstance().cancelAll();
            mProgressDialog.dismiss();
            mProgressDialog = null;
        } else if (stopRecorder()) {
            return;
        } else if (mIsHeaderPlaying || mPlayingComment != -1) {
            playHeader(false);
            playComment(-1);
        } else if (areCommentsShown()) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playComment(-1);
        playHeader(false);
        stopRecorder();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.PHOTO_DELETE)) {
                    String photoId = intent.getStringExtra("photo_id");
                    if (!photoId.equals(mPhoto.getId())) return;
                    finish();
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String storyId = intent.getStringExtra("story_id");
                    if (mPhoto.getStory().getId().equals(storyId))
                        finish();
                } else if (intent.getAction().equals(Broadcasting.LOGOUT)) {
                    finish();
                }
            }
        };


        IntentFilter intentFilter = new IntentFilter(Broadcasting.PHOTO_DELETE);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        intentFilter.addAction(Broadcasting.LOGOUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initReferences();
        initEvents();
        fillFields();
    }

    private void initReferences() {
        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mDetailsContainer = (LinearLayout) findViewById(R.id.details_container);
        mShowCommentsButton = ((TextView) findViewById(R.id.comments_number_text_view));
        mLikeButton = ((TextView) findViewById(R.id.like_button));
        mLikesCount = ((TextView) findViewById(R.id.likes_number_text_view));
        mShareButton = ((TextView) findViewById(R.id.share_button));
        mLiveTextView = ((TextView) findViewById(R.id.is_live_text_view));
        mHashTagsTextView = ((TextView) findViewById(R.id.hash_tags_text_view));
        mPhotoImageView = ((SubsamplingScaleImageView) findViewById(R.id.photo));
        mPlayStopButton = ((ImageView) findViewById(R.id.play_stop_button));
        mMicButton = ((ImageButton) findViewById(R.id.mic_button));
        mProgressBar = ((ProgressBar) findViewById(R.id.progress));
        mRecordComment = ((TextView) findViewById(R.id.record_comment));
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        mCommentsLayoutCommentCount = ((TextView) findViewById(R.id.comments_layout_comments_number));
        mRecorderContainer = ((FrameLayout) findViewById(R.id.recorder));
        mAudioVisualizer = ((AudioVisualizer) findViewById(R.id.audio_visualizer));
        mRecordDuration = ((TextView) findViewById(R.id.record_duration));
        mRecordFlag = findViewById(R.id.record_flag);
        mRecordIndicatorWrapper = ((LinearLayout) findViewById(R.id.record_indicator_wrapper));
        mCancelRecordingButton = ((ImageView) findViewById(R.id.cancel_button));
        mRecordButton = ((FloatingActionButton) findViewById(R.id.record_button));
    }

    private void initEvents() {

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

        mMicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordDialog();
            }
        });

        mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playHeader(!mIsHeaderPlaying);
            }
        });

        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotosApi.like(mPhoto.getId(), !mPhoto.isLiked(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            mPhoto.setIsLiked(!mPhoto.isLiked());
                            mPhoto.setLikesCount(mPhoto.getLikesCount() + (mPhoto.isLiked() ? 1 : -1));
                            mLikeButton.setCompoundDrawablesWithIntrinsicBounds(mPhoto.isLiked() ? R.mipmap.ic_like_blue : R.mipmap.ic_empty_heart, 0, 0, 0);
                            mLikesCount.setText(getResources().getQuantityString(R.plurals.d_likes, mPhoto.getLikesCount(), mPhoto.getLikesCount()));
                        } else
                            Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());
                    }
                });
            }
        });

        mRecordComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordDialog();
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mPhoto.getShareUrl());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_to)));
            }
        });

        mPlayer = new Player(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (whatIsPlaying() == HEADER) {
                    changeHeaderUi(true, false);
                } else if (whatIsPlaying() == COMMENT && mPlayingComment != -1) {
                    mAdapter.setIsCommentLoading(false);
                    mAdapter.notifyItemChanged(mPlayingComment);
                }

            }
        }, new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {

            }
        }, new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (whatIsPlaying() == HEADER) {
                    playHeader(false);
                } else if (whatIsPlaying() == COMMENT) {
                    playComment(-1);
                }
            }
        }, new Runnable() {
            @Override
            public void run() {
                Notifications.showSnackbar(PhotoActivity.this, getString(R.string.failed_to_load_the_audio));
            }
        }, new Player.OnPlayerUpdateListener() {
            @Override
            public void onUpdate(int position) {

                AppCompatSeekBar appCompatSeekBar = mAdapter.getActiveSeekBar(mRefreshRecyclerViewFragment.getRecyclerView());
                if (appCompatSeekBar != null) {
                    appCompatSeekBar.setMax(mPlayer.getDuration() / 10);
                    appCompatSeekBar.setProgress(position / 10);
                }
            }
        });

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * if it is currently recording
                 * stop and post
                 */
                if (mRecorder.getState() == Recorder.RECORDING) {
                    mRecorder.stop();
                    submitComment();
                    mRecorderContainer.setVisibility(View.GONE);
                }
                /**
                 * start recording
                 */
                else if (mRecorder.getState() == Recorder.NONE) {
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
                    mRecorder.record(20);
                }
            }
        });

        mCancelRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecorder.cancel();
                mRecorder.clean();
                mRecorderContainer.setVisibility(View.GONE);
            }
        });

        mOnActionListener = new PhotoActivityAdapterNew.OnActionListener() {
            @Override
            public void onPlayStopComment(Comment comment, int position, boolean isStopping) {
                playComment(isStopping ? -1 : position);
            }

            @Override
            public void onSeek(int progress) {
                if (mPlayingComment == -1) return;
                mPlayer.seek(progress * 10);
            }

            @Override
            public void onDeleteCommentClicked(final int position) {
                if (!mPhoto.isAuthor()) return;
                playComment(-1);
                AlertDialog.Builder b = new AlertDialog.Builder(PhotoActivity.this);
                b.setTitle(R.string.confirm);
                b.setMessage(R.string.delete_this_comment);
                b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Comment comment = mAdapter.getItems().get(position);
                        PhotosApi.deleteComment(mPhoto.getId(), comment.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    Notifications.showSnackbar(PhotoActivity.this, getString(R.string.comment_has_been_deleted_successfully));
                                    mAdapter.getItems().remove(position);
                                    mPhoto.setCommentsCount(mPhoto.getCommentsCount() - 1);
                                    refreshCommentCount();
                                    mAdapter.notifyItemRemoved(position);
                                    if (position == 0) {
                                        mAdapter.notifyItemChanged(0);
                                    }
                                } else {
                                    Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                                }
                            }
                        });
                    }
                });

                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                b.create().show();
            }

            @Override
            public void onUserClicked(String userId) {
                Intent i = new Intent(PhotoActivity.this, ProfileActivity.class);
                i.putExtra(ProfileActivity.USER_ID, userId);
                startActivity(i);
            }


        };
    }

    private int whatIsPlaying() {
        if (mIsHeaderPlaying) return HEADER;
        if (mPlayingComment != -1) return COMMENT;
        return NONE;
    }

    private void playHeader(boolean play) {
        if (play == mIsHeaderPlaying) return;

        if (play) {
            playComment(-1);
            mPlayer.play(mPhoto.getAudioUrl());
        } else {
            mPlayer.stop();
        }

        changeHeaderUi(play, play);
        mIsHeaderPlaying = play;

    }

    private void playComment(int position) {
        /**
         * if the comment currently playing is the
         * same as the one requested, just return
         */
        if (position == mPlayingComment) return;

        /**
         * if play a comment indicated by sending a position
         * that is not equal to -1
         */
        if (position != -1) {
            // stop the header audio
            playHeader(false);

            // stop any other playing comment
            playComment(-1);

            // label the comment as loading
            mAdapter.setIsCommentLoading(true);

            // set the playing comment in the adapter
            mAdapter.setPlayingComments(position);

            // play the comment using the Player object
            mPlayer.play(mAdapter.getItems().get(position).getAudioUrl());
        }

        /**
         * if the position is -1, i.e. stop any playing comment
         */
        else {
            // shut off the loading indicator
            mAdapter.setIsCommentLoading(false);

            // set the playing comment in the adapter as no one (-1)
            mAdapter.setPlayingComments(-1);

            // stop the audio using the Player.stop() method.
            mPlayer.stop();
        }

        //changeCommentUi(position != -1, position != -1 ? position : mPlayingComment);

        // update which comment is playing now
        mPlayingComment = position;
    }

    private boolean stopRecorder() {
        if (mRecorderContainer != null && mRecorderContainer.getVisibility() == View.VISIBLE) {
            if (mRecorder.getState() == Recorder.RECORDING)
                mRecorder.cancel();
            mRecorderContainer.setVisibility(View.GONE);
            return true;
        }

        return false;
    }

    private void changeHeaderUi(boolean play, boolean loading) {
        if (play && loading) {
            mPlayStopButton.setImageResource(R.drawable.crystal_button);
            mProgressBar.setVisibility(View.VISIBLE);
        } else if (play) {
            mPlayStopButton.setImageResource(R.drawable.stop_blue);
            mProgressBar.setVisibility(View.GONE);
        } else {
            mPlayStopButton.setImageResource(R.drawable.play_blue);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void fillFields() {

        /**
         * connect the comments number textview with the
         * sliding up pane
         */
        mSlidingUpPanelLayout.setDragView(mShowCommentsButton);

        mCancelRecordingButton.setVisibility(View.VISIBLE);

        mAdapter = new PhotoActivityAdapterNew(mOnActionListener);
        mRefreshRecyclerViewFragment.setAdapter(mAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                PhotosApi.comments(
                        mPhoto.getId(),
                        mRefreshRecyclerViewFragment.getAppender(),
                        mRefreshRecyclerViewFragment.getMaxId(),
                        mRefreshRecyclerViewFragment.getSinceId());
            }
        });


        /**
         * initialize the recorder instance
         */
        mRecorder = new Recorder(PhotoActivity.this, new Recorder.OnRecordingListener() {
            @Override
            public void onUpdate(int amplitude) {
                mAudioVisualizer.update(amplitude);
            }

            @Override
            public void onMaxDurationReached() {
                Notifications.showSnackbar(PhotoActivity.this, getString(R.string.recording_stopped_maximum_20_seconds));
                mRecorderContainer.setVisibility(View.GONE);
                mRecorder.cancel();
            }
        }, null);

        /**
         * first get the photo from the internet
         */
        final String photoId = getIntent().getStringExtra(PHOTO_ID);//"56d5ba43db66470c2681ebd9";//

        mProgressDialog = Notifications.showLoadingDialog(this, getString(R.string.loading));
        PhotosApi.get(photoId, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {
                    mPhoto = (Photo) item;

                    /**
                     * fill the photos fields
                     */
                    /**
                     * the actual photo
                     */
                    loadPhoto();


                    mLiveTextView.setVisibility(mPhoto.isLive() ? View.VISIBLE : View.GONE);
                    mHashTagsTextView.setText(mPhoto.getHashTagsConcatenated());
                    mLikesCount.setText(getResources().getQuantityString(R.plurals.d_likes, mPhoto.getLikesCount(), mPhoto.getLikesCount()));
                    refreshCommentCount();
                    mLikeButton.setCompoundDrawablesWithIntrinsicBounds(mPhoto.isLiked() ? R.mipmap.ic_like_blue : R.mipmap.ic_empty_heart, 0, 0, 0);

                    mPlayStopButton.setVisibility(mPhoto.getAudioUrl().isEmpty() ? View.GONE : View.VISIBLE);

                    /**
                     * load the comments
                     */
                    mRefreshRecyclerViewFragment.refreshItems(null, null);

                } else {
                    Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());

                }

                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        });


    }

    private void submitComment() {
        final AlertDialog progressBar = Notifications.showLoadingDialog(this, getString(R.string.posting_comment));
        PhotosApi.comment(new File(mRecorder.getFileName()), mPhoto.getId(), mRecorder.getDurationFormatted(), new ApiListeners.OnActionExecutedListener() {
            @Override
            public void onExecuted(Result result) {
                progressBar.dismiss();
                /**
                 * remove the file after uploading
                 */
                mRecorder.clean();
                if (result.isSucceeded()) {
                    mRefreshRecyclerViewFragment.refreshItems(mRefreshRecyclerViewFragment.getFirstItemId(), null);
                    mPhoto.setCommentsCount(mPhoto.getCommentsCount() + 1);
                    refreshCommentCount();
                    if (areCommentsShown()) return;
                    mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else {
                    Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());
                }
            }
        });
    }

    private void showRecordDialog() {
        /**
         * first check for the recording permission
         */
        if (!mArePermissionsGranted) {
            ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            mRecorderContainer.setVisibility(View.VISIBLE);
        }
    }

    private boolean areCommentsShown() {
        return mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
    }

    private void loadPhoto() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String filename = "photo.jpg";
                    URL photoURL = new URL(mPhoto.getImageUrl());
                    HttpURLConnection connection = (HttpURLConnection) photoURL.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream is = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    FileOutputStream fileOutputStream = new FileOutputStream(getCacheDir() + filename);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                    return filename;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String filename) {
                super.onPostExecute(filename);
                if (filename == null) return;
                mPhotoImageView.setImage(ImageSource.uri(getCacheDir() + filename));
                mPhotoImageView.setMinimumDpi(10);
            }
        }.execute();

    }

    private void refreshCommentCount() {
        mShowCommentsButton.setText(getResources().getQuantityString(R.plurals.d_comments, mPhoto.getCommentsCount(), mPhoto.getCommentsCount()));
        mCommentsLayoutCommentCount.setText(getResources().getQuantityString(R.plurals.d_people_commented_on_this, mPhoto.getCommentsCount(), mPhoto.getCommentsCount()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mArePermissionsGranted = true;
                mRecorderContainer.setVisibility(View.VISIBLE);
            } else {
                Notifications.showSnackbar(this, getString(R.string.you_should_grant_all_requested_comments_to_post_a_comment));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
