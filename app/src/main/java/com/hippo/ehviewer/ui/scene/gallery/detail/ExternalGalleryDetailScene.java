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

package com.hippo.ehviewer.ui.scene.gallery.detail;

import static com.hippo.ehviewer.client.EhClient.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.transition.TransitionInflater;

import com.hippo.android.resource.AttrResources;
import com.hippo.beerbelly.BeerBelly;
import com.hippo.drawable.RoundSideRectDrawable;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhFilter;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhTagDatabase;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryCommentList;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.data.TorrentDownloadMessage;
import com.hippo.ehviewer.client.exception.NoHAtHClientException;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.ExternalDownloadInfo;
import com.hippo.ehviewer.dao.Filter;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.annotation.WholeLifeCircle;
import com.hippo.ehviewer.ui.dialog.ArchiverDownloadDialog;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.ui.scene.download.DownloadsScene;
import com.hippo.ehviewer.ui.scene.EhCallback;
import com.hippo.ehviewer.ui.scene.FavoritesScene;
import com.hippo.ehviewer.ui.scene.GalleryCommentsScene;
import com.hippo.ehviewer.ui.scene.GalleryInfoScene;
import com.hippo.ehviewer.ui.scene.GalleryPreviewsScene;
import com.hippo.ehviewer.ui.scene.gallery.list.EnterGalleryDetailTransaction;
import com.hippo.ehviewer.ui.scene.history.HistoryScene;
import com.hippo.ehviewer.ui.scene.TransitionNameFactory;
import com.hippo.ehviewer.ui.scene.gallery.list.GalleryListScene;
import com.hippo.ehviewer.util.AppCenterAnalytics;
import com.hippo.ehviewer.util.ClipboardUtil;
import com.hippo.ehviewer.widget.GalleryRatingBar;
import com.hippo.reveal.ViewAnimationUtils;
import com.hippo.ripple.Ripple;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.TransitionHelper;
import com.hippo.text.Html;
import com.hippo.text.URLImageGetter;
import com.hippo.util.AppHelper;
import com.hippo.ehviewer.download.DownloadTorrentManager;
import com.hippo.util.DrawableManager;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.FileUtils;
import com.hippo.util.ReadableTime;
import com.hippo.view.ViewTransition;
import com.hippo.widget.AutoWrapLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.ObservedTextView;
import com.hippo.widget.ProgressView;
import com.hippo.widget.SimpleGridAutoSpanLayout;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.IntIdGenerator;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import com.hippo.ehviewer.spider.SpiderQueen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import okhttp3.OkHttpClient;

