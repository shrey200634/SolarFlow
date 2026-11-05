package com.solarFlow.solarFlow.project;

import com.solarFlow.solarFlow.project.domain.ProjectStatus;
import org.junit.jupiter.api.Test;

import static com.solarFlow.solarFlow.project.domain.ProjectStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectStatusTest {

    @Test
    void movesForwardOneStageAtATime() {
        assertTrue(ADVANCE_PAID.canMoveTo(INSTALLATION_SCHEDULED));
        assertTrue(INSTALLATION_SCHEDULED.canMoveTo(INSTALLED));
        assertTrue(INSTALLED.canMoveTo(SUBSIDY_CREDITED));
    }

    @Test
    void skippingStagesIsBlocked() {
        assertFalse(ADVANCE_PAID.canMoveTo(INSTALLED));
        assertFalse(ADVANCE_PAID.canMoveTo(SUBSIDY_CREDITED));
        assertFalse(INSTALLATION_SCHEDULED.canMoveTo(SUBSIDY_CREDITED));
    }

    @Test
    void movingBackwardsOrStayingIsBlocked() {
        assertFalse(INSTALLED.canMoveTo(ADVANCE_PAID));
        assertFalse(INSTALLED.canMoveTo(INSTALLATION_SCHEDULED));
        assertFalse(INSTALLED.canMoveTo(INSTALLED));
    }

    @Test
    void finalStageHasNoNext() {
        assertNull(SUBSIDY_CREDITED.next());
        assertFalse(SUBSIDY_CREDITED.canMoveTo(ADVANCE_PAID));
        assertFalse(SUBSIDY_CREDITED.canMoveTo(SUBSIDY_CREDITED));
    }

    @Test
    void nullTargetIsBlocked() {
        assertFalse(ADVANCE_PAID.canMoveTo(null));
    }

    @Test
    void sequenceIsInTheAgreedOrder() {
        assertArrayEquals(
                new ProjectStatus[]{ADVANCE_PAID, INSTALLATION_SCHEDULED, INSTALLED, SUBSIDY_CREDITED},
                ProjectStatus.values());
    }
}