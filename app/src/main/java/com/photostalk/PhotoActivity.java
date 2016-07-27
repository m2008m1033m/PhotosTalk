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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.photostalk.adapters.PhotoActivityAdapter;
import com.photostalk.apis.PhotosApi;
import com.photostalk.apis.Result;
import com.photostalk.core.Communicator;
import com.photostalk.customViews.AudioVisualizer;
import com.photostalk.customViews.WaveAudioVisualizer;
import com.photostalk.fragments.PhotoFragment;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.models.Comment;
import com.photostalk.models.Model;
import com.photostalk.models.Photo;
import com.photostalk.models.Story;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.PhotosTalkUtils;
import com.photostalk.utils.Player;
import com.photostalk.utils.recorder.Recorder;
import com.photostalk.utils.recorder.RecorderNewAPI;
import com.photostalk.utils.recorder.RecorderOldAPI;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;

public class PhotoActivity extends LoggedInActivity {

    public final static String PHOTO_ID = "photo_id";
    public final static String STORY = "story";
    public final static String POSITION_IN_STORY = "position_in_story";
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
    private ViewPager mViewPager;
    private ImageView mPlayStopButton;
    private ImageButton mMicButton;
    private ProgressBar mProgressBar;
    private FrameLayout mRecorderContainer;
    private View mAudioVisualizer;
    private TextView mRecordDuration;
    private View mRecordFlag;
    private LinearLayout mRecordIndicatorWrapper;
    private FloatingActionButton mRecordButton;
    private FloatingActionButton mPlayStopRecordingButton;
    private FloatingActionButton mPostRecordedCommentButton;
    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private TextView mCommentsLayoutCommentCount;
    private AlertDialog mProgressDialog;

    private PhotoActivityAdapter mAdapter;
    private PhotoActivityAdapter.OnActionListener mOnActionListener;
    private Menu mMenu;

    private BroadcastReceiver mBroadcastReceiver;
    private Recorder mRecorder;
    private boolean mArePermissionsGranted = false;

