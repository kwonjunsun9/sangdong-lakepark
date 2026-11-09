package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Align;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.world.EasyRun;

/** Fixed-screen HUD bound to the current attempt's lives, score, time and progress. */
public final class EasyHud extends Group implements Disposable {
    public static final int MAX_LIVES = EasyRun.MAX_LIVES;
    private static final Color INK = Color.valueOf("3D3029"), CREAM = Color.valueOf("FFF8E5");
    private static final Color CONTROL_INK = Color.WHITE;
    private static final Color CARD_EDGE = Color.valueOf("D4AD7D"), STEEL = Color.valueOf("9DA7A5");
    private static final Color METAL_LIGHT = Color.valueOf("F0EFE2"), TIMER = new Color(.1412f, .3647f, .4039f, .3f);
    private static final Color CARD_SHADOW = new Color(0, 0, 0, .12f), TIMER_SHADOW = new Color(0, 0, 0, .045f);
    private static final Color PROGRESS_EDGE = Color.valueOf("59372B"), PROGRESS_GOLD = Color.valueOf("E2AD43");
    private static final Color BLUE_EMPTY = Color.valueOf("CFEFFF"), BLUE_FILL = Color.valueOf("139EFF"), BLUE_STRIPE = Color.valueOf("48CAFF");
    private static final Color PURPLE_EMPTY = Color.valueOf("E9D7FA"), PURPLE_FILL = Color.valueOf("8B3FE0"), PURPLE_STRIPE = Color.valueOf("BA7CF2");
    private static final Color ORANGE_EMPTY = Color.valueOf("FFE7CE"), ORANGE_BOTTOM = Color.valueOf("F07818"), ORANGE_TOP = Color.valueOf("FFB85C");
    private static final float HEART_X = 14, HEART_STEP = 49, HEART_WIDTH = 42;
    private static final float CARD_X = HEART_X, CARD_Y = 576;
    private static final float CARD_WIDTH = (MAX_LIVES - 1) * HEART_STEP + HEART_WIDTH, CARD_HEIGHT = 130;
    private static final float INFO_X = 106, INFO_WIDTH = 138, INFO_CENTER_Y = CARD_Y + CARD_HEIGHT / 2 - 8;
    private static final float PROGRESS_X = 467, PROGRESS_Y = 640, PROGRESS_WIDTH = 340, PROGRESS_HEIGHT = 26;
    private static final float TIMER_WIDTH = 132, TIMER_HEIGHT = 30, TIMER_Y = 684;
    private static final float TIMER_X = PROGRESS_X + (PROGRESS_WIDTH - TIMER_WIDTH) / 2;
    private final EasySprites sprites;
    private final Texture icons = createIcons();
    private final BitmapFont regular;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final ButtonAccent accent = new ButtonAccent();
    private final Image[] hearts = new Image[MAX_LIVES];
    private final TextureRegionDrawable fullHeart, lostHeart;
    private final Image sound;
    private final Label scoreLabel, timeLabel;
    private final StageStatusCard statusCard;
    private float progress;
    private final boolean stripedProgress, orangeProgress, purpleProgress;
    private final Color progressBase = new Color(), progressStripe = new Color();
    private int lives = -1;
    private int shownScore = -1;
    private long shownSeconds = -1;

    public EasyHud(MenuTheme theme, EasySprites sprites, Runnable options, Runnable toggleSound, Runnable pause) {
        this(theme, sprites, options, toggleSound, pause, StageId.EASY);
    }

