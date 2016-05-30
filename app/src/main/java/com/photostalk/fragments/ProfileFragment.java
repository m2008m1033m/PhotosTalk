package com.photostalk.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.photostalk.PhotoActivity;
import com.photostalk.R;
import com.photostalk.StoryActivity;
import com.photostalk.adapters.ProfileActivityPhotosAdapter;
import com.photostalk.adapters.ProfileActivityStoriesAdapter;
import com.photostalk.adapters.ProfileViewPagerAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.models.Photo;
import com.photostalk.models.Story;
import com.photostalk.models.UserModel;
import com.photostalk.services.UserApi;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Player;
import com.squareup.picasso.Picasso;

/**
 * Created by mohammed on 3/16/16.
 */
public class ProfileFragment extends Fragment {

    private RefreshRecyclerViewFragment mStoriesFragment = null;
    private AboutMeFragment mAboutMeFragment = null;
    private RefreshRecyclerViewFragment mPhotosFragment = null;

    private View mView;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private ImageView mUserPhoto;
    private TextView mUserNameTextView;
    private TextView mLocationTextView;
    private Toolbar mToolbar;

    private ProfileActivityStoriesAdapter mStoriesAdapter;
    private ProfileActivityPhotosAdapter mPhotosAdapter;

    private BroadcastReceiver mBroadcastReceiver;

    private Player mPlayer;
    private UserModel mUser;

