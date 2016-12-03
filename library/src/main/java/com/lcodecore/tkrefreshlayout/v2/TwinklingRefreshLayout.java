package com.lcodecore.tkrefreshlayout.v2;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.lcodecore.tkrefreshlayout.Footer.BottomProgressView;
import com.lcodecore.tkrefreshlayout.IBottomView;
import com.lcodecore.tkrefreshlayout.IHeaderView;
import com.lcodecore.tkrefreshlayout.OnAnimEndListener;
import com.lcodecore.tkrefreshlayout.R;
import com.lcodecore.tkrefreshlayout.header.GoogleDotView;
import com.lcodecore.tkrefreshlayout.utils.DensityUtil;
import com.lcodecore.tkrefreshlayout.utils.ScrollingUtil;

/**
 * Created by lcodecore on 16/3/2.
 */
public class TwinklingRefreshLayout extends FrameLayout {

    //波浪的高度,最大扩展高度
    protected float mWaveHeight;

    //头部的高度
    protected float mHeadHeight;

    //允许的越界回弹的高度
    protected float mOverScrollHeight;

    //子控件
    private View mChildView;

    //头部layout
    protected FrameLayout mHeadLayout;

    private IHeaderView mHeadView;
    private IBottomView mBottomView;

    //底部高度
    private float mBottomHeight;

    //底部layout
    private FrameLayout mBottomLayout;


    //刷新的状态
    protected boolean isRefreshing;

    //加载更多的状态
    protected boolean isLoadingmore;

    //是否需要加载更多,默认需要
    protected boolean enableLoadmore = true;
    //是否需要下拉刷新,默认需要
    protected boolean enableRefresh = true;

    //是否在越界回弹的时候显示下拉图标
    protected boolean isOverlayRefreshShow = true;

    //是否隐藏刷新控件,开启越界回弹模式(开启之后刷新控件将隐藏)
    protected boolean isPureScrollModeOn = false;

    //是否自动加载更多
    protected boolean autoLoadMore = true;

    //是否开启悬浮刷新模式
    protected boolean floatRefresh = true;

    //满足越界的手势的最低速度(默认5000)
    protected int overScrollMinVx = 5000;


    private CoProcessor cp;

