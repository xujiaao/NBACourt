package com.xujiaao.android.court;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Adapter;

public class CourtView extends ViewGroup {

    private static final long ANIMATION_DURATION = 1500L;

    private static final float DEFAULT_DST_SCALE_X = .7F;
    private static final float DEFAULT_DST_SCALE_Y = .5F;

    private final Rect mCourtBounds = new Rect();

    private final CameraHelper mCameraHelper = new CameraHelper();

    private boolean mExpanded;
    private boolean mFirstLayout;

    private int mCourtDrawableWidth;
    private int mCourtDrawableHeight;
    private int mCourtDrawableInsetLeft;
    private int mCourtDrawableInsetTop;
    private int mCourtDrawableInsetRight;
    private int mCourtDrawableInsetBottom;

    private float mCurrentProgress;

    private Drawable mCourtDrawable;
    private Animator mCourtAnimator;

    private Adapter mAdapter;
    private DataSetObserver mDataSetObserver;

    private OnCourtStateChangedListener mOnCourtStateChangedListener;

    public CourtView(Context context) {
        super(context);

        initialize(context, null);
    }

    public CourtView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initialize(context, attrs);
    }

    private void initialize(Context context, @Nullable AttributeSet attrs) {
        setWillNotDraw(false);
        setClipToPadding(false);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CourtView, 0, 0);
        setCourt(a.getDrawable(R.styleable.CourtView_court));
        setCourtScaleX(a.getFloat(R.styleable.CourtView_courtScaleX, DEFAULT_DST_SCALE_X));
        setCourtScaleY(a.getFloat(R.styleable.CourtView_courtScaleY, DEFAULT_DST_SCALE_Y));

        final int insets = a.getDimensionPixelSize(R.styleable.CourtView_courtInsets, 0);
        setCourtInsets(
                a.getDimensionPixelSize(R.styleable.CourtView_courtInsetLeft, insets),
                a.getDimensionPixelSize(R.styleable.CourtView_courtInsetTop, insets),
                a.getDimensionPixelSize(R.styleable.CourtView_courtInsetRight, insets),
                a.getDimensionPixelSize(R.styleable.CourtView_courtInsetBottom, insets));

        a.recycle();
    }

    public void setCourt(int resource) {
        setCourt(getResources().getDrawable(resource));
    }

    public void setCourt(Drawable drawable) {
        if (mCourtDrawable == drawable) {
            return;
        }

        if (mCourtDrawable != null) {
            mCourtDrawable.setCallback(null);
            unscheduleDrawable(mCourtDrawable);
        }

        final int dw = mCourtDrawableWidth;
        final int dh = mCourtDrawableHeight;

        mCourtDrawable = drawable;

        if (drawable != null) {
            drawable.setCallback(this);

            DrawableCompat.setLayoutDirection(drawable, ViewCompat.getLayoutDirection(this));
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }

            mCourtDrawableWidth = drawable.getIntrinsicWidth();
            mCourtDrawableHeight = drawable.getIntrinsicHeight();
        } else {
            mCourtDrawableWidth = -1;
            mCourtDrawableHeight = -1;
        }

        if (dw != mCourtDrawableWidth || dh != mCourtDrawableHeight) {
            requestLayout();
        } else if (mFirstLayout) {
            configureBounds();
        }

        invalidate();
    }

    public void setCourtScaleX(float scaleX) throws IllegalArgumentException {
        if (scaleX <= 0F || scaleX >= 1F) {
            throw new IllegalArgumentException("Scale X MUST in range (0F, 1F).");
        }

        if (mCameraHelper.getScaleX() != scaleX) {
            mCameraHelper.setScaleX(scaleX);

            requestLayout();
            invalidate();
        }
    }

    public void setCourtScaleY(float scaleY) throws IllegalArgumentException {
        if (scaleY <= 0F || scaleY >= 1F) {
            throw new IllegalArgumentException("Scale Y MUST in range (0F, 1F).");
        }

        if (mCameraHelper.getScaleY() != scaleY) {
            mCameraHelper.setScaleY(scaleY);

            requestLayout();
            invalidate();
        }
    }

    public void setCourtInsets(int left, int top, int right, int bottom) {
        if (mCourtDrawableInsetLeft != left || mCourtDrawableInsetTop != top
                || mCourtDrawableInsetRight != right || mCourtDrawableInsetBottom != bottom) {
            mCourtDrawableInsetLeft = left;
            mCourtDrawableInsetTop = top;
            mCourtDrawableInsetRight = right;
            mCourtDrawableInsetBottom = bottom;

            requestLayout();
            invalidate();
        }
    }

    public void setExpanded(boolean expanded, boolean animate) {
        if (mExpanded != expanded) {
            mExpanded = expanded;

            final float sp = mCurrentProgress;
            final float ep = expanded ? 1F : 0F;

            if (!animate || Math.abs(sp - ep) <= .01F) {
                setCourtProgress(ep);
            } else {
                if (mCourtAnimator != null) {
                    mCourtAnimator.cancel();
                }

                final ObjectAnimator animator = ObjectAnimator.ofFloat(this, "courtProgress", sp, ep);
                animator.setDuration((long) (ANIMATION_DURATION * Math.abs(sp - ep)));
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.addListener(new CourtAnimatorListener());
                animator.start();

                mCourtAnimator = animator;
            }

            onCourtStateChanged();
        }
    }

    private void setCourtProgress(float progress) {
        if (mCurrentProgress != progress) {
            mCurrentProgress = progress;

            invalidate();
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public boolean isCourtAnimationRunning() {
        return mCourtAnimator != null;
    }

    public void setAdapter(Adapter adapter) {
        if (mAdapter == adapter && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        mAdapter = adapter;

        if (adapter != null) {
            if (mDataSetObserver == null) {
                mDataSetObserver = new CourtDataSetObserver();
            }

            adapter.registerDataSetObserver(mDataSetObserver);
        }

        onDataSetChanged();
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    void onDataSetChanged() {
        int position = 0;

        final Adapter adapter = mAdapter;
        if (adapter != null) {
            final int visibility = isChildrenVisible() ? VISIBLE : GONE;

            for (int itemCount = adapter.getCount(); position < itemCount; position++) {
                final View scrap = getChildAt(position);
                final View child = adapter.getView(position, scrap, this);
                if (child != scrap) {
                    child.setVisibility(visibility);

                    addView(child, position);
                }
            }
        }

        final int removeCount = getChildCount() - position;
        if (removeCount > 0) {
            removeViews(position, removeCount);
        }
    }

    public void setOnCourtStateChangedListener(OnCourtStateChangedListener listener) {
        mOnCourtStateChangedListener = listener;
    }

    void onCourtStateChanged() {
        final int visibility = isChildrenVisible() ? VISIBLE : GONE;

        for (int index = 0, childCount = getChildCount(); index < childCount; index++) {
            getChildAt(index).setVisibility(visibility);
        }

        if (mOnCourtStateChangedListener != null) {
            mOnCourtStateChangedListener.onCourtStateChanged(this);
        }
    }

    private boolean isChildrenVisible() {
        return isExpanded() && !isCourtAnimationRunning();
    }

    public CameraState getCameraState() {
        return mCameraHelper.getCameraState();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int ph = getPaddingLeft() + getPaddingRight();
        final int pv = getPaddingTop() + getPaddingBottom();

        final boolean hasW = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED;
        final boolean hasH = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED;

        final float fw = mCourtDrawableWidth;
        final float fh = mCourtDrawableHeight;

        int mw = Math.max((hasW ? MeasureSpec.getSize(widthMeasureSpec) : 0) - ph, 0);
        int mh = Math.max((hasH ? MeasureSpec.getSize(heightMeasureSpec) : 0) - pv, 0);

        if (fw > 0F && fh > 0F) {
            final float sh = fh * mCameraHelper.getScaleX();

            if (hasW && hasH) {
                final float scale = Math.min(mw / fw, mh / sh);
                mw = Math.round(fw * scale);
                mh = Math.round(sh * scale);
            } else if (hasW) {
                mh = Math.round(sh * mw / fw);
            } else if (hasH) {
                mw = Math.round(fw * mh / sh);
            } else {
                mw = Math.round(fw);
                mh = Math.round(sh);
            }
        }

        mw = resolveSize(Math.max(mw + ph, getSuggestedMinimumWidth()), widthMeasureSpec);
        mh = resolveSize(Math.max(mh + pv, getSuggestedMinimumHeight()), heightMeasureSpec);

        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mw, MeasureSpec.AT_MOST);
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mh, MeasureSpec.AT_MOST);
        for (int index = 0, childCount = getChildCount(); index < childCount; index++) {
            final View child = getChildAt(index);
            if (child.getVisibility() != GONE) {
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        setMeasuredDimension(mw, mh);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        configureBounds();

        final Rect bounds = mCourtBounds;
        final Matrix matrix = mCameraHelper.getMatrix(1F, false);

        final int centerX = (right - left + getPaddingLeft() - getPaddingRight()) / 2;
        final int centerY = (bottom - top + getPaddingTop() - getPaddingBottom()) / 2;
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        for (int index = 0, childCount = getChildCount(); index < childCount; index++) {
            final View child = getChildAt(index);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final float lx = mapLocation(lp.x, lp.viewportLeft, lp.viewportRight, bounds.left, bounds.right);
                final float ly = mapLocation(lp.y, lp.viewportTop, lp.viewportBottom, bounds.top, bounds.bottom);
                final float[] point = mCameraHelper.mapPoint(matrix, lx, ly);

                final int cx = Math.round(point[0]) + centerX;
                final int cy = Math.round(point[1]) + centerY;
                final int cw = child.getMeasuredWidth();
                final int ch = child.getMeasuredHeight();

                final int absoluteGravity = GravityCompat.getAbsoluteGravity(lp.gravity, layoutDirection);

                int cl;
                int ct;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.LEFT:
                        cl = cx + lp.leftMargin;
                        break;
                    case Gravity.RIGHT:
                        cl = cx - cw - lp.rightMargin;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                    default:
                        cl = cx - cw / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                }

                switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.TOP:
                        ct = cy + lp.topMargin;
                        break;
                    case Gravity.BOTTOM:
                        ct = cy - ch - lp.bottomMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                    default:
                        ct = cy - ch / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                }

                child.layout(cl, ct, cl + cw, ct + ch);
            }
        }

        mFirstLayout = true;
    }

    private static float mapLocation(float srcLocation, float srcStart, float srcEnd, float dstStart, float dstEnd) {
        return dstStart + (dstEnd - dstStart) * (srcLocation - srcStart) / (srcEnd - srcStart);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final Drawable court = mCourtDrawable;
        if (court != null) {
            final int saveCount = canvas.save();

            final float centerX = (getWidth() + getPaddingLeft() - getPaddingRight()) * .5F;
            final float centerY = (getHeight() + getPaddingTop() - getPaddingBottom()) * .5F;
            canvas.translate(centerX, centerY);
            canvas.concat(mCameraHelper.getMatrix(mCurrentProgress, true));

            court.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mCourtDrawable != null) {
            DrawableCompat.setLayoutDirection(mCourtDrawable, layoutDirection);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mCourtDrawable != null && mCourtDrawable.isStateful()) {
            mCourtDrawable.setState(getDrawableState());
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mCourtDrawable != null) {
            DrawableCompat.setHotspot(mCourtDrawable, x, y);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return mCourtDrawable == drawable || super.verifyDrawable(drawable);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();

        if (mCourtDrawable != null) {
            mCourtDrawable.jumpToCurrentState();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (drawable == mCourtDrawable) {
            final int w = drawable.getIntrinsicWidth();
            final int h = drawable.getIntrinsicHeight();
            if (w != mCourtDrawableWidth || h != mCourtDrawableHeight) {
                mCourtDrawableWidth = w;
                mCourtDrawableHeight = h;

                configureBounds();
            }

            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    private void configureBounds() {
        final CameraHelper helper = mCameraHelper;

        final int vw = getWidth() - getPaddingLeft() - getPaddingRight();
        final int vh = getHeight() - getPaddingTop() - getPaddingBottom();

        float scale;
        int dw = mCourtDrawableWidth;
        int dh = mCourtDrawableHeight;

        if (dw <= 0 || dh <= 0) {
            scale = 1F;
            dw = vw;
            dh = Math.round(vh / helper.getScaleX());
        } else {
            scale = Math.min((float) vw / dw, (float) vh / dh / helper.getScaleX());
            dw = Math.round(dw * scale);
            dh = Math.round(dh * scale);
        }

        helper.setHeight(dh);

        final Rect bounds = mCourtBounds;
        bounds.left = -dw / 2;
        bounds.top = -dh / 2;
        bounds.right = bounds.left + dw;
        bounds.bottom = bounds.top + dh;

        if (mCourtDrawable != null) {
            mCourtDrawable.setBounds(bounds);
        }

        bounds.left += (int) (mCourtDrawableInsetLeft * scale);
        bounds.top += (int) (mCourtDrawableInsetTop * scale);
        bounds.right -= (int) (mCourtDrawableInsetRight * scale);
        bounds.bottom -= (int) (mCourtDrawableInsetBottom * scale);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Tools
    // -----------------------------------------------------------------------------------------------------------------

    private class CourtAnimatorListener extends AnimatorListenerAdapter {

        @Override
        public void onAnimationEnd(Animator animation) {
            mCourtAnimator = null;

            onCourtStateChanged();
        }
    }

    private class CourtDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            onDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            onDataSetChanged();
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Camera
    // -----------------------------------------------------------------------------------------------------------------

    private static class CameraHelper {

        private final float[] mTmpPoint = new float[2];

        private final Camera mTmpCamera = new Camera();
        private final Matrix mTmpMatrix = new Matrix();

        private final CameraState mCameraState = new CameraState();

        private final int mDefaultCameraDistance;

        private int mHeight;
        private float mScaleX;
        private float mScaleY;

        CameraHelper() {
            mDefaultCameraDistance = getDefaultCameraDistance();
        }

        private int getDefaultCameraDistance() {
            final float sz = 100F;
            final float sy = 100F;

            final Camera camera = mTmpCamera;
            final Matrix matrix = mTmpMatrix;

            camera.save();
            camera.translate(0F, 0F, sz);
            camera.getMatrix(matrix);
            camera.restore();

            return Math.round(sz / (sy / mapPoint(matrix, 0F, sy)[1] - 1F));
        }

        void setHeight(int height) {
            mHeight = height;
        }

        void setScaleX(float scaleX) {
            mScaleX = scaleX;
        }

        float getScaleX() {
            return mScaleX;
        }

        void setScaleY(float scaleY) {
            mScaleY = scaleY;
        }

        float getScaleY() {
            return mScaleY;
        }

        Matrix getMatrix(float progress, boolean updateState) {
            final Matrix matrix = mTmpMatrix;

            final float d = mDefaultCameraDistance;
            final float h = mHeight;

            final float sz = d / mScaleX - d;
            final float ez = sz * (1F - progress);

            final float cy = .5F * ((float) Math.sqrt(h * h - sz * sz) - mScaleY * h) * progress;
            final float ch = .5F * (float) Math.sqrt(h * h - sz * sz * progress * progress);

            final float sy = cy + ch;
            final float ey = cy - ch;

            final float rotate = (float) (Math.asin((sz - ez) / h) * 180D / Math.PI);
            final float offset = (2F * d * d * cy + d * (sz * ey + ez * sy)) / (d * (sz + ez) + sz * ez);

            final float ty = .5F * h * (cy + offset) / ch;
            final float tz = .5F * (ez * (sy + offset) - sz * (ey + offset)) / ch;

            // get matrix.
            final Camera camera = mTmpCamera;
            camera.save();
            camera.translate(0F, 0F, tz);
            camera.rotateX(rotate);
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(0F, -ty);
            matrix.postTranslate(0F, offset);

            if (updateState) {
                final CameraState state = mCameraState;
                state.ready = true;
                state.cameraY = -offset;
                state.cameraZ = -d;
                state.courtStartY = sy;
                state.courtStartZ = sz;
                state.courtEndY = ey;
                state.courtEndZ = ez;
            }

            return matrix;
        }

        CameraState getCameraState() {
            return mCameraState;
        }

        float[] mapPoint(Matrix matrix, float x, float y) {
            final float[] point = mTmpPoint;
            point[0] = x;
            point[1] = y;
            matrix.mapPoints(point);

            return point;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class CameraState {

        public boolean ready;

        public float cameraY;
        public float cameraZ;

        public float courtStartY;
        public float courtStartZ;

        public float courtEndY;
        public float courtEndZ;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // LayoutParams
    // -----------------------------------------------------------------------------------------------------------------

    public static class LayoutParams extends MarginLayoutParams {

        public int gravity = Gravity.CENTER;

        public float x;
        public float y;

        public float viewportLeft;
        public float viewportTop;
        public float viewportRight;
        public float viewportBottom;

        @SuppressWarnings("WeakerAccess")
        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @SuppressWarnings("WeakerAccess")
        public LayoutParams() {
            super(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }

        @SuppressWarnings("WeakerAccess")
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------------------------------------------------

    public interface OnCourtStateChangedListener {

        void onCourtStateChanged(CourtView court);
    }
}