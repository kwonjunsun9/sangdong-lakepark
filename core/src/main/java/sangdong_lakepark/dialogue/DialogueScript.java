package sangdong_lakepark.dialogue;

/** Conversations shown before the corresponding stage starts. */
public enum DialogueScript {
    INTRO("IntroDialogue", true,
        new Line("어머니,\n상동 호수공원 어디로 가요?", false, "intro-question.mp3"),
        new Line("이 쪽 방향에서...", true, null),
        new Line("오케이 감사드리고~", false, "intro-thanks.mp3"),
        new Line("자 출발!\n달달달달달달달달달달달", false, "intro-departure.mp3")),
    NORMAL("NormalDialogue", false,
        new Line("어머니, 안녕하세요~ 비감입니다~\n혹시 그 상동 호수공원 어디로 가는지 아세요?", false, "normal-greeting.mp3"),
        new Line("일로 쭉 가시다가 또 여쭤봐야해요.\n왜냐면 그냥 지금 여기서 가르쳐드려도 좀 멀어서~", true, null),
        new Line("좀 멀어요?\n많이 멀어요??", false, "normal-distance.mp3"),
        new Line("계~속 직진하시다가 한번\n한번 더 여쭤보세요.", true, null),
        new Line("아 알겠습니다~\n고맙습니다 비감이었습니다~", false, "normal-thanks.mp3")),
    HARD("HardDialogue",false,
        new Line("저 안녕하세요 길 좀 물을게요~\n그 혹시 상동 호수공원 어디로 가는지 아세요?",false,"hard-question.mp3"),
        new Line("어... 일단 쭉 가시고\n다음에 또 물어보시는게 나아요~",true,null),
        new Line("아 그래요?\n왜 이렇게 미인이세요?",false,"hard-thanks.mp3"),
        new Line("??? 네???",true,null)),
    EXTREME("ExtremeDialogue",false,
        new Line("달달달달달달달달달달달달달",false,"extreme-departure.mp3"),
        new Line("바람을 느끼고 가본다~\n어젯밤을~ 위해서~",false,"extreme-night-ride.mp3"),
        new Line("인지기 재미떠여~\n인지기 이거 너무 재미떠여~",false,"extreme-fun.mp3"));

    public final String logTag;
    public final boolean shakeLastPage;
    private final Line[] lines;

    DialogueScript(String logTag, boolean shakeLastPage, Line... lines) {
        this.logTag = logTag;
        this.shakeLastPage = shakeLastPage;
        this.lines = lines;
    }

    public int lastPage() {
        return lines.length - 1;
    }

    public Line line(int index) {
        return lines[index];
    }

    public String backgroundPath() {
        return this == EXTREME ? "sangdong/extreme/park-conversation.png" : this == HARD ? "sangdong/hard/park-conversation.png" : this == NORMAL ? "sangdong/normal/park-conversation.png" : "sangdong/intro/park-conversation.png";
    }

    public boolean usesRiderOverlay() {
        // The Normal reference scene already includes the rider and the woman in blue.
        return this == INTRO;
    }

    public float silentPageDelay() {
        return this != INTRO ? 2 : 3;
    }

    public static final class Line {
        public final String text, voice;
        public final boolean mother;

        private Line(String text, boolean mother, String voice) {
            this.text = text;
            this.mother = mother;
            this.voice = voice;
        }

        public String speaker() {
            return mother ? "어머니" : "인직카와";
        }
    }
}
