package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.widget.Button;

import com.photostalk.adapters.FollowshipAndBlockagesActivityAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.models.Followship;
import com.photostalk.models.UserModel;
import com.photostalk.apis.Result;
import com.photostalk.apis.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;


public class FollowshipAndBlockagesActivity extends LoggedInActivity {

    public static final String TYPE = "type";
    public static final int TYPE_FOLLOWERS = 0;
    public static final int TYPE_FOLLOWING = 1;
    public static final int TYPE_BLOCKED = 2;

    public static final String USER_ID = "user_id";

    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private FollowshipAndBlockagesActivityAdapter mAdapter;

    private BroadcastReceiver mBroadcastReceiver;

    private int mType;
    private boolean mIsOtherUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = getIntent().getIntExtra(TYPE, TYPE_FOLLOWING);
        setContentView(R.layout.folllowship_activity);
        setTitle(mType == TYPE_FOLLOWERS ? R.string.followers : mType == TYPE_FOLLOWING ? R.string.following : R.string.blocked_users);

        setupBroadcastReceiver();

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        initAdapter();
        initRecyclerView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Communicator.getInstance().cancelByTag("user_followers");
        Communicator.getInstance().cancelByTag("user_followings");
        Communicator.getInstance().cancelByTag("user_blocked");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void setupBroadcastReceiver() {

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.FOLLOW) && mType != TYPE_BLOCKED) {
                    String userId = intent.getStringExtra("user_id");
                    UserModel user = mAdapter.getUserById(userId);
                    if (user == null) return;
                    user.setIsFollowRequestSent(intent.getBooleanExtra("request", user.isFollowRequestSent()));
                    user.setIsFollowingUser(intent.getBooleanExtra("follow", user.isFollowingUser()));
                    mAdapter.notifyDataSetChanged();
                } else if (intent.getAction().equals(Broadcasting.BLOCK) && mType == TYPE_BLOCKED) {
                    String userId = intent.getStringExtra("user_id");
                    int position = mAdapter.getUserPosition(userId);
                    if (position == -1) return;
                    mAdapter.getItems().remove(position);
                    mAdapter.notifyDataSetChanged();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.FOLLOW);
        intentFilter.addAction(Broadcasting.BLOCK);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void initAdapter() {

        mAdapter = new FollowshipAndBlockagesActivityAdapter(new FollowshipAndBlockagesActivityAdapter.OnActionListener() {
            @Override
            public void onItemClicked(int position) {
                Intent i = new Intent(FollowshipAndBlockagesActivity.this, ProfileActivity.class);
                UserModel user;
                if (mType != TYPE_BLOCKED)
                    user = ((Followship) mAdapter.getItems().get(position)).getUser();
                else
                    user = ((UserModel) mAdapter.getItems().get(position));
                i.putExtra(ProfileActivity.USER_ID, user.getId());
                //mCheckUserPosition = position;
                startActivity(i);
            }

            @Override
            public void onFollowButtonClicked(final int position, final Button button) {
                final UserModel user = mAdapter.getUser(position);
                if (mType != TYPE_BLOCKED)
                    if (user.isFollowingUser()) {
                        //unfollow
                        UserApi.unfollow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    if (!mIsOtherUser && mType == TYPE_FOLLOWING) {
                                        mAdapter.getItems().remove(position);
                                    } else {
                                        updateUser(user, false, false);
                                    }
                                    mAdapter.notifyDataSetChanged();
                                    Broadcasting.sendFollow(mAdapter.getUser(position).getId(), false, false);
                                } else
                                    Notifications.showSnackbar(FollowshipAndBlockagesActivity.this, result.getMessages().get(0));
                            }
                        });
                    } else if (!user.isFollowRequestSent()) {
                        if (user.isPrivate()) {
                            //request
                            UserApi.request(user.getId(), new ApiListeners.OnActionExecutedListener() {
                                @Override
                                public void onExecuted(Result result) {
                                    if (result.isSucceeded()) {
                                        Broadcasting.sendFollow(mAdapter.getUser(position).getId(), true, false);
                                        updateUser(user, true, false);
                                    } else
                                        Notifications.showSnackbar(FollowshipAndBlockagesActivity.this, result.getMessages().get(0));
                                }
                            });
                        } else {
                            //follow
                            UserApi.follow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                                @Override
                                public void onExecuted(Result result) {
                                    if (result.isSucceeded()) {
                                        Broadcasting.sendFollow(mAdapter.getUser(position).getId(), false, true);
                                        updateUser(user, false, true);
                                    } else
                                        Notifications.showSnackbar(FollowshipAndBlockagesActivity.this, result.getMessages().get(0));
                                }
                            });
                        }
                    } else {
                        //cancel
                        UserApi.cancel(user.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    Broadcasting.sendFollow(mAdapter.getUser(position).getId(), false, false);
                                    updateUser(user, false, false);
                                } else
                                    Notifications.showSnackbar(FollowshipAndBlockagesActivity.this, result.getMessages().get(0));
                            }
                        });
                    }
                else {
                    UserApi.unblock(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                Broadcasting.sendBlock(user.getId(), true);
                                mAdapter.getItems().remove(position);
                                mAdapter.notifyDataSetChanged();
                            } else {
                                Notifications.showSnackbar(FollowshipAndBlockagesActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                }
            }
        }, mType);

    }

    private void updateUser(UserModel user, boolean request, boolean follow) {
        /*button.setBackgroundResource(follow ? R.drawable.main_button : R.drawable.bordered_button_main);
        button.setTextColor(follow ? ContextCompat.getColor(this, R.color.white) : ContextCompat.getColor(this, R.color.main));
        button.setText(follow ? R.string.following : request ? R.string.requested : R.string.follow);*/

        user.setIsFollowRequestSent(request);
        user.setIsFollowingUser(follow);
        mAdapter.notifyDataSetChanged();
    }

    private void initRecyclerView() {
        final String userId = getIntent().getStringExtra(USER_ID);
        mIsOtherUser = !(userId == null || userId.equals(User.getInstance().getId()));


        mRefreshRecyclerViewFragment.setIsLazyLoading(mType != TYPE_BLOCKED);
        mRefreshRecyclerViewFragment.setAdapter(mAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                if (mType == TYPE_FOLLOWERS)
                    UserApi.followers(userId, mRefreshRecyclerViewFragment.getMaxId(), mRefreshRecyclerViewFragment.getSinceId(), mRefreshRecyclerViewFragment.getAppender());
                else if (mType == TYPE_FOLLOWING)
                    UserApi.followings(userId, mRefreshRecyclerViewFragment.getMaxId(), mRefreshRecyclerViewFragment.getSinceId(), mRefreshRecyclerViewFragment.getAppender());
                else
                    UserApi.blocked(mRefreshRecyclerViewFragment.getAppender());
            }
        });

        mRefreshRecyclerViewFragment.refreshItems(null, null);
    }


}