    public TwinklingRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public TwinklingRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwinklingRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwinklingRefreshLayout, defStyleAttr, 0);
        mWaveHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_wave_height, (int) DensityUtil.dp2px(context, 120));
        mHeadHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_head_height, (int) DensityUtil.dp2px(context, 80));
        mBottomHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_bottom_height, (int) DensityUtil.dp2px(context, 60));
        mOverScrollHeight = a.getDimensionPixelSize(R.styleable.TwinklingRefreshLayout_tr_overscroll_height, (int) DensityUtil.dp2px(context, 80));
        enableLoadmore = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_enable_loadmore, true);
        isPureScrollModeOn = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_pureScrollMode_on, false);
        isOverlayRefreshShow = a.getBoolean(R.styleable.TwinklingRefreshLayout_tr_show_overlay_refreshview, true);
        a.recycle();

        cp = new CoProcessor();
    }

    private void init() {
        //使用isInEditMode解决可视化编辑器无法识别自定义控件的问题
        if (isInEditMode()) return;

        setPullListener(new TwinklingPullListener());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //添加头部
        if (mHeadLayout == null) {
            FrameLayout headViewLayout = new FrameLayout(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mHeadHeight);
            layoutParams.gravity = Gravity.TOP;
            headViewLayout.setLayoutParams(layoutParams);

            mHeadLayout = headViewLayout;

            this.addView(mHeadLayout);//addView(view,-1)添加到-1的位置

            if (mHeadView == null) setHeaderView(new GoogleDotView(getContext()));
            mHeadLayout.setTranslationY(mHeadHeight);
        }

        //添加底部
        if (mBottomLayout == null) {
            FrameLayout bottomViewLayout = new FrameLayout(getContext());
            LayoutParams layoutParams2 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mBottomHeight);
            layoutParams2.gravity = Gravity.BOTTOM;
            bottomViewLayout.setLayoutParams(layoutParams2);

            mBottomLayout = bottomViewLayout;
            this.addView(mBottomLayout);

            if (mBottomView == null) {
                BottomProgressView mProgressView = new BottomProgressView(getContext());
                setBottomView(mProgressView);
            }
            mBottomLayout.setTranslationY(mBottomHeight);
        }

        //获得子控件
        mChildView = getChildAt(0);
    }


    /*************************************  触摸事件处理  *****************************************/
    /**
     * 拦截事件
     *
     * @return return true时,ViewGroup的事件有效,执行onTouchEvent事件
     * return false时,事件向下传递,onTouchEvent无效
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = cp.interceptTouchEvent(ev);
        return intercept || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean resume = cp.consumeTouchEvent(e);
        return resume || super.onTouchEvent(e);
    }


    //主动刷新
    public void startRefresh() {
        cp.startRefresh();
    }

    //主动加载跟多
    public void startLoadMore() {
        cp.startLoadMore();
    }


    /*************************************  开放api区  *****************************************/
    /**
     * 刷新结束
     */
    public void finishRefreshing() {
        cp.finishRefreshing();
    }

    /**
     * 加载更多结束
     */
    public void finishLoadmore() {
        cp.finishLoadmore();
    }

    public void setOverScrollMinVx(int overScrollMinVx) {
        this.overScrollMinVx = overScrollMinVx;
    }

    /**
     * 设置头部View
     */
    public void setHeaderView(final IHeaderView headerView) {
        if (headerView != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mHeadLayout.removeAllViewsInLayout();
                    mHeadLayout.addView(headerView.getView());
                }
            });
            mHeadView = headerView;
        }
    }

    /**
     * 设置底部View
     */
    public void setBottomView(final IBottomView bottomView) {
        if (bottomView != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    mBottomLayout.removeAllViewsInLayout();
                    mBottomLayout.addView(bottomView.getView());
                }
            });
            mBottomView = bottomView;
        }
    }

    /**
     * 设置wave的下拉高度
     *
     * @param waveHeight
     */
    public void setWaveHeight(float waveHeight) {
        this.mWaveHeight = waveHeight;
    }

    /**
     * 设置下拉头的高度
     */
    public void setHeaderHeight(float headHeight) {
        this.mHeadHeight = headHeight;
    }

    /**
     * 设置底部高度
     */
    public void setBottomHeight(float bottomHeight) {
        this.mBottomHeight = bottomHeight;
    }

    /**
     * 是否允许加载更多
     */
    public void setEnableLoadmore(boolean enableLoadmore1) {
        enableLoadmore = enableLoadmore1;
        if (mBottomView != null) {
            if (enableLoadmore) mBottomView.getView().setVisibility(VISIBLE);
            else mBottomView.getView().setVisibility(GONE);
        }
    }

    /**
     * 是否允许下拉刷新
     */
    public void setEnableRefresh(boolean enableRefresh1) {
        this.enableRefresh = enableRefresh1;
    }

    /**
     * 是否允许越界时显示刷新控件
     */
    public void setEnableOverlayRefreshView(boolean enableShow) {
        isOverlayRefreshShow = enableShow;
    }

    /**
     * 是否开启纯净的越界回弹模式,开启时刷新和加载更多控件不显示
     */
    public void setPureScrollModeOn(boolean pureScrollModeOn) {
        isPureScrollModeOn = pureScrollModeOn;
        isOverlayRefreshShow = !isPureScrollModeOn;
        if (pureScrollModeOn) {
            setWaveHeight(mOverScrollHeight);
            setHeaderHeight(mOverScrollHeight);
            setBottomHeight(mOverScrollHeight);
        }
    }

    /**
     * 设置刷新控件监听器
     */
    private RefreshListenerAdapter refreshListener;

    public void setOnRefreshListener(RefreshListenerAdapter refreshListener) {
        if (refreshListener != null) {
            this.refreshListener = refreshListener;
        }
    }

    /**
     * 设置拖动屏幕的监听器
     */
    private PullListener pullListener;

    private void setPullListener(PullListener pullListener) {
        this.pullListener = pullListener;
    }

    private class TwinklingPullListener implements PullListener {

        @Override
        public void onPullingDown(TwinklingRefreshLayout refreshLayout, float fraction) {
            mHeadView.onPullingDown(fraction, mWaveHeight, mHeadHeight);
            if (refreshListener != null) refreshListener.onPullingDown(refreshLayout, fraction);
        }

        @Override
        public void onPullingUp(TwinklingRefreshLayout refreshLayout, float fraction) {
            mBottomView.onPullingUp(fraction, mWaveHeight, mHeadHeight);
            if (refreshListener != null) refreshListener.onPullingUp(refreshLayout, fraction);
        }

        @Override
        public void onPullDownReleasing(TwinklingRefreshLayout refreshLayout, float fraction) {
            mHeadView.onPullReleasing(fraction, mWaveHeight, mHeadHeight);
            if (refreshListener != null)
                refreshListener.onPullDownReleasing(refreshLayout, fraction);
        }

        @Override
        public void onPullUpReleasing(TwinklingRefreshLayout refreshLayout, float fraction) {
            mBottomView.onPullReleasing(fraction, mWaveHeight, mHeadHeight);
            if (refreshListener != null) refreshListener.onPullUpReleasing(refreshLayout, fraction);
        }

        @Override
        public void onRefresh(TwinklingRefreshLayout refreshLayout) {
            mHeadView.startAnim(mWaveHeight, mHeadHeight);
            if (refreshListener != null) refreshListener.onRefresh(refreshLayout);
        }

        @Override
        public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
            mBottomView.startAnim(mWaveHeight, mHeadHeight);
            if (refreshListener != null) refreshListener.onLoadMore(refreshLayout);
        }

        @Override
        public void onFinishRefresh() {
            mHeadView.onFinish(new OnAnimEndListener() {
                @Override
                public void onAnimEnd() {
                    cp.finishRefreshAfterAnim();
                }
            });
        }

        @Override
        public void onFinishLoadMore() {
            mBottomView.onFinish();
        }
    }

    public class CoProcessor {
        private RefreshProcessor refreshProcessor;
        private OverScrollProcessor overScrollProcessor;
        private AnimProcessor animProcessor;

        private final static int PULLING_TOP_DOWN = 0;
        private final static int PULLING_BOTTOM_UP = 1;
        private int state = PULLING_TOP_DOWN;


        public CoProcessor() {
            animProcessor = new AnimProcessor(this);
            overScrollProcessor = new OverScrollProcessor(this);
            refreshProcessor = new RefreshProcessor(this);
        }

        public void init() {
            if (isPureScrollModeOn) {
                setEnableOverlayRefreshView(false);
                if (mHeadLayout != null) mHeadLayout.setVisibility(GONE);
                if (mBottomLayout != null) mBottomLayout.setVisibility(GONE);
            } else {
                setEnableOverlayRefreshView(true);
                if (mHeadLayout != null) mHeadLayout.setVisibility(VISIBLE);
                if (mBottomLayout != null) mBottomLayout.setVisibility(VISIBLE);
            }


            overScrollProcessor.init();
            animProcessor.init();
        }

        public Context getContext() {
            return TwinklingRefreshLayout.this.getContext();
        }

        public int getTouchSlop() {
            return ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        public AnimProcessor getAnimProcessor() {
            return animProcessor;
        }

        public boolean interceptTouchEvent(MotionEvent ev) {
            return refreshProcessor.interceptTouchEvent(ev);
        }

        public boolean consumeTouchEvent(MotionEvent ev) {
            return refreshProcessor.consumeTouchEvent(ev);
        }

        public float getMaxHeadHeight() {
            return mWaveHeight;
        }

        public float getHeadHeight() {
            return mHeadHeight;
        }

        public float getBottomHeight() {
            return mBottomHeight;
        }

        public View getScrollableView() {
            return mChildView;
        }

        public View getHeadLayout() {
            return mHeadLayout;
        }

        public View getBottomLayout() {
            return mBottomLayout;
        }

        public View getContent() {
            return mChildView;
        }

        public View getHeader() {
            return mHeadLayout;
        }

        public View getFooter() {
            return mBottomLayout;
        }

        public void startRefresh() {
            post(new Runnable() {
                @Override
                public void run() {
                    setStatePTD();
                    if (!isPureScrollModeOn && mChildView != null) {
                        setRefreshing(true);
                        animProcessor.animLayoutByTime(0, (int) mHeadHeight);
                        if (pullListener != null) {
                            pullListener.onRefresh(TwinklingRefreshLayout.this);
                        }
                    }
                }
            });
        }

        public void startLoadMore() {
            post(new Runnable() {
                @Override
                public void run() {
                    setStatePBU();
                    if (!isPureScrollModeOn && mChildView != null) {
                        setLoadingMore(true);
                        animProcessor.animLayoutByTime(0, (int) mBottomHeight);
                        if (pullListener != null) {
                            pullListener.onLoadMore(TwinklingRefreshLayout.this);
                        }
                    }
                }
            });
        }

        public void finishRefreshing() {
            setRefreshing(false);
            onFinishRefresh();
        }

        public void finishRefreshAfterAnim() {
            if (mChildView != null) {
                animProcessor.animLayoutByTime((int) mHeadHeight, 0);
            }
        }

        public void finishLoadmore() {
            setLoadingMore(false);
            onFinishLoadMore();
            if (mChildView != null) {
                ScrollingUtil.scrollAViewBy(mChildView, (int) mBottomHeight);
                animProcessor.animLayoutByTime((int) mBottomHeight, 0);
            }
        }

        private boolean enableOverScrollTop = false, enableOverScrollBottom = false;

        public boolean allowPullDown() {
            return enableRefresh || enableOverScrollTop;
        }

        public boolean allowPullUp() {
            return enableLoadmore || enableOverScrollBottom;
        }

        public boolean allowOverScroll() {
            return (!isRefreshing && !isLoadingmore);
        }

        public boolean isRefreshing() {
            return isRefreshing;
        }

        public boolean isLoadingmore() {
            return isLoadingmore;
        }

        public void setRefreshing(boolean refreshing) {
            isRefreshing = refreshing;
        }

        public void setLoadingMore(boolean loadingMore) {
            isLoadingmore = loadingMore;
        }


        public boolean isOpenFloatRefresh() {
            return floatRefresh;
        }

        public boolean isPureScrollModeOn() {
            return isPureScrollModeOn;
        }

        public boolean isOverScrollRefreshShow() {
            return isOverlayRefreshShow;
        }


        public void onPullingDown(float offsetY) {
            pullListener.onPullingDown(TwinklingRefreshLayout.this, offsetY / mHeadHeight);
        }

        public void onPullingUp(float offsetY) {
            pullListener.onPullingUp(TwinklingRefreshLayout.this, offsetY / mBottomHeight);
        }

        public void onRefresh() {
            pullListener.onRefresh(TwinklingRefreshLayout.this);
        }

        public void onLoadMore() {
            pullListener.onLoadMore(TwinklingRefreshLayout.this);
        }

        public void onFinishRefresh() {
            pullListener.onFinishRefresh();
        }

        public void onFinishLoadMore() {
            pullListener.onFinishLoadMore();
        }

        public void onPullDownReleasing(float offsetY) {
            pullListener.onPullDownReleasing(TwinklingRefreshLayout.this, offsetY / mHeadHeight);
        }

        public void onPullUpReleasing(float offsetY) {
            pullListener.onPullUpReleasing(TwinklingRefreshLayout.this, offsetY / mBottomHeight);
        }

        public void setStatePTD() {
            state = PULLING_TOP_DOWN;
        }

        public void setStatePBU() {
            state = PULLING_BOTTOM_UP;
        }

        public boolean isStatePTD() {
            return PULLING_TOP_DOWN == state;
        }

        public boolean isStatePBU() {
            return PULLING_BOTTOM_UP == state;
        }
    }
}
