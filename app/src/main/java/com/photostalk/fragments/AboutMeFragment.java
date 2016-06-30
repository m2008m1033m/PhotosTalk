package com.photostalk.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.photostalk.FollowshipAndBlockagesActivity;
import com.photostalk.R;
import com.photostalk.models.UserModel;
import com.photostalk.apis.Result;
import com.photostalk.apis.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

/**
 * Created by mohammed on 3/6/16.
 */
public class AboutMeFragment extends Fragment {

    private View mView;
    private TextView mAboutMeTextView;
    private TextView mFollowersCountTextView;
    private TextView mFollowingCountTextView;
    private TextView mStoriesCount;
    private Button mFollowButton;

    private BroadcastReceiver mBroadcastReceiver;

    private UserModel mUser;
    private boolean mIsOtherUser;

    public AboutMeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.about_fragment, container, false);
        initReferences();
        setupBroadcastReceiver();
        return mView;
    }


    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    public void refresh(UserModel user) {
        if (user == null) return;
        mUser = user;
        mAboutMeTextView.setText(mUser.getBio() + "\n\n" + mUser.getWebsite());
        mStoriesCount.setText(String.valueOf(mUser.getStoriesCount()));
        //mFollowersCountTextView.setText(String.valueOf(mUser.getFollowersCount()));
        mFollowingCountTextView.setText(String.valueOf(mUser.getFollowingCount()));
        refreshUi();
    }

    public void setUser(UserModel user, boolean isOtherUser) {
        refresh(user);

        mIsOtherUser = isOtherUser;
        mFollowButton.setVisibility(isOtherUser ? View.VISIBLE : View.GONE);
        refreshUi();

        if (mIsOtherUser) {
            mFollowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mUser.isFollowingUser()) {
                        //unfollow
                        UserApi.unfollow(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    Broadcasting.sendFollow(mUser.getId(), false, false);
                                    mUser.setIsFollowingUser(false);
                                    refreshUi();
                                } else
                                    Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                            }
                        });
                    } else if (!mUser.isFollowRequestSent()) {
                        //follow user or request
                        if (mUser.isPrivate()) {
                            //request
                            UserApi.request(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                                @Override
                                public void onExecuted(Result result) {
                                    if (result.isSucceeded()) {
                                        Broadcasting.sendFollow(mUser.getId(), true, false);
                                        mFollowButton.setText(R.string.requested);
                                        mUser.setIsFollowRequestSent(true);
                                    } else
                                        Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                                }
                            });
                        } else {
                            //follow
                            UserApi.follow(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                                @Override
                                public void onExecuted(Result result) {
                                    if (result.isSucceeded()) {
                                        Broadcasting.sendFollow(mUser.getId(), false, true);
                                        mUser.setIsFollowingUser(true);
                                        refreshUi();
                                    } else
                                        Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                                }
                            });
                        }

                    } else {
                        // cancel request
                        UserApi.cancel(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                            @Override
                            public void onExecuted(Result result) {
                                if (result.isSucceeded()) {
                                    Broadcasting.sendFollow(mUser.getId(), false, false);
                                    mFollowButton.setText(R.string.follow);
                                    mUser.setIsFollowRequestSent(false);
                                } else
                                    Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                            }
                        });
                    }

                }
            });
        }

        mFollowersCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), FollowshipAndBlockagesActivity.class);
                i.putExtra(FollowshipAndBlockagesActivity.TYPE, FollowshipAndBlockagesActivity.TYPE_FOLLOWERS);
                i.putExtra(FollowshipAndBlockagesActivity.USER_ID, mUser.getId());
                startActivity(i);
            }
        });

        mFollowingCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), FollowshipAndBlockagesActivity.class);
                i.putExtra(FollowshipAndBlockagesActivity.TYPE, FollowshipAndBlockagesActivity.TYPE_FOLLOWING);
                i.putExtra(FollowshipAndBlockagesActivity.USER_ID, mUser.getId());
                startActivity(i);
            }
        });
    }

    private void refreshUi() {
        if (mIsOtherUser) {
            mFollowButton.setText(mUser.isFollowingUser() ? R.string.following : mUser.isFollowRequestSent() ? R.string.requested : R.string.follow);
            mFollowButton.setTextColor(ContextCompat.getColor(getActivity(), mUser.isFollowingUser() ? R.color.white : R.color.main));
            mFollowButton.setBackgroundResource(mUser.isFollowingUser() ? R.drawable.main_button : R.drawable.bordered_button_main);
            //if (mUser.isFollowRequestSent()) mFollowButton.setEnabled(false);
        }
        mFollowersCountTextView.setText(String.valueOf(mUser.getFollowersCount()));
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.FOLLOW)) {
                    /**
                     * get the params:
                     */
                    String userId = intent.getStringExtra("user_id");
                    if (!userId.equals(mUser.getId())) return;

                    boolean request = intent.getBooleanExtra("request", false);
                    boolean follow = intent.getBooleanExtra("follow", false);

                    if (!mIsOtherUser) {
                        if (follow) {
                            mUser.setFollowingCount(mUser.getFollowingCount() + 1);
                        } else if (!request) {
                            mUser.setFollowingCount(mUser.getFollowingCount() - 1);
                        }

                    } else {
                        mUser.setIsFollowRequestSent(request);
                        mUser.setIsFollowingUser(follow);
                        if (mUser.isFollowingUser())
                            mUser.setFollowersCount(mUser.getFollowersCount() + 1);
                        else if (!mUser.isFollowRequestSent())
                            mUser.setFollowersCount(mUser.getFollowersCount() - 1);

                    }
                    refresh(mUser);
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String userId = intent.getStringExtra("user_id");
                    if (!mUser.getId().equals(userId)) return;
                    mUser.setStoriesCount(mUser.getStoriesCount() - 1);
                    refresh(mUser);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.FOLLOW);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void initReferences() {
        mAboutMeTextView = ((TextView) mView.findViewById(R.id.about_text_view));
        mFollowersCountTextView = ((TextView) mView.findViewById(R.id.follower_count));
        mFollowingCountTextView = ((TextView) mView.findViewById(R.id.following_count));
        mStoriesCount = ((TextView) mView.findViewById(R.id.stories_count));
        mFollowButton = ((Button) mView.findViewById(R.id.follow_button));
    }
}
