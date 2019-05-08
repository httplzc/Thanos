package com.lizechao.thanos;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import io.reactivex.android.schedulers.AndroidSchedulers;
import pers.lizechao.android_lib.utils.JavaUtils;

/**
 * Created with
 * ********************************************************************************
 * #         ___                     ________                ________             *
 * #       |\  \                   |\_____  \              |\   ____\             *
 * #       \ \  \                   \|___/  /|             \ \  \___|             *
 * #        \ \  \                      /  / /              \ \  \                *
 * #         \ \  \____                /  /_/__              \ \  \____           *
 * #          \ \_______\             |\________\             \ \_______\         *
 * #           \|_______|              \|_______|              \|_______|         *
 * #                                                                              *
 * ********************************************************************************
 * Date: 2019/5/6 0006
 * Time: 12:01
 */
public class CrashAnim extends SurfaceHandler {
    private boolean isPlayAnim = false;
    private Paint paint = new Paint();
    private Set<ViewFrame> viewFrameList = new HashSet<>();
    private float baseSpeedX;
    private float baseSpeedY;
    private final static int MaxTime = 2000;
    private final static int AlphaTime = 0;
    private onAnimListener onAnimListener;
    private Random random = new Random();
    private android.view.animation.Interpolator interpolator = new AccelerateDecelerateInterpolator();

    private boolean firstFrame = true;

    public void setOnAnimListener(onAnimListener onAnimListener) {
        this.onAnimListener = onAnimListener;
    }

    private class ViewFrame {
        private View view;
        private List<PointData> pointList;

        private boolean haveRemove = false;
        private long startTime = 0;
        private Bitmap bitmap;

        private final float MaxDelayProgress = 0.7f;

        private Rect rectBitmap = new Rect();
        private RectF drawArea = new RectF();

        private float viewX;
        private float viewY;


        public ViewFrame(View view, float x, float y) {
            this.view = view;
            bitmap = loadBitmapFromView(view);
            pointList = spitToPoint(bitmap, x, y, randomSymbol(random));
            paint.setStrokeWidth(view.getResources().getDimension(R.dimen.oneDP));
            viewX = x;
            viewY = y;
        }

        @Override
        public boolean equals(Object obj) {
            return view.equals(obj);
        }

        @Override
        public int hashCode() {
            return view.hashCode();
        }

        void draw(Canvas canvas) {
            if (haveRemove)
                return;
            if (startTime == 0)
                startTime = System.currentTimeMillis();
            int currentTime = (int) (System.currentTimeMillis() - startTime);
            if (currentTime > MaxTime) {
                haveRemove = true;
                return;
            }
            int alpha = -1;
            if (currentTime > AlphaTime) {
                float alphaRatio = (float) (MaxTime - currentTime) / (float) (MaxTime - AlphaTime); // 0-1
                alpha = (int) (255 * alphaRatio);
                alpha = alpha < 0 ? 0 : alpha;
            }
            float progress = (float) currentTime / MaxTime;
            int cancelDrawWidth = (int) (bitmap.getWidth() * (progress / MaxDelayProgress));
            rectBitmap.set(cancelDrawWidth, 0, bitmap.getWidth(), bitmap.getHeight());
            drawArea.set(viewX + cancelDrawWidth, viewY, viewX + bitmap.getWidth(), viewY + bitmap.getHeight());
            canvas.drawBitmap(bitmap, rectBitmap, drawArea, null);
            for (PointData pointData : pointList) {
                if (progress > MaxDelayProgress * pointData.widthPercent) {
                    if (alpha != -1)
                        pointData.setColorAlpha(alpha);
                    paint.setColor(pointData.color);
                    pointData.calcPosition(progress);
                    canvas.drawPoint(pointData.x, pointData.y, paint);
                }
            }
        }
    }


    private static int randomSymbol(Random random) {
        if (random.nextInt(2) == 0)
            return -1;
        else
            return 1;
    }


    private List<PointData> spitToPoint(Bitmap bitmap, float x, float y, int xSymbol) {
        List<PointData> pointDataList = new ArrayList<>();
        for (int i = 0; i < bitmap.getWidth(); i += 6) {
            for (int i1 = 0; i1 < bitmap.getHeight(); i1 += 6) {
                pointDataList.add(new PointData(x + i, y + i1, bitmap.getPixel(i, i1), xSymbol, (float) i / bitmap.getWidth()));
            }
        }

        return pointDataList;
    }

    private Bitmap loadBitmapFromView(View v) {
        Bitmap screenshot;
        screenshot = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(screenshot);
        c.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(c);
        return screenshot;
    }

    private class PointData {
        private float x;
        private float y;
        private int color;

        private float speedX;
        private float speedY;

        private float widthPercent;


        public PointData(float x, float y, int color, int xSymbol, float widthPercent) {
            this.x = x;
            this.y = y;
            this.color = color;
            speedX = JavaUtils.random(random, 0.2f, 1f) * 1;
            speedY = JavaUtils.random(random, 0.05f, 1f) * -1;
            this.widthPercent = widthPercent;
        }

        /**
         * @param progress 0-1
         */
        public void calcPosition(float progress) {
            float percentWidthSpeed = (1 - widthPercent) * 3f;
            x +=  speedX * interpolator.getInterpolation(progress) * SurfaceManager.getIntervalRefresh();
            y += percentWidthSpeed*baseSpeedY * speedY * interpolator.getInterpolation(progress) * SurfaceManager.getIntervalRefresh();
        }

        public void setColorAlpha(int alpha) {
            this.color = this.color & 0x00ffffff;
            this.color = this.color | (alpha << 24);
        }
    }


    public void addAnimView(View view, float x, float y) {
        viewFrameList.add(new ViewFrame(view, x, y));
    }

    public void addAnimViewRecursion(View view, float x, float y) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                addAnimViewRecursion(child, child.getX() + x, child.getY() + y);
            }
        } else {
            addAnimView(view, x, y);
        }

    }

    public void startAnim() {
        if (isPlayAnim)
            return;
        firstFrame = true;
        isPlayAnim = true;
    }

    public interface onAnimListener {
        void onEnd();

        void onStart();
    }

    public CrashAnim(SurfaceManager surfaceManager) {
        super(surfaceManager);
    }

    public boolean isPlayAnim() {
        return isPlayAnim;
    }

    @Override
    protected void surfaceCreated() {

    }

    @Override
    protected void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        baseSpeedX = width / (1080 / 3); //  px/ms
//        baseSpeedY = height / (1920 / 3); //  px/ms
        baseSpeedX = width / (1080 / 0.5f); //  px/ms
        baseSpeedY = width / (1080 / 0.25f); //  px/ms
    }

    @Override
    protected void surfaceDestroyed() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isPlayAnim)
            return;
        //绘图
        boolean haveFinishAll = true;
        for (ViewFrame viewFrame : viewFrameList) {
            if (!viewFrame.haveRemove) {
                viewFrame.draw(canvas);
                haveFinishAll = false;
            }

        }
        if (firstFrame) {
            firstFrame = false;
            AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    if (onAnimListener != null)
                        onAnimListener.onStart();
                }
            });
        }
        if (haveFinishAll) {
            AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    isPlayAnim = false;
                    viewFrameList.clear();
                    if (onAnimListener != null)
                        onAnimListener.onEnd();
                }
            });
        }
    }

    @Override
    protected void onDrawAfter() {

    }
}
