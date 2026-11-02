package com.mustycetin.loophome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Podcasts {
    static final String KANTAN = "kantan.gundem";
    static final String BBC_6MIN = "bbc.6minute";
    static final Show[] ALL = {new Show(KANTAN, "Kantan Gündem", "Türkçe · günlük teknoloji özeti", "", "", false), new Show("aposto", "Aposto Altı Otuz", "Türkçe · sabah gündem özeti", "https://feeds.megaphone.fm/PFC3270786384", "https://feeds.megaphone.fm/PFC3270786384", false), new Show("111hz", "Barış Özcan ile 111 Hz", "Türkçe · teknoloji ve fikir", "https://feeds.megaphone.fm/PM8722895261", "https://feeds.megaphone.fm/PM8722895261", false), new Show("tesaduf", "Hiçbir Şey Tesadüf Değil", "Türkçe · bilim ve fizik", "https://feeds.megaphone.fm/PM7940137763", "https://feeds.megaphone.fm/PM7940137763", false), new Show("fularsiz", "Fularsız Entellik", "Türkçe · kültür ve tarih", "https://feeds.megaphone.fm/PM5261345791", "https://feeds.megaphone.fm/PM5261345791", false), new Show("acaib", "Tarihte Acaib ve Garaib", "Türkçe · tarihin tuhaf hikâyeleri", "https://feeds.megaphone.fm/PM2572913684", "https://feeds.megaphone.fm/PM2572913684", false), new Show(BBC_6MIN, "6 Minute English", "İngilizce · BBC, reklamsız", "/learningenglish/english/features/6-minute-english", "https://podcasts.files.bbci.co.uk/p02pc9tn.rss", true), new Show("bbc.tews", "The English We Speak", "İngilizce · BBC deyimler, reklamsız", "/learningenglish/english/features/the-english-we-speak", "https://podcasts.files.bbci.co.uk/p02pc9zn.rss", true), new Show("nasa", "NASA's Curious Universe", "İngilizce · uzay ve bilim", "https://feeds.megaphone.fm/NATIONALAERONAUTICSANDSPACEADMINISTRATION8162188566", "https://feeds.megaphone.fm/NATIONALAERONAUTICSANDSPACEADMINISTRATION8162188566", false)};

    static final class Show {
        final String artFeed;
        final boolean bbcPage;
        final String id;
        final String name;
        final String source;
        final String sub;

        Show(String str, String str2, String str3, String str4, String str5, boolean z) {
            this.id = str;
            this.name = str2;
            this.sub = str3;
            this.source = str4;
            this.artFeed = str5;
            this.bbcPage = z;
        }
    }

    static final class Episode {
        final int secs;
        final String title;
        final String url;

        Episode(String str, String str2, int r3) {
            this.title = str;
            this.url = str2;
            this.secs = r3;
        }
    }

    private Podcasts() {
    }

    static Show byId(String str) {
        for (Show show : ALL) {
            if (show.id.equals(str)) {
                return show;
            }
        }
        return null;
    }

    static Episode latest(Show show) throws IOException {
        if (show.bbcPage) {
            Matcher matcher = Pattern.compile("href=\"(" + Pattern.quote(show.source) + "_[^\"]+/ep-\\d+)\"").matcher(Net.text("https://www.bbc.co.uk" + show.source));
            if (!matcher.find()) {
                throw new IOException("no episode");
            }
            String strText = Net.text("https://www.bbc.co.uk" + matcher.group(1));
            Matcher matcher2 = Pattern.compile("https?://downloads\\.bbc\\.co\\.uk/[^\"]+\\.mp3").matcher(strText);
            if (!matcher2.find()) {
                throw new IOException("no mp3");
            }
            return new Episode(unescape(between(strText, "<title>", "</title>", 0)).replaceFirst("^.*?/\\s*", ""), matcher2.group().replace("http://", "https://").replace("'", "%27"), show.id.equals(BBC_6MIN) ? 360 : 180);
        }
        String strText2 = Net.text(show.source);
        int r0 = strText2.indexOf("<item");
        if (r0 < 0) {
            throw new IOException("empty feed");
        }
        String strUnescape = unescape(between(strText2, "<title>", "</title>", r0));
        Matcher matcher3 = Pattern.compile("<enclosure[^>]*url=\"([^\"]+)\"").matcher(strText2);
        if (!matcher3.find(r0)) {
            throw new IOException("no enclosure");
        }
        return new Episode(strUnescape, matcher3.group(1).replace("&amp;", "&").replace("http://", "https://"), seconds(between(strText2, "<itunes:duration>", "</itunes:duration>", r0)));
    }

    static Bitmap art(Show show) {
        if (show.artFeed.isEmpty()) {
            return null;
        }
        try {
            Matcher matcher = Pattern.compile("<itunes:image[^>]*href=\"([^\"]+)\"").matcher(Net.text(show.artFeed));
            if (!matcher.find()) {
                return null;
            }
            byte[] bArr = Net.get(matcher.group(1).replace("http://", "https://"), 6291456);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
            int r3 = 1;
            while (true) {
                int r6 = r3 * 2;
                if (options.outWidth / r6 < 256) {
                    break;
                }
                r3 = r6;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = r3;
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options2);
            if (bitmapDecodeByteArray == null) {
                return null;
            }
            return Bitmap.createScaledBitmap(bitmapDecodeByteArray, 256, 256, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static int seconds(String str) {
        try {
            String strTrim = str.trim();
            if (!strTrim.contains(":")) {
                return (int) Double.parseDouble(strTrim);
            }
            int r3 = 0;
            for (String str2 : strTrim.split(":")) {
                r3 = (r3 * 60) + Integer.parseInt(str2.trim());
            }
            return r3;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String between(String str, String str2, String str3, int r4) {
        int length;
        int r2;
        int r5 = str.indexOf(str2, Math.max(0, r4));
        return (r5 >= 0 && (r2 = str.indexOf(str3, (length = r5 + str2.length()))) >= 0) ? str.substring(length, r2) : "";
    }

    private static String unescape(String str) {
        return Html.fromHtml(Html.fromHtml(str.replace("<![CDATA[", "").replace("]]>", ""), 0).toString(), 0).toString().trim();
    }
}
