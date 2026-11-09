package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import sangdong_lakepark.stages.StageStartSequence;

/** Transparent wordmarks drawn over the existing stage and HUD. */
public final class StageStartOverlay implements Disposable {
    private final Texture texture;
    private final TextureRegion ready, go;

    public StageStartOverlay() {
        Pixmap image = new Pixmap(Gdx.files.classpath("sangdong/start/ready-go.png"));
        texture = new Texture(image);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        ready = trim(image, 0, image.getHeight() / 2);
        go = trim(image, image.getHeight() / 2, image.getHeight());
        image.dispose();
    }

    private TextureRegion trim(Pixmap image, int top, int bottom) {
        int left = image.getWidth(), right = -1, first = bottom, last = top;
        for (int y = top; y < bottom; y++) for (int x = 0; x < image.getWidth(); x++) {
            // Ignore near-invisible export noise when finding each wordmark's bounds.
            if ((image.getPixel(x, y) & 255) <= 16) continue;
            left = Math.min(left, x); right = Math.max(right, x);
            first = Math.min(first, y); last = Math.max(last, y);
        }
        if (right < left) throw new IllegalStateException("Missing READY/Go artwork");
        return new TextureRegion(texture, left, first, right - left + 1, last - first + 1);
    }

    /** The caller supplies a begun batch using its 1280 by 720 HUD projection. */
    public void draw(SpriteBatch batch, StageStartSequence sequence) {
        if (!sequence.isActive()) return;
        TextureRegion word = sequence.isReady() ? ready : go;
        float width = (sequence.isReady() ? 480 : 280) * sequence.getScale();
        float height = width * word.getRegionHeight() / word.getRegionWidth();
        float color = batch.getPackedColor();
        batch.setColor(1, 1, 1, sequence.getAlpha());
        batch.draw(word, (1280 - width) / 2 + sequence.getOffsetX(), (720 - height) / 2, width, height);
        batch.setPackedColor(color);
    }

    @Override public void dispose() { texture.dispose(); }
}
