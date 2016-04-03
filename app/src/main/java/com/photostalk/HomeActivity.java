package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.photostalk.adapters.TimelineFragmentAdapter;
import com.photostalk.core.User;
import com.photostalk.fragments.MyStoriesFragment;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.fragments.TrendingFragment;
import com.photostalk.models.Model;
import com.photostalk.models.Story;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/10/16.
 */
public class HomeActivity extends AppCompatActivity {

    public final static int USER = 0;
    public final static int TRENDING = 1;

    private ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;
    private ImageButton mMenuButton;
    private View mNotificationsBadge;
    private CoordinatorLayout mCoordinatorLayout;
    private TabLayout mTabLayout;
    private FloatingActionButton mFloatingActionButton;
    private NavigationView mNavigationView;

    private RefreshRecyclerViewFragment mTimelineFragment;
    private TrendingFragment mTrendingFragment;
    private MyStoriesFragment mMyStoriesFragment;

    private TimelineFragmentAdapter mTimelineAdapter;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        setupBroadcastReceiver();

        Toolbar toolbar = ((Toolbar) findViewById(R.id.toolbar));
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        View v;
        if (actionBar != null) {
            //v = findViewById(android.R.id.home);

            //actionBar.setHomeButtonEnabled(true);
            //actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeAsUpIndicator(R.mipmap.ic_menu_white_24dp);

            //int i = 1 + 9;
        }

