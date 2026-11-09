package sangdong_lakepark;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import sangdong_lakepark.audio.GameAudio;
import sangdong_lakepark.screens.EasyScreen;
import sangdong_lakepark.screens.NormalScreen;
import sangdong_lakepark.screens.HardScreen;
import sangdong_lakepark.screens.ExtremeScreen;
import sangdong_lakepark.screens.EndingScreen;
import sangdong_lakepark.dialogue.DialogueScript;
import sangdong_lakepark.screens.DialogueScreen;
import sangdong_lakepark.screens.StageSelectionScreen;
import sangdong_lakepark.screens.TitleScreen;
import sangdong_lakepark.screens.StartupScreen;
import sangdong_lakepark.stages.StageId;
import sangdong_lakepark.stages.StageProgress;
import sangdong_lakepark.stages.StageStartSequence;
import sangdong_lakepark.ui.StageStartOverlay;
import sangdong_lakepark.ui.GameCursor;
import sangdong_lakepark.ui.MenuTheme;
import sangdong_lakepark.ui.ScreenFade;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private static final float WORLD_WIDTH = 2600;
    private static final float MOVE_SPEED = 260;
    private static final float JUMP_SPEED = 650;
    private static final float GRAVITY = 1700;

    private SpriteBatch batch;
    private Texture playerTexture, blockTexture, flagTexture;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private FitViewport hudViewport;
    private BitmapFont hudFont;
    private MenuTheme hudTheme;
    private Drawable clearBackdrop, clearPanel;
    private final GlyphLayout hudText = new GlyphLayout();
    private String clearMessage;

    private final Rectangle player = new Rectangle(80, 60, 32, 44);
    private final Rectangle flag = new Rectangle(2420, 344, 20, 100);
    private final Array<Rectangle> blocks = new Array<>();
    private float velocityX, velocityY;
    private boolean grounded;
    private boolean gameOver;
    private TitleScreen titleScreen;
    private StartupScreen startupScreen;
    private DialogueScreen introDialogueScreen, normalDialogueScreen, hardDialogueScreen, extremeDialogueScreen;
    private StageSelectionScreen stageSelectionScreen;
    private EasyScreen easyScreen;
    private NormalScreen normalScreen;
    private HardScreen hardScreen;
    private ExtremeScreen extremeScreen;
    private EndingScreen endingScreen;
    private StageId currentStage = StageId.EASY;
    private StageProgress progress;
    private View view = View.STARTUP;
    private GameCursor cursor;
    private GameAudio audio;
    private ScreenFade fade;
    private final StageStartSequence startSequence = new StageStartSequence();
    private StageStartOverlay startOverlay;

    private enum View { STARTUP, TITLE, INTRO, NORMAL_DIALOGUE, HARD_DIALOGUE, EXTREME_DIALOGUE, STAGE_SELECT, EASY_GAMEPLAY, NORMAL_GAMEPLAY, HARD_GAMEPLAY, EXTREME_GAMEPLAY, ENDING, GAMEPLAY }

    @Override
    public void create() {
        audio = new GameAudio();
        progress = new StageProgress();
        cursor = new GameCursor();
        fade = new ScreenFade();
        startOverlay = new StageStartOverlay();
        fade.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch = new SpriteBatch();
        playerTexture = new Texture(Gdx.files.internal("player.png"));
        blockTexture = new Texture(Gdx.files.internal("block.png"));
        flagTexture = new Texture(Gdx.files.internal("flag.png"));

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        viewport = new FitViewport(800, 600, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        hudViewport = new FitViewport(1280, 720);
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        hudTheme = new MenuTheme();
        hudFont = hudTheme.small;
        hudFont.setColor(MenuTheme.INK);
        clearBackdrop = hudTheme.overlay(new Color(.10f, .22f, .17f, .45f));
        clearPanel = hudTheme.roundedOverlay(MenuTheme.CREAM, 24);

        // x, y, 너비, 높이. 바닥도 같은 충돌 블록입니다.
        blocks.add(new Rectangle(0, 0, 620, 60));
        blocks.add(new Rectangle(760, 0, 540, 60));
        blocks.add(new Rectangle(1440, 0, 1160, 60));
        blocks.add(new Rectangle(300, 60, 80, 64));
        blocks.add(new Rectangle(900, 60, 96, 80));
        blocks.add(new Rectangle(1080, 180, 160, 24));
        blocks.add(new Rectangle(1650, 60, 120, 70));
        blocks.add(new Rectangle(1790, 60, 120, 140));
        blocks.add(new Rectangle(1930, 60, 120, 210));
        blocks.add(new Rectangle(2100, 270, 110, 24));
        blocks.add(new Rectangle(2280, 320, 210, 24));

        startupScreen = new StartupScreen(this::prepareTitle, () -> fade.start(this::showTitle));
        startupScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        startupScreen.show();
    }


    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        fade.update(audio.isPaused() ? 0 : delta);
        cursor.update();
        if (view != View.STARTUP && !fade.isActive() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) audio.playClick();
        renderView(fade.isActive() ? 0 : delta);
        fade.draw();
    }

    private void renderView(float delta) {
        if (view == View.STARTUP) {
            startupScreen.render(audio.isPaused() ? 0 : delta);
            return;
        }
        if(view==View.ENDING){endingScreen.render(delta);return;}
        if(view==View.EXTREME_DIALOGUE){extremeDialogueScreen.render(delta);return;}
        if(view==View.EXTREME_GAMEPLAY){extremeScreen.render(delta);return;}
        if(view==View.HARD_DIALOGUE){hardDialogueScreen.render(delta);return;}
        if(view==View.HARD_GAMEPLAY){hardScreen.render(delta);return;}
        if (view == View.TITLE) {
            titleScreen.render(delta);
            return;
        }
        if (view == View.INTRO) {
            introDialogueScreen.render(delta);
            return;
        }
        if (view == View.NORMAL_DIALOGUE) {
            normalDialogueScreen.render(delta);
            return;
        }
        if (view == View.STAGE_SELECT) {
            stageSelectionScreen.render(delta);
            return;
        }
        if (view == View.EASY_GAMEPLAY) {
            easyScreen.render(delta);
            return;
        }
        if (view == View.NORMAL_GAMEPLAY) {
            normalScreen.render(delta);
            return;
        }
        if (!fade.isActive() && (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || (gameOver && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)))) {
            showStageSelection();
            stageSelectionScreen.render(delta);
            return;
        }
        // 큰 프레임 시간으로 인해 한 번에 너무 멀리 이동하는 것을 제한합니다.
        float dt = Math.min(delta, 1f / 30f);
        update(dt);

        ScreenUtils.clear(1f, 1f, 1f, 1f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Rectangle block : blocks) {
            batch.draw(blockTexture, block.x, block.y, block.width, block.height);
        }
        batch.draw(flagTexture, flag.x, flag.y, flag.width, flag.height);
        batch.draw(playerTexture, player.x, player.y, player.width, player.height);
        batch.end();

        hudViewport.apply();
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.begin();
        hudFont.draw(batch, "STAGE: " + currentStage.displayName() + "  |  ESC: 지도  |  ENTER: 다시 시작", 24, 696);
        if (gameOver) {
            clearBackdrop.draw(batch, 0, 0, 1280, 720);
            clearPanel.draw(batch, 300, 256, 680, 208);
            drawCentered(hudTheme.heading, currentStage.displayName() + " 클리어!", 425);
            drawCentered(hudTheme.body, clearMessage, 367);
            drawCentered(hudTheme.small, "ENTER / ESC: 스테이지 지도로", 307);
        }
        if (!gameOver) startOverlay.draw(batch, startSequence);
        batch.end();
    }

    private void drawCentered(BitmapFont font, String text, float y) {
        font.setColor(MenuTheme.INK);
        hudText.setText(font, text);
        font.draw(batch, hudText, (1280 - hudText.width) / 2, y);
    }

    private void update(float dt) {
        if (dt <= 0 || audio.isPaused()) return;

        // 엔터 키를 누르면 게임 재시작
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            resetGame();
        }
        if (startSequence.isActive()) {
            boolean wasReady = startSequence.isReady();
            startSequence.update(dt);
            if (wasReady && !startSequence.isReady() && startSequence.isActive()) audio.playGo();
            return;
        }

        // 깃발 도착을 클리어로 기록하고 바로 다음 스테이지만 해금합니다.
        if (!gameOver && player.overlaps(flag)) {
            gameOver = true;
            boolean advanced = progress.complete(currentStage);
            StageId next = progress.nextChallenge();
            clearMessage = next == null ? "모든 스테이지를 클리어했어요!"
                : advanced ? next.displayName() + " 스테이지가 열렸어요."
                : "다른 스테이지에도 도전해 보세요.";
            Gdx.app.log("Sangdong", "Cleared stage " + currentStage.displayName()
                + "; next=" + (next == null ? "complete" : next.displayName()));
        }

        if (gameOver) {
            velocityX = 0;
            velocityY = 0;
            return;
        }

        // 1. 좌우 이동과 지상 점프
        velocityX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocityX -= MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocityX += MOVE_SPEED;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }

        // 2. 중력: 아래쪽 속도는 음수이며 최대 낙하 속도를 제한합니다.
        velocityY = Math.max(velocityY - GRAVITY * dt, -900);

        // 3. X축과 Y축을 나누어 이동하고 충돌을 보정합니다.
        moveX(dt);
        moveY(dt);

        // 4. 낭떠러지에 떨어지면 시작점으로 복귀
        if (player.y < -100) {
            resetGame();
            return;
        }

        // 5. 일반 플레이 중 카메라 추적
        camera.position.x = MathUtils.clamp(player.x + player.width / 2, 400, WORLD_WIDTH - 400);
        camera.position.y = 300; // 기본 화면 높이의 절반
        camera.update();
    }

    private void prepareTitle() {
        if (titleScreen == null) {
            titleScreen = new TitleScreen(audio, () -> fade.start(this::showIntroDialogue));
        }
    }

    private void showTitle() {
        if (startupScreen != null) {
            startupScreen.dispose();
            startupScreen = null;
        }
        if (endingScreen != null) endingScreen.hide();
        if(extremeScreen!=null)extremeScreen.hide();
        audio.stopGo();
        if (easyScreen != null) easyScreen.hide();
        if (normalScreen != null) normalScreen.hide();
        if(hardScreen!=null)hardScreen.hide();
        if (stageSelectionScreen != null) stageSelectionScreen.hide();
        if (introDialogueScreen != null) introDialogueScreen.hide();
        if (normalDialogueScreen != null) normalDialogueScreen.hide();
        if(hardDialogueScreen!=null)hardDialogueScreen.hide();
        if(extremeDialogueScreen!=null)extremeDialogueScreen.hide();
        prepareTitle();
        titleScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        titleScreen.show();
        view = View.TITLE;
    }

    private void showIntroDialogue() {
        titleScreen.hide();
        if (introDialogueScreen == null) {
            introDialogueScreen = new DialogueScreen(audio, DialogueScript.INTRO,
                () -> fade.start(() -> showStageSelection(true)), this::showTitle);
        }
        introDialogueScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        introDialogueScreen.show();
        view = View.INTRO;
    }

    private void returnAfterClear(StageId clearedStage) {
        if (fade.isActive()) return;
        switch (clearedStage) {
            case EASY:
                if (view != View.EASY_GAMEPLAY) return;
                easyScreen.hide();
                break;
            case NORMAL:
                if (view != View.NORMAL_GAMEPLAY) return;
                normalScreen.hide();
                break;
            case HARD:
                if (view != View.HARD_GAMEPLAY) return;
                hardScreen.hide();
                break;
            default:
                return;
        }
        // Leave the result audio before returning to the cleared badge and following the road.
        fade.start(() -> showStageSelection(false, clearedStage));
    }

    private void showNormalDialogue() {
        if (normalDialogueScreen == null) {
            normalDialogueScreen = new DialogueScreen(audio, DialogueScript.NORMAL,
                () -> fade.start(this::finishNormalDialogue), () -> fade.start(this::showStageSelection));
        }
        normalDialogueScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        normalDialogueScreen.show();
        view = View.NORMAL_DIALOGUE;
    }

    private void finishNormalDialogue() {
        if (view != View.NORMAL_DIALOGUE) return;
        normalDialogueScreen.hideForStage();
        if (normalScreen != null) normalScreen.dispose();
        normalScreen = new NormalScreen(audio,
            () -> Gdx.app.postRunnable(this::showStageSelection),
            () -> Gdx.app.postRunnable(this::showTitle),
            () -> {
                progress.complete(StageId.NORMAL);
                Gdx.app.log("Sangdong", "Cleared stage Normal; Hard unlocked");
            }, () -> Gdx.app.postRunnable(() -> returnAfterClear(StageId.NORMAL)));
        normalScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        normalScreen.show();
        view = View.NORMAL_GAMEPLAY;
        Gdx.app.log("Sangdong", "Entered stage Normal (after-rain waterside trail)");
    }

    private void showStageSelection() {
        showStageSelection(false);
    }

    private void showStageSelection(boolean arrival) {
        showStageSelection(arrival, null);
    }

    private void showStageSelection(boolean arrival, StageId clearedStage) {
        if (endingScreen != null) endingScreen.hide();
        if(extremeScreen!=null)extremeScreen.hide();
        audio.stopGo();
        if (easyScreen != null) easyScreen.hide();
        if (normalScreen != null) normalScreen.hide();
        if(hardScreen!=null)hardScreen.hide();
        if (titleScreen != null) {
            titleScreen.hide();
        }
        if (introDialogueScreen != null) introDialogueScreen.hide();
        if (normalDialogueScreen != null) normalDialogueScreen.hide();
        if(hardDialogueScreen!=null)hardDialogueScreen.hide();
        if(extremeDialogueScreen!=null)extremeDialogueScreen.hide();
        if (stageSelectionScreen == null) {
            stageSelectionScreen = new StageSelectionScreen(audio, progress,
                selected -> fade.start(() -> enterStage(selected)), this::showTitle);
        }
        stageSelectionScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (clearedStage != null) stageSelectionScreen.showAfterClear(clearedStage);
        else if (arrival) stageSelectionScreen.showWithArrival();
        else stageSelectionScreen.show();
        view = View.STAGE_SELECT;
    }

    private void enterStage(StageId selected) {
        if (view != View.STAGE_SELECT || !progress.isUnlocked(selected)) return;
        stageSelectionScreen.hide();
        currentStage = selected;
        if (selected == StageId.EASY) {
            if (easyScreen != null) easyScreen.dispose();
            easyScreen = new EasyScreen(audio, () -> Gdx.app.postRunnable(this::showStageSelection),
                () -> Gdx.app.postRunnable(this::showTitle), () -> {
                progress.complete(StageId.EASY);
                Gdx.app.log("Sangdong", "Cleared stage Easy; next="
                    + (progress.nextChallenge() == null ? "complete" : progress.nextChallenge().displayName()));
            }, () -> Gdx.app.postRunnable(() -> returnAfterClear(StageId.EASY)));
            easyScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            easyScreen.show();
            view = View.EASY_GAMEPLAY;
            Gdx.app.log("Sangdong", "Entered stage Easy (sunny walking trail)");
            return;
        }
        if (selected == StageId.NORMAL) {
            showNormalDialogue();
            return;
        }
        if(selected==StageId.HARD){showHardDialogue();return;}
        if(selected==StageId.EXTREME){showExtremeDialogue();return;}
        startGameplay();
    }

    private void showHardDialogue(){
        if(hardDialogueScreen==null)hardDialogueScreen=new DialogueScreen(audio,DialogueScript.HARD,
            ()->fade.start(this::finishHardDialogue),()->fade.start(this::showStageSelection));
        hardDialogueScreen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());hardDialogueScreen.show();view=View.HARD_DIALOGUE;
    }
    private void finishHardDialogue(){
        if(view!=View.HARD_DIALOGUE)return;
        hardDialogueScreen.hideForStage();if(hardScreen!=null)hardScreen.dispose();
        hardScreen=new HardScreen(audio,()->Gdx.app.postRunnable(this::showStageSelection),
            ()->Gdx.app.postRunnable(this::showTitle),()->{progress.complete(StageId.HARD);Gdx.app.log("Sangdong","Cleared stage Hard; Extreme unlocked");},
            ()->Gdx.app.postRunnable(()->returnAfterClear(StageId.HARD)));
        hardScreen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());hardScreen.show();view=View.HARD_GAMEPLAY;
        Gdx.app.log("Sangdong","Entered stage Hard (sunset lakeside trail)");
    }

    private void showExtremeDialogue(){
        if(extremeDialogueScreen==null)extremeDialogueScreen=new DialogueScreen(audio,DialogueScript.EXTREME,
            ()->fade.start(this::finishExtremeDialogue),()->fade.start(this::showStageSelection));
        extremeDialogueScreen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        extremeDialogueScreen.show();view=View.EXTREME_DIALOGUE;
    }

    private void finishExtremeDialogue(){
        if(view!=View.EXTREME_DIALOGUE)return;
        extremeDialogueScreen.hideForStage();if(extremeScreen!=null)extremeScreen.dispose();
        extremeScreen=new ExtremeScreen(audio,()->Gdx.app.postRunnable(this::showStageSelection),
            ()->Gdx.app.postRunnable(this::showTitle),()->{
                progress.complete(StageId.EXTREME);
                Gdx.app.log("Sangdong","Cleared stage Extreme");
            },()->Gdx.app.postRunnable(this::beginEnding));
        extremeScreen.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());extremeScreen.show();view=View.EXTREME_GAMEPLAY;
        Gdx.app.log("Sangdong","Entered stage Extreme (night lakeside trail)");
    }

    private void beginEnding() {
        if (view == View.EXTREME_GAMEPLAY && extremeScreen.attempt().isCleared() && !fade.isActive())
            fade.start(this::showEnding);
    }

    private void showEnding() {
        extremeScreen.hide(); audio.stopGo();
        if (endingScreen == null) endingScreen = new EndingScreen(audio, this::restartCampaign,
            () -> { if (view == View.ENDING && !fade.isActive())
                fade.start(() -> showStageSelection(false, StageId.EXTREME)); });
        endingScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        endingScreen.show(); view = View.ENDING;
    }

    private void restartCampaign() {
        if (view != View.ENDING || fade.isActive()) return;
        fade.start(() -> {
            progress.reset(); currentStage = StageId.EASY;
            if (stageSelectionScreen != null) {
                stageSelectionScreen.dispose(); stageSelectionScreen = null;
            }
            showTitle();
        });
    }

    private void startGameplay() {
        resetGame();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        view = View.GAMEPLAY;
        Gdx.input.setInputProcessor(null);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
        Gdx.app.log("Sangdong", "Entered stage " + currentStage.displayName());
    }

    private void resetGame() {
        audio.stopGo();
        startSequence.reset();
        player.setPosition(80, 60);
        velocityX = 0;
        velocityY = 0;
        grounded = false;
        gameOver = false;
        camera.position.set(400, 300, 0);
        camera.update();
    }

    private void moveX(float dt) {
        player.x += velocityX * dt;
        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) continue;
            if (velocityX > 0) player.x = block.x - player.width;
            else if (velocityX < 0) player.x = block.x + block.width;
        }
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
    }

    private void moveY(float dt) {
        float dy = velocityY * dt;
        player.y += dy;
        grounded = false;

        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) continue;
            if (dy < 0) { // 내려오던 중: 블록 위에 착지
                player.y = block.y + block.height;
                velocityY = 0;
                grounded = true;
            } else if (dy > 0) { // 올라가던 중: 블록 아래에 머리 충돌
                player.y = block.y - player.height;
                velocityY = 0;
            }
        }
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        hudViewport.update(width, height, true);
        if (fade != null) fade.resize(width, height);
        if (startupScreen != null) startupScreen.resize(width, height);
        if (titleScreen != null) titleScreen.resize(width, height);
        if (introDialogueScreen != null) introDialogueScreen.resize(width, height);
        if (normalDialogueScreen != null) normalDialogueScreen.resize(width, height);
        if (stageSelectionScreen != null) stageSelectionScreen.resize(width, height);
        if (easyScreen != null) easyScreen.resize(width, height);
        if (normalScreen != null) normalScreen.resize(width, height);
        if(hardScreen!=null)hardScreen.resize(width,height);
        if(extremeScreen!=null)extremeScreen.resize(width,height);
        if (endingScreen != null) endingScreen.resize(width, height);
        if(hardDialogueScreen!=null)hardDialogueScreen.resize(width,height);
        if(extremeDialogueScreen!=null)extremeDialogueScreen.resize(width,height);
    }

    @Override
    public void pause() {
        audio.pause();
        if (view == View.EASY_GAMEPLAY) easyScreen.pause();
        if (view == View.NORMAL_GAMEPLAY) normalScreen.pause();
        if(view==View.HARD_GAMEPLAY)hardScreen.pause();
        if(view==View.EXTREME_GAMEPLAY)extremeScreen.pause();
    }

    @Override
    public void resume() {
        audio.resume();
        if (view == View.EASY_GAMEPLAY) easyScreen.resume();
        if (view == View.NORMAL_GAMEPLAY) normalScreen.resume();
        if(view==View.HARD_GAMEPLAY)hardScreen.resume();
        if(view==View.EXTREME_GAMEPLAY)extremeScreen.resume();
    }

    @Override
    public void dispose() {
        if (startupScreen != null) startupScreen.dispose();
        if (titleScreen != null) titleScreen.dispose();
        if (introDialogueScreen != null) introDialogueScreen.dispose();
        if (normalDialogueScreen != null) normalDialogueScreen.dispose();
        if (stageSelectionScreen != null) stageSelectionScreen.dispose();
        if (easyScreen != null) easyScreen.dispose();
        if (normalScreen != null) normalScreen.dispose();
        if(hardScreen!=null)hardScreen.dispose();
        if(extremeScreen!=null)extremeScreen.dispose();
        if (endingScreen != null) endingScreen.dispose();
        if(hardDialogueScreen!=null)hardDialogueScreen.dispose();
        if(extremeDialogueScreen!=null)extremeDialogueScreen.dispose();
        fade.dispose();
        startOverlay.dispose();
        audio.dispose();
        cursor.dispose();
        batch.dispose();
        playerTexture.dispose();
        blockTexture.dispose();
        flagTexture.dispose();
        hudTheme.dispose();
    }
}
