package sangdong_lakepark.dialogue;

import org.junit.Test;

import static org.junit.Assert.*;

public class DialogueTimerTest {
    @Test public void voicedPageWaitsForCompletionThenAFullSecondAndFiresOnce() {
        DialogueTimer timer = new DialogueTimer();
        timer.reset(true, 2);
        assertFalse(timer.update(30, false));
        assertFalse(timer.update(.5f, true));
        assertFalse(timer.update(.75f, true));
        assertTrue(timer.update(.25f, true));
        assertFalse(timer.update(10, true));
    }

    @Test public void silentPageWaitsTwoSecondsFromDisplay() {
        DialogueTimer timer = new DialogueTimer();
        timer.reset(false, 2);
        assertFalse(timer.update(1.75f, false));
        assertTrue(timer.update(.25f, false));
    }

    @Test public void rewindingRestartsBothVoiceWaitAndCountdown() {
        DialogueTimer timer = new DialogueTimer();
        timer.reset(true, 2);
        timer.update(.1f, true);
        assertFalse(timer.update(.75f, true));
        timer.reset(true, 2);
        assertFalse(timer.update(10, false));
        assertFalse(timer.update(.1f, true));
        assertFalse(timer.update(.75f, true));
        assertTrue(timer.update(.25f, true));
    }

    @Test public void manualNavigationDiscardsThePreviousSilentCountdown() {
        DialogueTimer timer = new DialogueTimer();
        timer.reset(false, 2);
        assertFalse(timer.update(1.75f, false));
        timer.reset(false, 2);
        assertFalse(timer.update(.25f, true));
        assertTrue(timer.update(1.75f, true));
    }

    @Test public void frozenFramesDoNotObserveCompletionOrAdvanceTime() {
        DialogueTimer timer = new DialogueTimer();
        timer.reset(true, 2);
        assertFalse(timer.update(0, true));
        assertFalse(timer.update(1, true));
        assertFalse(timer.update(0, true));
        assertFalse(timer.update(-1, true));
        assertTrue(timer.update(1, true));
    }

    @Test public void normalConversationUsesVoiceOnOddPagesAndTwoSecondsOnEvenPages() {
        DialogueScript script = DialogueScript.NORMAL;
        DialogueTimer timer = new DialogueTimer();
        assertEquals(4, script.lastPage());
        for (int page = 0; page <= script.lastPage(); page++) {
            DialogueScript.Line line = script.line(page);
            boolean voiced = line.voice != null;
            assertEquals(page % 2 == 0, voiced);
            assertEquals(page % 2 == 1, line.mother);
            timer.reset(voiced, script.silentPageDelay());
            if (voiced) {
                assertFalse(timer.update(10, false));
                assertFalse(timer.update(.1f, true));
                assertTrue(timer.update(1, true));
            } else {
                assertFalse(timer.update(1.75f, true));
                assertTrue(timer.update(.25f, true));
            }
        }
    }

    @Test public void openingConversationKeepsItsThreeSecondSilentPage() {
        DialogueTimer timer = new DialogueTimer();
        DialogueScript script = DialogueScript.INTRO;
        timer.reset(script.line(1).voice != null, script.silentPageDelay());
        assertFalse(timer.update(2.75f, true));
        assertTrue(timer.update(.25f, true));
    }

    @Test public void extremePagesMatchTheUploadedVoiceOrderAndWaitOneSecondAfterEachVoice() {
        DialogueScript script=DialogueScript.EXTREME;
        String[] voices={"extreme-departure.mp3","extreme-night-ride.mp3","extreme-fun.mp3"};
        assertEquals(2,script.lastPage());
        for(int page=0;page<voices.length;page++){
            DialogueScript.Line line=script.line(page);
            assertEquals(voices[page],line.voice);assertEquals("인직카와",line.speaker());
            DialogueTimer timer=new DialogueTimer();timer.reset(true,script.silentPageDelay());
            assertFalse(timer.update(20,false));assertFalse(timer.update(.1f,true));
            assertFalse(timer.update(.75f,true));assertTrue(timer.update(.25f,true));
            assertFalse(timer.update(5,true));
        }
        assertFalse(script.usesRiderOverlay());assertFalse(script.shakeLastPage);
    }
}
