package com.photostalk.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.photostalk.R;
import com.photostalk.adapters.RefreshAdapter;
import com.photostalk.models.Model;
import com.photostalk.apis.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Notifications;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/4/16.
 */
public class RefreshRecyclerViewFragment extends Fragment {

    public interface ServiceWrapper {
        void executeService();
    }

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private RefreshAdapter mAdapter;
    private Runnable mRefreshRunnable;
    ApiListeners.OnItemsArrayLoadedListener mOnItemsArrayLoadedListener;
    ServiceWrapper mServiceWrapper;

    private String mFirstItemId;
    private String mLastItemId;
    private boolean mIsRefreshing = false;
    private boolean mIsLazyLoading = false;
    private boolean mKeepLoading = true;
    private boolean mIsLoading = true;
    private String mSinceId;
    private String mMaxId;

    public RefreshRecyclerViewFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.refresh_recyclerview_fragment, container, false);

        mRecyclerView = ((RecyclerView) view.findViewById(R.id.recycler_view));
        mSwipeRefreshLayout = ((SwipeRefreshLayout) view.findViewById(R.id.refresh_layout));

        init();
        return view;
    }

    public void setAdapter(RefreshAdapter adapter, ServiceWrapper serviceWrapper) {
        mAdapter = adapter;
        mServiceWrapper = serviceWrapper;

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        mRecyclerView.setAdapter(mAdapter);

        /**
         * init an event listener to know
         * if the recycler view is at the bottom.
         * if so, load new comments
         */
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || !mKeepLoading || mIsLoading || !mIsLazyLoading) return;
                LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                int numberOfVisibleItems = layoutManager.getChildCount();
                int numberOfTotalItems = layoutManager.getItemCount();
                int orderOfFirstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if (numberOfVisibleItems + orderOfFirstVisibleItem >= numberOfTotalItems)
                    refreshItems(null, mLastItemId);
            }
        });

        //if (!refreshable)
        //    mSwipeRefreshLayout.setEnabled(false);

    }

    public void setIsLazyLoading(boolean isLazyLoading) {
        if (isLazyLoading == mIsLazyLoading) return;
        mIsLazyLoading = isLazyLoading;
        mFirstItemId = null;
        mLastItemId = null;
        mSinceId = null;
        mMaxId = null;
    }

    public String getFirstItemId() {
        return mFirstItemId;
    }

    public String getLastItemId() {
        return mLastItemId;
    }

    public String getSinceId() {
        return mSinceId;
    }

    public String getMaxId() {
        return mMaxId;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void refreshItems(@Nullable final String sinceId, @Nullable String maxId) {
        setRefreshing(true);
        mSinceId = sinceId;
        mMaxId = maxId;
        mIsLoading = true;
        mServiceWrapper.executeService();
    }

    public ApiListeners.OnItemsArrayLoadedListener getAppender() {
        return mOnItemsArrayLoadedListener;
    }

    private void init() {
        initAppender();
        /**
         * initialize the runnable that will
         * refresh the RefreshLayout
         */
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            }
        };

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshItems(mFirstItemId, null);
            }
        });
    }

    private void initAppender() {
        mOnItemsArrayLoadedListener = new ApiListeners.OnItemsArrayLoadedListener() {
            @Override
            public void onLoaded(Result result, ArrayList<Model> items) {
                if (result.isSucceeded()) {

                    /**
                     * if we set the refreshing to false
                     * just refresh the list without max and min
                     */
                    if (!mIsLazyLoading) {
                        mAdapter.getItems().clear();
                        int len = items.size();
                        for (int i = 0; i < len; i++)
                            mAdapter.getItems().add(items.get(i));
                        /**
                         * notify
                         */
                        mAdapter.notifyDataSetChanged();
                        mIsLoading = false;
                        setRefreshing(false);
                        return;
                    }

                    int len = items.size();
                    /**
                     * if we are requesting newer
                     * comments, append them to the top
                     */
                    if (mSinceId != null) {
                        for (int i = len - 1; i >= 0; i--)
                            mAdapter.getItems().add(0, items.get(i));

                        /**
                         * update the value of the first element
                         * if there are values
                         */
                        if (len != 0)
                            mFirstItemId = items.get(0).getId();
                    }

                    /**
                     * anything else just append to
                     * the end
                     */
                    else {
                        for (int i = 0; i < len; i++)
                            mAdapter.getItems().add(items.get(i));

                        /**
                         * update the last comment id
                         */
                        if (len > 0) mLastItemId = items.get(len - 1).getId();

                        /**
                         * for the first time, update the first comment id
                         */
                        if (len > 0 && mFirstItemId == null)
                            mFirstItemId = items.get(0).getId();

                        /**
                         * id nothing returned, don't load any more
                         */
                        if (items.size() == 0)
                            mKeepLoading = false;
                    }

                    /**
                     * notify
                     */
                    mAdapter.notifyDataSetChanged();
                    mIsLoading = false;
                    mSinceId = null;
                    mMaxId = null;

                } else {
                    Notifications.showSnackbar(RefreshRecyclerViewFragment.this.getActivity(), result.getMessages().get(0));
                }
                setRefreshing(false);
            }
        };
    }

    public void setRefreshing(boolean refresh) {
        mIsRefreshing = refresh;
        mSwipeRefreshLayout.post(mRefreshRunnable);
    }

}
