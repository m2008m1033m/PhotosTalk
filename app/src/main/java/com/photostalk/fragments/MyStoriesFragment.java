package com.photostalk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.photostalk.R;
import com.photostalk.StoryActivity;
import com.photostalk.adapters.ProfileActivityStoriesAdapter;
import com.photostalk.core.User;
import com.photostalk.models.Story;
import com.photostalk.apis.UserApi;

import java.util.Iterator;

/**
 * Created by mohammed on 3/20/16.
 */
public class MyStoriesFragment extends Fragment {

    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private ProfileActivityStoriesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.my_stories_fragment, container, false);
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getChildFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));

        mAdapter = new ProfileActivityStoriesAdapter(new ProfileActivityStoriesAdapter.OnActionListener() {
            @Override
            public void onStoryClicked(int position) {
                Story story = ((Story) mAdapter.getItems().get(position));
                Intent i = new Intent(getActivity(), StoryActivity.class);
                i.putExtra(StoryActivity.STORY_ID, story.getId());
                startActivity(i);
            }

            @Override
            public void onPlayStopButtonClicked(int position) {

            }
        });

        mRefreshRecyclerViewFragment.setAdapter(mAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                UserApi.stories(User.getInstance().getId(),
                        mRefreshRecyclerViewFragment.getSinceId(),
                        mRefreshRecyclerViewFragment.getMaxId(),
                        mRefreshRecyclerViewFragment.getAppender());
            }
        });

        mRefreshRecyclerViewFragment.refreshItems(null, null);

        return v;
    }


    public void removeStory(String storyId) {
        Iterator iterator = mAdapter.getItems().iterator();
        while (iterator.hasNext()) {
            Story story = ((Story) iterator.next());
            if (story.getId().equals(storyId)) {
                iterator.remove();
            }
        }
        mAdapter.notifyDataSetChanged();
    }
}
