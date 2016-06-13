package com.photostalk;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.photostalk.customViews.CameraView;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.SimpleOrientationListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mohammed on 2/20/16.
 */
public class CameraActivity extends AppCompatActivity {

    private final static int PERMISSION_REQUEST = 0;

    private final int PICK_IMAGE = 1;

    private final int PORTRAIT = 0;
    private final int LANDSCAPE_CW = 1;
    private final int LANDSCAPE_CCW = 2;

    private Camera mCamera;
    private CameraView mCameraView;

    private FloatingActionButton mTakePhotoFAB;
    private FloatingActionButton mSelectShotPhotoFAB;
    private FloatingActionButton mSelectGalleryPhotoFAB;

    private ImageView mPhoto1;
    private ImageView mPhoto2;
    private ImageView mPhoto3;

    private View mOverlay;

    private int mScreenPosition = PORTRAIT;
    private int mPreviousPosition = PORTRAIT;


    private BroadcastReceiver mBroadcastReceiver;
    private ArrayList<String> mPhotoPaths = new ArrayList<>();

    private int mPhotosNumber = 0;
    private int mNeededPermissions = 0;

    private boolean mHasStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

    }

    private void start() {
        if (mHasStarted) return;
        mHasStarted = true;
        setupBroadcastReceiver();
        init();
        setupRotationDetector();


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= 16)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

    }

    private void setupCamera() {
        mCamera = getCameraInstance();
        if (mCameraView == null) {
            mCameraView = new CameraView(this/*, mCamera*/);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mCameraView);
        }
        mCameraView.setCamera(mCamera);
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.PHOTO_POSTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        checkAndCreateDirectory();
        initReferences();
        initEvents();
    }

    private void initEvents() {


        mSelectGalleryPhotoFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_IMAGE);
            }
        });

        mSelectShotPhotoFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhotosNumber == 0) return;

                if (mPhotosNumber == 1) {
                    //String dirName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "PhotosTalk" + File.separator + "tmp";
                    String dirName = getCacheDir() + File.separator + "PhotosTalk" + File.separator + "tmp";
                    File dir = new File(dirName);
                    if (dir.isDirectory()) {
                        for (String file : dir.list()) {
                            if (file.endsWith("jpg")) {
                                Intent i = new Intent(CameraActivity.this, RecordTagFilterActivityNew.class);
                                i.putExtra(RecordTagFilterActivityNew.PHOTO_PATH, dirName + File.separator + file);
                                i.putExtra(RecordTagFilterActivityNew.IS_LIVE, true);
                                startActivity(i);
                                break;
                            }
                        }
                    }
                } else {
                    //TODO: start photo selection activity
                    startActivity(new Intent(CameraActivity.this, SelectBestPhotoActivity.class));
                }

                // when navigate, finish this activity since
                // we dont need it any more
            }
        });

        mTakePhotoFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTakePhotoFAB.setEnabled(false);
                mOverlay.setVisibility(View.VISIBLE);
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        try {

                            //String path = Environment.getExternalStorageDirectory() + File.separator + "PhotosTalk" + File.separator + "tmp" + File.separator + System.currentTimeMillis() + ".jpg";
                            String path = getCacheDir() + File.separator + "PhotosTalk" + File.separator + "tmp" + File.separator + System.currentTimeMillis() + ".jpg";

                            addPhotoPath(path);

                            File pictureFile = new File(path);

                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                            realImage = rotate(realImage);

                            boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();

                            getNextImageView().setImageBitmap(ThumbnailUtils.extractThumbnail(realImage, 100, 100));

                            mOverlay.setVisibility(View.GONE);
                            mTakePhotoFAB.setEnabled(true);
                            mCamera.startPreview();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        mPhoto3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhotoPaths.size() == 0) return;
                goToRecordFilterHashtagActivity(mPhotoPaths.get(0));
            }
        });

        mPhoto2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhotoPaths.size() < 2) return;
                goToRecordFilterHashtagActivity(mPhotoPaths.get(1));
            }
        });

        mPhoto1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhotoPaths.size() < 3) return;
                goToRecordFilterHashtagActivity(mPhotoPaths.get(2));
            }
        });

    }

    private void checkAndCreateDirectory() {
        //File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "PhotosTalk/tmp");
        File directory = new File(getCacheDir() + File.separator + "PhotosTalk/tmp");
        if (!directory.mkdirs()) {
            String[] children = directory.list();
            for (String aChildren : children) {
                new File(directory, aChildren).delete();
            }
        }
    }

    private void setupRotationDetector() {
        SimpleOrientationListener simpleOrientationListener = new SimpleOrientationListener(this) {
            @Override
            public void onSimpleOrientationChanged(int orientation) {
                mPreviousPosition = mScreenPosition;
                if (orientation == ROTATION_0) {
                    mScreenPosition = PORTRAIT;
                } else if (orientation == ROTATION_90) {
                    mScreenPosition = LANDSCAPE_CW;
                } else {
                    mScreenPosition = LANDSCAPE_CCW;
                }
                refreshViews();
            }
        };

        simpleOrientationListener.enable();
    }

    private Camera getCameraInstance() {
        releaseCameraAndPreview();
        Camera camera = null;
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            e.printStackTrace();
            /**
             * camera not available
             */
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.camera_is_not_available)).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    finish();
                }
            });
        }
        return camera;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseCameraAndPreview();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        releaseCameraAndPreview();
        if (mCameraView != null)
            mCameraView.setCamera(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        requestPermissions();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && data != null) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String photoPath = cursor.getString(columnIndex);
            cursor.close();

            goToRecordFilterHashtagActivity(photoPath);

        }
    }

    private Animation getAnimation() {

        int animationId;
        switch (mScreenPosition) {
            case PORTRAIT:
                animationId = (mPreviousPosition == LANDSCAPE_CCW) ? R.anim.rotate_270_to_0 : R.anim.rotate_90_to_0;
                break;
            case LANDSCAPE_CW:
                animationId = R.anim.rotate_0_to_90;
                break;
            case LANDSCAPE_CCW:
                animationId = R.anim.rotate_0_to_270;
                break;
            default:
                animationId = (mPreviousPosition == LANDSCAPE_CCW) ? R.anim.rotate_270_to_0 : R.anim.rotate_90_to_0;
        }

        Animation animation = AnimationUtils.loadAnimation(this, animationId);
        animation.setFillAfter(true);

        return animation;
    }

    private void initReferences() {
        mTakePhotoFAB = ((FloatingActionButton) findViewById(R.id.take_photo_fab));
        mSelectGalleryPhotoFAB = ((FloatingActionButton) findViewById(R.id.select_gallery_photo));
        mSelectShotPhotoFAB = ((FloatingActionButton) findViewById(R.id.select_shot_photo));
        mPhoto1 = ((ImageView) findViewById(R.id.photo_1));
        mPhoto2 = ((ImageView) findViewById(R.id.photo_2));
        mPhoto3 = ((ImageView) findViewById(R.id.photo_3));
        mOverlay = findViewById(R.id.overlay);
    }

    private void refreshViews() {
        mPhoto1.startAnimation(getAnimation());
        mPhoto2.startAnimation(getAnimation());
        mPhoto3.startAnimation(getAnimation());

        mTakePhotoFAB.startAnimation(getAnimation());
        mSelectShotPhotoFAB.startAnimation(getAnimation());
        mSelectGalleryPhotoFAB.startAnimation(getAnimation());
    }

    private Bitmap rotate(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();

        float degree = 0;

        if (mScreenPosition == PORTRAIT) {
            degree = 90;
        } else if (mScreenPosition == LANDSCAPE_CW) {
            degree = 180;
        }

        if (degree == 0) return bitmap;

        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    private ImageView getNextImageView() {
        mPhotosNumber++;
        if (mPhotosNumber - 1 == 0) {
            mPhoto3.setAlpha(1.0f);
            return mPhoto3;
        } else if (mPhotosNumber - 1 == 1) {
            mPhoto2.setAlpha(1.0f);
            mPhoto2.setImageBitmap(((BitmapDrawable) mPhoto3.getDrawable()).getBitmap());
            return mPhoto3;
        } else {
            if (mPhotosNumber - 1 == 2) {
                mPhoto1.setAlpha(1.0f);
            }
            mPhoto1.setImageBitmap(((BitmapDrawable) mPhoto2.getDrawable()).getBitmap());
            mPhoto2.setImageBitmap(((BitmapDrawable) mPhoto3.getDrawable()).getBitmap());
            return mPhoto3;
        }
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.CAMERA);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 16)
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        mNeededPermissions = permissionsNeeded.size();
        if (mNeededPermissions != 0)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[permissionsNeeded.size()]), PERMISSION_REQUEST);
        else {
            start();
            setupCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST:
                if (grantResults.length == mNeededPermissions) {
                    boolean finish = false;
                    for (int i = 0; i < mNeededPermissions; i++)
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            finish = true;
                            break;
                        }

                    if (finish) {
                        Toast.makeText(this, R.string.you_need_to_grant_all_requested_permissions, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        start();
                        setupCamera();
                    }

                } else {
                    Toast.makeText(this, R.string.you_need_to_grant_all_requested_permissions, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    private void addPhotoPath(String path) {
        mPhotoPaths.add(0, path);
        if (mPhotoPaths.size() > 3)
            mPhotoPaths.remove(mPhotoPaths.size() - 1);
    }

    private void goToRecordFilterHashtagActivity(String photoPath) {
        if (photoPath != null) {
            Intent i = new Intent(this, RecordTagFilterActivityNew.class);
            i.putExtra(RecordTagFilterActivityNew.PHOTO_PATH, photoPath);
            i.putExtra(RecordTagFilterActivityNew.IS_LIVE, true);
            startActivity(i);
        }
    }
}
