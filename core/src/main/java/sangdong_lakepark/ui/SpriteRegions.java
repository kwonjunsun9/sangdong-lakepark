package sangdong_lakepark.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Ignore transparent padding and faint export noise when sizing a drawable sprite. */
final class SpriteRegions {
    private SpriteRegions() { }

    static TextureRegion crop(Pixmap source, Texture texture, int x0, int y0, int width, int height) {
        int left = x0 + width, top = y0 + height, right = x0, bottom = y0;
        for (int y = y0; y < y0 + height; y++) for (int x = x0; x < x0 + width; x++) {
            if ((source.getPixel(x, y) & 255) < 32) continue;
            left = Math.min(left, x); top = Math.min(top, y);
            right = Math.max(right, x); bottom = Math.max(bottom, y);
        }
        if (left > right || top > bottom) throw new IllegalStateException("Empty sprite cell");
        return new TextureRegion(texture, left, top, right - left + 1, bottom - top + 1);
    }
}
