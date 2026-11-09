package sangdong_lakepark.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;

/** A shared silver padlock sprite for the locked badges and their notice. */
public final class MapLockArtwork implements Disposable {
    private final Texture texture;
    private final TextureRegion lock;

    public MapLockArtwork(FileHandle file) {
        Pixmap pixels = new Pixmap(file);
        try {
            texture = new Texture(pixels, true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            lock = SpriteRegions.crop(pixels, texture, 0, 0, pixels.getWidth(), pixels.getHeight());
        } finally {
            pixels.dispose();
        }
    }

    public Image image(float width) {
        Image image = new Image(lock);
        image.setSize(width, width * lock.getRegionHeight() / lock.getRegionWidth());
        image.setTouchable(Touchable.disabled);
        return image;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
