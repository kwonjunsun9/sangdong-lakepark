package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import sangdong_lakepark.world.HardBoxes.Vehicle;

/** Pickup artwork, separate from the mounted character sprites. */
final class ItemSprites implements Disposable {
    private final Texture vehicles;
    private final TextureRegion bicycle, scooter, motorcycle;

    ItemSprites() {
        Pixmap pixels = new Pixmap(Gdx.files.internal("sangdong/items/vehicle-items.png"));
        try {
            vehicles = new Texture(pixels);
            vehicles.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            // The supplied sheet has unequal gutters; splitting into thirds clips the bicycle tire.
            bicycle = SpriteRegions.crop(pixels, vehicles, 0, 0, 790, pixels.getHeight());
            scooter = SpriteRegions.crop(pixels, vehicles, 790, 0, 560, pixels.getHeight());
            motorcycle = SpriteRegions.crop(pixels, vehicles, 1350, 0, pixels.getWidth() - 1350, pixels.getHeight());
        } finally { pixels.dispose(); }
    }

    void drawVehicle(SpriteBatch batch, Vehicle vehicle, float x, float y, float width, float height) {
        TextureRegion image;
        switch (vehicle) {
            case BICYCLE: image = bicycle; break;
            case SCOOTER: image = scooter; break;
            case MOTORCYCLE: image = motorcycle; break;
            default: return;
        }
        float scale = Math.min(width / image.getRegionWidth(), height / image.getRegionHeight());
        float w = image.getRegionWidth() * scale, h = image.getRegionHeight() * scale;
        batch.draw(image, x + (width - w) / 2, y, w, h);
    }

    @Override public void dispose() { vehicles.dispose(); }
}
