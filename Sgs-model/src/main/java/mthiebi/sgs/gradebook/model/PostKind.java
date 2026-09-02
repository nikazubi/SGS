package mthiebi.sgs.gradebook.model;

/**
 * Which of the brief's content modules a post belongs to.
 * <p>
 * They share a table because they differ in about four fields and agree on
 * everything structural - authored by staff, scoped to a class, dated, drafted
 * then published, edited, archived. Five near-identical tables would become five
 * services that drift, which is how the legacy system ended up with four
 * copy-pasted export methods that each excluded a different set of columns.
 * <p>
 * Only HOMEWORK is built in phase 8. The rest are declared so the discriminator
 * is honest about what the table is for.
 */
public enum PostKind {

    /**
     * Per subject, per date, optionally aimed at particular students.
     */
    HOMEWORK,

    /**
     * The class's standing weekly routine. One per class.
     */
    SCHEDULE,

    /**
     * The week's menu. One per class.
     */
    MENU,

    /**
     * A subject teacher's written account of one student.
     */
    CHARACTERIZATION,

    /**
     * School-wide, with a picture and a category. Not scoped to a class.
     */
    NEWS
}
