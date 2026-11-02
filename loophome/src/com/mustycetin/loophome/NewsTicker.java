package com.mustycetin.loophome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class NewsTicker extends View {
    private static final long CARD_MS = 7000;
    private static final long REFRESH_MS = 900000;
    static final String SITE = "https://kantan.news/";
    private static final Locale TR = new Locale("tr", "TR");
    private final Runnable advance;
    private ValueAnimator anim;
    private final Paint badge;
    private final TextPaint badgeText;
    private final Paint bar;
    private final TextPaint category;
    private final ExecutorService ex;
    private float fade;
    private int index;
    private List<String[]> items;
    private final Handler main;
    private volatile String[] podcast;
    private final RectF rect;
    private final Runnable refresh;
    private final TextPaint text;

    public NewsTicker(Context context) {
        super(context);
        Paint paint = new Paint(1);
        this.bar = paint;
        Paint paint2 = new Paint(1);
        this.badge = paint2;
        TextPaint textPaint = new TextPaint(129);
        this.badgeText = textPaint;
        TextPaint textPaint2 = new TextPaint(129);
        this.text = textPaint2;
        TextPaint textPaint3 = new TextPaint(129);
        this.category = textPaint3;
        this.rect = new RectF();
        this.main = new Handler(Looper.getMainLooper());
        this.ex = Executors.newSingleThreadExecutor();
        this.items = new ArrayList();
        this.fade = 1.0f;
        this.refresh = new Runnable() { // from class: com.mustycetin.loophome.NewsTicker.1
            @Override // java.lang.Runnable
            public void run() {
                NewsTicker.this.fetch();
                NewsTicker.this.main.postDelayed(this, NewsTicker.REFRESH_MS);
            }
        };
        this.advance = new Runnable() { // from class: com.mustycetin.loophome.NewsTicker.2
            @Override // java.lang.Runnable
            public void run() {
                NewsTicker.this.next();
                NewsTicker.this.main.postDelayed(this, NewsTicker.CARD_MS);
            }
        };
        paint.setShader(new LinearGradient(0.0f, 0.0f, 1920.0f, 0.0f, new int[]{-1508896746, -1945104362, 856690710}, (float[]) null, Shader.TileMode.CLAMP));
        paint2.setShader(new LinearGradient(0.0f, 0.0f, 250.0f, 0.0f, -34235, -49810, Shader.TileMode.CLAMP));
        textPaint.setColor(-1);
        textPaint.setTextSize(21.0f);
        textPaint.setLetterSpacing(0.18f);
        textPaint2.setColor(-218103809);
        textPaint2.setTextSize(30.0f);
        textPaint3.setColor(-22662);
        textPaint3.setTextSize(20.0f);
        textPaint3.setLetterSpacing(0.2f);
    }

    public void setTypeface(Typeface typeface, Typeface typeface2) {
        this.text.setTypeface(typeface);
        this.category.setTypeface(typeface2);
        this.badgeText.setTypeface(typeface2);
        invalidate();
    }

    public String[] podcast() {
        return this.podcast;
    }

    public void start() {
        this.main.removeCallbacks(this.refresh);
        this.main.removeCallbacks(this.advance);
        this.main.post(this.refresh);
        this.main.postDelayed(this.advance, CARD_MS);
    }

    public void stop() {
        this.main.removeCallbacks(this.refresh);
        this.main.removeCallbacks(this.advance);
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void next() {
        if (this.items.size() < 2) {
            return;
        }
        this.index = (this.index + 1) % this.items.size();
        ValueAnimator valueAnimator = this.anim;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.anim = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(700L);
        this.anim.addUpdateListener((valueAnimator2) -> lambda_next_0(valueAnimator2));
        this.anim.start();
    }
    /* synthetic */ void lambda_next_0(ValueAnimator valueAnimator) {
        this.fade = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetch() {
        this.ex.execute(() -> lambda_fetch_2());
    }
    /* synthetic */ void lambda_fetch_2() {
        try {
            JSONArray jSONArray = new JSONObject(Net.text("https://kantan.news/api/news")).getJSONArray("data");
            final ArrayList arrayList = new ArrayList();
            for (int r5 = 0; r5 < jSONArray.length(); r5++) {
                JSONObject jSONObject = jSONArray.getJSONObject(r5);
                String strTrim = jSONObject.optString("title").trim();
                if (!strTrim.isEmpty() && !"Podcastler".equals(jSONObject.optString("category"))) {
                    arrayList.add(new String[]{jSONObject.optString("category").toUpperCase(TR), strTrim});
                }
            }
            JSONArray jSONArray2 = new JSONObject(Net.text("https://kantan.news/api/news?category=Podcastler")).getJSONArray("data");
            for (int r1 = 0; r1 < jSONArray2.length(); r1++) {
                JSONObject jSONObject2 = jSONArray2.getJSONObject(r1);
                String strOptString = jSONObject2.optString("audio_url");
                if (!strOptString.isEmpty() && "ready".equals(jSONObject2.optString("audio_status"))) {
                    this.podcast = new String[]{jSONObject2.optString("title"), SITE + strOptString, String.valueOf((int) jSONObject2.optDouble("audio_seconds", 0.0d))};
                    break;
                }
            }
            if (!arrayList.isEmpty()) {
                this.main.post(() -> lambda_fetch_1(arrayList));
            }
        } catch (Exception e) {
        }
    }
    /* synthetic */ void lambda_fetch_1(List list) {
        this.items = list;
        this.index = 0;
        this.fade = 1.0f;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (this.items.isEmpty()) {
            return;
        }
        this.rect.set(0.0f, 0.0f, width, height);
        canvas.drawRect(this.rect, this.bar);
        float f = height / 2.0f;
        this.rect.set(110.0f, f - 20.0f, 360.0f, f + 20.0f);
        canvas.drawRoundRect(this.rect, 20.0f, 20.0f, this.badge);
        canvas.drawText("KANTAN · GÜNDEM", ((250.0f - this.badgeText.measureText("KANTAN · GÜNDEM")) / 2.0f) + 110.0f, f - ((this.badgeText.descent() + this.badgeText.ascent()) / 2.0f), this.badgeText);
        float f2 = (width - 396.0f) - 110.0f;
        if (this.fade < 1.0f && this.items.size() > 1) {
            List<String[]> list = this.items;
            String[] strArr = list.get(Math.floorMod(this.index - 1, list.size()));
            float f3 = this.fade;
            drawCard(canvas, strArr, 396.0f, f - (f3 * 18.0f), f2, 1.0f - f3);
        }
        String[] strArr2 = this.items.get(this.index);
        float f4 = this.fade;
        drawCard(canvas, strArr2, 396.0f, f + ((1.0f - f4) * 18.0f), f2, f4);
    }

    private void drawCard(Canvas canvas, String[] strArr, float f, float f2, float f3, float f4) {
        this.category.setAlpha((int) (255.0f * f4));
        this.text.setAlpha((int) (242.0f * f4));
        String str = strArr[0] + "   ";
        float fMeasureText = this.category.measureText(str);
        canvas.drawText(str, f, f2 - ((this.category.descent() + this.category.ascent()) / 2.0f), this.category);
        CharSequence charSequenceEllipsize = TextUtils.ellipsize(strArr[1], this.text, f3 - fMeasureText, TextUtils.TruncateAt.END);
        canvas.drawText(charSequenceEllipsize, 0, charSequenceEllipsize.length(), f + fMeasureText, f2 - ((this.text.descent() + this.text.ascent()) / 2.0f), this.text);
    }
}
