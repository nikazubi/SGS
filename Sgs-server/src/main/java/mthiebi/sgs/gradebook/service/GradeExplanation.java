package mthiebi.sgs.gradebook.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import mthiebi.sgs.gradebook.engine.EvaluationTrace;

import java.math.BigDecimal;

/**
 * The working behind one calculated cell, for the popover a teacher opens when
 * they think a column is wrong. Usually they are right to check: the old
 * monthly formula divided homework by a fixed four however many marks existed,
 * and nothing on screen ever said so.
 */
@Data
@AllArgsConstructor
public class GradeExplanation {
    private String componentCode;
    private String componentLabel;
    private BigDecimal value;
    private EvaluationTrace trace;
}
