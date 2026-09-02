package mthiebi.sgs.gradebook.service.roster;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * What the roster screens send.
 * <p>
 * Separate from the read rows on purpose: a student is written with a password
 * and read without one, and a draft that doubled as a view would either leak
 * the hash or tempt somebody into round-tripping it back.
 */
public final class RosterDraft {

    private RosterDraft() {
    }

    @Data
    public static class Student {
        private Long id;
        private String firstName;
        private String lastName;
        private String personalNumber;
        private String username;

        /**
         * Plain text, hashed by the service.
         * <p>
         * Null on an edit means "leave the password alone", which is what makes
         * the edit form usable without showing the current one. It is not the
         * same as an empty string, which is a password somebody typed nothing
         * into and is refused.
         */
        private String password;
        private String guardianEmail;
        private Boolean active;

        /**
         * Where to put them for the year being edited. Null enrols them
         * nowhere, which is allowed - a record can exist before a placement is
         * decided.
         */
        private Long classGroupId;
        private LocalDate joinedOn;
    }

    @Data
    public static class ClassGroup {
        private Long id;
        private String name;
        private Short level;
        private Long schoolId;
        private Long academicYearId;
        private Long periodSchemeId;
    }

    @Data
    public static class Subject {
        private Long id;
        private String name;
        private String shortName;
        private Boolean active;
    }

    @Data
    public static class ClassSubject {
        private Long subjectId;
        private String teacherName;
        private Long teacherUserId;
    }

    /**
     * The order of a class's subjects, as the ids in the order they should sit.
     */
    @Data
    public static class Reorder {
        private List<Long> classSubjectIds;
    }

    @Data
    public static class Move {
        private Long classGroupId;
        private LocalDate on;
    }

    /**
     * Starting the next academic year.
     * <p>
     * Deliberately small: the year, its period tree, and optionally a copy of
     * this year's class list. No enrollments - the school decides who goes
     * where, and nothing here can guess it well enough to be worth the
     * unpicking.
     */
    @Data
    public static class NewYear {
        private String code;
        private LocalDate startsOn;
        private LocalDate endsOn;
        /**
         * Copy the class list from this year, with the level incremented.
         */
        private Long copyClassesFromYearId;
        private boolean makeCurrent;
    }
}
