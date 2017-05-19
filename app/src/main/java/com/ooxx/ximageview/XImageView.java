package com.ooxx.ximageview;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class XImageView extends View {

    private enum TouchState {
        None,
        Drag,
        Zoom
    }

    private List<Area> mRegions = new ArrayList<>();

    private static final float VELOCITY_MULTI = 0.3f;// 滑动速度加权，计算松手后移动距离
    private static final int VELOCITY_DURATION = 300;// 缓动持续时间

    private TouchState mTouchState = TouchState.None;

    private float mOffsetX;
    private float mOffsetY;

    private float mInitOffsetX;
    private float mInitOffserY;

    //private float mScaleX;
    //private float mScaleY;

    private float mBitmapDisplayWidth;
    private float mBitmapDisplayHeight;

    private float mInitBitmapDisplayWidth;
    private float mInitBitmapDisplayHeight;

    private float mScale;
    private float mInitScale;

    private float mCanvasWidth;
    private float mCanvasHeight;

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    private Paint mPaint;

    private Bitmap mBitmap;

    public static class Area {
        public Region mRegion;
        public onClickListner mOnClickListner;
    }

    public interface onClickListner{
        void onClick();
    }

    public XImageView(Context context) {
        super(context);
        init(context);
    }

    public XImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public XImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mScroller = new Scroller(context);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        initScale();
    }

    private void initScale() {
        if (mCanvasWidth == 0 || mCanvasHeight == 0) return;
        if (mBitmap == null) return;

        float bitmapWidth = mBitmap.getWidth();
        float bitmapHeight = mBitmap.getHeight();

        float scaleX = mCanvasWidth / bitmapWidth;
        float scaleY = mCanvasHeight / bitmapHeight;

        mInitScale = Math.min(scaleX, scaleY);
        mScale = mInitScale;

        mInitBitmapDisplayWidth = bitmapWidth * mScale;
        mInitBitmapDisplayHeight = bitmapHeight * mScale;

        mBitmapDisplayWidth = mInitBitmapDisplayWidth;
        mBitmapDisplayHeight = mInitBitmapDisplayHeight;

        initOffset();
    }

    private void initOffset() {
        mOffsetX = (mCanvasWidth - mBitmapDisplayWidth) / 2;
        mOffsetY = (mCanvasHeight - mBitmapDisplayHeight) / 2;

        mInitOffsetX = mOffsetX;
        mInitOffserY = mOffsetY;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private PointF mStartPoint = new PointF();
    private float mStartDistance;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!canRender()) return true;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mStartPoint.set(event.getX(), event.getY());
                mTouchState = TouchState.Drag;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mStartDistance = spacing(event);
                mTouchState = TouchState.Zoom;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchState == TouchState.Drag) {
                    drag(event);
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    mVelocityTracker.addMovement(event);

                } else if (mTouchState == TouchState.Zoom) {
                    zoom(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                click(event);

                mTouchState = TouchState.None;
                int dx = 0;
                int dy = 0;
                if (mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(100);
                    dx = (int) (mVelocityTracker.getXVelocity() * VELOCITY_MULTI);
                    dy = (int) (mVelocityTracker.getYVelocity() * VELOCITY_MULTI);
                }
                mScroller.startScroll((int) mStartPoint.x, (int) mStartPoint.y, dx, dy, VELOCITY_DURATION);
                invalidate();
                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchState = TouchState.None;
                break;
        }

        return true;
    }

    private void click (MotionEvent event){
        int x = (int)((event.getX()-mOffsetX)/mScale);
        int y = (int)((event.getY()-mOffsetY)/mScale);
        for (Area area : mRegions){
            if (area.mRegion.contains(x, y)){
                area.mOnClickListner.onClick();
            }
        }
    }

    private void zoom(MotionEvent motionEvent) {
        float newDistance = spacing(motionEvent);
        float scale = newDistance / mStartDistance;
        mStartDistance = newDistance;
        mScale *= scale;
        mBitmapDisplayWidth *= scale;
        mBitmapDisplayHeight *= scale;

        float fx = (motionEvent.getX(1) - motionEvent.getX(0)) / 2 + motionEvent.getX(0);
        float fy = (motionEvent.getY(1) - motionEvent.getY(0)) / 2 + motionEvent.getY(0);
        mOffsetX = (mOffsetX - fx) * scale + fx;
        mOffsetY = (mOffsetY - fy) * scale + fy;

        if ((mOffsetX > mInitOffsetX || mOffsetX + mBitmapDisplayWidth < mInitOffsetX + mInitBitmapDisplayWidth)
                ||
            (mOffsetY > mInitOffserY || mOffsetY + mBitmapDisplayHeight < mInitOffserY + mInitBitmapDisplayHeight)) {

            mOffsetX = mInitOffsetX;
            mScale = mInitScale;
            mBitmapDisplayWidth = mInitBitmapDisplayWidth;

            mOffsetY = mInitOffserY;
            mScale = mInitScale;
            mBitmapDisplayHeight = mInitBitmapDisplayHeight;

        }

        postInvalidate();
    }

    private void drag(MotionEvent motionEvent) {
        drag(motionEvent.getX(), motionEvent.getY());
    }

    private void drag (float curTouchX, float curTouchY){
        float dX = curTouchX - mStartPoint.x;
        float dY = curTouchY - mStartPoint.y;
        mStartPoint = new PointF(curTouchX, curTouchY);

        if (mBitmapDisplayWidth > mCanvasWidth) {
            Log.d("xxx", "mOffsetX + mBitmapDisplayWidth +dX=" + (mOffsetX + mBitmapDisplayWidth + dX));
            Log.d("xxx", "mCanvasWidth=" + mCanvasWidth);

            if (mOffsetX + dX <= 0 && mOffsetX + mBitmapDisplayWidth + dX >= mCanvasWidth) {
                mOffsetX += dX;
            }

        }
        if (mBitmapDisplayHeight > mCanvasHeight) {
            if (mOffsetY + dY <= 0 && mOffsetY + mBitmapDisplayHeight + dY >= mCanvasHeight) {
                mOffsetY += dY;
            }
        }

        postInvalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        doInitIfNeed(canvas);

        if (!canRender()) return;

        //Log.d("xxx","mScaleX="+mScaleX+", mScaleY="+mScaleY);
        Log.d("xxx", "mScale=" + mScale);

        canvas.drawColor(Color.BLACK);
        canvas.scale(mScale, mScale);
        canvas.drawBitmap(mBitmap, mOffsetX / mScale, mOffsetY / mScale, mPaint);
    }

    private void doInitIfNeed(Canvas canvas) {
        if (mScale == 0) {
            if (mBitmap != null) {
                mCanvasWidth = canvas.getWidth();
                mCanvasHeight = canvas.getHeight();
                initScale();
            }
        }
    }

    private boolean canRender() {
        if (mBitmap == null) return false;
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset() &&
               mTouchState == TouchState.None ){
            drag(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    public void addArea(Area area){
        mRegions.add(area);
    }
}
