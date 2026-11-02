package com.mustycetin.loophome;

import java.util.Calendar;
import java.util.TimeZone;

final class Sun {
    private static final long FADE_MS = 2700000;
    // Night mode follows sunset here; setup.sh sets the user's city (default: Ankara).
    private static double LAT = 39.9334d;
    private static double LON = 32.8597d;
    private static final double ZENITH = 90.833d;

    private Sun() {
    }

    static void setLocation(double lat, double lon) {
        LAT = lat;
        LON = lon;
    }

    static float night(long j) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        long jEvent = event(calendar, true);
        long jEvent2 = event(calendar, false);
        if (jEvent < 0 || jEvent2 < 0) {
            return 0.0f;
        }
        if (j >= FADE_MS + jEvent && j <= jEvent2) {
            return 0.0f;
        }
        if (j > jEvent2) {
            return Math.min(1.0f, (j - jEvent2) / 2700000.0f);
        }
        if (j < jEvent) {
            return 1.0f;
        }
        return 1.0f - ((j - jEvent) / 2700000.0f);
    }

    static long event(Calendar calendar, boolean z) {
        double d = ((double) calendar.get(6)) + ((((double) (z ? 6 : 18)) - (LON / 15.0d)) / 24.0d);
        double d2 = (0.9856d * d) - 3.289d;
        double dNorm = norm((sin(d2) * 1.916d) + d2 + (sin(d2 * 2.0d) * 0.02d) + 282.634d, 360.0d);
        double dNorm2 = norm(Math.toDegrees(Math.atan(Math.tan(Math.toRadians(dNorm)) * 0.91764d)), 360.0d);
        double dFloor = (dNorm2 + ((Math.floor(dNorm / 90.0d) * 90.0d) - (Math.floor(dNorm2 / 90.0d) * 90.0d))) / 15.0d;
        double dSin = sin(dNorm) * 0.39782d;
        double dCos = (cos(ZENITH) - (dSin * sin(LAT))) / (Math.cos(Math.asin(dSin)) * cos(LAT));
        if (dCos > 1.0d || dCos < -1.0d) {
            return -1L;
        }
        double dNorm3 = norm((((((z ? 360.0d - Math.toDegrees(Math.acos(dCos)) : Math.toDegrees(Math.acos(dCos))) / 15.0d) + dFloor) - (d * 0.06571d)) - 6.622d) - (LON / 15.0d), 24.0d);
        Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar2.clear();
        calendar2.set(calendar.get(1), calendar.get(2), calendar.get(5));
        long timeInMillis = calendar2.getTimeInMillis() + ((long) (dNorm3 * 3600000.0d));
        Calendar calendar3 = Calendar.getInstance();
        calendar3.setTimeInMillis(timeInMillis);
        int r0 = calendar.get(6) - calendar3.get(6);
        if (r0 == 1 || r0 < -300) {
            return timeInMillis + 86400000;
        }
        if (r0 == -1 || r0 > 300) {
            return timeInMillis - 86400000;
        }
        return timeInMillis;
    }

    private static double sin(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double cos(double d) {
        return Math.cos(Math.toRadians(d));
    }

    private static double norm(double d, double d2) {
        double d3 = d % d2;
        return d3 < 0.0d ? d3 + d2 : d3;
    }
}
