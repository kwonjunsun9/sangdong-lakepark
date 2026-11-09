package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import sangdong_lakepark.stages.StageId;

/** The supplied stage card, with a live score aligned with the name printed above it. */
public final class StageStatusCard extends Actor implements Disposable {
    private static final float REFERENCE_WIDTH = 238, SCORE_DROP = 2;
    private final int cropX, cropY, cropWidth, cropHeight, scoreX, scoreY;
    private final Texture artwork;
    private final BitmapFont digits;
    private int score;

    public StageStatusCard(StageId stageId, float width) {
        boolean normal = stageId == StageId.NORMAL;
        boolean extreme = stageId == StageId.EXTREME;
        boolean hard = stageId == StageId.HARD || extreme, colored = normal || hard;
        cropX = hard ? 84 : normal ? 74 : 112; cropY = colored ? 17 : 176;
        cropWidth = hard ? 2566 : normal ? 2549 : 1488; cropHeight = hard ? 1516 : normal ? 1523 : 964;
        scoreX = hard ? 1817 : normal ? 1784 : 1115; scoreY = hard ? 947 : normal ? 955 : 781;
        String file = extreme ? "1certify.png" : hard ? "2certify.png" : normal ? "3certify.png" : "5certify.png";
        Pixmap original = new Pixmap(Gdx.files.internal("sangdong/hud/" + file));
        Pixmap card = new Pixmap(cropWidth, cropHeight, Pixmap.Format.RGBA8888);
        try {
            // Exclude the export's padding and stray right-hand line while retaining the original proportions.
            // Colored card outlines reach the crop edges, so clear their export margins before cropping.
            if (colored) {
                original.setBlending(Pixmap.Blending.None);
                removeOutsideWhite(original);
            }
            card.setBlending(Pixmap.Blending.None);
            card.drawPixmap(original, 0, 0, cropX, cropY, cropWidth, cropHeight);
            if (!colored) removeOutsideWhite(card);
            // Clear the baked-in zero in memory; the packaged source image remains unmodified.
            if (colored) restoreScoreBackground(original, card, hard);
            else {
                card.setColor(Color.WHITE);
                card.fillRectangle(1080 - cropX, 769 - cropY, 180, 115);
            }
            artwork = new Texture(card, true);
            artwork.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        } finally {
            original.dispose(); card.dispose();
        }
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/pretendard-jp/PretendardJP-Regular.otf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            // Rasterize at the displayed size: no downscaled outline around the live digits.
            parameter.size = Math.max(1, Math.round(width * (colored ? 174 : 120) / cropWidth));
            parameter.characters = "0123456789";
            parameter.minFilter = Texture.TextureFilter.Linear; parameter.magFilter = Texture.TextureFilter.Linear;
            digits = generator.generateFont(parameter);
        } finally { generator.dispose(); }
        setName("easy-status-card");
        setSize(width, width * cropHeight / cropWidth);
        setTouchable(Touchable.disabled);
    }

    public void setScore(int value) { score = value; }
    public int getScore() { return score; }

    @Override public void draw(Batch batch, float parentAlpha) {
        Color previous = batch.getColor();
        float r = previous.r, g = previous.g, b = previous.b, a = previous.a;
        Color tint = getColor(); float alpha = tint.a * parentAlpha;
        batch.setColor(tint.r, tint.g, tint.b, alpha);
        batch.draw(artwork, getX(), getY(), getWidth(), getHeight());
        float scale = getWidth() / cropWidth;
        digits.setColor(0, 0, 0, alpha);
        digits.draw(batch, Integer.toString(score), getX() + (scoreX - cropX) * scale,
            getY() + (cropHeight - (scoreY - cropY)) * scale - SCORE_DROP * getWidth() / REFERENCE_WIDTH);
        batch.setColor(r, g, b, a);
    }

    private static void removeOutsideWhite(Pixmap pixels) {
        int width = pixels.getWidth(), height = pixels.getHeight();
        IntArray pending = new IntArray(); pending.add(0);
        while (pending.size > 0) {
            int index = pending.pop(), x = index % width, y = index / width;
            if (!isWhite(pixels.getPixel(x, y))) continue;
            while (x > 0 && isWhite(pixels.getPixel(x - 1, y))) x--;
            for (; x < width && isWhite(pixels.getPixel(x, y)); x++) {
                pixels.drawPixel(x, y, 0);
                if (y > 0 && isWhite(pixels.getPixel(x, y - 1))) pending.add((y - 1) * width + x);
                if (y + 1 < height && isWhite(pixels.getPixel(x, y + 1))) pending.add((y + 1) * width + x);
            }
        }
    }

    private void restoreScoreBackground(Pixmap original, Pixmap card, boolean hard) {
        // Interpolate between the untouched edges of the numeral to retain the card's background gradient.
        int x0 = hard ? 1772 : 1748, y0 = hard ? 943 : 951, width = hard ? 126 : 128, height = 156;
        for (int y = 0; y < height; y++) {
            int left = original.getPixel(x0 - 1, y0 + y), right = original.getPixel(x0 + width, y0 + y);
            for (int x = 0; x < width; x++) {
                float t = (x + 1f) / (width + 1);
                int color = channel(left, right, 24, t) << 24 | channel(left, right, 16, t) << 16
                    | channel(left, right, 8, t) << 8 | 255;
                card.drawPixel(x0 - cropX + x, y0 - cropY + y, color);
            }
        }
    }

    private static int channel(int left, int right, int shift, float t) {
        return Math.round(((left >>> shift) & 255) * (1 - t) + ((right >>> shift) & 255) * t);
    }

    private static boolean isWhite(int rgba) {
        return (rgba & 255) > 0 && (rgba >>> 24) >= 210
            && ((rgba >>> 16) & 255) >= 210 && ((rgba >>> 8) & 255) >= 210;
    }

    @Override public void dispose() { artwork.dispose(); digits.dispose(); }
}
