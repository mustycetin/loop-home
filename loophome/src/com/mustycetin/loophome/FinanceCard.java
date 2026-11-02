package com.mustycetin.loophome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinanceCard extends View {
    private static final long FLIP_MS = 650;
    private static final long SHOW_MS = 5000;
    private static final Locale TR = new Locale("tr", "TR");
    private ValueAnimator anim;
    private final DecimalFormat big;
    private final Camera camera;
    private final Paint card;
    private final Paint change;
    private float flipAt;
    private int index;
    private final Matrix matrix;
    private final Paint name;
    private final Runnable next;
    private final DecimalFormat pct;
    private final Paint pill;
    private final Paint price;
    private final RectF rect;
    private final Paint rim;
    private List<Finance.Row> rows;
    private boolean running;
    private final DecimalFormat small;
    private final Paint time;
    private String updated;

    public FinanceCard(Context context) {
        super(context);
        this.card = new Paint(1);
        Paint paint = new Paint(1);
        this.rim = paint;
        Paint paint2 = new Paint(129);
        this.name = paint2;
        Paint paint3 = new Paint(129);
        this.price = paint3;
        Paint paint4 = new Paint(129);
        this.change = paint4;
        this.pill = new Paint(1);
        Paint paint5 = new Paint(129);
        this.time = paint5;
        this.rect = new RectF();
        this.camera = new Camera();
        this.matrix = new Matrix();
        this.rows = new ArrayList();
        this.updated = "";
        this.flipAt = -1.0f;
        this.next = new Runnable() { // from class: com.mustycetin.loophome.FinanceCard.1
            @Override // java.lang.Runnable
            public void run() {
                FinanceCard.this.flip();
                if (FinanceCard.this.running) {
                    FinanceCard.this.postDelayed(this, FinanceCard.SHOW_MS);
                }
            }
        };
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(TR);
        this.big = new DecimalFormat("#,##0", decimalFormatSymbols);
        this.small = new DecimalFormat("#,##0.00", decimalFormatSymbols);
        this.pct = new DecimalFormat("0.00", decimalFormatSymbols);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        paint.setColor(1090519039);
        paint2.setColor(-30116);
        paint2.setTextSize(19.0f);
        paint2.setLetterSpacing(0.3f);
        paint3.setColor(-1);
        paint3.setTextSize(42.0f);
        paint4.setTextSize(21.0f);
        paint4.setTextAlign(Paint.Align.CENTER);
        paint5.setColor(-2130706433);
        paint5.setTextSize(17.0f);
        paint5.setTextAlign(Paint.Align.RIGHT);
        setVisibility(8);
    }

    public void setTypefaces(Typeface typeface, Typeface typeface2) {
        this.name.setTypeface(typeface2);
        this.price.setTypeface(typeface);
        this.change.setTypeface(typeface2);
        this.time.setTypeface(typeface);
    }

    public void setRows(List<Finance.Row> list, String str) {
        if (list.isEmpty()) {
            return;
        }
        this.rows = list;
        this.updated = str;
        if (this.index >= list.size()) {
            this.index = 0;
        }
        setVisibility(0);
        invalidate();
    }

    public void start() {
        this.running = true;
        removeCallbacks(this.next);
        postDelayed(this.next, SHOW_MS);
    }

    public void stop() {
        this.running = false;
        removeCallbacks(this.next);
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.flipAt = -1.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flip() {
        if (this.rows.size() < 2) {
            return;
        }
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        final boolean[] zArr = {false};
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(FLIP_MS);
        this.anim.setInterpolator(new AccelerateDecelerateInterpolator());
        this.anim.addUpdateListener((valueAnimator2) -> lambda_flip_0(zArr, valueAnimator2));
        this.anim.addListener(new AnimatorListenerAdapter() { // from class: com.mustycetin.loophome.FinanceCard.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                FinanceCard.this.flipAt = -1.0f;
                FinanceCard.this.invalidate();
            }
        });
        this.anim.start();
    }
    /* synthetic */ void lambda_flip_0(boolean[] zArr, ValueAnimator valueAnimator) {
        float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.flipAt = fFloatValue;
        if (fFloatValue >= 0.5f && !zArr[0]) {
            zArr[0] = true;
            this.index = (this.index + 1) % this.rows.size();
        }
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int r3;
        String str;
        if (this.rows.isEmpty()) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        int r10 = canvas.save();
        float f = this.flipAt;
        if (f >= 0.0f) {
            if (f >= 0.5f) {
                f -= 1.0f;
            }
            this.camera.save();
            this.camera.setLocation(0.0f, 0.0f, -14.0f);
            this.camera.rotateX(f * 180.0f);
            this.camera.getMatrix(this.matrix);
            this.camera.restore();
            this.matrix.preTranslate((-width) / 2.0f, (-height) / 2.0f);
            this.matrix.postTranslate(width / 2.0f, height / 2.0f);
            canvas.concat(this.matrix);
        }
        this.rect.set(2.0f, 2.0f, width - 2.0f, height - 2.0f);
        this.card.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, height, 872415231, 352321535, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(this.rect, 26.0f, 26.0f, this.card);
        canvas.drawRoundRect(this.rect, 26.0f, 26.0f, this.rim);
        Finance.Row row = this.rows.get(this.index);
        canvas.drawText(row.name.toUpperCase(TR), 28.0f, 40.0f, this.name);
        if (!this.updated.isEmpty()) {
            canvas.drawText(this.updated, width - 28.0f, 38.0f, this.time);
        }
        canvas.drawText((row.price >= 1000.0d ? this.big : this.small).format(row.price) + " ₺", 28.0f, height - 26.0f, this.price);
        if (row.change > 0.0d) {
            r3 = -10754917;
        } else {
            r3 = row.change < 0.0d ? -38037 : -1275068417;
        }
        if (row.change > 0.0d) {
            str = "▲ %";
        } else {
            str = row.change < 0.0d ? "▼ %" : "• %";
        }
        String str2 = str + this.pct.format(Math.abs(row.change));
        float f2 = width - 28.0f;
        this.rect.set(f2 - (this.change.measureText(str2) + 30.0f), height - 66.0f, f2, height - 28.0f);
        this.pill.setColor((16777215 & r3) | 855638016);
        canvas.drawRoundRect(this.rect, 19.0f, 19.0f, this.pill);
        this.change.setColor(r3);
        canvas.drawText(str2, this.rect.centerX(), this.rect.centerY() - ((this.change.descent() + this.change.ascent()) / 2.0f), this.change);
        canvas.restoreToCount(r10);
    }
}
