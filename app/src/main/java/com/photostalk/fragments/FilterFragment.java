package com.photostalk.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.photostalk.R;
import com.photostalk.customListeners.OnSwipeTouchListener;
import com.photostalk.filters.IFAmaroFilter;
import com.photostalk.filters.IFEarlybirdFilter;
import com.photostalk.filters.IFXprollFilter;

import java.io.File;
import java.io.FileOutputStream;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSepiaFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class FilterFragment extends Fragment {

    public interface OnActionListener {
        void onPhotoSaved();
    }

    private final int TOTAL_FILTERS = 7;

    private View mView;
    private GPUImageView mGPUImageView;

    private String mPhotoPath;

    private int mCurrentFilter = 0;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //mPhotoPath = "/storage/emulated/0/DCIM/Camera/test.jpg";
        //mPhotoPath = "/storage/emulated/0/DCIM/Camera/20160421_215310.jpg";
        mView = inflater.inflate(R.layout.filter_fragment, container, false);

        init();
        return mView;
    }

    public void setPhoto(String photoPath) {
        mPhotoPath = photoPath;
        fillFields();
        initEvents();
    }

    public String saveFilteredPhoto() {
        String fileName = getContext().getCacheDir().toString() + "/filtered.jpg";
        try {
            Bitmap bitmap = mGPUImageView.getGPUImage().getBitmapWithFilterApplied();
            FileOutputStream out = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileName;
    }

    private void init() {
        initReferences();
    }

    private void initReferences() {
        mGPUImageView = ((GPUImageView) mView.findViewById(R.id.photo));
    }

    private void initEvents() {
        mGPUImageView.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            @Override
            public void onSwipeTop() {
                if (mCurrentFilter == 0)
                    mCurrentFilter = TOTAL_FILTERS - 1;
                else
                    mCurrentFilter--;

                changeFilter();
            }

            @Override
            public void onSwipeBottom() {
                if (mCurrentFilter + 1 == TOTAL_FILTERS)
                    mCurrentFilter = 0;
                else
                    mCurrentFilter++;
                changeFilter();
            }
        });
    }

    private void fillFields() {
        mGPUImageView.setImage(new File(mPhotoPath));
        mGPUImageView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
    }

    private void changeFilter() {
        switch (mCurrentFilter) {
            case 0:
                mGPUImageView.setFilter(normal());
                break;
            case 1:
                mGPUImageView.setFilter(greyScale());
                break;
            case 2:
                mGPUImageView.setFilter(sepia());
                break;
            case 3:
                mGPUImageView.setFilter(loFi());
                break;
            case 4:
                mGPUImageView.setFilter(amaro());
                break;
            case 5:
                mGPUImageView.setFilter(earlyBird());
                break;
            case 6:
                mGPUImageView.setFilter(xProII());
                break;
        }

    }

    private GPUImageFilter normal() {
        return new GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, GPUImageFilter.NO_FILTER_FRAGMENT_SHADER);
    }

    private GPUImageFilter greyScale() {
        return new GPUImageGrayscaleFilter();
    }

    private GPUImageFilter sepia() {
        return new GPUImageSepiaFilter();
    }

    private GPUImageFilter loFi() {
        GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
        filterGroup.addFilter(new GPUImageBrightnessFilter(0.2f));
        filterGroup.addFilter(new GPUImageContrastFilter(2.3f));
        return filterGroup;
    }

    private GPUImageFilter amaro() {
        return new IFAmaroFilter(getContext());
    }

    private GPUImageFilter earlyBird() {
        return new IFEarlybirdFilter(getContext());
    }

    private GPUImageFilter xProII() {
        return new IFXprollFilter(getContext());
    }

}