    public EasyHud(MenuTheme theme, EasySprites sprites, Runnable options, Runnable toggleSound, Runnable pause, StageId stageId) {
        this.sprites = sprites;
        stripedProgress = stageId != StageId.EASY;
        orangeProgress = stageId == StageId.HARD;
        purpleProgress = stageId == StageId.EXTREME;
        setSize(1280, 720);
        setTouchable(Touchable.childrenOnly);
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pretendard-jp/PretendardJP-Regular.otf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 16; parameter.characters = "연습면허급Name : 인직카와Score0123456789EasyNormalHardExtreme/";
            parameter.minFilter = Texture.TextureFilter.Linear; parameter.magFilter = Texture.TextureFilter.Linear;
            regular = generator.generateFont(parameter);
        } finally { generator.dispose(); }
        if (stageId == StageId.EASY || stageId == StageId.NORMAL || stageId == StageId.HARD || stageId == StageId.EXTREME) {
            statusCard = new StageStatusCard(stageId, CARD_WIDTH);
            statusCard.setPosition(CARD_X, 718 - statusCard.getHeight());
            addActor(statusCard); scoreLabel = null;
        } else {
            statusCard = null;
            Actor card = new Actor() {
                @Override public void draw(Batch batch, float alpha) { drawCard(batch); }
            };
            card.setName("easy-status-card"); card.setBounds(CARD_X, CARD_Y - 8, CARD_WIDTH, CARD_HEIGHT + 19);
            card.setTouchable(Touchable.disabled); addActor(card);
            image(sprites.portrait, 26, 586, 78, 78).setName("easy-portrait");
            Label license = label("easy-license", stageId == StageId.EXTREME ? "1급" : "연습면허", regular, INK, 27, 679, 70, 18);
            license.setFontScale(.75f * 1.05f);
            Label name = label("easy-name", "Name : 인직카와", theme.small, INK,
                INFO_X, INFO_CENTER_Y + 1, INFO_WIDTH, 28);
            name.setFontScale(1.2f);
            Label difficulty = label("easy-difficulty", stageId.displayName(), regular, INK, 170, 585, 70, 22);
            difficulty.setAlignment(Align.right);
            scoreLabel = label("easy-score", "", regular, INK,
                INFO_X, INFO_CENTER_Y - 29, INFO_WIDTH, 28);
            scoreLabel.setFontScale(1.2f);
        }
        Actor timer = new Actor() {
            @Override public void draw(Batch batch, float alpha) { drawTimer(batch); }
        };
        timer.setName("easy-timer"); timer.setBounds(TIMER_X, TIMER_Y - 3, TIMER_WIDTH, TIMER_HEIGHT + 3);
        timer.setTouchable(Touchable.disabled); addActor(timer);
        timeLabel = label("easy-time", "", theme.button, Color.WHITE,
            TIMER_X + 34, TIMER_Y, TIMER_WIDTH - 42, TIMER_HEIGHT);
        timeLabel.setAlignment(Align.center);
        fullHeart = new TextureRegionDrawable(sprites.heart);
        lostHeart = new TextureRegionDrawable(sprites.brokenHeart);
        for (int i = 0; i < MAX_LIVES; i++) {
            hearts[i] = image(sprites.heart, HEART_X + i * HEART_STEP,
                stageId == StageId.EASY ? 516 : 522, HEART_WIDTH, 40);
            hearts[i].setName("easy-heart-" + i);
        }
        setLives(MAX_LIVES);
        Actor track = new Actor() {
            @Override public void draw(Batch batch, float alpha) { drawProgress(batch); }
        };
        track.setName("easy-progress"); track.setBounds(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT);
        track.setTouchable(Touchable.disabled); addActor(track);
        iconButton(theme, "easy-options", 0, 1076, options);
        sound = iconButton(theme, "easy-sound", 1, 1140, toggleSound);
        iconButton(theme, "easy-pause", 2, 1204, pause);
        label("easy-pause-hint", "ESC", theme.small, CONTROL_INK, 1204, 630, 48, 22).setAlignment(Align.center);
    }

    private Label label(String name, String text, BitmapFont font, Color color,
                        float x, float y, float width, float height) {
        Label label = new Label(text, new Label.LabelStyle(font, color)) {
            @Override public void draw(Batch batch, float parentAlpha) {
                float x = getX(), y = getY(); Color tint = getColor();
                float r = tint.r, g = tint.g, b = tint.b, a = tint.a;
                setPosition(x + 1, y - 1); setColor(0, 0, 0, a * .22f);
                super.draw(batch, parentAlpha);
                setPosition(x, y); setColor(r, g, b, a);
                super.draw(batch, parentAlpha);
            }
        };
        label.setName(name); label.setBounds(x, y, width, height);
        label.setTouchable(Touchable.disabled); addActor(label); return label;
    }

    private Image image(TextureRegion region, float x, float y, float width, float height) {
        Image image = new Image(region); image.setBounds(x, y, width, height);
        image.setTouchable(Touchable.disabled); addActor(image); return image;
    }

    private Image iconButton(MenuTheme theme, String name, int index, float x, Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.over = theme.roundedOverlay(new Color(1, 1, 1, .15f), 12);
        style.down = theme.roundedOverlay(new Color(0, 0, 0, .10f), 12);
        Button button = new Button(style); button.setName(name); button.setBounds(x, 650, 48, 48);
        Image icon = new Image(new TextureRegion(icons, index * 128, 0, 128, 128));
        icon.setBounds(4, 4, 40, 40); icon.setTouchable(Touchable.disabled); button.addActor(icon);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        addActor(button); addActor(accent.forButton(button)); return icon;
    }

    public void setProgress(float value) { progress = MathUtils.clamp(value, 0, 1); }

    public void setStats(int score, int total, double elapsed) {
        if (shownScore != score) {
            shownScore = score;
            if (statusCard != null) statusCard.setScore(score);
            else scoreLabel.setText("Score : " + score + (stripedProgress ? " / " + total : ""));
        }
        long seconds = (long) Math.max(0, elapsed);
        if (shownSeconds != seconds) {
            shownSeconds = seconds;
            timeLabel.setText(EasyRun.formatTime(elapsed));
            timeLabel.setFontScale(1);
            if (timeLabel.getPrefWidth() > timeLabel.getWidth())
                timeLabel.setFontScale(timeLabel.getWidth() / timeLabel.getPrefWidth());
        }
    }

    /** The attempt owns damage, death and respawn; the HUD only displays the supplied count. */
    public void setLives(int value) {
        int bounded = MathUtils.clamp(value, 0, MAX_LIVES);
        if (lives == bounded) return;
        lives = bounded;
        for (int i = 0; i < hearts.length; i++) hearts[i].setDrawable(i < lives ? fullHeart : lostHeart);
    }

    public int getLives() { return lives; }

    public void syncSound(GameAudio audio) {
        int index = audio.isEnabled() ? 1 : 3;
        sound.setDrawable(new TextureRegionDrawable(new TextureRegion(icons, index * 128, 0, 128, 128)));
    }

    private void beginShapes(Batch batch) {
        batch.end(); Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(batch.getProjectionMatrix()); shapes.setTransformMatrix(batch.getTransformMatrix());
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void endShapes(Batch batch) {
        shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND); batch.begin();
    }

    private void drawCard(Batch batch) {
        beginShapes(batch);
        round(CARD_X, CARD_Y - 8, CARD_WIDTH, CARD_HEIGHT, 24, CARD_SHADOW);
        round(CARD_X, CARD_Y - 6, CARD_WIDTH, CARD_HEIGHT, 24, INK);
        round(CARD_X + 2, CARD_Y - 4, CARD_WIDTH - 4, CARD_HEIGHT - 4, 22, CARD_EDGE);
        round(CARD_X, CARD_Y, CARD_WIDTH, CARD_HEIGHT, 24, INK);
        round(CARD_X + 2, CARD_Y + 2, CARD_WIDTH - 4, CARD_HEIGHT - 4, 22, CREAM);
        shapes.setColor(INK); shapes.rect(INFO_X, INFO_CENTER_Y - .75f, INFO_WIDTH, 1.5f);
        for (int i = 0; i < 3; i++) {
            float x = CARD_X + 84 + i * 53;
            shapes.setColor(CARD_EDGE); shapes.ellipse(x - 6, 689, 12, 9, 24);
            shapes.setColor(INK); shapes.ellipse(x - 4, 691, 8, 5, 24);
            ringArc(x, 704, -95, 245, 6, INK);
            ringArc(x, 704, -95, 245, 3.5f, STEEL);
            ringArc(x - .5f, 704.5f, 20, 155, 1.2f, METAL_LIGHT);
        }
        endShapes(batch);
    }

    private void ringArc(float x, float y, float from, float to, float thickness, Color color) {
        shapes.setColor(color);
        for (int i = 0; i < 28; i++) {
            float a = (from + (to - from) * i / 28) * MathUtils.degreesToRadians;
            float b = (from + (to - from) * (i + 1) / 28) * MathUtils.degreesToRadians;
            shapes.rectLine(x + MathUtils.cos(a) * 6, y + MathUtils.sin(a) * 10,
                x + MathUtils.cos(b) * 6, y + MathUtils.sin(b) * 10, thickness);
        }
    }

    private void drawTimer(Batch batch) {
        beginShapes(batch);
        round(TIMER_X, TIMER_Y - 3, TIMER_WIDTH, TIMER_HEIGHT, TIMER_SHADOW);
        round(TIMER_X, TIMER_Y, TIMER_WIDTH, TIMER_HEIGHT, TIMER);
        float x = TIMER_X + 17, y = TIMER_Y + TIMER_HEIGHT / 2;
        shapes.setColor(Color.WHITE);
        for (int i = 0; i < 32; i++) {
            float a = i * MathUtils.PI2 / 32, b = (i + 1) * MathUtils.PI2 / 32;
            shapes.rectLine(x + MathUtils.cos(a) * 10, y + MathUtils.sin(a) * 10,
                x + MathUtils.cos(b) * 10, y + MathUtils.sin(b) * 10, 1.4f);
        }
        for (int i = 0; i < 4; i++) {
            float a = i * MathUtils.PI / 2;
            shapes.rectLine(x + MathUtils.cos(a) * 7, y + MathUtils.sin(a) * 7,
                x + MathUtils.cos(a) * 9, y + MathUtils.sin(a) * 9, 1.3f);
        }
        shapes.rectLine(x, y, x + 4, y + 5, 1.8f); shapes.rectLine(x, y, x - 3, y + 1, 1.8f);
        endShapes(batch);
    }

    private void drawProgress(Batch batch) {
        if (stripedProgress) { drawStripedProgress(batch); return; }
        float x = PROGRESS_X, y = PROGRESS_Y, width = PROGRESS_WIDTH, height = PROGRESS_HEIGHT;
        batch.end(); shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.setTransformMatrix(batch.getTransformMatrix()); shapes.begin(ShapeRenderer.ShapeType.Filled);
        round(x, y, width, height, Color.valueOf("59372B"));
        round(x + 2, y + 2, width - 4, height - 4, Color.valueOf("E2AD43"));
        round(x + 4, y + 4, width - 8, height - 8, Color.valueOf("FFF4C5"));
        round(x + 6, y + 6, width - 12, height - 12, Color.valueOf("E0EBA0"));
        float fill = (width - 12) * progress;
        if (fill > 0) {
            round(x + 6, y + 6, fill, height - 12, Color.valueOf("79BD20"));
            shapes.setColor(Color.valueOf("A1DC28"));
            for (float stripe = 0; stripe + 18 < fill; stripe += 26)
                shapes.triangle(x + 6 + stripe, y + 7, x + 24 + stripe, y + 7, x + 24 + stripe, y + 19);
        }
        shapes.setColor(Color.valueOf("59372B")); shapes.rect(x + width - 12, y + 15, 3, 27);
        shapes.triangle(x + width - 10, y + 39, x + width + 10, y + 32, x + width - 10, y + 25);
        shapes.setColor(MenuTheme.GOLD);
        shapes.triangle(x + width - 7, y + 36, x + width + 4, y + 32, x + width - 7, y + 28);
        shapes.end(); batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(sprites.portrait, x + 8 + (width - 16) * progress - 22, y + 13 - 22, 44, 44);
    }

    private void drawStripedProgress(Batch batch) {
        float x=PROGRESS_X,y=PROGRESS_Y,w=PROGRESS_WIDTH,h=PROGRESS_HEIGHT;
        batch.end();shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.setTransformMatrix(batch.getTransformMatrix());shapes.begin(ShapeRenderer.ShapeType.Filled);
        round(x,y,w,h,PROGRESS_EDGE);
        round(x+2,y+2,w-4,h-4,PROGRESS_GOLD);
        round(x+4,y+4,w-8,h-8,CREAM);
        round(x+6,y+6,w-12,h-12,orangeProgress ? ORANGE_EMPTY : purpleProgress ? PURPLE_EMPTY : BLUE_EMPTY);
        float inner=w-12,fill=inner*progress,radius=(h-12)/2;
        for(int row=0;row<h-12;row++) {
            float dy=row+.5f-radius;
            float inset=radius-(float)Math.sqrt(Math.max(0,radius*radius-dy*dy));
            float left=x+6+inset,right=x+6+Math.min(fill,inner-inset);
            if(right<=left)continue;
            if (orangeProgress) {
                progressBase.set(ORANGE_BOTTOM).lerp(ORANGE_TOP,(row+.5f)/(h-12));
                progressStripe.set(progressBase).lerp(Color.WHITE,.3f);
            } else {
                progressBase.set(purpleProgress ? PURPLE_FILL : BLUE_FILL);
                progressStripe.set(purpleProgress ? PURPLE_STRIPE : BLUE_STRIPE);
            }
            shapes.setColor(progressBase);shapes.rect(left,y+6+row,right-left,1);
            shapes.setColor(progressStripe);
            for(float stripe=x+6-24+row;stripe<right;stripe+=24) {
                float a=Math.max(left,stripe),b=Math.min(right,stripe+12);
                if(b>a)shapes.rect(a,y+6+row,b-a,1);
            }
        }
        Color outline = orangeProgress ? PROGRESS_EDGE : INK;
        shapes.setColor(outline);shapes.rect(x+w-3,y+13,2,25);
        shapes.triangle(x+w-1,y+38,x+w+15,y+31,x+w-1,y+24);
        shapes.setColor(orangeProgress ? PROGRESS_GOLD : MenuTheme.GOLD);shapes.triangle(x+w+1,y+35,x+w+10,y+31,x+w+1,y+27);
        float marker=x+6+fill,centerY=y+h/2;
        shapes.setColor(outline);shapes.circle(marker,centerY,23,48);
        shapes.setColor(CARD_EDGE);shapes.circle(marker,centerY,21,48);
        shapes.setColor(CREAM);shapes.circle(marker,centerY,19.5f,48);
        shapes.end();batch.begin();batch.setColor(Color.WHITE);
        batch.draw(sprites.portrait,marker-20,centerY-20,40,40);
    }

    private void round(float x, float y, float width, float height, Color color) {
        round(x, y, width, height, Math.min(width, height) / 2, color);
    }

    private void round(float x, float y, float width, float height, float radius, Color color) {
        if (width <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        shapes.setColor(color); shapes.rect(x + radius, y, width - 2 * radius, height);
        shapes.rect(x, y + radius, radius, height - 2 * radius);
        shapes.rect(x + width - radius, y + radius, radius, height - 2 * radius);
        // Quarter circles avoid painting translucent pixels more than once.
        shapes.arc(x + radius, y + radius, radius, 180, 90, 12);
        shapes.arc(x + width - radius, y + radius, radius, 270, 90, 12);
        shapes.arc(x + radius, y + height - radius, radius, 90, 90, 12);
        shapes.arc(x + width - radius, y + height - radius, radius, 0, 90, 12);
    }

    private static Texture createIcons() {
        Pixmap pixels = new Pixmap(512, 128, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        float[] speaker = {.10f,.38f,.28f,.38f,.50f,.20f,.50f,.80f,.28f,.62f,.10f,.62f};
        float[] gear = new float[64];
        for (int i = 0; i < 32; i++) {
            float angle = (i / 4 + (i % 4 == 0 ? -.42f : i % 4 == 1 ? -.23f : i % 4 == 2 ? .23f : .42f)) * MathUtils.PI / 4;
            float radius = i % 4 == 0 || i % 4 == 3 ? .31f : .41f;
            gear[i * 2] = .5f + MathUtils.cos(angle) * radius;
            gear[i * 2 + 1] = .5f + MathUtils.sin(angle) * radius;
        }
        for (int icon = 0; icon < 4; icon++) for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            float px = (x + .5f) / 128, py = (y + .5f) / 128, edge;
            if (icon == 0) edge = Math.min(polygon(px, py, gear), Math.abs(length(px - .5f, py - .5f) - .13f));
            else if (icon == 2) edge = Math.min(Math.abs(length(px - .5f, py - .5f) - .40f),
                Math.min(segment(px, py, .37f,.32f,.37f,.68f), segment(px, py, .63f,.32f,.63f,.68f)));
            else {
                edge = Math.min(polygon(px, py, speaker), Math.min(arc(px, py, .23f), arc(px, py, .38f)));
                if (icon == 3) edge = Math.min(edge, segment(px, py, .12f,.86f,.90f,.12f));
            }
            float alpha = MathUtils.clamp(.5f - (edge - .022f) * 128, 0, 1);
            pixels.setColor(CONTROL_INK.r, CONTROL_INK.g, CONTROL_INK.b, alpha);
            pixels.drawPixel(icon * 128 + x, y);
        }
        Texture texture = new Texture(pixels, true); pixels.dispose();
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear); return texture;
    }

    private static float arc(float x, float y, float radius) {
        float angle = MathUtils.clamp(MathUtils.atan2(y - .5f, x - .46f), -.88f, .88f);
        return length(x - .46f - radius * MathUtils.cos(angle), y - .5f - radius * MathUtils.sin(angle));
    }
    private static float polygon(float x, float y, float[] points) {
        float result = Float.MAX_VALUE;
        for (int i = 0; i < points.length; i += 2) {
            int j = (i + 2) % points.length;
            result = Math.min(result, segment(x, y, points[i], points[i + 1], points[j], points[j + 1]));
        }
        return result;
    }
    private static float segment(float x, float y, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float t = MathUtils.clamp(((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy), 0, 1);
        return length(x - ax - dx * t, y - ay - dy * t);
    }
    private static float length(float x, float y) { return (float) Math.sqrt(x * x + y * y); }

    @Override public void dispose() {
        if (statusCard != null) statusCard.dispose();
        regular.dispose(); icons.dispose(); shapes.dispose(); accent.dispose();
    }
}
