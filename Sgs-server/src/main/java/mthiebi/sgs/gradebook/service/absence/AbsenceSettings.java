package mthiebi.sgs.gradebook.service.absence;

/**
 * The keys ClassPeriodSetting uses for the brief's two inputs.
 * <p>
 * Named constants rather than string literals scattered about: the setting
 * table is key/value on purpose, and a typo in a key is a setting that silently
 * does nothing.
 */
public final class AbsenceSettings {

    /**
     * თვის აკადემიური საათების სრული რაოდენობა
     */
    public static final String TOTAL_ACADEMIC_HOURS = "TOTAL_ACADEMIC_HOURS";

    /**
     * გაცდენილი საათების დასაშვები რაოდენობა - what turns the chart red.
     */
    public static final String PERMITTED_MISSED_HOURS = "PERMITTED_MISSED_HOURS";

    private AbsenceSettings() {
    }
}
