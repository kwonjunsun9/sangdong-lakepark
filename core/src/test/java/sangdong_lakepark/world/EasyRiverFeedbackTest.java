package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class EasyRiverFeedbackTest {
    private final EasyMap map = new EasyMap();
    private final EasyRun run = new EasyRun(map);
    private void advance(int frames, float direction) {
        for (int i = 0; i < frames; i++) run.update(1f / 60f, direction, false);
    }

    @Test public void riverWavesMoveAroundAWaterlineSixtyFourPixelsBelowShore() {
        float sum = 0;
        for (int x = 4384; x < 4544; x++) {
            float height = EasyMap.riverSurface(x, 0);
            assertTrue(height >= 115 && height <= 125); sum += height;
        }
        assertEquals(EasyMap.GROUND_Y - 64, sum / 160, .01f);
        assertNotEquals(EasyMap.riverSurface(4500, 0), EasyMap.riverSurface(4500, .7), .1f);
    }

    @Test public void enteringWaterCostsExactlyOneLifeDismountsAndAllowsAnotherPickup() {
        EasyLayoutTest.equip(map, run);
        run.player.setPosition(4600, EasyMap.RIVER_LEVEL + 6);
        run.update(1f / 120f, 0, false);
        assertEquals(5, run.getLives()); assertTrue(run.isRiding());
        run.player.setPosition(4600, EasyMap.RIVER_LEVEL - 6);
        run.update(1f / 120f, 0, false);
        assertEquals(4, run.getLives()); assertFalse(run.isRiding()); assertFalse(run.isItemUsed());
        assertEquals(1, run.getWaterEntryCount());
        assertEquals(4250, run.player.x, 0); assertEquals(184, run.player.y, 0);
        assertTrue(run.getBorderFeedback() > 0); assertTrue(run.getDamageFeedback() > 0);
        advance(120, 0); assertEquals(4, run.getLives()); assertEquals(0, run.getBorderFeedback(), 0);
        assertEquals(1, run.getWaterEntryCount());
        EasyLayoutTest.equip(map, run); assertTrue(run.isRiding());
    }

    @Test public void riverStillCostsLifeDuringProtectionAndDismountsOnTheLastLife() {
        EasyLayoutTest.equip(map, run);
        for (int life = 4; life >= 1; life--) {
            run.player.setPosition(1200, -101); run.update(1f / 120f, 0, false);
            assertEquals(life, run.getLives()); assertFalse(run.isRiding());
        }
        assertTrue(run.isInvulnerable());
        run.player.setPosition(2810, map.itemBlock.y - run.player.height - 5);
        run.update(1f / 60f, 0, true); assertTrue(run.isBicycleAvailable());
        run.player.setPosition(map.bicycle.x, map.bicycle.y); run.update(1f / 120f, 0, false);
        assertTrue(run.isRiding()); assertTrue(run.isInvulnerable());
        run.player.setPosition(4600, EasyMap.RIVER_LEVEL - 6); run.update(1f / 120f, 0, false);
        assertEquals(0, run.getLives()); assertTrue(run.isFailed()); assertFalse(run.isRiding());
        run.update(.4f, 0, false); assertTrue(run.getBorderFeedback() > 0);
        assertEquals(0, run.getDamageFeedback(), 0);
        run.update(.11f, 0, false); assertEquals(0, run.getBorderFeedback(), 0);
        run.reset(); assertEquals(5, run.getLives()); assertEquals(0, run.getBorderFeedback(), 0);
    }

    @Test public void walkingIntoTerrainAndBlocksNeverFlashesOrLosesLife() {
        advance(120, 0); run.player.setPosition(510, 184);
        advance(2, 1);
        assertEquals(512, run.player.x, .01f); assertEquals(0, run.getBorderFeedback(), 0);
        assertEquals(5, run.getLives()); assertEquals(0, run.getDamageFeedback(), 0);
        advance(60, 1); assertEquals(0, run.getBorderFeedback(), 0);
        advance(2, -1); advance(4, 1); assertEquals(0, run.getBorderFeedback(), 0);
        run.reset(); assertEquals(0, run.getBorderFeedback(), 0);
        for (Rectangle block : new Rectangle[]{map.blocks.first(), map.blinking.first()}) {
            run.reset(); advance(120, 0);
            run.player.setPosition(block.x - run.player.width - 2, block.y + 8); advance(2, 1);
            assertEquals(block.x, run.player.x + run.player.width, .01f);
            assertEquals(0, run.getBorderFeedback(), 0); assertEquals(5, run.getLives());
        }
    }

    @Test public void blockHeadCollisionStillSpawnsBikeWithoutFlashing() {
        advance(120, 0); assertEquals(0, run.getBorderFeedback(), 0);
        run.player.setPosition(2810, 184); run.update(1f / 60f, 0, true);
        for (int i = 0; i < 60 && !run.isBicycleAvailable(); i++) {
            advance(1, 0); assertEquals(0, run.getBorderFeedback(), 0);
        }
        assertTrue(run.isBicycleAvailable());
        assertTrue(run.player.y + run.player.height <= map.itemBlock.y + .1f);
        assertEquals(5, run.getLives()); assertEquals(0, run.getDamageFeedback(), 0);
        advance(90, 0); assertEquals(0, run.getBorderFeedback(), 0);
        run.player.setPosition(400, 320); advance(60, 0);
        assertTrue(run.isGrounded()); assertEquals(184, run.player.y, .01f);
        assertEquals(0, run.getBorderFeedback(), 0);
    }
}
