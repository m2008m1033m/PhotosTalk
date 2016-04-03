package com.photostalk;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.photostalk.core.User;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.soundcloud.android.crop.Crop;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Created by mohammed on 3/6/16.
 */
public class UpdateUserActivity extends AppCompatActivity {

    private final static int PICK_IMAGE = 0;
    private final static int PIC_CROP = 1;

    private EditText mUserName;
    private EditText mBio;
    private EditText mWebsite;
    private EditText mMobile;
    private RadioGroup mGender;
    private RadioButton mMale;
    private RadioButton mFemale;
    private ImageView mPhoto;
    private CheckBox mPrivate;
    private Button mUpdateButton;

    private BroadcastReceiver mBroadcastReceiver;

    private UserModel mUser;
    private String mPhotoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.update_user_profile);
        setTitle(getString(R.string.edit_profile));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mUser == null) return true;
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.LOGOUT))
                    finish();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Broadcasting.LOGOUT));
    }

    private void init() {
        initReferences();
        fillFields();
    }

    private void initReferences() {
        mUserName = ((EditText) findViewById(R.id.full_name));
        mBio = ((EditText) findViewById(R.id.bio));
        mWebsite = ((EditText) findViewById(R.id.website));
        mMobile = ((EditText) findViewById(R.id.mobile));
        mGender = ((RadioGroup) findViewById(R.id.gender));
        mMale = ((RadioButton) findViewById(R.id.male));
        mFemale = ((RadioButton) findViewById(R.id.female));
        mPhoto = ((ImageView) findViewById(R.id.photo));
        mPrivate = ((CheckBox) findViewById(R.id.make_private));
        mUpdateButton = ((Button) findViewById(R.id.update_button));
    }

    private void fillFields() {
        final AlertDialog progressDialog = Notifications.showLoadingDialog(this, getString(R.string.loading));
        UserApi.get(null, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {

                    mUser = ((UserModel) item);
                    mUserName.setText(mUser.getName());
                    mBio.setText(mUser.getBio());
                    mWebsite.setText(mUser.getWebsite());
                    mMobile.setText(mUser.getMobile());
                    if (mUser.getGender().equals("m")) {
                        mMale.setChecked(true);
                    } else if (mUser.getGender().equals("f")) {
                        mFemale.setChecked(true);
                    }
                    if (!mUser.getPhoto().isEmpty())
                        Picasso.with(UpdateUserActivity.this)
                                .load(mUser.getPhoto())
                                .into(mPhoto);
                    mPrivate.setChecked(mUser.isPrivate());

                    initEvents();

                } else {
                    Notifications.showListAlertDialog(UpdateUserActivity.this, getString(R.string.error), result.getMessages()).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                        }
                    });
                }
                progressDialog.dismiss();
            }
        });
    }

    private void initEvents() {
        mPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean isChecked) {
                UserApi.makePrivate(isChecked ? "1" : "0", new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            User.getInstance().setIsPrivate(isChecked);
                            User.getInstance().update();
                        } else
                            Notifications.showSnackbar(UpdateUserActivity.this, result.getMessages().get(0));
                    }
                });
            }
        });

        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performUpdate();
            }
        });

        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_IMAGE);
            }
        });
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
            Crop.of(Uri.fromFile(new File(cursor.getString(columnIndex))), Uri.fromFile(new File(getCacheDir(), "cropped.jpg"))).asSquare().start(this);
            cursor.close();
        } else if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            mPhotoPath = Crop.getOutput(data).getPath();
            Picasso.with(this)
                    .load(new File(mPhotoPath))
                    .skipMemoryCache()
                    .into(mPhoto);
            performUpdate();
        }
    }

    private void performUpdate() {
        final String userName = mUserName.getText().toString().trim();
        final String bio = mBio.getText().toString().trim();
        final String website = mWebsite.getText().toString().trim();
        String mobile = mMobile.getText().toString().trim();
        String gender = mGender.getCheckedRadioButtonId() == R.id.male ? "m" : mGender.getCheckedRadioButtonId() == R.id.female ? "f" : null;
        File photo = mPhotoPath != null ? new File(mPhotoPath) : null;
        final AlertDialog progress = Notifications.showLoadingDialog(UpdateUserActivity.this, getString(R.string.loading));
        UserApi.update(userName, photo, bio, website, mobile, gender, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {
                    UserModel u = ((UserModel) item);
                    User.getInstance().setName(u.getName());
                    User.getInstance().setBio(u.getBio());
                    User.getInstance().setWebsite(u.getWebsite());
                    User.getInstance().setMobile(u.getMobile());
                    User.getInstance().setGender(u.getGender());
                    User.getInstance().setPhoto(u.getPhoto());
                    User.getInstance().update();
                    Broadcasting.sendProfileUpdated(UpdateUserActivity.this);
                    mPhotoPath = null;
                } else {
                    Notifications.showListAlertDialog(UpdateUserActivity.this, getString(R.string.error), result.getMessages());
                }
                progress.dismiss();
            }
        });
    }

}