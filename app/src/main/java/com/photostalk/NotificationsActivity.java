package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.photostalk.adapters.NotificationsFragmentAdapter;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.models.Notification;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

public class NotificationsActivity extends AppCompatActivity {


    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private NotificationsFragmentAdapter mNotificationsAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_activity);
        setTitle(R.string.notifications);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setupBroadcastReceiver();


        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        mRefreshRecyclerViewFragment.setIsLazyLoading(true);

        mNotificationsAdapter = new NotificationsFragmentAdapter(new NotificationsFragmentAdapter.OnActionListener() {
            @Override
            public void onAcceptButtonClicked(final int position) {
                String userId = ((Notification) mNotificationsAdapter.getItems().get(position)).getUser().getId();
                UserApi.accept(userId, new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {

                            mNotificationsAdapter.getItems().remove(position);
                            mNotificationsAdapter.notifyItemRemoved(position);
                            mRefreshRecyclerViewFragment.refreshItems(mRefreshRecyclerViewFragment.getSinceId(), null);

                        } else {
                            Notifications.showSnackbar(NotificationsActivity.this, result.getMessages().get(0));
                        }
                    }
                });
            }

            @Override
            public void onRejectButtonClicked(final int position) {
                String userId = ((Notification) mNotificationsAdapter.getItems().get(position)).getUser().getId();
                UserApi.reject(userId, new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {

                            mNotificationsAdapter.getItems().remove(position);
                            mNotificationsAdapter.notifyItemRemoved(position);
                            mRefreshRecyclerViewFragment.refreshItems(mRefreshRecyclerViewFragment.getSinceId(), null);

                        } else {
                            Notifications.showSnackbar(NotificationsActivity.this, result.getMessages().get(0));
                        }
                    }
                });
            }

            @Override
            public void onFollowButtonClicked(int position) {
                UserModel user = ((Notification) mNotificationsAdapter.getItems().get(position)).getUser();
                follow(user, mNotificationsAdapter, position);
            }

            @Override
            public void onUserClicked(int position) {
                UserModel user = ((Notification) mNotificationsAdapter.getItems().get(position)).getUser();
                if (user == null) return;
                goToUser(user.getId());
            }

            @Override
            public void onItemClicked(int position) {
                Notification notification = ((Notification) mNotificationsAdapter.getItems().get(position));
                if (notification.getType() == Notification.Type.COMMENT) {
                    String photoId = notification.getPhoto().getId();
                    Intent i = new Intent(NotificationsActivity.this, PhotoActivity.class);
                    i.putExtra(PhotoActivity.PHOTO_ID, photoId);
                    startActivity(i);
                } else {
                    goToUser(notification.getUser().getId());
                }
            }
        });
        mRefreshRecyclerViewFragment.setAdapter(mNotificationsAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                UserApi.notifications(
                        null,
                        mRefreshRecyclerViewFragment.getMaxId(),
                        mRefreshRecyclerViewFragment.getSinceId(),
                        mRefreshRecyclerViewFragment.getAppender()
                );
            }
        });
        mRefreshRecyclerViewFragment.refreshItems(null, null);

        UserApi.seenNotifications(new ApiListeners.OnActionExecutedListener() {
            @Override
            public void onExecuted(Result result) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
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

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.FOLLOW)) {
                    String userId = intent.getStringExtra("user_id");
                    boolean request = intent.getBooleanExtra("request", false);
                    boolean follow = intent.getBooleanExtra("follow", false);
                    if (userId == null) return;
                    if (mNotificationsAdapter != null)
                        mNotificationsAdapter.updateUser(userId, request, follow);

                } else if (intent.getAction().equals(Broadcasting.LOGOUT))
                    finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.FOLLOW);
        intentFilter.addAction(Broadcasting.LOGOUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void goToUser(String userId) {
        Intent i = new Intent(this, ProfileActivity.class);
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
                        Broadcasting.sendFollow(NotificationsActivity.this, user.getId(), false, false);
                    } else {
                        Notifications.showSnackbar(NotificationsActivity.this, result.getMessages().get(0));
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
                        Broadcasting.sendFollow(NotificationsActivity.this, user.getId(), true, false);
                    } else {
                        Notifications.showSnackbar(NotificationsActivity.this, result.getMessages().get(0));
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
                        Broadcasting.sendFollow(NotificationsActivity.this, user.getId(), false, true);
                    } else {
                        Notifications.showSnackbar(NotificationsActivity.this, result.getMessages().get(0));
                    }
                }
            });
        }
    }

}
