package com.photostalk;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.photostalk.adapters.PhotosActivityAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
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

import java.io.File;

/**
 * Created by mohammed on 3/4/16.
 */
public class PhotoActivity extends AppCompatActivity {

    public final static String PHOTO_ID = "photo_id";
    private final static int REQUEST_AUDIO_PERMISSION = 0;

    private final static int HEADER = 0;
    private final static int COMMENT = 1;
    private final static int NONE = 2;

    private FrameLayout mRecorderContainer;
    private AudioVisualizer mAudioVisualizer;
    private TextView mRecordDuration;
    private View mRecordFlag;
    private LinearLayout mRecordIndicatorWrapper;
    private ImageView mCancelRecordingButton;
    private FloatingActionButton mRecordButton;
    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private AlertDialog mProgressDialog;
    private PopupMenu mPopupMenu;

    private PhotosActivityAdapter mAdapter;
    private PhotosActivityAdapter.OnActionListener mOnActionListener;

    private BroadcastReceiver mBroadcastReceiver;

    private Recorder mRecorder;

    private boolean mArePermissionsGranted = false;

    /**
     * for the player
     */
    private Player mPlayer;
    private Photo mPhoto;
    private int mPlayingComment = -1;
    private boolean mIsHeaderPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * load the state of the audio permission
         */
        mArePermissionsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        setContentView(R.layout.photos_activity);
        setupBroadcastReceiver();
        init();
    }

    @Override
    public void onBackPressed() {
        if (mProgressDialog != null) {
            Communicator.getInstance().cancelAll();
            mProgressDialog.dismiss();
            mProgressDialog = null;
        } else if (mRecorderContainer != null && mRecorderContainer.getVisibility() == View.VISIBLE) {
            if (mRecorder.getState() == Recorder.RECORDING)
                mRecorder.cancel();
            mRecorderContainer.setVisibility(View.GONE);
            return;
        } else if (mIsHeaderPlaying || mPlayingComment != -1) {
            playHeader(false);
            playComment(-1);
        }
        super.onBackPressed();
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
                } else if (intent.getAction().equals(Broadcasting.PROFILE_UPDATED)) {
                    if (!User.getInstance().getId().equals(mPhoto.getUser().getId())) return;
                    mAdapter.updateUserPhoto();
                }
            }
        };


        IntentFilter intentFilter = new IntentFilter(Broadcasting.PHOTO_DELETE);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        intentFilter.addAction(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initReferences();
        initEvents();
        fillFields();
    }

    private void initReferences() {
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        mRecorderContainer = ((FrameLayout) findViewById(R.id.recorder));
        mAudioVisualizer = ((AudioVisualizer) findViewById(R.id.audio_visualizer));
        mRecordDuration = ((TextView) findViewById(R.id.record_duration));
        mRecordFlag = findViewById(R.id.record_flag);
        mRecordIndicatorWrapper = ((LinearLayout) findViewById(R.id.record_indicator_wrapper));
        mCancelRecordingButton = ((ImageView) findViewById(R.id.cancel_button));
        mRecordButton = ((FloatingActionButton) findViewById(R.id.record_button));
    }

    private void initEvents() {
        mPlayer = new Player(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (whatIsPlaying() == HEADER) {
                    mAdapter.setHeaderState(PhotosActivityAdapter.PLAY);
                } else if (whatIsPlaying() == COMMENT && mPlayingComment != -1) {
                    mAdapter.setIsCommentLoading(false);
                    View v = getItemAt(mPlayingComment + 1);
                    if (v != null) {
                        v.findViewById(R.id.progress).setVisibility(View.GONE);
                        v.findViewById(R.id.button_icon).setVisibility(View.VISIBLE);
                    }
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
                /*if (mCommentSeekBar != null)
                    mCommentSeekBar.setProgress(position / 10);*/
                AppCompatSeekBar appCompatSeekBar = mAdapter.getActiveSeekbar(mRefreshRecyclerViewFragment.getRecyclerView());
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

        mOnActionListener = new PhotosActivityAdapter.OnActionListener() {
            @Override
            public void onBackButtonClicked() {
                onBackPressed();
            }

            @Override
            public void onMenuButtonClicked(View v) {
                if (mPopupMenu == null) {
                    mPopupMenu = new PopupMenu(PhotoActivity.this, v);
                    mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
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
                                    break;
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
                                    break;
                            }
                            return false;
                        }
                    });

                    MenuInflater inflater = mPopupMenu.getMenuInflater();
                    inflater.inflate(R.menu.photo_actions, mPopupMenu.getMenu());
                    if (!mPhoto.isAuthor()) mPopupMenu.getMenu().getItem(0).setVisible(false);
                }
                mPopupMenu.show();
            }

            @Override
            public void onLikeButtonClicked() {
                if (mPhoto == null) return;
                //TODO: send a like
                PhotosApi.like(mPhoto.getId(), !mPhoto.isLiked(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            mPhoto.setIsLiked(!mPhoto.isLiked());
                            mPhoto.setLikesCount(mPhoto.getLikesCount() + (mPhoto.isLiked() ? 1 : -1));
                            mAdapter.notifyDataSetChanged();
                        } else
                            Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());
                    }
                });
            }

            @Override
            public void onShareButtonClicked() {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mPhoto.getShareUrl());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_to)));
            }

            @Override
            public void onRecordButtonClicked() {
                /**
                 * first check for the recording permission
                 */
                if (!mArePermissionsGranted) {
                    ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION);
                } else {
                    mRecorderContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPlayButtonClicked() {
                playHeader(!mIsHeaderPlaying);
            }

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
                        Comment comment = mPhoto.getComments().get(position);
                        PhotosApi.deleteComment(mPhoto.getId(), comment.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    Notifications.showSnackbar(PhotoActivity.this, getString(R.string.comment_has_been_deleted_successfully));
                                    mPhoto.getComments().remove(position);
                                    mAdapter.notifyItemRemoved(position);
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

    private void fillFields() {

        mCancelRecordingButton.setVisibility(View.VISIBLE);

        mAdapter = new PhotosActivityAdapter(mOnActionListener);
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
                    mAdapter.setPhoto(mPhoto);

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
                    mAdapter.notifyDataSetChanged();
                } else {
                    Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());
                }
            }
        });
    }

    private void playHeader(boolean play) {
        if (play == mIsHeaderPlaying) return;

        if (play) {
            playComment(-1);
            mPlayer.play(mPhoto.getAudioUrl());
        } else {
            mPlayer.stop();
        }

        changeHeaderUi(play);
        mIsHeaderPlaying = play;

    }

    private void playComment(int position) {
        if (position == mPlayingComment) return;

        if (position != -1) {
            playHeader(false);
            playComment(-1);
            mAdapter.setIsCommentLoading(true);
            mAdapter.setPlayingComments(position);
            mPlayer.play(mPhoto.getComments().get(position).getAudioUrl());
        } else {
            mAdapter.setIsCommentLoading(false);
            mAdapter.setPlayingComments(-1);
            mPlayer.stop();
        }

        changeCommentUi(position != -1, position != -1 ? position : mPlayingComment);
        mPlayingComment = position;
    }

    private int whatIsPlaying() {
        if (mIsHeaderPlaying) return HEADER;
        if (mPlayingComment != -1) return COMMENT;
        return NONE;
    }

    private void changeHeaderUi(boolean play) {
        mAdapter.setHeaderState(play ? PhotosActivityAdapter.PLAY : PhotosActivityAdapter.STOP);
    }

    private void changeCommentUi(boolean play, int position) {
        position++;
        /**
         * get the visible position of the item
         * above the desired one
         */
        View aboveIt = getItemAt(position - 1);


        /**
         * get the visible position of the item
         */
        View v = getItemAt(position);

        /**
         * change the ui of the item
         */
        if (v != null) {
            v.findViewById(R.id.container).setBackgroundResource(play ? R.color.light : R.color.white);
            ((TextView) v.findViewById(R.id.full_name)).setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), play ? R.color.main : R.color.black));
            ((ImageView) v.findViewById(R.id.button_icon)).setImageResource(play ? R.drawable.stop : R.drawable.play);
            v.findViewById(R.id.button_icon).setVisibility(play ? View.GONE : View.VISIBLE);
            v.findViewById(R.id.progress).setVisibility(play ? View.VISIBLE : View.GONE);
            AppCompatSeekBar appCompatSeekBar = ((AppCompatSeekBar) v.findViewById(R.id.seek_bar));
            appCompatSeekBar.setVisibility(play ? View.VISIBLE : View.GONE);
            appCompatSeekBar.setProgress(0);
        }

        /**
         * change the ui of the previous item
         */
        if (aboveIt != null)
            if (position != 1)
                aboveIt = aboveIt.findViewById(R.id.wrapper);
            else
                aboveIt = aboveIt.findViewById(R.id.header_of_first_comment);
        if (aboveIt != null)
            aboveIt.setBackgroundResource(play ? R.color.light : R.color.white);
    }

    private View getItemAt(int position) {
        position = position -
                ((LinearLayoutManager) mRefreshRecyclerViewFragment.getRecyclerView().getLayoutManager()).findFirstVisibleItemPosition();
        return mRefreshRecyclerViewFragment.getRecyclerView().getChildAt(position);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mArePermissionsGranted = true;
                mRecorderContainer.setVisibility(View.VISIBLE);
            } else {
                Notifications.showSnackbar(this, getString(R.string.you_should_grant_all_requested_comments_to_post_a_comment));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
