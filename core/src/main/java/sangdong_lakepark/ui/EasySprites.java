package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Transparent, independently drawable character and life-icon regions. */
public final class EasySprites implements Disposable {
    /** Let the opaque foot outline touch the grass beneath the antialiased sprite edge. */
    public static final float PLAYER_FOOT_OVERLAP = 2;
    private final Texture smooth, pixel, rider;
    public final TextureRegion player, portrait, heart, brokenHeart, bikePlayer;

    public EasySprites() {
        Pixmap source = new Pixmap(Gdx.files.internal("sangdong/easy/character-hud-atlas.png"));
        try {
            smooth = new Texture(source, true);
            smooth.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            pixel = new Texture(source);
            pixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            player = region(source, smooth, 0, 0);
            portrait = region(source, smooth, 1, 0);
            heart = region(source, pixel, 0, 1);
            brokenHeart = region(source, pixel, 1, 1);
        } finally { source.dispose(); }
        Pixmap bicycle = new Pixmap(Gdx.files.internal("sangdong/easy/bicycle-rider.png"));
        try {
            rider = new Texture(bicycle, true);
            rider.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            bikePlayer = SpriteRegions.crop(bicycle, rider, 0, 0, bicycle.getWidth(), bicycle.getHeight());
        } finally { bicycle.dispose(); }
    }

    private static TextureRegion region(Pixmap source, Texture texture, int column, int row) {
        int width = source.getWidth() / 2, height = source.getHeight() / 2;
        return SpriteRegions.crop(source, texture, column * width, row * height, width, height);
    }

    @Override public void dispose() { smooth.dispose(); pixel.dispose(); rider.dispose(); }
}
