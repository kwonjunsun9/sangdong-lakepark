package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/** Image-backed dialog pages with rounded edges and artwork-aligned hit regions. */
public final class DialogArtwork implements Disposable {
    private final ObjectMap<String, Texture> textures = new ObjectMap<>();
    private final JsonValue pages;
    private final ShaderProgram shader;

    public DialogArtwork() {
        FileHandle file = Gdx.files.classpath("sangdong/dialogs/dialogs.json");
        JsonValue layout = new JsonReader().parse(file);
        pages = layout.get("pages");
        for (JsonValue image = layout.get("images").child; image != null; image = image.next) {
            Texture texture = new Texture(file.parent().child(image.asString()));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.put(image.name, texture);
        }
        shader = new ShaderProgram(file.parent().child("page.vert"), file.parent().child("page.frag"));
        if (!shader.isCompiled()) throw new GdxRuntimeException("Dialog page shader: " + shader.getLog());
        Gdx.app.log("DialogArtwork", "Loaded " + file.path() + " (layout " + layout.getInt("version") + ")");
    }

    public Page page(String name) {
        JsonValue definition = pages.get(name);
        return new Page(textures.get(definition.getString("image")),
            definition.has("frame") ? textures.get(definition.getString("frame")) : null,
            definition.has("soundOffImage") ? textures.get(definition.getString("soundOffImage")) : null,
            definition, shader);
    }

    public static final class Page {
        private final TextureRegion content;
        private final Texture frame, soundOff;
        private final ShaderProgram shader;
        private final JsonValue definition;
        private final float sourceX, sourceY, sourceWidth, sourceHeight, scaleX, scaleY;
        public final float width, height;

        private Page(Texture image, Texture frame, Texture soundOff, JsonValue definition, ShaderProgram shader) {
            this.definition = definition;
            this.frame = frame;
            this.soundOff = soundOff;
            this.shader = shader;
            JsonValue source = definition.get("source");
            sourceX = source.getFloat("x");
            sourceY = source.getFloat("y");
            sourceHeight = source.getFloat("height");
            sourceWidth = source.getFloat("width");
            float fitScale = Math.min(960 / sourceWidth, 652 / sourceHeight);
            JsonValue displaySize = definition.get("displaySize");
            width = displaySize == null ? sourceWidth * fitScale : displaySize.getFloat("width");
            height = displaySize == null ? sourceHeight * fitScale : displaySize.getFloat("height");
            scaleX = width / sourceWidth;
            scaleY = height / sourceHeight;
            content = new TextureRegion(image, (int) sourceX, (int) sourceY, (int) sourceWidth, (int) sourceHeight);
        }

        public Group image() {
            Group layers = new Group();
            layers.setSize(width, height);
            layers.setTouchable(Touchable.disabled);
            if (frame != null) {
                JsonValue reference = definition.get("frameReference");
                float sx = width / reference.getFloat("width"), sy = height / reference.getFloat("height");
                Image border = new Image(new PageDrawable(new TextureRegion(frame), false));
                border.setBounds(-reference.getFloat("x") * sx,
                    (reference.getFloat("y") + reference.getFloat("height") - frame.getHeight()) * sy,
                    frame.getWidth() * sx, frame.getHeight() * sy);
                layers.addActor(border);
            }
            Image original = new Image(new PageDrawable(content, !definition.getBoolean("transparent", false)));
            original.setBounds(0, 0, width, height);
            layers.addActor(original);
            return layers;
        }

        private final class PageDrawable extends TextureRegionDrawable {
            private final boolean interior;

            PageDrawable(TextureRegion region, boolean interior) {
                super(region);
                this.interior = interior;
            }

            @Override
            public void draw(Batch batch, float x, float y, float w, float h) {
                ShaderProgram previous = batch.getShader();
                batch.setShader(shader);
                try {
                    TextureRegion region = getRegion();
                    shader.setUniformf("u_isInterior", interior ? 1 : 0);
                    if (interior) {
                        JsonValue inset = definition.get("interior");
                        shader.setUniformf("u_region", region.getU(), region.getV(),
                            region.getU2() - region.getU(), region.getV2() - region.getV());
                        shader.setUniformf("u_sourceSize", sourceWidth, sourceHeight);
                        shader.setUniformf("u_interior", inset.getFloat("x"), inset.getFloat("y"),
                            inset.getFloat("width"), inset.getFloat("height"));
                        shader.setUniformf("u_radius", inset.getFloat("radius"));
                        shader.setUniformf("u_feather", inset.getFloat("feather"));
                    }
                    super.draw(batch, x, y, w, h);
                } finally {
                    batch.flush();
                    batch.setShader(previous);
                }
            }
        }

        public Rectangle buttonBounds(String name) {
            return bounds(definition.get("buttons").get(name));
        }

        public float buttonRadius(String name) {
            JsonValue button = definition.get("buttons").get(name);
            return button.getFloat("radius", button.getFloat("height") / 2) * Math.min(scaleX, scaleY);
        }

        public boolean hasPaintedButtonAccent() {
            return definition.getBoolean("paintedButtonAccent", false);
        }

        public Rectangle soundIndicatorBounds() {
            return bounds(definition.get("soundIndicator"));
        }

        public TextureRegionDrawable soundOffDrawable() {
            JsonValue region = definition.get("soundIndicator");
            return new TextureRegionDrawable(new TextureRegion(soundOff,
                region.getInt("x"), region.getInt("y"), region.getInt("width"), region.getInt("height")));
        }

        private Rectangle bounds(JsonValue region) {
            return new Rectangle((region.getFloat("x") - sourceX) * scaleX,
                (sourceHeight - (region.getFloat("y") - sourceY) - region.getFloat("height")) * scaleY,
                region.getFloat("width") * scaleX, region.getFloat("height") * scaleY);
        }
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
        shader.dispose();
    }
}
