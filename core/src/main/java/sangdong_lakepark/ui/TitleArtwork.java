package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Scaling;

/** Displays the artwork and maps its painted buttons and icons to the stage. */
public final class TitleArtwork implements Disposable {
    private final Texture texture, selectionTexture, overlaysTexture;
    private final JsonValue overlays;
    private final TextureRegion selectionRegion;
    private final float markerWidth, markerHeight;
    private final Rectangle bounds;
    private final Rectangle soundIndicator;
    private final JsonValue buttons;
    private final float referenceWidth, referenceHeight;

    private TitleArtwork(FileHandle layoutFile, float width, float height) {
        JsonValue layout = new JsonReader().parse(layoutFile);
        buttons = layout.get("buttons");
        referenceWidth = layout.getFloat("referenceWidth");
        referenceHeight = layout.getFloat("referenceHeight");
        FileHandle imageFile = layoutFile.parent().child(layout.getString("image"));
        texture = new Texture(imageFile);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Vector2 size = Scaling.fit.apply(texture.getWidth(), texture.getHeight(), width, height);
        bounds = new Rectangle((width - size.x) / 2, (height - size.y) / 2, size.x, size.y);
        soundIndicator = regionBounds(layout.get("soundIndicator"));
        JsonValue overlay = layout.get("overlays");
        overlays = overlay.get("regions");
        overlaysTexture = new Texture(layoutFile.parent().child(overlay.getString("image")));
        overlaysTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        JsonValue marker = layout.get("selectionMarker");
        selectionTexture = new Texture(layoutFile.parent().child(marker.getString("image")), true);
        selectionTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        JsonValue source = marker.get("source");
        selectionRegion = new TextureRegion(selectionTexture, source.getInt("x"), source.getInt("y"),
            source.getInt("width"), source.getInt("height"));
        markerWidth = marker.getFloat("width") * bounds.width / referenceWidth;
        markerHeight = marker.getFloat("height") * bounds.height / referenceHeight;
        Gdx.app.log("TitleScreen", "Loaded " + layoutFile.path() + " (layout " + layout.getInt("version")
            + ", " + texture.getWidth() + "x" + texture.getHeight() + ")");
    }

    public static TitleArtwork load(float width, float height) {
        // The compiled application supplies both the image and its layout as one resource set.
        // A loose PNG in the working directory must never override the packaged layout.
        FileHandle layoutFile = Gdx.files.classpath("sangdong/title/title-menu.json");
        if (!layoutFile.exists()) {
            throw new GdxRuntimeException("Missing sangdong/title/title-menu.json. Run gradlew clean lwjgl3:run from midterm-2d-project.");
        }
        return new TitleArtwork(layoutFile, width, height);
    }

    public Group image() {
        Group layers = new Group();
        layers.setSize(bounds.x * 2 + bounds.width, bounds.y * 2 + bounds.height);
        layers.setTouchable(Touchable.disabled);
        Image image = new Image(texture);
        image.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        image.setTouchable(Touchable.disabled);
        layers.addActor(image);
        // Keep the original landscape intact; replace only the painted menu regions.
        for (JsonValue region : overlays) {
            Rectangle area = regionBounds(region);
            layers.addActor(new ArtworkPatch(overlaysTexture, area, region,
                bounds.width / referenceWidth, bounds.height / referenceHeight));
        }
        return layers;
    }

    public Rectangle startButtonBounds() {
        return buttonBounds("start");
    }

    public Rectangle aboutButtonBounds() {
        return buttonBounds("about");
    }

    public Rectangle exitButtonBounds() {
        return buttonBounds("exit");
    }

    public Image selectionMarker() {
        Image image = new Image(selectionRegion);
        image.setSize(markerWidth, markerHeight);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    public Rectangle optionsButtonBounds() {
        return buttonBounds("options");
    }

    public Rectangle soundButtonBounds() {
        return buttonBounds("sound");
    }

    public Rectangle soundIndicatorBounds() {
        return new Rectangle(soundIndicator);
    }

    private Rectangle buttonBounds(String name) {
        return regionBounds(buttons.get(name));
    }

    private Rectangle regionBounds(JsonValue region) {
        return new Rectangle(bounds.x + bounds.width * region.getFloat("x") / referenceWidth,
            bounds.y + bounds.height * (1 - (region.getFloat("y") + region.getFloat("height")) / referenceHeight),
            bounds.width * region.getFloat("width") / referenceWidth,
            bounds.height * region.getFloat("height") / referenceHeight);
    }

    /** Feather UI artwork into the original lawn without altering the source background. */
    private static final class ArtworkPatch extends Actor {
        private final Texture texture;
        private final float[] xs, ys, us, vs;
        private final float[] vertices = new float[20];

        ArtworkPatch(Texture texture, Rectangle area, JsonValue source, float scaleX, float scaleY) {
            this.texture = texture;
            setBounds(area.x, area.y, area.width, area.height);
            setTouchable(Touchable.disabled);
            float edge = source.getFloat("feather");
            float width = source.getFloat("width"), height = source.getFloat("height");
            xs = new float[] { 0, edge * scaleX, area.width - edge * scaleX, area.width };
            ys = new float[] { 0, edge * scaleY, area.height - edge * scaleY, area.height };
            float left = source.getFloat("x"), bottom = source.getFloat("y") + height;
            us = new float[] { left / texture.getWidth(), (left + edge) / texture.getWidth(),
                (left + width - edge) / texture.getWidth(), (left + width) / texture.getWidth() };
            vs = new float[] { bottom / texture.getHeight(), (bottom - edge) / texture.getHeight(),
                (bottom - height + edge) / texture.getHeight(), (bottom - height) / texture.getHeight() };
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    vertex(0, x, y, parentAlpha);
                    vertex(5, x, y + 1, parentAlpha);
                    vertex(10, x + 1, y + 1, parentAlpha);
                    vertex(15, x + 1, y, parentAlpha);
                    batch.draw(texture, vertices, 0, vertices.length);
                }
            }
        }

        private void vertex(int offset, int x, int y, float parentAlpha) {
            Color color = getColor();
            float alpha = x > 0 && x < 3 && y > 0 && y < 3 ? color.a * parentAlpha : 0;
            vertices[offset] = getX() + xs[x];
            vertices[offset + 1] = getY() + ys[y];
            vertices[offset + 2] = Color.toFloatBits(color.r, color.g, color.b, alpha);
            vertices[offset + 3] = us[x];
            vertices[offset + 4] = vs[y];
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
        selectionTexture.dispose();
        overlaysTexture.dispose();
    }
}
