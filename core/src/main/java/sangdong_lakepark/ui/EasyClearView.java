package sangdong_lakepark.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import sangdong_lakepark.world.StageRun;

/** Transparent result layouts with reusable outlined fonts and gold button artwork. */
public final class EasyClearView implements Disposable {
    public static final float WIDTH = 640, HEIGHT = 386;
    private static final int BUTTON_WIDTH = 224, BUTTON_HEIGHT = 68, SCALE = 2;
    private static final Color OUTLINE = Color.valueOf("173F35"), BUTTON_INK = Color.valueOf("603917");
    private final BitmapFont titleFont, gradeFont, statsFont, buttonFont;
    private final BitmapFont failTitleFont;
    private final Texture buttons = createButtons();
    private final TextureRegionDrawable[] buttonStates = new TextureRegionDrawable[4];
    private final ButtonAccent accent = new ButtonAccent();

    public EasyClearView() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/pretendard-jp/PretendardJP-Bold.otf"));
        try {
            titleFont = font(generator, 128, 3.5f, 3, 5, "Clear! ", Color.valueOf("FFF8E5"));
            gradeFont = font(generator, 44, 2, 2, 3, "BadGoodGreatPerfect ", Color.WHITE);
            statsFont = font(generator, 26, 1.3f, 1, 2, "Time:Score:0123456789/ ", Color.WHITE);
            buttonFont = font(generator, 26, 0, 0, 1, "계속하기다시 시작스테이지 ", Color.WHITE);
            failTitleFont = font(generator, 160, 3.5f, 3, 5, "Fail ", Color.valueOf("E65353"));
        } finally { generator.dispose(); }
        for (int state = 0; state < buttonStates.length; state++) {
            TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(buttons,
                state * BUTTON_WIDTH * SCALE, 0, BUTTON_WIDTH * SCALE, BUTTON_HEIGHT * SCALE));
            drawable.setMinWidth(BUTTON_WIDTH); drawable.setMinHeight(BUTTON_HEIGHT);
            drawable.setLeftWidth(12); drawable.setRightWidth(12);
            drawable.setTopHeight(state == 2 ? 8 : 6); drawable.setBottomHeight(state == 2 ? 10 : 12);
            buttonStates[state] = drawable;
        }
    }

    private BitmapFont font(FreeTypeFontGenerator generator, int size, float border, int shadowX, int shadowY,
                            String characters, Color color) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size; p.characters = characters; p.color = color;
        p.borderWidth = border; p.borderColor = OUTLINE;
        p.shadowOffsetX = shadowX; p.shadowOffsetY = shadowY; p.shadowColor = new Color(0, .08f, .05f, .35f);
        p.minFilter = Texture.TextureFilter.Linear; p.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(p);
    }

    public Table panel(StageRun run, int total, Runnable retry, Runnable stage, Array<Button> controls) {
        Table panel = new Table(); panel.setName("easy-clear-panel"); panel.setSize(WIDTH, HEIGHT);
        panel.setTransform(true); panel.setTouchable(Touchable.childrenOnly);
        label(panel, "easy-result-title", "Clear!", titleFont, Color.WHITE, 0, 220, WIDTH, 166);
        Color grade = Color.valueOf(run.getGrade().equals("Bad") ? "E65353"
            : run.getGrade().equals("Great") ? "FFD34F" : "3D91F5");
        label(panel, "easy-result-grade", run.getGrade(), gradeFont, grade, 0, 164, WIDTH, 68);
        label(panel, "easy-result-time", "Time: " + StageRun.formatTime(run.getElapsed()), statsFont,
            Color.WHITE, 116, 112, 196, 44);
        label(panel, "easy-result-score", "Score: " + run.getScore() + " / " + total, statsFont,
            Color.WHITE, 328, 112, 196, 44);
        button(panel, controls, "스테이지", 84, stage);
        button(panel, controls, "다시 시작", 332, retry);
        return panel;
    }

    public Table failPanel(StageRun run, int total, Runnable retry, Runnable stage, Array<Button> controls) {
        Table panel = new Table(); panel.setName("easy-fail-panel"); panel.setSize(WIDTH, HEIGHT);
        panel.setTransform(true); panel.setTouchable(Touchable.childrenOnly);
        label(panel, "easy-result-title", "Fail", failTitleFont, Color.WHITE, 0, 186, WIDTH, 200);
        label(panel, "easy-result-time", "Time: " + StageRun.formatTime(run.getElapsed()), statsFont,
            Color.WHITE, 116, 112, 196, 44);
        label(panel, "easy-result-score", "Score: " + run.getScore() + " / " + total, statsFont,
            Color.WHITE, 328, 112, 196, 44);
        float width = BUTTON_WIDTH * .9f, height = BUTTON_HEIGHT * .9f;
        button(panel, controls, "다시 시작", 84 + (BUTTON_WIDTH - width) / 2, width, height, buttonStyle(), retry)
            .getLabel().setFontScale(.9f);
        button(panel, controls, "스테이지", 332 + (BUTTON_WIDTH - width) / 2, width, height, buttonStyle(), stage)
            .getLabel().setFontScale(.9f);
        return panel;
    }

    private void label(Table panel, String name, String text, BitmapFont font, Color color,
                       float x, float y, float width, float height) {
        Label label = new Label(text, new Label.LabelStyle(font, color));
        label.setName(name); label.setBounds(x, y, width, height); label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled); panel.addActor(label);
    }

    public TextButton.TextButtonStyle buttonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = buttonFont; style.fontColor = BUTTON_INK;
        style.up = buttonStates[0]; style.over = buttonStates[1];
        style.down = buttonStates[2]; style.focused = buttonStates[3];
        return style;
    }

    private void button(Table panel, Array<Button> controls, String text, float x, Runnable action) {
        button(panel, controls, text, x, BUTTON_WIDTH, BUTTON_HEIGHT, buttonStyle(), action);
    }

    private TextButton button(Table panel, Array<Button> controls, String text, float x, float width, float height,
                              TextButton.TextButtonStyle style, Runnable action) {
        TextButton button = new TextButton(text, style); button.setName("easy-" + text);
        button.setBounds(x, 0, width, height);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        button.addListener(new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (pointer == -1) button.getStage().setKeyboardFocus(button);
            }
        });
        controls.add(button); panel.addActor(button); panel.addActor(accent.forButton(button));
        return button;
    }

    private static Texture createButtons() {
        Pixmap image = new Pixmap(BUTTON_WIDTH * SCALE * 4, BUTTON_HEIGHT * SCALE, Pixmap.Format.RGBA8888);
        image.setBlending(Pixmap.Blending.None);
        Color pixel = new Color(), fill = new Color();
        Color edge = Color.valueOf("6A401D"), depth = Color.valueOf("BD7618"), focus = Color.valueOf("FFF4B0");
        for (int state = 0; state < 4; state++) for (int py = 0; py < image.getHeight(); py++)
            for (int px = 0; px < BUTTON_WIDTH * SCALE; px++) {
                float x = (px + .5f) / SCALE, y = BUTTON_HEIGHT - (py + .5f) / SCALE;
                float offset = state == 2 ? -2 : 0;
                pixel.set(0, 0, 0, 0);
                over(pixel, edge, .18f * MathUtils.clamp(1 - Math.max(0, distance(x, y, 1, 1, 222, 62)) / 2, 0, 1));
                over(pixel, edge, coverage(x, y, 0, 3, 224, 62));
                over(pixel, depth, coverage(x, y, 2, 5, 220, 58));
                over(pixel, state == 3 ? focus : edge, coverage(x, y, 0, 8 + offset, 224, 60));
                float t = MathUtils.clamp((y - 11 - offset) / 54, 0, 1);
                float light = state == 1 || state == 3 ? .05f : state == 2 ? -.07f : 0;
                fill.set(1, MathUtils.clamp(.75f + .14f * t + light, 0, 1),
                    MathUtils.clamp(.09f + .34f * t + light, 0, 1), 1);
                over(pixel, fill, coverage(x, y, 3, 11 + offset, 218, 54));
                float shine = (x - 25) * (x - 25) / 81 + (y - 53 - offset) * (y - 53 - offset) / 25;
                over(pixel, Color.WHITE, .65f * MathUtils.clamp((1 - shine) * 3, 0, 1));
                image.drawPixel(state * BUTTON_WIDTH * SCALE + px, py, Color.rgba8888(pixel));
            }
        Texture texture = new Texture(image, true); image.dispose();
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear); return texture;
    }

    private static float distance(float x, float y, float left, float bottom, float width, float height) {
        float radius = height / 2;
        float dx = Math.max(Math.abs(x - left - width / 2) - (width / 2 - radius), 0);
        return (float) Math.sqrt(dx * dx + (y - bottom - radius) * (y - bottom - radius)) - radius;
    }

    private static float coverage(float x, float y, float left, float bottom, float width, float height) {
        return MathUtils.clamp(.5f - distance(x, y, left, bottom, width, height) * SCALE, 0, 1);
    }

    private static void over(Color base, Color top, float alpha) {
        float a = alpha + base.a * (1 - alpha);
        if (a <= 0) return;
        float previous = base.a * (1 - alpha);
        base.set((top.r * alpha + base.r * previous) / a, (top.g * alpha + base.g * previous) / a,
            (top.b * alpha + base.b * previous) / a, a);
    }

    @Override public void dispose() {
        titleFont.dispose(); gradeFont.dispose(); statsFont.dispose(); buttonFont.dispose();
        failTitleFont.dispose(); buttons.dispose(); accent.dispose();
    }
}
