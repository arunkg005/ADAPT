package com.example.adapt.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ProgressTrendView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = new int[]{68, 74, 79, 83, 87, 82, 90};

    public ProgressTrendView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(Color.rgb(0, 6, 20));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setColor(Color.argb(34, 252, 129, 37));
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(68, 71, 77));
        textPaint.setTextSize(34f);
        textPaint.setFakeBoldText(true);
    }

    public void setValues(int[] values) {
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        canvas.drawText("Task Completion Trend", left, top + 42, textPaint);
        if (values == null || values.length == 0) {
            return;
        }

        float chartTop = top + 74f;
        float chartBottom = top + height - 28f;
        float step = width / (float) (values.length - 1);
        Path line = new Path();
        Path fill = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = left + i * step;
            float y = chartBottom - (values[i] / 100f) * (chartBottom - chartTop);
            if (i == 0) {
                line.moveTo(x, y);
                fill.moveTo(x, chartBottom);
                fill.lineTo(x, y);
            } else {
                line.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(left + width, chartBottom);
        fill.close();
        canvas.drawPath(fill, fillPaint);
        canvas.drawPath(line, linePaint);

        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(28f);
        for (int i = 0; i < days.length; i++) {
            canvas.drawText(days[i], left + i * step - 8, chartBottom + 28, textPaint);
        }
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(34f);
    }
}
