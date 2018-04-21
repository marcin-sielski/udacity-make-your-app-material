package com.sielski.marcin.xyzreader.ui;

import android.support.v4.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sielski.marcin.xyzreader.util.GlideApp;
import com.sielski.marcin.xyzreader.R;
import com.sielski.marcin.xyzreader.data.ArticleLoader;
import com.sielski.marcin.xyzreader.data.ItemsContract;
import com.sielski.marcin.xyzreader.data.UpdaterService;
import com.sielski.marcin.xyzreader.util.XYZReaderUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (XYZReaderUtils.isNetworkAvailable(this)) {
                setEnabled(false);
                startService( new Intent(this, UpdaterService.class));
            } else {
                XYZReaderUtils.showSnackBar(this, mSwipeRefreshLayout,
                        getString(R.string.snackbar_network_unavailable));
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        getSupportLoaderManager().initLoader(0, null, this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
        intentFilter.addAction(XYZReaderUtils.BROADCAST_ACTION_NETWORK_CHANGE);
        registerReceiver(mRefreshingReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void setEnabled(boolean enabled) {
        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case UpdaterService.BROADCAST_ACTION_STATE_CHANGE:
                        boolean refreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING,
                                false);
                        setEnabled(!refreshing);
                        mSwipeRefreshLayout.setRefreshing(refreshing);
                        break;
                    case XYZReaderUtils.BROADCAST_ACTION_NETWORK_CHANGE:
                        if (XYZReaderUtils.isNetworkAvailable(context)) {
                            setEnabled(false);
                            startService(new Intent(context, UpdaterService.class));
                        }
                        break;
                }
            }
        }
    };

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final Cursor mCursor;

        Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener((v) ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))))
            );
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(String.format("%s \n%s %s",
                    XYZReaderUtils.convertDate(mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE)),
                    getString(R.string.by), mCursor.getString(ArticleLoader.Query.AUTHOR)));
            Context context = holder.itemView.getContext();
            GlideApp.with(context).load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .centerCrop()
                    //TODO .placeholder()
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnailView;
        final TextView titleView;
        final TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
