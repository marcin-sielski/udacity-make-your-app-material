package com.sielski.marcin.xyzreader.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;

import java.util.List;

import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.florent37.shapeofview.shapes.ArcView;
import com.sielski.marcin.xyzreader.R;
import com.sielski.marcin.xyzreader.data.ArticleLoader;
import com.sielski.marcin.xyzreader.util.GlideApp;
import com.sielski.marcin.xyzreader.util.XYZReaderUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbar;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.body)
    RecyclerView mBody;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.photo)
    ImageView mPhoto;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.article_byline)
    TextView mArticleByLine;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.action_share)
    FloatingActionButton mActionShare;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.appbar)
    AppBarLayout mAppBarLayout;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.arcview)
    ArcView mArcView;

    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        Activity activity = getActivity();

        if (mCursor != null) {
            if (activity != null) {
                GlideApp.with(activity.getApplicationContext()).asBitmap()
                        .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                        .into(new BitmapImageViewTarget(mPhoto) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap>
                                                                transition) {
                                super.onResourceReady(resource, transition);
                                Palette.from(resource).generate((palette) -> {
                                    int color;
                                    if (palette.getDarkVibrantSwatch() != null)
                                        color = palette.getDarkVibrantSwatch().getRgb();
                                    else
                                        color = palette.getDarkMutedColor(ContextCompat
                                                .getColor(activity.getApplicationContext(),
                                                        R.color.colorPrimary));

                                    mCollapsingToolbar.setContentScrimColor(color);
                                });
                            }
                        });
                mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                    DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
                    if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()/2) {
                        mArticleByLine.setVisibility(View.GONE);
                        mArcView.setElevation(0);
                    } else {
                        mArticleByLine.setVisibility(View.VISIBLE);
                        mArcView.setElevation(4*displayMetrics.density);
                    }
                });
            }
            mToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            mArticleByLine.setText(String.format("%s %s %s",
                    XYZReaderUtils.convertDate(mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE)),
                    getString(R.string.by),
                    mCursor.getString(ArticleLoader.Query.AUTHOR)));
            List<String> body = ArticleLoader.getSplitBody(mCursor);
            mBody.setAdapter(new RecyclerView.Adapter() {
                class ViewHolder extends RecyclerView.ViewHolder {

                    ViewHolder(View itemView) {
                        super(itemView);
                    }
                }

                @NonNull
                @Override
                public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


                    return new ViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_body, parent, false));
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                    ((TextView) holder.itemView.findViewById(R.id.paragraph))
                            .setText(body.get(holder.getLayoutPosition()));
                }

                @Override
                public int getItemCount() {
                    return body.size();
                }
            });
            TranslateAnimation translateAnimation = new TranslateAnimation(0,0,
                    mBody.getHeight(), 0);
            translateAnimation.setDuration(500);
            translateAnimation.setFillAfter(true);
            mBody.startAnimation(translateAnimation);
            mActionShare.setOnClickListener((view) -> {
                Intent chooser = ShareCompat.IntentBuilder.from(getActivity())
                        .setType(getString(R.string.mime_type))
                        .setText(String.format("%s %s %s",
                                mCursor.getString(ArticleLoader.Query.TITLE),
                                getString(R.string.by),
                                mCursor.getString(ArticleLoader.Query.AUTHOR)))
                        .getIntent();

                Intent intent = Intent.createChooser(chooser, getString(R.string.action_share));

                startActivity(intent);
            });

        } else {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView.invalidate();
    }

}
