package sangdong_lakepark.world;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class EasyCollisionTest {
    private void advance(EasyRun run, int frames, float direction) {
        for (int i = 0; i < frames; i++) run.update(1f / 60f, direction, false);
    }

    @Test public void fallingBesideTallHillCannotWalkInsideAndCanStillJumpOntoIt() {
        for (int direction : new int[]{1, -1}) {
            EasyMap map = new EasyMap(); EasyRun run = new EasyRun(map); advance(run, 120, 0);
            Rectangle hill = null;
            for (Rectangle ground : map.ground) if (ground.x == 3064) hill = ground;
            assertNotNull(hill);
            run.player.setPosition(direction > 0 ? 2972 : 3204, 274); advance(run, 1, 0);
            assertTrue(run.isGrounded());
            // Jump past the edge with the body, but stop before the feet reach the top.
            boolean overhang = false;
            for (int frame = 0; frame < 90; frame++) {
                run.update(1f / 60f, direction, frame == 0);
                overhang = direction > 0 ? run.player.x + run.player.width > hill.x + .5f
                    : run.player.x < hill.x + hill.width - .5f;
                if (overhang) break;
            }
            assertTrue(overhang); advance(run, 90, 0);
            assertTrue(run.isGrounded()); assertEquals(274, run.player.y, .01f);
            advance(run, 35, direction);
            assertFalse("Walking from an overhang must not enter the hill", run.player.overlaps(hill));
            assertEquals(direction > 0 ? hill.x - run.player.width : hill.x + hill.width, run.player.x, .01f);
            assertEquals(5, run.getLives()); assertEquals(0, run.getBorderFeedback(), 0);
            float target = direction > 0 ? 3032 : 3144;
            run.update(1f / 60f, direction, true);
            for (int frame = 0; frame < 60; frame++)
                run.update(1f / 60f, Math.abs(target - run.player.x) < 3 ? 0 : Math.signum(target - run.player.x), false);
            assertTrue(run.isGrounded()); assertEquals(hill.y + hill.height, run.player.y, .01f);
        }
    }

    @Test public void solidWoodAndBlinkingEdgesAlsoBlockInwardMovesAfterFalling() {
        EasyMap map = new EasyMap();
        Rectangle[] edges = {map.blocks.first(), map.blocks.get(2), map.blinking.first(), map.blinking.get(1)};
        for (int i = 0; i < edges.length; i++) {
            Rectangle edge = edges[i]; EasyRun run = new EasyRun(map); advance(run, 120, 0);
            boolean fromLeft = i % 2 == 0;
            run.player.setPosition(fromLeft ? edge.x - run.player.width + 4 : edge.x + edge.width - 4,
                edge.y + edge.height + 40);
            advance(run, 20, 0); run.update(1f / 60f, fromLeft ? 1 : -1, false);
            assertFalse("Overhanging body must not bypass a solid block", run.player.overlaps(edge));
            assertEquals(fromLeft ? edge.x - run.player.width : edge.x + edge.width, run.player.x, .01f);
            assertEquals(5, run.getLives()); assertEquals(0, run.getBorderFeedback(), 0);
        }
    }

    @Test public void leavingRoundedLedgeDoesNotPushPlayerSidewaysOrPreventFalling() {
        for (int direction : new int[]{1, -1}) {
            EasyRun run = new EasyRun(new EasyMap()); advance(run, 120, 0);
            run.player.setPosition(direction > 0 ? 612 : 628, 232); advance(run, 1, 0);
            for (int frame = 0; frame < 22; frame++) {
                float x = run.player.x; advance(run, 1, direction);
                assertTrue(Math.abs(run.player.x - x) <= EasyMap.MOVE_SPEED / 60 + .01f);
            }
            float x = run.player.x; advance(run, 60, 0);
            assertEquals(x, run.player.x, .01f); assertEquals(184, run.player.y, .01f);
            assertEquals(5, run.getLives());
        }
    }
}
