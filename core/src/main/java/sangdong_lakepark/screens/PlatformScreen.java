package sangdong_lakepark.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.ui.StageArtwork;
import sangdong_lakepark.ui.EasyHud;
import sangdong_lakepark.ui.EasyClearView;
import sangdong_lakepark.ui.EasyPauseView;
import sangdong_lakepark.ui.EasyBicycleTrail;
import sangdong_lakepark.ui.EasySprites;
import sangdong_lakepark.ui.DialogArtwork;
import sangdong_lakepark.ui.ExitConfirmation;
import sangdong_lakepark.ui.PopupAnimation;
import sangdong_lakepark.ui.ButtonAccent;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.world.PlatformMap;
import sangdong_lakepark.world.StageRun;
import sangdong_lakepark.world.SurfaceRun;
import sangdong_lakepark.world.HardRun;
import sangdong_lakepark.world.HardThunder;
import sangdong_lakepark.world.ExtremeRun;
import sangdong_lakepark.world.HardBoxes.Vehicle;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.stages.StageStartSequence;
import sangdong_lakepark.ui.StageStartOverlay;

/** Easy gameplay, HUD, hit feedback, menus and clear/fail results. */
public class PlatformScreen extends ScreenAdapter {
    private static final Color LETTERBOX = Color.valueOf("202428");
    protected final PlatformMap map;
    private final StageId stageId;
    protected final StageRun run;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(PlatformMap.VIEW_WIDTH, PlatformMap.VIEW_HEIGHT, camera);
    private final Stage ui = new Stage(new FitViewport(PlatformMap.VIEW_WIDTH, PlatformMap.VIEW_HEIGHT));
    private final SpriteBatch batch = new SpriteBatch();
    private final MenuTheme theme = new MenuTheme("fonts/pretendard-jp/PretendardJP-Bold.otf");
    private final StageArtwork artwork;
    private final EasySprites sprites = new EasySprites();
    private final EasyBicycleTrail trail = new EasyBicycleTrail();
    private final StageStartSequence startSequence = new StageStartSequence();
    private final StageStartOverlay startOverlay = new StageStartOverlay();
    private final Matrix4 feedbackProjection = new Matrix4();
    private final Rectangle player;
    private final Runnable onMap, onHome, onClear, onClearedMap;
    private final GameAudio audio;
    private final EasyHud hud;
    private final Array<Button> modalButtons = new Array<>();
    private DialogArtwork dialogs;
    private ButtonAccent buttonAccent;
    private EasyClearView clearView;
    private EasyPauseView pauseView;
    private Image soundOff;
    private Table modal;
    private Group modalPanel;
    private String modalKind;
    private boolean systemPaused, debug, visible, waterSoundActive;
    private HardThunder.Cloud thunderSoundCloud;

    protected PlatformScreen(GameAudio audio, StageId stageId, PlatformMap map, StageRun run, StageArtwork artwork,
                             Runnable onMap, Runnable onHome, Runnable onClear, Runnable onClearedMap) {
        this.stageId = stageId; this.map = map; this.run = run; this.artwork = artwork; this.player = run.player;
        this.audio = audio; this.onMap = onMap; this.onHome = onHome; this.onClear = onClear;
        this.onClearedMap = onClearedMap;
        hud = new EasyHud(theme, sprites, () -> showModal("settings"), this::toggleSound,
            () -> { if (modal == null) showModal("pause"); else closeModal(); }, stageId);
        hud.syncSound(audio); ui.addActor(hud);
        reset();
    }

    private void reset() {
        run.reset(); trail.clear(); startSequence.reset(); waterSoundActive = false; thunderSoundCloud = null;
        if (visible) { startStageAudio(); if (stageId == StageId.EASY) audio.playArrival(); }
        hud.setLives(EasyHud.MAX_LIVES); hud.setTouchable(Touchable.childrenOnly);
        hud.setStats(run.getScore(), map.coins.size, run.getElapsed()); followPlayer();
    }

    private void toggleSound() {
        audio.toggleEnabled(); hud.syncSound(audio);
        if (soundOff != null) soundOff.setVisible(!audio.isEnabled());
    }

