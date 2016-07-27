package com.photostalk.models;

/**
 * Created by mohammed on 2/24/16.
 */
public class BestPhoto extends Model {

    private String mPath;
    private boolean isSelected;

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    @Override
    public void copyFrom(Model model) {
        BestPhoto bestPhoto = ((BestPhoto) model);
        setPath(bestPhoto.getPath());
        setIsSelected(bestPhoto.isSelected());
    }
}
