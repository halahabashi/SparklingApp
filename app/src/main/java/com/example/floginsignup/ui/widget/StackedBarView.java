package com.example.floginsignup.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class StackedBarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();

    private float[] values = new float[]{14f, 2f, 4f};
    private int[] colors = new int[]{
            Color.parseColor("#EF4444"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#22C55E")
    };

    public StackedBarView(Context context) { super(context); }
    public StackedBarView(Context context, AttributeSet attrs) { super(context, attrs); }
    public StackedBarView(Context context, AttributeSet attrs, int s) { super(context, attrs, s); }

    public void setData(float[] values, int[] colors) {
        this.values = values;
        this.colors = colors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float radius = 6f * density;

        rect.set(0, 0, getWidth(), getHeight());
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        // sum all the parts
        float total = 0;
        for (float v : values) total += v;
        // nothing to draw
        if (total <= 0) {
            canvas.restore();
            return;
        }

        float x = 0;
        float gap = 2f * density;
        for (int i = 0; i < values.length; i++) {
            float w = (values[i] / total) * getWidth();
            paint.setColor(colors[i]);
            float right = x + w - (i < values.length - 1 ? gap : 0);
            canvas.drawRect(x, 0, right, getHeight(), paint);
            x += w;
        }
        canvas.restore();
    }
}
