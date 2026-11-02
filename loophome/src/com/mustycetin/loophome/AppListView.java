package com.mustycetin.loophome;

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

/** All apps as one alphabetical list, in the same look as RecentsView. */
public class AppListView extends View {
    private static final float ROW_H = 92.0f;
    private static final float ROW_W = 860.0f;
    private static final float ICON = 60.0f;
    private static final int PAGE = 6;
    private ValueAnimator anim;
    private float appear;
    private final Paint art;
    private final Paint card;
    private final Paint dim;
    private final RectF dst;
    private final List<LoopGrid.Tile> items;
    private final Paint label;
    private Listener listener;
    private final RectF rect;
    private final Paint rim;
    private float scroll;
    private ValueAnimator scrollAnim;
    private int sel;
    private final Paint sub;
    private final Paint title;
    private int toggleKey;

    public interface Listener {
        void onOpen(LoopGrid.Tile tile);

        void onDismiss();
    }

    public AppListView(Context context) {
        super(context);
        this.items = new ArrayList<>();
        this.dim = new Paint();
        this.card = new Paint(1);
        this.rim = new Paint(1);
        this.art = new Paint(3);
        this.title = new Paint(129);
        this.label = new Paint(129);
        this.sub = new Paint(129);
        this.rect = new RectF();
        this.dst = new RectF();
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.dim.setColor(-653982456);
        this.rim.setStyle(Paint.Style.STROKE);
        this.title.setColor(-30116);
        this.title.setTextSize(26.0f);
        this.title.setLetterSpacing(0.35f);
        this.label.setColor(-1);
        this.label.setTextSize(34.0f);
        this.sub.setColor(-1711276033);
        this.sub.setTextSize(22.0f);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setTypefaces(Typeface regular, Typeface medium) {
        this.title.setTypeface(medium);
        this.label.setTypeface(medium);
        this.sub.setTypeface(regular);
    }

    /** The assigned list key also closes the list. */
    public void setToggleKey(int keyCode) {
        this.toggleKey = keyCode;
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    public void show(List<LoopGrid.Tile> tiles) {
        this.items.clear();
        this.items.addAll(tiles);
        this.sel = 0;
        this.scroll = 0.0f;
        setVisibility(VISIBLE);
        requestFocus();
        if (this.anim != null) {
            this.anim.cancel();
        }
        this.anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim.setDuration(220L);
        this.anim.addUpdateListener(a -> {
            this.appear = (Float) a.getAnimatedValue();
            invalidate();
        });
        this.anim.start();
    }

    public void hide() {
        setVisibility(GONE);
        if (this.listener != null) {
            this.listener.onDismiss();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 0 && keyCode == this.toggleKey) {
            hide();
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                hide();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                select(this.sel - 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                select(this.sel + 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_PAGE_UP:
                select(this.sel - PAGE);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                select(this.sel + PAGE);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.getRepeatCount() == 0 && this.sel < this.items.size()) {
                    LoopGrid.Tile tile = this.items.get(this.sel);
                    hide();
                    if (this.listener != null) {
                        this.listener.onOpen(tile);
                    }
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void select(int index) {
        if (this.items.isEmpty()) {
            return;
        }
        this.sel = Math.max(0, Math.min(this.items.size() - 1, index));
        // Keep the selection in the upper-middle; the list scrolls under it.
        float target = Math.max(0, this.sel - 3) * ROW_H;
        if (this.scrollAnim != null) {
            this.scrollAnim.cancel();
        }
        this.scrollAnim = ValueAnimator.ofFloat(this.scroll, target);
        this.scrollAnim.setDuration(160L);
        this.scrollAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        this.scrollAnim.addUpdateListener(a -> {
            this.scroll = (Float) a.getAnimatedValue();
            invalidate();
        });
        this.scrollAnim.start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        this.dim.setAlpha((int) (this.appear * 242.0f));
        canvas.drawRect(0.0f, 0.0f, width, height, this.dim);
        int alpha = (int) (this.appear * 255.0f);
        this.title.setAlpha(alpha);
        canvas.drawText("TÜM UYGULAMALAR  ·  " + this.items.size(), 110.0f, 150.0f, this.title);
        this.sub.setAlpha((int) (this.appear * 153.0f));
        canvas.drawText("OK: aç   ·   ◀ ▶: sayfa atla   ·   Geri: çık", 110.0f, height - 90.0f, this.sub);
        if (this.items.isEmpty()) {
            return;
        }
        float left = (width - ROW_W) / 2.0f;
        float top = 210.0f;
        float bottom = height - 140.0f;
        int save = canvas.save();
        canvas.clipRect(0.0f, top - 20.0f, width, bottom);
        for (int i = 0; i < this.items.size(); i++) {
            float y = (top + (i * ROW_H)) - this.scroll;
            if (y + ROW_H < top - 20.0f || y > bottom) {
                continue;
            }
            boolean focused = i == this.sel;
            float grow = focused ? 14.0f : 0.0f;
            this.rect.set(left - grow, (y + 6.0f) - (grow / 3.0f), left + ROW_W + grow, (y + ROW_H) - 6.0f + (grow / 3.0f));
            if (focused) {
                this.card.setShader(new LinearGradient(0.0f, this.rect.top, 0.0f, this.rect.bottom, 1392508927, 654311423, Shader.TileMode.CLAMP));
                this.card.setAlpha(alpha);
                canvas.drawRoundRect(this.rect, 26.0f, 26.0f, this.card);
                this.rim.setColor(-419430401);
                this.rim.setStrokeWidth(3.0f);
                this.rim.setAlpha((int) (230 * this.appear));
                canvas.drawRoundRect(this.rect, 26.0f, 26.0f, this.rim);
            }
            LoopGrid.Tile tile = this.items.get(i);
            float cy = this.rect.centerY();
            float textAlpha = focused ? 1.0f : 0.72f;
            if (tile.art != null) {
                float half = (focused ? ICON + 6.0f : ICON) / 2.0f;
                float cx = left + 30.0f + (ICON / 2.0f);
                this.dst.set(cx - half, cy - half, cx + half, cy + half);
                this.art.setAlpha((int) (alpha * textAlpha));
                canvas.drawBitmap(tile.art, (Rect) null, this.dst, this.art);
            }
            this.label.setAlpha((int) (alpha * textAlpha));
            canvas.drawText(fit(tile.label, this.label, ROW_W - ICON - 90.0f), left + ICON + 60.0f, cy + 12.0f, this.label);
        }
        canvas.restoreToCount(save);
    }

    private static String fit(String str, Paint paint, float max) {
        if (paint.measureText(str) <= max) {
            return str;
        }
        int length = str.length();
        while (length > 1 && paint.measureText(str, 0, length) + paint.measureText("…") > max) {
            length--;
        }
        return str.substring(0, length).trim() + "…";
    }
}
