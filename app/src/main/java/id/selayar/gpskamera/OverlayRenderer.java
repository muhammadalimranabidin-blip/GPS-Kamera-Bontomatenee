package id.selayar.gpskamera;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Menggambar cap GPS permanen dan pratinjau dengan susunan seperti aplikasi GPS Map Camera.
 * Mini map dibuat secara lokal dan hanya menampilkan satu label Google.
 */
public final class OverlayRenderer {

    private static final Locale LOCALE_ID = new Locale("id", "ID");
    private static final TimeZone WITA = TimeZone.getTimeZone("Asia/Makassar");

    private OverlayRenderer() {
    }

    public static final class OverlayData {
        public String village = "Tanete";
        public long selectedTimeMillis = System.currentTimeMillis();
        public long capturedAtMillis = System.currentTimeMillis();
        public double latitude = 0d;
        public double longitude = 0d;
        public float accuracyMeters = 0f;
        public String locationCode = "BTM-TNT-00000000-000000";
        public boolean hasLocation = false;
    }

    public static void draw(Canvas canvas, int width, int height, OverlayData data) {
        if (canvas == null || width <= 0 || height <= 0 || data == null) return;

        final float shortSide = Math.min(width, height);
        final float margin = clamp(shortSide * 0.026f, 14f, 44f);
        final float corner = clamp(shortSide * 0.018f, 12f, 30f);
        final float mapSize = clamp(Math.min(width * 0.20f, height * 0.26f), 150f, height * 0.42f);
        final float bottom = height - margin;
        final float mapLeft = margin;
        final float mapTop = bottom - mapSize;
        final RectF mapRect = new RectF(mapLeft, mapTop, mapLeft + mapSize, bottom);

        final float panelLeft = mapRect.right + margin * 0.45f;
        final float panelRight = width - margin;
        final float panelWidth = Math.max(240f, panelRight - panelLeft);

        final float titleSize = clamp(shortSide * 0.043f, 24f, 58f);
        final float bodySize = clamp(shortSide * 0.0305f, 18f, 42f);
        final float smallSize = clamp(shortSide * 0.026f, 16f, 36f);
        final float lineSpacing = bodySize * 1.26f;
        final float panelPadding = clamp(shortSide * 0.024f, 14f, 36f);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(Math.max(2f, shortSide * 0.003f), 1.2f, 1.2f, Color.BLACK);

        List<TextLine> lines = buildLines(data);
        float estimatedHeight = panelPadding * 1.7f + titleSize * 1.15f;
        for (int i = 1; i < lines.size(); i++) estimatedHeight += lineSpacing;
        estimatedHeight = Math.max(mapSize * 0.82f, estimatedHeight);
        estimatedHeight = Math.min(height * 0.42f, estimatedHeight);

        float panelTop = bottom - estimatedHeight;
        RectF panelRect = new RectF(panelLeft, panelTop, panelRight, bottom);

        Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelPaint.setColor(Color.argb(194, 10, 10, 10));
        canvas.drawRoundRect(panelRect, corner, corner, panelPaint);

        drawMiniMap(canvas, mapRect, corner, data);
        drawBadge(canvas, panelRect, margin, corner, smallSize);

        float textLeft = panelRect.left + panelPadding;
        float textRight = panelRect.right - panelPadding;
        float maxTextWidth = textRight - textLeft;
        float y = panelRect.top + panelPadding + titleSize;

        TextLine title = lines.get(0);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(fitTextSize(title.text, textPaint, maxTextWidth - titleSize * 0.9f, titleSize, smallSize));
        canvas.drawText(title.text, textLeft, y, textPaint);
        drawIndonesiaFlag(canvas, Math.min(textLeft + textPaint.measureText(title.text) + bodySize * 0.35f,
                textRight - bodySize * 1.25f), y - bodySize * 0.78f, bodySize * 1.15f, bodySize * 0.74f);
        y += lineSpacing * 1.13f;

        for (int i = 1; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            textPaint.setTypeface(line.bold
                    ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    : Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            textPaint.setTextSize(line.small ? smallSize : bodySize);
            float fitted = fitTextSize(line.text, textPaint, maxTextWidth, textPaint.getTextSize(), smallSize * 0.82f);
            textPaint.setTextSize(fitted);
            canvas.drawText(ellipsize(line.text, textPaint, maxTextWidth), textLeft, y, textPaint);
            y += lineSpacing;
            if (y > panelRect.bottom - panelPadding * 0.5f) break;
        }
    }

    private static List<TextLine> buildLines(OverlayData data) {
        String village = safe(data.village, "-");

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm 'WITA (GMT +08:00)'", LOCALE_ID);
        dateFormat.setTimeZone(WITA);

        List<TextLine> lines = new ArrayList<>();
        lines.add(new TextLine("Kecamatan Bontomatene, Sulawesi Selatan, Indonesia", false, false));
        lines.add(new TextLine("Desa " + village + ", Kec. Bontomatene, Kab. Kepulauan Selayar,", false, false));
        lines.add(new TextLine("Sulawesi Selatan, Indonesia", false, false));
        lines.add(new TextLine("Kode GPS: " + safe(data.locationCode, "-"), false, true));

        if (data.hasLocation) {
            lines.add(new TextLine(
                    "Lat " + String.format(Locale.US, "%.6f°", data.latitude)
                            + "  Long " + String.format(Locale.US, "%.6f°", data.longitude)
                            + "  |  Akurasi ±" + Math.round(data.accuracyMeters) + " m",
                    false,
                    false
            ));
        } else {
            lines.add(new TextLine("Lat menunggu GPS  |  Long menunggu GPS", false, false));
        }
        lines.add(new TextLine(dateFormat.format(data.selectedTimeMillis), false, false));
        return lines;
    }

    private static void drawBadge(Canvas canvas, RectF panelRect, float margin, float corner, float textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(textSize);
        String label = "GPS Map Camera";
        float iconSize = textSize * 1.5f;
        float badgePadding = textSize * 0.55f;
        float badgeWidth = paint.measureText(label) + iconSize + badgePadding * 3f;
        float badgeHeight = iconSize + badgePadding * 1.1f;
        RectF badge = new RectF(
                panelRect.right - badgeWidth,
                panelRect.top - badgeHeight - margin * 0.28f,
                panelRect.right,
                panelRect.top - margin * 0.28f
        );

        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setColor(Color.argb(205, 20, 20, 20));
        canvas.drawRoundRect(badge, corner * 0.78f, corner * 0.78f, background);

        float iconLeft = badge.left + badgePadding;
        float iconTop = badge.centerY() - iconSize * 0.5f;
        drawCameraPinIcon(canvas, new RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize));

        paint.setColor(Color.WHITE);
        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK);
        canvas.drawText(label, iconLeft + iconSize + badgePadding, badge.centerY() - (paint.ascent() + paint.descent()) / 2f, paint);
    }

    private static void drawCameraPinIcon(Canvas canvas, RectF rect) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(244, 193, 40));
        canvas.drawRoundRect(rect, rect.width() * 0.17f, rect.width() * 0.17f, p);

        p.setColor(Color.rgb(24, 112, 180));
        float cameraMargin = rect.width() * 0.17f;
        RectF camera = new RectF(rect.left + cameraMargin, rect.top + rect.height() * 0.30f,
                rect.right - cameraMargin, rect.bottom - cameraMargin);
        canvas.drawRoundRect(camera, rect.width() * 0.08f, rect.width() * 0.08f, p);

        p.setColor(Color.WHITE);
        canvas.drawCircle(camera.centerX(), camera.centerY(), rect.width() * 0.13f, p);
        p.setColor(Color.rgb(33, 74, 100));
        canvas.drawCircle(camera.centerX(), camera.centerY(), rect.width() * 0.075f, p);

        p.setColor(Color.rgb(220, 45, 112));
        canvas.drawCircle(rect.left + rect.width() * 0.35f, rect.top + rect.height() * 0.23f,
                rect.width() * 0.11f, p);
    }

    private static void drawMiniMap(Canvas canvas, RectF rect, float corner, OverlayData data) {
        int save = canvas.save();
        Path clipPath = new Path();
        clipPath.addRoundRect(rect, corner, corner, Path.Direction.CW);
        canvas.clipPath(clipPath);

        long seed = safe(data.village, "BTM").hashCode();
        if (data.hasLocation) {
            seed ^= Double.doubleToLongBits(Math.rint(data.latitude * 10000d) / 10000d);
            seed ^= Double.doubleToLongBits(Math.rint(data.longitude * 10000d) / 10000d);
        }
        Random random = new Random(seed);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(35, 72, 43));
        canvas.drawRect(rect, paint);

        // Petak hijau/cokelat bergaya citra satelit, seluruhnya dibuat lokal.
        for (int i = 0; i < 75; i++) {
            float x = rect.left + random.nextFloat() * rect.width();
            float y = rect.top + random.nextFloat() * rect.height();
            float w = rect.width() * (0.04f + random.nextFloat() * 0.16f);
            float h = rect.height() * (0.035f + random.nextFloat() * 0.13f);
            int choice = random.nextInt(5);
            if (choice == 0) paint.setColor(Color.rgb(73, 99, 49));
            else if (choice == 1) paint.setColor(Color.rgb(47, 85, 48));
            else if (choice == 2) paint.setColor(Color.rgb(98, 105, 57));
            else if (choice == 3) paint.setColor(Color.rgb(59, 73, 42));
            else paint.setColor(Color.rgb(81, 69, 47));
            canvas.save();
            canvas.rotate(random.nextFloat() * 24f - 12f, x + w / 2f, y + h / 2f);
            canvas.drawRect(x, y, x + w, y + h, paint);
            canvas.restore();
        }

        // Jalan utama dan jalan lokal.
        Paint roadEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadEdge.setStyle(Paint.Style.STROKE);
        roadEdge.setStrokeCap(Paint.Cap.ROUND);
        roadEdge.setStrokeJoin(Paint.Join.ROUND);
        roadEdge.setColor(Color.argb(175, 35, 35, 30));
        roadEdge.setStrokeWidth(rect.width() * 0.042f);

        Paint road = new Paint(roadEdge);
        road.setColor(Color.rgb(205, 187, 143));
        road.setStrokeWidth(rect.width() * 0.024f);

        Path mainRoad = new Path();
        mainRoad.moveTo(rect.left - rect.width() * 0.1f, rect.bottom - rect.height() * 0.15f);
        mainRoad.cubicTo(rect.left + rect.width() * 0.22f, rect.top + rect.height() * 0.72f,
                rect.left + rect.width() * 0.46f, rect.top + rect.height() * 0.40f,
                rect.right + rect.width() * 0.1f, rect.top + rect.height() * 0.24f);
        canvas.drawPath(mainRoad, roadEdge);
        canvas.drawPath(mainRoad, road);

        Paint minorRoad = new Paint(road);
        minorRoad.setStrokeWidth(rect.width() * 0.011f);
        minorRoad.setColor(Color.rgb(190, 180, 153));
        for (int i = 0; i < 7; i++) {
            float sx = rect.left + random.nextFloat() * rect.width();
            float sy = rect.top + random.nextFloat() * rect.height();
            Path p = new Path();
            p.moveTo(sx, sy);
            p.quadTo(rect.centerX() + (random.nextFloat() - 0.5f) * rect.width() * 0.5f,
                    rect.centerY() + (random.nextFloat() - 0.5f) * rect.height() * 0.5f,
                    rect.left + random.nextFloat() * rect.width(),
                    rect.top + random.nextFloat() * rect.height());
            canvas.drawPath(p, minorRoad);
        }

        // Bangunan kecil.
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 40; i++) {
            float bw = rect.width() * (0.012f + random.nextFloat() * 0.02f);
            float bh = rect.height() * (0.012f + random.nextFloat() * 0.02f);
            float bx = rect.left + random.nextFloat() * (rect.width() - bw);
            float by = rect.top + random.nextFloat() * (rect.height() - bh);
            paint.setColor(random.nextBoolean() ? Color.rgb(185, 174, 142) : Color.rgb(139, 120, 91));
            canvas.drawRect(bx, by, bx + bw, by + bh, paint);
        }

        // Pin lokasi warna magenta seperti contoh.
        float pinX = rect.centerX();
        float pinY = rect.centerY() - rect.height() * 0.06f;
        float pinRadius = rect.width() * 0.072f;
        Paint pin = new Paint(Paint.ANTI_ALIAS_FLAG);
        pin.setColor(Color.argb(110, 0, 0, 0));
        canvas.drawCircle(pinX + pinRadius * 0.10f, pinY + pinRadius * 0.17f, pinRadius * 1.28f, pin);

        pin.setColor(Color.rgb(213, 47, 126));
        Path marker = new Path();
        marker.moveTo(pinX, pinY + pinRadius * 2.05f);
        marker.cubicTo(pinX - pinRadius * 0.42f, pinY + pinRadius * 1.28f,
                pinX - pinRadius, pinY + pinRadius * 0.55f,
                pinX - pinRadius, pinY);
        marker.arcTo(new RectF(pinX - pinRadius, pinY - pinRadius, pinX + pinRadius, pinY + pinRadius),
                180f, 180f, false);
        marker.cubicTo(pinX + pinRadius, pinY + pinRadius * 0.55f,
                pinX + pinRadius * 0.42f, pinY + pinRadius * 1.28f,
                pinX, pinY + pinRadius * 2.05f);
        marker.close();
        canvas.drawPath(marker, pin);
        pin.setColor(Color.rgb(72, 54, 91));
        canvas.drawCircle(pinX, pinY, pinRadius * 0.43f, pin);

        Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        shade.setColor(Color.argb(65, 0, 0, 0));
        canvas.drawRect(rect.left, rect.bottom - rect.height() * 0.22f, rect.right, rect.bottom, shade);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        label.setColor(Color.WHITE);
        label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        label.setShadowLayer(4f, 1f, 1f, Color.BLACK);
        label.setTextSize(rect.width() * 0.105f);
        canvas.drawText("Google", rect.left + rect.width() * 0.06f,
                rect.bottom - rect.height() * 0.055f, label);

        canvas.restoreToCount(save);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(2f, rect.width() * 0.008f));
        border.setColor(Color.argb(160, 255, 255, 255));
        canvas.drawRoundRect(rect, corner, corner, border);
    }

    private static void drawIndonesiaFlag(Canvas canvas, float left, float top, float width, float height) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF flag = new RectF(left, top, left + width, top + height);
        p.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRoundRect(new RectF(flag.left + 2f, flag.top + 2f, flag.right + 2f, flag.bottom + 2f),
                height * 0.12f, height * 0.12f, p);
        p.setColor(Color.rgb(225, 35, 45));
        canvas.drawRect(flag.left, flag.top, flag.right, flag.centerY(), p);
        p.setColor(Color.WHITE);
        canvas.drawRect(flag.left, flag.centerY(), flag.right, flag.bottom, p);
    }

    private static float fitTextSize(String text, Paint paint, float maxWidth, float preferred, float minimum) {
        float size = preferred;
        paint.setTextSize(size);
        while (size > minimum && paint.measureText(text) > maxWidth) {
            size -= 1f;
            paint.setTextSize(size);
        }
        return size;
    }

    private static String ellipsize(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 1 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(1, end)) + suffix;
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class TextLine {
        final String text;
        final boolean bold;
        final boolean small;

        TextLine(String text, boolean bold, boolean small) {
            this.text = text;
            this.bold = bold;
            this.small = small;
        }
    }
}
