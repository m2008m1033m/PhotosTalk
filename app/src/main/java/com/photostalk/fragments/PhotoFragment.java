package com.photostalk.fragments;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.photostalk.R;
import com.photostalk.models.Model;
import com.photostalk.models.Photo;
import com.photostalk.services.PhotosApi;
import com.photostalk.services.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Notifications;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class PhotoFragment extends Fragment {

    public interface OnActionListener {
        void onPhotoLoaded(int id, Photo photo);

        void onSwipped();
    }

    private View mView;
    private ProgressBar mProgressBar;
    private SubsamplingScaleImageView mPhotoImageView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Photo mPhoto;
    private int mId;

    private OnActionListener mOnActionListener;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.photo_fragment, container, false);
        mPhotoImageView = ((SubsamplingScaleImageView) mView.findViewById(R.id.photo));
        mProgressBar = ((ProgressBar) mView.findViewById(R.id.progress));
        mSwipeRefreshLayout = ((SwipeRefreshLayout) mView.findViewById(R.id.wrapper));


        mPhotoImageView.setOnImageEventListener(new SubsamplingScaleImageView.OnImageEventListener() {
            @Override
            public void onReady() {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onImageLoaded() {

            }

            @Override
            public void onPreviewLoadError(Exception e) {

            }

            @Override
            public void onImageLoadError(Exception e) {

            }

            @Override
            public void onTileLoadError(Exception e) {

            }
        });

        return mView;
    }

    /**
     * should be called by overriding the onViewCreated() method and called inside it
     */
    public void setPhotoId(String photoId, int id, OnActionListener onActionListener) {
        mId = id;
        mOnActionListener = onActionListener;
        PhotosApi.get(photoId, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {
                    mPhoto = (Photo) item;

                    /**
                     * notify the activity that the photo is loaded
                     */
                    mOnActionListener.onPhotoLoaded(mId, mPhoto);


                    /**
                     * event for listening for the pull to refresh
                     */
                    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            mOnActionListener.onSwipped();
                        }
                    });

                    /**
                     * the actual photo
                     */
                    loadPhoto();


                } else {
                    Notifications.showListAlertDialog(getActivity(), getString(R.string.error), result.getMessages()).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            getActivity().finish();
                        }
                    });

                }
            }
        });
    }

    public void stopLoadingUI() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void loadPhoto() {
        mProgressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String url = mPhoto.getImageOriginalUrl();
                    String[] bits = url.split("/");
                    String filename = bits[bits.length - 1];
                    URL photoURL = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) photoURL.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream is = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    FileOutputStream fileOutputStream = new FileOutputStream(getActivity().getCacheDir() + filename);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                    return filename;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String filename) {
                super.onPostExecute(filename);
                if (filename == null) return;
                mPhotoImageView.setImage(ImageSource.uri(getActivity().getCacheDir() + filename));
                mPhotoImageView.setMinimumDpi(10);
            }
        }.execute();

    }


    public int getFragmentId() {
        return mId;
    }

    public Photo getPhoto() {
        return mPhoto;
    }
}
