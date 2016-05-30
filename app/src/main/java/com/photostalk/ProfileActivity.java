package com.photostalk;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.fragments.ProfileFragment;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;
import com.photostalk.utils.Notifications;

/**
 * Created by mohammed on 3/6/16.
 */
public class ProfileActivity extends AppCompatActivity {

    private final static int FOLLOW = 0;
    private final static int EDIT = 1;
    private final static int CHANGE_PASSWORD = 2;
    private final static int REPORT = 3;
    private final static int BLOCK = 4;
    private final static int BLOCKED_USERS = 5;
    private final static int LOGOUT = 6;
    private final static int UNFOLLOW = 7;


    public final static String USER_ID = "user_id";

    private AlertDialog mProgressDialog;
    private AlertDialog mChangePasswordDialog;

    private BroadcastReceiver mBroadcastReceiver;

    private UserModel mUser;

    private Menu mMenu;

    private boolean mIsOtherUser = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        setupBroadcastReceiver();

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_actions, menu);
        mMenu = menu;
        refreshMenuItems();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mUser == null) return true;
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.logout:
                User.getInstance().logout();
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                Broadcasting.sendLogout(this);
                return true;
            case R.id.edit:
                startActivity(new Intent(this, UpdateUserActivity.class));
                return true;
            case R.id.change_password:
                showChangePasswordDialog();
                return true;
            case R.id.report:
                UserApi.report(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded())
                            Notifications.showSnackbar(ProfileActivity.this, getString(R.string.the_user_has_been_reported));
                        else
                            Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                    }
                });
                return true;
            case R.id.blocked_users:
                Intent i = new Intent(this, FollowshipAndBlockagesActivity.class);
                i.putExtra(FollowshipAndBlockagesActivity.TYPE, FollowshipAndBlockagesActivity.TYPE_BLOCKED);
                startActivity(i);
                return true;
            case R.id.block:
                if (mUser.isBlocked()) {
                    UserApi.unblock(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mUser.setIsBlocked(false);
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.unblocked_successfully));
                                item.setTitle(R.string.block);
                                Broadcasting.sendBlock(ProfileActivity.this, mUser.getId(), false);
                            } else
                                Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                        }
                    });
                } else {
                    UserApi.block(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mUser.setIsBlocked(true);
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.blocked_successfully));
                                item.setTitle(R.string.unblock);
                                Broadcasting.sendBlock(ProfileActivity.this, mUser.getId(), true);
                            } else
                                Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                        }
                    });
                }
                return true;
            case R.id.unfollow:
                UserApi.unfollow(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        if (result.isSucceeded()) {
                            mUser.setIsFollowingUser(false);
                            Notifications.showSnackbar(ProfileActivity.this, getString(R.string.you_have_unfollowed_s_successully, mUser.getName()));
                            mMenu.getItem(UNFOLLOW).setVisible(false);
                            Broadcasting.sendFollow(ProfileActivity.this, mUser.getId(), false, false);
                        } else {
                            Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                        }
                    }
                });
                return true;
            case R.id.follow:
                if (mUser.isFollowingUser()) return true;
                if (mUser.isPrivate()) {
                    // send a follow request
                    UserApi.request(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mUser.setIsFollowingUser(false);
                                mUser.setIsFollowRequestSent(true);
                                refreshFollowUnfollowActionButtons();
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.a_request_has_been_sent_to_s, mUser.getName()));
                                Broadcasting.sendFollow(ProfileActivity.this, mUser.getId(), true, false);
                            } else {
                                Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                            }
                        }
                    });

                } else {
                    // just follow the user
                    UserApi.follow(mUser.getId(), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                mUser.setIsFollowingUser(true);
                                mUser.setIsFollowRequestSent(false);
                                refreshFollowUnfollowActionButtons();
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.you_have_followed_s_successfully, mUser.getName()));
                                Broadcasting.sendFollow(ProfileActivity.this, mUser.getId(), false, true);
                            } else {
                                Notifications.showSnackbar(ProfileActivity.this, result.getMessages().get(0));
                            }
                        }
                    });
                }


                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            finish();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillFields();
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.BLOCK)) {
                    String userId = intent.getStringExtra("user_id");
                    if (!userId.equals(mUser.getId())) return;
                    boolean block = intent.getBooleanExtra("block", mUser.isBlocked());
                    mMenu.getItem(BLOCK).setTitle(block ? R.string.unblock : R.string.block);
                } else if (intent.getAction().equals(Broadcasting.LOGOUT)) {
                    finish();
                } else if (intent.getAction().equals(Broadcasting.FOLLOW)) {
                    boolean isFollowing = intent.getBooleanExtra("follow", false);
                    boolean isFollowRequestSent = intent.getBooleanExtra("request", false);
                    mUser.setIsFollowingUser(isFollowing);
                    mUser.setIsFollowRequestSent(isFollowRequestSent);
                    refreshFollowUnfollowActionButtons();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(Broadcasting.BLOCK);
        intentFilter.addAction(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.FOLLOW);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void init() {

    }

    private void fillFields() {
        String userId = getIntent().getStringExtra(USER_ID);//"56dc564ddb6647aa6c97f7df";//

        mIsOtherUser = !(userId == null || userId.equals(User.getInstance().getId()));

        mProgressDialog = Notifications.showLoadingDialog(this, getString(R.string.loading));
        UserApi.get(userId, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {

                    mUser = ((UserModel) item);

                    ProfileFragment profileFragment = ((ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.profile_fragment));
                    profileFragment.setToolbarVisible(true);
                    profileFragment.fillFields(mUser);

                    // hide the the unfollow option in menu if the local user is following the user.
                    refreshFollowUnfollowActionButtons();

                } else {
                    Notifications.showListAlertDialog(ProfileActivity.this, getString(R.string.error), result.getMessages()).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                        }
                    });
                }
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        });
    }

    private void refreshMenuItems() {
        if (mIsOtherUser) {
            mMenu.getItem(EDIT).setVisible(false);
            mMenu.getItem(CHANGE_PASSWORD).setVisible(false);
            mMenu.getItem(BLOCKED_USERS).setVisible(false);
            mMenu.getItem(LOGOUT).setVisible(false);
            refreshFollowUnfollowActionButtons();
        } else {
            mMenu.getItem(REPORT).setVisible(false);
            mMenu.getItem(BLOCK).setVisible(false);
            mMenu.getItem(UNFOLLOW).setVisible(false);
            mMenu.getItem(FOLLOW).setVisible(false);
        }
    }

    private void refreshFollowUnfollowActionButtons() {
        if (mUser == null || mMenu == null) return;
        mMenu.getItem(UNFOLLOW).setVisible(mUser.isFollowingUser());
        mMenu.getItem(FOLLOW).setVisible(!mUser.isFollowingUser() && !mUser.isFollowRequestSent());
    }

    private void showChangePasswordDialog() {
        if (mIsOtherUser) return;
        if (mChangePasswordDialog == null) {

            AlertDialog.Builder b = new AlertDialog.Builder(this);
            View v = LayoutInflater.from(this).inflate(R.layout.change_password_dialog, null);
            final TextView mOldPassword = ((TextView) v.findViewById(R.id.old_password));
            final TextView mNewPassword = ((TextView) v.findViewById(R.id.new_password));
            final TextView mConfirmPassword = ((TextView) v.findViewById(R.id.confirm_password));

            b.setView(v);

            b.setPositiveButton(R.string.ok, null);

            b.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mChangePasswordDialog.dismiss();
                }
            });

            mChangePasswordDialog = b.create();

            mChangePasswordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    final Button b = mChangePasswordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String old = mOldPassword.getText().toString();
                            String _new = mNewPassword.getText().toString();
                            String confirm = mConfirmPassword.getText().toString();

                            if (old.isEmpty()) {
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.old_password_cannot_be_empty));
                                return;
                            }


                            if (_new.isEmpty()) {
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.new_password_cannot_be_empty));
                                return;
                            }

                            if (confirm.isEmpty()) {
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.please_confirm_your_password));
                                return;
                            }

                            if (!_new.equals(confirm)) {
                                Notifications.showSnackbar(ProfileActivity.this, getString(R.string.the_new_passwords_do_not_match));
                                return;
                            }

                            Communicator.getInstance().cancelByTag("user_change_password");
                            b.setEnabled(false);
                            UserApi.changePassword(old, _new, confirm, new ApiListeners.OnActionExecutedListener() {
                                @Override
                                public void onExecuted(Result result) {
                                    if (result.isSucceeded()) {
                                        mChangePasswordDialog.dismiss();
                                        mOldPassword.setText("");
                                        mNewPassword.setText("");
                                        mConfirmPassword.setText("");
                                        Notifications.showSnackbar(ProfileActivity.this, getString(R.string.password_updated_successfully));
                                    } else {
                                        Notifications.showListAlertDialog(ProfileActivity.this, getString(R.string.error), result.getMessages());
                                        b.setEnabled(true);
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }

        mChangePasswordDialog.show();
    }
}