public class ExternalGalleryDetailScene extends BaseScene implements View.OnClickListener, View.OnLongClickListener
{
    // region Definitions
    @IntDef({STATE_INIT, STATE_NORMAL, STATE_REFRESH, STATE_REFRESH_HEADER, STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    private static class ExitTransaction implements TransitionHelper {

        private final View mThumb;

        public ExitTransaction(View thumb) {
            mThumb = thumb;
        }

        @Override
        public boolean onTransition(Context context,
                                    FragmentTransaction transaction, Fragment exit, Fragment enter) {
            if (!(enter instanceof GalleryListScene) && !(enter instanceof DownloadsScene) &&
                    !(enter instanceof FavoritesScene) && !(enter instanceof HistoryScene)) {
                return false;
            }

            String transitionName = ViewCompat.getTransitionName(mThumb);
            if (transitionName != null) {
                exit.setSharedElementReturnTransition(
                        TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
                exit.setExitTransition(
                        TransitionInflater.from(context).inflateTransition(R.transition.trans_fade));
                enter.setSharedElementEnterTransition(
                        TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
                enter.setEnterTransition(
                        TransitionInflater.from(context).inflateTransition(R.transition.trans_fade));
                transaction.addSharedElement(mThumb, transitionName);
            }
            return true;
        }
    }

    // endregion

    // region Constants
    private static final int REQUEST_CODE_COMMENT_GALLERY = 0;

    private static final int STATE_INIT = -1;
    private static final int STATE_NORMAL = 0;
    private static final int STATE_REFRESH = 1;
    private static final int STATE_REFRESH_HEADER = 2;
    private static final int STATE_FAILED = 3;

    public final static String KEY_ACTION = "action";
    public static final String ACTION_GALLERY_INFO = "action_gallery_info";
    public static final String ACTION_DOWNLOAD_GALLERY_INFO = "action_download_gallery_info";
    public static final String ACTION_GID_TOKEN = "action_gid_token";

    public static final String KEY_GALLERY_INFO = "gallery_info";

    public static final String KEY_COME_FROM_DOWNLOAD = "come_from_download";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_PAGE = "page";

    private static final String KEY_GALLERY_DETAIL = "gallery_detail";
    private static final String KEY_REQUEST_ID = "request_id";

    private static final boolean TRANSITION_ANIMATION_DISABLED = true;
    // endregion

    // region Variables

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private TextView mTip;
    @Nullable
    private ViewTransition mViewTransition;
    // Header
    @Nullable
    private View mHeader;
    @Nullable
    private View mColorBg;
    @Nullable
    private LoadImageView mThumb;
    @Nullable
    private TextView mTitle;
    @Nullable
    private TextView mUploader;
    @Nullable
    private TextView mCategory;
    @Nullable
    private ImageView mOtherActions;
    @Nullable
    private ViewGroup mActionGroup;
    @Nullable
    private TextView mDownload;
    @Nullable
    private TextView mHaveNewVersion;
    @Nullable
    private View mRead;
    // Below header
    @Nullable
    private View mBelowHeader;
    // Info
    @Nullable
    private View mInfo;
    @Nullable
    private TextView mLanguage;
    @Nullable
    private TextView mPages;
    @Nullable
    private TextView mSize;
    @Nullable
    private TextView mPosted;
    @Nullable
    private TextView mFavoredTimes;
    // Actions
    @Nullable
    private View mActions;
    @Nullable
    private TextView mSimilar;
    @Nullable
    private TextView mSearchCover;
    // Tags
    @Nullable
    private LinearLayout mTags;
    @Nullable
    private TextView mNoTags;
    // Comments
    @Nullable
    private LinearLayout mComments;
    @Nullable
    private TextView mCommentsText;
    // Previews
    @Nullable
    private View mPreviews;
    @Nullable
    private SimpleGridAutoSpanLayout mGridLayout;
    @Nullable
    private TextView mPreviewText;
    // Progress
    @Nullable
    private View mProgress;
    @Nullable
    private ViewTransition mViewTransition2;
    @Nullable
    private PopupMenu mPopupMenu;

    @WholeLifeCircle
    private int mDownloadState;

    @Nullable
    private String mAction;
    @Nullable
    private ExternalDownloadInfo mGalleryInfo;
    private DownloadInfo mDownloadInfo;
    private long mGid;
    private String mToken;

    @Nullable
    private GalleryDetail mGalleryDetail;
    private int mRequestId = IntIdGenerator.INVALID_ID;

    private Pair<String, String>[] mTorrentList;

    private String mArchiveFormParamOr;
    private Pair<String, String>[] mArchiveList;

    @Nullable
    private Map<String, String> properties;

    @State
    private int mState = STATE_INIT;

    private boolean mModifingFavorites;

    @Nullable
    private AlertDialog downLoadAlertDialog;
    @Nullable
    private View torrentDownloadView;
    @Nullable
    private TextView downloadProgress;

    private GalleryUpdateDialog myUpdateDialog;

    private boolean useNetWorkLoadThumb = false;

    private boolean comeFromDownload = false;

    private Context mContext;
    private MainActivity activity;

    private ExecutorService executorService;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // endregion

    // region Fragments

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            handleArgs(getArguments());
        } else {
            mAction = savedInstanceState.getString(KEY_ACTION);
            mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
            mGid = savedInstanceState.getLong(KEY_GID);
            mToken = savedInstanceState.getString(KEY_TOKEN);
            mGalleryDetail = savedInstanceState.getParcelable(KEY_GALLERY_DETAIL);
            mRequestId = savedInstanceState.getInt(KEY_REQUEST_ID);
        }

        if (null == properties && mGalleryInfo != null) {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            properties = new HashMap<>();
            properties.put("Title", mGalleryInfo.title);
            properties.put("Time", dateFormat.format(date));
            AppCenterAnalytics.trackEvent("进入画廊详情页", properties);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAction != null) {
            outState.putString(KEY_ACTION, mAction);
        }
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putLong(KEY_GID, mGid);
        if (mToken != null) {
            outState.putString(KEY_TOKEN, mAction);
        }
        if (mGalleryDetail != null) {
            outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail);
        }
        outState.putInt(KEY_REQUEST_ID, mRequestId);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Context context = getEHContext();
        AssertUtils.assertNotNull(context);

        setDrawerGestureBlocker(null);

        mTip = null;
        mViewTransition = null;

        mHeader = null;
        mColorBg = null;
        mThumb = null;
        mTitle = null;
        mUploader = null;
        mCategory = null;
        mOtherActions = null;
        mActionGroup = null;
        mDownload = null;
        mHaveNewVersion = null;
        mRead = null;
        mBelowHeader = null;

        mInfo = null;
        mLanguage = null;
        mPages = null;
        mSize = null;
        mPosted = null;
        mFavoredTimes = null;

        mActions = null;
        mSimilar = null;
        mSearchCover = null;

        mTags = null;
        mNoTags = null;

        mComments = null;
        mCommentsText = null;

        mPreviews = null;
        mGridLayout = null;
        mPreviewText = null;

        mProgress = null;

        mViewTransition2 = null;

        mPopupMenu = null;

        properties = null;
    }

    @Override
    public void onBackPressed() {
        if (mViewTransition != null && mThumb != null &&
                mViewTransition.getShownViewIndex() == 0 && mThumb.isShown()) {
            int[] location = new int[2];
            mThumb.getLocationInWindow(location);
            // Only show transaction when thumb can be seen
            if (location[1] + mThumb.getHeight() > 0) {
                setTransitionName();
                finish(new ExitTransaction(mThumb));
                return;
            }
        }
        finish();
    }

    @Override
    protected void onSceneResult(int requestCode, int resultCode, Bundle data) {
        if (requestCode == REQUEST_CODE_COMMENT_GALLERY) {
            if (resultCode != RESULT_OK || data == null) {
                return;
            }
            GalleryCommentList comments = data.getParcelable(GalleryCommentsScene.KEY_COMMENT_LIST);
            if (mGalleryDetail == null || comments == null) {
                return;
            }
            mGalleryDetail.comments = comments;
            bindComments(comments.comments);
        } else {
            super.onSceneResult(requestCode, resultCode, data);
        }
    }


    // endregion

    // region BaseScene

    @Nullable
    @Override
    public View onCreateView2(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getEHContext();
        // Get download state
        long gid = getGid();
        if (gid != -1) {
            AssertUtils.assertNotNull(context);
            mDownloadState = EhApplication.getDownloadManager(context).getDownloadState(gid);
        } else {
            mDownloadState = DownloadInfo.STATE_INVALID;
        }

        torrentDownloadView = View.inflate(context, R.layout.notification_contentview, null);

        View view = inflater.inflate(R.layout.scene_gallery_detail, container, false);

        ViewGroup main = (ViewGroup) ViewUtils.$$(view, R.id.main);
        View mainView = ViewUtils.$$(main, R.id.scroll_view);
        View progressView = ViewUtils.$$(main, R.id.progress_view);
        mTip = (TextView) ViewUtils.$$(main, R.id.tip);
        mViewTransition = new ViewTransition(mainView, progressView, mTip);

        assert context != null;
        AssertUtils.assertNotNull(context);

        View actionsScrollView = ViewUtils.$$(view, R.id.actions_scroll_view);
        setDrawerGestureBlocker(new DrawerLayout.GestureBlocker() {
            private void transformPointToViewLocal(int[] point, View child) {
                ViewParent viewParent = child.getParent();

                while (viewParent instanceof View) {
                    View view = (View) viewParent;
                    point[0] += view.getScrollX() - child.getLeft();
                    point[1] += view.getScrollY() - child.getTop();

                    if (view instanceof DrawerLayout) {
                        break;
                    }

                    child = view;
                    viewParent = child.getParent();
                }
            }

            @Override
            public boolean shouldBlockGesture(MotionEvent ev) {
                int[] point = new int[]{(int) ev.getX(), (int) ev.getY()};
                transformPointToViewLocal(point, actionsScrollView);
                return !isDrawersVisible()
                        && point[0] > 0 && point[0] < actionsScrollView.getWidth()
                        && point[1] > 0 && point[1] < actionsScrollView.getHeight();
            }
        });

        Drawable drawable = DrawableManager.getVectorDrawable(context, R.drawable.big_sad_pandroid);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mTip.setCompoundDrawables(null, drawable, null, null);
        mTip.setOnClickListener(this);

        mBelowHeader = mainView.findViewById(R.id.below_header);
        View belowHeader = mBelowHeader;

        boolean isDarkTheme = !AttrResources.getAttrBoolean(context, androidx.appcompat.R.attr.isLightTheme);
        mHeader = ViewUtils.$$(belowHeader, R.id.header);
        mColorBg = ViewUtils.$$(mHeader, R.id.color_bg);
        mThumb = (LoadImageView) ViewUtils.$$(mHeader, R.id.thumb);
        mTitle = (TextView) ViewUtils.$$(mHeader, R.id.title);
        mUploader = (TextView) ViewUtils.$$(mHeader, R.id.uploader);
        mCategory = (TextView) ViewUtils.$$(mHeader, R.id.category);
        mOtherActions = (ImageView) ViewUtils.$$(mHeader, R.id.other_actions);
        mActionGroup = (ViewGroup) ViewUtils.$$(mHeader, R.id.action_card);
        mDownload = (TextView) ViewUtils.$$(mActionGroup, R.id.download);
        mHaveNewVersion = (TextView) ViewUtils.$$(mHeader, R.id.new_version);
        mRead = ViewUtils.$$(mActionGroup, R.id.read);
        Ripple.addRipple(mOtherActions, isDarkTheme);
        Ripple.addRipple(mDownload, isDarkTheme);
        Ripple.addRipple(mRead, isDarkTheme);
        mUploader.setOnClickListener(this);
        mCategory.setOnClickListener(this);
        mOtherActions.setOnClickListener(this);
        mDownload.setOnClickListener(this);
        mDownload.setOnLongClickListener(this);
        mHaveNewVersion.setOnClickListener(this);
        mRead.setOnClickListener(this);
        mTitle.setOnClickListener(this);

        mUploader.setOnLongClickListener(this);


        mInfo = ViewUtils.$$(belowHeader, R.id.info);
        mLanguage = (TextView) ViewUtils.$$(mInfo, R.id.language);
        mPages = (TextView) ViewUtils.$$(mInfo, R.id.pages);
        mSize = (TextView) ViewUtils.$$(mInfo, R.id.size);
        mPosted = (TextView) ViewUtils.$$(mInfo, R.id.posted);
        mFavoredTimes = (TextView) ViewUtils.$$(mInfo, R.id.favoredTimes);
        Ripple.addRipple(mInfo, isDarkTheme);
        mInfo.setOnClickListener(this);

        mActions = ViewUtils.$$(belowHeader, R.id.actions);
        mSimilar = (TextView) ViewUtils.$$(mActions, R.id.similar);
        mSearchCover = (TextView) ViewUtils.$$(mActions, R.id.search_cover);
        Ripple.addRipple(mSimilar, isDarkTheme);
        Ripple.addRipple(mSearchCover, isDarkTheme);
        mSimilar.setOnClickListener(this);
        mSearchCover.setOnClickListener(this);
        ensureActionDrawable(context);

        mTags = (LinearLayout) ViewUtils.$$(belowHeader, R.id.tags);
        mNoTags = (TextView) ViewUtils.$$(mTags, R.id.no_tags);

        mComments = (LinearLayout) ViewUtils.$$(belowHeader, R.id.comments);
        mCommentsText = (TextView) ViewUtils.$$(mComments, R.id.comments_text);
        if (!Settings.getShowGalleryComment()) {
            mComments.setVisibility(View.GONE);
            mCommentsText.setVisibility(View.GONE);
        }

        Ripple.addRipple(mComments, isDarkTheme);
        mComments.setOnClickListener(this);

        mPreviews = ViewUtils.$$(belowHeader, R.id.previews);
        mGridLayout = (SimpleGridAutoSpanLayout) ViewUtils.$$(mPreviews, R.id.grid_layout);
        mPreviewText = (TextView) ViewUtils.$$(mPreviews, R.id.preview_text);
        Ripple.addRipple(mPreviews, isDarkTheme);
        mPreviews.setOnClickListener(this);

        mProgress = ViewUtils.$$(mainView, R.id.progress);

        mViewTransition2 = new ViewTransition(mBelowHeader, mProgress);

        // Setup View
        bindViewSecond();
        setTransitionName();

        return view;
    }

    // endregion

    // region OnClickListener

    @Override
    public void onClick(View v) {
        mContext = getEHContext();
        activity = getActivity2();
        if (null == mContext || null == activity) {
            return;
        }

        if (mTip == v) {
            adjustViewVisibility(STATE_REFRESH, true);
            // TODO: implement refresh
            Log.e(TAG, "mTip clicked!");
        } else if (mOtherActions == v) {
            ensurePopMenu();
            if (mPopupMenu != null) {
                mPopupMenu.show();
            }
        } else if (mUploader == v) {
            String uploader = getUploader();
            if (TextUtils.isEmpty(uploader)) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_UPLOADER);
            lub.setKeyword(uploader);
            GalleryListScene.startScene(this, lub);
        } else if (mCategory == v) {
            int category = getCategory();
            if (category == -1) {
                return;
            }
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setCategory(category);
            GalleryListScene.startScene(this, lub);
        } else if (mDownload == v) {
            Toast.makeText(mContext, R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
        } else if (mHaveNewVersion == v) {
            if (mGalleryDetail == null) {
                return;
            }
            // myUpdateDialog.showSelectDialog(mGalleryDetail);
        } else if (mRead == v) {
            GalleryInfo galleryInfo = getGalleryInfo();
            if (galleryInfo != null) {

                Intent intent = new Intent(activity, GalleryActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                var file = new File(Environment.getExternalStorageDirectory() + "/Download/1.zip");
                var contentUri = Uri.fromFile(file);
                System.out.println("<Flag> path = " + contentUri + ", file.exists() = " + file.exists());
                intent.setData(contentUri);

                startActivity(intent);
            }
        } else if (mInfo == v) {
            Bundle args = new Bundle();
            args.putParcelable(GalleryInfoScene.KEY_GALLERY_DETAIL, mGalleryInfo.galleryDetail);
            startScene(new Announcer(GalleryInfoScene.class).setArgs(args));
        } else if (mSimilar == v) {
            showSimilarGalleryList();
        } else if (mSearchCover == v) {
            showCoverGalleryList();
        } else if (mComments == v) {
            if (mGalleryDetail == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putLong(GalleryCommentsScene.KEY_API_UID, mGalleryDetail.apiUid);
            args.putString(GalleryCommentsScene.KEY_API_KEY, mGalleryDetail.apiKey);
            args.putLong(GalleryCommentsScene.KEY_GID, mGalleryDetail.gid);
            args.putString(GalleryCommentsScene.KEY_TOKEN, mGalleryDetail.token);
            args.putParcelable(GalleryCommentsScene.KEY_COMMENT_LIST, mGalleryDetail.comments);
            startScene(new Announcer(GalleryCommentsScene.class)
                    .setArgs(args)
                    .setRequestCode(this, REQUEST_CODE_COMMENT_GALLERY));
        } else if (mPreviews == v) {
            if (null != mGalleryDetail) {
                Bundle args = new Bundle();
                args.putParcelable(GalleryPreviewsScene.KEY_GALLERY_INFO, mGalleryDetail);
                startScene(new Announcer(GalleryPreviewsScene.class).setArgs(args));
            }
        } else if (mTitle == v) {
            if (mGalleryDetail != null && mGalleryDetail.title != null) {
                ClipboardUtil.copyText(mGalleryDetail.title);
                Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        } else {
            Object o = v.getTag(R.id.tag);
            if (o instanceof String) {
                String tag = (String) o;
                ListUrlBuilder lub = new ListUrlBuilder();
                lub.setMode(ListUrlBuilder.MODE_TAG);
                lub.setKeyword(tag);
                GalleryListScene.startScene(this, lub);
                return;
            }

            GalleryInfo galleryInfo = getGalleryInfo();
            o = v.getTag(R.id.index);
            if (null != galleryInfo && o instanceof Integer) {
                int index = (Integer) o;
                Intent intent = new Intent(mContext, GalleryActivity.class);
                intent.setAction(GalleryActivity.ACTION_EH);
                intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, galleryInfo);
                intent.putExtra(GalleryActivity.KEY_PAGE, index);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        mContext = getEHContext();
        activity = getActivity2();
        if (null == activity) {
            return false;
        }

        if (mUploader == v) {
            showFilterUploaderDialog();
        } else {
            String tag = (String) v.getTag(R.id.tag);
            if (null != tag) {
                showTagDialog(tag);
                return true;
            }
        }

        return false;
    }

    // endregion

    // region Private Methods

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        String action = args.getString(KEY_ACTION);
        mAction = action;
        if (ACTION_GALLERY_INFO.equals(action)) {
            mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
            // Add history
            if (null != mGalleryInfo) {
                EhDB.putHistoryInfo(mGalleryInfo);
            }
        } else if (ACTION_GID_TOKEN.equals(action)) {
            mGid = args.getLong(KEY_GID);
            mToken = args.getString(KEY_TOKEN);
        } else if (ACTION_DOWNLOAD_GALLERY_INFO.equals(action)) {
            try {
                mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
                if (null != mGalleryInfo) {
                    EhDB.putHistoryInfo(mGalleryInfo);
                }
            } catch (ClassCastException e) {
                mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
                if (null != mGalleryInfo) {
                    EhDB.putHistoryInfo(mGalleryInfo);
                }
            }
            // Add history

        }
        comeFromDownload = args.getBoolean(KEY_COME_FROM_DOWNLOAD);
    }

    @Nullable
    private String getGalleryDetailUrl() {
        long gid;
        String token;
        if (mGalleryDetail != null) {
            gid = mGalleryDetail.gid;
            token = mGalleryDetail.token;
        } else if (mGalleryInfo != null) {
            gid = mGalleryInfo.gid;
            token = mGalleryInfo.token;
        } else if (ACTION_GID_TOKEN.equals(mAction)) {
            gid = mGid;
            token = mToken;
        } else {
            return null;
        }
        return EhUrl.getGalleryDetailUrl(gid, token, 0, false);
    }

    // -1 for error
    private long getGid() {
        GalleryInfo info = getGalleryInfo();
        if (info == null && ACTION_GID_TOKEN.equals(mAction)){
            return mGid;
        }
        return info == null ? null : info.gid;
    }

    private String getToken() {
        GalleryInfo info = getGalleryInfo();
        if (info == null && ACTION_GID_TOKEN.equals(mAction)){
            return mToken;
        }
        return info == null ? null : info.token;
    }

    private String getUploader() {
        GalleryInfo info = getGalleryInfo();
        return info == null ? null : info.uploader;
    }

    // -1 for error
    private int getCategory() {
        GalleryInfo info = getGalleryInfo();
        return info == null ? -1 : info.category;
    }

    private GalleryInfo getGalleryInfo() {
        if (null != mGalleryDetail) {
            return mGalleryDetail;
        } else if (null != mGalleryInfo) {
            return mGalleryInfo;
        } else {
            return null;
        }
    }

    private void ensureActionDrawable(Context context) {
        Drawable similar = DrawableManager.getVectorDrawable(context, R.drawable.v_similar_primary_x48);
        if (mSimilar != null) {
            setActionDrawable(mSimilar, similar);
        }
        Drawable searchCover = DrawableManager.getVectorDrawable(context, R.drawable.v_file_find_primary_x48);
        if (mSearchCover != null) {
            setActionDrawable(mSearchCover, searchCover);
        }
    }

    private void setActionDrawable(TextView text, Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        text.setCompoundDrawables(null, drawable, null, null);
    }

    private boolean createCircularReveal() {
        if (mColorBg == null) {
            return false;
        }

        int w = mColorBg.getWidth();
        int h = mColorBg.getHeight();
        if (ViewCompat.isAttachedToWindow(mColorBg) && w != 0 && h != 0) {
            Context context = getEHContext();
            if (context == null) {
                return false;
            }
            Resources resources = context.getResources();
            int keylineMargin = resources.getDimensionPixelSize(R.dimen.keyline_margin);
            int thumbWidth = resources.getDimensionPixelSize(R.dimen.gallery_detail_thumb_width);
            int thumbHeight = resources.getDimensionPixelSize(R.dimen.gallery_detail_thumb_height);

            int x = thumbWidth / 2 + keylineMargin;
            int y = thumbHeight / 2 + keylineMargin;

            int radiusX = Math.max(Math.abs(x), Math.abs(w - x));
            int radiusY = Math.max(Math.abs(y), Math.abs(h - y));
            float radius = (float) Math.hypot(radiusX, radiusY);

            ViewAnimationUtils.createCircularReveal(mColorBg, x, y, 0, radius).setDuration(300).start();
            return true;
        } else {
            return false;
        }
    }

    private void adjustViewVisibility(int state, boolean animation) {
        if (state == mState) {
            return;
        }
        if (mViewTransition == null || mViewTransition2 == null) {
            return;
        }

        int oldState = mState;
        mState = state;

        animation = !TRANSITION_ANIMATION_DISABLED && animation;

        switch (state) {
            case STATE_NORMAL:
                // Show mMainView
                mViewTransition.showView(0, animation);
                // Show mBelowHeader
                mViewTransition2.showView(0, animation);
                break;
            case STATE_REFRESH:
                // Show mProgressView
                mViewTransition.showView(1, animation);
                break;
            case STATE_REFRESH_HEADER:
                // Show mMainView
                mViewTransition.showView(0, animation);
                // Show mProgress
                mViewTransition2.showView(1, animation);
                break;
            default:
            case STATE_INIT:
            case STATE_FAILED:
                // Show mFailedView
                mViewTransition.showView(2, animation);
                break;
        }
        Context context = getEHContext();
        if (context == null) {
            return;
        }
        if ((oldState == STATE_INIT || oldState == STATE_FAILED || oldState == STATE_REFRESH) &&
                (state == STATE_NORMAL || state == STATE_REFRESH_HEADER) && AttrResources.getAttrBoolean(context, androidx.appcompat.R.attr.isLightTheme)) {
            if (!createCircularReveal()) {
                SimpleHandler.getInstance().post(this::createCircularReveal);
            }
        }
    }


    private void bindViewSecond() {
        if (mThumb == null || mTitle == null || mUploader == null || mCategory == null ||
                mLanguage == null || mPages == null || mSize == null || mPosted == null ||
                mFavoredTimes == null) {
            return;
        }
        /*Resources resources = getResources2();
        if (gd.newVersions != null && mHaveNewVersion != null && resources != null) {
            mHaveNewVersion.setVisibility(View.VISIBLE);
            mHaveNewVersion.setBackground(ResourcesCompat.getDrawable(resources, R.drawable.new_version_style, null));
        } else {
            if (mHaveNewVersion != null) {
                mHaveNewVersion.setVisibility(View.GONE);
            }
        }
        if (null == mGalleryInfo) {
            mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb);
        } else {
            if (useNetWorkLoadThumb) {
                mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb);
                useNetWorkLoadThumb = false;
            } else {
                mThumb.load(EhCacheKeyFactory.getThumbKey(gd.gid), gd.thumb, false);
            }
        }*/

        ExternalDownloadInfo info = mGalleryInfo;
        mThumb.load(EhCacheKeyFactory.getThumbKey(info.gid), info.thumb);
        mTitle.setText(EhUtils.getSuitableTitle(info));

        mLanguage.setText(info.language);
        mPages.setText("0/" + info.pages + "P");
        mSize.setText(info.size);
        mPosted.setText(info.posted);
        bindTags(info.tags);

        mDownload.setText(R.string.download_state_downloaded);
        mPreviewText.setText(R.string.no_previews);
        mCommentsText.setText(R.string.no_comments);

        // Hide fields
        mUploader.setVisibility(View.GONE);
        mCategory.setVisibility(View.GONE);
        mActions.setVisibility(View.GONE);
        mFavoredTimes.setVisibility(View.GONE);
    }

    @SuppressWarnings("deprecation")
    private void bindTags(GalleryTagGroup[] tagGroups) {
        Context context = getEHContext();
        LayoutInflater inflater = getLayoutInflater2();
        Resources resources = getResources2();
        if (null == context || null == resources || null == mTags || null == mNoTags) {
            return;
        }

        mTags.removeViews(1, mTags.getChildCount() - 1);
        if (tagGroups == null || tagGroups.length == 0) {
            mNoTags.setVisibility(View.VISIBLE);
            return;
        } else {
            mNoTags.setVisibility(View.GONE);
        }

        EhTagDatabase ehTags = Settings.getShowTagTranslations() ? EhTagDatabase.getInstance(context) : null;

        int colorTag = AttrResources.getAttrColor(context, R.attr.tagBackgroundColor);
        int colorName = AttrResources.getAttrColor(context, R.attr.tagGroupBackgroundColor);
        for (GalleryTagGroup tg : tagGroups) {
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.gallery_tag_group, mTags, false);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            mTags.addView(ll);

            String readableTagName = null;
            if (ehTags != null) {
                readableTagName = ehTags.getTranslation("n:" + tg.groupName);
            }

            TextView tgName = (TextView) inflater.inflate(R.layout.item_gallery_tag, ll, false);
            ll.addView(tgName);
            tgName.setText(readableTagName != null ? readableTagName : tg.groupName);
            tgName.setBackgroundDrawable(new RoundSideRectDrawable(colorName));

            String prefix = EhTagDatabase.namespaceToPrefix(tg.groupName);
            if (prefix == null) {
                prefix = "";
            }

            AutoWrapLayout awl = new AutoWrapLayout(context);
            ll.addView(awl, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            for (int j = 0, z = tg.size(); j < z; j++) {
                TextView tag = (TextView) inflater.inflate(R.layout.item_gallery_tag, awl, false);
                awl.addView(tag);
                String tagStr = tg.getTagAt(j);

                String readableTag = null;
                if (ehTags != null) {
                    readableTag = ehTags.getTranslation(prefix + tagStr);
                }

                tag.setText(readableTag != null ? readableTag : tagStr);
                tag.setBackgroundDrawable(new RoundSideRectDrawable(colorTag));
                tag.setTag(R.id.tag, tg.groupName + ":" + tagStr);
                tag.setOnClickListener(this);
                tag.setOnLongClickListener(this);
            }
        }
    }

    private void bindComments(GalleryComment[] comments) {
        Context context = getEHContext();
        LayoutInflater inflater = getLayoutInflater2();
        if (null == context || null == mComments || null == mCommentsText) {
            return;
        }

        mComments.removeViews(0, mComments.getChildCount() - 1);

        final int maxShowCount = 2;
        if (comments == null || comments.length == 0) {
            mCommentsText.setText(R.string.no_comments);
            return;
        } else if (comments.length <= maxShowCount) {
            mCommentsText.setText(R.string.no_more_comments);
        } else {
            mCommentsText.setText(R.string.more_comment);
        }

        int length = Math.min(maxShowCount, comments.length);
        for (int i = 0; i < length; i++) {
            GalleryComment comment = comments[i];
            View v = inflater.inflate(R.layout.item_gallery_comment, mComments, false);
            mComments.addView(v, i);
            TextView user = v.findViewById(R.id.user);
            user.setText(comment.user);
            TextView time = v.findViewById(R.id.time);
            time.setText(ReadableTime.getTimeAgo(comment.time));
            ObservedTextView c = v.findViewById(R.id.comment);
            c.setMaxLines(5);
            c.setText(Html.fromHtml(comment.comment,
                    new URLImageGetter(c, EhApplication.getConaco(context)), null));
        }
    }

    @SuppressLint("SetTextI18n")
    private void bindPreviews(GalleryDetail gd) {
        LayoutInflater inflater = getLayoutInflater2();
        Resources resources = getResources2();
        if (null == resources || null == mGridLayout || null == mPreviewText) {
            return;
        }

        mGridLayout.removeAllViews();
        PreviewSet previewSet = gd.previewSet;
        if (gd.previewPages <= 0 || previewSet == null || previewSet.size() == 0) {
            mPreviewText.setText(R.string.no_previews);
            return;
        } else if (gd.previewPages == 1) {
            mPreviewText.setText(R.string.no_more_previews);
        } else {
            mPreviewText.setText(R.string.more_previews);
        }

        int columnWidth = resources.getDimensionPixelOffset(Settings.getThumbSizeResId());
        mGridLayout.setColumnSize(columnWidth);
        mGridLayout.setStrategy(SimpleGridAutoSpanLayout.STRATEGY_SUITABLE_SIZE);
        for (int i = 0, size = previewSet.size(); i < size; i++) {
            View view = inflater.inflate(R.layout.item_gallery_preview, mGridLayout, false);
            mGridLayout.addView(view);

            LoadImageView image = view.findViewById(R.id.image);
            previewSet.load(image, gd.gid, i);
            image.setTag(R.id.index, i);
            image.setOnClickListener(this);
            TextView text = view.findViewById(R.id.text);
            text.setText(Integer.toString(previewSet.getPosition(i) + 1));
        }
    }

    private static String getRatingText(float rating, Resources resources) {
        int resId;
        switch (Math.round(rating * 2)) {
            case 0:
                resId = R.string.rating0;
                break;
            case 1:
                resId = R.string.rating1;
                break;
            case 2:
                resId = R.string.rating2;
                break;
            case 3:
                resId = R.string.rating3;
                break;
            case 4:
                resId = R.string.rating4;
                break;
            case 5:
                resId = R.string.rating5;
                break;
            case 6:
                resId = R.string.rating6;
                break;
            case 7:
                resId = R.string.rating7;
                break;
            case 8:
                resId = R.string.rating8;
                break;
            case 9:
                resId = R.string.rating9;
                break;
            case 10:
                resId = R.string.rating10;
                break;
            default:
                resId = R.string.rating_none;
                break;
        }

        return resources.getString(resId);
    }

    private void setTransitionName() {
        long gid = getGid();

        if (gid != -1 && mThumb != null &&
                mTitle != null && mUploader != null && mCategory != null) {
            ViewCompat.setTransitionName(mThumb, TransitionNameFactory.getThumbTransitionName(gid));
            ViewCompat.setTransitionName(mTitle, TransitionNameFactory.getTitleTransitionName(gid));
            ViewCompat.setTransitionName(mUploader, TransitionNameFactory.getUploaderTransitionName(gid));
            ViewCompat.setTransitionName(mCategory, TransitionNameFactory.getCategoryTransitionName(gid));
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void ensurePopMenu() {
        if (mPopupMenu != null || mOtherActions == null) {
            return;
        }

        Context context = getEHContext();
        AssertUtils.assertNotNull(context);
        PopupMenu popup = new PopupMenu(context, mOtherActions, Gravity.TOP);
        mPopupMenu = popup;
        popup.getMenuInflater().inflate(R.menu.scene_gallery_detail, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_in_other_app:
                    String url = getGalleryDetailUrl();
                    Activity activity = getActivity2();
                    if (null != url && null != activity) {
                        UrlOpener.openUrl(activity, url, false);
                    }
                    break;
                case R.id.action_refresh:
                    Toast.makeText(getEHContext(), R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        });
    }

    @Nullable
    private static String getArtist(GalleryTagGroup[] tagGroups) {
        if (null == tagGroups) {
            return null;
        }
        for (GalleryTagGroup tagGroup : tagGroups) {
            if ("artist".equals(tagGroup.groupName) && tagGroup.size() > 0) {
                return tagGroup.getTagAt(0);
            }
        }
        return null;
    }

    private void showSimilarGalleryList() {
        GalleryDetail gd = mGalleryDetail;
        if (null == gd) {
            return;
        }
        String keyword = EhUtils.extractTitle(gd.title);
        if (null != keyword) {
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_NORMAL);
            lub.setKeyword("\"" + keyword + "\"");
            GalleryListScene.startScene(this, lub);
            return;
        }
        String artist = getArtist(gd.tags);
        if (null != artist) {
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_TAG);
            lub.setKeyword("artist:" + artist);
            GalleryListScene.startScene(this, lub);
            return;
        }
        if (null != gd.uploader) {
            ListUrlBuilder lub = new ListUrlBuilder();
            lub.setMode(ListUrlBuilder.MODE_UPLOADER);
            lub.setKeyword(gd.uploader);
            GalleryListScene.startScene(this, lub);
        }
    }

    private void showCoverGalleryList() {
        Context context = getEHContext();
        if (null == context) {
            return;
        }
        long gid = getGid();
        if (-1L == gid) {
            return;
        }
        File temp = AppConfig.createTempFile();
        if (null == temp) {
            return;
        }
        BeerBelly beerBelly = EhApplication.getConaco(context).getBeerBelly();

        OutputStream os = null;
        try {
            os = new FileOutputStream(temp);
            if (beerBelly.pullFromDiskCache(EhCacheKeyFactory.getThumbKey(gid), os)) {
//            if (beerBelly.pullRawFromDisk(EhCacheKeyFactory.getThumbKey(gid), os)) {
                ListUrlBuilder lub = new ListUrlBuilder();
                lub.setMode(ListUrlBuilder.MODE_IMAGE_SEARCH);
                lub.setImagePath(temp.getPath());
                lub.setUseSimilarityScan(true);
                lub.setShowExpunged(true);
                GalleryListScene.startScene(this, lub);
            }
        } catch (FileNotFoundException e) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(os);
        }
    }


    private void showFilterUploaderDialog() {
        Context context = getEHContext();
        String uploader = getUploader();
        if (context == null || uploader == null) {
            return;
        }

        new AlertDialog.Builder(context)
                .setMessage(getString(R.string.filter_the_uploader, uploader))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (which != DialogInterface.BUTTON_POSITIVE) {
                        return;
                    }

                    Filter filter = new Filter();
                    filter.mode = EhFilter.MODE_UPLOADER;
                    filter.text = uploader;
                    EhFilter.getInstance().addFilter(filter);

                    showTip(R.string.filter_added, LENGTH_SHORT);
                }).show();
    }

    private void showFilterTagDialog(String tag) {
        Context context = getEHContext();
        if (context == null) {
            return;
        }

        new AlertDialog.Builder(context)
                .setMessage(getString(R.string.filter_the_tag, tag))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (which != DialogInterface.BUTTON_POSITIVE) {
                        return;
                    }

                    Filter filter = new Filter();
                    filter.mode = EhFilter.MODE_TAG;
                    filter.text = tag;
                    EhFilter.getInstance().addFilter(filter);

                    showTip(R.string.filter_added, LENGTH_SHORT);
                }).show();
    }

    private void showTagDialog(final String tag) {
        final Context context = getEHContext();
        if (null == context) {
            return;
        }
        String temp;
        int index = tag.indexOf(':');
        if (index >= 0) {
            temp = tag.substring(index + 1);
        } else {
            temp = tag;
        }
        final String tag2 = temp;

        new AlertDialog.Builder(context)
                .setTitle(tag)
                .setItems(R.array.tag_menu_entries, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            UrlOpener.openUrl(context, EhUrl.getTagDefinitionUrl(tag2), false);
                            break;
                        case 1:
                            showFilterTagDialog(tag);
                            break;
                    }
                })
                .setNegativeButton(R.string.copy_tag, (dialog, which) -> copyTag(tag))
                .show();
    }

    private void copyTag(String tag) {
        Context context = requireContext();
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText(null, tag));
        Toast.makeText(context, R.string.gallery_tag_copy, Toast.LENGTH_LONG).show();
    }

    // endregion
}
