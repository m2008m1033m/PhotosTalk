package com.photostalk.services;

import android.graphics.Bitmap;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.models.BestPhoto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by mohammed on 2/24/16.
 */
public class Local {

    public static ArrayList<BestPhoto> getTmpTakenPhotos() {
        ArrayList<BestPhoto> photos = new ArrayList<>();

        //String dirStr = Environment.getExternalStorageDirectory() + File.separator + "PhotosTalk" + File.separator + "tmp";
        String dirStr = PhotosTalkApplication.getContext().getCacheDir() + File.separator + "PhotosTalk" + File.separator + "tmp";

        File tmpDir = new File(dirStr);

        for (String fileName : tmpDir.list()) {
            if (fileName.trim().endsWith("jpg")) {
                BestPhoto item = new BestPhoto();
                item.setIsSelected(false);
                item.setPath(dirStr + File.separator + fileName);
                photos.add(item);
            }
        }

        return photos;
    }

    public static String savePhoto(Bitmap bitmap, String dir) {
        try {
            new File(dir).mkdir();

            String path = dir + System.currentTimeMillis() + ".jpg";
            File pictureFile = new File(path);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
