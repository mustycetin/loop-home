package com.mustycetin.loophome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class PhotoFeed {
    private static final String COMMONS = "https://commons.wikimedia.org/w/api.php";
    private static final String[][] DECK = {new String[]{"HAYVANLAR", "3"}, new String[]{"MANZARA", "3"}, new String[]{"UZAY", "2"}, new String[]{"PORTRE", "2"}, new String[]{"MİMARİ", "1"}, new String[]{"ÇİÇEKLER", "1"}, new String[]{"OTOMOBİL", "1"}};
    private static final int MIN_H = 1080;
    private static final int MIN_W = 1920;
    private static final Pattern NOT_SPACE;
    private static final String[] SPACE_CATS;
    private static final Map<String, String[]> UNSPLASH;
    private static final int WANT_W = 2400;
    private volatile boolean busy;
    private final Context ctx;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final Map<String, List<String>> titleCache = new HashMap();
    private final Map<String, List<String[]>> queue = new HashMap();
    private final List<String> deck = new ArrayList();
    private String lastKind = "";
    private final String unsplashKey;

    public interface Callback {
        void onPhoto(Bitmap bitmap, String str, String str2);
    }

    static {
        HashMap map = new HashMap();
        UNSPLASH = map;
        map.put("HAYVANLAR", new String[]{"wildlife", "animal portrait", "wild animals nature", "birds wildlife"});
        map.put("MANZARA", new String[]{"landscape", "mountains landscape", "ocean coast", "nature scenery", "aerial landscape"});
        map.put("PORTRE", new String[]{"woman portrait fashion", "elegant woman portrait", "fashion model editorial", "woman golden hour portrait"});
        map.put("MİMARİ", new String[]{"architecture", "modern architecture", "historic architecture"});
        map.put("ÇİÇEKLER", new String[]{"flowers", "flower macro", "blossom"});
        map.put("OTOMOBİL", new String[]{"sports car", "classic car", "supercar"});
        SPACE_CATS = new String[]{"Featured pictures of galaxies", "Featured pictures of the Moon", "Featured pictures of planets", "Featured pictures of astronomy"};
        NOT_SPACE = Pattern.compile("(?i)sunset|sunrise|sonnenuntergang|sonnenaufgang|dawn|dusk|landscape|naturschutz|pavement|beach|lake|field|rainbow|cloud|halo|twilight|evening|morning");
    }

    public PhotoFeed(Context context) {
        this.ctx = context.getApplicationContext();
        this.unsplashKey = readKey();
    }

    public void next(final Callback callback) {
        if (this.busy) {
            return;
        }
        this.busy = true;
        this.ex.execute(() -> lambda_next_1(callback));
    }
    /* synthetic */ void lambda_next_1(final Callback callback) {
        try {
            for (int attempt = 0; attempt < 6; attempt++) {
                try {
                    final String kind = drawCard();
                    final String[] pick = pick(kind);
                    if (pick == null) {
                        continue;
                    }
                    byte[] data = Net.get(pick[0], 14680064);
                    final Bitmap bmp = decode(data);
                    if (bmp == null || bmp.getWidth() < MIN_W || bmp.getHeight() < MIN_H) {
                        continue;
                    }
                    save(data, kind, pick[1]);
                    this.lastKind = kind;
                    if (pick.length > 2 && pick[2] != null) {
                        trackUnsplashDownload(pick[2]);
                    }
                    this.main.post(() -> callback.onPhoto(bmp, kind, pick[1]));
                    return;
                } catch (Exception e) {
                }
            }
        } finally {
            this.busy = false;
        }
    }

    private String drawCard() {
        if (this.deck.isEmpty()) {
            for (String[] strArr : DECK) {
                if ("UZAY".equals(strArr[0]) || !this.unsplashKey.isEmpty()) {
                    for (int r4 = 0; r4 < Integer.parseInt(strArr[1]); r4++) {
                        this.deck.add(strArr[0]);
                    }
                }
            }
            if (this.deck.isEmpty()) {
                this.deck.add("UZAY");
            }
            Collections.shuffle(this.deck, this.rnd);
        }
        for (int r0 = 0; r0 < this.deck.size(); r0++) {
            if (!this.deck.get(r0).equals(this.lastKind)) {
                return this.deck.remove(r0);
            }
        }
        return this.deck.remove(0);
    }

    private String[] pick(String str) throws Exception {
        return "UZAY".equals(str) ? space() : unsplash(str);
    }

    private String[] unsplash(String str) throws Exception {
        List<String[]> arrayList = this.queue.get(str);
        if (arrayList == null || arrayList.isEmpty()) {
            arrayList = new ArrayList<>();
            String[] strArr = UNSPLASH.get(str);
            JSONArray jSONArray = new JSONArray(Net.text("https://api.unsplash.com/photos/random?count=30&orientation=landscape&content_filter=high&query=" + Net.enc(strArr[this.rnd.nextInt(strArr.length)]) + "&client_id=" + Net.enc(this.unsplashKey)));
            for (int r1 = 0; r1 < jSONArray.length(); r1++) {
                JSONObject jSONObject = jSONArray.getJSONObject(r1);
                if (jSONObject.optInt("width") >= MIN_W && jSONObject.optInt("height") >= MIN_H && jSONObject.optInt("width") >= jSONObject.optInt("height") * 1.3f) {
                    String str2 = jSONObject.getJSONObject("urls").getString("raw") + "&w=2400&q=85&fm=jpg";
                    String strOptString = jSONObject.getJSONObject("user").optString("name");
                    String str3 = "";
                    String strOptString2 = jSONObject.optString("alt_description", "");
                    if (!strOptString2.isEmpty() && !"null".equals(strOptString2)) {
                        str3 = "  ·  " + cap(strOptString2);
                    }
                    arrayList.add(new String[]{str2, strOptString + " / Unsplash" + str3, jSONObject.getJSONObject("links").optString("download_location")});
                }
            }
            this.queue.put(str, arrayList);
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList.remove(arrayList.size() - 1);
    }

    private void trackUnsplashDownload(String str) {
        if (str.isEmpty()) {
            return;
        }
        try {
            Net.get(str + (str.contains("?") ? "&" : "?") + "client_id=" + Net.enc(this.unsplashKey), 65536);
        } catch (Exception e) {
        }
    }

    private static String cap(String str) {
        if (str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private String[] space() throws Exception {
        JSONArray jSONArrayOptJSONArray;
        String[] strArr = SPACE_CATS;
        String str = strArr[this.rnd.nextInt(strArr.length)];
        List<String> arrayList = this.titleCache.get(str);
        if (arrayList == null || arrayList.isEmpty()) {
            arrayList = new ArrayList<>();
            JSONArray jSONArray = new JSONObject(Net.text("https://commons.wikimedia.org/w/api.php?action=query&format=json&list=search&srnamespace=6&srlimit=300&srsearch=" + Net.enc("deepcat:\"" + str + "\" filew:>1919 fileh:>1079"))).getJSONObject("query").getJSONArray("search");
            for (int r5 = 0; r5 < jSONArray.length(); r5++) {
                arrayList.add(jSONArray.getJSONObject(r5).getString("title"));
            }
            Collections.shuffle(arrayList, this.rnd);
            this.titleCache.put(str, arrayList);
        }
        while (!arrayList.isEmpty()) {
            String strRemove = arrayList.remove(arrayList.size() - 1);
            if (!NOT_SPACE.matcher(strRemove).find()) {
                JSONObject jSONObject = new JSONObject(Net.text("https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url%7Csize%7Cmime&iiurlwidth=2400&titles=" + Net.enc(strRemove))).getJSONObject("query").getJSONObject("pages");
                Iterator<String> itKeys = jSONObject.keys();
                if (itKeys.hasNext() && (jSONArrayOptJSONArray = jSONObject.getJSONObject(itKeys.next()).optJSONArray("imageinfo")) != null && jSONArrayOptJSONArray.length() != 0) {
                    JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(0);
                    int r6 = jSONObject2.optInt("width");
                    int r7 = jSONObject2.optInt("height");
                    if (r6 >= MIN_W && r7 >= MIN_H && r6 >= r7 * 1.2f && jSONObject2.optString("mime").startsWith("image/jpeg")) {
                        return new String[]{jSONObject2.optString("thumburl", jSONObject2.optString("url")), strRemove.replaceFirst("^File:", "").replaceFirst("\\.[A-Za-z]+$", "").replace('_', ' ')};
                    }
                }
            }
        }
        return null;
    }

    private File cacheFile() {
        return new File(this.ctx.getFilesDir(), "last.jpg");
    }

    private SharedPreferences prefs() {
        return this.ctx.getSharedPreferences("photo", 0);
    }

    private void save(byte[] bArr, String str, String str2) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(cacheFile());
            try {
                fileOutputStream.write(bArr);
                prefs().edit().putString("kind", str).putString("title", str2).apply();
                fileOutputStream.close();
            } catch (Throwable th) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
        }
    }

    public Bitmap loadCached() {
        try {
            File fileCacheFile = cacheFile();
            if (fileCacheFile.exists()) {
                return decode(Files.readAllBytes(fileCacheFile.toPath()));
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public String cachedKind() {
        return prefs().getString("kind", "");
    }

    public String cachedTitle() {
        return prefs().getString("title", "");
    }

    boolean hasUnsplash() {
        return !this.unsplashKey.isEmpty();
    }

    private String readKey() {
        // Set on the device by setup.sh (MainActivity ACTION_CONFIG); a key built into the APK is the fallback.
        String saved = this.ctx.getSharedPreferences("settings", 0).getString(MainActivity.PREF_UNSPLASH_KEY, "").trim();
        if (!saved.isEmpty()) {
            return saved;
        }
        try {
            InputStream inputStreamOpen = this.ctx.getAssets().open("config.properties");
            try {
                Properties properties = new Properties();
                properties.load(inputStreamOpen);
                String strTrim = properties.getProperty("unsplash_key", "").trim();
                if (inputStreamOpen != null) {
                    inputStreamOpen.close();
                }
                return strTrim;
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
        } catch (IOException e) {
            return "";
        }
    }

    static Bitmap decode(byte[] bArr) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }
        int r2 = 1;
        while (true) {
            int r5 = r2 * 2;
            if (options.outWidth / r5 < MIN_W || options.outHeight / r5 < MIN_H) {
                break;
            }
            r2 = r5;
        }
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = r2;
        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options2);
        if (bitmapDecodeByteArray != null && bitmapDecodeByteArray.getWidth() > 2560) {
            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapDecodeByteArray, 2560, Math.round((bitmapDecodeByteArray.getHeight() * 2560.0f) / bitmapDecodeByteArray.getWidth()), true);
            bitmapDecodeByteArray.recycle();
            return bitmapCreateScaledBitmap;
        }
        return bitmapDecodeByteArray;
    }
}
