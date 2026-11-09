package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.stages.StageProgress;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.ui.MapCertificate;
import sangdong_lakepark.ui.MapLockArtwork;
import sangdong_lakepark.ui.MapMenuOverlay;
import sangdong_lakepark.ui.RiderArtwork;

import java.util.function.Consumer;

/** The stage map, with a separate rider following the winding road between stages. */
public final class StageSelectionScreen extends ScreenAdapter {
    private static final float WIDTH = 1280, HEIGHT = 720;
    private static final float TRAVEL_SPEED = 420;
    private static final float ENTRY_PRESS_DURATION = .16f;
    private static final float LOCK_ICON_WIDTH = 44, LOCK_BADGE_OVERLAP = 28;
    private static final StageId[] STAGES = StageId.values();
    private static final int[] MENU_KEYS = {
        Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.SPACE, Input.Keys.ENTER, Input.Keys.ESCAPE,
        Input.Keys.UP, Input.Keys.DOWN, Input.Keys.TAB
    };

    private final Stage stage = new Stage(new FitViewport(WIDTH, HEIGHT));
    private final MenuTheme theme = new MenuTheme();
    private final GameAudio audio;
    private final StageProgress progress;
    private final Consumer<StageId> onEnter;
    private final Texture background, decorationTexture, ringTexture, lockedTexture;
    private final ShaderProgram pressShader;
    private final RiderArtwork riderArtwork;
    private final MapLockArtwork lockArtwork;
    private final MapCertificate certificateArtwork, normalCertificateArtwork, hardCertificateArtwork,
        extremeCertificateArtwork;
    private final Image certificate, normalCertificate, hardCertificate, extremeCertificate;
    private final Image rider, ring;
    private final Label instructions;
    private final Group lockNotice;
    private final MapMenuOverlay menu;
    private final InputMultiplexer inputs;
    private final Vector2[] route;
    private final float[] distances;
    private final Stop[] stops = new Stop[STAGES.length];
    private final Image[] lockedShades = new Image[STAGES.length];
    private final Image[] lockedIcons = new Image[STAGES.length];
    private final Vector2 position = new Vector2();
    private final IntSet heldKeys = new IntSet();
    private int selectedIndex;
    private float travelled, target;
    private float noticeTime;
    private float arrivalTime = -1;
    private float entryPressTime = -1, pressAmount;
    private boolean entryDispatched;
    private boolean arriving, enterOnArrival, leaving;
    private Group certificateReveal;
    private StageId revealingCertificate, advanceAfterReveal;

