package com.photostalk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.photostalk.ProfileActivity;
import com.photostalk.R;
import com.photostalk.SearchActivity;
import com.photostalk.StoryActivity;
import com.photostalk.adapters.TrendingFragmentAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.models.Story;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.StoriesApi;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;


public class TrendingFragment extends Fragment {
    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private TrendingFragmentAdapter mTrendingAdapterTest;

    public TrendingFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.trending_fragment, container, false);
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getChildFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
        view.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });
        setupTrendingFragment();
        return view;
    }

    public void updateUser(String userId, boolean request, boolean follow) {
        mTrendingAdapterTest.updateUser(userId, request, follow);
    }

    public void removeStory(String storyId) {
        mTrendingAdapterTest.removeStory(storyId);
    }

    public void updateUserPhoto() {
        mTrendingAdapterTest.updateUserPhoto();
    }

    private void setupTrendingFragment() {
        mTrendingAdapterTest = new TrendingFragmentAdapter(new TrendingFragmentAdapter.OnActionListener() {
            @Override
            public void onFollowButtonClicked(int position) {
                final UserModel user = mTrendingAdapterTest.getUser(position);
                if (user == null) return;
                if (user.isFollowingUser())
                    UserApi.unfollow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowingUser(false);
                                mTrendingAdapterTest.notifyDataSetChanged();
                                Broadcasting.sendFollow(getActivity(), user.getId(), false, false);
                            } else {
                                Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                            }
                        }
                    });
                else if (user.isPrivate()) {
                    UserApi.request(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowRequestSent(true);
                                mTrendingAdapterTest.notifyDataSetChanged();
                                Broadcasting.sendFollow(getActivity(), user.getId(), true, false);
                            } else {
                                Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                            }
                        }
                    });
                } else {
                    UserApi.follow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowingUser(true);
                                mTrendingAdapterTest.notifyDataSetChanged();
                                Broadcasting.sendFollow(getActivity(), user.getId(), false, true);
                            } else {
                                Notifications.showSnackbar(getActivity(), result.getMessages().get(0));
                            }
                        }
                    });
                }
            }

            @Override
            public void onUserClicked(int position) {
                UserModel userModel = mTrendingAdapterTest.getUser(position);
                if (userModel == null) return;
                Intent i = new Intent(getActivity(), ProfileActivity.class);
                i.putExtra(ProfileActivity.USER_ID, userModel.getId());
                startActivity(i);
            }

            @Override
            public void onStoryClicked(int position) {
                Story story = mTrendingAdapterTest.getStory(position);
                if (story == null) return;
                Intent i = new Intent(getActivity(), StoryActivity.class);
                i.putExtra(StoryActivity.STORY_ID, story.getId());
                startActivity(i);
            }
        });
        mRefreshRecyclerViewFragment.setIsLazyLoading(false);
        mRefreshRecyclerViewFragment.setAdapter(mTrendingAdapterTest, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                Communicator.getInstance().cancelByTag("story_trending");
                StoriesApi.trending(mRefreshRecyclerViewFragment.getAppender());
            }
        });
        mRefreshRecyclerViewFragment.refreshItems(null, null);
    }

}
