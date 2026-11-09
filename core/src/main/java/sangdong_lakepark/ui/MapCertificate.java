package sangdong_lakepark.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;

/** The supplied certificate, with its white export margin removed outside the closed outline. */
public final class MapCertificate implements Disposable {
    private final Texture texture;
    private final TextureRegion card;

    public MapCertificate(FileHandle file) {
        Pixmap pixels = new Pixmap(file);
        try {
            removeExportMargin(pixels);
            texture = new Texture(pixels, true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            card = SpriteRegions.crop(pixels, texture, 0, 0, pixels.getWidth(), pixels.getHeight());
        } finally {
            pixels.dispose();
        }
    }

    public Image image(float width) {
        Image image = new Image(card);
        image.setName("map-certificate");
        image.setSize(width, width * card.getRegionHeight() / card.getRegionWidth());
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private static void removeExportMargin(Pixmap pixels) {
        // Flood only connected white outside the brown border; the card face stays intact.
        pixels.setBlending(Pixmap.Blending.None);
        int width = pixels.getWidth(), height = pixels.getHeight();
        IntArray pending = new IntArray();
        pending.add(0);
        while (pending.size > 0) {
            int index = pending.pop();
            int x = index % width, y = index / width;
            if (!isWhite(pixels.getPixel(x, y))) continue;
            while (x > 0 && isWhite(pixels.getPixel(x - 1, y))) x--;
            for (; x < width && isWhite(pixels.getPixel(x, y)); x++) {
                pixels.drawPixel(x, y, 0);
                if (y > 0 && isWhite(pixels.getPixel(x, y - 1))) pending.add((y - 1) * width + x);
                if (y + 1 < height && isWhite(pixels.getPixel(x, y + 1))) pending.add((y + 1) * width + x);
            }
        }
    }

    private static boolean isWhite(int rgba) {
        return (rgba & 255) > 0 && (rgba >>> 24) >= 210
            && ((rgba >>> 16) & 255) >= 210 && ((rgba >>> 8) & 255) >= 210;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
