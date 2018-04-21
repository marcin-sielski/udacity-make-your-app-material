package com.sielski.marcin.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sielski.marcin.xyzreader.util.XYZReaderUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class UpdaterService extends IntentService {
    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.sielski.marcin.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.sielski.marcin.xyzreader.intent.extra.REFRESHING";

    private RequestQueue mRequestQueue;

    public UpdaterService() {
        super(UpdaterService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (!XYZReaderUtils.isNetworkAvailable(this)) return;

        sendBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }

        mRequestQueue.add(new StringRequest(Request.Method.GET,
                XYZReaderUtils.URL,
                (response) -> {
                    ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                    Uri dirUri = ItemsContract.Items.buildDirUri();
                    cpo.add(ContentProviderOperation.newDelete(dirUri).build());
                    List<Book> books =
                            new Gson().fromJson(response,
                                    new TypeToken<List<Book>>() {
                                    }.getType());
                    for (Book book: books) {
                        ContentValues values = new ContentValues();
                        values.put(ItemsContract.Items.SERVER_ID, book.id);
                        values.put(ItemsContract.Items.AUTHOR, book.author);
                        values.put(ItemsContract.Items.TITLE, book.title);
                        values.put(ItemsContract.Items.BODY, book.body);
                        values.put(ItemsContract.Items.THUMB_URL, book.thumb);
                        values.put(ItemsContract.Items.PHOTO_URL, book.photo);
                        values.put(ItemsContract.Items.ASPECT_RATIO, book.aspect_ratio);
                        values.put(ItemsContract.Items.PUBLISHED_DATE, book.published_date);
                        cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
                    }
                    try {
                        getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);
                    } catch (RemoteException | OperationApplicationException e) {
                        Timber.e(e);
                    }
                    sendBroadcast(
                            new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
                }, (error) ->
                    sendBroadcast(
                            new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false))

                ));
    }
}
