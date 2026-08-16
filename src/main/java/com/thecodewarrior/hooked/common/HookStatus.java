package com.thecodewarrior.hooked.common;

public enum HookStatus {

    EXTENDING,
    PLANTED,
    RETRACTING;

    public static HookStatus byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : RETRACTING;
    }
}
