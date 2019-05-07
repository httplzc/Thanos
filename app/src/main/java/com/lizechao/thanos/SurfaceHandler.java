package com.lizechao.thanos;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
 * Time: 18:22
 */
public abstract class SurfaceHandler {
    private final SurfaceManager surfaceManager;

    public SurfaceHandler(SurfaceManager surfaceManager) {
        this.surfaceManager = surfaceManager;
        surfaceManager.addHandler(this);
    }

    protected abstract void surfaceCreated();

    protected abstract void surfaceChanged(SurfaceHolder holder, int format, int width, int height);

    protected abstract void surfaceDestroyed();

    protected abstract void onDraw(Canvas canvas);

    protected abstract void onDrawAfter();

    public SurfaceView getSurfaceView() {
        return surfaceManager.getSurfaceView();
    }

    protected int getWidth() {
        return surfaceManager.getSurfaceView().getWidth();
    }

    protected int getHeight() {
        return surfaceManager.getSurfaceView().getHeight();
    }


}
