package com.mustycetin.loophome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import java.util.Locale;

public class KeyService extends AccessibilityService {
    static final String ACTION_RECENTS = "com.mustycetin.loophome.RECENTS";
    static final String ACTION_SECRET = "com.mustycetin.loophome.SECRET";
    private static final long EXTEND_MS = 900000;
    private static final long LONG_PRESS_MS = 550;
    private static final long WARN_MS = 300000;
    static KeyService instance;
    private TextView badge;
    private boolean badgeBig;
    private Typeface face;
    private boolean longFired;
    private long sleepEnd;
    private boolean tvPlusFired;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPress = () -> lambda_new_0();
    private final Runnable tvPlusHold = () -> lambda_new_1();
    private final Runnable timerTick = new Runnable() { // from class: com.mustycetin.loophome.KeyService.1
        @Override // java.lang.Runnable
        public void run() {
            long jCurrentTimeMillis = KeyService.this.sleepEnd - System.currentTimeMillis();
            if (KeyService.this.sleepEnd == 0) {
                return;
            }
            if (jCurrentTimeMillis <= 0) {
                KeyService.this.setTimer(0);
                KeyService.sleepNow();
            } else {
                KeyService.this.showBadge(jCurrentTimeMillis);
                KeyService.this.handler.postDelayed(this, 1000L);
            }
        }
    };
    /* synthetic */ void lambda_new_0() {
        this.longFired = true;
        startActivity(new Intent(this, (Class<?>) MainActivity.class).setAction(ACTION_RECENTS).addFlags(805306368));
    }
    /* synthetic */ void lambda_new_1() {
        this.tvPlusFired = true;
        startActivity(new Intent(this, (Class<?>) MainActivity.class).setAction(ACTION_SECRET).addFlags(805306368));
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected void onServiceConnected() {
        instance = this;
        try {
            this.face = new Typeface.Builder(getAssets(), "fonts/outfit.ttf").setFontVariationSettings("'wght' 400").build();
        } catch (Exception e) {
            this.face = Typeface.DEFAULT;
        }
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        setTimer(0);
        instance = null;
        return super.onUnbind(intent);
    }

    static boolean sleepNow() {
        KeyService keyService = instance;
        return keyService != null && keyService.performGlobalAction(8);
    }

    static boolean timer(int r1) {
        KeyService keyService = instance;
        if (keyService == null) {
            return false;
        }
        keyService.setTimer(r1);
        return true;
    }

    static int timerMinutesLeft() {
        KeyService keyService = instance;
        if (keyService == null) {
            return 0;
        }
        long j = keyService.sleepEnd;
        if (j == 0) {
            return 0;
        }
        return (int) Math.max(1L, ((j - System.currentTimeMillis()) + 59999) / 60000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTimer(int r7) {
        this.handler.removeCallbacks(this.timerTick);
        if (r7 <= 0) {
            this.sleepEnd = 0L;
            removeBadge();
        } else {
            this.sleepEnd = System.currentTimeMillis() + (((long) r7) * 60000);
            this.timerTick.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showBadge(long j) {
        StringBuilder sb;
        StringBuilder sbAppend;
        WindowManager windowManager = (WindowManager) getSystemService(WindowManager.class);
        boolean z = j <= WARN_MS;
        long j2 = (j + 999) / 1000;
        String str = String.format(Locale.US, "%d:%02d", Long.valueOf(j2 / 60), Long.valueOf(j2 % 60));
        if (this.badge == null || z != this.badgeBig) {
            removeBadge();
            TextView textView = new TextView(this);
            this.badge = textView;
            textView.setTypeface(this.face);
            this.badge.setTextColor(z ? -1 : -1275068417);
            this.badge.setTextSize(0, z ? 60.0f : 20.0f);
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setCornerRadius(z ? 48.0f : 18.0f);
            gradientDrawable.setColor(z ? -434234578 : -1728053248);
            if (z) {
                gradientDrawable.setStroke(3, -1711306148);
            }
            this.badge.setBackground(gradientDrawable);
            this.badge.setPadding(z ? 44 : 16, z ? 24 : 6, z ? 44 : 16, z ? 24 : 6);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2032, 280, -3);
            layoutParams.gravity = 8388661;
            layoutParams.x = 36;
            layoutParams.y = 28;
            windowManager.addView(this.badge, layoutParams);
            this.badgeBig = z;
        }
        TextView textView2 = this.badge;
        if (z) {
            sb = new StringBuilder();
            sbAppend = sb.append("☾  ").append(str).append(" sonra kapanacak  ·  OK: +15 dk");
        } else {
            sb = new StringBuilder();
            sbAppend = sb.append("☾ ").append(str);
        }
        textView2.setText(sbAppend.toString());
    }

    private void removeBadge() {
        if (this.badge != null) {
            try {
                ((WindowManager) getSystemService(WindowManager.class)).removeView(this.badge);
            } catch (Exception e) {
            }
            this.badge = null;
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected boolean onKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (this.badgeBig && this.sleepEnd != 0 && (keyCode == 23 || keyCode == 66)) {
            if (keyEvent.getAction() == 1) {
                this.sleepEnd += EXTEND_MS;
                this.handler.removeCallbacks(this.timerTick);
                this.timerTick.run();
            }
            return true;
        }
        // Assigning the list key: take the next free key here, before the system can use it.
        if (MainActivity.assigningKey && !MainActivity.isReserved(keyCode)) {
            if (keyEvent.getAction() == 1) {
                this.handler.post(() -> MainActivity.keyCaptured(keyCode));
            }
            return true;
        }
        // The list key opens (or closes) the app list from anywhere, not only on the home screen.
        int listKey = getSharedPreferences("settings", 0).getInt(MainActivity.PREF_LIST_KEY, 0);
        if (listKey != 0 && keyCode == listKey && !MainActivity.assigningKey) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                startActivity(new Intent(this, (Class<?>) MainActivity.class).setAction(MainActivity.ACTION_LIST).addFlags(805306368));
            }
            return true;
        }
        if (keyCode == 192) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                this.tvPlusFired = false;
                this.handler.removeCallbacks(this.tvPlusHold);
                this.handler.postDelayed(this.tvPlusHold, 600L);
            } else if (keyEvent.getAction() == 1) {
                this.handler.removeCallbacks(this.tvPlusHold);
                if (!this.tvPlusFired) {
                    this.longPress.run();
                }
            }
            return true;
        }
        if (keyCode != 3) {
            return false;
        }
        if (keyEvent.getAction() == 0) {
            if (keyEvent.getRepeatCount() == 0) {
                this.longFired = false;
                this.handler.removeCallbacks(this.longPress);
                this.handler.postDelayed(this.longPress, LONG_PRESS_MS);
            }
            return true;
        }
        if (keyEvent.getAction() != 1) {
            return false;
        }
        this.handler.removeCallbacks(this.longPress);
        if (!this.longFired) {
            performGlobalAction(2);
        }
        return true;
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
        this.handler.removeCallbacks(this.longPress);
    }
}
