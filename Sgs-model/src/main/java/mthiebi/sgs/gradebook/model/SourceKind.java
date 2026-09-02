package mthiebi.sgs.gradebook.model;

public enum SourceKind {
    /**
     * A single component in the same subject.
     */
    COMPONENT,
    /**
     * Several components reduced to one number, e.g. avg(ONGOING_1..7).
     */
    GROUP,
    /**
     * The same component across every subject the student takes. Exists because
     * the legacy "rating" column averages a student's marks over all subjects;
     * every other source kind resolves inside one subject.
     */
    ALL_SUBJECTS
}
