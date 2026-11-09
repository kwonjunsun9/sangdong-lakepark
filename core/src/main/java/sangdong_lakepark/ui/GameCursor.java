package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;

/** The same glove and fingertip hotspot are used in both native cursor states. */
public final class GameCursor implements Disposable {
    private static final int SIZE = 64;
    private static final int SCALE = 4;
    private final Cursor normal, pressed;
    private boolean showingPress;

    public GameCursor() {
        Pixmap source = new Pixmap(Gdx.files.classpath("sangdong/cursor/glove.png"));
        Pixmap hand = new Pixmap(SIZE * SCALE, SIZE * SCALE, Pixmap.Format.RGBA8888);
        Pixmap idle = null, clicked = null;
        try {
            // Ignore nearly transparent padding when fitting the asset into the cursor.
            int left = source.getWidth(), top = source.getHeight(), right = 0, bottom = 0;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    if ((source.getPixel(x, y) & 0xff) < 128) continue;
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
            int height = 44 * SCALE;
            int width = Math.round(height * (right - left + 1f) / (bottom - top + 1));
            hand.setFilter(Pixmap.Filter.BiLinear);
            hand.drawPixmap(source, left, top, right - left + 1, bottom - top + 1,
                14 * SCALE, 20 * SCALE, width, height);
            idle = downsample(hand);

            // The uppermost opaque point is the tip of the raised index finger.
            int hotspotX = 0, hotspotY = 0;
            for (int y = 0; y < SIZE; y++) {
                int sum = 0, count = 0;
                for (int x = 0; x < SIZE; x++) {
                    if ((idle.getPixel(x, y) & 0xff) >= 128) { sum += x; count++; }
                }
                if (count > 0) {
                    hotspotX = Math.round((float) sum / count);
                    hotspotY = y;
                    break;
                }
            }
            drawClickRays(hand, hotspotX * SCALE, hotspotY * SCALE);
            clicked = downsample(hand);
            normal = Gdx.graphics.newCursor(idle, hotspotX, hotspotY);
            pressed = Gdx.graphics.newCursor(clicked, hotspotX, hotspotY);
        } finally {
            source.dispose();
            hand.dispose();
            if (idle != null) idle.dispose();
            if (clicked != null) clicked.dispose();
        }
        Gdx.graphics.setCursor(normal);
    }

    private static Pixmap downsample(Pixmap source) {
        Pixmap result = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        result.setFilter(Pixmap.Filter.BiLinear);
        result.setBlending(Pixmap.Blending.None);
        result.drawPixmap(source, 0, 0, source.getWidth(), source.getHeight(), 0, 0, SIZE, SIZE);
        return result;
    }

    private static void drawClickRays(Pixmap target, int x, int y) {
        for (int degrees = -190; degrees <= -25; degrees += 33) {
            double angle = Math.toRadians(degrees);
            float dx = (float) Math.cos(angle), dy = (float) Math.sin(angle);
            float x1 = x + dx * 10 * SCALE, y1 = y + dy * 10 * SCALE;
            float x2 = x + dx * 17 * SCALE, y2 = y + dy * 17 * SCALE;
            target.setColor(0x61351fff);
            ray(target, x1, y1, x2, y2, dx, dy, 1.6f * SCALE, 2.5f * SCALE);
            target.setColor(0xffd95bff);
            ray(target, x1, y1, x2, y2, dx, dy, .65f * SCALE, 1.45f * SCALE);
        }
    }

    private static void ray(Pixmap target, float x1, float y1, float x2, float y2,
                            float dx, float dy, float nearWidth, float farWidth) {
        int ax = Math.round(x1 - dy * nearWidth), ay = Math.round(y1 + dx * nearWidth);
        int bx = Math.round(x1 + dy * nearWidth), by = Math.round(y1 - dx * nearWidth);
        int cx = Math.round(x2 + dy * farWidth), cy = Math.round(y2 - dx * farWidth);
        int ex = Math.round(x2 - dy * farWidth), ey = Math.round(y2 + dx * farWidth);
        target.fillTriangle(ax, ay, bx, by, cx, cy);
        target.fillTriangle(ax, ay, cx, cy, ex, ey);
        target.fillCircle(Math.round(x1), Math.round(y1), Math.round(nearWidth));
        target.fillCircle(Math.round(x2), Math.round(y2), Math.round(farWidth));
    }

    public void update() {
        boolean down = Gdx.input.isButtonPressed(Input.Buttons.LEFT)
            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (down == showingPress) return;
        showingPress = down;
        Gdx.graphics.setCursor(down ? pressed : normal);
    }

    @Override
    public void dispose() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        normal.dispose();
        pressed.dispose();
    }
}
