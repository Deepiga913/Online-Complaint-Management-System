package com.cms.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the lifecycle state of a Complaint.
 * Encodes the legal state-transition graph so business rules
 * (e.g. a CLOSED ticket can never jump back to NEW) live in one place.
 */
public enum ComplaintStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> TRANSITIONS = Map.of(
            NEW, EnumSet.of(IN_PROGRESS, CLOSED),
            IN_PROGRESS, EnumSet.of(RESOLVED, CLOSED),
            RESOLVED, EnumSet.of(CLOSED, IN_PROGRESS),
            CLOSED, EnumSet.noneOf(ComplaintStatus.class)
    );

    public boolean canTransitionTo(ComplaintStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
