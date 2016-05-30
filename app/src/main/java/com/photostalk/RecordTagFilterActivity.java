package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.photostalk.fragments.FilterFragment;
import com.photostalk.fragments.HashtagFragment;
import com.photostalk.fragments.RecordFragment;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.MiscUtils;
import com.photostalk.utils.recorder.Recorder;


public class RecordTagFilterActivity extends AppCompatActivity {
    public static final String PHOTO_PATH = "photo_path";
    public static final String IS_LIVE = "is_live";

    private ViewPager mViewPager;
    private Toolbar mToolbar;

    private RecordFragment mRecordFragment;
    private FilterFragment mFilterFragment;
    private HashtagFragment mHashtagFragment;

    private BroadcastReceiver mBroadcastReceiver;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.record_tag_filter_activity);
        init();

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mRecordFragment.getRecorder().stop();
        mRecordFragment.getRecorder().stopPlaying();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record_filter_hashtag_actions, menu);
        menu.getItem(0).setVisible(false);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.proceed:

                /**
                 * first save the filtered image
                 */
                MiscUtils.showKeyboard(false, mHashtagFragment.getEditText());

                Recorder recorder = mRecordFragment.getRecorder();

                String fileName = mFilterFragment.saveFilteredPhoto();
                String audioName = recorder.hasRecorded() ? recorder.getFileName() : null;
                String duration = (recorder.hasRecorded()) ? recorder.getDurationFormatted() : "00:00";
                String description = mHashtagFragment.getHashtag().trim();
                boolean isLive = getIntent().getBooleanExtra(IS_LIVE, false);

                Intent i = new Intent(RecordTagFilterActivity.this, PreviewPhotoActivity.class);
                i.putExtra(PreviewPhotoActivity.PHOTO_PATH, fileName);
                i.putExtra(PreviewPhotoActivity.AUDIO_PATH, audioName);
                i.putExtra(PreviewPhotoActivity.DURATION, duration);
                i.putExtra(PreviewPhotoActivity.DESCRIPTION, description);
                i.putExtra(PreviewPhotoActivity.IS_LIVE, isLive);

                startActivity(i);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.LOGOUT) || intent.getAction().equals(Broadcasting.PHOTO_POSTED))
                    finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.PHOTO_POSTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {
        initReferences();
        fillFields();
    }

    private void initReferences() {
        mViewPager = ((ViewPager) findViewById(R.id.viewpager));
        mToolbar = ((Toolbar) findViewById(R.id.toolbar));
    }

    private void fillFields() {
        setSupportActionBar(mToolbar);
        mToolbar.setTitleTextColor(0xFFFFFFFF);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.mipmap.back);
            ab.setTitle(R.string.record_audio);
        }

        setupViewPager();
    }

    private void setupViewPager() {
        setupFragments();

        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0)
                    return mRecordFragment;
                else if (position == 1)
                    return mFilterFragment;
                else if (position == 2)
                    return mHashtagFragment;
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    mToolbar.setTitle(R.string.record_audio);
                else if (position == 1)
                    mToolbar.setTitle(R.string.add_filters);
                else if (position == 2) {
                    mToolbar.setTitle(R.string.add_hashtags);
                }

                if (position == 2) {
                    mMenu.getItem(0).setVisible(true);
                    MiscUtils.showKeyboard(true, mHashtagFragment.getEditText());
                } else {
                    mMenu.getItem(0).setVisible(false);
                    MiscUtils.showKeyboard(false, mHashtagFragment.getEditText());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mViewPager.setOffscreenPageLimit(3);
    }

    private void setupFragments() {
        final String photoPath = getIntent().getStringExtra(PHOTO_PATH);
        //final String photoPath = "/storage/emulated/0/DCIM/Camera/20160421_215310.jpg";


        mRecordFragment = new RecordFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                setPhoto(photoPath);
            }
        };

        mFilterFragment = new FilterFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                setPhoto(photoPath);
            }
        };

        mHashtagFragment = new HashtagFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                setPhoto(photoPath);
            }
        };
    }
}
