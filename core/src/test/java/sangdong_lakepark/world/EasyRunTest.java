package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class EasyRunTest {
    private final EasyMap map = new EasyMap();
    private final EasyRun run = new EasyRun(map);
    private void advance(int frames) { for (int i = 0; i < frames; i++) run.update(1f / 60f, 0, false); }

    @Test public void arrivalIgnoresMovementAndJumpUntilLandingAndResets() {
        EasyRun control = new EasyRun(new EasyMap());
        for (int frame = 0; frame < 45; frame++) {
            run.update(1f / 60f, 1, true); control.update(1f / 60f, 0, false);
            assertEquals(control.player.x, run.player.x, .01f); assertEquals(control.player.y, run.player.y, .01f);
        }
        advance(120); assertFalse(run.isArriving()); assertEquals(184, run.player.y, .01f);
        run.update(1f / 60f, 1, true); assertTrue(run.player.x > 260); assertTrue(run.player.y > 184);
        run.reset(); assertTrue(run.isArriving()); assertEquals(0, run.getElapsed(), 0);
    }
    @Test public void bareSecondJumpDoesNotResetAirborneVelocity() {
        advance(120); run.update(1f / 60f, 0, true); advance(18);
        EasyRun other = new EasyRun(new EasyMap());
        for (int i = 0; i < 120; i++) other.update(1f / 60f, 0, false);
        other.update(1f / 60f, 0, true);
        for (int i = 0; i < 18; i++) other.update(1f / 60f, 0, false);
        run.update(1f / 60f, 0, true); other.update(1f / 60f, 0, false);
        assertEquals(other.player.y, run.player.y, .01f);
        advance(120); run.update(1f / 60f, 0, true); assertTrue(run.player.y > 184);
    }
    @Test public void coinsScoreOnlyOnceAndGradesMatchEveryBoundary() {
        String[] grades = {"Bad","Bad","Bad","Good","Good","Good","Good","Great","Great","Great","Perfect"};
        assertEquals(10, map.coins.size); assertEquals("Bad", run.getGrade());
        for (int i = 0; i < map.coins.size; i++) {
            Rectangle coin = map.coins.get(i); assertEquals(36, coin.width, 0);
            run.player.setPosition(coin.x, coin.y); run.update(1f / 120f, 0, false);
            assertEquals(i + 1, run.getScore()); assertEquals(grades[i + 1], run.getGrade());
            run.update(1f / 120f, 0, false); assertEquals(i + 1, run.getScore());
        }
        run.reset(); assertEquals(0, run.getScore()); assertFalse(run.isCollected(0));
    }
    @Test public void everyGapUsesItsSpecifiedRespawnAndPreservesScoreAndClock() {
        for (EasyMap.Gap gap : map.gaps) {
            run.reset(); advance(120);
            Rectangle coin = map.coins.first(); run.player.setPosition(coin.x, coin.y); run.update(1f / 60f, 0, false);
            int score = run.getScore(); double elapsed = run.getElapsed();
            run.player.setPosition(gap.x + gap.width / 2 - run.player.width / 2, -101);
            run.update(1f / 120f, 0, false);
            assertEquals(4, run.getLives()); assertEquals(gap.respawn.x, run.player.x, .01f);
            assertEquals(1, run.getFallCount());
            assertEquals(gap.respawn.y, run.player.y, .01f); assertEquals(score, run.getScore());
            for (Rectangle ground : map.ground)
                assertFalse("Respawn must not place the player inside terrain", run.player.overlaps(ground));
            for (Rectangle block : map.blocks)
                assertFalse("Respawn must not place the player inside a block", run.player.overlaps(block));
            assertTrue(run.getElapsed() > elapsed); assertFalse(run.isArriving());
        }
    }
    @Test public void fifthLostLifeFailsFreezesAttemptAndRetryRestoresIt() {
        advance(120);
        for (int life = 4; life >= 0; life--) {
            run.player.setPosition(1200, -101); run.update(1f / 120f, 0, false); assertEquals(life, run.getLives());
        }
        assertTrue(run.isFailed()); assertFalse(run.isCleared()); assertTrue(run.isFinished());
        double elapsed = run.getElapsed(); run.update(5, 1, true); assertEquals(elapsed, run.getElapsed(), 0);
        run.reset(); assertEquals(5, run.getLives()); assertEquals(0, run.getScore()); assertFalse(run.isFailed());
        assertEquals(0, run.getFallCount());
        assertFalse(run.isRiding()); assertFalse(run.isItemUsed()); assertEquals(0, run.getDamageFeedback(), 0);
    }
    @Test public void archStopFreezesImmediatelyThenRunsOutAtOnePointFiveSpeed() {
        advance(120); run.player.setPosition(7279, 184); run.update(1f / 60f, 1, false);
        assertTrue(run.isCleared()); assertFalse(run.isFinished()); assertEquals(7280, run.player.x, 0);
        double time = run.getElapsed(); int score = run.getScore();
        run.update(1f / 60f, -1, true); assertEquals(7286.5f, run.player.x, .01f);
        assertEquals(184, run.player.y, .01f);
        advance(180); assertTrue(run.isFinished()); assertTrue(run.player.x >= EasyMap.WORLD_WIDTH + run.player.width);
        assertEquals(time, run.getElapsed(), 0); assertEquals(score, run.getScore());
    }
    @Test public void debugSectionsResetAttemptAndStayWithinTheMap() {
        for (int i = 0; i < 6; i++) {
            run.jumpToSection(i); assertEquals(map.sectionStarts[i].x, run.player.x, 0);
            assertEquals(map.sectionStarts[i].y, run.player.y, 0); assertEquals(5, run.getLives());
            assertEquals(i == 0, run.isArriving());
            for (Rectangle ground : map.ground) assertFalse("Debug spawn must not overlap solid terrain", run.player.overlaps(ground));
        }
    }
    @Test public void clockFormatsMinutesAndCountsSlowFrames() {
        run.update(.5f, 0, false); assertEquals(.5, run.getElapsed(), .0001);
        assertEquals("00:59", EasyRun.formatTime(59.9)); assertEquals("01:00", EasyRun.formatTime(60));
        assertEquals("100:01", EasyRun.formatTime(6001));
    }
}
