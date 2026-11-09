package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** The dialogue and map use the same transparent rider, with the same proportions. */
public final class RiderArtwork implements Disposable {
    private final Texture texture;
    private final TextureRegion region;

    public RiderArtwork(FileHandle directory, JsonValue definition) {
        texture = new Texture(directory.child(definition.getString("image")), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        JsonValue source = definition.get("source");
        region = new TextureRegion(texture, source.getInt("x"), source.getInt("y"),
            source.getInt("width"), source.getInt("height"));
    }

    public static RiderArtwork load() {
        FileHandle file = Gdx.files.classpath("sangdong/map/map.json");
        return new RiderArtwork(file.parent(), new JsonReader().parse(file).get("rider"));
    }

    public Image image(float height) {
        Image image = new Image(region);
        image.setSize(height * region.getRegionWidth() / region.getRegionHeight(), height);
        image.setOrigin(image.getWidth() / 2, 0);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
