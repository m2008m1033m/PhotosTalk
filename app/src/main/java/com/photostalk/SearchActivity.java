package com.photostalk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.photostalk.adapters.SearchActivityAdapter;
import com.photostalk.core.Communicator;
import com.photostalk.fragments.RefreshRecyclerViewFragment;
import com.photostalk.models.Story;
import com.photostalk.models.UserModel;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

import java.util.List;


public class SearchActivity extends AppCompatActivity {

    public static String SEARCH_TERM = "search_term";
    public static String SEARCH_TYPE = "search_type";
    public static int SEARCH_TYPE_USER = 0;
    public static int SEARCH_TYPE_STORY = 1;

    private static final int SPEECH_REQUEST_CODE = 0;

    private ImageButton mBackButton;
    private EditText mSearchText;
    private ImageButton mMicButton;
    private TabLayout mTabLayout;

    private SearchActivityAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;


    private RefreshRecyclerViewFragment mRefreshRecyclerViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.search_activity);
        init();

        /**
         * check if there is a search:
         */
        String searchTerm = getIntent().getStringExtra(SEARCH_TERM);
        if (searchTerm == null) return;
        int searchType = getIntent().getIntExtra(SEARCH_TYPE, SEARCH_TYPE_STORY);
        TabLayout.Tab tab = mTabLayout.getTabAt(searchType);
        if (tab != null)
            tab.select();
        mSearchText.setText(searchTerm);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        loadItems();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.FOLLOW)) {
                    String userId = intent.getStringExtra("user_id");
                    boolean request = intent.getBooleanExtra("request", false);
                    boolean follow = intent.getBooleanExtra("follow", false);
                    updateUser(userId, request, follow);
                } else if (intent.getAction().equals(Broadcasting.STORY_DELETE)) {
                    String storyId = intent.getStringExtra("story_id");
                    removeStory(storyId);
                } else if (intent.getAction().equals(Broadcasting.LOGOUT))
                    finish();
                else if (intent.getAction().equals(Broadcasting.PROFILE_UPDATED)) {
                    updateUserPhoto();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.FOLLOW);
        intentFilter.addAction(Broadcasting.STORY_DELETE);
        intentFilter.addAction(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    public void updateUser(String userId, boolean request, boolean follow) {
        mAdapter.updateUser(userId, request, follow);
    }

    public void removeStory(String storyId) {
        mAdapter.removeStory(storyId);
    }

    private void init() {
        initReferences();
        setupAdapter();
        setupTabLayout();
        initEvents();
    }

    private void initReferences() {
        mBackButton = ((ImageButton) findViewById(R.id.back_button));
        mSearchText = ((EditText) findViewById(R.id.search_text));
        mMicButton = ((ImageButton) findViewById(R.id.mic_button));
        mTabLayout = ((TabLayout) findViewById(R.id.tabs));
        mRefreshRecyclerViewFragment = ((RefreshRecyclerViewFragment) getSupportFragmentManager().findFragmentById(R.id.refresh_recycler_view_fragment));
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


        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Communicator.getInstance().cancelByTag("user_search");
                Communicator.getInstance().cancelByTag("photo_hashtag");

                mRefreshRecyclerViewFragment.setRefreshing(false);

                mAdapter.getItems().clear();
                mAdapter.notifyDataSetChanged();
                mRefreshRecyclerViewFragment.setIsLazyLoading(HomeActivity.TRENDING == getType());

                loadItems();
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
        Communicator.getInstance().cancelByTag("user_search");
        Communicator.getInstance().cancelByTag("photo_hashtag");

        //if (mSearchingTerm == null || mSearchingTerm.isEmpty()) return false;
        if (mSearchText.getText().toString().trim().isEmpty()) return false;
        if (getType() == HomeActivity.USER) {
            mRefreshRecyclerViewFragment.setIsLazyLoading(false);
            mAdapter.getItems().clear();
            mAdapter.notifyDataSetChanged();
            UserApi.search(mSearchText.getText().toString().trim(), mRefreshRecyclerViewFragment.getAppender());
        } else {
            mRefreshRecyclerViewFragment.setIsLazyLoading(true);
            PhotosApi.searchHashtag(mSearchText.getText().toString().trim(),
                    mRefreshRecyclerViewFragment.getMaxId(),
                    mRefreshRecyclerViewFragment.getSinceId(),
                    mRefreshRecyclerViewFragment.getAppender());
        }

        return true;
    }

    public void updateUserPhoto() {
        mAdapter.updateUserPhoto();
    }

    private void setupAdapter() {
        mAdapter = new SearchActivityAdapter(this, new SearchActivityAdapter.OnActionListener() {
            @Override
            public void onFollowButtonClicked(int position) {
                final UserModel user = mAdapter.getUser(position);
                if (user == null) return;
                if (user.isFollowingUser())
                    UserApi.unfollow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowingUser(false);
                                mAdapter.notifyDataSetChanged();
                                Broadcasting.sendFollow(SearchActivity.this, user.getId(), false, false);
                            } else {
                                Notifications.showSnackbar(SearchActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                else if (user.isPrivate()) {
                    UserApi.request(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowRequestSent(true);
                                mAdapter.notifyDataSetChanged();
                                Broadcasting.sendFollow(SearchActivity.this, user.getId(), true, false);
                            } else {
                                Notifications.showSnackbar(SearchActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                } else {
                    UserApi.follow(user.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                user.setIsFollowingUser(true);
                                mAdapter.notifyDataSetChanged();
                                Broadcasting.sendFollow(SearchActivity.this, user.getId(), false, true);
                            } else {
                                Notifications.showSnackbar(SearchActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                }
            }

            @Override
            public void onUserClicked(int position) {
                UserModel userModel = mAdapter.getUser(position);
                if (userModel == null) return;
                Intent i = new Intent(SearchActivity.this, ProfileActivity.class);
                i.putExtra(ProfileActivity.USER_ID, userModel.getId());
                startActivity(i);
            }

            @Override
            public void onStoryClicked(int position) {
                Story story = mAdapter.getStory(position);
                if (story == null) return;
                Intent i = new Intent(SearchActivity.this, StoryActivity.class);
                i.putExtra(StoryActivity.STORY_ID, story.getId());
                startActivity(i);
            }
        });


        mRefreshRecyclerViewFragment.setAdapter(mAdapter, new RefreshRecyclerViewFragment.ServiceWrapper() {
            @Override
            public void executeService() {
                loadItems();
            }
        });
    }
}
