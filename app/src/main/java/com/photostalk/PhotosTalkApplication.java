package com.photostalk;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.photostalk.core.User;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;


@ReportsCrashes(
        formKey = "",
        mailTo = "m2008m1033m@gmail.com",
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.a_report_has_been_sent_successfully)


public class PhotosTalkApplication extends MultiDexApplication {
    private static Context mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        User.getInstance();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }


    public static Context getContext() {
        return mContext;
    }
}
