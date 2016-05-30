package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.photostalk.adapters.SelectBestPhotoAdapter;
import com.photostalk.models.BestPhoto;
import com.photostalk.services.Local;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

/**
 * Created by mohammed on 2/24/16.
 */
public class SelectBestPhotoActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private SelectBestPhotoAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
        setContentView(R.layout.select_best_photo_activity);
        setTitle(getString(R.string.select_best_photo));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_best_photo_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcasting.PHOTO_POSTED);
        intentFilter.addAction(Broadcasting.LOGOUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Broadcasting.PHOTO_POSTED));
    }


    private void init() {
        initReferences();
        fill();
    }

    private void initReferences() {
        mRecyclerView = ((RecyclerView) findViewById(R.id.recycler_view));
    }

    private void fill() {
        createAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void createAdapter() {
        mAdapter = new SelectBestPhotoAdapter(Local.getTmpTakenPhotos(), new SelectBestPhotoAdapter.OnPhotoSelectedListener() {
            @Override
            public void onSelected(BestPhoto selectedPhoto) {
                goToFilterRecordHashTag(selectedPhoto.getPath());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.forward:
                BestPhoto bestPhoto = mAdapter.getSelectedItem();
                if (bestPhoto == null)
                    Notifications.showSnackbar(this, getString(R.string.you_need_to_select_a_photo_first));
                else {
                    goToFilterRecordHashTag(bestPhoto.getPath());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void goToFilterRecordHashTag(String photoPath) {
        Intent i = new Intent(SelectBestPhotoActivity.this, RecordTagFilterActivity.class);
        i.putExtra(RecordTagFilterActivity.PHOTO_PATH, photoPath);
        i.putExtra(RecordTagFilterActivity.IS_LIVE, true);
        startActivity(i);
    }
}
