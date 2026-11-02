package com.mustycetin.loophome;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity implements LoopGrid.Listener, PhotoFeed.Callback {
    private static final int ACCENT = -30116;
    static final String ACTION_GREET = "com.mustycetin.loophome.GREET";
    static final String ACTION_LIST = "com.mustycetin.loophome.LIST";
    // setup.sh: am start -n com.mustycetin.loophome/.MainActivity -a com.mustycetin.loophome.CONFIG
    //   --es unsplash_key KEY --es lat 39.93 --es lon 32.86   (each extra optional)
    static final String ACTION_CONFIG = "com.mustycetin.loophome.CONFIG";
    static final String PREF_UNSPLASH_KEY = "unsplash_key";
    static final String PREF_LAT = "lat";
    static final String PREF_LON = "lon";
    static final String PREF_PEEK = "peek";
    static final String PREF_LIST_KEY = "list_key";
    // While the key-assignment dialog is open, KeyService captures the next key itself:
    // the remote's app buttons (BUTTON_3…16) are taken by the system before any app sees them.
    static volatile boolean assigningKey;
    private static MainActivity current;
    private AlertDialog keyDialog;
    // Keys that already do something (navigation, Home/TV+, volume, voice search) can't be the list key.
    private static final int[] RESERVED_KEYS = {0, 3, 4, 19, 20, 21, 22, 23, 24, 25, 26, 66, 82, 84, 160, 164, 165, 192, 193, 219, 223, 224, 231};
    private static final String BBC = "bbc.6minute";
    private static final String BBC_LIST = "https://www.bbc.co.uk/learningenglish/english/features/6-minute-english";
    private static final String BBC_SITE = "https://www.bbc.co.uk";
    private static final long FINANCE_MS = 60000;
    private static final Map<String, String> ICONS;
    private static final long IDLE_MS = 300000;
    private static final String KANTAN = "kantan.gundem";
    private static final long ROTATE_MS = 45000;
    private static final String SLEEP_TIMER = "@sleep.timer";
    private static final long SLEEP_WARN_MS = 300000;
    private static final float ZOOM = 1.09f;
    private FrameLayout blackout;
    private TextView blackoutText;
    private TextView captionKind;
    private TextView captionTitle;
    private TextView clock;
    private TextView date;
    private TextView day;
    private PhotoFeed feed;
    private FinanceCard financeCard;
    private FrameLayout greet;
    private TextView greetSub;
    private TextView greetText;
    private LoopGrid grid;
    private Typeface light;
    private Typeface medium;
    private TextView muteBadge;
    private View night;
    private TextView nowPlaying;
    private ImageView photoA;
    private ImageView photoB;
    private FrameLayout photoHost;
    private ImageView photoShown;
    private MediaPlayer player;
    private String playing;
    private String playingUrl;
    private boolean prepared;
    private RecentsView recents;
    private boolean recentsPending;
    private AppListView appList;
    private boolean listPending;
    private Typeface regular;
    private boolean resumed;
    private boolean secretPending;
    private long sleepAt;
    private TextView sleepBanner;
    private boolean swallowUp;
    private Typeface thin;
    private NewsTicker ticker;
    private static final String START_PKG = "com.google.android.youtube.tv";
    private static final String MIRROR_PKG = "com.xiaomi.mitv.smartshare";
    private static final String PODCASTS = "podcasts";
    private static final List<String> FRONT = Arrays.asList(START_PKG, MIRROR_PKG, PODCASTS);
    private static final List<String> BACK = Arrays.asList("com.android.vending", "com.android.tv.settings");
    private static final Locale TR = new Locale("tr", "TR");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Bitmap> artCache = new HashMap();
    private final Map<String, LoopGrid.Tile> tilesByPkg = new HashMap();
    private final Map<String, Bitmap> podcastArt = new HashMap();
    private boolean greetPending = true;
    private final BroadcastReceiver screenOn = new BroadcastReceiver() { // from class: com.mustycetin.loophome.MainActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MainActivity.this.startActivity(new Intent(context, (Class<?>) MainActivity.class).setAction(MainActivity.ACTION_GREET).addFlags(805306368));
        }
    };
    private final Runnable idleWarn = () -> lambda_new_0();
    private final Runnable sleepTick = new Runnable() { // from class: com.mustycetin.loophome.MainActivity.2
        @Override // java.lang.Runnable
        public void run() {
            long jCurrentTimeMillis = MainActivity.this.sleepAt - System.currentTimeMillis();
            if (jCurrentTimeMillis <= 0) {
                MainActivity.this.hideSleepBanner();
                if (!KeyService.sleepNow()) {
                    Toast.makeText(MainActivity.this, "Uyutulamadı (erişilebilirlik servisi kapalı)", 1).show();
                    return;
                }
                return;
            }
            long j = (jCurrentTimeMillis + 999) / 1000;
            MainActivity.this.sleepBanner.setText(String.format(MainActivity.TR, "☾  %d:%02d sonra cihaz uyutulacak  ·  iptal etmek için herhangi bir tuşa basın", Long.valueOf(j / 60), Long.valueOf(j % 60)));
            MainActivity.this.handler.postDelayed(this, 1000L);
        }
    };
    private final Random rnd = new Random();
    private final Runnable drift = new Runnable() { // from class: com.mustycetin.loophome.MainActivity.3
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.blackoutText.setText(MainActivity.this.nowPlaying.getText());
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) MainActivity.this.blackoutText.getLayoutParams();
            layoutParams.leftMargin = MainActivity.this.rnd.nextInt(900) + 80;
            layoutParams.topMargin = MainActivity.this.rnd.nextInt(860) + 80;
            MainActivity.this.blackoutText.setLayoutParams(layoutParams);
            MainActivity.this.handler.postDelayed(this, 20000L);
        }
    };
    private final Runnable rotate = new Runnable() { // from class: com.mustycetin.loophome.MainActivity.4
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.feed.next(MainActivity.this);
            MainActivity.this.handler.postDelayed(this, MainActivity.ROTATE_MS);
        }
    };
    private final Runnable tick = new Runnable() { // from class: com.mustycetin.loophome.MainActivity.5
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.updateClock();
            MainActivity.this.updateNight();
            MainActivity.this.updateMute();
            MainActivity.this.handler.postDelayed(this, (MainActivity.FINANCE_MS - (System.currentTimeMillis() % MainActivity.FINANCE_MS)) + 50);
        }
    };
    private final Runnable finance = new AnonymousClass6();
    private final BroadcastReceiver packages = new BroadcastReceiver() { // from class: com.mustycetin.loophome.MainActivity.7
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MainActivity.this.loadApps();
            MainActivity.this.loadPodcastArt();
            MainActivity mainActivity = MainActivity.this;
            mainActivity.recentsPending = "com.mustycetin.loophome.RECENTS".equals(mainActivity.getIntent().getAction());
        }
    };
    private final Runnable savePeriodically = new Runnable() { // from class: com.mustycetin.loophome.MainActivity.9
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.savePosition();
            if (MainActivity.this.player != null) {
                MainActivity.this.handler.postDelayed(this, 10000L);
            }
        }
    };

    static {
        HashMap map = new HashMap();
        ICONS = map;
        map.put(START_PKG, "youtube");
        map.put("com.apple.atve.androidtv.appletv", "apple_tv");
        map.put("com.stremio.one", "stremio");
        map.put("com.lagradost.cloudstream3", "cloudstream");
        map.put("com.wbd.stream", "max_stream");
        map.put("com.teamviewer.quicksupport.market", "teamviewer");
        map.put("com.android.tv.settings", "settings");
        map.put("org.localsend.localsend_app", "localsend");
        map.put("com.spocky.projengmenu", "projectivy_launcher");
        map.put("com.android.vending", "google_play_store");
        map.put(MIRROR_PKG, "android_miracast");
        map.put("com.turkcell.ott", "tvplus");
    }
    /* synthetic */ void lambda_new_0() {
        if (!this.resumed || audioBusy()) {
            return;
        }
        this.sleepAt = System.currentTimeMillis() + 300000;
        this.sleepBanner.setAlpha(0.0f);
        this.sleepBanner.setVisibility(0);
        this.sleepBanner.animate().alpha(1.0f).setDuration(500L).start();
        this.sleepTick.run();
    }
    class AnonymousClass6 implements Runnable {
        AnonymousClass6() {
        }

        @Override // java.lang.Runnable
        public void run() {
            new Thread(() -> lambda_run_1()).start();
            MainActivity.this.handler.postDelayed(this, MainActivity.FINANCE_MS);
        }
        /* synthetic */ void lambda_run_1() {
            try {
                final Object[] objArrFetch = Finance.fetch();
                final List list = (List) objArrFetch[0];
                MainActivity.this.handler.post(() -> lambda_run_0(list, objArrFetch));
            } catch (Exception e) {
            }
        }
        /* synthetic */ void lambda_run_0(List list, Object[] objArr) {
            MainActivity.this.showFinance(list, (String) objArr[1]);
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.thin = font(150);
        this.light = font(300);
        this.regular = font(400);
        this.medium = font(600);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(-16053488);
        this.photoHost = new FrameLayout(this);
        this.photoA = photoView();
        this.photoB = photoView();
        this.photoHost.addView(this.photoA, match());
        this.photoHost.addView(this.photoB, match());
        this.photoB.setAlpha(0.0f);
        this.photoShown = this.photoA;
        frameLayout.addView(this.photoHost, match());
        View view = new View(this);
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-436207616, 1711276032, 1291845632, Integer.MIN_VALUE}));
        frameLayout.addView(view, match());
        View view2 = new View(this);
        view2.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{-872415232, 0}));
        frameLayout.addView(view2, new FrameLayout.LayoutParams(-1, 420, 80));
        View view3 = new View(this);
        this.night = view3;
        view3.setBackgroundColor(-14938364);
        this.night.setAlpha(0.0f);
        frameLayout.addView(this.night, match());
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        TextView textViewText = text(176.0f, -1, this.thin);
        this.clock = textViewText;
        textViewText.setLetterSpacing(-0.02f);
        this.clock.getPaint().setShader(new LinearGradient(0.0f, 20.0f, 0.0f, 180.0f, -1, -1275068417, Shader.TileMode.CLAMP));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        TextView textViewText2 = text(24.0f, ACCENT, this.medium);
        this.day = textViewText2;
        textViewText2.setLetterSpacing(0.42f);
        View view4 = new View(this);
        view4.setBackgroundColor(-2130706433);
        TextView textViewText3 = text(28.0f, -419430401, this.light);
        this.date = textViewText3;
        textViewText3.setLetterSpacing(0.06f);
        linearLayout2.addView(this.day);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(56, 2);
        layoutParams.leftMargin = 18;
        layoutParams.rightMargin = 22;
        linearLayout2.addView(view4, layoutParams);
        linearLayout2.addView(this.date);
        TextView textViewText4 = text(24.0f, -1, this.regular);
        this.nowPlaying = textViewText4;
        textViewText4.setBackground(pill());
        this.nowPlaying.setPadding(26, 12, 26, 12);
        this.nowPlaying.setVisibility(8);
        this.nowPlaying.setMaxWidth(760);
        this.nowPlaying.setSingleLine(true);
        this.nowPlaying.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout.addView(this.clock);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = 6;
        layoutParams2.leftMargin = 8;
        linearLayout.addView(linearLayout2, layoutParams2);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = 26;
        layoutParams3.leftMargin = 4;
        linearLayout.addView(this.nowPlaying, layoutParams3);
        this.muteBadge = text(22.0f, -419430401, this.medium);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(30.0f);
        gradientDrawable.setColor(-1716312546);
        this.muteBadge.setBackground(gradientDrawable);
        this.muteBadge.setPadding(22, 10, 22, 10);
        this.muteBadge.setText("🔇  Ses kapalı");
        this.muteBadge.setVisibility(8);
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams4.topMargin = 22;
        layoutParams4.leftMargin = 4;
        linearLayout.addView(this.muteBadge, layoutParams4);
        FinanceCard financeCard = new FinanceCard(this);
        this.financeCard = financeCard;
        financeCard.setTypefaces(this.light, this.medium);
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(440, 118);
        layoutParams5.topMargin = 30;
        linearLayout.addView(this.financeCard, layoutParams5);
        FrameLayout.LayoutParams layoutParams6 = new FrameLayout.LayoutParams(-2, -2, 8388659);
        layoutParams6.leftMargin = 102;
        layoutParams6.topMargin = 56;
        frameLayout.addView(linearLayout, layoutParams6);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        TextView textViewText5 = text(19.0f, -1275068417, this.medium);
        this.captionKind = textViewText5;
        textViewText5.setLetterSpacing(0.35f);
        TextView textViewText6 = text(26.0f, -419430401, this.light);
        this.captionTitle = textViewText6;
        textViewText6.setMaxWidth(700);
        this.captionTitle.setMaxLines(1);
        this.captionTitle.setEllipsize(TextUtils.TruncateAt.END);
        linearLayout3.addView(this.captionKind);
        linearLayout3.addView(this.captionTitle);
        FrameLayout.LayoutParams layoutParams7 = new FrameLayout.LayoutParams(-2, -2, 8388691);
        layoutParams7.leftMargin = 110;
        layoutParams7.bottomMargin = 120;
        frameLayout.addView(linearLayout3, layoutParams7);
        NewsTicker newsTicker = new NewsTicker(this);
        this.ticker = newsTicker;
        newsTicker.setTypeface(this.regular, this.medium);
        frameLayout.addView(this.ticker, new FrameLayout.LayoutParams(-1, 78, 80));
        LoopGrid loopGrid = new LoopGrid(this);
        this.grid = loopGrid;
        loopGrid.setListener(this);
        this.grid.setLabelTypeface(this.medium);
        this.grid.setParallaxTarget(this.photoHost);
        this.grid.setPeek(settings().getBoolean(PREF_PEEK, false));
        current = this;
        applyConfig(getIntent());
        applyLocation();
        frameLayout.addView(this.grid, match());
        RecentsView recentsView = new RecentsView(this);
        this.recents = recentsView;
        recentsView.setTypefaces(this.regular, this.medium);
        this.recents.setVisibility(8);
        this.recents.setListener(new RecentsView.Listener() { // from class: com.mustycetin.loophome.MainActivity.8
            @Override // com.mustycetin.loophome.RecentsView.Listener
            public void onOpen(LoopGrid.Tile tile) {
                MainActivity.this.grid.requestFocus();
                if (MainActivity.SLEEP_TIMER.equals(tile.pkg)) {
                    MainActivity.this.chooseSleepTimer();
                } else {
                    MainActivity.this.onLaunch(tile);
                }
            }

            @Override // com.mustycetin.loophome.RecentsView.Listener
            public void onClose(LoopGrid.Tile tile) {
                MainActivity.this.closeApp(tile.pkg);
            }

            @Override // com.mustycetin.loophome.RecentsView.Listener
            public void onDismiss() {
                MainActivity.this.grid.requestFocus();
                MainActivity.this.grid.wake();
            }
        });
        frameLayout.addView(this.recents, match());
        this.appList = new AppListView(this);
        this.appList.setTypefaces(this.regular, this.medium);
        this.appList.setToggleKey(listKey());
        this.appList.setVisibility(8);
        this.appList.setListener(new AppListView.Listener() {
            @Override
            public void onOpen(LoopGrid.Tile tile) {
                MainActivity.this.grid.requestFocus();
                MainActivity.this.onLaunch(tile);
            }

            @Override
            public void onDismiss() {
                MainActivity.this.grid.requestFocus();
                MainActivity.this.grid.wake();
            }
        });
        frameLayout.addView(this.appList, match());
        FrameLayout frameLayout2 = new FrameLayout(this);
        this.blackout = frameLayout2;
        frameLayout2.setBackgroundColor(-16777216);
        this.blackout.setFocusable(true);
        this.blackout.setFocusableInTouchMode(true);
        this.blackout.setVisibility(8);
        TextView textViewText7 = text(24.0f, 1509949439, this.light);
        this.blackoutText = textViewText7;
        textViewText7.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
        this.blackout.addView(this.blackoutText, new FrameLayout.LayoutParams(-2, -2));
        this.blackout.setOnKeyListener((view5, r3, keyEvent) -> lambda_onCreate_1(view5, r3, keyEvent));
        frameLayout.addView(this.blackout, match());
        this.sleepBanner = text(26.0f, -1, this.regular);
        GradientDrawable gradientDrawable2 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-433444294, -434496986});
        gradientDrawable2.setCornerRadius(44.0f);
        gradientDrawable2.setStroke(2, 1728053247);
        this.sleepBanner.setBackground(gradientDrawable2);
        this.sleepBanner.setPadding(40, 22, 40, 22);
        this.sleepBanner.setVisibility(8);
        FrameLayout.LayoutParams layoutParams8 = new FrameLayout.LayoutParams(-2, -2, 49);
        layoutParams8.topMargin = 40;
        frameLayout.addView(this.sleepBanner, layoutParams8);
        FrameLayout frameLayout3 = new FrameLayout(this);
        this.greet = frameLayout3;
        frameLayout3.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-234157294, -434891744, -234157294}));
        this.greet.setVisibility(8);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(1);
        linearLayout4.setGravity(1);
        TextView textViewText8 = text(104.0f, -1, this.thin);
        this.greetText = textViewText8;
        textViewText8.setGravity(17);
        this.greetText.setSingleLine(true);
        TextView textViewText9 = text(28.0f, ACCENT, this.medium);
        this.greetSub = textViewText9;
        textViewText9.setLetterSpacing(0.4f);
        this.greetSub.setGravity(17);
        linearLayout4.addView(this.greetText, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams layoutParams9 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams9.topMargin = 26;
        linearLayout4.addView(this.greetSub, layoutParams9);
        this.greet.addView(linearLayout4, new FrameLayout.LayoutParams(-1, -2, 17));
        frameLayout.addView(this.greet, match());
        setContentView(frameLayout);
        this.grid.requestFocus();
        PhotoFeed photoFeed = new PhotoFeed(this);
        this.feed = photoFeed;
        Bitmap bitmapLoadCached = photoFeed.loadCached();
        if (bitmapLoadCached != null) {
            this.photoA.setImageBitmap(bitmapLoadCached);
            this.grid.setBackdrop(bitmapLoadCached);
            setCaption(this.feed.cachedKind(), this.feed.cachedTitle());
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme("package");
        registerReceiver(this.packages, intentFilter);
        registerReceiver(this.screenOn, new IntentFilter("android.intent.action.SCREEN_ON"));
        loadApps();
    }
    /* synthetic */ boolean lambda_onCreate_1(View view, int r2, KeyEvent keyEvent) {
        if (keyEvent.getAction() != 0 || keyEvent.getRepeatCount() != 0) {
            return true;
        }
        hideBlackout();
        return true;
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        // KeyService normally catches the list key everywhere; this covers it being off.
        int listKey = listKey();
        if (listKey != 0 && keyEvent.getKeyCode() == listKey && KeyService.instance == null && !assigningKey) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                toggleAppList();
            }
            return true;
        }
        if (keyEvent.getAction() == 0) {
            boolean z = this.sleepBanner.getVisibility() == 0;
            restartIdle();
            if (z) {
                return true;
            }
        } else if (keyEvent.getAction() == 1 && this.swallowUp) {
            this.swallowUp = false;
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    private void restartIdle() {
        if (this.sleepBanner.getVisibility() == 0) {
            hideSleepBanner();
            this.swallowUp = true;
        }
        this.handler.removeCallbacks(this.idleWarn);
        if (!this.resumed || audioBusy()) {
            return;
        }
        this.handler.postDelayed(this.idleWarn, 300000L);
    }

    private boolean audioBusy() {
        MediaPlayer mediaPlayer = this.player;
        return mediaPlayer != null && (!this.prepared || mediaPlayer.isPlaying());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideSleepBanner() {
        this.handler.removeCallbacks(this.sleepTick);
        this.sleepBanner.animate().cancel();
        this.sleepBanner.setVisibility(8);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_CONFIG.equals(intent.getAction())) {
            applyConfig(intent);
            return;
        }
        if (ACTION_LIST.equals(intent.getAction())) {
            if (this.recents.getVisibility() == 0) {
                this.recents.hide();
            }
            if (this.appList.isShowing()) {
                this.appList.hide();
            } else {
                this.listPending = true;
            }
            return;
        }
        if (this.appList.isShowing()) {
            this.appList.hide();
        }
        if ("com.mustycetin.loophome.SECRET".equals(intent.getAction())) {
            if (this.recents.getVisibility() == 0) {
                this.recents.hide();
            }
            this.secretPending = true;
            return;
        }
        if (ACTION_GREET.equals(intent.getAction())) {
            this.greetPending = true;
            if (this.recents.getVisibility() == 0) {
                this.recents.hide();
                return;
            }
            return;
        }
        if ("com.mustycetin.loophome.RECENTS".equals(intent.getAction())) {
            if (this.recents.getVisibility() == 0) {
                this.recents.hide();
                return;
            } else {
                this.recentsPending = true;
                return;
            }
        }
        if (this.recents.getVisibility() == 0) {
            this.recents.hide();
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.resumed = true;
        loadApps();
        if (this.greetPending) {
            this.greetPending = false;
            showGreeting();
        }
        if (this.secretPending) {
            this.secretPending = false;
            this.handler.post(() -> onSecret());
        }
        restartIdle();
        if (this.recentsPending) {
            this.recentsPending = false;
            this.handler.post(() -> onRecents());
        }
        if (this.listPending) {
            this.listPending = false;
            this.handler.post(this::showAppList);
        }
        this.grid.requestFocus();
        this.grid.wake();
        this.ticker.start();
        this.handler.post(this.tick);
        this.handler.post(this.finance);
        this.financeCard.start();
        this.handler.postDelayed(this.rotate, 1500L);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        this.resumed = false;
        this.handler.removeCallbacks(this.idleWarn);
        hideSleepBanner();
        this.grid.sleepNow();
        this.ticker.stop();
        this.handler.removeCallbacks(this.tick);
        this.handler.removeCallbacks(this.finance);
        this.financeCard.stop();
        this.handler.removeCallbacks(this.rotate);
    }

    @Override // android.app.Activity
    protected void onStop() {
        super.onStop();
        stopAudio();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (current == this) {
            current = null;
        }
        unregisterReceiver(this.packages);
        unregisterReceiver(this.screenOn);
        super.onDestroy();
    }

    @Override // android.app.Activity
    public void onBackPressed() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadApps() {
        PackageManager packageManager = getPackageManager();
        LinkedHashMap<String, LoopGrid.Tile> linkedHashMap = new LinkedHashMap<>();
        String[] strArr = {"android.intent.category.LEANBACK_LAUNCHER", "android.intent.category.LAUNCHER"};
        for (int r4 = 0; r4 < 2; r4++) {
            String str = strArr[r4];
            for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory(str), 0)) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                if (!activityInfo.packageName.equals(getPackageName()) && !linkedHashMap.containsKey(activityInfo.packageName) && !activityInfo.name.endsWith("MediaExplorerActivity")) {
                    LoopGrid.Tile tile = new LoopGrid.Tile();
                    tile.pkg = activityInfo.packageName;
                    tile.label = MIRROR_PKG.equals(tile.pkg) ? "Ekran Yansıtma" : String.valueOf(resolveInfo.loadLabel(packageManager));
                    tile.art = art(tile.pkg, resolveInfo, packageManager);
                    tile.intent = new Intent("android.intent.action.MAIN").addCategory(str).setComponent(new ComponentName(activityInfo.packageName, activityInfo.name)).addFlags(270532608);
                    linkedHashMap.put(tile.pkg, tile);
                }
            }
        }
        LoopGrid.Tile tile2 = new LoopGrid.Tile();
        tile2.pkg = PODCASTS;
        tile2.label = "Podcastler";
        tile2.art = podcastsArt();
        linkedHashMap.put(PODCASTS, tile2);
        ArrayList<LoopGrid.Tile> arrayList = new ArrayList(linkedHashMap.values());
        final Collator collator = Collator.getInstance(TR);
        arrayList.sort(new Comparator() { // from class: com.mustycetin.loophome.MainActivity$$ExternalSyntheticLambda3
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MainActivity.lambda$loadApps$2(collator, (LoopGrid.Tile) obj, (LoopGrid.Tile) obj2);
            }
        });
        this.grid.setTiles(arrayList, START_PKG);
        this.tilesByPkg.clear();
        for (LoopGrid.Tile tile3 : arrayList) {
            this.tilesByPkg.put(tile3.pkg, tile3);
        }
    }

    static /* synthetic */ int lambda$loadApps$2(Collator collator, LoopGrid.Tile tile, LoopGrid.Tile tile2) {
        int r0 = rank(tile.pkg);
        int r1 = rank(tile2.pkg);
        return r0 != r1 ? Integer.compare(r0, r1) : collator.compare(tile.label, tile2.label);
    }

    private static int rank(String str) {
        int r0 = FRONT.indexOf(str);
        if (r0 >= 0) {
            return r0;
        }
        int r1 = BACK.indexOf(str);
        if (r1 >= 0) {
            return r1 + 1000;
        }
        return 100;
    }

    private Bitmap art(String str, ResolveInfo resolveInfo, PackageManager packageManager) {
        Bitmap bitmapDecodeStream;
        Bitmap bitmap = this.artCache.get(str);
        if (bitmap != null) {
            return bitmap;
        }
        String str2 = ICONS.get(str);
        Drawable drawableLoadIcon = null;
        if (str2 == null) {
            bitmapDecodeStream = null;
        } else {
            try {
                InputStream inputStreamOpen = getAssets().open("icons/" + str2 + ".png");
                try {
                    bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpen);
                    if (inputStreamOpen != null) {
                        try {
                            inputStreamOpen.close();
                        } catch (IOException e) {
                        }
                    }
                } catch (Throwable th) {
                    if (inputStreamOpen != null) {
                        try {
                            inputStreamOpen.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (IOException e2) {
                bitmapDecodeStream = null;
            }
        }
        if (bitmapDecodeStream == null) {
            try {
                Resources resourcesForApplication = packageManager.getResourcesForApplication(resolveInfo.activityInfo.applicationInfo);
                int iconResource = resolveInfo.getIconResource();
                if (iconResource != 0) {
                    drawableLoadIcon = resourcesForApplication.getDrawableForDensity(iconResource, 640, null);
                }
            } catch (Exception e3) {
            }
            if (drawableLoadIcon == null) {
                drawableLoadIcon = resolveInfo.loadIcon(packageManager);
            }
            bitmapDecodeStream = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
            drawableLoadIcon.setBounds(0, 0, 256, 256);
            drawableLoadIcon.draw(new Canvas(bitmapDecodeStream));
        }
        this.artCache.put(str, bitmapDecodeStream);
        return bitmapDecodeStream;
    }

    private Bitmap bbcArt() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        paint.setShader(new LinearGradient(0.0f, 0.0f, 256.0f, 256.0f, -3926500, -10613998, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(16.0f, 16.0f, 240.0f, 240.0f), 52.0f, 52.0f, paint);
        Paint paint2 = new Paint(1);
        paint2.setColor(-1);
        paint2.setTextAlign(Paint.Align.CENTER);
        paint2.setTypeface(this.medium);
        paint2.setTextSize(120.0f);
        canvas.drawText("6'", 128.0f, 140.0f, paint2);
        paint2.setTypeface(this.regular);
        paint2.setTextSize(30.0f);
        paint2.setLetterSpacing(0.2f);
        canvas.drawText("ENGLISH", 128.0f, 196.0f, paint2);
        return bitmapCreateBitmap;
    }

    private Bitmap podcastsArt() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        paint.setShader(new LinearGradient(0.0f, 0.0f, 256.0f, 256.0f, -7447297, -12965218, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(16.0f, 16.0f, 240.0f, 240.0f), 52.0f, 52.0f, paint);
        Paint paint2 = new Paint(1);
        paint2.setColor(-1);
        canvas.drawRoundRect(new RectF(103.0f, 58.0f, 153.0f, 148.0f), 25.0f, 25.0f, paint2);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(11.0f);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(new RectF(83.0f, 88.0f, 173.0f, 168.0f), 0.0f, 180.0f, false, paint2);
        canvas.drawLine(128.0f, 168.0f, 128.0f, 196.0f, paint2);
        canvas.drawLine(104.0f, 196.0f, 152.0f, 196.0f, paint2);
        return bitmapCreateBitmap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadPodcastArt() {
        new Thread(() -> lambda_loadPodcastArt_4()).start();
    }
    /* synthetic */ void lambda_loadPodcastArt_4() {
        for (final Podcasts.Show show : Podcasts.ALL) {
            final Bitmap bitmapArt = Podcasts.art(show);
            if (bitmapArt != null) {
                this.handler.post(() -> lambda_loadPodcastArt_3(show, bitmapArt));
            }
        }
    }
    /* synthetic */ void lambda_loadPodcastArt_3(Podcasts.Show show, Bitmap bitmap) {
        this.podcastArt.put(show.id, bitmap);
        this.recents.invalidate();
    }

    private void showPodcasts() {
        ArrayList arrayList = new ArrayList();
        for (Podcasts.Show show : Podcasts.ALL) {
            LoopGrid.Tile tile = new LoopGrid.Tile();
            tile.pkg = show.id;
            tile.label = show.name;
            Bitmap bitmapKantanArt = this.podcastArt.get(show.id);
            if (bitmapKantanArt == null) {
                bitmapKantanArt = KANTAN.equals(show.id) ? kantanArt() : BBC.equals(show.id) ? bbcArt() : podcastsArt();
            }
            tile.art = bitmapKantanArt;
            arrayList.add(new RecentsView.Item(tile, 0L, show.equals(Podcasts.byId(this.playing)) ? "♪ çalıyor" : show.sub));
        }
        this.recents.show("PODCASTLER", "OK: dinle / duraklat   ·   Geri: çık", false, arrayList);
    }

    private Bitmap kantanArt() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        paint.setShader(new LinearGradient(0.0f, 0.0f, 256.0f, 256.0f, -34235, -49810, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(16.0f, 16.0f, 240.0f, 240.0f), 52.0f, 52.0f, paint);
        Paint paint2 = new Paint(1);
        paint2.setColor(-1);
        paint2.setTypeface(this.medium);
        paint2.setTextSize(150.0f);
        paint2.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("K", 128.0f, 128.0f - ((paint2.descent() + paint2.ascent()) / 2.0f), paint2);
        return bitmapCreateBitmap;
    }

    @Override // com.mustycetin.loophome.LoopGrid.Listener
    public void onLaunch(LoopGrid.Tile tile) {
        if (PODCASTS.equals(tile.pkg)) {
            showPodcasts();
            return;
        }
        if (Podcasts.byId(tile.pkg) != null) {
            toggleAudio(tile.pkg);
            return;
        }
        try {
            startActivity(tile.intent);
        } catch (Exception e) {
            Toast.makeText(this, tile.label + " açılamadı", 0).show();
        }
    }

    @Override // com.mustycetin.loophome.LoopGrid.Listener
    public void onInfo(LoopGrid.Tile tile) {
        if (tile.intent == null) {
            return;
        }
        try {
            startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + tile.pkg)).addFlags(268435456));
        } catch (Exception e) {
        }
    }

    @Override // com.mustycetin.loophome.LoopGrid.Listener
    public void onRecents() {
        ArrayList arrayList = new ArrayList();
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(UsageStatsManager.class);
            long jCurrentTimeMillis = System.currentTimeMillis();
            UsageEvents usageEventsQueryEvents = usageStatsManager.queryEvents(jCurrentTimeMillis - 21600000, jCurrentTimeMillis);
            LinkedHashMap<String, Long> linkedHashMap = new LinkedHashMap<>();
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEventsQueryEvents.hasNextEvent()) {
                usageEventsQueryEvents.getNextEvent(event);
                if (event.getEventType() == 1) {
                    linkedHashMap.put(event.getPackageName(), Long.valueOf(event.getTimeStamp()));
                }
            }
            SharedPreferences sharedPreferences = getSharedPreferences("closed", 0);
            for (Map.Entry<String, Long> entry : linkedHashMap.entrySet()) {
                LoopGrid.Tile tile = this.tilesByPkg.get(entry.getKey());
                if (tile != null && tile.intent != null && sharedPreferences.getLong((String) entry.getKey(), 0L) < ((Long) entry.getValue()).longValue()) {
                    arrayList.add(new RecentsView.Item(tile, ((Long) entry.getValue()).longValue()));
                }
            }
            arrayList.sort(new Comparator() { // from class: com.mustycetin.loophome.MainActivity$$ExternalSyntheticLambda5
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return Long.compare(((RecentsView.Item) obj2).lastUsed, ((RecentsView.Item) obj).lastUsed);
                }
            });
        } catch (Exception e) {
        }
        LoopGrid.Tile tile2 = new LoopGrid.Tile();
        tile2.pkg = SLEEP_TIMER;
        tile2.label = "Uyku zamanlayıcısı";
        tile2.art = moonArt();
        int r5 = KeyService.timerMinutesLeft();
        arrayList.add(0, new RecentsView.Item(tile2, 0L, r5 > 0 ? r5 + " dk sonra kapanacak" : "kapalı"));
        this.recents.show(arrayList);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void closeApp(String str) {
        try {
            ((ActivityManager) getSystemService(ActivityManager.class)).killBackgroundProcesses(str);
        } catch (Exception e) {
        }
        getSharedPreferences("closed", 0).edit().putLong(str, System.currentTimeMillis()).apply();
    }

    @Override // com.mustycetin.loophome.LoopGrid.Listener
    public void onSecret() {
        int r0 = KeyService.timerMinutesLeft();
        final boolean zIsStreamMute = audio().isStreamMute(3);
        String[] strArr = new String[6];
        strArr[0] = r0 > 0 ? "Uyku zamanlayıcısı (" + r0 + " dk kaldı)" : "Uyku zamanlayıcısı";
        strArr[1] = "Şimdi kapat";
        strArr[2] = "Sonraki fotoğraf";
        strArr[3] = "Android ayarları";
        strArr[4] = zIsStreamMute ? "Sesi aç" : "Sesi kapat";
        strArr[5] = "Loop Home ayarları";
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Gizli menü").setItems(strArr, (dialogInterface, r4) -> lambda_onSecret_6(zIsStreamMute, dialogInterface, r4)).show();
    }
    /* synthetic */ void lambda_onSecret_6(boolean z, DialogInterface dialogInterface, int r6) {
        try {
            if (r6 == 0) {
                chooseSleepTimer();
            } else if (r6 == 1) {
                if (!KeyService.sleepNow()) {
                    Toast.makeText(this, "Kapatılamadı", 0).show();
                }
            } else if (r6 == 2) {
                this.feed.next(this);
            } else if (r6 == 3) {
                startActivity(new Intent("android.settings.SETTINGS").addFlags(268435456));
            } else if (r6 == 4) {
                audio().adjustStreamVolume(3, z ? 100 : -100, 1);
                updateMute();
            } else if (r6 == 5) {
                showSettings();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Açılamadı", 0).show();
        }
    }

    private SharedPreferences settings() {
        return getSharedPreferences("settings", 0);
    }

    /** Stores the Unsplash key and city that setup.sh sends; each user brings their own. */
    private void applyConfig(Intent intent) {
        if (intent == null || !ACTION_CONFIG.equals(intent.getAction())) {
            return;
        }
        SharedPreferences.Editor editor = settings().edit();
        String key = intent.getStringExtra(PREF_UNSPLASH_KEY);
        if (key != null) {
            editor.putString(PREF_UNSPLASH_KEY, key.trim());
        }
        try {
            String lat = intent.getStringExtra(PREF_LAT);
            String lon = intent.getStringExtra(PREF_LON);
            if (lat != null && lon != null) {
                editor.putFloat(PREF_LAT, Float.parseFloat(lat)).putFloat(PREF_LON, Float.parseFloat(lon));
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Konum anlaşılamadı", 1).show();
        }
        editor.commit();
        applyLocation();
        if (key != null && this.feed != null) {
            this.feed = new PhotoFeed(this);
        }
        Toast.makeText(this, "Loop Home ayarları güncellendi", 0).show();
    }

    private void applyLocation() {
        SharedPreferences prefs = settings();
        if (prefs.contains(PREF_LAT) && prefs.contains(PREF_LON)) {
            Sun.setLocation(prefs.getFloat(PREF_LAT, 0.0f), prefs.getFloat(PREF_LON, 0.0f));
        }
    }

    private int listKey() {
        return settings().getInt(PREF_LIST_KEY, 0);
    }

    private void showSettings() {
        boolean peek = settings().getBoolean(PREF_PEEK, false);
        int key = listKey();
        String[] items = {
            "Kenar önizlemesi ve efektler: " + (peek ? "Açık" : "Kapalı"),
            "Uygulama listesi tuşu: " + (key == 0 ? "Atanmadı" : keyName(key)),
            "Uygulama listesini aç",
            "Fotoğraflar: " + (this.feed != null && this.feed.hasUnsplash() ? "Unsplash + NASA" : "sadece NASA (Unsplash anahtarı setup.sh ile eklenir)")
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Loop Home ayarları").setItems(items, (dialog, which) -> {
            if (which == 0) {
                settings().edit().putBoolean(PREF_PEEK, !peek).apply();
                this.grid.setPeek(!peek);
                showSettings();
            } else if (which == 1) {
                assignListKey();
            } else if (which == 2) {
                showAppList();
            }
        }).show();
    }

    private void assignListKey() {
        final int current = listKey();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Uygulama listesi tuşu")
            .setMessage("Kumandada atamak istediğin tuşa bas.\n\nŞu an: " + (current == 0 ? "atanmadı" : keyName(current))
                + "\n\nBasınca bir şey olmuyorsa o tuşu sistem kullanıyor demektir, başka bir tuş dene.")
            .setNegativeButton("İptal", null);
        if (current != 0) {
            builder.setNeutralButton("Kaldır", (dialog, which) -> saveListKey(0));
        }
        AlertDialog dialog = builder.create();
        // Fallback for when KeyService is off; ordinary keys still arrive here.
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (isReserved(keyCode)) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                keyCaptured(keyCode);
            }
            return true;
        });
        dialog.setOnDismissListener(d -> {
            assigningKey = false;
            this.keyDialog = null;
        });
        this.keyDialog = dialog;
        assigningKey = true;
        dialog.show();
    }

    /** Called by KeyService (or the dialog) with the key pressed while assigning. */
    static void keyCaptured(int keyCode) {
        MainActivity activity = current;
        if (activity == null || !assigningKey) {
            return;
        }
        assigningKey = false;
        activity.saveListKey(keyCode);
        if (activity.keyDialog != null) {
            activity.keyDialog.dismiss();
        }
    }

    private void saveListKey(int keyCode) {
        settings().edit().putInt(PREF_LIST_KEY, keyCode).apply();
        this.appList.setToggleKey(keyCode);
        Toast.makeText(this, keyCode == 0 ? "Liste tuşu kaldırıldı" : "Liste tuşu: " + keyName(keyCode), 1).show();
    }

    static boolean isReserved(int keyCode) {
        for (int reserved : RESERVED_KEYS) {
            if (reserved == keyCode) {
                return true;
            }
        }
        return false;
    }

    static String keyName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_PROG_RED:
                return "Kırmızı";
            case KeyEvent.KEYCODE_PROG_GREEN:
                return "Yeşil";
            case KeyEvent.KEYCODE_PROG_YELLOW:
                return "Sarı";
            case KeyEvent.KEYCODE_PROG_BLUE:
                return "Mavi";
            case KeyEvent.KEYCODE_GUIDE:
                return "Rehber";
            case KeyEvent.KEYCODE_CAPTIONS:
                return "Altyazı";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                return "Oynat/Duraklat";
            default:
                // The remote's app buttons (Netflix, Prime…) arrive as gamepad buttons.
                if (keyCode >= KeyEvent.KEYCODE_BUTTON_1 && keyCode <= KeyEvent.KEYCODE_BUTTON_16) {
                    return "Kumanda özel tuşu " + (keyCode - KeyEvent.KEYCODE_BUTTON_1 + 1);
                }
                String name = KeyEvent.keyCodeToString(keyCode);
                return name.startsWith("KEYCODE_") ? name.substring(8) : name;
        }
    }

    private void toggleAppList() {
        if (this.appList.isShowing()) {
            this.appList.hide();
        } else {
            showAppList();
        }
    }

    private void showAppList() {
        if (this.recents.getVisibility() == 0) {
            this.recents.hide();
        }
        ArrayList<LoopGrid.Tile> tiles = new ArrayList<>(this.tilesByPkg.values());
        Collator collator = Collator.getInstance(TR);
        tiles.sort((a, b) -> collator.compare(a.label, b.label));
        this.appList.show(tiles);
    }

    private void toggleAudio(final String str) {
        if (this.player != null && str.equals(this.playing)) {
            if (this.prepared) {
                if (this.player.isPlaying()) {
                    this.player.pause();
                    savePosition();
                    restartIdle();
                    TextView textView = this.nowPlaying;
                    textView.setText("❚❚  Duraklatıldı · " + String.valueOf(textView.getTag()));
                    return;
                }
                this.player.start();
                this.handler.removeCallbacks(this.idleWarn);
                TextView textView2 = this.nowPlaying;
                textView2.setText("♪  " + String.valueOf(textView2.getTag()));
                return;
            }
            return;
        }
        stopAudio();
        this.playing = str;
        askBlackout();
        this.nowPlaying.setTag("");
        this.nowPlaying.setText("…  Yükleniyor");
        this.nowPlaying.setVisibility(0);
        if (KANTAN.equals(str)) {
            String[] strArrPodcast = this.ticker.podcast();
            if (strArrPodcast == null) {
                stopAudio();
                Toast.makeText(this, "Günlük özet henüz yüklenmedi", 0).show();
                return;
            } else {
                play(str, strArrPodcast[0], strArrPodcast[1], Integer.parseInt(strArrPodcast[2]));
                return;
            }
        }
        final Podcasts.Show showById = Podcasts.byId(str);
        new Thread(() -> lambda_toggleAudio_9(showById, str)).start();
    }
    /* synthetic */ void lambda_toggleAudio_9(final Podcasts.Show show, final String str) {
        try {
            final Podcasts.Episode episodeLatest = Podcasts.latest(show);
            this.handler.post(() -> lambda_toggleAudio_7(str, show, episodeLatest));
        } catch (Exception e) {
            this.handler.post(() -> lambda_toggleAudio_8(show));
        }
    }
    /* synthetic */ void lambda_toggleAudio_7(String str, Podcasts.Show show, Podcasts.Episode episode) {
        play(str, show.name + ": " + episode.title, episode.url, episode.secs);
    }
    /* synthetic */ void lambda_toggleAudio_8(Podcasts.Show show) {
        stopAudio();
        Toast.makeText(this, show.name + " yüklenemedi", 0).show();
    }

    private static String between(String str, String str2, String str3, int r4) {
        int r5 = str.indexOf(str2, Math.max(0, r4)) + str2.length();
        return Html.fromHtml(str.substring(r5, str.indexOf(str3, r5)), 0).toString();
    }

    private void play(final String str, String title, final String str3, int r7) {
        if (str.equals(this.playing)) {
            if (r7 > 0) {
                title = title + " · " + Math.max(1, r7 / 60) + " dk";
            }
            final String str2 = title;
            this.nowPlaying.setTag(str2);
            this.nowPlaying.setText("…  Yükleniyor · " + str2);
            this.playingUrl = str3;
            this.prepared = false;
            MediaPlayer mediaPlayer = new MediaPlayer();
            this.player = mediaPlayer;
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(1).setContentType(1).build());
            try {
                this.player.setDataSource(str3);
                this.player.setOnPreparedListener((mediaPlayer2) -> lambda_play_10(str, str3, str2, mediaPlayer2));
                this.player.setOnCompletionListener((mediaPlayer2) -> lambda_play_11(str, mediaPlayer2));
                this.player.setOnErrorListener((mediaPlayer2, r3, r4) -> lambda_play_12(mediaPlayer2, r3, r4));
                this.player.prepareAsync();
            } catch (IOException e) {
                stopAudio();
            }
        }
    }
    /* synthetic */ void lambda_play_10(String str, String str2, String str3, MediaPlayer mediaPlayer) {
        this.prepared = true;
        getWindow().addFlags(128);
        int r3 = savedPosition(str, str2);
        if (r3 > 5000 && r3 < mediaPlayer.getDuration() - 15000) {
            mediaPlayer.seekTo(r3);
            Toast.makeText(this, "Kaldığın yerden: " + clockOf(r3), 0).show();
        }
        mediaPlayer.start();
        this.handler.removeCallbacks(this.idleWarn);
        hideSleepBanner();
        this.nowPlaying.setText("♪  " + str3);
        this.handler.postDelayed(this.savePeriodically, 10000L);
    }
    /* synthetic */ void lambda_play_11(String str, MediaPlayer mediaPlayer) {
        audioPrefs().edit().remove(str).apply();
        this.prepared = false;
        stopAudio();
    }
    /* synthetic */ boolean lambda_play_12(MediaPlayer mediaPlayer, int r2, int r3) {
        stopAudio();
        Toast.makeText(this, "Ses çalınamadı", 0).show();
        return true;
    }

    private SharedPreferences audioPrefs() {
        return getSharedPreferences("audio", 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void savePosition() {
        if (this.player == null || !this.prepared || this.playing == null || this.playingUrl == null) {
            return;
        }
        try {
            audioPrefs().edit().putString(this.playing, this.playingUrl + "|" + this.player.getCurrentPosition()).apply();
        } catch (IllegalStateException e) {
        }
    }

    private int savedPosition(String str, String str2) {
        String string = audioPrefs().getString(str, "");
        int r0 = string.lastIndexOf(124);
        if (r0 < 0 || !string.substring(0, r0).equals(str2)) {
            return 0;
        }
        try {
            return Integer.parseInt(string.substring(r0 + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String clockOf(int r2) {
        int r3 = r2 / 1000;
        return String.format(Locale.US, "%d:%02d", Integer.valueOf(r3 / 60), Integer.valueOf(r3 % 60));
    }

    private void stopAudio() {
        this.handler.removeCallbacks(this.savePeriodically);
        getWindow().clearFlags(128);
        FrameLayout frameLayout = this.blackout;
        if (frameLayout != null && frameLayout.getVisibility() == 0) {
            hideBlackout();
        }
        savePosition();
        this.prepared = false;
        this.playing = null;
        this.playingUrl = null;
        MediaPlayer mediaPlayer = this.player;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            this.player = null;
        }
        TextView textView = this.nowPlaying;
        if (textView != null) {
            textView.setVisibility(8);
        }
        if (this.sleepBanner != null) {
            restartIdle();
        }
    }

    private void askBlackout() {
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Ekranı karartayım mı?").setMessage("Ses çalmaya devam eder. Herhangi bir tuşla ekran geri gelir.").setPositiveButton("Karart", (dialogInterface, r3) -> lambda_askBlackout_13(dialogInterface, r3)).setNegativeButton("Açık kalsın", (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.show();
        alertDialogCreate.getButton(-1).requestFocus();
    }
    /* synthetic */ void lambda_askBlackout_13(DialogInterface dialogInterface, int r2) {
        showBlackout();
    }

    private void showBlackout() {
        if (this.playing == null) {
            return;
        }
        this.handler.removeCallbacks(this.rotate);
        this.ticker.stop();
        this.financeCard.stop();
        this.grid.sleepNow();
        this.blackout.setAlpha(0.0f);
        this.blackout.setVisibility(0);
        this.blackout.animate().alpha(1.0f).setDuration(600L).start();
        this.blackout.requestFocus();
        this.handler.post(this.drift);
    }

    private void hideBlackout() {
        this.handler.removeCallbacks(this.drift);
        this.blackout.animate().cancel();
        this.blackout.setVisibility(8);
        this.grid.requestFocus();
        this.grid.wake();
        this.ticker.start();
        this.financeCard.start();
        this.handler.removeCallbacks(this.rotate);
        this.handler.postDelayed(this.rotate, ROTATE_MS);
    }

    @Override // com.mustycetin.loophome.PhotoFeed.Callback
    public void onPhoto(Bitmap bitmap, String str, String str2) {
        final ImageView imageView = this.photoShown;
        ImageView imageView2 = this.photoA;
        if (imageView == imageView2) {
            imageView2 = this.photoB;
        }
        imageView2.animate().cancel();
        imageView2.setImageBitmap(bitmap);
        imageView2.setAlpha(0.0f);
        imageView2.setScaleX(1.0f);
        imageView2.setScaleY(1.0f);
        imageView2.bringToFront();
        imageView2.animate().alpha(1.0f).setDuration(1600L).withEndAction(new Runnable() { // from class: com.mustycetin.loophome.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.lambda$onPhoto$14(imageView);
            }
        }).start();
        imageView2.animate().scaleX(ZOOM).scaleY(ZOOM).setDuration(47000L).setInterpolator(new LinearInterpolator()).withLayer().start();
        this.photoShown = imageView2;
        this.grid.setBackdrop(bitmap);
        setCaption(str, str2);
    }

    static /* synthetic */ void lambda$onPhoto$14(ImageView imageView) {
        imageView.animate().cancel();
        imageView.setImageDrawable(null);
    }

    private void setCaption(String str, String str2) {
        this.captionKind.setText(str);
        this.captionTitle.setText(str2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showFinance(List<Finance.Row> list, String str) {
        this.financeCard.setRows(list, str);
    }

    private AudioManager audio() {
        return (AudioManager) getSystemService(AudioManager.class);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMute() {
        this.muteBadge.setVisibility(audio().isStreamMute(3) ? 0 : 8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNight() {
        float fNight = Sun.night(System.currentTimeMillis()) * 0.42f;
        if (Math.abs(this.night.getAlpha() - fNight) > 0.005f) {
            this.night.animate().alpha(fNight).setDuration(4000L).start();
        }
    }

    private void showGreeting() {
        Calendar calendar = Calendar.getInstance();
        this.greetText.setText("Hoş geldiniz");
        TextView textView = this.greetSub;
        Locale locale = TR;
        textView.setText(new SimpleDateFormat("d MMMM EEEE", locale).format(calendar.getTime()).toUpperCase(locale));
        this.greet.animate().cancel();
        this.greet.setAlpha(0.0f);
        this.greet.setVisibility(0);
        this.greet.animate().alpha(1.0f).setDuration(450L).withEndAction(() -> lambda_showGreeting_16()).start();
    }
    /* synthetic */ void lambda_showGreeting_16() {
        this.greet.animate().alpha(0.0f).setStartDelay(2400L).setDuration(900L).withEndAction(() -> lambda_showGreeting_15()).start();
    }
    /* synthetic */ void lambda_showGreeting_15() {
        this.greet.setVisibility(8);
        this.greet.animate().setStartDelay(0L);
    }

    private Bitmap moonArt() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        paint.setShader(new LinearGradient(0.0f, 0.0f, 256.0f, 256.0f, -13944198, -15722956, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(16.0f, 16.0f, 240.0f, 240.0f), 52.0f, 52.0f, paint);
        Paint paint2 = new Paint(1);
        paint2.setColor(-7512);
        canvas.drawCircle(128.0f, 128.0f, 62.0f, paint2);
        paint2.setShader(new LinearGradient(0.0f, 0.0f, 256.0f, 256.0f, -13944198, -15722956, Shader.TileMode.CLAMP));
        canvas.drawCircle(158.0f, 106.0f, 56.0f, paint2);
        return bitmapCreateBitmap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void chooseSleepTimer() {
        String[] strArr;
        int r0 = KeyService.timerMinutesLeft();
        if (r0 > 0) {
            strArr = new String[]{"30 dakika", "60 dakika", "90 dakika", "120 dakika", "Kapat (" + r0 + " dk kalmıştı)"};
        } else {
            strArr = new String[]{"30 dakika", "60 dakika", "90 dakika", "120 dakika"};
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("Uyku zamanlayıcısı").setItems(strArr, (dialogInterface, r3) -> lambda_chooseSleepTimer_17(dialogInterface, r3)).show();
    }
    /* synthetic */ void lambda_chooseSleepTimer_17(DialogInterface dialogInterface, int r4) {
        int r5 = r4 < 4 ? (r4 + 1) * 30 : 0;
        if (!KeyService.timer(r5)) {
            Toast.makeText(this, "Erişilebilirlik servisi kapalı", 1).show();
        } else {
            Toast.makeText(this, r5 > 0 ? r5 + " dakika sonra TV kapanacak" : "Uyku zamanlayıcısı kapatıldı", 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateClock() {
        Date date = new Date();
        Locale locale = TR;
        SpannableString spannableString = new SpannableString(new SimpleDateFormat("HH:mm", locale).format(date));
        spannableString.setSpan(new ForegroundColorSpan(ACCENT), 2, 3, 33);
        this.clock.setText(spannableString);
        this.day.setText(new SimpleDateFormat("EEEE", locale).format(date).toUpperCase(locale));
        this.date.setText(new SimpleDateFormat("d MMMM yyyy", locale).format(date));
    }

    private Typeface font(int r4) {
        try {
            return new Typeface.Builder(getAssets(), "fonts/outfit.ttf").setFontVariationSettings("'wght' " + r4).build();
        } catch (Exception e) {
            return Typeface.create("sans-serif", 0);
        }
    }

    private static GradientDrawable pill() {
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{-855672251, -855687826});
        gradientDrawable.setCornerRadius(40.0f);
        return gradientDrawable;
    }

    private ImageView photoView() {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }

    private TextView text(float f, int r4, Typeface typeface) {
        TextView textView = new TextView(this);
        textView.setTextSize(0, f);
        textView.setTextColor(r4);
        textView.setTypeface(typeface);
        textView.setIncludeFontPadding(false);
        textView.setShadowLayer(12.0f, 0.0f, 2.0f, -1728053248);
        return textView;
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(-1, -1);
    }
}
