package com.mustycetin.loophome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;

public class RecentsView extends View {
    private static final float CARD_H = 360.0f;
    private static final float CARD_W = 300.0f;
    private static final float GAP = 44.0f;
    private ValueAnimator anim;
    private float appear;
    private final Paint art;
    private final Paint card;
    private boolean closable;
    private final Paint dim;
    private final RectF dst;
    private String heading;
    private String hint;
    private final List<Item> items;
    private final Paint label;
    private Listener listener;
    private final RectF rect;
    private float removeAt;
    private int removing;
    private final Paint rim;
    private float scroll;
    private ValueAnimator scrollAnim;
    private int sel;
    private final Paint sub;
    private final Paint title;

    public interface Listener {
        void onClose(LoopGrid.Tile tile);

        void onDismiss();

        void onOpen(LoopGrid.Tile tile);
    }

    public static final class Item {
        final long lastUsed;
        final String sub;
        final LoopGrid.Tile tile;

        public Item(LoopGrid.Tile tile, long j) {
            this(tile, j, null);
        }

        public Item(LoopGrid.Tile tile, long j, String str) {
            this.tile = tile;
            this.lastUsed = j;
            this.sub = str;
        }
    }

    public RecentsView(Context context) {
        super(context);
        this.items = new ArrayList();
        Paint paint = new Paint();
        this.dim = paint;
        this.card = new Paint(1);
        Paint paint2 = new Paint(1);
        this.rim = paint2;
        this.art = new Paint(3);
        Paint paint3 = new Paint(129);
        this.title = paint3;
        Paint paint4 = new Paint(129);
        this.label = paint4;
        Paint paint5 = new Paint(129);
        this.sub = paint5;
        this.rect = new RectF();
        this.dst = new RectF();
        this.heading = "AÇIK UYGULAMALAR";
        this.hint = "OK: aç   ·   ▼: kapat   ·   Geri: çık";
        this.closable = true;
        this.removing = -1;
        setFocusable(true);
        setFocusableInTouchMode(true);
        paint.setColor(-653982456);
        paint2.setStyle(Paint.Style.STROKE);
        paint3.setColor(-30116);
        paint3.setTextSize(26.0f);
        paint3.setLetterSpacing(0.35f);
        paint4.setColor(-1);
        paint4.setTextSize(30.0f);
        paint4.setTextAlign(Paint.Align.CENTER);
        paint5.setColor(-1711276033);
        paint5.setTextSize(22.0f);
        paint5.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setTypefaces(Typeface typeface, Typeface typeface2) {
        this.title.setTypeface(typeface2);
        this.label.setTypeface(typeface2);
        this.sub.setTypeface(typeface);
    }

    public void show(List<Item> list) {
        show("AÇIK UYGULAMALAR", "OK: aç   ·   ▼: kapat   ·   Geri: çık", true, list);
    }

    public void show(String str, String str2, boolean z, List<Item> list) {
        this.heading = str;
        this.hint = str2;
        this.closable = z;
        this.items.clear();
        this.items.addAll(list);
        this.sel = 0;
        this.scroll = 0.0f;
        this.removing = -1;
        setVisibility(0);
        requestFocus();
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(220L);
        this.anim.addUpdateListener((valueAnimator2) -> lambda_show_0(valueAnimator2));
        this.anim.start();
    }
    /* synthetic */ void lambda_show_0(ValueAnimator valueAnimator) {
        this.appear = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    public void hide() {
        setVisibility(8);
        Listener listener = this.listener;
        if (listener != null) {
            listener.onDismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int count() {
        if (this.items.isEmpty()) {
            return 0;
        }
        return this.items.size() + (this.closable ? 1 : 0);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int r4, KeyEvent keyEvent) {
        if (this.removing >= 0) {
            return true;
        }
        switch (r4) {
            case 4:
            case 82:
            case 192:
            case 193:
                hide();
                return true;
            case 19:
                return true;
            case 20:
                if (this.closable && this.sel < this.items.size() && !this.items.get(this.sel).tile.pkg.startsWith("@")) {
                    throwAway(this.sel);
                }
                return true;
            case 21:
                select(this.sel - 1);
                return true;
            case 22:
                select(this.sel + 1);
                return true;
            case 23:
            case 66:
            case 160:
                if (keyEvent.getRepeatCount() > 0) {
                    return true;
                }
                if (this.sel < this.items.size()) {
                    LoopGrid.Tile tile = this.items.get(this.sel).tile;
                    hide();
                    Listener listener = this.listener;
                    if (listener != null) {
                        listener.onOpen(tile);
                    }
                } else if (this.closable && !this.items.isEmpty()) {
                    for (Item item : this.items) {
                        if (this.listener != null && !item.tile.pkg.startsWith("@")) {
                            this.listener.onClose(item.tile);
                        }
                    }
                    this.items.clear();
                    hide();
                }
                return true;
            default:
                return super.onKeyDown(r4, keyEvent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void select(int r5) {
        if (count() == 0) {
            return;
        }
        int r6 = Math.max(0, Math.min(count() - 1, r5));
        this.sel = r6;
        float fMax = Math.max(0, r6 - 2) * 344.0f;
        ValueAnimator valueAnimator = this.scrollAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.scroll, fMax);
        this.scrollAnim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(200L);
        this.scrollAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        this.scrollAnim.addUpdateListener((valueAnimator2) -> lambda_select_1(valueAnimator2));
        this.scrollAnim.start();
        invalidate();
    }
    /* synthetic */ void lambda_select_1(ValueAnimator valueAnimator) {
        this.scroll = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    private void throwAway(int r3) {
        this.removing = r3;
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(280L);
        this.anim.setInterpolator(new DecelerateInterpolator(1.2f));
        this.anim.addUpdateListener((valueAnimator2) -> lambda_throwAway_2(valueAnimator2));
        this.anim.addListener(new AnimatorListenerAdapter() { // from class: com.mustycetin.loophome.RecentsView.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (RecentsView.this.removing < 0) {
                    return;
                }
                Item item = (Item) RecentsView.this.items.remove(RecentsView.this.removing);
                RecentsView.this.removing = -1;
                if (RecentsView.this.listener != null) {
                    RecentsView.this.listener.onClose(item.tile);
                }
                if (RecentsView.this.items.isEmpty()) {
                    RecentsView.this.hide();
                } else {
                    RecentsView recentsView = RecentsView.this;
                    recentsView.select(Math.min(recentsView.sel, RecentsView.this.count() - 1));
                }
            }
        });
        this.anim.start();
    }
    /* synthetic */ void lambda_throwAway_2(ValueAnimator valueAnimator) {
        this.removeAt = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f;
        float width = getWidth();
        float height = getHeight();
        this.dim.setAlpha((int) (this.appear * 242.0f));
        canvas.drawRect(0.0f, 0.0f, width, height, this.dim);
        int r1 = (int) (this.appear * 255.0f);
        this.title.setAlpha(r1);
        float f2 = 110.0f;
        float f3 = 150.0f;
        canvas.drawText(this.heading, 110.0f, 150.0f, this.title);
        float f4 = 153.0f;
        this.sub.setAlpha((int) (this.appear * 153.0f));
        this.sub.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(this.hint, 110.0f, height - 90.0f, this.sub);
        this.sub.setTextAlign(Paint.Align.CENTER);
        float f5 = 2.0f;
        if (this.items.isEmpty()) {
            this.label.setAlpha(r1);
            canvas.drawText("Açık uygulama yok", width / 2.0f, height / 2.0f, this.label);
            return;
        }
        float f6 = CARD_H;
        float f7 = (height - CARD_H) / 2.0f;
        int r11 = 0;
        while (r11 < count()) {
            float f8 = ((r11 * 344.0f) + f2) - this.scroll;
            if (f8 <= width && f8 + CARD_W >= 0.0f) {
                float f9 = this.appear;
                if (r11 != this.removing) {
                    f = f7;
                } else {
                    float f10 = this.removeAt;
                    f = f7 + (260.0f * f10);
                    f9 *= 1.0f - f10;
                }
                boolean z = r11 == this.sel;
                float f11 = z ? 1.08f : 1.0f;
                float f12 = f8 + f3;
                float f13 = f + 180.0f;
                RectF rectF = this.rect;
                float f14 = CARD_W * f11;
                float f15 = f14 / f5;
                float f16 = (f11 * f6) / f5;
                rectF.set(f12 - f15, f13 - f16, f12 + f15, f13 + f16);
                this.card.setShader(new LinearGradient(0.0f, this.rect.top, 0.0f, this.rect.bottom, z ? 1392508927 : 620756991, z ? 654311423 : 268435455, Shader.TileMode.CLAMP));
                int r3 = (int) (f9 * 255.0f);
                this.card.setAlpha(r3);
                canvas.drawRoundRect(this.rect, 34.0f, 34.0f, this.card);
                this.rim.setColor(z ? -419430401 : 1090519039);
                this.rim.setStrokeWidth(z ? 3.0f : 1.5f);
                this.rim.setAlpha((int) ((z ? 230 : 64) * f9));
                canvas.drawRoundRect(this.rect, 34.0f, 34.0f, this.rim);
                this.label.setAlpha(r3);
                this.sub.setAlpha((int) (f9 * f4));
                if (r11 >= this.items.size()) {
                    canvas.drawText("✕", f12, f13 - 10.0f, this.label);
                    canvas.drawText("Tümünü kapat", f12, f13 + 40.0f, this.label);
                } else {
                    Item item = this.items.get(r11);
                    if (item.tile.art != null) {
                        float f17 = f11 * 150.0f;
                        float f18 = f17 / 2.0f;
                        float f19 = 50.0f * f11;
                        this.dst.set(f12 - f18, this.rect.top + f19, f18 + f12, this.rect.top + f19 + f17);
                        this.art.setAlpha(r3);
                        canvas.drawBitmap(item.tile.art, (Rect) null, this.dst, this.art);
                    }
                    float f20 = f14 - 30.0f;
                    canvas.drawText(fit(item.tile.label, this.label, f20), f12, this.rect.top + (f11 * 255.0f), this.label);
                    canvas.drawText(fit(item.sub != null ? item.sub : ago(item.lastUsed), this.sub, f20), f12, this.rect.top + (f11 * 295.0f), this.sub);
                }
            }
            r11++;
            f6 = CARD_H;
            f5 = 2.0f;
            f2 = 110.0f;
            f3 = 150.0f;
            f4 = 153.0f;
        }
    }

    private static String fit(String str, Paint paint, float f) {
        if (paint.measureText(str) <= f) {
            return str;
        }
        int length = str.length();
        while (length > 1 && paint.measureText(str, 0, length) + paint.measureText("…") > f) {
            length--;
        }
        return str.substring(0, length).trim() + "…";
    }

    private static String ago(long j) {
        long jMax = Math.max(0L, (System.currentTimeMillis() - j) / 60000);
        if (jMax < 1) {
            return "az önce";
        }
        if (jMax < 60) {
            return jMax + " dk önce";
        }
        return (jMax / 60) + " sa önce";
    }
}
