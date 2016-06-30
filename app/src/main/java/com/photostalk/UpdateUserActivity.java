package com.photostalk;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.photostalk.core.User;
import com.photostalk.models.Country;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.photostalk.apis.Result;
import com.photostalk.apis.SettingsAPI;
import com.photostalk.apis.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;
import com.soundcloud.android.crop.Crop;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by mohammed on 3/6/16.
 */
public class UpdateUserActivity extends LoggedInActivity {

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
    private Spinner mCountries;
    private EditText mCity;

    private AlertDialog mProgressDialog;

    private UserModel mUser;
    private String mPhotoPath = null;

    private ArrayList<String> mCountriesNames = new ArrayList<>();
    private ArrayList<String> mCountriesIds = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


    private void init() {
        initReferences();
        loadCountries();
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
        mCountries = ((Spinner) findViewById(R.id.countries_spinner));
        mCity = ((EditText) findViewById(R.id.city));
    }

    private void loadCountries() {
        showProgressDialog(true);
        SettingsAPI.getCountries(new ApiListeners.OnItemsArrayLoadedListener() {
            @Override
            public void onLoaded(Result result, ArrayList<Model> items) {
                if (result.isSucceeded()) {
                    mCountriesNames.add(getString(R.string.select_country));
                    mCountriesIds.add("");
                    for (Model item : items) {
                        mCountriesNames.add(((Country) item).getName());
                        mCountriesIds.add(item.getId());

                        // fill the spinner:
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(UpdateUserActivity.this, android.R.layout.simple_spinner_item, mCountriesNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mCountries.setAdapter(adapter);
                        loadUserInfo();
                    }

                } else {
                    showProgressDialog(false);
                    Notifications.showYesNoDialog(UpdateUserActivity.this,
                            getString(R.string.error),
                            result.getMessages().get(0),
                            getString(R.string.retry),
                            getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    loadCountries();
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                }

            }
        });
    }

    private void loadUserInfo() {
        if (mProgressDialog == null) showProgressDialog(true);
        UserApi.get(null, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                showProgressDialog(false);

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
                    mCity.setText(mUser.getCity());

                    // fill the country spinner:
                    if (!mUser.getCountry().isEmpty()) {
                        for (int i = 0; i < mCountriesNames.size(); i++) {
                            if (mCountriesNames.get(i).equals(mUser.getCountry())) {
                                mCountries.setSelection(i);
                                break;
                            }
                        }
                    }

                    initEvents();

                } else {
                    Notifications.showYesNoDialog(UpdateUserActivity.this,
                            getString(R.string.error),
                            result.getMessages().get(0),
                            getString(R.string.retry),
                            getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    loadUserInfo();
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                }
            }
        });
    }

    private void showProgressDialog(boolean show) {
        if (show && mProgressDialog == null) {
            mProgressDialog = Notifications.showLoadingDialog(this, getString(R.string.loading));
        } else if (!show && mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

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
        String countryId = mCountriesIds.get(mCountries.getSelectedItemPosition());
        String city = mCity.getText().toString().trim();

        final AlertDialog progress = Notifications.showLoadingDialog(UpdateUserActivity.this, getString(R.string.loading));
        UserApi.update(userName, photo, bio, website, mobile, gender, countryId, city, new ApiListeners.OnItemLoadedListener() {
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
                    Broadcasting.sendProfileUpdated();
                    mPhotoPath = null;
                    finish();
                } else {
                    Notifications.showListAlertDialog(UpdateUserActivity.this, getString(R.string.error), result.getMessages());
                }
                progress.dismiss();
            }
        });
    }

}
