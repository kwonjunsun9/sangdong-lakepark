package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;

/** Owns menu resources; the font generator stays alive for incremental Korean glyphs. */
public final class MenuTheme implements Disposable {
    public static final Color INK = Color.valueOf("244F45");
    public static final Color MUTED = Color.valueOf("638276");
    public static final Color CREAM = Color.valueOf("FFF9E8");
    public static final Color GREEN = Color.valueOf("386C50");
    public static final Color GOLD = Color.valueOf("F4C96B");
    private final FreeTypeFontGenerator generator;
    private final Texture pixel;
    public final BitmapFont small, body, button, heading, title;

    public MenuTheme() {
        this("fonts/NanumGothic-Bold.ttf");
    }

    public MenuTheme(String fontPath) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
        small = font(16);
        body = font(21);
        body.getData().setLineHeight(30);
        button = font(24);
        heading = font(34);
        title = font(72);
    }

    private BitmapFont font(int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.incremental = true;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(p);
    }

    public Label label(String text, BitmapFont font, Color color) {
        return new Label(text, new Label.LabelStyle(font, color));
    }

    public TextButton.TextButtonStyle buttonStyle(boolean primary) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = button;
        style.fontColor = primary ? CREAM : INK;
        style.up = panel(primary ? GREEN : CREAM, primary ? INK : Color.valueOf("A8BC8C"), 4);
        style.over = panel(primary ? Color.valueOf("477D59") : Color.valueOf("FFF2C9"), INK, 4);
        style.focused = style.over;
        style.down = panel(primary ? INK : GOLD, INK, 0);
        return style;
    }

    public Drawable panel(Color fill, Color edge, int shadow) {
        return new PixelPanel(fill, edge, shadow);
    }

    public Drawable overlay(final Color tint) {
        return new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                float r = batch.getColor().r, g = batch.getColor().g;
                float b = batch.getColor().b, a = batch.getColor().a;
                batch.setColor(tint.r, tint.g, tint.b, tint.a * a);
                batch.draw(pixel, x, y, width, height);
                batch.setColor(r, g, b, a);
            }
        };
    }

    public Drawable roundedOverlay(final Color tint, final float cornerRadius) {
        return new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                float r = batch.getColor().r, g = batch.getColor().g;
                float b = batch.getColor().b, a = batch.getColor().a;
                batch.setColor(tint.r, tint.g, tint.b, tint.a * a);
                float radius = Math.min(cornerRadius, Math.min(width, height) / 2);
                for (float row = 0; row < height; row++) {
                    float h = Math.min(1, height - row), center = row + h / 2;
                    float distance = Math.max(radius - center, center - (height - radius));
                    float inset = distance > 0 ? radius - (float) Math.sqrt(Math.max(0, radius * radius - distance * distance)) : 0;
                    batch.draw(pixel, x + inset, y + row, width - inset * 2, h);
                }
                batch.setColor(r, g, b, a);
            }
        };
    }

    public Drawable muteSlash() {
        return muteSlash(new Color(.93f, .93f, .93f, 1), null);
    }

    public Drawable muteSlash(final Color stroke, final Color separator) {
        return new BaseDrawable() {
            @Override
            public void draw(Batch batch, float x, float y, float width, float height) {
                float r = batch.getColor().r, g = batch.getColor().g;
                float b = batch.getColor().b, a = batch.getColor().a;
                float dx = width - 4, dy = height - 4;
                float thickness = Math.max(2.5f, Math.min(width, height) * .09f);
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                if (separator != null) {
                    float outline = thickness + Math.max(2, thickness * .45f);
                    batch.setColor(separator.r, separator.g, separator.b, separator.a * a);
                    batch.draw(pixel, x + 2, y + 2 - outline / 2, 0, outline / 2,
                        length, outline, 1, 1, angle, 0, 0, 1, 1, false, false);
                }
                batch.setColor(stroke.r, stroke.g, stroke.b, stroke.a * a);
                batch.draw(pixel, x + 2, y + 2 - thickness / 2, 0, thickness / 2,
                    length, thickness, 1, 1, angle, 0, 0, 1, 1, false, false);
                batch.setColor(r, g, b, a);
            }
        };
    }

    private final class PixelPanel extends BaseDrawable {
        private final Color fill, edge;
        private final int shadow;

        PixelPanel(Color fill, Color edge, int shadow) {
            this.fill = new Color(fill);
            this.edge = new Color(edge);
            this.shadow = shadow;
            setLeftWidth(20);
            setRightWidth(20);
            setTopHeight(12);
            setBottomHeight(12);
        }

        @Override
        public void draw(Batch batch, float x, float y, float width, float height) {
            float r = batch.getColor().r, g = batch.getColor().g;
            float b = batch.getColor().b, a = batch.getColor().a;
            if (shadow > 0) {
                batch.setColor(edge.r, edge.g, edge.b, a * 0.25f);
                cutRect(batch, x, y - shadow, width, height);
            }
            batch.setColor(edge.r, edge.g, edge.b, edge.a * a);
            cutRect(batch, x, y, width, height);
            batch.setColor(fill.r, fill.g, fill.b, fill.a * a);
            cutRect(batch, x + 2, y + 2, width - 4, height - 4);
            batch.setColor(r, g, b, a);
        }

        private void cutRect(Batch batch, float x, float y, float w, float h) {
            batch.draw(pixel, x, y + 6, w, h - 12);
            batch.draw(pixel, x + 6, y, w - 12, 6);
            batch.draw(pixel, x + 6, y + h - 6, w - 12, 6);
        }
    }

    @Override
    public void dispose() {
        small.dispose();
        body.dispose();
        button.dispose();
        heading.dispose();
        title.dispose();
        generator.dispose();
        pixel.dispose();
    }
}
