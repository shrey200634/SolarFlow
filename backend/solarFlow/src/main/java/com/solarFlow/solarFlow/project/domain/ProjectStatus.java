package com.solarFlow.solarFlow.project.domain;


public enum ProjectStatus {
    ADVANCE_PAID,
    INSTALLATION_SCHEDULED,
    INSTALLED,
    SUBSIDY_CREDITED;

    public ProjectStatus next() {
        ProjectStatus[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    public boolean canMoveTo(ProjectStatus target) {
        return target != null && target == next();
    }
}