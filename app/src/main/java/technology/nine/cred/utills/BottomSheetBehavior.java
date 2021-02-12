package technology.nine.cred.utills;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import technology.nine.cred.R;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static technology.nine.cred.utills.ViewUtils.doOnApplyWindowInsets;
import static technology.nine.cred.utills.ViewUtils.isLayoutRtl;


public class BottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {


    public abstract static class BottomSheetCallback {

        public abstract void onStateChanged(@NonNull View bottomSheet, @State int newState);


        public abstract void onSlide(@NonNull View bottomSheet, float slideOffset);
    }

    public static final int STATE_DRAGGING = 1;

    public static final int STATE_SETTLING = 2;

    public static final int STATE_EXPANDED = 3;

    public static final int STATE_COLLAPSED = 4;

    @IntDef({STATE_EXPANDED, STATE_COLLAPSED, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    private static final float HIDE_THRESHOLD = 0.5f;
    private static final float HIDE_FRICTION = 0.1f;

    private float mMinimumVelocity;

    private int mPeekHeight;
    public static final int PEEK_HEIGHT_AUTO = -1;
    private boolean peekHeightAuto;

    private int mMinOffset;
    private int mMaxOffset;

    private static final int DEFAULT_ANCHOR_POINT = 700;
    private int mAnchorPoint;

    private boolean mHideable;

    private boolean mCollapsible;

    @State
    private int mState = STATE_COLLAPSED;
    @State
    private int mLastStableState = STATE_COLLAPSED;

    private ViewDragHelper mViewDragHelper;

    private boolean mIgnoreEvents;

    private boolean mNestedScrolled;

    private int mParentHeight;
    private int mParentWidth;
    private int childHeight;


    private WeakReference<V> mViewRef;

    private WeakReference<View> mNestedScrollingChildRef;

    private Vector<BottomSheetCallback> mCallback;

    private int mActivePointerId;

    private int mInitialY;

    private boolean mTouchingScrollingChild;

    private boolean draggable;

    private SettleRunnable settleRunnable = null;
    private boolean isShapeExpanded;

    private boolean shapeThemingEnabled;

    private MaterialShapeDrawable materialShapeDrawable;
    private static final int DEF_STYLE_RES = R.style.Widget_Design_BottomSheet_Modal;

    private ShapeAppearanceModel shapeAppearanceModelDefault;
    private static final int CORNER_ANIMATION_DURATION = 500;

    @Nullable
    private ValueAnimator interpolatorAnimator;
    float elevation = -1;
    private int peekHeightMin;

    @Nullable
    private Map<View, Integer> importantForAccessibilityMap;
    private boolean updateImportantForAccessibilityOnSiblings = false;
    private int peekHeightGestureInsetBuffer;

    private int gestureInsetBottom;
    private boolean gestureInsetBottomIgnored;
    private boolean paddingBottomSystemWindowInsets = true;
    private boolean paddingLeftSystemWindowInsets = true;
    private boolean paddingRightSystemWindowInsets = true;

    private int insetBottom;
    private int insetTop;


    public BottomSheetBehavior() {
    }

    public BottomSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        peekHeightGestureInsetBuffer =
                context.getResources().getDimensionPixelSize(R.dimen.mtrl_min_touch_target_size);


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout);
        TypedArray b = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior);
        this.shapeThemingEnabled = a.hasValue(R.styleable.BottomSheetBehavior_Layout_shapeAppearance);
        createMaterialShapeDrawable(context, attrs, false);

