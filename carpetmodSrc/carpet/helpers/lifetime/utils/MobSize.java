package carpet.helpers.lifetime.utils;

public enum MobSize {
    ADULT("Adult"),
    BABY("Baby"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large");
    private final String display;

    MobSize(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
