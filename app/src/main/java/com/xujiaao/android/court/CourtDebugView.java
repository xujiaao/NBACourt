package com.xujiaao.android.court;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class CourtDebugView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private CourtView mCourt;

    public CourtDebugView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCourt(CourtView court) {
        mCourt = court;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final CourtView court = mCourt;
        if (court != null) {
            final CourtView.CameraState state = court.getCameraState();
            if (state.ready) {
                drawCameraState(canvas, mPaint, court, state);
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void drawCameraState(Canvas canvas, Paint paint, CourtView court, CourtView.CameraState state) {
        final int saveCount = canvas.save();

        final float pl = getPaddingLeft();
        final float pt = getPaddingTop();
        final float hvw = (getWidth() - pl - getPaddingRight()) * .5F;
        final float hvh = (getHeight() - pt - getPaddingBottom()) * .5F;
        final float hch = (court.getHeight() - court.getPaddingTop() - court.getPaddingBottom()) * .5F;

        final float cx = state.cameraZ;
        final float cy = -state.cameraY;
        final float sx = state.courtStartZ;
        final float sy = -state.courtStartY;
        final float ex = state.courtEndZ;
        final float ey = -state.courtEndY;

        final float density = getResources().getDisplayMetrics().density;
        final float radius = 4F * density;
        final float textSize = 24F * density;

        paint.setStrokeWidth(density);

        canvas.translate(pl + hvw, pt + hvh);
        canvas.scale(.5F, .5F);

        paint.setColor(0xFFAAAAAA);
        canvas.drawLine(cx, -hvw, cx, hvw, paint);
        canvas.drawLine(0F, -hvw, 0F, hvw, paint);
        canvas.drawLine(sx, -hvw, sx, hvw, paint);
        canvas.drawCircle(0F, 0F, radius, paint);
        canvas.drawCircle(0F, 0F, radius, paint);
        canvas.drawCircle(0F, -hch, radius, paint);
        canvas.drawCircle(0F, hch, radius, paint);

        paint.setColor(0xFF00FF00);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(textSize);
        canvas.drawLine(cx, cy, sx, sy, paint);
        canvas.drawLine(cx, cy, ex, ey, paint);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawText("CAMERA  ", cx, cy, paint);

        paint.setColor(0xFFFF0000);
        canvas.drawLine(sx, sy, ex, ey, paint);
        canvas.drawCircle(sx, sy, radius, paint);
        canvas.drawCircle(ex, ey, radius, paint);

        canvas.restoreToCount(saveCount);
    }
}