        createShapeValueAnimator();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.elevation = a.getDimension(R.styleable.BottomSheetBehavior_Layout_android_elevation, -1);
        }

        TypedValue value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            setPeekHeight(value.data);
        } else {
            setPeekHeight(
                    a.getDimensionPixelSize(
                            R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO));
        }
        setHideable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        setDraggable(a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_draggable, true));

        setGestureInsetBottomIgnored(
                a.getBoolean(R.styleable.BottomSheetBehavior_Layout_gestureInsetBottomIgnored, false));

        // Reading out if we are handling padding, so we can apply it to the content.
        paddingBottomSystemWindowInsets =
                b.getBoolean(R.styleable.BottomSheetBehavior_paddingBottomSystemWindowInsets, false);
        paddingLeftSystemWindowInsets =
                b.getBoolean(R.styleable.BottomSheetBehavior_paddingLeftSystemWindowInsets, false);
        paddingRightSystemWindowInsets =
                b.getBoolean(R.styleable.BottomSheetBehavior_paddingRightSystemWindowInsets, false);
        a.recycle();
        b.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumVelocity = configuration.getScaledMaximumFlingVelocity();
    }


    @Override
    public Parcelable onSaveInstanceState(@NonNull CoordinatorLayout parent, @NonNull V child) {
        return new SavedState(super.onSaveInstanceState(parent, child), this);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        // Intermediate states are restored as collapsed state
        restoreOptionalState(ss);

        if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            mState = STATE_COLLAPSED;
        } else {
            mState = ss.state;
        }

        mLastStableState = mState;
    }

    private void restoreOptionalState(@NonNull SavedState ss) {
        this.mPeekHeight = ss.peekHeight;
        this.mHideable = ss.hideable;
    }


    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        // First let the parent lay it out
        if (mState != STATE_DRAGGING && mState != STATE_SETTLING) {
            if (parent.getFitsSystemWindows() &&
                    !child.getFitsSystemWindows()) {
                child.setFitsSystemWindows(true);
            }

            parent.onLayoutChild(child, layoutDirection);
        }


        if (mViewRef == null) {
            peekHeightMin =
                    parent.getResources().getDimensionPixelSize(R.dimen.design_bottom_sheet_peek_height_min);

            setWindowInsetsListener(child);
            mViewRef = new WeakReference<>(child);

            if (shapeThemingEnabled && materialShapeDrawable != null) {
                ViewCompat.setBackground(child, materialShapeDrawable);
            }
            // Set elevation on MaterialShapeDrawable
            if (materialShapeDrawable != null) {
                // Use elevation attr if set on bottomsheet; otherwise, use elevation of child view.
                materialShapeDrawable.setElevation(
                        elevation == -1 ? ViewCompat.getElevation(child) : elevation);
                // Update the material shape based on initial state.
                isShapeExpanded = mState == STATE_EXPANDED;
                materialShapeDrawable.setInterpolation(isShapeExpanded ? 0f : 1f);
            }
            updateAccessibilityActions();
            if (ViewCompat.getImportantForAccessibility(child)
                    == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                ViewCompat.setImportantForAccessibility(child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }

        // Offset the bottom sheet
        mParentHeight = parent.getHeight();
        mParentWidth = parent.getWidth();
        childHeight = child.getHeight();

        if (mParentHeight - childHeight < insetTop) {
            childHeight = mParentHeight;
        }

        mMinOffset = Math.max(0, mParentHeight - child.getHeight());
        mMaxOffset = Math.max(mParentHeight - mPeekHeight, mMinOffset);

        if (mState == STATE_EXPANDED) {
            ViewCompat.offsetTopAndBottom(child, mMinOffset);
        } else if (mState == STATE_COLLAPSED) {
            ViewCompat.offsetTopAndBottom(child, mMaxOffset);
        }
        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }


        mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown() || !draggable) {
            mIgnoreEvents = true;
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchingScrollingChild = false;
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mScrollVelocityTracker.clear();
                int initialX = (int) event.getX();
                mInitialY = (int) event.getY();
                View scroll = mNestedScrollingChildRef.get();
                if (scroll != null && parent.isPointInChildBounds(scroll, initialX, mInitialY)) {
                    mActivePointerId = event.getPointerId(event.getActionIndex());
                    mTouchingScrollingChild = true;
                }
                mIgnoreEvents = mActivePointerId == MotionEvent.INVALID_POINTER_ID &&
                        !parent.isPointInChildBounds(child, initialX, mInitialY);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }

        if (!mIgnoreEvents && mViewDragHelper.shouldInterceptTouchEvent(event)) {
            return true;
        }
        // We have to handle cases that the ViewDragHelper does not capture the bottom sheet because
        // it is not the top most view of its parent. This is not necessary when the touch event is
        // happening over the scrolling content as nested scrolling logic handles that case.
        View scroll = mNestedScrollingChildRef.get();
        boolean ret = action == MotionEvent.ACTION_MOVE && scroll != null &&
                !mIgnoreEvents && mState != STATE_DRAGGING &&
                !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) &&
                Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop();
        return ret;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (!child.isShown() || !draggable) {
            return false;
        }

        int action = event.getActionMasked();
        if (mState == STATE_DRAGGING && action == MotionEvent.ACTION_DOWN) {
            return true;
        }

        // Detect scroll direction for ignoring collapsible
        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getY() > mInitialY && !mCollapsible) {
                reset();
                return false;
            }
        }

        if (mViewDragHelper == null) {
            mViewDragHelper = ViewDragHelper.create(parent, mDragCallback);
        }

        mViewDragHelper.processTouchEvent(event);

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
        }

        // The ViewDragHelper tries to capture only the top-most View. We have to explicitly tell it
        // to capture the bottom sheet in case it is not captured and the touch slop is passed.
        if (action == MotionEvent.ACTION_MOVE && !mIgnoreEvents) {
            if (Math.abs(mInitialY - event.getY()) > mViewDragHelper.getTouchSlop()) {
                mViewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
            }
        }
        return !mIgnoreEvents;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child,
                                       View directTargetChild, View target, int nestedScrollAxes,
                                       @ViewCompat.NestedScrollType int type) {
        if (!draggable) {
            return false;
        }
        mNestedScrolled = false;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    private ScrollVelocityTracker mScrollVelocityTracker = new ScrollVelocityTracker();

    private class ScrollVelocityTracker {
        private long mPreviousScrollTime = 0;
        private float mScrollVelocity = 0;

        public void recordScroll(int dy) {
            long now = System.currentTimeMillis();

            if (mPreviousScrollTime != 0) {
                long elapsed = now - mPreviousScrollTime;
                mScrollVelocity = (float) dy / elapsed * 1000; // pixels per sec
            }

            mPreviousScrollTime = now;
        }

        public void clear() {
            mPreviousScrollTime = 0;
            mScrollVelocity = 0;
        }

        public float getScrollVelocity() {
            return mScrollVelocity;
        }
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                                  int dx, int dy, int[] consumed,
                                  @ViewCompat.NestedScrollType int type) {


        View scrollingChild = mNestedScrollingChildRef.get();
        if (target != scrollingChild) {
            return;
        }

        mScrollVelocityTracker.recordScroll(dy);

        int currentTop = child.getTop();
        int newTop = currentTop - dy;

        // Force stop at the anchor - do not go from collapsed to expanded in one scroll
        if (
                (mLastStableState == STATE_COLLAPSED && newTop < mAnchorPoint) ||
                        (mLastStableState == STATE_EXPANDED && newTop > mAnchorPoint)
        ) {
            consumed[1] = dy;
            ViewCompat.offsetTopAndBottom(child, mAnchorPoint - currentTop);
            dispatchOnSlide(child.getTop());
            mNestedScrolled = true;
            return;
        }

        if (dy > 0) { // Upward
            if (newTop < mMinOffset) {
                consumed[1] = currentTop - mMinOffset;
                ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                setStateInternal(STATE_EXPANDED);
            } else {
                if (!draggable) {
                    // Prevent dragging
                    return;
                }


                consumed[1] = dy;
                ViewCompat.offsetTopAndBottom(child, -dy);
                setStateInternal(STATE_DRAGGING);
            }
        } else if (dy < 0) { // Downward
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTop <= mMaxOffset || mHideable) {
                    if (!draggable) {
                        // Prevent dragging
                        return;
                    }

                    // Restrict STATE_COLLAPSED if restrictedState is set
                    if (mCollapsible == true || (mCollapsible == false && (mAnchorPoint - newTop) >= 0)) {
                        consumed[1] = dy;
                        ViewCompat.offsetTopAndBottom(child, -dy);
                        setStateInternal(STATE_DRAGGING);
                    }
                } else {
                    consumed[1] = currentTop - mMaxOffset;
                    ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                    setStateInternal(STATE_COLLAPSED);
                }
            }
        }
        dispatchOnSlide(child.getTop());
        mNestedScrolled = true;
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target,
                                   @ViewCompat.NestedScrollType int type) {

        if (!draggable) {
            return;
        }
        if (child.getTop() == mMinOffset) {
            setStateInternal(STATE_EXPANDED);
            mLastStableState = STATE_EXPANDED;
            return;
        }
        if (target != mNestedScrollingChildRef.get() || !mNestedScrolled) {
            return;
        }
        int top;
        int targetState = 6;
        // Are we flinging up?
        float scrollVelocity = mScrollVelocityTracker.getScrollVelocity();
        if (scrollVelocity > mMinimumVelocity) {
            if (mLastStableState == STATE_COLLAPSED) {
                // Fling from collapsed to anchor
                top = mAnchorPoint;
            } else {
                // We are already expanded
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            }
        } else
            // Are we flinging down?
            if (scrollVelocity < -mMinimumVelocity) {
                if (mLastStableState == STATE_EXPANDED) {
                    // Fling to from expanded to anchor
                    top = mAnchorPoint;
                } else if (mCollapsible) {
                    top = mMaxOffset;
                    targetState = STATE_COLLAPSED;
                } else {
                    top = mAnchorPoint;
                }
            }
            // Not flinging, just settle to the nearest state
            else {
                // Collapse?
                int currentTop = child.getTop();
                if (currentTop > mAnchorPoint * 1.25 && mCollapsible == true) { // Multiply by 1.25 to account for parallax. The currentTop needs to be pulled down 50% of the anchor point before collapsing.
                    top = mMaxOffset;
                    targetState = STATE_COLLAPSED;
                }
                // Expand?
                else if (currentTop < mAnchorPoint * 0.5) {
                    top = mMinOffset;
                    targetState = STATE_EXPANDED;
                }
                // Snap back to the anchor
                else {
                    top = mAnchorPoint;
                }
            }

        mLastStableState = targetState;
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            setStateInternal(STATE_SETTLING);
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, targetState));
        } else {
            setStateInternal(targetState);
        }
        mNestedScrolled = false;
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target,
                                    float velocityX, float velocityY) {
        if (!draggable) {
            return false;
        }

        return target == mNestedScrollingChildRef.get() &&
                (mState != STATE_EXPANDED ||
                        super.onNestedPreFling(coordinatorLayout, child, target,
                                velocityX, velocityY));
    }

    public final void setPeekHeight(int peekHeight) {
        mPeekHeight = Math.max(0, peekHeight);
        mMaxOffset = mParentHeight - peekHeight;
    }

    public final int getPeekHeight() {
        return mPeekHeight;
    }

    public void setAnchorPoint(int anchorPoint) {
        mAnchorPoint = anchorPoint;
    }

    public int getAnchorPoint() {
        return mAnchorPoint;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isDraggable() {
        return draggable;
    }


    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    public boolean isHideable() {
        return mHideable;
    }


    public void setCollapsible(boolean collapsible) {
        mCollapsible = collapsible;
    }

    public boolean isCollapsible() {
        return mCollapsible;
    }

    public void setGestureInsetBottomIgnored(boolean gestureInsetBottomIgnored) {
        this.gestureInsetBottomIgnored = gestureInsetBottomIgnored;
    }

    public boolean isGestureInsetBottomIgnored() {
        return gestureInsetBottomIgnored;
    }


    public final void setPeekHeight(int peekHeight, boolean animate) {
        boolean layout = false;
        if (peekHeight == PEEK_HEIGHT_AUTO) {
            if (!peekHeightAuto) {
                peekHeightAuto = true;
                layout = true;
            }
        } else if (peekHeightAuto || this.mPeekHeight != peekHeight) {
            peekHeightAuto = false;
            this.mParentHeight = max(0, peekHeight);
            layout = true;
        }
        // If sheet is already laid out, recalculate the collapsed offset based on new setting.
        // Otherwise, let onLayoutChild handle this later.
        if (layout) {
            updatePeekHeight(animate);
        }
    }

    private void updatePeekHeight(boolean animate) {
        if (mViewRef != null) {
            calculateCollapsedOffset();
            if (mState == STATE_COLLAPSED) {
                V view = mViewRef.get();
                if (view != null) {
                    if (animate) {
                        settleToStatePendingLayout(mState);
                    } else {
                        view.requestLayout();
                    }
                }
            }
        }
    }


    public void addBottomSheetCallback(BottomSheetCallback callback) {
        if (mCallback == null)
            mCallback = new Vector<>();

        mCallback.add(callback);
    }

    public void removeBottomSheetCallback(@NonNull BottomSheetCallback callback) {
        mCallback.remove(callback);
    }


    public final void setState(@State int state) {
        if (state == mState) {
            return;
        }

        if (state == STATE_COLLAPSED || state == STATE_EXPANDED ||
                mHideable) {
            mState = state;
            mLastStableState = state;
        }

        V child = mViewRef == null ? null : mViewRef.get();
        if (child == null) {
            return;
        }
        int top;
        if (state == STATE_COLLAPSED) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else if (mHideable) {
            top = mParentHeight;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        setStateInternal(STATE_SETTLING);
        if (mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            ViewCompat.postOnAnimation(child, new SettleRunnable(child, state));
        }
    }


    @State
    public final int getState() {
        return mState;
    }

    private void setStateInternal(@State int state) {
        if (mState == state) {
            return;
        }
        if (mViewRef == null) {
            return;
        }

        mState = state;
        View bottomSheet = mViewRef.get();

        if (state == STATE_EXPANDED) {
            updateImportantForAccessibility(true);
        } else if (state == STATE_COLLAPSED) {
            updateImportantForAccessibility(false);
        }

        if (bottomSheet != null && mCallback != null) {

//            mCallback.onStateChanged(bottomSheet, state);
            notifyStateChangedToListeners(bottomSheet, state);
        }
        updateDrawableForTargetState(state);


    }

    private void notifyStateChangedToListeners(@NonNull View bottomSheet, @State int newState) {
        for (BottomSheetCallback bottomSheetCallback : mCallback) {
            bottomSheetCallback.onStateChanged(bottomSheet, newState);
        }
    }

    private void notifyOnSlideToListeners(@NonNull View bottomSheet, float slideOffset) {
        for (BottomSheetCallback bottomSheetCallback : mCallback) {
            bottomSheetCallback.onSlide(bottomSheet, slideOffset);
        }
    }

    private void reset() {
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
    }

    private boolean shouldHide(View child, float yvel) {
        if (child.getTop() < mMaxOffset) {
            // It should not hide, but collapse.
            return false;
        }
        final float newTop = child.getTop() + yvel * HIDE_FRICTION;
        return Math.abs(newTop - mMaxOffset) / (float) mPeekHeight > HIDE_THRESHOLD;
    }

    private View findScrollingChild(View view) {
        if (view instanceof NestedScrollingChild) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mState == STATE_DRAGGING) {
                return false;
            }
            if (mTouchingScrollingChild) {
                return false;
            }
            if (mState == STATE_EXPANDED && mActivePointerId == pointerId) {
                View scroll = mNestedScrollingChildRef.get();
                if (scroll != null && scroll.canScrollVertically(-1)) {
                    // Let the content scroll up
                    return false;
                }
            }
            return mViewRef != null && mViewRef.get() == child;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            dispatchOnSlide(top);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top;
            @State int targetState = 0;
            if (yvel < 0) { // Moving up
                top = mMinOffset;
                targetState = STATE_EXPANDED;
            } else if (mHideable && shouldHide(releasedChild, yvel)) {
                top = mParentHeight;

            } else if (yvel == 0.f) {
                int currentTop = releasedChild.getTop();
                if (Math.abs(currentTop - mMinOffset) < Math.abs(currentTop - mMaxOffset)) {
                    top = mMinOffset;
                    targetState = STATE_EXPANDED;
                } else {
                    top = mMaxOffset;
                    targetState = STATE_COLLAPSED;
                }
            } else {
                top = mMaxOffset;
                targetState = STATE_COLLAPSED;
            }

            // Restrict Collapsed view (optional)
            if (!mCollapsible && targetState == STATE_COLLAPSED) {
                top = mAnchorPoint;
            }

            if (mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                setStateInternal(STATE_SETTLING);
                ViewCompat.postOnAnimation(releasedChild,
                        new SettleRunnable(releasedChild, targetState));
            } else {
                setStateInternal(targetState);
            }
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return constrain(top, mMinOffset, mHideable ? mParentHeight : mMaxOffset);
        }

        int constrain(int amount, int low, int high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            return child.getLeft();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            if (mHideable) {
                return mParentHeight;
            } else {
                return mMaxOffset;
            }
        }
    };

    private void dispatchOnSlide(int top) {
        View bottomSheet = mViewRef.get();
        if (bottomSheet != null && mCallback != null) {
            if (top > mMaxOffset) {
                notifyOnSlideToListeners(bottomSheet, (float) (mMaxOffset - top) / mPeekHeight);
            } else {
                notifyOnSlideToListeners(bottomSheet, (float) (mMaxOffset - top) / ((mMaxOffset - mMinOffset)));
            }
        }


    }

    private class SettleRunnable implements Runnable {

        private final View mView;
        private boolean isPosted;
        @State
        private int mTargetState;

        SettleRunnable(View view, @State int targetState) {
            mView = view;
            mTargetState = targetState;
        }

        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                setStateInternal(mTargetState);
            }
            this.isPosted = false;

        }

    }

    protected static class SavedState extends AbsSavedState {
        @BottomSheetBehavior.State
        final int state;
        int peekHeight;
        boolean hideable;

        public SavedState(@NonNull Parcel source) {
            this(source, null);
        }

        public SavedState(@NonNull Parcel source, ClassLoader loader) {
            super(source, loader);
            //noinspection ResourceType
            state = source.readInt();
            peekHeight = source.readInt();
            hideable = source.readInt() == 1;
        }

        public SavedState(Parcelable superState, @NonNull BottomSheetBehavior<?> behavior) {
            super(superState);
            this.state = behavior.mState;
            this.peekHeight = behavior.mPeekHeight;
            this.hideable = behavior.mHideable;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
            out.writeInt(peekHeight);
            out.writeInt(hideable ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR =
                new ClassLoaderCreator<SavedState>() {
                    @NonNull
                    @Override
                    public SavedState createFromParcel(@NonNull Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Nullable
                    @Override
                    public SavedState createFromParcel(@NonNull Parcel in) {
                        return new SavedState(in, null);
                    }

                    @NonNull
                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @SuppressWarnings("unchecked")
    public static <V extends View> BottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof BottomSheetBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with BottomSheetBehavior");
        }
        return (BottomSheetBehavior<V>) behavior;
    }

    private int calculatePeekHeight() {
        if (peekHeightAuto) {
            int desiredHeight = max(peekHeightMin, mParentHeight - mParentWidth * 9 / 16);
            return min(desiredHeight, childHeight) + insetBottom;
        }
        // Only make sure the peek height is above the gesture insets if we're not applying system
        // insets.
        if (!gestureInsetBottomIgnored && !paddingBottomSystemWindowInsets && gestureInsetBottom > 0) {
            return max(mParentHeight, gestureInsetBottom + peekHeightGestureInsetBuffer);
        }
        return mPeekHeight + insetBottom;
    }

    private void calculateCollapsedOffset() {
        int peek = calculatePeekHeight();

        mMaxOffset = mParentHeight - peek;

    }


    private void settleToStatePendingLayout(@State int state) {
        final V child = mViewRef.get();
        if (child == null) {
            return;
        }
        // Start the animation; wait until a pending layout if there is one.
        ViewParent parent = child.getParent();
        if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
            final int finalState = state;
            child.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            settleToState(child, finalState);
                        }
                    });
        } else {
            settleToState(child, state);
        }
    }

    void settleToState(@NonNull View child, int state) {
        int top;
        if (state == STATE_COLLAPSED) {
            top = mMaxOffset;
        } else if (state == STATE_EXPANDED) {
            top = mMinOffset;
        } else {
            throw new IllegalArgumentException("Illegal state argument: " + state);
        }
        startSettlingAnimation(child, state, top, false);
    }

    void startSettlingAnimation(View child, int state, int top, boolean settleFromViewDragHelper) {
        boolean startedSettling =
                mViewDragHelper != null
                        && (settleFromViewDragHelper
                        ? mViewDragHelper.settleCapturedViewAt(child.getLeft(), top)
                        : mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top));
        if (startedSettling) {
            setStateInternal(STATE_SETTLING);
            // STATE_SETTLING won't animate the material shape, so do that here with the target state.
            updateDrawableForTargetState(state);
            if (settleRunnable == null) {
                // If the singleton SettleRunnable instance has not been instantiated, create it.
                settleRunnable = new SettleRunnable(child, state);
            }
            // If the SettleRunnable has not been posted, post it with the correct state.
            if (settleRunnable.isPosted == false) {
                settleRunnable.mTargetState = state;
                ViewCompat.postOnAnimation(child, settleRunnable);
                settleRunnable.isPosted = true;
            } else {
                // Otherwise, if it has been posted, just update the target state.
                settleRunnable.mTargetState = state;
            }
        } else {
            setStateInternal(state);
        }
    }

    private void updateDrawableForTargetState(@State int state) {
        if (state == STATE_SETTLING) {
            // Special case: we want to know which state we're settling to, so wait for another call.
            return;
        }

        boolean expand = state == STATE_EXPANDED;
        if (isShapeExpanded != expand) {
            isShapeExpanded = expand;
            if (materialShapeDrawable != null && interpolatorAnimator != null) {
                if (interpolatorAnimator.isRunning()) {
                    interpolatorAnimator.reverse();
                } else {
                    float to = expand ? 0f : 1f;
                    float from = 1f - to;
                    interpolatorAnimator.setFloatValues(from, to);
                    interpolatorAnimator.start();
                }
            }
        }
    }

    private void createMaterialShapeDrawable(
            @NonNull Context context, AttributeSet attrs, boolean hasBackgroundTint) {
        this.createMaterialShapeDrawable(context, attrs, hasBackgroundTint, null);
    }

    private void createMaterialShapeDrawable(
            @NonNull Context context,
            AttributeSet attrs,
            boolean hasBackgroundTint,
            @Nullable ColorStateList bottomSheetColor) {
        if (this.shapeThemingEnabled) {
            this.shapeAppearanceModelDefault =
                    ShapeAppearanceModel.builder(context, attrs, R.attr.bottomSheetStyle, DEF_STYLE_RES)
                            .build();

            this.materialShapeDrawable = new MaterialShapeDrawable(shapeAppearanceModelDefault);
            this.materialShapeDrawable.initializeElevationOverlay(context);

            if (hasBackgroundTint && bottomSheetColor != null) {
                materialShapeDrawable.setFillColor(bottomSheetColor);
            } else {
                // If the tint isn't set, use the theme default background color.
                TypedValue defaultColor = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.colorBackground, defaultColor, true);
                materialShapeDrawable.setTint(defaultColor.data);
            }
        }
    }

    private void createShapeValueAnimator() {
        interpolatorAnimator = ValueAnimator.ofFloat(0f, 1f);
        interpolatorAnimator.setDuration(CORNER_ANIMATION_DURATION);
        interpolatorAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        if (materialShapeDrawable != null) {
                            materialShapeDrawable.setInterpolation(value);
                        }
                    }
                });
    }

    public void setUpdateImportantForAccessibilityOnSiblings(
            boolean updateImportantForAccessibilityOnSiblings) {
        this.updateImportantForAccessibilityOnSiblings = updateImportantForAccessibilityOnSiblings;
    }

    private void updateImportantForAccessibility(boolean expanded) {
        if (mViewRef == null) {
            return;
        }

        ViewParent viewParent = mViewRef.get().getParent();
        if (!(viewParent instanceof CoordinatorLayout)) {
            return;
        }

        CoordinatorLayout parent = (CoordinatorLayout) viewParent;
        final int childCount = parent.getChildCount();
        if (expanded) {
            if (importantForAccessibilityMap == null) {
                importantForAccessibilityMap = new HashMap<>(childCount);
            } else {
                // The important for accessibility values of the child views have been saved already.
                return;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if (child == mViewRef.get()) {
                continue;
            }

            if (expanded) {
                // Saves the important for accessibility value of the child view.
                importantForAccessibilityMap.put(child, child.getImportantForAccessibility());
                if (updateImportantForAccessibilityOnSiblings) {
                    ViewCompat.setImportantForAccessibility(
                            child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                }
            } else {
                if (updateImportantForAccessibilityOnSiblings
                        && importantForAccessibilityMap != null
                        && importantForAccessibilityMap.containsKey(child)) {
                    // Restores the original important for accessibility value of the child view.
                    ViewCompat.setImportantForAccessibility(child, importantForAccessibilityMap.get(child));
                }
            }
        }

        if (!expanded) {
            importantForAccessibilityMap = null;
        } else if (updateImportantForAccessibilityOnSiblings) {
            // If the siblings of the bottom sheet have been set to not important for a11y, move the focus
            // to the bottom sheet when expanded.
            mViewRef.get().sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
    }

    private void updateAccessibilityActions() {
        if (mViewRef == null) {
            return;
        }
        V child = mViewRef.get();
        if (child == null) {
            return;
        }
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_COLLAPSE);
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_EXPAND);
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_DISMISS);


        switch (mState) {
            case STATE_EXPANDED: {
                int nextState = STATE_EXPANDED;
                replaceAccessibilityActionForState(
                        child, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE, nextState);
                break;
            }
            case STATE_COLLAPSED: {
                int nextState = STATE_EXPANDED;
                replaceAccessibilityActionForState(
                        child, AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND, nextState);
                break;
            }
            default: // fall out
        }
    }


    private void replaceAccessibilityActionForState(
            V child, AccessibilityNodeInfoCompat.AccessibilityActionCompat action, int state) {
        ViewCompat.replaceAccessibilityAction(
                child, action, null, createAccessibilityViewCommandForState(state));
    }

    private int addAccessibilityActionForState(V child, @StringRes int stringResId, int state) {
        return ViewCompat.addAccessibilityAction(
                child,
                child.getResources().getString(stringResId),
                createAccessibilityViewCommandForState(state));
    }


    private AccessibilityViewCommand createAccessibilityViewCommandForState(final int state) {
        return new AccessibilityViewCommand() {
            @Override
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                setState(state);
                return true;
            }
        };
    }

    private void setWindowInsetsListener(@NonNull View child) {
        // Ensure the peek height is at least as large as the bottom gesture inset size so that
        // the sheet can always be dragged, but only when the inset is required by the system.
        final boolean shouldHandleGestureInsets =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isGestureInsetBottomIgnored() && !peekHeightAuto;

        // If were not handling insets at all, don't apply the listener.
        if (!paddingBottomSystemWindowInsets && !paddingLeftSystemWindowInsets && !paddingRightSystemWindowInsets && !shouldHandleGestureInsets) {
            return;
        }
        doOnApplyWindowInsets(
                child,
                (view, insets, initialPadding) -> {
                    insetTop = insets.getSystemWindowInsetTop();

                    boolean isRtl = isLayoutRtl(view);

                    int bottomPadding = view.getPaddingBottom();
                    int leftPadding = view.getPaddingLeft();
                    int rightPadding = view.getPaddingRight();

                    if (paddingBottomSystemWindowInsets) {
                        insetBottom = insets.getSystemWindowInsetBottom();
                        bottomPadding = initialPadding.bottom + insetBottom;
                    }

                    if (paddingLeftSystemWindowInsets) {
                        leftPadding = isRtl ? initialPadding.end : initialPadding.start;
                        leftPadding += insets.getSystemWindowInsetLeft();
                    }

                    if (paddingRightSystemWindowInsets) {
                        rightPadding = isRtl ? initialPadding.start : initialPadding.end;
                        rightPadding += insets.getSystemWindowInsetRight();
                    }

                    view.setPadding(leftPadding, view.getPaddingTop(), rightPadding, bottomPadding);

                    if (shouldHandleGestureInsets) {
                        gestureInsetBottom = insets.getMandatorySystemGestureInsets().bottom;
                    }

                    // Don't update the peek height to be above the navigation bar or gestures if these
                    // flags are off. It means the client is already handling it.
                    if (paddingBottomSystemWindowInsets || shouldHandleGestureInsets) {
                        updatePeekHeight(/* animate= */ false);
                    }
                    return insets;
                });
    }


}
