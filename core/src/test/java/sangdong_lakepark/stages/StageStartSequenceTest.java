package sangdong_lakepark.stages;

import org.junit.Test;
import static org.junit.Assert.*;

public class StageStartSequenceTest {
    @Test public void readyGrowsThenHoldsBeforeGoSlidesAwayAtAConstantSize() {
        StageStartSequence sequence = new StageStartSequence();
        assertTrue(sequence.isActive()); assertTrue(sequence.isReady());
        assertEquals(.8f, sequence.getScale(), .001f);
        sequence.update(.2f);
        assertEquals(1, sequence.getScale(), .001f);
        sequence.update(.99f);
        assertTrue(sequence.isReady()); assertEquals(0, sequence.getOffsetX(), 0);
        sequence.update(.02f);
        assertFalse(sequence.isReady()); assertTrue(sequence.isActive());
        float previousX = 0, previousAlpha = 1;
        float goTime = .01f;
        while (sequence.isActive()) {
            assertEquals(1, sequence.getScale(), 0);
            assertTrue(sequence.getOffsetX() >= previousX);
            assertTrue(sequence.getAlpha() <= previousAlpha);
            previousX = sequence.getOffsetX(); previousAlpha = sequence.getAlpha();
            sequence.update(.02f);
            goTime += .02f;
        }
        assertEquals("Go keeps gameplay locked for one second", 1f, goTime, .021f);
        assertEquals(0, sequence.getAlpha(), .001f);
    }

    @Test public void frozenFramesDoNotAdvanceAndRetryStartsWithReadyAgain() {
        StageStartSequence sequence = new StageStartSequence();
        sequence.update(1.5f);
        float x = sequence.getOffsetX(), alpha = sequence.getAlpha();
        sequence.update(0); sequence.update(-1);
        assertEquals(x, sequence.getOffsetX(), 0); assertEquals(alpha, sequence.getAlpha(), 0);
        sequence.update(10); assertFalse(sequence.isActive());
        sequence.reset(); assertTrue(sequence.isActive()); assertTrue(sequence.isReady());
        assertEquals(0, sequence.getOffsetX(), 0); assertEquals(1, sequence.getAlpha(), 0);
        sequence.finish(); assertFalse(sequence.isActive());
    }
}
