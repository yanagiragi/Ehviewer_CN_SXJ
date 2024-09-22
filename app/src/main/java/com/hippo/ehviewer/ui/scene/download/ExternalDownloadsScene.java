/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene.download;

import static com.hippo.ehviewer.AppConfig.getDefaultExternalDownloadDir;
import static com.hippo.ehviewer.spider.SpiderDen.getGalleryDownloadDir;
import static com.hippo.ehviewer.ui.scene.gallery.detail.GalleryDetailScene.KEY_COME_FROM_DOWNLOAD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson.JSONArray;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.android.resource.AttrResources;
import com.hippo.conaco.DataContainer;
import com.hippo.conaco.ProgressNotifier;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.dao.ExternalDownloadInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.spider.SpiderInfo;
import com.hippo.ehviewer.sync.DownloadListInfosExecutor;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.ehviewer.ui.scene.TransitionNameFactory;
import com.hippo.ehviewer.ui.scene.gallery.detail.ExternalGalleryDetailScene;
import com.hippo.ehviewer.ui.scene.gallery.detail.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.gallery.list.EnterGalleryDetailTransaction;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.ripple.Ripple;
import com.hippo.scene.Announcer;
import com.hippo.streampipe.InputStreamPipe;
import com.hippo.unifile.UniFile;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.ProgressView;
import com.hippo.widget.SearchBarMover;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.ViewUtils;
import com.hippo.yorozuya.collect.LongList;
import com.sxj.paginationlib.PaginationIndicator;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExternalDownloadsScene extends ToolbarScene
        implements
        EasyRecyclerView.OnItemClickListener,
        EasyRecyclerView.OnItemLongClickListener,
        FabLayout.OnClickFabListener,
        FabLayout.OnExpandListener,
        FastScroller.OnDragHandlerListener,
        SearchBarMover.Helper,
        SearchBar.OnStateChangeListener {

    // region Definitions
    private class MoveDialogHelper implements DialogInterface.OnClickListener {

        private final String[] mLabels;
        private final List<DownloadInfo> mDownloadInfoList;

        public MoveDialogHelper(String[] labels, List<DownloadInfo> downloadInfoList) {
            mLabels = labels;
            mDownloadInfoList = downloadInfoList;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Cancel check mode
            Context context = getEHContext();
            if (null == context) {
                return;
            }
            if (null != mRecyclerView) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            String label;
            if (which == 0) {
                label = null;
            } else {
                label = mLabels[which];
            }
            EhApplication.getDownloadManager(context).changeLabel(mDownloadInfoList, label);
        }
    }

    private class DownloadHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final LoadImageView thumb;
        public final TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final TextView readProgress;
        public final View start;
        public final View stop;
        public final TextView state;
        public final ProgressBar progressBar;
        public final TextView percent;
        public final TextView speed;

        public DownloadHolder(View itemView) {
            super(itemView);

            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            uploader = itemView.findViewById(R.id.uploader);
            rating = itemView.findViewById(R.id.rating);
            category = itemView.findViewById(R.id.category);
            readProgress = itemView.findViewById(R.id.read_progress);
            start = itemView.findViewById(R.id.start);
            stop = itemView.findViewById(R.id.stop);
            state = itemView.findViewById(R.id.state);
            progressBar = itemView.findViewById(R.id.progress_bar);
            percent = itemView.findViewById(R.id.percent);
            speed = itemView.findViewById(R.id.speed);

            // TODO cancel on click listener when select items
            thumb.setOnClickListener(this);
            start.setOnClickListener(this);
            stop.setOnClickListener(this);

            boolean isDarkTheme = !AttrResources.getAttrBoolean(getEHContext(), androidx.appcompat.R.attr.isLightTheme);
            Ripple.addRipple(start, isDarkTheme);
            Ripple.addRipple(stop, isDarkTheme);
        }

        @Override
        public void onClick(View v) {
            Context context = getEHContext();
            Activity activity = getActivity2();
            EasyRecyclerView recyclerView = mRecyclerView;
            if (null == context || null == activity || null == recyclerView || recyclerView.isInCustomChoice()) {
                return;
            }
            List<ExternalDownloadInfo> list = mList;
            if (list == null) {
                return;
            }
            int size = list.size();
            int index = recyclerView.getChildAdapterPosition(itemView);
            if (index < 0 || index >= size) {
                return;
            }

            if (thumb == v) {
                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_DOWNLOAD_GALLERY_INFO);
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list.get(positionInList(index)));
                args.putBoolean(KEY_COME_FROM_DOWNLOAD, true);
                Announcer announcer = new Announcer(ExternalGalleryDetailScene.class).setArgs(args);
                announcer.setTranHelper(new EnterGalleryDetailTransaction(thumb));
                startScene(announcer);
            } else if (start == v) {
                final ExternalDownloadInfo info = list.get(positionInList(index));
                Intent intent = new Intent(activity, DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, info);
                activity.startService(intent);
            }
        }
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        private final LayoutInflater mInflater;
        private final int mListThumbWidth;
        private final int mListThumbHeight;

        public DownloadAdapter() {
            LayoutInflater mInflater1;
            try {
                mInflater1 = getLayoutInflater2();
            } catch (NullPointerException e) {
                mInflater1 = getLayoutInflater();
            }
            mInflater = mInflater1;
            AssertUtils.assertNotNull(mInflater);

            View calculator = mInflater.inflate(R.layout.item_gallery_list_thumb_height, null);
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
            mListThumbHeight = calculator.getMeasuredHeight();
            mListThumbWidth = mListThumbHeight * 2 / 3;
        }

        @Override
        public long getItemId(int position) {
            int posInList = positionInList(position);
            if (mList == null || posInList < 0 || posInList >= mList.size()) {
                return 0;
            }
            return mList.get(posInList).gid;
        }

        @NonNull
        @Override
        public DownloadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DownloadHolder holder = new DownloadHolder(mInflater.inflate(R.layout.item_download, parent, false));

            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);

            return holder;
        }

        @Override
        public void onBindViewHolder(DownloadHolder holder, int position) {
            if (mList == null) {
                return;
            }

            try {
                int pos = positionInList(position);
                DownloadInfo info = mList.get(pos);

                String title = EhUtils.getSuitableTitle(info);
                holder.thumb.load(EhCacheKeyFactory.getExternalThumbKey(info.gid, info.thumb), info.thumb, new ThumbDataContainer(info), info.thumb.startsWith("http"));

                holder.title.setText(title);
                holder.uploader.setText(info.uploader);
                holder.rating.setRating(info.rating);
                holder.readProgress.setText(info.pages + "P");

                TextView category = holder.category;
                String newCategoryText = EhUtils.getCategory(info.category);
                if (!newCategoryText.equals(category.getText())) {
                    category.setText(newCategoryText);
                    category.setBackgroundColor(EhUtils.getCategoryColor(info.category));
                }
                bindForState(holder, info);

                // Update transition name
                ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(info.gid));
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        @Override
        public int getItemCount() {
            if (mList == null) {
                return 0;
            }
            int listSize = mList.size();
            if (listSize < paginationSize || !canPagination) {
                return listSize;
            }
            int count = listSize - pageSize * (indexPage - 1);
            return Math.min(count, pageSize);
        }
    }

    private class DownloadChoiceListener implements EasyRecyclerView.CustomChoiceListener {

        @Override
        public void onIntoCustomChoice(EasyRecyclerView view) {
            if (mRecyclerView != null) {
                mRecyclerView.setOnItemLongClickListener(null);
                mRecyclerView.setLongClickable(false);
            }
            if (mFabLayout != null) {
                mFabLayout.setExpanded(true);
            }
            // Lock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        }

        @Override
        public void onOutOfCustomChoice(EasyRecyclerView view) {
            if (mRecyclerView != null) {
                mRecyclerView.setOnItemLongClickListener(ExternalDownloadsScene.this);
            }
            if (mFabLayout != null) {
                mFabLayout.setExpanded(false);
            }
            // Unlock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        }

        @Override
        public void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked) {
            if (view.getCheckedItemCount() == 0) {
                view.outOfCustomChoiceMode();
            }
        }
    }

    private class ThumbDataContainer implements DataContainer {

        private final DownloadInfo mInfo;
        @Nullable
        private UniFile mFile;

        public ThumbDataContainer(@NonNull DownloadInfo info) {
            mInfo = info;
        }

        private void ensureFile() {
            if (mFile == null) {
                if (mInfo.thumb.startsWith("http")) {
                    var externalDownloadDir = getDefaultExternalDownloadDir();
                    var dir = UniFile.fromFile(new File(externalDownloadDir + "/" + "Thumbnail"));
                    dir.ensureDir();

                    mFile = dir.createFile(mInfo.gid + ".thumb");
                }
                else {
                    mFile = UniFile.fromFile(new File(mInfo.thumb));
                }
            }
        }

        @Override
        public boolean isEnabled() {
            ensureFile();
            return mFile != null;
        }

        @Override
        public void onUrlMoved(String requestUrl, String responseUrl) {
        }

        @Override
        public boolean save(InputStream is, long length, String mediaType, ProgressNotifier notify) {
            ensureFile();
            if (mFile == null) {
                return false;
            }

            OutputStream os = null;
            try {
                os = mFile.openOutputStream();
                IOUtils.copy(is, os);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                IOUtils.closeQuietly(os);
            }
        }

        @Override
        public InputStreamPipe get() {
            ensureFile();
            if (mFile != null) {
                return new UniFileInputStreamPipe(mFile);
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            if (mFile != null) {
                mFile.delete();
            }
        }
    }

    public class MyPageChangeListener implements PaginationIndicator.OnChangedListener {

        @Override
        public void onPageSelectedChanged(int currentPagePos, int lastPagePos, int totalPageCount, int total) {
            if (indexPage == currentPagePos) {
                needInitPage = false;
            }
            if (needInitPage) {
                if (mPaginationIndicator != null) {
                    mPaginationIndicator.skip2Pos(indexPage);
                }
                return;
            }
            if (indexPage == currentPagePos) {
                return;
            }
            indexPage = currentPagePos;
            notifyAdapter();
        }

        @Override
        public void onPerPageCountChanged(int perPageCount) {
            if (pageSize == perPageCount) {
                return;
            }
            pageSize = perPageCount;
            notifyAdapter();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void notifyAdapter() {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            if (mRecyclerView != null) {
                if (doNotScroll) {
                    doNotScroll = false;
                    return;
                }
                mRecyclerView.scrollToPosition(0);
            }
        }
    }

    // endregion

    // region Constants
    public static final String KEY_GID = "gid";
    public static final String KEY_ACTION = "action";
    public static final String ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service";
    public static final int LOCAL_GALLERY_INFO_CHANGE = 909;

    private static final String TAG = ExternalDownloadsScene.class.getSimpleName();
    private static final String KEY_LABEL = "label";
    private static final long ANIMATE_TIME = 300L;
    public static final int MOVE_BUTTON_INDEX = 4;
    // endregion

    // region Variables

    @Nullable
    private AddDeleteDrawable mActionFabDrawable;

    /*---------------
         Whole life cycle
         ---------------*/
    @Nullable
    public String mLabel;
    @Nullable
    private List<ExternalDownloadInfo> mList;
    @Nullable
    private List<ExternalDownloadInfo> mFullList;
    private boolean mForceRefresh = false;

    /*---------------
     List pagination
     ---------------*/
    private int indexPage = 1;
    private int pageSize = 1;
    private boolean canPagination = true;
    private final int paginationSize = 500;
    //    private final int paginationSize = 5;
    private final int[] perPageCountChoices = {50, 100, 200, 300, 500};
//    private final int[] perPageCountChoices = {1, 2, 3, 4, 5};

    private final MyPageChangeListener myPageChangeListener = new MyPageChangeListener();

    private final Map<Long, SpiderInfo> mSpiderInfoMap = new HashMap<>();

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private FabLayout mFabLayout;
    @Nullable
    private DownloadAdapter mAdapter;
    @Nullable
    private AutoStaggeredGridLayoutManager mLayoutManager;

    private ShowcaseView mShowcaseView;

    private ProgressView mProgressView;

    private AlertDialog mSearchDialog;
    private SearchBar mSearchBar;

    private ExternalDownloadLabelDraw downloadLabelDraw;

    @Nullable
    private PaginationIndicator mPaginationIndicator;

    @Nullable
    private Menu mMenu;

    private int mInitPosition = -1;

    private boolean doNotScroll = false;

    private boolean needInitPage = false;
    private boolean needInitPageSize = false;

    // endregion

    // region Fragments
    @Override
    public void onNewArguments(@NonNull Bundle args) {
        handleArguments(args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getEHContext();
        AssertUtils.assertNotNull(context);
        canPagination = Settings.getDownloadPagination();
        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mList = null;

        mActionFabDrawable = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LABEL, mLabel);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateTitle();
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mShowcaseView) {
            ViewUtils.removeFromParent(mShowcaseView);
            mShowcaseView = null;
        }
        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout);
            mFabLayout = null;
        }

        mRecyclerView = null;
        mViewTransition = null;
        mAdapter = null;
        mLayoutManager = null;
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (null != mShowcaseView) {
            return;
        }

        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else {
            super.onBackPressed();
        }
    }

    // endregion

    // region Toolbar scene
    @Override
    public void onNavigationClick(View view) {
        onBackPressed();
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_external_download;
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_external_download, container, false);

        mProgressView = (ProgressView) ViewUtils.$$(view, R.id.download_progress_view);
        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        if (mPaginationIndicator != null) {
            needInitPage = true;
        }
        mPaginationIndicator = (PaginationIndicator) ViewUtils.$$(view, R.id.indicator);

        mPaginationIndicator.setPerPageCountChoices(perPageCountChoices, getPageSizePos(pageSize));

        mViewTransition = new ViewTransition(content, tip);

        Context context = getEHContext();
        AssertUtils.assertNotNull(content);
        Resources resources = context.getResources();

        Drawable drawable = DrawableManager.getVectorDrawable(context, R.drawable.big_download);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mLayoutManager = new AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL);
        mLayoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()));
        mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, !AttrResources.getAttrBoolean(context, androidx.appcompat.R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(new DownloadChoiceListener());
//        mRecyclerView.setOnGenericMotionListener(this::onGenericMotion);
        // Cancel change animation
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        int interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
        MarginItemDecoration decoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
        mRecyclerView.addItemDecoration(decoration);
        decoration.applyPaddings(mRecyclerView);
        if (mInitPosition >= 0 && indexPage != 1) {
            initPage(mInitPosition);
            mRecyclerView.scrollToPosition(listIndexInPage(mInitPosition));
            mInitPosition = -1;
        }

        fastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable handlerDrawable = new HandlerDrawable();
        handlerDrawable.setColor(AttrResources.getAttrColor(context, R.attr.widgetColorThemeAccent));
        fastScroller.setHandlerDrawable(handlerDrawable);
        fastScroller.setOnDragHandlerListener(this);

        mFabLayout.setExpanded(false, true);
        mFabLayout.setHidePrimaryFab(false);
        mFabLayout.setAutoCancel(false);
        mFabLayout.setOnClickFabListener(this);
        mFabLayout.setOnExpandListener(this);
        mActionFabDrawable = new AddDeleteDrawable(context, resources.getColor(R.color.primary_drawable_dark, null));
        mFabLayout.getPrimaryFab().setImageDrawable(mActionFabDrawable);
        addAboveSnackView(mFabLayout);

        View toolBarview = inflater.inflate(R.layout.scene_toolbar, container, false);
        Toolbar toolbar = (Toolbar) toolBarview.findViewById(R.id.toolbar);
        mMenu = toolbar.getMenu();

        updateView();

        guide();
        updatePaginationIndicator();
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh_external_downloads)
        {
            mForceRefresh = true;
            updateForLabel();
            mForceRefresh = false;
        }
        else {
            Toast.makeText(getEHContext(), R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
        }

        return false;
    }
    // endregion

    // region BaseScene

    @Override
    public View onCreateDrawerView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.bookmarks_draw, container, false);

        if (downloadLabelDraw == null) {
            downloadLabelDraw = new ExternalDownloadLabelDraw(inflater, container, this);
        }

        return downloadLabelDraw.createView();
    }

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_external_download;
    }

    // endregion

    // region Public Methods
    @SuppressLint("NotifyDataSetChanged")
    public void updateForLabel() {
        if (mList == null || mForceRefresh) {
            mFullList = readInfoJson(mLabel);
            mList = new ArrayList<>();
        }

        mList.clear();
        for(var i = 0; i < mFullList.size(); ++i) {
            var info = mFullList.get(i);
            var label = info.getLabel();
            if (mLabel == null && label == null) {
                mList.add(info);
            }
            if (label != null && mLabel != null && info.getLabel().compareTo(mLabel) == 0) {
                mList.add(info);
            }
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        updateTitle();
        updatePaginationIndicator();
    }

    public void updateView() {
        if (mViewTransition != null) {
            if (mList == null || mList.size() == 0) {
                mViewTransition.showView(1);
            } else {
                mViewTransition.showView(0);
            }
        }
    }

    public HashMap<String, Integer> getLabelList() {
        var result = new HashMap<String, Integer>();
        for(var i = 0; i < mFullList.size(); ++i) {
            var info = mFullList.get(i);
            var label = info.getLabel();
            if (label == null) {
                label = getString(R.string.default_download_label_name);
            }
            var count = result.get(label);
            if (label != null && count == null) {
                result.put(label, 1);
            }
            else {
                result.put(label, count + 1);
            }
        }
        return result;
    }

    // endregion

    // region Private Methods
    private boolean handleArguments(Bundle args) {
        if (null == args) {
            return false;
        }

        if (ACTION_CLEAR_DOWNLOAD_SERVICE.equals(args.getString(KEY_ACTION))) {
            DownloadService.clear();
        }

        // long gid;
        // if (null != mDownloadManager && -1L != (gid = args.getLong(KEY_GID, -1L))) {
        //     DownloadInfo info = mDownloadManager.getDownloadInfo(gid);
        //     if (null != info) {
        //         mLabel = info.getLabel();
        //         updateForLabel();
        //         updateView();
//
        //         // Get position
        //         if (null != mList) {
        //             int position = mList.indexOf(info);
        //             if (position >= 0 && null != mRecyclerView) {
        //                 initPage(position);
        //             } else {
        //                 mInitPosition = position;
        //             }
        //         }
        //         return true;
        //     }
        // }
        return false;
    }

    private void updatePaginationIndicator() {
        if (mPaginationIndicator == null || mList == null) {
            return;
        }
        if (mList.size() < paginationSize || !canPagination) {
            mPaginationIndicator.setVisibility(View.GONE);
            return;
        }
        mPaginationIndicator.setVisibility(View.VISIBLE);
        needInitPageSize = true;
        mPaginationIndicator.initPaginationIndicator(pageSize, perPageCountChoices, mList.size(), indexPage);
//        mPaginationIndicator.setTotalCount();
        mPaginationIndicator.setListener(myPageChangeListener);
    }

    @SuppressLint("StringFormatMatches")
    private void updateTitle() {
        try {
            setTitle('@' + getString(R.string.scene_download_title,
                    Integer.toString(mList == null ? 0 : mList.size()),
                    mLabel != null ? mLabel : getString(R.string.default_download_label_name)));
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            setTitle(getString(R.string.scene_download_title,
                    mLabel != null ? mLabel : getString(R.string.default_download_label_name)));
        }
    }

    private void onInit() {
        if (!handleArguments(getArguments())) {
            mLabel = Settings.getRecentDownloadLabel();
            updateForLabel();
        }
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mLabel = savedInstanceState.getString(KEY_LABEL);
        updateForLabel();
    }

    private void guide() {
        if (Settings.getGuideDownloadThumb() && null != mRecyclerView) {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Settings.getGuideDownloadThumb()) {
                        guideDownloadThumb();
                    }
                    if (null != mRecyclerView) {
                        ViewUtils.removeOnGlobalLayoutListener(mRecyclerView.getViewTreeObserver(), this);
                    }
                }
            });
        } else {
            guideDownloadLabels();
        }
    }

    private void guideDownloadThumb() {
        MainActivity activity = getActivity2();
        if (null == activity || !Settings.getGuideDownloadThumb() || null == mLayoutManager || null == mRecyclerView) {
            guideDownloadLabels();
            return;
        }
        int position = mLayoutManager.findFirstCompletelyVisibleItemPositions(null)[0];
        if (position < 0) {
            guideDownloadLabels();
            return;
        }
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (null == holder) {
            guideDownloadLabels();
            return;
        }

        mShowcaseView = new ShowcaseView.Builder(activity)
                .withMaterialShowcase()
                .setStyle(R.style.Guide)
                .setTarget(new ViewTarget(((DownloadHolder) holder).thumb))
                .blockAllTouches()
                .setContentTitle(R.string.guide_download_thumb_title)
                .setContentText(R.string.guide_download_thumb_text)
                .replaceEndButton(R.layout.button_guide)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        mShowcaseView = null;
                        ViewUtils.removeFromParent(showcaseView);
                        Settings.putGuideDownloadThumb(false);
                        guideDownloadLabels();
                    }
                }).build();
    }

    private void guideDownloadLabels() {
        MainActivity activity = getActivity2();
        if (null == activity || !Settings.getGuideDownloadLabels()) {
            return;
        }

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        mShowcaseView = new ShowcaseView.Builder(activity)
                .withMaterialShowcase()
                .setStyle(R.style.Guide)
                .setTarget(new PointTarget(point.x, point.y / 3))
                .blockAllTouches()
                .setContentTitle(R.string.guide_download_labels_title)
                .setContentText(R.string.guide_download_labels_text)
                .replaceEndButton(R.layout.button_guide)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        mShowcaseView = null;
                        ViewUtils.removeFromParent(showcaseView);
                        Settings.puttGuideDownloadLabels(false);
                        openDrawer(Gravity.RIGHT);
                    }
                }).build();
    }

    private void viewRandom() {
        List<ExternalDownloadInfo> list = mList;
        if (list == null) {
            return;
        }
        int position = (int) (Math.random() * list.size());
        if (position < 0 || position >= list.size()) {
            return ;
        }
        Activity activity = getActivity2();
        if (null == activity || null == mRecyclerView) {
            return;
        }

        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.setAction(GalleryActivity.ACTION_EH);
        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, list.get(position));
        startActivity(intent);
    }

    private void bindForState(DownloadHolder holder, DownloadInfo info) {
        Resources resources = getResources2();
        if (null == resources) {
            return;
        }

        holder.uploader.setVisibility(View.VISIBLE);
        holder.rating.setVisibility(View.VISIBLE);
        holder.category.setVisibility(View.VISIBLE);
        holder.readProgress.setVisibility(View.VISIBLE);
        holder.state.setVisibility(View.VISIBLE);
        holder.progressBar.setVisibility(View.GONE);
        holder.percent.setVisibility(View.GONE);
        holder.speed.setVisibility(View.GONE);
        holder.start.setVisibility(View.GONE);
        holder.stop.setVisibility(View.GONE);
        holder.state.setVisibility(View.GONE);
    }

    private void gotoFilterAndSort(int id) {
        mProgressView.setVisibility(View.VISIBLE);
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(View.GONE);
        }

        // DownloadListInfosExecutor executor = new DownloadListInfosExecutor(mBackList, mDownloadManager);
        // executor.setDownloadSearchingListener(this);
        // executor.executeFilterAndSort(id);
    }

    private void updateAdapter() {
        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initPage(int position) {
        if (mList != null && mList.size() > paginationSize && canPagination) {
            indexPage = position / pageSize + 1;
        }
        doNotScroll = true;
        if (mPaginationIndicator != null) {
            mPaginationIndicator.skip2Pos(indexPage);
        }
        mRecyclerView.scrollToPosition(listIndexInPage(position));
    }

    private int positionInList(int position) {
        if (mList != null && mList.size() > paginationSize && canPagination) {
            return position + pageSize * (indexPage - 1);
        }
        return position;
    }

    private int listIndexInPage(int position) {
        if (mList != null && mList.size() > paginationSize && canPagination) {
            return position % pageSize;
        }
        return position;
    }

    private int getPageSizePos(int pageSize) {
        int index = 0;
        for (int i = 0; i < perPageCountChoices.length; i++) {
            if (pageSize == perPageCountChoices[i]) {
                index = i;
                break;
            }
        }
        return index;
    }

    private List<ExternalDownloadInfo> readInfoJson(String label) {
        var list = new ArrayList<ExternalDownloadInfo>();

        File dir = getDefaultExternalDownloadDir();
        File[] files = dir.listFiles();
        File jsonFile = null;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains(".json")) {
                jsonFile = files[i];
                break;
            }
        }

        if (jsonFile == null) {
            Toast.makeText(getContext(), R.string.unable_to_read_external_downloads_configs, Toast.LENGTH_SHORT).show();
            return list;
        }
        else {
            Log.i(TAG, "Use " + jsonFile.getPath());
        }

        var content = FileUtils.read(jsonFile);
        var json = JSONArray.parseArray(content);
        for (int i = 0; i < json.size(); i++) {
            var element = json.getJSONObject(i);
            var newDownloadInfo = ExternalDownloadInfo.externalDownloadInfoFromJson(element);

            var file = new File(newDownloadInfo.filePath);
            if (file.exists()) {
                list.add(newDownloadInfo);
            }
        }

        return list;
    }

    // endregion

    // region FastScroller Implements

    @Override
    public void onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
    }

    @Override
    public void onEndDragHandler() {
        // Restore right drawer
        if (null != mRecyclerView && !mRecyclerView.isInCustomChoice()) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        }
    }

    // endregion

    // region EasyRecyclerView Implements

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        Activity activity = getActivity2();
        EasyRecyclerView recyclerView = mRecyclerView;
        if (null == activity || null == recyclerView) {
            return false;
        }

        if (recyclerView.isInCustomChoice()) {
            recyclerView.toggleItemChecked(position);
            return true;
        } else {
            List<ExternalDownloadInfo> list = mList;
            if (list == null) {
                return false;
            }
            if (position < 0 || position >= list.size()) {
                return false;
            }

            var info = list.get(positionInList(position));
            var file = new File(info.filePath);
            var contentUri = Uri.fromFile(file);

            Intent intent = new Intent(activity, GalleryActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(contentUri);
            startActivity(intent);

            return true;
        }
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        EasyRecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return false;
        }

        if (!recyclerView.isInCustomChoice()) {
            recyclerView.intoCustomChoiceMode();
        }
        recyclerView.toggleItemChecked(position);

        return true;
    }

    // endregion

    // region FabLayout Implements

    @SuppressLint("RtlHardcoded")
    @Override
    public void onExpand(boolean expanded) {
        if (null == mActionFabDrawable) {
            return;
        }

        if (expanded) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            mActionFabDrawable.setDelete(ANIMATE_TIME);
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
            mActionFabDrawable.setAdd(ANIMATE_TIME);
        }
    }

    @Override
    public void onClickPrimaryFab(FabLayout view, FloatingActionButton fab) {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
            return;
        }
        if (mRecyclerView != null && !mRecyclerView.isInCustomChoice()) {
            mRecyclerView.intoCustomChoiceMode();
            return;
        }
        view.toggle();
    }

    @Override
    public void onClickSecondaryFab(FabLayout view, FloatingActionButton fab, int position) {
        Context context = getEHContext();
        Activity activity = getActivity2();
        EasyRecyclerView recyclerView = mRecyclerView;
        if (null == context || null == activity || null == recyclerView) {
            return;
        }

        if (MOVE_BUTTON_INDEX == position) {
            List<ExternalDownloadInfo> list = mList;
            if (list == null) {
                return;
            }

            LongList gidList = null;
            List<DownloadInfo> downloadInfoList = null;
            boolean collectGid = position == 1 || position == 2 || position == 3; // Start, Stop, Delete
            boolean collectDownloadInfo = position == 3 || position == 4; // Delete or Move
            if (collectGid) {
                gidList = new LongList();
            }
            if (collectDownloadInfo) {
                downloadInfoList = new LinkedList<>();
            }

            SparseBooleanArray stateArray = mRecyclerView.getCheckedItemPositions();
            for (int i = 0, n = stateArray.size(); i < n; i++) {
                if (stateArray.valueAt(i)) {
                    DownloadInfo info = list.get(positionInList(stateArray.keyAt(i)));
                    if (collectDownloadInfo) {
                        downloadInfoList.add(info);
                    }
                    if (collectGid) {
                        gidList.add(info.gid);
                    }
                }
            }

            List<DownloadLabel> labelRawList = EhApplication.getDownloadManager(context).getLabelList();
            List<String> labelList = new ArrayList<>(labelRawList.size() + 1);
            labelList.add(getString(R.string.default_download_label_name));
            for (int i = 0, n = labelRawList.size(); i < n; i++) {
                labelList.add(labelRawList.get(i).getLabel());
            }
            String[] labels = labelList.toArray(new String[labelList.size()]);

            MoveDialogHelper helper = new MoveDialogHelper(labels, downloadInfoList);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.download_move_dialog_title)
                    .setItems(labels, helper)
                    .show();
        }
        else {
            Toast.makeText(context, R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
        }
    }

    // endregion

    // region SearchBar.OnStageChangeListener Implements
    @Override
    public void onStateChange(SearchBar searchBar, int newState, int oldState, boolean animation) {

    }
    // endregion

    // region SearchBarMover Implements

    @Override
    public boolean isValidView(RecyclerView recyclerView) {
        return false;
    }

    @Nullable
    @Override
    public RecyclerView getValidRecyclerView() {
        return null;
    }

    @Override
    public boolean forceShowSearchBar() {
        return false;
    }

    // endregion
}
