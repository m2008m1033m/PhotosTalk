package com.photostalk.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.photostalk.R;

import java.util.ArrayList;

/**
 * Created by mohammed on 2/19/16.
 */
public class Notifications {

    public static AlertDialog showAlertDialog(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        final AlertDialog ad = builder.create();
        ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ad.dismiss();
            }
        });
        ad.show();
        return ad;
    }

    public static AlertDialog showListAlertDialog(Context context, String title, ArrayList<String> items) {
        String message = "";
        if (items.size() == 1) {
            message = items.get(0);
        } else {
            for (int i = 0; i < items.size(); i++)
                message += "• " + items.get(i) + "\n";
        }
        return showAlertDialog(context, title, message);
    }

    public static AlertDialog showLoadingDialog(Context context, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.loading, null, false);
        ((TextView) view.findViewById(R.id.message)).setText(message);
        builder.setView(view);
        builder.setCancelable(false);
        AlertDialog ad = builder.create();
        ad.show();
        return ad;
    }

    public static void showSnackbar(Activity activity, String message) {
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity
                .findViewById(android.R.id.content)).getChildAt(0);
        Snackbar.make(viewGroup, message, Snackbar.LENGTH_LONG).show();
    }

    public static void showSnackbar(View v, String message) {
        Snackbar.make(v, message, Snackbar.LENGTH_LONG).show();
    }

    public static AlertDialog showYesNoDialog(Context context, String title, String message, String yes, String no, final DialogInterface.OnClickListener positiveListener, final DialogInterface.OnClickListener negativeListener) {
        final boolean[] posClicked = {false};

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        AlertDialog ad = dialog.create();
        ad.setMessage(message);
        if (!title.isEmpty()) ad.setTitle(title);
        ad.setButton(AlertDialog.BUTTON_POSITIVE, yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveListener.onClick(dialog, which);
                posClicked[0] = true;
            }
        });

        ad.setButton(AlertDialog.BUTTON_NEGATIVE, no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                posClicked[0] = true;
                negativeListener.onClick(dialog, which);
            }
        });

        ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!posClicked[0])
                    negativeListener.onClick(dialog, -1);
            }
        });
        ad.show();

        return ad;
    }

}