    private void showModal(String kind) {
        showModal(kind, true);
    }

    private void showModal(String kind, boolean animate) {
        if (modal != null) modal.remove();
        modalKind = kind; modalButtons.clear(); soundOff = null; modalPanel = null;
        modal = new Table(); modal.setName("easy-" + kind); modal.setFillParent(true);
        modal.setTouchable(Touchable.enabled);
        boolean result = kind.equals("result"), success = result && run.isCleared();
        if (result) {
            audio.resume();
            if (success) audio.playStageClear();
            else audio.playFail();
        } else { audio.pause(); audio.playWindow(); }
        modal.setBackground(theme.overlay(new Color(0, 0, 0, result ? .12f : kind.equals("pause") ? .42f : .38f)));
        if (kind.equals("pause")) {
            if (clearView == null) clearView = new EasyClearView();
            if (pauseView == null) pauseView = new EasyPauseView(clearView);
            modalPanel = pauseView.panel(this::closeModal, () -> { reset(); closeModal(); }, onMap, modalButtons);
            modal.top().add(modalPanel).size(EasyPauseView.WIDTH, EasyPauseView.HEIGHT).padTop(140);
            ui.addActor(modal); modal.validate(); ui.setKeyboardFocus(modalButtons.first());
            Gdx.app.log("Easy", "Opened pause"); return;
        }
        if (result) {
            if (clearView == null) clearView = new EasyClearView();
            Runnable retry = () -> { reset(); closeModal(); };
            modalPanel = success ? clearView.panel(run, map.coins.size, retry, onClearedMap, modalButtons)
                : clearView.failPanel(run, map.coins.size, retry, onMap, modalButtons);
            modal.top().add(modalPanel).size(EasyClearView.WIDTH, EasyClearView.HEIGHT).padTop(142);
            ui.addActor(modal); modal.validate(); ui.setKeyboardFocus(modalButtons.first());
            Gdx.app.log("Easy", "Opened result"); return;
        }
        showArtworkModal(kind, animate);
    }

    private void showArtworkModal(String kind, boolean animate) {
        if (dialogs == null) dialogs = new DialogArtwork();
        if (buttonAccent == null) buttonAccent = new ButtonAccent();
        boolean quitting = kind.equals("quit");
        DialogArtwork.Page page = dialogs.page(quitting ? "exit-confirm" : kind.equals("settings") ? "options" : "manual");
        Group panel;
        if (quitting) {
            ExitConfirmation confirmation = new ExitConfirmation(theme, page, buttonAccent, () -> Gdx.app.exit(),
                () -> showModal("settings", false));
            panel = confirmation;
            modalButtons.add(confirmation.yes);
            modalButtons.add(confirmation.no);
            modalButtons.add(confirmation.close);
        } else {
            panel = new Group(); panel.setSize(page.width, page.height);
            panel.setTouchable(Touchable.childrenOnly);
            panel.addActor(page.image());
        }
        if (kind.equals("settings")) {
            Rectangle area = page.soundIndicatorBounds();
            soundOff = new Image(page.soundOffDrawable());
            soundOff.setBounds(area.x, area.y, area.width, area.height);
            soundOff.setTouchable(Touchable.disabled); soundOff.setVisible(!audio.isEnabled()); panel.addActor(soundOff);
            artworkButton(panel, page, "controls", () -> showModal("controls"));
            artworkButton(panel, page, "sound", this::toggleSound);
            artworkButton(panel, page, "mainMenu", onHome);
            artworkButton(panel, page, "quit", () -> showModal("quit"));
            artworkButton(panel, page, "close", this::closeModal);
        } else if (!quitting) {
            artworkButton(panel, page, "back", () -> showModal("settings", false));
            artworkButton(panel, page, "close", () -> showModal("settings", false));
        }
        modal.add(panel).size(page.width, page.height); ui.addActor(modal); modal.validate();
        modalPanel = panel;
        if (animate && (quitting || kind.equals("settings"))) PopupAnimation.open(panel);
        ui.setKeyboardFocus(quitting ? modalButtons.get(1) : modalButtons.first());
        Gdx.app.log("Easy", "Opened " + kind + " (shared artwork)");
    }