        init();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        /**
         * if I'm at the trending page and the search mode is
         * enabled and the user presses the back button
         * just flip back from searching mode to normal
         */
        if (mTrendingFragment != null && mTrendingFragment.isSearching() && mTabLayout.getSelectedTabPosition() == 2) {
            mTrendingFragment.setSearching(false);
            return;
        } else if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        // update the notification badge:
        UserApi.notifications("1", null, null, new ApiListeners.OnItemsArrayLoadedListener() {
            @Override
            public void onLoaded(Result result, ArrayList<Model> items) {
                if (items == null) return;
                if (items.size() >= 1)
                    mNotificationsBadge.setVisibility(View.VISIBLE);
                else
                    mNotificationsBadge.setVisibility(View.GONE);

                int count = items.size();
                TextView view = (TextView) mNavigationView.getMenu().findItem(R.id.notifications).getActionView();
                view.setText(count > 0 ? String.valueOf(count) + "+" : null);
            }
        });
        super.onResume();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.FOLLOW)) {
                    String userId = intent.getStringExtra("user_id");
                    boolean request = intent.getBooleanExtra("request", false);
                    boolean follow = intent.getBooleanExtra("follow", false);
                    if (userId == null) return;
                    if (mTimelineAdapter != null)
                        mTimelineAdapter.updateUser(userId, request, follow);
                    if (mTrendingFragment != null)
                        mTrendingFragment.updateUser(userId, request, follow);


                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String storyId = intent.getStringExtra("story_id");
                    if (storyId == null) return;
                    if (mTimelineAdapter != null)
                        mTimelineAdapter.removeStory(storyId);
                    if (mTrendingFragment != null)
                        mTrendingFragment.removeStory(storyId);
                    if (mMyStoriesFragment != null)
                        mMyStoriesFragment.removeStory(storyId);
                } else if (intent.getAction().equals(Broadcasting.LOGOUT))
                    finish();
                else if (intent.getAction().equals(Broadcasting.PROFILE_UPDATED)) {
                    refreshNavigationView();
                    mTimelineAdapter.updateUserPhoto();
                    mTrendingFragment.updateUserPhoto();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.FOLLOW);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        intentFilter.addAction(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initReferences();
        setupNavigationView();
        setupViewPager();
        refreshNavigationView();

        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });
    }

    private void initReferences() {
        mDrawerLayout = ((DrawerLayout) findViewById(R.id.drawer_layout));
        mMenuButton = ((ImageButton) findViewById(R.id.menu_button));
        mNotificationsBadge = findViewById(R.id.notifications_badge);
        mNavigationView = ((NavigationView) findViewById(R.id.navigation_view));
        mCoordinatorLayout = ((CoordinatorLayout) findViewById(R.id.wrapper));
        mViewPager = ((ViewPager) findViewById(R.id.viewpager));
        mTabLayout = ((TabLayout) findViewById(R.id.tabs));
        mFloatingActionButton = ((FloatingActionButton) findViewById(R.id.fab));

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, CameraActivity.class));
            }
        });
    }

    private void setupNavigationView() {
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.my_profile:
                        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                        break;
                    case R.id.notifications:
                        startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
                        break;
                    case R.id.logout:
                        User.getInstance().logout();
                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                        Broadcasting.sendLogout(HomeActivity.this);
                        break;
                }

                return true;
            }
        });
    }

    private void setupViewPager() {


        setupTimelineAdapter();
        mTrendingFragment = new TrendingFragment();
        setupMyStoriesFragment();
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return mTimelineFragment;
                    case 1:
                        return mMyStoriesFragment;
                    case 2:
                        return mTrendingFragment;
                }
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        mTabLayout.setupWithViewPager(mViewPager);

        mTabLayout.getTabAt(0).setIcon(R.drawable.tab_timeline_icon);
        mTabLayout.getTabAt(1).setIcon(R.drawable.tab_person_icon);
        mTabLayout.getTabAt(2).setIcon(R.drawable.tab_trending_icon);


    }

    private void setupTimelineAdapter() {
        mTimelineAdapter = new TimelineFragmentAdapter(new TimelineFragmentAdapter.OnActionListener() {
            @Override
            public void onFollowButtonClicked(int position) {
                final UserModel user = mTimelineAdapter.getUser(position);
                follow(user, mTimelineAdapter, position);
            }

            @Override
            public void onUserClicked(int position) {
                UserModel userModel = mTimelineAdapter.getUser(position);
                if (userModel == null) return;
                goToUser(userModel.getId());

            }

            @Override
            public void onStoryClicked(int position) {
                Story story = mTimelineAdapter.getStory(position);
                if (story == null) return;
                Intent i = new Intent(HomeActivity.this, StoryActivity.class);
                i.putExtra(StoryActivity.STORY_ID, story.getId());
                startActivity(i);
            }
        });
        mTimelineFragment = new RefreshRecyclerViewFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                mTimelineFragment.setAdapter(mTimelineAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
                    @Override
                    public void executeService() {
                        UserApi.timeline(
                                mTimelineFragment.getMaxId(),
                                mTimelineFragment.getSinceId(),
                                mTimelineFragment.getAppender()
                        );
                    }
                });
                mTimelineFragment.refreshItems(null, null);
            }
        };
    }

    private void setupMyStoriesFragment() {
        mMyStoriesFragment = new MyStoriesFragment();
    }

    private void refreshNavigationView() {

        /**
         * add information to the navigation view:
         */
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.user_name)).setText(User.getInstance().getName());
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.username)).setText(User.getInstance().getUsername());

        if (User.getInstance().getPhoto().isEmpty())
            Picasso.with(HomeActivity.this)
                    .load(R.drawable.no_avatar)
                    .into(((ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.user_photo)));
        else
            Picasso.with(HomeActivity.this)
                    .load(User.getInstance().getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(((ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.user_photo)));
    }

    private void goToUser(String userId) {
        Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
        i.putExtra(ProfileActivity.USER_ID, userId);
        startActivity(i);
    }

    private void follow(final UserModel user, final RecyclerView.Adapter adapter, final int position) {
        if (user == null) return;
        if (user.isFollowingUser())
            UserApi.unfollow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                @Override
                public void onExecuted(Result result) {
                    if (result.isSucceeded()) {
                        user.setIsFollowingUser(false);
                        adapter.notifyItemChanged(position);
                        Broadcasting.sendFollow(HomeActivity.this, user.getId(), false, false);
                    } else {
                        Notifications.showSnackbar(mCoordinatorLayout, result.getMessages().get(0));
                    }
                }
            });
        else if (user.isPrivate()) {
            UserApi.request(user.getId(), new ApiListeners.OnActionExecutedListener() {
                @Override
                public void onExecuted(Result result) {
                    if (result.isSucceeded()) {
                        user.setIsFollowRequestSent(true);
                        adapter.notifyItemChanged(position);
                        Broadcasting.sendFollow(HomeActivity.this, user.getId(), true, false);
                    } else {
                        Notifications.showSnackbar(mCoordinatorLayout, result.getMessages().get(0));
                    }
                }
            });
        } else {
            UserApi.follow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                @Override
                public void onExecuted(Result result) {
                    if (result.isSucceeded()) {
                        user.setIsFollowingUser(true);
                        adapter.notifyItemChanged(position);
                        Broadcasting.sendFollow(HomeActivity.this, user.getId(), false, true);
                    } else {
                        Notifications.showSnackbar(mCoordinatorLayout, result.getMessages().get(0));
                    }
                }
            });
        }
    }

}
