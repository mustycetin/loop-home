package com.mustycetin.loophome;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class Net {
    static final String UA = "LoopHome/0.2 (Android TV personal launcher)";

    private Net() {
    }

    static byte[] get(String str, int r7) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(20000);
        httpURLConnection.setRequestProperty("User-Agent", UA);
        try {
            if (httpURLConnection.getResponseCode() != 200) {
                throw new IOException("HTTP " + httpURLConnection.getResponseCode());
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] bArr = new byte[16384];
                int r4 = 0;
                while (true) {
                    int r5 = bufferedInputStream.read(bArr);
                    if (r5 > 0) {
                        r4 += r5;
                        if (r4 > r7) {
                            throw new IOException("too large");
                        }
                        byteArrayOutputStream.write(bArr, 0, r5);
                    } else {
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        bufferedInputStream.close();
                        httpURLConnection.disconnect();
                        return byteArray;
                    }
                }
            } catch (Throwable th) {
                try {
                    bufferedInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Throwable th3) {
            httpURLConnection.disconnect();
            throw th3;
        }
    }

    static String text(String str) throws IOException {
        return new String(get(str, 8388608), StandardCharsets.UTF_8);
    }

    static String enc(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
        } catch (IOException e) {
            return str;
        }
    }
}