    private void artworkButton(Group panel, DialogArtwork.Page page, String name, Runnable action) {
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.over = theme.roundedOverlay(new Color(1, 1, 1, .12f), page.buttonRadius(name));
        style.focused = style.over; style.down = theme.roundedOverlay(new Color(0, 0, 0, .10f), page.buttonRadius(name));
        Button button = new Button(style); button.setName("easy-" + name);
        Rectangle bounds = page.buttonBounds(name); button.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        button.addListener(new ClickListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (pointer == -1) ui.setKeyboardFocus(button);
            }
        });
        modalButtons.add(button); panel.addActor(button);
        if (!page.hasPaintedButtonAccent()) panel.addActor(buttonAccent.forButton(button));
    }

    private void closeModal() {
        boolean wasOpen = modal != null;
        if (modal != null) modal.remove();
        modal = null; modalPanel = null; modalKind = null; soundOff = null; modalButtons.clear(); ui.setKeyboardFocus(null);
        if (!systemPaused) audio.resume();
        if (wasOpen) audio.playWindow();
    }

    private void update(float dt) {
        if (systemPaused || dt <= 0) return;
        if (PopupAnimation.isOpening(modalPanel)) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debug = !debug;
        if (debug && modal == null) for (int section = 0; section < map.sectionStarts.length; section++) {
            if (Gdx.input.isKeyJustPressed(section==9?Input.Keys.NUM_0:Input.Keys.NUM_1+section)) {
                run.jumpToSection(section); trail.clear(); waterSoundActive = false; startStageAudio();
                if (section == 0 && !(run instanceof ExtremeRun)) startSequence.reset(); else startSequence.finish();
                if (!startSequence.isActive()) audio.finishStageStart();
                if (run.isArriving() && stageId == StageId.EASY) audio.playArrival();
                hud.setTouchable(Touchable.childrenOnly); followPlayer(); return;
            }
        }
        if (run.isArriving() && modal == null) {
            advanceRun(dt, 0, false);
            followPlayer();
            return;
        }
        // Finished attempts can still fade the last hit feedback.
        if ("result".equals(modalKind)) advanceRun(dt, 0, false);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (modal != null) {
                if ("result".equals(modalKind)) {
                    if (run.isCleared()) onClearedMap.run(); else onMap.run();
                }
                else if ("controls".equals(modalKind) || "quit".equals(modalKind)) showModal("settings", false);
                else closeModal();
            }
            else if (!run.isCleared()) showModal("pause");
            return;
        }
        if (modal != null) {
            boolean result = "result".equals(modalKind);
            if ("quit".equals(modalKind) && (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))) {
                Button target = modalButtons.get(Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? 0 : 1);
                if (ui.getKeyboardFocus() != target && ui.setKeyboardFocus(target)) audio.playSelection();
                return;
            }
            int direction = Gdx.input.isKeyJustPressed(Input.Keys.UP) || result && Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? -1
                : Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)
                    || result && Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1 : 0;
            int index = ui.getKeyboardFocus() instanceof Button ? modalButtons.indexOf((Button) ui.getKeyboardFocus(), true) : -1;
            if (direction != 0) {
                Button target = modalButtons.get((index + direction + modalButtons.size) % modalButtons.size);
                if (ui.getKeyboardFocus() != target && ui.setKeyboardFocus(target)) audio.playSelection();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                Button selected = ui.getKeyboardFocus() instanceof Button ? (Button) ui.getKeyboardFocus() : modalButtons.first();
                selected.fire(new ChangeListener.ChangeEvent());
            }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && !run.isCleared()) { reset(); return; }
        if (startSequence.isActive()) {
            boolean wasReady = startSequence.isReady();
            startSequence.update(dt);
            if (wasReady && !startSequence.isReady() && startSequence.isActive()) audio.playGo();
            if (!startSequence.isActive()) {
                audio.finishStageStart();
                if(run instanceof ExtremeRun)((ExtremeRun)run).finishIntro();
            }
            return;
        }
        float direction = (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0);
        boolean wasCleared = run.isCleared();
        advanceRun(dt, direction, Gdx.input.isKeyJustPressed(Input.Keys.SPACE));
        if (!wasCleared && run.isCleared()) {
            onClear.run(); hud.setTouchable(Touchable.disabled); showModal("result");
        } else if (run.isFailed()) {
            hud.setTouchable(Touchable.disabled); showModal("result");
        }
        followPlayer();
    }

    private void advanceRun(float dt, float direction, boolean jump) {
        int score = run.getScore(), falls = run.getFallCount(), mounts = run.getBicycleMountCount();
        int jumps = run.getJumpCount(), damage = run.getDamageCount();
        int bumps = run.getBumpCount(), pickups = run.getPickupCount();
        int falling = run.rocks.getFallingSoundCount(), rolling = run.rocks.getRollingSoundCount();
        int waterEntries = run.getWaterEntryCount();
        SurfaceRun normal = run instanceof SurfaceRun ? (SurfaceRun)run : null;
        HardRun hard = run instanceof HardRun ? (HardRun)run : null;
        ExtremeRun extreme = run instanceof ExtremeRun ? (ExtremeRun)run : null;
        int blueShots=extreme==null?0:extreme.beacons.getShotCount();
        int blueHits=extreme==null?0:extreme.beacons.getHitCount();
        int wind=hard==null?0:hard.wind.getGustCount(), laser=hard==null?0:hard.laser.getShotCount();
        int thunder=hard==null?0:hard.thunder.getVisibleStrikeCount(), chase=hard==null?0:hard.boulder.getLaunchCount();
        int bridges=hard==null?0:hard.getCollapseCount();
        int chaseBreaks=hard==null?0:hard.boulder.getBreakCount();
        int shots = normal == null ? 0 : normal.stings.getShotCount();
        int launches = normal == null ? 0 : normal.dog.getLaunchCount();
        int mud = normal == null ? 0 : normal.getMudEntryCount();
        int puddles = normal == null ? 0 : normal.getPuddleEntryCount();
        boolean wasFailed = run.isFailed();
        boolean wasArriving = run.isArriving();
        float previousX = player.x;
        run.update(dt, direction, jump);
        if(run.isRiding())trail.update(dt, run, player.x - previousX, falls != run.getFallCount());
        else trail.clear();
        if (wasArriving && !run.isArriving()) audio.stopArrival();
        if (!wasFailed && run.isFailed()) { waterSoundActive = false; audio.playFail(); return; }
        float left = MathUtils.clamp(player.x + player.width / 2, PlatformMap.VIEW_WIDTH / 2,
            map.worldWidth() - PlatformMap.VIEW_WIDTH / 2) - PlatformMap.VIEW_WIDTH / 2;
        boolean motorized = map.usesScooter() || hard != null
            && (hard.vehicle() == Vehicle.SCOOTER || hard.vehicle() == Vehicle.MOTORCYCLE);
        boolean moving = direction != 0 && player.x != previousX && falls == run.getFallCount();
        audio.syncRiding(run.isRiding() && !run.isCleared() && !run.isFailed(), motorized, moving);
        for (int i = falling; i < run.rocks.getFallingSoundCount(); i++) audio.playFallingRock();
        for (int i = rolling; i < run.rocks.getRollingSoundCount(); i++) audio.playRollingRock();
        for (int i = waterEntries; i < run.getWaterEntryCount(); i++) audio.playWaterEntry();
        for (int i = jumps; i < run.getJumpCount(); i++) audio.playJump();
        for (int i = damage; i < run.getDamageCount(); i++) audio.playDamage();
        for (int i = bumps; i < run.getBumpCount(); i++) audio.playBump();
        for (int i = pickups; i < run.getPickupCount(); i++) audio.playPickup();
        for (int i = score; i < run.getScore(); i++) audio.playCoin();
        for (int i = falls; i < run.getFallCount(); i++) audio.playFall();
        for (int i = mounts; i < run.getBicycleMountCount(); i++) {
            if(hard!=null&&hard.vehicle()==Vehicle.MOTORCYCLE)audio.playMotorcycle();
            else if(map.usesScooter()||hard!=null&&hard.vehicle()==Vehicle.SCOOTER)audio.playKickboard();else audio.playBicycle();
        }
        if (normal != null) {
            for (int i=shots;i<normal.stings.getShotCount();i++) audio.playSting();
            for (int i=launches;i<normal.dog.getLaunchCount();i++) audio.playDog();
            for (int i=mud;i<normal.getMudEntryCount();i++) audio.playMud();
            for (int i=puddles;i<normal.getPuddleEntryCount();i++) audio.playWaterEntry();
        }
        if(hard!=null){
            for(int i=wind;i<hard.wind.getGustCount();i++)audio.playWind();
            for(int i=laser;i<hard.laser.getShotCount();i++)audio.playLaser();
            for(int i=thunder;i<hard.thunder.getVisibleStrikeCount();i++) {
                thunderSoundCloud = hard.thunder.getLastVisibleStrike(); audio.playThunder();
            }
            if (thunderSoundCloud != null && !thunderSoundCloud.visible(left, left + PlatformMap.VIEW_WIDTH)) {
                audio.stopThunder(); thunderSoundCloud = null;
            }
            for(int i=chase;i<hard.boulder.getLaunchCount();i++)audio.playRollingRock();
            for(int i=bridges;i<hard.getCollapseCount();i++)audio.playFallingRock();
            for(int i=chaseBreaks;i<hard.boulder.getBreakCount();i++)audio.playFallingRock();
        }
        if(extreme!=null){
            for(int i=blueShots;i<extreme.beacons.getShotCount();i++)audio.playBlueLaser();
            for(int i=blueHits;i<extreme.beacons.getHitCount();i++)audio.playCoin();
        }
        boolean pouring = false;
        for (PlatformMap.WaterStream water : map.waterStreams)
            pouring |= !run.isFailed() && !run.isCleared() && water.active(run.getElapsed())
                && water.x + water.width >= left && water.x <= left + PlatformMap.VIEW_WIDTH;
        if (pouring && !waterSoundActive) audio.playWaterStream();
        else if (!pouring && waterSoundActive) audio.stopWaterStream();
        waterSoundActive = pouring;
    }

    private void followPlayer() {
        if (run.isCleared() || run.isFailed()) return;
        camera.position.set(MathUtils.clamp(player.x + player.width / 2, PlatformMap.VIEW_WIDTH / 2,
            map.worldWidth() - PlatformMap.VIEW_WIDTH / 2), PlatformMap.VIEW_HEIGHT / 2, 0); camera.update();
    }

    @Override public void render(float delta) {
        float dt = Math.min(delta, 1f / 30f); update(delta); ScreenUtils.clear(LETTERBOX);
        float cameraX = camera.position.x, cameraY = camera.position.y;
        float feedback = run.getDamageFeedback();
        if(run instanceof HardRun)feedback=Math.max(feedback,((HardRun)run).boulder.shake());
        camera.position.add(MathUtils.sin((1 - feedback) * 55) * feedback * 7,
            MathUtils.cos((1 - feedback) * 70) * feedback * 4, 0); camera.update();
        ui.getViewport().apply(); batch.setProjectionMatrix(ui.getCamera().combined); batch.begin();
        artwork.drawBackground(batch, camera.position.x, map.worldWidth()); batch.end();
        viewport.apply();
        artwork.drawBehindTerrain(camera.combined,map,run);
        batch.setProjectionMatrix(camera.combined); batch.begin();
        artwork.drawTerrain(batch, camera.position.x, map); batch.end();
        artwork.drawHazards(camera.combined, map, run);
        batch.begin(); artwork.drawCoins(batch, map, run); artwork.drawRockSprites(batch, run.rocks); artwork.drawGoal(batch, map, theme.small); batch.end();
        artwork.drawRidingAccessory(camera.combined, run);
        batch.begin(); artwork.drawItems(batch, map, run); artwork.drawBirds(batch, run.rocks); artwork.drawAdditionalSprites(batch, run);
        artwork.drawBicycleTrail(batch, trail);
        boolean bicycleRider = run.isRiding() && (run instanceof HardRun ? ((HardRun)run).vehicle()==Vehicle.BICYCLE : !map.usesScooter());
        TextureRegion mountedSprite = artwork.ridingSprite(run);
        TextureRegion character = run.isRiding() && mountedSprite != null ? mountedSprite
            : bicycleRider ? sprites.bikePlayer : sprites.player;
        float spriteHeight = player.height;
        float width = spriteHeight * character.getRegionWidth() / character.getRegionHeight();
        float x = player.x + (player.width - width) / 2;
        if(run instanceof HardRun&&((HardRun)run).getStunTime()>0)x+=MathUtils.sin((float)run.getElapsed()*60)*2;
        float footOverlap = bicycleRider ? 0 : EasySprites.PLAYER_FOOT_OVERLAP;
        float rideLift = run.isRiding() && map.usesScooter() && mountedSprite == null ? 22 : 0;
        if (run.isInvulnerable() || run instanceof HardRun && ((HardRun)run).getDismountFlash()>0) batch.setColor(1, 1, 1, MathUtils.sin((float) run.getElapsed() * 24) > 0 ? .45f : 1);
        batch.draw(character, run.isFacingLeft() ? x + width : x, player.y + player.height - spriteHeight - footOverlap + rideLift,
            run.isFacingLeft() ? -width : width, spriteHeight);
        batch.setColor(Color.WHITE); batch.end();
        artwork.drawScreenEffects(camera.combined,run);
        if (debug) artwork.drawDebug(camera.combined, map, run);
        camera.position.set(cameraX, cameraY, 0); camera.update();
        hud.setProgress(run.isCleared() ? 1 : (player.x - PlatformMap.START_X) / (map.stopX() - PlatformMap.START_X));
        hud.setStats(run.getScore(), map.coins.size, run.getElapsed());
        hud.setLives(run.getLives());
        float screenWidth = Gdx.graphics.getWidth(), screenHeight = Gdx.graphics.getHeight();
        HdpiUtils.glViewport(0, 0, (int) screenWidth, (int) screenHeight);
        artwork.drawDamageFeedback(feedbackProjection.setToOrtho2D(0, 0, screenWidth, screenHeight),
            run.getBorderFeedback(), screenWidth, screenHeight);
        ui.getViewport().apply();
        ui.act(dt); ui.draw();
        batch.setProjectionMatrix(ui.getCamera().combined);batch.begin();
        artwork.drawHud(batch,run,theme.small);batch.end();
        if (modal == null && !run.isArriving()) {
            batch.setProjectionMatrix(ui.getCamera().combined); batch.begin();
            startOverlay.draw(batch, startSequence); batch.end();
        }
    }

    private void startStageAudio() {
        if(stageId==StageId.EXTREME)audio.prepareExtreme();else if(stageId==StageId.HARD)audio.prepareHard();else if (stageId == StageId.NORMAL) audio.prepareNormal(); else audio.startEasy();
    }

    public StageRun attempt() { return run; }

    @Override public void show() {
        visible = true; waterSoundActive = false; startStageAudio();
        if (!systemPaused && modal == null) audio.resume();
        if (run.isArriving() && stageId == StageId.EASY) audio.playArrival();
        Gdx.input.setInputProcessor(ui); Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
    }
    @Override public void hide() {
        boolean wasVisible = visible; visible = false;
        if (modal != null) closeModal();
        if (wasVisible) audio.endStage();
        if (Gdx.input.getInputProcessor() == ui) Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, false);
    }
    @Override public void pause() { systemPaused = true; audio.pause(); }
    @Override public void resume() {
        systemPaused = false;
        if (modal != null && !"result".equals(modalKind)) audio.pause(); else audio.resume();
    }
    @Override public void resize(int width, int height) {
        viewport.update(width, height, false); followPlayer(); ui.getViewport().update(width, height, true);
    }
    @Override public void dispose() {
        hide(); ui.dispose(); batch.dispose(); hud.dispose(); sprites.dispose(); artwork.dispose(); theme.dispose(); startOverlay.dispose();
        if (dialogs != null) dialogs.dispose();
        if (buttonAccent != null) buttonAccent.dispose();
        if (clearView != null) clearView.dispose();
        if (pauseView != null) pauseView.dispose();
    }
}
