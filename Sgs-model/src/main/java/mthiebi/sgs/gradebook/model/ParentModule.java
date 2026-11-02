package mthiebi.sgs.gradebook.model;

/**
 * Something the parent portal can show a child, granted by
 * {@link ParentModuleGrant} rows.
 * <p>
 * Declared in landing-page order, and the module list is returned in it.
 * <p>
 * A journal names the module that shows it ({@link GradingTemplate#getParentModule()}).
 * Absence is not here because the absence register names none: every child
 * sees it.
 * <p>
 * {@code db/038} and {@code db/039} pin the stored values with CHECK constraints,
 * so a new value here needs both widened in the migration that first uses it.
 */
public enum ParentModule {

    HOMEWORK,

    NEWS,

    /**
     * The class's weekly timetable. Shares its name with {@link PostKind#SCHEDULE}.
     */
    SCHEDULE,

    /**
     * The week's meals. Shares its name with {@link PostKind#MENU}.
     */
    MENU,

    /**
     * What the school writes about the child, per subject.
     */
    CHARACTERIZATION,

    /**
     * The journals that name it: the trimester gradebook, ethics, and by
     * default any journal the school creates later.
     */
    GRADES
}
