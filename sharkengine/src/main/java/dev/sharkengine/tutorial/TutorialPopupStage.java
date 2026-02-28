package dev.sharkengine.tutorial;

public enum TutorialPopupStage {
    WELCOME("welcome"),
    MODE_SELECTION("mode_selection"),
    BUILD_GUIDE("build_guide"),
    READY_TO_LAUNCH("ready"),
    FLIGHT_TIPS("flight_tips");

    private final String id;

    TutorialPopupStage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TutorialPopupStage fromId(String id) {
        for (TutorialPopupStage stage : values()) {
            if (stage.id.equals(id)) {
                return stage;
            }
        }
        return WELCOME;
    }
}
