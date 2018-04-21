package com.sielski.marcin.xyzreader.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.sielski.marcin.xyzreader.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class XYZReaderUtils {
    public static final String URL = "https://go.udacity.com/xyz-reader-json";
    public static final String BROADCAST_ACTION_NETWORK_CHANGE =
            "android.net.conn.CONNECTIVITY_CHANGE";

    private static final SimpleDateFormat mDateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());

    private static final SimpleDateFormat mOutputFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static void showSnackBar(Context context, View view, String text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
        view = snackbar.getView();
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
        ((TextView) view.findViewById(android.support.design.R.id.snackbar_text))
                .setTextColor(ContextCompat.getColor(context, android.R.color.white));
        snackbar.show();
    }

    public static String convertDate(String date) {
        Date d = new Date();
        try {
            d =  mDateFormat.parse(date);
        } catch (ParseException e) {
            Timber.e(e);
        }
        return mOutputFormat.format(d);
    }

}
