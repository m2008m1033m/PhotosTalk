package com.photostalk.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.photostalk.HomeActivity;
import com.photostalk.ProfileActivity;
import com.photostalk.R;
import com.photostalk.StoryActivity;
import com.photostalk.adapters.TrendingFragmentAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.models.Story;
import com.photostalk.models.UserModel;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.services.StoriesApi;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

import java.util.List;

/**
 * Created by mohammed on 3/10/16.
 */
public class TrendingFragment extends Fragment {
    private static final int SPEECH_REQUEST_CODE = 0;

    private View mView;
    private FrameLayout mSearchButtonContainer;
    private ImageButton mSearchButton;
    private LinearLayout mSearchArea;
    private ImageButton mBackButton;
    private EditText mSearchText;
    private ImageButton mMicButton;
    private TabLayout mTabLayout;

    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;
    private TrendingFragmentAdapter mTrendingAdapterTest;
    private boolean mIsSearching = false;
    //private String mSearchingTerm;

    public TrendingFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.trending_fragment, container, false);
        init();
        return mView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            mSearchText.setText(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public int getType() {
        return mTabLayout.getSelectedTabPosition();
    }

    public boolean isSearching() {
        return mIsSearching;
    }

    public void updateUser(String userId, boolean request, boolean follow) {
        mTrendingAdapterTest.updateUser(userId, request, follow);
    }

    public void removeStory(String storyId) {
        mTrendingAdapterTest.removeStory(storyId);
    }

    private void init() {
        initReferences();
        setupTrendingFragment();
        setupTabLayout();
        initEvents();
        setSearching(false);
    }

    private void initReferences() {
        mSearchButtonContainer = ((FrameLayout) mView.findViewById(R.id.search_button_container));
        mSearchButton = ((ImageButton) mView.findViewById(R.id.search_button));
        mSearchArea = ((LinearLayout) mView.findViewById(R.id.search_area));
        mBackButton = ((ImageButton) mView.findViewById(R.id.back_button));
        mSearchText = ((EditText) mView.findViewById(R.id.search_text));
        mMicButton = ((ImageButton) mView.findViewById(R.id.mic_button));
        mTabLayout = ((TabLayout) mView.findViewById(R.id.tabs));
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getChildFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
    }

    private void setupTabLayout() {
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.username_all_caps));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.hashtag_all_caps));
    }

    private void initEvents() {
        mMicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                // Start the activity, the intent will be populated with the speech text
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSearching(true);
                mSearchText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mSearchText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSearching(false);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                mSearchText.setText("");
                //mSearchingTerm = null;
            }
        });


        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setSearching(true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        /**
         * event for the search
         */
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    /**
                     * perform the searching
                     */
                    //mSearchingTerm = mSearchText.getText().toString().trim();
                    if (!loadItems()) return true;
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (getType() == HomeActivity.USER) {
                    //mSearchingTerm = mSearchText.getText().toString().trim();
                    loadItems();
                }
            }
        });
    }

    private boolean loadItems() {
        Communicator.getInstance().cancelByTag("story_trending");
        Communicator.getInstance().cancelByTag("user_search");
        Communicator.getInstance().cancelByTag("photo_hashtag");
        if (!mIsSearching) {
            mRefreshRecyclerViewFragment.setIsLazyLoading(false);
            mTrendingAdapterTest.clearAll();
            StoriesApi.trending(mRefreshRecyclerViewFragment.getAppender());
        } else {
            //if (mSearchingTerm == null || mSearchingTerm.isEmpty()) return false;
            if (mSearchText.getText().toString().trim().isEmpty()) return false;
            if (getType() == HomeActivity.USER) {
                mRefreshRecyclerViewFragment.setIsLazyLoading(false);
                mTrendingAdapterTest.clearAll();
                UserApi.search(mSearchText.getText().toString().trim(), mRefreshRecyclerViewFragment.getAppender());
            } else {
                mRefreshRecyclerViewFragment.setIsLazyLoading(true);
                PhotosApi.searchHashtag(mSearchText.getText().toString().trim(),
                        mRefreshRecyclerViewFragment.getMaxId(),
                        mRefreshRecyclerViewFragment.getSinceId(),
                        mRefreshRecyclerViewFragment.getAppender());
            }
        }

        return true;
    }

    public void setSearching(boolean searching) {
        Communicator.getInstance().cancelByTag("user_search");
        Communicator.getInstance().cancelByTag("photo_hashtag");
        Communicator.getInstance().cancelByTag("story_trending");

        mIsSearching = searching;
        mRefreshRecyclerViewFragment.setRefreshing(false);

        mTrendingAdapterTest.clearAll();
        mRefreshRecyclerViewFragment.setIsLazyLoading(HomeActivity.TRENDING == getType());

        mSearchButtonContainer.setVisibility(searching ? View.GONE : View.VISIBLE);
        mSearchArea.setVisibility(searching ? View.VISIBLE : View.GONE);

        loadItems();
    }

    public void updateUserPhoto() {
        mTrendingAdapterTest.updateUserPhoto();
    }

    private void setupTrendingFragment() {
        mTrendingAdapterTest = new TrendingFragmentAdapter(this, new TrendingFragmentAdapter.OnActionListener() {
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


        mRefreshRecyclerViewFragment.setAdapter(mTrendingAdapterTest, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                loadItems();
            }
        });
    }

}