    private boolean mIsOtherUser = false;
    private boolean mIsAboutFragmentLoaded = false;
    private int mCurrentlyPlaying = -1;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.profile_fragment, container, false);
        setupBroadcastReceiver();
        init();

        return mView;
    }

    public void setToolbarVisible(boolean visible) {
        if (!visible) {
            mToolbar.setVisibility(View.GONE);
        }

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().setTitle("");
    }


    @Override
    public void onPause() {
        if (mPlayer != null)
            mPlayer.stop();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mPlayer != null)
            mPlayer.stop();
        Communicator.getInstance().cancelByTag("user_get");
        Communicator.getInstance().cancelByTag("user_stories");
        Communicator.getInstance().cancelByTag("user_photos");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.PHOTO_DELETE)) {
                    String photoId = intent.getStringExtra("photo_id");
                    int len = mPhotosAdapter.getItemCount();
                    int i = 0;
                    for (; i < len; i++)
                        if (((Photo) mPhotosAdapter.getItems().get(i)).getId().equals(photoId))
                            break;

                    if (i >= len) return;
                    mPhotosAdapter.getItems().remove(i);
                    mPhotosAdapter.notifyDataSetChanged();
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String storyId = intent.getStringExtra("story_id");
                    int len = mStoriesAdapter.getItemCount();
                    int i = 0;
                    for (; i < len; i++)
                        if (((Story) mStoriesAdapter.getItems().get(i)).getId().equals(storyId))
                            break;
                    if (i >= len) return;
                    mStoriesAdapter.getItems().remove(i);
                    mStoriesAdapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcasting.PHOTO_DELETE);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }


    private void init() {
        initReferences();
        setupPlayer();
    }

    public void fillFields(UserModel user) {
        mIsOtherUser = !(user.getId() == null || user.getId().equals(User.getInstance().getId()));
        mUser = user;
        if (!mUser.getPhoto().isEmpty())
            Picasso.with(getActivity())
                    .load(mUser.getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(mUserPhoto);
        mUserNameTextView.setText(mUser.getName());
        mLocationTextView.setText(mUser.getUsername());
        setupViewPager();
        if (mIsAboutFragmentLoaded)
            mAboutMeFragment.refresh(mUser);
    }

    private void setupPlayer() {
        mPlayer = new Player(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                View v = getItemAt(mCurrentlyPlaying, mPhotosFragment.getRecyclerView());
                if (v != null) {
                    v.findViewById(R.id.progress).setVisibility(View.GONE);
                    ((ImageView) v.findViewById(R.id.play_stop_button)).setImageResource(R.drawable.stop_blue);
                }
                mPhotosAdapter.setIsLoading(false);
            }
        }, new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {

            }
        }, new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playPhoto(-1);
            }
        }, new Runnable() {
            @Override
            public void run() {
                playPhoto(-1);
                Notifications.showSnackbar(getActivity(), getString(R.string.failed_to_load_the_audio));
            }
        }, new Player.OnPlayerUpdateListener() {
            @Override
            public void onUpdate(int position) {

            }
        });
    }

    private void setupViewPager() {
        if (mViewPager.getAdapter() != null) return;
        setupFragments();
        ProfileViewPagerAdapter profileViewPagerAdapter = new ProfileViewPagerAdapter(getChildFragmentManager());
        profileViewPagerAdapter.addFragment(mStoriesFragment == null ? new PrivateUserFragment() : mStoriesFragment, getString(R.string.stories_captial));
        profileViewPagerAdapter.addFragment(mAboutMeFragment, getString(R.string.about_capital));
        profileViewPagerAdapter.addFragment(mPhotosFragment == null ? new PrivateUserFragment() : mPhotosFragment, getString(R.string.photos_capital));
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(profileViewPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setCurrentItem(1);
    }

    private void setupFragments() {
        if (!mIsOtherUser || !mUser.isPrivate() || mUser.isFollowingUser())
            setupStoriesFragment();
        setupAboutMeFragment();
        if (!mIsOtherUser || !mUser.isPrivate() || mUser.isFollowingUser())
            setupPhotosFragment();
    }

    private void setupStoriesFragment() {
        if (mStoriesFragment != null) return;
        mStoriesFragment = new RefreshRecyclerViewFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                mStoriesAdapter = new ProfileActivityStoriesAdapter(new ProfileActivityStoriesAdapter.OnActionListener() {
                    @Override
                    public void onStoryClicked(int position) {
                        Story story = ((Story) mStoriesAdapter.getItems().get(position));
                        Intent i = new Intent(getActivity(), StoryActivity.class);
                        i.putExtra(StoryActivity.STORY_ID, story.getId());
                        startActivity(i);
                    }

                    @Override
                    public void onPlayStopButtonClicked(int position) {

                    }
                });
                mStoriesFragment.setAdapter(mStoriesAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
                    @Override
                    public void executeService() {
                        UserApi.stories(
                                mIsOtherUser ? mUser.getId() : null,
                                mStoriesFragment.getSinceId(),
                                mStoriesFragment.getMaxId(),
                                mStoriesFragment.getAppender()
                        );

                    }
                });
                mStoriesFragment.refreshItems(null, null);
            }
        };
    }

    private void setupAboutMeFragment() {
        if (mAboutMeFragment != null) return;
        mAboutMeFragment = new AboutMeFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                mAboutMeFragment.setUser(mUser, mIsOtherUser);
                mIsAboutFragmentLoaded = true;
            }
        };
    }

    private void setupPhotosFragment() {
        if (mPhotosFragment != null) return;
        mPhotosFragment = new RefreshRecyclerViewFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);

                mPhotosAdapter = new ProfileActivityPhotosAdapter(new ProfileActivityPhotosAdapter.OnActionListener() {
                    @Override
                    public void onPlayStopButtonClicked(int position) {
                        playPhoto(position);
                    }

                    @Override
                    public void onPhotoClicked(int position) {
                        Photo photo = ((Photo) mPhotosAdapter.getItems().get(position));
                        Intent i = new Intent(getActivity(), PhotoActivity.class);
                        i.putExtra(PhotoActivity.PHOTO_ID, photo.getId());
                        startActivity(i);
                    }
                });

                mPhotosFragment.setAdapter(mPhotosAdapter, new ServiceWrapper() {
                    @Override
                    public void executeService() {
                        UserApi.photos(
                                mIsOtherUser ? mUser.getId() : null,
                                mPhotosFragment.getSinceId(),
                                mPhotosFragment.getMaxId(),
                                mPhotosFragment.getAppender()
                        );
                    }
                });

                mPhotosFragment.refreshItems(null, null);
            }
        };
    }

    private void initReferences() {
        mToolbar = ((Toolbar) mView.findViewById(R.id.toolbar));
        mViewPager = ((ViewPager) mView.findViewById(R.id.viewpager));
        mUserPhoto = ((ImageView) mView.findViewById(R.id.photo));
        mTabLayout = ((TabLayout) mView.findViewById(R.id.tabs));
        mUserNameTextView = ((TextView) mView.findViewById(R.id.user_name));
        mLocationTextView = ((TextView) mView.findViewById(R.id.location));
        mViewPager = ((ViewPager) mView.findViewById(R.id.viewpager));
    }

    private void playPhoto(int position) {
        if (position == -1 && mCurrentlyPlaying == -1) return;
        if (position != -1 && position != mCurrentlyPlaying) {
            //play
            playPhoto(-1);
            String audioUrl = ((Photo) mPhotosAdapter.getItems().get(position)).getAudioUrl();
            mPlayer.play(audioUrl);
            changePhotoUi(position, true);
            mPhotosAdapter.setCurrentlyPlaying(position);
            mPhotosAdapter.setIsLoading(true);
            mCurrentlyPlaying = position;
        } else {
            //stop
            mPlayer.stop();
            changePhotoUi(mCurrentlyPlaying, false);
            mPhotosAdapter.setCurrentlyPlaying(-1);
            mPhotosAdapter.setIsLoading(false);
            mCurrentlyPlaying = -1;
        }
    }

    private void changePhotoUi(int position, boolean play) {
        View v = getItemAt(position, mPhotosFragment.getRecyclerView());
        if (v != null) {
            ((ImageView) v.findViewById(R.id.play_stop_button)).setImageResource(play ? R.drawable.crystal_button : R.drawable.play_blue);
            v.findViewById(R.id.progress).setVisibility(play ? View.VISIBLE : View.GONE);
        }
    }

    private View getItemAt(int position, RecyclerView recyclerView) {
        position = position -
                ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        return recyclerView.getChildAt(position);
    }
}
