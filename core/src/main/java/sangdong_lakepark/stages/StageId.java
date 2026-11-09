package sangdong_lakepark.stages;

/** The order of the four stops on the route to Sangdong Lake Park. */
public enum StageId {
    EASY("Easy"), NORMAL("Normal"), HARD("Hard"), EXTREME("Extreme");

    private final String displayName;

    StageId(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
