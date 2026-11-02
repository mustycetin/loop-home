package com.mustycetin.loophome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class LoopGrid extends View {
    private static final long FLIP_EVERY_MS = 30000;
    private static final long FLIP_MS = 900;
    private static final int GLASS_H = 72;
    private static final int GLASS_W = 128;
    private static final long IDLE_MS = 6000;
    private static final int[] SECRET = {19, 19, 20, 20};
    private static final long SECRET_WINDOW_MS = 2500;
    private static final long SHINE_DELAY_MS = 1700;
    private static final long SHINE_MS = 950;
    private static final long SLIDE_MS = 340;
    // Grid size as a share of screen height; a bit smaller in peek mode so the peek row fits.
    private static final float GRID_H = 0.3f;
    private static final float GRID_H_PEEK = 0.285f;
    // Tiles two steps away sit at 1 + (2-1) * PEEK_PULL steps, so they peek in from the edges.
    private static final float PEEK_PULL = 0.6f;
    private static final float PEEK_ALPHA = 0.34f;
    private static final float PEEK_SCALE = 0.58f;
    private static final float POP = 0.07f;
    private static final float PARALLAX_X = 18.0f;
    private static final float PARALLAX_Y = 12.0f;
    private static final int TICKER_H = 78;
    private View parallax;
    private float pop;
    // Edge peeks, icon pop and parallax; off by default (Loop Home ayarları).
    private boolean peek;
    private ValueAnimator anim;
    private final Paint art;
    private BitmapShader backdrop;
    private ValueAnimator backdropAnim;
    private float backdropMix;
    private BitmapShader backdropOld;
    private final Camera camera;
    private boolean centerLong;
    private final Runnable collapse;
    private int cols;
    private final RectF dst;
    private final Runnable flip;
    private ValueAnimator flipAnim;
    private float flipAt;
    private int flipAxis;
    private final Matrix flipMatrix;
    private int flipSign;
    private int focus;
    private final Paint glass;
    private final Paint label;
    private float lensRadius;
    private final RectF lensRect;
    private Listener listener;
    private final Matrix m;
    private float offX;
    private float offY;
    private boolean placed;
    private final int[] recent;
    private final long[] recentAt;
    private final RectF rect;
    private final Paint rim;
    private final LinearGradient rimShader;
    private final Random rnd;
    private final Paint shadow;
    private final Paint sheen;
    private final LinearGradient sheenShader;
    private final Paint shine;
    private ValueAnimator shineAnim;
    private float shineAt;
    private final LinearGradient shineShader;
    private float spread;
    private ValueAnimator spreadAnim;
    private final Runnable startShine;
    private final List<Tile> tiles;
    private final Paint tint;

    public interface Listener {
        void onInfo(Tile tile);

        void onLaunch(Tile tile);

        void onRecents();

        void onSecret();
    }

    public static class Tile {
        public Bitmap art;
        public Intent intent;
        public String label;
        public String pkg;
    }
    /* synthetic */ void lambda_new_0() {
        animateSpread(0.0f, 700L);
        postDelayed(this.flip, FLIP_EVERY_MS);
    }
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (LoopGrid.this.tiles.size() < 2 || LoopGrid.this.spread > 0.01f) {
                return;
            }
            LoopGrid loopGrid = LoopGrid.this;
            loopGrid.flipAxis = loopGrid.rnd.nextInt(2);
            LoopGrid loopGrid2 = LoopGrid.this;
            loopGrid2.flipSign = loopGrid2.rnd.nextBoolean() ? 1 : -1;
            final int[] r0 = {Math.floorMod(LoopGrid.this.focus + 1 + LoopGrid.this.rnd.nextInt(LoopGrid.this.tiles.size() - 1), LoopGrid.this.tiles.size())};
            if (LoopGrid.this.flipAnim != null) {
                LoopGrid.this.flipAnim.cancel();
            }
            LoopGrid.this.flipAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
            LoopGrid.this.flipAnim.setDuration(LoopGrid.FLIP_MS);
            LoopGrid.this.flipAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            LoopGrid.this.flipAnim.addUpdateListener((valueAnimator) -> lambda_run_0(r0, valueAnimator));
            LoopGrid.this.flipAnim.addListener(new AnimatorListenerAdapter() { // from class: com.mustycetin.loophome.LoopGrid.1.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    LoopGrid.this.flipAt = -1.0f;
                    LoopGrid.this.invalidate();
                }
            });
            LoopGrid.this.flipAnim.start();
            LoopGrid.this.postDelayed(this, LoopGrid.FLIP_EVERY_MS);
        }
        /* synthetic */ void lambda_run_0(int[] r4, ValueAnimator valueAnimator) {
            int r1;
            float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            if (fFloatValue >= 0.5f && (r1 = r4[0]) >= 0) {
                LoopGrid.this.focus = r1;
                r4[0] = -1;
            }
            LoopGrid.this.flipAt = fFloatValue;
            LoopGrid.this.invalidate();
        }
    }
    /* synthetic */ void lambda_new_2() {
        ValueAnimator valueAnimator = this.shineAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.shineAnim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(SHINE_MS);
        this.shineAnim.setInterpolator(new DecelerateInterpolator(1.2f));
        this.shineAnim.addUpdateListener((valueAnimator2) -> lambda_new_1(valueAnimator2));
        this.shineAnim.addListener(new AnimatorListenerAdapter() { // from class: com.mustycetin.loophome.LoopGrid.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                LoopGrid.this.shineAt = -1.0f;
                LoopGrid.this.invalidate();
            }
        });
        this.shineAnim.start();
    }
    /* synthetic */ void lambda_new_1(ValueAnimator valueAnimator) {
        this.shineAt = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    public LoopGrid(Context context) {
        super(context);
        this.tiles = new ArrayList();
        this.glass = new Paint(3);
        this.tint = new Paint(1);
        Paint paint = new Paint(1);
        this.sheen = paint;
        Paint paint2 = new Paint(1);
        this.rim = paint2;
        this.shadow = new Paint(1);
        this.art = new Paint(3);
        Paint paint3 = new Paint(129);
        this.label = paint3;
        LinearGradient linearGradient = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, new int[]{1509949439, 352321535, 16777215}, new float[]{0.0f, 0.45f, 1.0f}, Shader.TileMode.CLAMP);
        this.sheenShader = linearGradient;
        LinearGradient linearGradient2 = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, new int[]{-419430401, 1090519039, -2130706433}, new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
        this.rimShader = linearGradient2;
        Paint paint4 = new Paint(1);
        this.shine = paint4;
        LinearGradient linearGradient3 = new LinearGradient(0.0f, 0.0f, 1.0f, 1.0f, new int[]{16777215, 16777215, 1946157055, 16777215, 16777215}, new float[]{0.0f, 0.38f, 0.5f, 0.62f, 1.0f}, Shader.TileMode.CLAMP);
        this.shineShader = linearGradient3;
        this.m = new Matrix();
        this.lensRect = new RectF();
        this.shineAt = -1.0f;
        this.rect = new RectF();
        this.dst = new RectF();
        this.backdropMix = 1.0f;
        this.cols = 3;
        this.spread = 1.0f;
        int[] r2 = SECRET;
        this.recent = new int[r2.length];
        this.recentAt = new long[r2.length];
        this.collapse = () -> lambda_new_0();
        this.camera = new Camera();
        this.flipMatrix = new Matrix();
        this.rnd = new Random();
        this.flipAt = -1.0f;
        this.flipSign = 1;
        this.flip = new AnonymousClass1();
        this.startShine = () -> lambda_new_2();
        setFocusable(true);
        setFocusableInTouchMode(true);
        paint.setShader(linearGradient);
        paint4.setShader(linearGradient3);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setShader(linearGradient2);
        paint3.setTextAlign(Paint.Align.CENTER);
        paint3.setShadowLayer(6.0f, 0.0f, 1.0f, -1728053248);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** The background moves a little against each step, for depth. */
    public void setParallaxTarget(View view) {
        this.parallax = view;
        applyParallaxScale();
    }

    public void setPeek(boolean on) {
        this.peek = on;
        this.pop = 0.0f;
        applyParallaxScale();
        invalidate();
    }

    private void applyParallaxScale() {
        if (this.parallax == null) {
            return;
        }
        float scale = this.peek ? 1.04f : 1.0f;
        this.parallax.setScaleX(scale);
        this.parallax.setScaleY(scale);
        this.parallax.setTranslationX(0.0f);
        this.parallax.setTranslationY(0.0f);
    }

    public void setLabelTypeface(Typeface typeface) {
        this.label.setTypeface(typeface);
        invalidate();
    }

    public void wake() {
        stopFlip();
        if (this.spread < 1.0f) {
            animateSpread(1.0f, 220L);
        }
        removeCallbacks(this.collapse);
        postDelayed(this.collapse, IDLE_MS);
    }

    private void stopFlip() {
        removeCallbacks(this.flip);
        ValueAnimator valueAnimator = this.flipAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.flipAt = -1.0f;
    }

    public void sleepNow() {
        stopFlip();
        removeCallbacks(this.collapse);
        removeCallbacks(this.startShine);
        ValueAnimator valueAnimator = this.shineAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.shineAt = -1.0f;
    }

    public void setTiles(List<Tile> list, String str) {
        if (this.placed && !this.tiles.isEmpty()) {
            str = current().pkg;
        }
        this.tiles.clear();
        this.tiles.addAll(list);
        this.cols = bestCols(this.tiles.size());
        this.focus = 0;
        for (int r2 = 0; r2 < this.tiles.size(); r2++) {
            if (this.tiles.get(r2).pkg.equals(str)) {
                this.focus = r2;
                break;
            }
        }
        this.placed = true;
        invalidate();
    }

    static int bestCols(int r16) {
        if (r16 <= 3) {
            return 3;
        }
        double dSqrt = Math.sqrt(r16);
        double d = Double.MAX_VALUE;
        int r6 = 3;
        int r7 = 3;
        for (int r1 = 3; r6 <= Math.max(r1, r16); r1 = 3) {
            HashSet hashSet = new HashSet();
            int r10 = 0;
            int r12 = 0;
            for (int r11 = -1; r11 <= 1; r11++) {
                for (int r14 = -1; r14 <= 1; r14++) {
                    if (!hashSet.add(Integer.valueOf(Math.floorMod((r11 * r6) + r14, r16)))) {
                        r12++;
                    }
                }
            }
            HashSet hashSet2 = new HashSet(hashSet);
            for (int r13 = -2; r13 <= 2; r13++) {
                for (int r15 = -2; r15 <= 2; r15++) {
                    if (Math.max(Math.abs(r13), Math.abs(r15)) == 2 && Math.min(Math.abs(r13), Math.abs(r15)) <= 1 && !hashSet2.add(Integer.valueOf(Math.floorMod((r13 * r6) + r15, r16)))) {
                        r10++;
                    }
                }
            }
            double dAbs = ((double) ((r12 * 1000) + (r10 * 10))) + Math.abs(((double) r6) - dSqrt);
            if (dAbs < d) {
                r7 = r6;
                d = dAbs;
            }
            r6++;
        }
        return r7;
    }

    private Tile at(int r4, int r5) {
        List<Tile> list = this.tiles;
        return list.get(Math.floorMod(this.focus + (r4 * this.cols) + r5, list.size()));
    }

    private Tile current() {
        return this.tiles.get(this.focus);
    }
    public void setBackdrop(final Bitmap bitmap) {
        Rect rect;
        if (bitmap == null) {
            return;
        }
        if (getWidth() == 0) {
            post(() -> setBackdrop(bitmap));
            return;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(GLASS_W, GLASS_H, Bitmap.Config.ARGB_8888);
        float width = getWidth() / getHeight();
        int width2 = bitmap.getWidth();
        int height = bitmap.getHeight();
        float f = width2;
        float f2 = height;
        if (f / f2 > width) {
            float f3 = f2 * width;
            rect = new Rect((int) ((f - f3) / 2.0f), 0, (int) ((f + f3) / 2.0f), height);
        } else {
            float f4 = f / width;
            rect = new Rect(0, (int) ((f2 - f4) / 2.0f), width2, (int) ((f2 + f4) / 2.0f));
        }
        new Canvas(bitmapCreateBitmap).drawBitmap(bitmap, rect, new Rect(0, 0, GLASS_W, GLASS_H), new Paint(2));
        int[] r4 = new int[9216];
        bitmapCreateBitmap.getPixels(r4, 0, GLASS_W, 0, 0, GLASS_W, GLASS_H);
        for (int r14 = 0; r14 < 2; r14++) {
            r4 = boxBlur(boxBlur(r4, GLASS_W, GLASS_H, 3, true), GLASS_W, GLASS_H, 3, false);
        }
        for (int r11 = 0; r11 < r4.length; r11++) {
            r4[r11] = (dim(r4[r11] >> 16) << 16) | (-16777216) | (dim(r4[r11] >> 8) << 8) | dim(r4[r11]);
        }
        bitmapCreateBitmap.setPixels(r4, 0, GLASS_W, 0, 0, GLASS_W, GLASS_H);
        BitmapShader bitmapShader = new BitmapShader(bitmapCreateBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setScale(getWidth() / 128.0f, getHeight() / 72.0f);
        bitmapShader.setLocalMatrix(matrix);
        ValueAnimator valueAnimator = this.backdropAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        BitmapShader bitmapShader2 = this.backdrop;
        this.backdropOld = bitmapShader2;
        this.backdrop = bitmapShader;
        this.backdropMix = bitmapShader2 == null ? 1.0f : 0.0f;
        if (bitmapShader2 != null) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.backdropAnim = valueAnimatorOfFloat;
            valueAnimatorOfFloat.setDuration(1400L);
            this.backdropAnim.addUpdateListener((valueAnimator2) -> lambda_setBackdrop_4(valueAnimator2));
            this.backdropAnim.start();
        }
        invalidate();
    }
    /* synthetic */ void lambda_setBackdrop_4(ValueAnimator valueAnimator) {
        this.backdropMix = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    private static int dim(int r1) {
        return (int) ((r1 & 255) * 0.62f);
    }

    private static int[] boxBlur(int[] r16, int r17, int r18, int r19, boolean z) {
        int[] r4 = new int[r16.length];
        for (int r6 = 0; r6 < r18; r6++) {
            for (int r7 = 0; r7 < r17; r7++) {
                int r9 = 0;
                int r10 = 0;
                int r11 = 0;
                int r12 = 0;
                for (int r8 = -r19; r8 <= r19; r8++) {
                    int r13 = r16[((z ? r6 : Math.min(r18 - 1, Math.max(0, r6 + r8))) * r17) + (z ? Math.min(r17 - 1, Math.max(0, r7 + r8)) : r7)];
                    r9 += (r13 >> 16) & 255;
                    r11 += (r13 >> 8) & 255;
                    r12 += r13 & 255;
                    r10++;
                }
                r4[(r6 * r17) + r7] = ((r9 / r10) << 16) | (-16777216) | ((r11 / r10) << 8) | (r12 / r10);
            }
        }
        return r4;
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int r4, KeyEvent keyEvent) {
        Listener listener;
        Listener listener2;
        if (this.tiles.isEmpty()) {
            return super.onKeyDown(r4, keyEvent);
        }
        if (keyEvent.getRepeatCount() == 0) {
            remember(r4);
        }
        switch (r4) {
            case 19:
                return step(-1, 0);
            case 20:
                return step(1, 0);
            case 21:
                return step(0, -1);
            case 22:
                return step(0, 1);
            case 23:
            case 66:
            case 160:
                if (keyEvent.getRepeatCount() == 0) {
                    this.centerLong = false;
                    keyEvent.startTracking();
                }
                return true;
            case 82:
            case 192:
            case 193:
                wake();
                if (keyEvent.getRepeatCount() == 0 && (listener = this.listener) != null) {
                    listener.onRecents();
                }
                return true;
            case 165:
                wake();
                if (keyEvent.getRepeatCount() == 0 && (listener2 = this.listener) != null) {
                    listener2.onInfo(current());
                }
                return true;
            default:
                return super.onKeyDown(r4, keyEvent);
        }
    }

    private static boolean isCenter(int r1) {
        return r1 == 23 || r1 == 66 || r1 == 160;
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyLongPress(int r2, KeyEvent keyEvent) {
        if (isCenter(r2) && !this.tiles.isEmpty()) {
            this.centerLong = true;
            wake();
            Listener listener = this.listener;
            if (listener != null) {
                listener.onInfo(current());
            }
            return true;
        }
        return super.onKeyLongPress(r2, keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int r2, KeyEvent keyEvent) {
        if (isCenter(r2) && !this.tiles.isEmpty()) {
            if (!this.centerLong && keyEvent.isTracking() && !keyEvent.isCanceled()) {
                wake();
                Listener listener = this.listener;
                if (listener != null) {
                    listener.onLaunch(current());
                }
            }
            this.centerLong = false;
            return true;
        }
        return super.onKeyUp(r2, keyEvent);
    }

    private boolean step(int r1, int r2) {
        wake();
        move(r1, r2);
        return true;
    }

    private void remember(int r7) {
        int[] r0 = this.recent;
        System.arraycopy(r0, 1, r0, 0, r0.length - 1);
        long[] jArr = this.recentAt;
        System.arraycopy(jArr, 1, jArr, 0, jArr.length - 1);
        int[] r1 = this.recent;
        r1[r1.length - 1] = r7;
        long[] jArr2 = this.recentAt;
        jArr2[jArr2.length - 1] = SystemClock.uptimeMillis();
        int r8 = 0;
        while (true) {
            int[] r2 = SECRET;
            if (r8 >= r2.length) {
                long[] jArr3 = this.recentAt;
                if (jArr3[jArr3.length - 1] - jArr3[0] > SECRET_WINDOW_MS) {
                    return;
                }
                Arrays.fill(this.recent, 0);
                if (this.listener != null) {
                    post(() -> lambda_remember_5());
                    return;
                }
                return;
            }
            if (this.recent[r8] != r2[r8]) {
                return;
            } else {
                r8++;
            }
        }
    }
    /* synthetic */ void lambda_remember_5() {
        this.listener.onSecret();
    }

    private void move(int r5, int r6) {
        removeCallbacks(this.startShine);
        ValueAnimator valueAnimator = this.shineAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        this.shineAt = -1.0f;
        postDelayed(this.startShine, SHINE_DELAY_MS);
        ValueAnimator valueAnimator2 = this.anim;
        boolean z = valueAnimator2 != null && valueAnimator2.isRunning();
        ValueAnimator valueAnimator3 = this.anim;
        if (valueAnimator3 != null) {
            valueAnimator3.cancel();
        }
        this.focus = Math.floorMod(this.focus + (this.cols * r5) + r6, this.tiles.size());
        final float fClamp = clamp(this.offX, -0.6f, 0.6f) + r6;
        final float fClamp2 = clamp(this.offY, -0.6f, 0.6f) + r5;
        this.offX = fClamp;
        this.offY = fClamp2;
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(SLIDE_MS);
        this.anim.setInterpolator(z ? new DecelerateInterpolator(1.4f) : new OvershootInterpolator(1.2f));
        this.anim.addUpdateListener((valueAnimator4) -> lambda_move_6(fClamp, fClamp2, valueAnimator4));
        this.anim.start();
    }
    /* synthetic */ void lambda_move_6(float f, float f2, ValueAnimator valueAnimator) {
        float fFloatValue = 1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.offX = f * fFloatValue;
        this.offY = f2 * fFloatValue;
        if (this.peek) {
            this.pop = (float) Math.sin(Math.PI * valueAnimator.getAnimatedFraction()) * POP;
        }
        if (this.peek && this.parallax != null) {
            this.parallax.setTranslationX(-this.offX * PARALLAX_X);
            this.parallax.setTranslationY(-this.offY * PARALLAX_Y);
        }
        invalidate();
    }

    private void animateSpread(float f, long j) {
        ValueAnimator valueAnimator = this.spreadAnim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.spread, f);
        this.spreadAnim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(j);
        this.spreadAnim.setInterpolator(new DecelerateInterpolator(1.8f));
        this.spreadAnim.addUpdateListener((valueAnimator2) -> lambda_animateSpread_7(valueAnimator2));
        this.spreadAnim.start();
    }
    /* synthetic */ void lambda_animateSpread_7(ValueAnimator valueAnimator) {
        this.spread = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f;
        float f2;
        if (this.tiles.isEmpty()) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float f3 = 0.3f;
        float f4 = height * (this.peek ? GRID_H_PEEK : GRID_H);
        float f5 = this.spread;
        float f6 = f4 * 0.93f * f5;
        float fSmooth = smooth(f5);
        float f7 = (f4 * 0.62f) / 2.0f;
        float f8 = (width - 96.0f) - f7;
        float f9 = f7 + 80.0f;
        float f10 = f8 + (((width * 0.685f) - f8) * fSmooth);
        float f11 = f9 + (((height * 0.46f) - f9) * fSmooth);
        float f12 = f4 * ((0.38f * fSmooth) + 0.62f);
        if (fSmooth <= 0.01f) {
            f = f11;
            f2 = f10;
        } else {
            // Peeking tiles stay above the news ticker.
            int outer = canvas.save();
            if (this.peek) {
                canvas.clipRect(0.0f, 0.0f, width, height - TICKER_H);
            }
            int r6 = -3;
            while (true) {
                int r5 = 3;
                if (r6 > 3) {
                    break;
                }
                int r4 = -3;
                while (r4 <= r5) {
                    float f13 = r4 + this.offX;
                    float f14 = r6 + this.offY;
                    float fMax = Math.max(Math.abs(f13), Math.abs(f14));
                    if (this.peek) {
                        // Corners two steps out would clutter the peek cross; fade them.
                        float corner = 1.0f - smoothRange(1.2f, 1.8f, Math.min(Math.abs(f13), Math.abs(f14)));
                        if (fMax >= 0.5f && corner > 0.01f) {
                            drawTile(canvas, at(r6, r4), f10 + (warp(f13) * f6), f11 + (warp(f14) * f6), f4 * ((fSmooth * f3) + 0.7f), fMax, true, fSmooth * corner);
                        }
                    } else if (fMax >= 0.5f) {
                        drawTile(canvas, at(r6, r4), f10 + (f13 * f6), f11 + (f14 * f6), f4 * ((fSmooth * f3) + 0.7f), fMax, true, fSmooth);
                    }
                    r4++;
                    f11 = f11;
                    f10 = f10;
                    r5 = r5;
                    r6 = r6;
                    f3 = 0.3f;
                }
                r6++;
                f3 = 0.3f;
            }
            canvas.restoreToCount(outer);
            f = f11;
            f2 = f10;
        }
        int r12 = canvas.save();
        float f15 = this.flipAt;
        if (f15 >= 0.0f) {
            if (f15 >= 0.5f) {
                f15 -= 1.0f;
            }
            float f16 = f15 * 180.0f * this.flipSign;
            this.camera.save();
            this.camera.setLocation(0.0f, 0.0f, -18.0f);
            if (this.flipAxis == 0) {
                this.camera.rotateY(f16);
            } else {
                this.camera.rotateX(f16);
            }
            this.camera.getMatrix(this.flipMatrix);
            this.camera.restore();
            this.flipMatrix.preTranslate(-f2, -f);
            this.flipMatrix.postTranslate(f2, f);
            canvas.concat(this.flipMatrix);
        }
        drawLens(canvas, f2, f, f12, f12 * 0.93f);
        int r8 = -1;
        while (true) {
            int r7 = 1;
            if (r8 > 1) {
                break;
            }
            int r9 = -1;
            while (r9 <= r7) {
                float f17 = r9 + this.offX;
                float f18 = r8 + this.offY;
                float fMax2 = Math.max(Math.abs(f17), Math.abs(f18));
                if (fMax2 < 0.5f) {
                    drawTile(canvas, at(r8, r9), f2 + (f17 * f6), f + (f18 * f6), f12, fMax2, false, 1.0f);
                }
                r9++;
                r7 = r7;
                r8 = r8;
            }
            r8++;
        }
        canvas.restoreToCount(r12);
        if (this.shineAt >= 0.0f) {
            float fWidth = this.lensRect.width() * 0.9f;
            float fWidth2 = (-fWidth) + (this.shineAt * (((this.lensRect.width() + this.lensRect.height()) / 2.0f) + fWidth));
            this.m.setScale(fWidth, fWidth);
            this.m.postTranslate(this.lensRect.left + fWidth2, this.lensRect.top + fWidth2);
            this.shineShader.setLocalMatrix(this.m);
            this.shine.setAlpha((int) ((Math.sin(((double) this.shineAt) * 3.141592653589793d) * 255.0d * 0.8999999761581421d) + 25.0d));
            RectF rectF = this.lensRect;
            float f19 = this.lensRadius;
            canvas.drawRoundRect(rectF, f19, f19, this.shine);
        }
    }

    /** Steps from the center → on-screen steps; beyond one step tiles are pulled in to peek. */
    private static float warp(float d) {
        float a = Math.abs(d);
        if (a <= 1.0f) {
            return d;
        }
        return Math.signum(d) * (1.0f + ((a - 1.0f) * PEEK_PULL));
    }

    private void drawTile(Canvas canvas, Tile tile, float f, float f2, float f3, float f4, boolean z, float f5) {
        float f6;
        if (f4 <= 1.0f) {
            f6 = 1.0f - (0.18f * f4);
        } else if (!this.peek) {
            f6 = (1.0f - ((f4 - 1.0f) / 0.6f)) * 0.82f;
        } else if (f4 <= 2.0f) {
            f6 = 0.82f + ((PEEK_ALPHA - 0.82f) * smooth(f4 - 1.0f));
        } else {
            f6 = PEEK_ALPHA * (1.0f - smooth((f4 - 2.0f) / 0.6f));
        }
        f6 *= f5;
        if (f6 <= 0.01f) {
            return;
        }
        float fSmooth = smooth(1.0f - Math.min(1.0f, f4));
        float f7 = f3 * ((0.24f * fSmooth) + 0.76f);
        float labelAlpha = 1.0f;
        if (this.peek && f4 > 1.0f) {
            f7 *= 1.0f - ((1.0f - PEEK_SCALE) * smooth(f4 - 1.0f));
            // Peeking tiles show only their icon; names would be noise at the edges.
            labelAlpha = 1.0f - smoothRange(1.0f, 1.4f, f4);
        }
        float f8 = f7 / 2.0f;
        float f9 = 0.13f * f7;
        this.rect.set(f - f8, f2 - f8, f + f8, f2 + f8);
        if (z) {
            float fSmoothRange = f6 * smoothRange(0.35f, 0.9f, f4);
            if (fSmoothRange > 0.01f) {
                drawGlass(canvas, this.rect, f9, fSmoothRange, 0.07f, false);
            }
        }
        if (tile.art != null) {
            // The centered icon swells slightly while it lands.
            float f10 = ((0.54f * f7) / 2.0f) * (z ? 1.0f : 1.0f + this.pop);
            float f11 = 0.08f * f7;
            this.dst.set(f - f10, (f2 - f10) - f11, f + f10, (f2 + f10) - f11);
            this.art.setAlpha((int) (f6 * 255.0f));
            canvas.drawBitmap(tile.art, (Rect) null, this.dst, this.art);
        }
        this.label.setTextSize(((0.012f * fSmooth) + 0.074f) * f3);
        this.label.setColor(-1);
        if (labelAlpha <= 0.01f) {
            return;
        }
        this.label.setAlpha((int) (f6 * 255.0f * ((fSmooth * 0.22f) + 0.78f) * labelAlpha));
        canvas.drawText(ellipsize(tile.label, 0.86f * f7), f, f2 + (f7 * 0.36f), this.label);
    }

    private void drawLens(Canvas canvas, float f, float f2, float f3, float f4) {
        float f5 = (f3 / 2.0f) + (0.035f * f3);
        float f6 = this.offX * f4 * 0.55f;
        float f7 = this.offY * f4 * 0.55f;
        this.rect.set(f - f5, f2 - f5, f + f5, f2 + f5);
        if (f6 > 0.0f) {
            this.rect.right += f6;
        } else {
            this.rect.left += f6;
        }
        if (f7 > 0.0f) {
            this.rect.bottom += f7;
        } else {
            this.rect.top += f7;
        }
        this.rect.inset(Math.abs(f7) * 0.1f, Math.abs(f6) * 0.1f);
        float fAbs = (0.15f * f3) + ((Math.abs(f6) + Math.abs(f7)) * 0.12f);
        this.shadow.setColor(16777216);
        this.shadow.setShadowLayer(0.14f * f3, 0.0f, f3 * 0.04f, -1728053248);
        canvas.drawRoundRect(this.rect, fAbs, fAbs, this.shadow);
        this.shadow.clearShadowLayer();
        drawGlass(canvas, this.rect, fAbs, 1.0f, 0.16f, true);
        this.lensRect.set(this.rect);
        this.lensRadius = fAbs;
    }

    private void drawGlass(Canvas canvas, RectF rectF, float f, float f2, float f3, boolean z) {
        BitmapShader bitmapShader = this.backdropOld;
        if (bitmapShader != null && this.backdropMix < 1.0f) {
            this.glass.setShader(bitmapShader);
            this.glass.setAlpha((int) (f2 * 255.0f));
            canvas.drawRoundRect(rectF, f, f, this.glass);
        }
        BitmapShader bitmapShader2 = this.backdrop;
        if (bitmapShader2 != null) {
            this.glass.setShader(bitmapShader2);
            this.glass.setAlpha((int) (f2 * 255.0f * (this.backdropOld != null ? this.backdropMix : 1.0f)));
            canvas.drawRoundRect(rectF, f, f, this.glass);
        } else {
            this.tint.setColor(-15395556);
            this.tint.setAlpha((int) (200.0f * f2));
            canvas.drawRoundRect(rectF, f, f, this.tint);
        }
        this.tint.setColor(-1);
        this.tint.setAlpha((int) (f3 * 255.0f * f2));
        canvas.drawRoundRect(rectF, f, f, this.tint);
        this.m.setScale(1.0f, rectF.height() * 0.6f);
        this.m.postTranslate(0.0f, rectF.top);
        this.sheenShader.setLocalMatrix(this.m);
        float f4 = f2 * 255.0f;
        this.sheen.setAlpha((int) ((z ? 1.0f : 0.55f) * f4));
        canvas.drawRoundRect(rectF, f, f, this.sheen);
        this.m.setScale(1.0f, rectF.height());
        this.m.postTranslate(0.0f, rectF.top);
        this.rimShader.setLocalMatrix(this.m);
        this.rim.setStrokeWidth(z ? 3.0f : 1.5f);
        this.rim.setAlpha((int) (f4 * (z ? 1.0f : 0.45f)));
        canvas.drawRoundRect(rectF, f, f, this.rim);
    }

    private String ellipsize(String str, float f) {
        if (this.label.measureText(str) <= f) {
            return str;
        }
        int length = str.length();
        while (length > 1 && this.label.measureText(str, 0, length) + this.label.measureText("…") > f) {
            length--;
        }
        return str.substring(0, length).trim() + "…";
    }

    private static float smooth(float f) {
        float fClamp = clamp(f, 0.0f, 1.0f);
        return fClamp * fClamp * (3.0f - (fClamp * 2.0f));
    }

    private static float smoothRange(float f, float f2, float f3) {
        return smooth((f3 - f) / (f2 - f));
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }
}
