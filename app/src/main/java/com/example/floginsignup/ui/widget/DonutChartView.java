package com.example.floginsignup.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class DonutChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float[] values = new float[]{14f, 4f, 2f};
    private int[] colors = new int[]{
            Color.parseColor("#EF4444"),
            Color.parseColor("#22C55E"),
            Color.parseColor("#F59E0B")
    };
    private float strokeWidthDp = 18f;

    public DonutChartView(Context context) { super(context); init(); }
    public DonutChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public DonutChartView(Context context, AttributeSet attrs, int s) { super(context, attrs, s); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    public void setData(float[] values, int[] colors) {
        this.values = values;
        this.colors = colors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float stroke = strokeWidthDp * density;
        paint.setStrokeWidth(stroke);

        float pad = stroke / 2f + 2f;
        int w = getWidth();
        int h = getHeight();
        float size = Math.min(w, h) - pad * 2f;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        rect.set(left, top, left + size, top + size);

        // add up everything so we know the total
        float total = 0;
        for (float v : values) total += v;
        if (total <= 0) return;

        // draw each slice starting from the top
        float startAngle = -90f;
        float gap = 2f;
        for (int i = 0; i < values.length; i++) {
            float sweep = (values[i] / total) * 360f - gap;
            if (sweep < 0) sweep = 0;
            paint.setColor(colors[i]);
            canvas.drawArc(rect, startAngle, sweep, false, paint);
            startAngle += (values[i] / total) * 360f;
        }
    }
}