    private Player mPlayer;
    private Photo mPhoto;
    private Story mStory;
    private int mPlayingComment = -1;
    private boolean mIsHeaderPlaying = false;
    private boolean mIsOverlayShown = true;
    private boolean mOverlayAnimating = false;
    private int mCurrentFragmentId; // same as the position of teh fragment in the view pager
    private boolean mFirstTimeToCheckHasRecorded = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * load the state of the audio permission
         */
        mArePermissionsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        /**
         * fetch the story
         */
        mStory = ((Story) getIntent().getExtras().getSerializable(STORY));
        if (mStory == null) {
            /**
             * if there are no stories passed: get the id of the photo
             */
            String photoId = getIntent().getStringExtra(PHOTO_ID);
            if (photoId == null) {
                Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.error_loading_the_photo)).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                });
                return;
            }

            /**
             * create a photo object
             */
            Photo photo = new Photo();
            photo.setId(photoId);

            /**
             * and a story object and add the photo object
             * into the story
             */
            mCurrentFragmentId = 0;
            mStory = new Story();
            mStory.getPhotos().add(photo);
        } else {
            mCurrentFragmentId = getIntent().getIntExtra(POSITION_IN_STORY, 0);
        }

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
        mMenu = menu;
        hideShowDeleteOption();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.mute:
                if (mPhoto == null) return true;
                if (mPhoto.isMuted()) {
                    PhotosApi.unmute(mPhoto.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mPhoto.setIsMuted(false);
                                hideShowDeleteOption();
                            } else {
                                Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                } else {
                    PhotosApi.mute(mPhoto.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mPhoto.setIsMuted(true);
                                hideShowDeleteOption();
                            } else {
                                Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                }
                break;
            case R.id.delete:
                if (mPhoto == null) return true;
                playHeader(false);
                PhotosApi.delete(mPhoto.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            Broadcasting.sendPhotoDelete(mPhoto.getId());
                            finish();
                        } else
                            Notifications.showSnackbar(PhotoActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
            case R.id.report:
                if (mPhoto == null) return true;
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
                if (intent.getAction().equals(Broadcasting.PHOTO_DELETE) && mPhoto != null) {
                    String photoId = intent.getStringExtra("photo_id");
                    if (!photoId.equals(mPhoto.getId())) return;
                    finish();
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE) && mPhoto != null) {
                    String storyId = intent.getStringExtra("story_id");
                    if (mPhoto.getStory().getId().equals(storyId))
                        finish();
                }
            }
        };


        IntentFilter intentFilter = new IntentFilter(Broadcasting.PHOTO_DELETE);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
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
        mViewPager = ((ViewPager) findViewById(R.id.viewpager));
        mPlayStopButton = ((ImageView) findViewById(R.id.play_stop_button));
        mMicButton = ((ImageButton) findViewById(R.id.mic_button));
        mProgressBar = ((ProgressBar) findViewById(R.id.progress));
        mRecordComment = ((TextView) findViewById(R.id.record_comment));
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        mCommentsLayoutCommentCount = ((TextView) findViewById(R.id.comments_layout_comments_number));
        mRecorderContainer = ((FrameLayout) findViewById(R.id.recorder));
        mAudioVisualizer = findViewById(Build.VERSION.SDK_INT >= 18 ? R.id.audio_visualizer_wave : R.id.audio_visualizer);
        mRecordDuration = ((TextView) findViewById(R.id.record_duration));
        mRecordFlag = findViewById(R.id.record_flag);
        mRecordIndicatorWrapper = ((LinearLayout) findViewById(R.id.record_indicator_wrapper));
        mRecordButton = ((FloatingActionButton) findViewById(R.id.fab));
        mPlayStopRecordingButton = ((FloatingActionButton) findViewById(R.id.play_stop_recording_button));
        mPostRecordedCommentButton = ((FloatingActionButton) findViewById(R.id.post_button));
    }

    private void initEvents() {

        mHashTagsTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    String hashtag = MiscUtils.getWordFromCharPosition(mHashTagsTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY()), mHashTagsTextView.getText().toString());
                    if (hashtag == null) return true;
                    if (hashtag.startsWith("#")) hashtag = hashtag.substring(1);
                    Intent i = new Intent(PhotoActivity.this, SearchActivity.class);
                    i.putExtra(SearchActivity.SEARCH_TERM, hashtag);
                    i.putExtra(SearchActivity.SEARCH_TYPE, SearchActivity.SEARCH_TYPE_STORY);
                    startActivity(i);
                    return true;
                }
                return false;
            }
        });

        mViewPager.setOnClickListener(new View.OnClickListener() {
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

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                /**
                 * stop the player of the photo and the comment
                 */
                playComment(-1);
                playHeader(false);

                /**
                 * set the current id
                 */
                mCurrentFragmentId = position;
                PhotoFragment fragment = ((PhotoFragment) ((FragmentPagerAdapter) mViewPager.getAdapter()).getItem(position));
                mPhoto = fragment.getPhoto();
                fillPhotoInfo();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mMicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhoto == null) return;
                showRecordDialog(true);
            }
        });

        mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhoto == null) return;
                playHeader(!mIsHeaderPlaying);
            }
        });

        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhoto == null) return;
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
                if (mPhoto == null) return;
                showRecordDialog(true);
            }
        });

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhoto == null) return;
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
                if (mPhoto == null) return;
                /**
                 * if it is currently recording
                 * stop and post
                 */
                if (mRecorder.getState() == Recorder.RECORDING) {
                    mRecorder.stop();
                    showRecordUI(false);

                }
                /**
                 * start recording
                 */
                else if (mRecorder.getState() == RecorderOldAPI.NONE) {
                    showRecordUI(true);
                    mRecorder.record(20);
                }
            }
        });


        mPostRecordedCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitComment();
                showRecordDialog(false);
            }
        });

        mPlayStopRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRecorder.getState() == Recorder.PLAYING) {
                    // stop
                    mRecorder.stopPlaying();
                    mPlayStopRecordingButton.setImageResource(R.drawable.play_blue);
                } else {
                    // play
                    mRecorder.play();
                    mPlayStopRecordingButton.setImageResource(R.drawable.stop_blue);
                }
            }
        });

        /*mCancelRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecorder.cancel();
                mRecorder.clean();
                showRecordDialog(false);
            }
        });*/

        mOnActionListener = new PhotoActivityAdapter.OnActionListener() {
            @Override
            public void onPlayStopComment(Comment comment, int position, boolean isStopping) {
                if (mPhoto == null) return;
                playComment(isStopping ? -1 : position);
            }

            @Override
            public void onSeek(int progress) {
                if (mPhoto == null) return;
                if (mPlayingComment == -1) return;
                mPlayer.seek(progress * 10);
            }

            @Override
            public void onDeleteCommentClicked(final int position) {
                if (mPhoto == null) return;
                final Comment comment = mAdapter.getItems().get(position);
                if (!mPhoto.isAuthor() && !comment.isAuthor()) return;
                playComment(-1);
                AlertDialog.Builder b = new AlertDialog.Builder(PhotoActivity.this);
                b.setTitle(R.string.confirm);
                b.setMessage(R.string.delete_this_comment);
                b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
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
                if (mPhoto == null) return;
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
        if (play && mPhoto == null) return;
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
        if (position != -1 && mPhoto == null) return;
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
            if (mRecorder.getState() == RecorderOldAPI.RECORDING)
                mRecorder.cancel();
            showRecordDialog(false);
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
         * hide the recorder dialog:
         */
        mRecorderContainer.setVisibility(View.GONE);

        /**
         * setup the view pager
         */
        final PhotoFragment[] photoFragments = new PhotoFragment[mStory.getPhotos().size()];
        int counter = 0;
        for (final Photo p : mStory.getPhotos()) {
            final int id = counter;
            photoFragments[counter++] = new PhotoFragment() {
                @Override
                public void onViewCreated(View view, Bundle savedInstanceState) {
                    super.onViewCreated(view, savedInstanceState);
                    setPhotoId(p.getId(), id, new OnActionListener() {
                        @Override
                        public void onPhotoLoaded(int id, Photo photo) {
                            if (mCurrentFragmentId == id) {
                                mPhoto = photo;
                                fillPhotoInfo();
                                hideShowDeleteOption();
                            }
                        }

                        @Override
                        public void onSwipped() {
                            mRefreshRecyclerViewFragment.refreshItems(mRefreshRecyclerViewFragment.getFirstItemId(), null);
                            photoFragments[id].stopLoadingUI();
                        }
                    });
                }
            };
        }

        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return photoFragments[position];
            }

            @Override
            public int getCount() {
                return photoFragments.length;
            }
        });

        mViewPager.setOffscreenPageLimit(photoFragments.length <= 20 ? photoFragments.length : 20);

        /**
         * connect the comments number textview with the
         * sliding up pane
         */
        mSlidingUpPanelLayout.setDragView(mShowCommentsButton);


        mAdapter = new PhotoActivityAdapter(mOnActionListener);
        mRefreshRecyclerViewFragment.setAdapter(mAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                if (mPhoto == null) return;
                PhotosApi.comments(
                        mPhoto.getId(),
                        new ApiListeners.OnItemsArrayLoadedListener() {
                            @Override
                            public void onLoaded(Result result, ArrayList<Model> items) {
                                boolean isFromTop = mRefreshRecyclerViewFragment.getSinceId() != null && mRefreshRecyclerViewFragment.getMaxId() == null;
                                if (result.isSucceeded() && items != null && isFromTop) {
                                    mPhoto.setCommentsCount(mPhoto.getCommentsCount() + items.size());
                                    refreshCommentCount();
                                }
                                mRefreshRecyclerViewFragment.getAppender().onLoaded(result, items);
                            }
                        },
                        mRefreshRecyclerViewFragment.getMaxId(),
                        mRefreshRecyclerViewFragment.getSinceId());
            }
        });
        mRefreshRecyclerViewFragment.setIsLazyLoading(true);


        /**
         * initialize the recorder instance
         */
        Recorder.OnRecordingListener recorderListener = new Recorder.OnRecordingListener() {
            @Override
            public void onUpdate(int amplitude, int elapsed) {
                if (Build.VERSION.SDK_INT >= 18) return;
                ((AudioVisualizer) mAudioVisualizer).update(amplitude);
                mRecordDuration.setText(PhotosTalkUtils.getDurationFormatted(elapsed));
            }

            @Override
            public void onUpdate(short[] audioData, int length, int timeElapsed) {
                if (Build.VERSION.SDK_INT < 18) return;
                ((WaveAudioVisualizer) mAudioVisualizer).update(audioData, length);
                mRecordFlag.setVisibility(timeElapsed % 2 == 0 ? View.VISIBLE : View.INVISIBLE);
                mRecordDuration.setText(PhotosTalkUtils.getDurationFormatted(timeElapsed));
            }

            @Override
            public void onMaxDurationReached() {
                Notifications.showSnackbar(PhotoActivity.this, getString(R.string.recording_stopped_maximum_20_seconds));
                showRecordDialog(false);
                mRecorder.cancel();
            }
        };

        MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mPlayStopRecordingButton.setImageResource(R.drawable.play_blue);
            }
        };

        if (Build.VERSION.SDK_INT >= 18) {
            mRecorder = new RecorderNewAPI(PhotoActivity.this, recorderListener, completionListener);
        } else {
            mRecorder = new RecorderOldAPI(PhotoActivity.this, recorderListener, completionListener);
        }


        /**
         * fill the information of the current photo
         */
        fillPhotoInfo();

        /**
         * move to the appropriate position in the view pager.
         */
        mViewPager.setCurrentItem(mCurrentFragmentId);
    }

    private void fillPhotoInfo() {
        mLiveTextView.setVisibility(mPhoto == null || !mPhoto.isLive() ? View.GONE : View.VISIBLE);
        mHashTagsTextView.setText(mPhoto == null ? "" : mPhoto.getHashTagsConcatenated());
        mLikesCount.setText(getResources().getQuantityString(R.plurals.d_likes, mPhoto == null ? 0 : mPhoto.getLikesCount(), mPhoto == null ? 0 : mPhoto.getLikesCount()));
        refreshCommentCount();
        mLikeButton.setCompoundDrawablesWithIntrinsicBounds(mPhoto == null || !mPhoto.isLiked() ? R.mipmap.ic_empty_heart : R.mipmap.ic_like_blue, 0, 0, 0);
        mPlayStopButton.setVisibility(mPhoto == null || mPhoto.getAudioUrl().isEmpty() ? View.GONE : View.VISIBLE);


        /**
         * load the comments
         */
        // clear
        mAdapter.getItems().clear();
        mAdapter.notifyDataSetChanged();
        mRefreshRecyclerViewFragment.refreshItems(null, null);
    }

    private void submitComment() {
        if (mPhoto == null) return;
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
                    //mPhoto.setCommentsCount(mPhoto.getCommentsCount() + 1);
                    //refreshCommentCount();
                    if (areCommentsShown()) return;
                    mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else {
                    Notifications.showListAlertDialog(PhotoActivity.this, getString(R.string.error), result.getMessages());
                }
            }
        });
    }

    private void showRecordDialog(boolean show) {
        if (mPhoto == null) return;
        /**
         * first check for the recording permission
         */
        if (!mArePermissionsGranted) {
            ActivityCompat.requestPermissions(PhotoActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            mRecorderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            showRecordUI(false);
            mFirstTimeToCheckHasRecorded = true;
            if (!show) {
                hidePostAndPlayRecordingButtons();
                mRecorder.clean();
            }
        }
    }

    private boolean areCommentsShown() {
        return mSlidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED;
    }

    private void refreshCommentCount() {
        mShowCommentsButton.setText(getResources().getQuantityString(R.plurals.d_comments, mPhoto == null ? 0 : mPhoto.getCommentsCount(), mPhoto == null ? 0 : mPhoto.getCommentsCount()));
        mCommentsLayoutCommentCount.setText(getResources().getQuantityString(R.plurals.d_people_commented_on_this, mPhoto == null ? 0 : mPhoto.getCommentsCount(), mPhoto == null ? 0 : mPhoto.getCommentsCount()));
    }

    private void hideShowDeleteOption() {
        if (mPhoto == null || mMenu == null) return;
        mMenu.getItem(0).setVisible(mPhoto.isAuthor()); // mute
        mMenu.getItem(1).setVisible(mPhoto.isAuthor()); // delete
        mMenu.getItem(2).setVisible(!mPhoto.isAuthor()); // report

        if (mPhoto.isAuthor()) {
            mMenu.getItem(0).setTitle(mPhoto.isMuted() ? getString(R.string.unmute) : getString(R.string.mute));
        }
    }

    private void showRecordUI(boolean record) {
        if (!record) {
            translateRecorderButtons(0);
            mRecordIndicatorWrapper.animate().alpha(0.0f).setDuration(150).start();
            mAudioVisualizer.setVisibility(View.INVISIBLE);

            /**
             * check if has recorded
             */
            if (mRecorder.hasRecorded() && mFirstTimeToCheckHasRecorded) {
                mPlayStopRecordingButton.animate().translationX(-MiscUtils.convertDP2Pixel(66)).alpha(1.0f).setDuration(250).start();
                mPostRecordedCommentButton.animate().translationX(MiscUtils.convertDP2Pixel(66)).alpha(1.0f).setDuration(250).start();

                mFirstTimeToCheckHasRecorded = false;
            } else {
                mPlayStopRecordingButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start();
                mPostRecordedCommentButton.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start();
            }

        } else {
            translateRecorderButtons(-MiscUtils.convertDP2Pixel(38));
            mRecordDuration.setText(PhotosTalkUtils.getDurationFormatted(0));
            mRecordIndicatorWrapper.animate().alpha(1.0f).setDuration(150).start();
            mAudioVisualizer.setVisibility(View.VISIBLE);

            if (!mFirstTimeToCheckHasRecorded) {
                mPlayStopRecordingButton.animate().scaleX(0.0f).scaleY(0.0f).setDuration(250).start();
                mPostRecordedCommentButton.animate().scaleX(0.0f).scaleY(0.0f).setDuration(250).start();
            }
        }
    }

    private void hidePostAndPlayRecordingButtons() {
        mPlayStopRecordingButton.setAlpha(0.0f);
        mPlayStopRecordingButton.setTranslationX(0.0f);
        mPostRecordedCommentButton.setAlpha(0.0f);
        mPostRecordedCommentButton.setTranslationX(0.0f);
    }

    private void translateRecorderButtons(int dY) {
        mRecordButton.animate().translationY(dY).setDuration(150).start();
        mPlayStopRecordingButton.animate().translationY(dY).setDuration(150).start();
        mPostRecordedCommentButton.animate().translationY(dY).setDuration(150).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mArePermissionsGranted = true;
                mRecorderContainer.setVisibility(View.VISIBLE);
            } else {
                Notifications.showSnackbar(this, getString(R.string.you_should_grant_all_requested_comments_to_post_a_comment));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
