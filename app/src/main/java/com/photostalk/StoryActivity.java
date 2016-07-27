package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.photostalk.adapters.StoryActivityAdapter;
import com.photostalk.core.User;
import com.photostalk.models.Model;
import com.photostalk.models.Story;
import com.photostalk.apis.Result;
import com.photostalk.apis.StoriesApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Player;
import com.squareup.picasso.Picasso;


public class StoryActivity extends LoggedInActivity {
    public static final String STORY_ID = "story_id";

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefreshLayout;
    private ImageView mUserPhoto;
    private TextView mStoryDateTextView;
    private TextView mUserNameTextView;
    private TextView mUsernameTextView;
    private Player mPlayer;
    private Story mStory;
    private StoryActivityAdapter mAdapter;
    private ApiListeners.OnItemLoadedListener mOnItemLoadedListener;
    private int mPlayingPhoto = -1;
    private Menu mMenu;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_activity);
        setupBroadcastReceiver();
        setTitle(getString(R.string.showing_story));
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.story_actions, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.delete:
                if (mStory == null) return true;
                StoriesApi.delete(mStory.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            Broadcasting.sendStoryDelete(mStory.getId(), mStory.getUser().getId());
                            finish();
                        } else
                            Notifications.showSnackbar(StoryActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
            case R.id.report:
                if (mStory == null) return true;
                StoriesApi.report(mStory.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded())
                            Notifications.showSnackbar(StoryActivity.this, getString(R.string.a_report_has_been_sent_successfully));
                        else
                            Notifications.showSnackbar(StoryActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mPlayer.isPlaying()) {
            playPhoto(-1);
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        playPhoto(-1);
        super.onPause();
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
                    int i = 0;
                    int len = mStory.getPhotos().size();
                    for (; i < len; i++)
                        if (mStory.getPhotos().get(i).getId().equals(photoId))
                            break;
                    if (i >= len) return;

                    mStory.getPhotos().remove(i);
                    mAdapter.notifyDataSetChanged();
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String storyId = intent.getStringExtra("story_id");
                    if (mStory.getId().equals(storyId))
                        finish();
                } else if (intent.getAction().equals(Broadcasting.PROFILE_UPDATED)) {
                    if (!User.getInstance().getId().equals(mStory.getUser().getId())) return;
                    Picasso.with(StoryActivity.this)
                            .load(User.getInstance().getPhoto())
                            .skipMemoryCache()
                            .into(mUserPhoto);
                }

            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.PHOTO_DELETE);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        intentFilter.addAction(Broadcasting.PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initReferences();
        initLoader();
        fillFields();
        initEvents();
    }

    private void initReferences() {
        mRecyclerView = ((RecyclerView) findViewById(R.id.recycler_view));
        mRefreshLayout = ((SwipeRefreshLayout) findViewById(R.id.refresh_layout));
        mUserPhoto = ((ImageView) findViewById(R.id.user_photo));
        mStoryDateTextView = ((TextView) findViewById(R.id.date));
        mUserNameTextView = ((TextView) findViewById(R.id.user_name));
        mUsernameTextView = ((TextView) findViewById(R.id.username));
    }

    private void initLoader() {
        mOnItemLoadedListener = new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {
                    mStory = ((Story) item);
                    if (!mStory.getUser().getPhoto().equals(""))
                        Picasso.with(StoryActivity.this)
                                .load(mStory.getUser().getPhoto())
                                .placeholder(R.drawable.no_avatar)
                                .into(mUserPhoto);
                    else {
                        Picasso.with(StoryActivity.this)
                                .load(R.drawable.no_avatar)
                                .into(mUserPhoto);
                    }
                    mStoryDateTextView.setText(mStory.getStoryDateAsString("dd - MMMM"));
                    mUserNameTextView.setText(mStory.getUser().getName());
                    mUsernameTextView.setText(mStory.getUser().getUsername());
                    mMenu.getItem(0).setVisible(mStory.isAuthor());
                    mAdapter.setStory(mStory);
                } else {
                    Notifications.showListAlertDialog(StoryActivity.this, getString(R.string.error), result.getMessages());
                    finish();
                }
                setRefreshing(false);
            }
        };
    }

    private void fillFields() {
        mAdapter = new StoryActivityAdapter(new StoryActivityAdapter.OnActionListener() {
            @Override
            public void onPlayStopButtonClicked(int position) {
                playPhoto(position);
            }

            @Override
            public void onPhotoClicked(int position) {
                Intent i = new Intent(StoryActivity.this, PhotoActivity.class);
                i.putExtra(PhotoActivity.POSITION_IN_STORY, position);
                Bundle b = new Bundle();
                b.putSerializable(PhotoActivity.STORY, mStory);
                i.putExtras(b);
                startActivity(i);
            }

            @Override
            public void onHashtagClicked(String hashtag) {
                Intent i = new Intent(StoryActivity.this, SearchActivity.class);
                i.putExtra(SearchActivity.SEARCH_TERM, hashtag);
                i.putExtra(SearchActivity.SEARCH_TYPE, SearchActivity.SEARCH_TYPE_STORY);
                startActivity(i);
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(StoryActivity.this));
        mRecyclerView.setAdapter(mAdapter);

        setRefreshing(true);
        loadItem();

        mPlayer = new Player(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        View v = getItemAt(mPlayingPhoto);
                        if (v == null) return;
                        v.findViewById(R.id.progress).setVisibility(View.GONE);
                        ((ImageView) v.findViewById(R.id.play_stop_button)).setImageResource(R.drawable.stop_blue);
                    }
                },
                new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {

                    }
                },
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        playPhoto(-1);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        Notifications.showSnackbar(StoryActivity.this, getString(R.string.failed_to_load_the_audio));
                    }
                },
                new Player.OnPlayerUpdateListener() {
                    @Override
                    public void onUpdate(int position) {

                    }
                });
    }

    private void initEvents() {
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadItem();
            }
        });

        mUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStory == null) return;
                Intent i = new Intent(StoryActivity.this, ProfileActivity.class);
                i.putExtra(ProfileActivity.USER_ID, mStory.getUser().getId());
                startActivity(i);
            }
        });
    }

    private void loadItem() {
        playPhoto(-1);
        String storyId = getIntent().getStringExtra(STORY_ID);
        StoriesApi.get(storyId, mOnItemLoadedListener);
    }

    private void setRefreshing(final boolean refreshing) {
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(refreshing);
            }
        });
    }

    private void playPhoto(int position) {
        if (mPlayer == null) return;
        /**
         * stop
         */
        if ((position == -1 && mPlayingPhoto != -1) || (position == mPlayingPhoto)) {
            mPlayer.stop();
        }

        /**
         * play
         */
        else {
            playPhoto(-1);
            mPlayer.play(mStory.getPhotos().get(position).getAudioUrl());
        }

        mAdapter.setItemPlaying(position);
        mAdapter.setIsLoading(true);
        changePhotoUi(position == -1 ? mPlayingPhoto : position, position != -1 && position != mPlayingPhoto);
        mPlayingPhoto = position;
    }

    private void changePhotoUi(int position, boolean play) {
        View v = getItemAt(position);
        if (v == null) return;
        ((ImageView) v.findViewById(R.id.play_stop_button)).setImageResource(play ? R.drawable.crystal_button : R.drawable.play_blue);
        v.findViewById(R.id.progress).setVisibility(play ? View.VISIBLE : View.GONE);
    }

    private View getItemAt(int position) {
        position = position -
                ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        return mRecyclerView.getChildAt(position);
    }
}