    private final InputAdapter input = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (!isMenuKey(keycode)) return false;
            // Ignore OS key repeats: one physical press moves exactly one stage.
            if (!heldKeys.add(keycode) || leaving || certificateReveal != null) return true;
            if (keycode == Input.Keys.ESCAPE) {
                audio.playClick();
                menu.showOptions();
                return true;
            }
            if (arriving) return true;
            if (keycode == Input.Keys.LEFT) select(selectedIndex - 1, true);
            else if (keycode == Input.Keys.RIGHT) select(selectedIndex + 1, true);
            else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                audio.playClick();
                requestEntry();
            }
            return true;
        }

        @Override
        public boolean keyUp(int keycode) {
            heldKeys.remove(keycode);
            return isMenuKey(keycode);
        }
    };

    public StageSelectionScreen(GameAudio audio, StageProgress progress, Consumer<StageId> onEnter, Runnable onBack) {
        this.audio = audio;
        this.progress = progress;
        this.onEnter = onEnter;
        FileHandle layoutFile = Gdx.files.classpath("sangdong/map/map.json");
        JsonValue layout = new JsonReader().parse(layoutFile);
        float scaleX = WIDTH / layout.getFloat("referenceWidth");
        float scaleY = HEIGHT / layout.getFloat("referenceHeight");

        background = texture(layoutFile.parent().child(layout.getString("image")));
        pressShader = new ShaderProgram(layoutFile.parent().child("press.vert"), layoutFile.parent().child("press.frag"));
        if (!pressShader.isCompiled()) throw new GdxRuntimeException("Map press shader: " + pressShader.getLog());
        Image map = new Image(background) {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                if (pressAmount <= 0) {
                    super.draw(batch, parentAlpha);
                    return;
                }
                Stop stop = stops[selectedIndex];
                ShaderProgram previous = batch.getShader();
                batch.setShader(pressShader);
                try {
                    pressShader.setUniformf("u_center", stop.x / WIDTH, 1 - stop.y / HEIGHT);
                    pressShader.setUniformf("u_size", WIDTH, HEIGHT);
                    pressShader.setUniformf("u_radius", stop.size * .45f);
                    pressShader.setUniformf("u_press", pressAmount);
                    super.draw(batch, parentAlpha);
                } finally {
                    batch.flush();
                    batch.setShader(previous);
                }
            }
        };
        map.setBounds(0, 0, WIDTH, HEIGHT);
        stage.addActor(map);

        JsonValue decoration = layout.get("decoration");
        decorationTexture = new Texture(layoutFile.parent().child(decoration.getString("image")), true);
        decorationTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        Image bicycle = new Image(decorationTexture);
        bicycle.setName("park-bicycle");
        bicycle.setBounds(decoration.getFloat("x") * scaleX,
            HEIGHT - (decoration.getFloat("y") + decoration.getFloat("height")) * scaleY,
            decoration.getFloat("width") * scaleX, decoration.getFloat("height") * scaleY);
        stage.addActor(bicycle);

        JsonValue points = layout.get("route");
        route = new Vector2[points.size];
        distances = new float[points.size];
        int i = 0;
        for (JsonValue point : points) {
            route[i] = new Vector2(point.getFloat("x") * scaleX, HEIGHT - point.getFloat("y") * scaleY);
            if (i > 0) distances[i] = distances[i - 1] + route[i - 1].dst(route[i]);
            i++;
        }
        for (JsonValue node : layout.get("stages")) {
            StageId id = StageId.valueOf(node.getString("id"));
            stops[id.ordinal()] = new Stop(node.getFloat("x") * scaleX,
                HEIGHT - node.getFloat("y") * scaleY,
                (node.getFloat("radius") + 10) * 2 * scaleX,
                distances[node.getInt("stopWaypoint")]);
        }

        lockedTexture = createLockedShade();
        lockArtwork = new MapLockArtwork(layoutFile.parent().child("stage-lock.png"));
        for (StageId id : STAGES) {
            Stop stop = stops[id.ordinal()];
            Image shade = new Image(lockedTexture);
            float size = stop.size * .86f;
            shade.setBounds(stop.x - size / 2, stop.y - size / 2, size, size);
            lockedShades[id.ordinal()] = shade;
            stage.addActor(shade);
            Image locked = lockArtwork.image(LOCK_ICON_WIDTH);
            locked.setName("stage-lock-" + id.name());
            // Hang over the lower rim, raised enough to keep the icon close to the badge.
            locked.setPosition(stop.x - locked.getWidth() / 2,
                stop.y - size / 2 - locked.getHeight() + LOCK_BADGE_OVERLAP);
            lockedIcons[id.ordinal()] = locked;
            stage.addActor(locked);
        }

        ringTexture = createSelectionRing();
        ring = new Image(ringTexture);
        stage.addActor(ring);

        JsonValue sprite = layout.get("rider");
        riderArtwork = new RiderArtwork(layoutFile.parent(), sprite);
        float riderHeight = sprite.getFloat("height") * scaleY;
        rider = riderArtwork.image(riderHeight);
        stage.addActor(rider);

        JsonValue card = layout.get("certificate");
        certificateArtwork = new MapCertificate(layoutFile.parent().child(card.getString("image")));
        certificate = certificateArtwork.image(card.getFloat("width") * scaleX);
        certificate.setPosition(WIDTH - card.getFloat("right") * scaleX - certificate.getWidth(),
            card.getFloat("bottom") * scaleY);
        stage.addActor(certificate);
        normalCertificateArtwork = new MapCertificate(layoutFile.parent().child(card.getString("normalImage")));
        normalCertificate = normalCertificateArtwork.image(card.getFloat("width") * scaleX);
        normalCertificate.setName("map-certificate-3");
        normalCertificate.setPosition(WIDTH - card.getFloat("right") * scaleX - normalCertificate.getWidth(),
            card.getFloat("bottom") * scaleY);
        stage.addActor(normalCertificate);
        hardCertificateArtwork = new MapCertificate(layoutFile.parent().child(card.getString("hardImage")));
        hardCertificate = hardCertificateArtwork.image(card.getFloat("width") * scaleX);
        hardCertificate.setName("map-certificate-2");
        hardCertificate.setPosition(WIDTH - card.getFloat("right") * scaleX - hardCertificate.getWidth(),
            card.getFloat("bottom") * scaleY);
        stage.addActor(hardCertificate);
        extremeCertificateArtwork = new MapCertificate(layoutFile.parent().child(card.getString("extremeImage")));
        extremeCertificate = extremeCertificateArtwork.image(card.getFloat("width") * scaleX);
        extremeCertificate.setName("map-certificate-1");
        extremeCertificate.setPosition(WIDTH - card.getFloat("right") * scaleX - extremeCertificate.getWidth(),
            card.getFloat("bottom") * scaleY);
        stage.addActor(extremeCertificate);

        lockNotice = new Group();
        lockNotice.setName("map-lock-notice");
        lockNotice.setBounds((WIDTH - 600) / 2, HEIGHT - 104, 600, 86);
        Image noticeShadow = new Image(theme.roundedOverlay(new Color(0, 0, 0, .22f), 10));
        noticeShadow.setBounds(0, -4, 600, 86);
        lockNotice.addActor(noticeShadow);
        Image noticeBorder = new Image(theme.roundedOverlay(MenuTheme.INK, 10));
        noticeBorder.setBounds(0, 0, 600, 86);
        lockNotice.addActor(noticeBorder);
        Image noticeFill = new Image(theme.roundedOverlay(Color.valueOf("FFF5CE"), 8));
        noticeFill.setBounds(2, 2, 596, 82);
        lockNotice.addActor(noticeFill);
        Image noticeLock = lockArtwork.image(44);
        noticeLock.setPosition(24, (86 - noticeLock.getHeight()) / 2);
        lockNotice.addActor(noticeLock);
        Label noticeTitle = theme.label("아직 잠긴 스테이지예요", theme.button, MenuTheme.INK);
        noticeTitle.setBounds(94, 44, 482, 30);
        lockNotice.addActor(noticeTitle);
        instructions = theme.label("", theme.body, Color.valueOf("512B18"));
        instructions.setName("map-notice");
        instructions.setBounds(94, 13, 482, 28);
        lockNotice.addActor(instructions);
        lockNotice.setVisible(false);
        stage.addActor(lockNotice);
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : stage.getActors()) actor.setTouchable(Touchable.disabled);
        menu = new MapMenuOverlay(stage, theme, audio, heldKeys, () -> {
            leaving = true;
            enterOnArrival = false;
            onBack.run();
        });
        addStageClickTargets();
        inputs = new InputMultiplexer(stage, input);
        updateSelection();
        Gdx.app.log("StageSelection", "Loaded " + layoutFile.path() + " (layout " + layout.getInt("version")
            + ", 1280x720)");
    }

    private static Texture texture(FileHandle file) {
        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static Texture createSelectionRing() {
        Pixmap pixels = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        Color gold = Color.valueOf("D69A23");
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float distance = Vector2.dst(x + .5f, y + .5f, 128, 128);
                float alpha = MathUtils.clamp(Math.min(distance - 110, 122 - distance), 0, 1);
                Color color = distance > 113 && distance < 119 ? MenuTheme.CREAM : gold;
                pixels.setColor(color.r, color.g, color.b, alpha);
                pixels.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static boolean isMenuKey(int keycode) {
        for (int key : MENU_KEYS) if (key == keycode) return true;
        return false;
    }

    private void addStageClickTargets() {
        for (StageId id : STAGES) {
            final int index = id.ordinal();
            Stop stop = stops[index];
            final float size = stop.size * .9f;
            Actor target = new Actor() {
                @Override
                public Actor hit(float x, float y, boolean touchable) {
                    if (touchable && getTouchable() != Touchable.enabled) return null;
                    if (Vector2.dst2(x, y, size / 2, size / 2) <= size * size / 4) return this;
                    // The part of the hanging lock below the badge is also a stage target.
                    Image lock = lockedIcons[index];
                    float lockX = lock.getX() - getX(), lockY = lock.getY() - getY();
                    return lock.isVisible() && x >= lockX && x < lockX + lock.getWidth()
                        && y >= lockY && y < lockY + lock.getHeight() ? this : null;
                }
            };
            target.setName("stage-" + id.name());
            target.setBounds(stop.x - size / 2, stop.y - size / 2, size, size);
            target.addListener(new ClickListener(Input.Buttons.LEFT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (leaving || arriving || menu.isOpen() || certificateReveal != null) return;
                    // Main supplies the shared mouse click sound; selection must not play it twice.
                    select(index, false);
                    if (progress.isUnlocked(STAGES[index])) requestEntry();
                }
            });
            // The HUD (and every subsequently opened modal) keeps priority over the map hit targets.
            stage.getRoot().addActorBefore(stage.getRoot().findActor("map-keyboard-guide"), target);
        }
    }

    private void select(int index, boolean playSound) {
        if (certificateReveal != null) return;
        int next = MathUtils.clamp(index, 0, STAGES.length - 1);
        if (next == selectedIndex) return;
        if (!progress.isUnlocked(STAGES[next])) {
            instructions.setText(STAGES[next - 1].displayName() + "를 클리어하면 "
                + STAGES[next].displayName() + "에 도전할 수 있어요.");
            lockNotice.setVisible(true);
            noticeTime = 2.5f;
            Gdx.app.log("StageSelection", "Locked " + STAGES[next].displayName());
            return;
        }
        selectedIndex = next;
        enterOnArrival = false;
        if (playSound) audio.playSelection();
        updateSelection();
        Gdx.app.log("StageSelection", "Selected " + STAGES[selectedIndex].displayName());
    }

    private void updateSelection() {
        noticeTime = 0;
        Stop stop = stops[selectedIndex];
        target = stop.distance;
        ring.setBounds(stop.x - stop.size / 2, stop.y - stop.size / 2, stop.size, stop.size);
        ring.setOrigin(stop.size / 2, stop.size / 2);
        lockNotice.setVisible(false);
    }

    private void requestEntry() {
        if (!leaving && !arriving && certificateReveal == null) enterOnArrival = true;
    }

    private void updateRider(float delta) {
        float previousX = position.x;
        boolean wasMoving = travelled != target;
        // Land at the entry first; movement towards Easy starts after the bounce.
        float step = arrivalTime >= 0 ? 0 : TRAVEL_SPEED * MathUtils.clamp(delta, 0, .05f);
        if (travelled < target) travelled = Math.min(travelled + step, target);
        else travelled = Math.max(travelled - step, target);
        int segment = 1;
        while (segment < distances.length - 1 && travelled > distances[segment]) segment++;
        float fraction = (travelled - distances[segment - 1]) / (distances[segment] - distances[segment - 1]);
        position.set(route[segment - 1]).lerp(route[segment], fraction);
        if (Math.abs(position.x - previousX) > .1f) rider.setScaleX(position.x < previousX ? -1 : 1);
        rider.setPosition(position.x - rider.getWidth() / 2, position.y);
        if (arrivalTime >= 0) {
            float previousArrivalTime = arrivalTime;
            arrivalTime += Math.min(delta, .05f);
            if (previousArrivalTime < .8f && arrivalTime >= .8f) audio.stopArrival();
            float fall = MathUtils.clamp(arrivalTime / .8f, 0, 1);
            float offset = (HEIGHT + rider.getHeight() - position.y) * (1 - fall * fall);
            if (arrivalTime >= .8f) {
                float bounce = MathUtils.clamp((arrivalTime - .8f) / .3f, 0, 1);
                offset = (float) Math.sin(bounce * Math.PI) * 16 * (1 - bounce);
            }
            rider.setY(position.y + offset);
            if (arrivalTime >= 1.1f) {
                arrivalTime = -1;
                Gdx.app.log("StageSelection", "Arrival landed entry road");
            }
        }
        if (wasMoving && travelled == target && arrivalTime < 0) {
            if (arriving) {
                arriving = false;
                updateSelection();
            }
            Gdx.app.log("StageSelection", "Arrived on " + STAGES[selectedIndex].displayName()
                + " x=" + position.x + " y=" + position.y);
        }
        if (enterOnArrival && !arriving && travelled == target && !leaving) {
            leaving = true;
            enterOnArrival = false;
            entryPressTime = 0;
            stage.cancelTouchFocus();
            // Keep settings, home, and repeated confirmation presses out of this short entry animation.
            Gdx.input.setInputProcessor(null);
        }
    }

    private void updateEntryPress(float delta) {
        if (entryPressTime < 0) return;
        entryPressTime = Math.min(ENTRY_PRESS_DURATION, entryPressTime + MathUtils.clamp(delta, 0, .05f));
        pressAmount = Interpolation.smooth.apply(entryPressTime / ENTRY_PRESS_DURATION);
        Stop stop = stops[selectedIndex];
        ring.setScale(1 - .06f * pressAmount, 1 - .10f * pressAmount);
        ring.setY(stop.y - stop.size / 2 - 5 * pressAmount);
        rider.setY(position.y - 5 * pressAmount);
        if (entryPressTime >= ENTRY_PRESS_DURATION && !entryDispatched) {
            entryDispatched = true;
            StageId chosen = STAGES[selectedIndex];
            Gdx.app.log("StageSelection", "Pressed " + chosen.displayName() + "; starting fade");
            Gdx.app.postRunnable(() -> onEnter.accept(chosen));
        }
    }

    @Override
    public void show() {
        dismissCertificateReveal();
        audio.stopArrival();
        menu.close();
        menu.syncSound();
        arrivalTime = -1;
        arriving = false;
        leaving = false;
        enterOnArrival = false;
        entryPressTime = -1;
        pressAmount = 0;
        entryDispatched = false;
        ring.setScale(1);
        heldKeys.clear();
        // An Enter held on the previous controls page must not enter a stage too.
        for (int key : MENU_KEYS) if (Gdx.input.isKeyPressed(key)) heldKeys.add(key);
        for (StageId id : STAGES) {
            boolean locked = !progress.isUnlocked(id);
            lockedShades[id.ordinal()].setVisible(locked);
            lockedIcons[id.ordinal()].setVisible(locked);
        }
        // Saved clears determine the grade, independently of the selected or replayed stage.
        boolean normalCleared = progress.isUnlocked(StageId.HARD);
        boolean hardCleared = progress.isUnlocked(StageId.EXTREME);
        boolean extremeCleared = progress.nextChallenge() == null;
        certificate.setVisible(!normalCleared);
        normalCertificate.setVisible(normalCleared && !hardCleared);
        hardCertificate.setVisible(hardCleared && !extremeCleared);
        extremeCertificate.setVisible(extremeCleared);
        updateSelection();
        travelled = target;
        position.setZero();
        rider.setScaleX(1);
        updateRider(0);
        Gdx.input.setInputProcessor(inputs);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
        showPendingCertificate();
        Gdx.app.log("StageSelection", "Showing " + STAGES[selectedIndex].displayName());
    }

    public void showWithArrival() {
        selectedIndex = 0;
        show();
        arriving = true;
        audio.playArrival();
        arrivalTime = 0;
        travelled = 0;
        position.setZero();
        updateRider(0);
        Gdx.app.log("StageSelection", "Arrival started entry road");
    }

    /** Return to the cleared badge, then follow the road to the next unlocked stage. */
    public void showAfterClear(StageId clearedStage) {
        selectedIndex = clearedStage.ordinal();
        show();
        if (certificateReveal != null) advanceAfterReveal = clearedStage;
        else travelAfterClear(clearedStage);
    }

    private void travelAfterClear(StageId clearedStage) {
        int next = selectedIndex + 1;
        if (next < STAGES.length && progress.isUnlocked(STAGES[next])) {
            select(next, false);
            arriving = travelled != target;
            Gdx.app.log("StageSelection", "Clear return: " + clearedStage.displayName()
                + " -> " + STAGES[next].displayName());
        }
    }

    private void showPendingCertificate() {
        StageId earned = progress.pendingCertificate();
        if (earned == null) return;
        revealingCertificate = earned;
        MapCertificate artwork = earned == StageId.EASY ? certificateArtwork
            : earned == StageId.NORMAL ? normalCertificateArtwork : hardCertificateArtwork;
        certificateReveal = new Group();
        certificateReveal.setName("map-first-clear-certificate-" + earned.name());
        certificateReveal.setBounds(0, 0, WIDTH, HEIGHT);
        certificateReveal.setTouchable(Touchable.enabled);
        certificateReveal.setColor(1, 1, 1, 0);
        Image shade = new Image(theme.overlay(new Color(0, 0, 0, .62f)));
        shade.setBounds(0, 0, WIDTH, HEIGHT);
        shade.setTouchable(Touchable.disabled);
        certificateReveal.addActor(shade);
        Image card = artwork.image(WIDTH * .4f);
        card.setPosition((WIDTH - card.getWidth()) / 2, (HEIGHT - card.getHeight()) / 2);
        certificateReveal.addActor(card);
        certificateReveal.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) { return true; }
            @Override public boolean keyDown(InputEvent event, int keycode) { return true; }
            @Override public boolean keyUp(InputEvent event, int keycode) { return true; }
        });
        stage.cancelTouchFocus();
        stage.addActor(certificateReveal);
        stage.setKeyboardFocus(certificateReveal);
        certificateReveal.addAction(Actions.sequence(
            Actions.fadeIn(.3f, Interpolation.smooth), Actions.delay(2),
            Actions.fadeOut(.7f, Interpolation.smooth), Actions.run(this::finishCertificateReveal)));
        Gdx.app.log("StageSelection", "Revealing first-clear certificate " + earned.displayName());
    }

    private void finishCertificateReveal() {
        progress.acknowledgeCertificate(revealingCertificate);
        certificateReveal.remove();
        certificateReveal = null;
        revealingCertificate = null;
        stage.setKeyboardFocus(null);
        heldKeys.clear();
        for (int key : MENU_KEYS) if (Gdx.input.isKeyPressed(key)) heldKeys.add(key);
        if (progress.pendingCertificate() != null) {
            showPendingCertificate();
        } else if (advanceAfterReveal != null) {
            StageId cleared = advanceAfterReveal;
            advanceAfterReveal = null;
            travelAfterClear(cleared);
        }
    }

    private void dismissCertificateReveal() {
        if (certificateReveal != null) certificateReveal.remove();
        certificateReveal = null;
        revealingCertificate = null;
        advanceAfterReveal = null;
    }

    @Override
    public void hide() {
        dismissCertificateReveal();
        audio.stopArrival();
        menu.close();
        if (Gdx.input.getInputProcessor() == inputs) Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, false);
        heldKeys.clear();
    }

    @Override
    public void render(float delta) {
        // Also recover from a release lost while the window did not have focus.
        for (int key : MENU_KEYS) if (!Gdx.input.isKeyPressed(key)) heldKeys.remove(key);
        if (!menu.isOpen() && noticeTime > 0) {
            noticeTime -= Math.min(delta, .05f);
            if (noticeTime <= 0) updateSelection();
        }
        if (!menu.isOpen() && certificateReveal == null) updateRider(delta);
        updateEntryPress(delta);
        ScreenUtils.clear(MenuTheme.INK);
        stage.getViewport().apply();
        stage.act(audio.isPaused() ? 0 : MathUtils.clamp(delta, 0, .05f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        hide();
        menu.dispose();
        stage.dispose();
        theme.dispose();
        background.dispose();
        pressShader.dispose();
        decorationTexture.dispose();
        riderArtwork.dispose();
        lockArtwork.dispose();
        certificateArtwork.dispose();
        normalCertificateArtwork.dispose();
        hardCertificateArtwork.dispose();
        extremeCertificateArtwork.dispose();
        ringTexture.dispose();
        lockedTexture.dispose();
    }

    private static Texture createLockedShade() {
        Pixmap pixels = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float edge = MathUtils.clamp(125 - Vector2.dst(x + .5f, y + .5f, 128, 128), 0, 1);
                pixels.setColor(.08f, .16f, .11f, .55f * edge);
                pixels.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private static final class Stop {
        final float x, y, size, distance;

        Stop(float x, float y, float size, float distance) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.distance = distance;
        }
    }
}
