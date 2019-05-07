package com.lizechao.thanos;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 * Date: 2019/1/14 0014
 * Time: 18:20
 */
public class SurfaceManager {
    private SurfaceView surfaceView;
    private volatile boolean draw;
    @Nullable
    private Paint backGroundPaint;
    private DrawThread drawThread;

    private static final int fps = 60;
    private static final int intervalRefresh = 1000 / fps;


    private List<SurfaceHandler> surfaceHandlerList = new ArrayList<>();

    private final Object wait = new Object();
    private volatile boolean isWaiting = false;

    void addHandler(SurfaceHandler surfaceHandler) {
        surfaceHandlerList.add(surfaceHandler);
    }

    public static int getIntervalRefresh() {
        return intervalRefresh;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setBackGroundPaint(@Nullable Paint backGroundPaint) {
        this.backGroundPaint = backGroundPaint;
    }

    public SurfaceManager(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                draw = true;
                drawThread = new DrawThread();
                drawThread.start();
                for (SurfaceHandler surfaceHandler : surfaceHandlerList) {
                    surfaceHandler.surfaceCreated();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                for (SurfaceHandler surfaceHandler : surfaceHandlerList) {
                    surfaceHandler.surfaceChanged(holder, format, width, height);
                }

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                draw = false;
                for (SurfaceHandler surfaceHandler : surfaceHandlerList) {
                    surfaceHandler.surfaceDestroyed();
                }
            }
        });
    }

    public void stopDraw() {
        draw = false;
    }

    public void waitFor() {
        isWaiting = true;
    }

    public void awake() {
        synchronized (wait) {
            isWaiting = false;
            wait.notifyAll();
        }
    }

    public void runInDrawThread(Runnable runnable) {
        if (drawThread != null)
            drawThread.addRunnable(runnable);
    }

    public class DrawThread extends Thread {
        private Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

        @Override
        public void run() {
            while (draw) {
                checkWait();
                drawCanvas();
                for (SurfaceHandler surfaceHandler : surfaceHandlerList) {
                    surfaceHandler.onDrawAfter();
                }
                while (!queue.isEmpty()) {
                    Runnable runnable = queue.poll();
                    if (runnable != null)
                        runnable.run();
                }
            }

        }

        public void addRunnable(Runnable runnable) {
            queue.add(runnable);
        }

        private void checkWait() {
            synchronized (wait) {
                if (isWaiting) {
                    try {
                        wait.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void drawCanvas() {
            long startTime = System.currentTimeMillis();
            Canvas mCanvas = null;
            try {
                //获得canvas对象
                mCanvas = surfaceView.getHolder().lockCanvas();
                if (mCanvas != null) {
                    //绘制背景
                    if (backGroundPaint != null)
                        mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), backGroundPaint);
                    else
                        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    for (SurfaceHandler surfaceHandler : surfaceHandlerList) {
                        surfaceHandler.onDraw(mCanvas);
                    }
                    if (draw && System.currentTimeMillis() - startTime < intervalRefresh && System.currentTimeMillis() - startTime > 0) {
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCanvas != null) {
                    //释放canvas对象并提交画布
                    try {
                        surfaceView.getHolder().unlockCanvasAndPost(mCanvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}
